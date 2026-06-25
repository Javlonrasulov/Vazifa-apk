import { IsArray, IsBoolean, IsEnum, IsNotEmpty, IsOptional, IsString, MinLength } from 'class-validator';
import { UserRole } from '../../common/enums';

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
  @IsArray()
  @IsString({ each: true })
  adminPermissions?: string[];

  @IsOptional()
  @IsArray()
  @IsString({ each: true })
  visibleDepartments?: string[];
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
}

export class ResetPasswordDto {
  @IsString()
  @MinLength(6)
  newPassword: string;
}

export class ApproveDeviceDto {
  approve: boolean;
}
