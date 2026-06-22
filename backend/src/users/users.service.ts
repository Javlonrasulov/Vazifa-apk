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
import { Task } from '../tasks/entities/task.entity';
import { TaskComment } from '../tasks/entities/task-comment.entity';
import { TaskAttachment } from '../tasks/entities/task-attachment.entity';
import { TaskAssignment } from '../tasks/entities/task-assignment.entity';
import { ChatMessage } from '../chat/entities/chat-message.entity';
import { AuditLog } from '../audit/entities/audit-log.entity';

@Injectable()
export class UsersService implements OnModuleInit {
  static readonly DEFAULT_ADMIN_PERMISSIONS = ['employees', 'system_users'];

  constructor(
    @InjectRepository(User)
    private repo: Repository<User>,
    @InjectRepository(UserFieldOption)
    private optionRepo: Repository<UserFieldOption>,
    private dataSource: DataSource,
    private audit: AuditService,
  ) {}

  async onModuleInit() {
    const users = await this.repo.find({
      select: ['id', 'role', 'position', 'department', 'adminPermissions', 'canAccessAdminPanel'],
    });
    for (const user of users) {
      await this.rememberFieldOption('position', user.position);
      await this.rememberFieldOption('department', user.department);
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
    if (!trimmed) return;

    const nameNormalized = trimmed.toLowerCase();
    const exists = await this.optionRepo.findOne({ where: { type, nameNormalized } });
    if (exists) return;

    await this.optionRepo.save(
      this.optionRepo.create({ type, name: trimmed, nameNormalized }),
    );
  }

  async getFieldOptions(type: UserFieldOptionType, q?: string) {
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

  async getMobileDepartmentOptions(): Promise<string[]> {
    const users = await this.repo.find({
      where: { role: UserRole.EMPLOYEE, isActive: true },
      select: ['department'],
    });
    return [
      ...new Set(
        users
          .map((u) => u.department?.trim())
          .filter((d): d is string => !!d),
      ),
    ].sort((a, b) => a.localeCompare(b, 'uz'));
  }

  async findAll() {
    const users = await this.repo.find({ order: { createdAt: 'DESC' } });
    return users.map((u) => this.sanitize(u));
  }

  async findEmployeesAndDirectors() {
    const users = await this.repo.find({
      where: [
        { role: UserRole.EMPLOYEE, isActive: true },
        { role: UserRole.DIRECTOR, isActive: true },
      ],
      order: { fullName: 'ASC' },
    });
    return users.map((u) => this.sanitize(u));
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

    const user = this.repo.create({
      login,
      passwordHash: await bcrypt.hash(dto.password?.trim() || '123456', 10),
      fullName: dto.fullName.trim(),
      role: dto.role,
      canAssignTasks: dto.canAssignTasks ?? dto.role === UserRole.DIRECTOR,
      position: isAdmin ? null : (dto.position ?? null),
      department: isAdmin ? null : (dto.department ?? null),
      phone: isAdmin ? null : (dto.phone ?? null),
      adminPermissions: isAdmin
        ? (dto.adminPermissions?.length
            ? dto.adminPermissions
            : UsersService.DEFAULT_ADMIN_PERMISSIONS)
        : null,
    });
    const saved = await this.repo.save(user);
    await this.rememberFieldOption('position', saved.position);
    await this.rememberFieldOption('department', saved.department);
    await this.audit.log(actorId ?? null, AuditAction.USER_CREATED, 'user', saved.id, {
      login: saved.login,
      role: saved.role,
    });
    return this.sanitize(saved);
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
    if (dto.password) {
      user.passwordHash = await bcrypt.hash(dto.password, 10);
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
    if (dto.position !== undefined) user.position = dto.position;
    if (dto.department !== undefined) user.department = dto.department;
    if (dto.phone !== undefined) {
      await this.assertUniquePhone(dto.phone, id);
      user.phone = dto.phone;
    }
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
    await this.rememberFieldOption('department', saved.department);
    await this.audit.log(actorId ?? null, AuditAction.USER_UPDATED, 'user', id);
    return this.sanitize(saved);
  }

  async resetPassword(id: string, newPassword: string, actorId?: string) {
    const user = await this.findById(id);
    user.passwordHash = await bcrypt.hash(newPassword, 10);
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
    user.deviceId = null;
    user.deviceApproved = false;
    user.pendingDeviceId = null;
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
      user.deviceId = user.pendingDeviceId;
      user.deviceApproved = true;
    }
    user.pendingDeviceId = null;
    await this.repo.save(user);
    await this.audit.log(actorId ?? null, AuditAction.DEVICE_BOUND, 'user', id, { approve });
    return this.sanitize(user);
  }

  async saveUser(user: User) {
    return this.repo.save(user);
  }

  sanitize(user: User) {
    const { passwordHash, fcmToken, ...rest } = user;
    return rest;
  }
}
