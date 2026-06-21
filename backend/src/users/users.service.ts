import {
  BadRequestException,
  ConflictException,
  Injectable,
  NotFoundException,
} from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import * as bcrypt from 'bcrypt';
import { Repository } from 'typeorm';
import { User } from './entities/user.entity';
import { UserRole } from '../common/enums';
import { CreateUserDto, UpdateUserDto } from '../auth/dto/auth.dto';
import { AuditService } from '../audit/audit.service';
import { AuditAction } from '../common/enums';
import { phonesMatch } from '../common/utils/phone';

@Injectable()
export class UsersService {
  constructor(
    @InjectRepository(User)
    private repo: Repository<User>,
    private audit: AuditService,
  ) {}

  async findAll() {
    const users = await this.repo.find({ order: { createdAt: 'DESC' } });
    return users.map((u) => this.sanitize(u));
  }

  async findEmployeesAndDirectors() {
    const users = await this.repo.find({
      where: [{ role: UserRole.EMPLOYEE }, { role: UserRole.DIRECTOR }],
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

  async create(dto: CreateUserDto, actorId?: string) {
    const login = dto.login.toLowerCase().trim();
    const exists = await this.findByLogin(login);
    if (exists) throw new ConflictException('Login band');

    if (dto.role === UserRole.ADMIN) {
      throw new BadRequestException('Admin faqat seed orqali yaratiladi');
    }

    const user = this.repo.create({
      login,
      passwordHash: await bcrypt.hash(dto.password?.trim() || '123456', 10),
      fullName: dto.fullName.trim(),
      role: dto.role,
      canAssignTasks: dto.canAssignTasks ?? dto.role === UserRole.DIRECTOR,
      position: dto.position ?? null,
      department: dto.department ?? null,
      phone: dto.phone ?? null,
    });
    const saved = await this.repo.save(user);
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
    if (dto.fullName !== undefined) user.fullName = dto.fullName.trim();
    if (dto.role !== undefined) user.role = dto.role;
    if (dto.canAssignTasks !== undefined) user.canAssignTasks = dto.canAssignTasks;
    if (dto.position !== undefined) user.position = dto.position;
    if (dto.department !== undefined) user.department = dto.department;
    if (dto.phone !== undefined) user.phone = dto.phone;
    if (dto.isActive !== undefined) user.isActive = dto.isActive;
    const saved = await this.repo.save(user);
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
      throw new BadRequestException('Admin o\'chirilmaydi');
    }
    await this.repo.remove(user);
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
