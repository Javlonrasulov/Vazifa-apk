import {
  BadRequestException,
  ForbiddenException,
  Injectable,
  NotFoundException,
} from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { Announcement, AnnouncementStatus } from './entities/announcement.entity';
import { AnnouncementRecipient } from './entities/announcement-recipient.entity';
import { AnnouncementAttachment } from './entities/announcement-attachment.entity';
import { CreateAnnouncementDto } from './dto/announcement.dto';
import { User } from '../users/entities/user.entity';
import { userCanAssignTasks } from '../common/utils/user-permissions';
import { parseTashkent, nowTashkent } from '../common/utils/time';
import { NotificationsService } from '../notifications/notifications.service';
import { UsersService } from '../users/users.service';
import { newAnnouncementText } from '../notifications/push-i18n';
import { compressImageIfNeeded } from '../common/utils/image';

@Injectable()
export class AnnouncementsService {
  constructor(
    @InjectRepository(Announcement) private announcementRepo: Repository<Announcement>,
    @InjectRepository(AnnouncementRecipient) private recipientRepo: Repository<AnnouncementRecipient>,
    @InjectRepository(AnnouncementAttachment) private attachRepo: Repository<AnnouncementAttachment>,
    private notifications: NotificationsService,
    private usersService: UsersService,
  ) {}

  async create(dto: CreateAnnouncementDto, creator: User) {
    if (!userCanAssignTasks(creator)) {
      throw new ForbiddenException('Xabar yuborish huquqi yo\'q');
    }

    const rawIds = [...new Set(dto.recipientIds)];
    const selfIncluded = rawIds.includes(creator.id);
    const recipientIds = rawIds.filter((id) => id !== creator.id);
    if (!recipientIds.length) {
      throw new BadRequestException(
        selfIncluded
          ? 'O\'zingizga xabar yubora olmaysiz'
          : 'Kamida bitta xodim tanlang',
      );
    }

    const deadlineAt = parseTashkent(dto.deadlineAt);
    if (deadlineAt <= nowTashkent()) {
      throw new BadRequestException('Muddat kelajakdagi vaqt bo\'lishi kerak');
    }

    const announcement = this.announcementRepo.create({
      title: dto.title.trim(),
      description: dto.description?.trim() ?? null,
      deadlineAt,
      reminderIntervalMinutes: dto.reminderIntervalMinutes,
      createdById: creator.id,
      status: AnnouncementStatus.ACTIVE,
    });
    const saved = await this.announcementRepo.save(announcement);

    const recipients = recipientIds.map((recipientId) =>
      this.recipientRepo.create({
        announcementId: saved.id,
        recipientId,
        lastReminderAt: nowTashkent(),
      }),
    );
    await this.recipientRepo.save(recipients);

    const users = await Promise.all(recipientIds.map((id) => this.usersService.findById(id)));
    await Promise.all(
      users
        .filter((u) => u.notificationsEnabled)
        .map((u) => {
          const text = newAnnouncementText(saved.title, u.language);
          return this.notifications.notifyUser(u.id, text.title, text.body, {
            announcementId: saved.id,
            type: 'announcement',
          });
        }),
    );

    return this.findOne(saved.id, creator);
  }

  async findSent(user: User) {
    if (!userCanAssignTasks(user)) {
      throw new ForbiddenException('Ruxsat yo\'q');
    }
    return this.announcementRepo.find({
      where: { createdById: user.id },
      relations: ['recipients', 'recipients.recipient', 'createdBy'],
      order: { createdAt: 'DESC' },
    });
  }

  async findReceived(user: User) {
    const rows = await this.recipientRepo.find({
      where: { recipientId: user.id },
      relations: ['announcement', 'announcement.createdBy', 'announcement.attachments'],
      order: { createdAt: 'DESC' },
    });
    return rows.map((r) => ({
      ...r.announcement,
      myRecipientId: r.id,
      acknowledgedAt: r.acknowledgedAt,
    }));
  }

  async findOne(id: string, user: User) {
    const announcement = await this.announcementRepo.findOne({
      where: { id },
      relations: [
        'recipients',
        'recipients.recipient',
        'createdBy',
        'attachments',
      ],
    });
    if (!announcement) throw new NotFoundException('Xabar topilmadi');

    const isCreator = announcement.createdById === user.id;
    const isRecipient = announcement.recipients.some((r) => r.recipientId === user.id);
    if (!isCreator && !isRecipient) {
      throw new ForbiddenException('Ruxsat yo\'q');
    }

    return announcement;
  }

  async acknowledge(id: string, user: User) {
    const recipient = await this.recipientRepo.findOne({
      where: { announcementId: id, recipientId: user.id },
      relations: ['announcement'],
    });
    if (!recipient) throw new NotFoundException('Xabar topilmadi');
    if (recipient.acknowledgedAt) {
      return { acknowledged: true, acknowledgedAt: recipient.acknowledgedAt };
    }

    recipient.acknowledgedAt = nowTashkent();
    await this.recipientRepo.save(recipient);
    return { acknowledged: true, acknowledgedAt: recipient.acknowledgedAt };
  }

  async cancel(id: string, user: User) {
    const announcement = await this.announcementRepo.findOne({ where: { id } });
    if (!announcement) throw new NotFoundException('Xabar topilmadi');
    if (announcement.createdById !== user.id) {
      throw new ForbiddenException('Faqat yaratuvchi bekor qila oladi');
    }
    announcement.status = AnnouncementStatus.CANCELLED;
    await this.announcementRepo.save(announcement);
    return announcement;
  }

  async addAttachment(id: string, user: User, file: Express.Multer.File) {
    const announcement = await this.findOne(id, user);
    if (announcement.createdById !== user.id) {
      throw new ForbiddenException('Faqat yaratuvchi fayl yuklay oladi');
    }

    const compressed = await compressImageIfNeeded(file.path, file.mimetype);
    const att = this.attachRepo.create({
      announcementId: id,
      uploadedById: user.id,
      fileName: file.originalname || 'file',
      filePath: compressed.filePath,
      mimeType: compressed.mimeType,
      fileSize: compressed.fileSize,
    });
    return this.attachRepo.save(att);
  }

  async getAttachmentFile(attachmentId: string, user: User) {
    const att = await this.attachRepo.findOne({
      where: { id: attachmentId },
      relations: ['announcement', 'announcement.recipients'],
    });
    if (!att) throw new NotFoundException('Fayl topilmadi');

    const isCreator = att.announcement.createdById === user.id;
    const isRecipient = att.announcement.recipients.some((r) => r.recipientId === user.id);
    if (!isCreator && !isRecipient) {
      throw new ForbiddenException('Ruxsat yo\'q');
    }
    return att;
  }

  /** Cron: eslatma yuborish uchun faol qabul qiluvchilar */
  async getPendingReminders(): Promise<AnnouncementRecipient[]> {
    const now = nowTashkent();
    return this.recipientRepo
      .createQueryBuilder('r')
      .innerJoinAndSelect('r.announcement', 'a')
      .innerJoinAndSelect('r.recipient', 'u')
      .where('r.acknowledgedAt IS NULL')
      .andWhere('a.status = :status', { status: AnnouncementStatus.ACTIVE })
      .andWhere('a.deadlineAt > :now', { now })
      .getMany();
  }

  async markReminderSent(recipientId: string) {
    await this.recipientRepo.update(recipientId, { lastReminderAt: nowTashkent() });
  }

  async expireOverdue() {
    const now = nowTashkent();
    await this.announcementRepo
      .createQueryBuilder()
      .update(Announcement)
      .set({ status: AnnouncementStatus.EXPIRED })
      .where('status = :status', { status: AnnouncementStatus.ACTIVE })
      .andWhere('deadlineAt <= :now', { now })
      .execute();
  }
}
