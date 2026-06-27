import {
  ForbiddenException,
  Injectable,
  UnauthorizedException,
} from '@nestjs/common';
import { JwtService } from '@nestjs/jwt';
import { ConfigService } from '@nestjs/config';
import * as bcrypt from 'bcrypt';
import { UsersService } from '../users/users.service';
import { User } from '../users/entities/user.entity';
import { UserRole } from '../common/enums';
import { AuditService } from '../audit/audit.service';
import { AuditAction } from '../common/enums';
import { bindUserDevice, getApprovedDevices, deviceLimitMessage } from '../common/utils/user-devices';

@Injectable()
export class AuthService {
  constructor(
    private usersService: UsersService,
    private jwt: JwtService,
    private config: ConfigService,
    private audit: AuditService,
  ) {}

  async validateUser(userId: string): Promise<User | null> {
    try {
      return await this.usersService.findById(userId);
    } catch {
      return null;
    }
  }

  async login(identifier: string, password: string, deviceId: string, deviceName?: string) {
    const trimmed = identifier.trim();
    const user =
      (await this.usersService.findByLogin(trimmed.toLowerCase())) ??
      (await this.usersService.findByPhone(trimmed));
    if (!user || !user.isActive) {
      throw new UnauthorizedException('Login yoki parol noto\'g\'ri');
    }

    const valid = await bcrypt.compare(password, user.passwordHash);
    if (!valid) throw new UnauthorizedException('Login yoki parol noto\'g\'ri');

    // Admin uses web panel only
    if (user.role === UserRole.ADMIN) {
      throw new ForbiddenException('Admin web panel orqali kiradi');
    }

    const hadDevice = getApprovedDevices(user).some((d) => d.id === deviceId);
    const bindResult = bindUserDevice(user, deviceId, deviceName);
    if (bindResult === 'limit') {
      throw new ForbiddenException({
        code: 'DEVICE_LIMIT_REACHED',
        message: deviceLimitMessage(),
      });
    }
    await this.usersService.saveUser(user);
    await this.usersService.touchLastSeen(user.id, true);
    if (!hadDevice) {
      await this.audit.log(user.id, AuditAction.DEVICE_BOUND, 'user', user.id, { deviceId });
    }

    const tokens = await this.generateTokens(user);
    await this.audit.log(user.id, AuditAction.LOGIN, 'user', user.id, { deviceId });

    return {
      ...tokens,
      user: this.usersService.sanitize(user),
      devicePendingApproval: false,
    };
  }

  async adminLogin(login: string, password: string) {
    const user = await this.usersService.findByLogin(login);
    if (!user || user.role !== UserRole.ADMIN || !user.isActive) {
      throw new UnauthorizedException('Login yoki parol noto\'g\'ri');
    }
    const valid = await bcrypt.compare(password, user.passwordHash);
    if (!valid) throw new UnauthorizedException('Login yoki parol noto\'g\'ri');

    const tokens = await this.generateTokens(user);
    await this.audit.log(user.id, AuditAction.LOGIN, 'user', user.id);
    return { ...tokens, user: this.usersService.sanitize(user) };
  }

  async refresh(refreshToken: string) {
    try {
      const payload = this.jwt.verify(refreshToken, {
        secret: this.config.get('JWT_REFRESH_SECRET'),
      });
      const user = await this.validateUser(payload.sub);
      if (!user || !user.isActive) throw new UnauthorizedException();
      return this.generateTokens(user);
    } catch {
      throw new UnauthorizedException('Refresh token yaroqsiz');
    }
  }

  async changePassword(userId: string, current: string, newPass: string) {
    const user = await this.usersService.findById(userId);
    const valid = await bcrypt.compare(current, user.passwordHash);
    if (!valid) throw new UnauthorizedException('Joriy parol noto\'g\'ri');
    const trimmed = newPass.trim();
    user.passwordHash = await bcrypt.hash(trimmed, 10);
    user.passwordPlain = trimmed;
    await this.usersService.saveUser(user);
    return { success: true };
  }

  async updateFcm(
    userId: string,
    fcmToken: string,
    enabled: boolean,
    language?: string,
  ) {
    const user = await this.usersService.findById(userId);
    user.fcmToken = fcmToken;
    user.notificationsEnabled = enabled;
    if (language) user.language = language;
    await this.usersService.saveUser(user);
    return this.usersService.sanitize(user);
  }

  async heartbeat(userId: string) {
    await this.usersService.touchLastSeen(userId, true);
    const user = await this.usersService.findById(userId);
    return this.usersService.sanitizeWithPresence(user);
  }

  private async generateTokens(user: User) {
    const payload = { sub: user.id, login: user.login, role: user.role };
    const [accessToken, refreshToken] = await Promise.all([
      this.jwt.signAsync(payload, {
        secret: this.config.get('JWT_SECRET'),
        expiresIn: this.config.get('JWT_ACCESS_EXPIRES', '15m'),
      }),
      this.jwt.signAsync(payload, {
        secret: this.config.get('JWT_REFRESH_SECRET'),
        expiresIn: this.config.get('JWT_REFRESH_EXPIRES', '7d'),
      }),
    ]);
    return { accessToken, refreshToken };
  }
}
