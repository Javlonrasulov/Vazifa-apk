import {
  IsArray,
  IsBoolean,
  IsEnum,
  IsInt,
  IsNotEmpty,
  IsOptional,
  IsString,
  Matches,
  Max,
  Min,
  MinLength,
} from 'class-validator';
import { UserRole } from '../../common/enums';

const TIME_PATTERN = /^([01]\d|2[0-3]):[0-5]\d$/;

export class LoginDto {
  @IsString()
  @IsNotEmpty()
  login: string;

  @IsString()
  @MinLength(4)
  password: string;

  @IsString()
  @IsNotEmpty()
  deviceId: string;

  @IsOptional()
  @IsString()
  deviceName?: string;
}

export class RefreshTokenDto {
  @IsString()
  refreshToken: string;
}

export class ChangePasswordDto {
  @IsString()
  @MinLength(4)
  currentPassword: string;

  @IsString()
  @MinLength(6)
  newPassword: string;
}

export class UpdateFcmDto {
  @IsString()
  @IsNotEmpty()
  fcmToken: string;

  @IsBoolean()
  notificationsEnabled: boolean;

  @IsOptional()
  @IsString()
  language?: string;
}

export class CreateUserDto {
  @IsString()
  login: string;

  @IsOptional()
  @IsString()
  @MinLength(6)
  password?: string;

  @IsString()
  fullName: string;

  @IsEnum(UserRole)
  role: UserRole;

  @IsOptional()
  @IsString()
  position?: string;

  @IsOptional()
  @IsString()
  department?: string;

  @IsOptional()
  @IsString()
  phone?: string;

  @IsOptional()
  @IsBoolean()
  canAssignTasks?: boolean;

  @IsOptional()
  @IsBoolean()
  allowScreenshot?: boolean;

  @IsOptional()
  @IsArray()
  @IsString({ each: true })
  adminPermissions?: string[];

  @IsOptional()
  @IsArray()
  @IsString({ each: true })
  visibleDepartments?: string[];

  @IsOptional()
  @Matches(TIME_PATTERN, { message: 'restStart HH:MM formatida bo\'lishi kerak' })
  restStart?: string | null;

  @IsOptional()
  @Matches(TIME_PATTERN, { message: 'restEnd HH:MM formatida bo\'lishi kerak' })
  restEnd?: string | null;

  @IsOptional()
  @IsArray()
  @IsInt({ each: true })
  @Min(0, { each: true })
  @Max(6, { each: true })
  restDays?: number[] | null;
}

export class UpdateUserDto {
  @IsOptional()
  @IsString()
  fullName?: string;

  @IsOptional()
  @IsString()
  login?: string;

  @IsOptional()
  @IsString()
  @MinLength(6)
  password?: string;

  @IsOptional()
  @IsEnum(UserRole)
  role?: UserRole;

  @IsOptional()
  @IsString()
  position?: string;

  @IsOptional()
  @IsString()
  department?: string;

  @IsOptional()
  @IsString()
  phone?: string;

  @IsOptional()
  isActive?: boolean;

  @IsOptional()
  @IsBoolean()
  canAssignTasks?: boolean;

  @IsOptional()
  @IsBoolean()
  allowScreenshot?: boolean;

  @IsOptional()
  @IsArray()
  @IsString({ each: true })
  adminPermissions?: string[];

  @IsOptional()
  @IsBoolean()
  canAccessAdminPanel?: boolean;

  @IsOptional()
  @IsArray()
  @IsString({ each: true })
  visibleDepartments?: string[];

  @IsOptional()
  @Matches(TIME_PATTERN, { message: 'restStart HH:MM formatida bo\'lishi kerak' })
  restStart?: string | null;

  @IsOptional()
  @Matches(TIME_PATTERN, { message: 'restEnd HH:MM formatida bo\'lishi kerak' })
  restEnd?: string | null;

  @IsOptional()
  @IsArray()
  @IsInt({ each: true })
  @Min(0, { each: true })
  @Max(6, { each: true })
  restDays?: number[] | null;
}

export class ResetPasswordDto {
  @IsString()
  @MinLength(6)
  newPassword: string;
}

export class ApproveDeviceDto {
  approve: boolean;
}
