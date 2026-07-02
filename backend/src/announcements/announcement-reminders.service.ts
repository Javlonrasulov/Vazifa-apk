import { Injectable, Logger } from '@nestjs/common';
import { Cron } from '@nestjs/schedule';
import { AnnouncementsService } from './announcements.service';
import { NotificationsService } from '../notifications/notifications.service';
import { announcementReminderText } from '../notifications/push-i18n';
import { nowTashkent } from '../common/utils/time';

@Injectable()
export class AnnouncementRemindersService {
  private readonly logger = new Logger(AnnouncementRemindersService.name);

  constructor(
    private announcementsService: AnnouncementsService,
    private notifications: NotificationsService,
  ) {}

  /** Har daqiqada xabar eslatmalarini tekshiradi */
  @Cron('* * * * *')
  async sendReminders() {
    await this.announcementsService.expireOverdue();
    const pending = await this.announcementsService.getPendingReminders();
    const now = nowTashkent();
    let sent = 0;

    for (const r of pending) {
      if (!r.recipient?.notificationsEnabled) continue;

      const intervalMs = r.announcement.reminderIntervalMinutes * 60 * 1000;
      const lastAt = r.lastReminderAt ?? r.createdAt;
      if (now.getTime() - lastAt.getTime() < intervalMs) continue;

      const text = announcementReminderText(r.announcement.title, r.recipient.language);
      await this.notifications.notifyUser(
        r.recipient.id,
        text.title,
        text.body,
        { announcementId: r.announcementId, type: 'announcement' },
      );
      await this.announcementsService.markReminderSent(r.id);
      sent++;
    }

    if (sent > 0) {
      this.logger.log(`Xabar eslatmasi: ${sent} ta yuborildi`);
    }
  }
}
