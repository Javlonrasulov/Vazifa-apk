import { IsArray, IsBoolean, IsEnum, IsNotEmpty, IsOptional, IsString, MinLength } from 'class-validator';
import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';
import { UserRole } from '../../common/enums';

export class LoginDto {
  @ApiProperty({ example: 'director1' })
  @IsString()
  @IsNotEmpty()
  login: string;

  @ApiProperty({ example: 'password123' })
  @IsString()
  @MinLength(4)
  password: string;

  @ApiProperty({ description: 'Android device ID' })
  @IsString()
  @IsNotEmpty()
  deviceId: string;
}

export class RefreshTokenDto {
  @ApiProperty()
  @IsString()
  refreshToken: string;
}

export class ChangePasswordDto {
  @ApiProperty()
  @IsString()
  @MinLength(4)
  currentPassword: string;

  @ApiProperty()
  @IsString()
  @MinLength(6)
  newPassword: string;
}

export class UpdateFcmDto {
  @ApiProperty()
  @IsString()
  @IsNotEmpty()
  fcmToken: string;

  @ApiProperty()
  @IsBoolean()
  notificationsEnabled: boolean;
}

export class CreateUserDto {
  @ApiProperty()
  @IsString()
  login: string;

  @ApiPropertyOptional({ default: '123456' })
  @IsOptional()
  @IsString()
  @MinLength(6)
  password?: string;

  @ApiProperty()
  @IsString()
  fullName: string;

  @ApiProperty({ enum: UserRole })
  @IsEnum(UserRole)
  role: UserRole;

  @ApiPropertyOptional()
  @IsOptional()
  @IsString()
  position?: string;

  @ApiPropertyOptional()
  @IsOptional()
  @IsString()
  department?: string;

  @ApiPropertyOptional()
  @IsOptional()
  @IsString()
  phone?: string;

  @ApiPropertyOptional({ description: 'Boshqalarga vazifa berish huquqi' })
  @IsOptional()
  @IsBoolean()
  canAssignTasks?: boolean;

  @ApiPropertyOptional({ description: 'Admin panel sahifalariga ruxsatlar', type: [String] })
  @IsOptional()
  @IsArray()
  @IsString({ each: true })
  adminPermissions?: string[];
}

export class UpdateUserDto {
  @ApiPropertyOptional()
  @IsOptional()
  @IsString()
  fullName?: string;

  @ApiPropertyOptional()
  @IsOptional()
  @IsString()
  login?: string;

  @ApiPropertyOptional()
  @IsOptional()
  @IsString()
  @MinLength(6)
  password?: string;

  @ApiPropertyOptional({ enum: UserRole })
  @IsOptional()
  @IsEnum(UserRole)
  role?: UserRole;

  @ApiPropertyOptional()
  @IsOptional()
  @IsString()
  position?: string;

  @ApiPropertyOptional()
  @IsOptional()
  @IsString()
  department?: string;

  @ApiPropertyOptional()
  @IsOptional()
  @IsString()
  phone?: string;

  @ApiPropertyOptional()
  @IsOptional()
  isActive?: boolean;

  @ApiPropertyOptional()
  @IsOptional()
  @IsBoolean()
  canAssignTasks?: boolean;

  @ApiPropertyOptional({ type: [String] })
  @IsOptional()
  @IsArray()
  @IsString({ each: true })
  adminPermissions?: string[];

  @ApiPropertyOptional()
  @IsOptional()
  @IsBoolean()
  canAccessAdminPanel?: boolean;
}

export class ResetPasswordDto {
  @ApiProperty()
  @IsString()
  @MinLength(6)
  newPassword: string;
}

export class ApproveDeviceDto {
  @ApiProperty()
  approve: boolean;
}
