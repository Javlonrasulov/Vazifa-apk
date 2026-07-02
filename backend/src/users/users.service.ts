import {
  BadRequestException,
  ConflictException,
  Injectable,
  NotFoundException,
  OnModuleInit,
} from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import * as bcrypt from 'bcrypt';
import { DataSource, Repository } from 'typeorm';
import { User } from './entities/user.entity';
import { UserFieldOption, UserFieldOptionType } from './entities/user-field-option.entity';
import { UserRole } from '../common/enums';
import { CreateUserDto, UpdateUserDto } from '../auth/dto/auth.dto';
import { AuditService } from '../audit/audit.service';
import { AuditAction } from '../common/enums';
import { phonesMatch } from '../common/utils/phone';
import { mediaUrl } from '../common/utils/media-url';
import { Task } from '../tasks/entities/task.entity';
import { TaskComment } from '../tasks/entities/task-comment.entity';
import { TaskAttachment } from '../tasks/entities/task-attachment.entity';
import { TaskAssignment } from '../tasks/entities/task-assignment.entity';
import { ChatMessage } from '../chat/entities/chat-message.entity';
import { AuditLog } from '../audit/entities/audit-log.entity';
import { resetUserDevices, bindUserDevice, MAX_USER_DEVICES } from '../common/utils/user-devices';
import { DepartmentsService } from '../departments/departments.service';
import {
  isUserOnline,
  PRESENCE_TOUCH_THROTTLE_MS,
  resolveLastSeenAt,
} from '../common/utils/presence';

@Injectable()
export class UsersService implements OnModuleInit {
  static readonly DEFAULT_ADMIN_PERMISSIONS = ['employees', 'system_users'];

  private readonly lastSeenTouchCache = new Map<string, number>();

  constructor(
    @InjectRepository(User)
    private repo: Repository<User>,
    @InjectRepository(UserFieldOption)
    private optionRepo: Repository<UserFieldOption>,
    private dataSource: DataSource,
    private audit: AuditService,
    private departmentsService: DepartmentsService,
  ) {}

  async onModuleInit() {
    const users = await this.repo.find({
      select: ['id', 'role', 'position', 'department', 'adminPermissions', 'canAccessAdminPanel'],
    });
    for (const user of users) {
      await this.rememberFieldOption('position', user.position);
      if (user.role === UserRole.ADMIN && !user.adminPermissions?.length) {
        user.adminPermissions = UsersService.DEFAULT_ADMIN_PERMISSIONS;
        await this.repo.save(user);
      }
      if (user.role !== UserRole.ADMIN && user.canAccessAdminPanel) {
        user.canAccessAdminPanel = false;
        user.adminPermissions = null;
        await this.repo.save(user);
      }
    }
  }

  async rememberFieldOption(type: UserFieldOptionType, name?: string | null) {
    const trimmed = name?.trim();
    if (!trimmed || type === 'department') return;

    const nameNormalized = trimmed.toLowerCase();
    const exists = await this.optionRepo.findOne({ where: { type, nameNormalized } });
    if (exists) return;

    await this.optionRepo.save(
      this.optionRepo.create({ type, name: trimmed, nameNormalized }),
    );
  }

  async getFieldOptions(type: UserFieldOptionType, q?: string) {
    if (type === 'department') {
      return this.departmentsService.findNames(q);
    }

    const qb = this.optionRepo
      .createQueryBuilder('o')
      .where('o.type = :type', { type })
      .orderBy('o.name', 'ASC')
      .limit(30);

    const query = q?.trim().toLowerCase();
    if (query) {
      qb.andWhere('o.nameNormalized LIKE :q', { q: `%${query}%` });
    }

    const rows = await qb.getMany();
    return rows.map((r) => r.name);
  }

  async getMobileDepartments() {
    return this.departmentsService.findAll();
  }

  private async applyPassword(user: User, plain: string) {
    const trimmed = plain.trim();
    user.passwordHash = await bcrypt.hash(trimmed, 10);
    user.passwordPlain = trimmed;
  }

  async findAll() {
    const users = await this.repo.find({ order: { createdAt: 'DESC' } });
    return users.map((u) => this.sanitize(u, { includePasswordPlain: true }));
  }

  async findEmployeesAndDirectors() {
    const users = await this.repo.find({
      where: [
        { role: UserRole.EMPLOYEE, isActive: true },
        { role: UserRole.DIRECTOR, isActive: true },
      ],
      order: { fullName: 'ASC' },
    });
    return users.map((u) => this.sanitizeWithPresence(u));
  }

  async findById(id: string) {
    const user = await this.repo.findOne({ where: { id } });
    if (!user) throw new NotFoundException('Foydalanuvchi topilmadi');
    return user;
  }

  async findByLogin(login: string) {
    return this.repo.findOne({ where: { login: login.toLowerCase() } });
  }

  async findByPhone(phone: string) {
    const users = await this.repo
      .createQueryBuilder('u')
      .where('u.phone IS NOT NULL')
      .getMany();
    return users.find((u) => phonesMatch(u.phone ?? '', phone)) ?? null;
  }

  async findByFullName(fullName: string, role: UserRole, excludeId?: string) {
    const qb = this.repo
      .createQueryBuilder('u')
      .where('LOWER(TRIM(u.fullName)) = LOWER(:name)', { name: fullName.trim() });
    if (role === UserRole.ADMIN) {
      qb.andWhere('u.role = :role', { role: UserRole.ADMIN });
    } else {
      qb.andWhere('u.role IN (:...roles)', { roles: [UserRole.EMPLOYEE, UserRole.DIRECTOR] });
    }
    if (excludeId) qb.andWhere('u.id != :excludeId', { excludeId });
    return qb.getOne();
  }

  private async assertUniquePhone(phone: string | null | undefined, excludeId?: string) {
    if (!phone?.trim()) return;
    const existing = await this.findByPhone(phone);
    if (existing && existing.id !== excludeId) {
      throw new ConflictException('Bu telefon allaqachon mavjud');
    }
  }

  private async assertUniqueFullName(fullName: string, role: UserRole, excludeId?: string) {
    const existing = await this.findByFullName(fullName, role, excludeId);
    if (existing) {
      throw new ConflictException('Bu ism allaqachon mavjud');
    }
  }

  private async countActiveAdmins(excludeId?: string) {
    const admins = await this.repo.find({
      where: { role: UserRole.ADMIN, isActive: true },
      select: ['id'],
    });
    return excludeId ? admins.filter((a) => a.id !== excludeId).length : admins.length;
  }

  async create(dto: CreateUserDto, actorId?: string) {
    const login = dto.login.toLowerCase().trim();
    const exists = await this.findByLogin(login);
    if (exists) {
      if (exists.role !== UserRole.ADMIN) {
        throw new ConflictException('Bu login APK foydalanuvchisida band');
      }
      throw new ConflictException('Login band');
    }

    const isAdmin = dto.role === UserRole.ADMIN;
    await this.assertUniqueFullName(dto.fullName, dto.role);
    if (!isAdmin) {
      await this.assertUniquePhone(dto.phone);
    }

    const plain = dto.password?.trim() || '123456';
    const user = this.repo.create({
      login,
      passwordHash: await bcrypt.hash(plain, 10),
      passwordPlain: plain,
      fullName: dto.fullName.trim(),
      role: dto.role,
      canAssignTasks: dto.canAssignTasks ?? dto.role !== UserRole.ADMIN,
      allowScreenshot: isAdmin ? true : (dto.allowScreenshot ?? true),
      position: isAdmin ? null : (dto.position ?? null),
      department: isAdmin ? null : (dto.department ?? null),
      visibleDepartments: isAdmin
        ? null
        : this.normalizeVisibleDepartments(dto.visibleDepartments),
      phone: isAdmin ? null : (dto.phone ?? null),
      restStart: isAdmin ? null : (dto.restStart ?? null),
      restEnd: isAdmin ? null : (dto.restEnd ?? null),
      restDays: isAdmin ? null : this.normalizeRestDays(dto.restDays),
      adminPermissions: isAdmin
        ? (dto.adminPermissions?.length
            ? dto.adminPermissions
            : UsersService.DEFAULT_ADMIN_PERMISSIONS)
        : null,
    });
    const saved = await this.repo.save(user);
    await this.rememberFieldOption('position', saved.position);
    await this.audit.log(actorId ?? null, AuditAction.USER_CREATED, 'user', saved.id, {
      login: saved.login,
      role: saved.role,
    });
    return this.sanitize(saved, { includePasswordPlain: true });
  }

  async update(id: string, dto: UpdateUserDto, actorId?: string) {
    const user = await this.findById(id);
    if (dto.login !== undefined) {
      const login = dto.login.toLowerCase().trim();
      if (login !== user.login) {
        const exists = await this.findByLogin(login);
        if (exists) throw new ConflictException('Login band');
        user.login = login;
      }
    }
    if (dto.password?.trim()) {
      await this.applyPassword(user, dto.password);
    }
    if (dto.fullName !== undefined) {
      await this.assertUniqueFullName(dto.fullName, user.role, id);
      user.fullName = dto.fullName.trim();
    }
    if (dto.role !== undefined) {
      if (user.role === UserRole.ADMIN && dto.role !== UserRole.ADMIN) {
        const adminCount = await this.countActiveAdmins(id);
        if (adminCount === 0) {
          throw new BadRequestException('Oxirgi admin roli o\'zgartirilmaydi');
        }
      }
      user.role = dto.role;
    }
    if (dto.canAssignTasks !== undefined) user.canAssignTasks = dto.canAssignTasks;
    if (dto.allowScreenshot !== undefined) user.allowScreenshot = dto.allowScreenshot;
    if (dto.position !== undefined) user.position = dto.position;
    if (dto.department !== undefined) user.department = dto.department;
    if (dto.visibleDepartments !== undefined) {
      user.visibleDepartments = this.normalizeVisibleDepartments(dto.visibleDepartments);
    }
    if (dto.phone !== undefined) {
      await this.assertUniquePhone(dto.phone, id);
      user.phone = dto.phone;
    }
    if (dto.restStart !== undefined) user.restStart = dto.restStart || null;
    if (dto.restEnd !== undefined) user.restEnd = dto.restEnd || null;
    if (dto.restDays !== undefined) user.restDays = this.normalizeRestDays(dto.restDays);
    if (dto.isActive !== undefined) {
      if (user.role === UserRole.ADMIN && !dto.isActive) {
        const adminCount = await this.countActiveAdmins(id);
        if (adminCount === 0) {
          throw new BadRequestException('Oxirgi admin o\'chirilmaydi');
        }
      }
      user.isActive = dto.isActive;
    }
    if (dto.adminPermissions !== undefined) {
      user.adminPermissions =
        user.role === UserRole.ADMIN
          ? dto.adminPermissions.length
            ? dto.adminPermissions
            : UsersService.DEFAULT_ADMIN_PERMISSIONS
          : null;
    }
    const saved = await this.repo.save(user);
    await this.rememberFieldOption('position', saved.position);
    await this.audit.log(actorId ?? null, AuditAction.USER_UPDATED, 'user', id);
    return this.sanitize(saved, { includePasswordPlain: true });
  }

  async resetPassword(id: string, newPassword: string, actorId?: string) {
    const trimmed = newPassword.trim();
    if (trimmed.length < 6) {
      throw new BadRequestException('Parol kamida 6 belgidan iborat bo\'lishi kerak');
    }
    const user = await this.findById(id);
    await this.applyPassword(user, trimmed);
    await this.repo.save(user);
    await this.audit.log(actorId ?? null, AuditAction.USER_UPDATED, 'user', id, {
      action: 'password_reset',
    });
    return { success: true };
  }

  async delete(id: string, actorId?: string) {
    const user = await this.findById(id);
    if (user.role === UserRole.ADMIN) {
      const adminCount = await this.countActiveAdmins();
      if (adminCount <= 1) {
        throw new BadRequestException('Oxirgi admin o\'chirilmaydi');
      }
    }

    await this.dataSource.transaction(async (manager) => {
      await manager
        .createQueryBuilder()
        .delete()
        .from(ChatMessage)
        .where('"senderId" = :id OR "receiverId" = :id', { id })
        .execute();

      await manager.delete(TaskComment, { authorId: id });
      await manager.delete(TaskAttachment, { uploadedById: id });
      await manager.delete(TaskAssignment, { assigneeId: id });

      const ownedTasks = await manager.find(Task, { where: { createdById: id } });
      if (ownedTasks.length) {
        await manager.remove(ownedTasks);
      }

      await manager.update(AuditLog, { userId: id }, { userId: null });
      await manager.remove(user);
    });

    await this.audit.log(actorId ?? null, AuditAction.USER_DELETED, 'user', id);
    return { success: true };
  }

  async resetDevice(id: string, actorId?: string) {
    const user = await this.findById(id);
    resetUserDevices(user);
    await this.repo.save(user);
    await this.audit.log(actorId ?? null, AuditAction.DEVICE_RESET, 'user', id);
    return this.sanitize(user);
  }

  async approveDevice(id: string, approve: boolean, actorId?: string) {
    const user = await this.findById(id);
    if (!user.pendingDeviceId) {
      throw new BadRequestException('Kutilayotgan qurilma yo\'q');
    }
    if (approve) {
      const bindResult = bindUserDevice(user, user.pendingDeviceId);
      if (bindResult === 'limit') {
        throw new BadRequestException(`Maksimal ${MAX_USER_DEVICES} ta qurilma`);
      }
    } else {
      user.pendingDeviceId = null;
    }
    await this.repo.save(user);
    await this.audit.log(actorId ?? null, AuditAction.DEVICE_BOUND, 'user', id, { approve });
    return this.sanitize(user);
  }

  async saveUser(user: User) {
    return this.repo.save(user);
  }

  async clearFcmToken(token: string) {
    if (!token?.trim()) return;
    await this.repo.update({ fcmToken: token }, { fcmToken: null, notificationsEnabled: false });
  }

  async setAvatar(userId: string, filePath: string | null) {
    const user = await this.findById(userId);
    if (!user) throw new NotFoundException('Foydalanuvchi topilmadi');
    user.avatarUrl = filePath ? mediaUrl(filePath) : null;
    await this.repo.save(user);
    return this.sanitizeWithPresence(user);
  }

  sanitize(user: User, options?: { includePasswordPlain?: boolean }) {
    const { passwordHash, fcmToken, passwordPlain, allowScreenshot, ...rest } = user;
    return {
      ...rest,
      allowScreenshot: allowScreenshot !== false,
      ...(options?.includePasswordPlain ? { passwordPlain: passwordPlain ?? null } : {}),
    };
  }

  async touchLastSeen(userId: string, force = false): Promise<void> {
    const now = Date.now();
    const lastTouch = this.lastSeenTouchCache.get(userId) ?? 0;
    if (!force && now - lastTouch < PRESENCE_TOUCH_THROTTLE_MS) return;

    this.lastSeenTouchCache.set(userId, now);
    await this.repo.update(userId, { lastSeenAt: new Date(now) });
  }

  sanitizeWithPresence(user: User) {
    const base = this.sanitize(user);
    const lastSeen = resolveLastSeenAt(user);
    return {
      ...base,
      lastSeenAt: lastSeen?.toISOString() ?? null,
      isOnline: isUserOnline(user),
    };
  }

  private normalizeRestDays(days?: number[] | null): number[] | null {
    if (!days?.length) return null;
    const unique = [...new Set(days.filter((d) => d >= 0 && d <= 6))].sort();
    return unique.length ? unique : null;
  }

  private normalizeVisibleDepartments(departments?: string[] | null): string[] | null {
    if (!departments?.length) return null;
    const unique = new Map<string, string>();
    for (const item of departments) {
      const trimmed = item?.trim();
      if (!trimmed) continue;
      unique.set(trimmed.toLowerCase(), trimmed);
    }
    const values = [...unique.values()];
    return values.length ? values : null;
  }
}
