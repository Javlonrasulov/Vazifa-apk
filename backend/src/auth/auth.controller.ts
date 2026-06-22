import { Body, Controller, Get, Post, UseGuards, Request } from '@nestjs/common';
import { AuthGuard } from '@nestjs/passport';
import { AuthService } from './auth.service';
import {
  ChangePasswordDto,
  LoginDto,
  RefreshTokenDto,
  UpdateFcmDto,
} from './dto/auth.dto';
import { serverTimeResponse } from '../common/utils/time';
import { User } from '../users/entities/user.entity';

@Controller('auth')
export class AuthController {
  constructor(private authService: AuthService) {}

  @Post('login')
  login(@Body() dto: LoginDto) {
    return this.authService.login(dto.login, dto.password, dto.deviceId);
  }

  @Post('admin/login')
  adminLogin(@Body() dto: Omit<LoginDto, 'deviceId'>) {
    return this.authService.adminLogin(dto.login, dto.password);
  }

  @Post('refresh')
  refresh(@Body() dto: RefreshTokenDto) {
    return this.authService.refresh(dto.refreshToken);
  }

  @Get('time')
  getServerTime() {
    return serverTimeResponse();
  }

  @UseGuards(AuthGuard('jwt'))
  @Post('change-password')
  changePassword(@Request() req: { user: User }, @Body() dto: ChangePasswordDto) {
    return this.authService.changePassword(req.user.id, dto.currentPassword, dto.newPassword);
  }

  @UseGuards(AuthGuard('jwt'))
  @Post('fcm')
  updateFcm(@Request() req: { user: User }, @Body() dto: UpdateFcmDto) {
    return this.authService.updateFcm(req.user.id, dto.fcmToken, dto.notificationsEnabled);
  }

  @UseGuards(AuthGuard('jwt'))
  @Get('me')
  me(@Request() req: { user: User }) {
    const { passwordHash, fcmToken, ...rest } = req.user;
    return rest;
  }
}
