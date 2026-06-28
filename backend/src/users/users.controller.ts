import {
  Body,
  Controller,
  Delete,
  Get,
  Param,
  Patch,
  Post,
  Query,
  UploadedFile,
  UseGuards,
  UseInterceptors,
  Request,
} from '@nestjs/common';
import { AuthGuard } from '@nestjs/passport';
import { FileInterceptor } from '@nestjs/platform-express';
import { diskStorage } from 'multer';
import { extname } from 'path';
import { v4 as uuid } from 'uuid';
import { UsersService } from './users.service';
import {
  ApproveDeviceDto,
  CreateUserDto,
  ResetPasswordDto,
  UpdateUserDto,
} from '../auth/dto/auth.dto';
import { RolesGuard, Roles } from '../common/guards/roles.guard';
import { UserRole } from '../common/enums';

@UseGuards(AuthGuard('jwt'), RolesGuard)
@Controller('users')
export class UsersController {
  constructor(private usersService: UsersService) {}

  @Get()
  @Roles(UserRole.ADMIN)
  findAll() {
    return this.usersService.findAll();
  }

  @Get('mobile/contacts')
  @Roles(UserRole.DIRECTOR, UserRole.EMPLOYEE)
  findContacts() {
    return this.usersService.findEmployeesAndDirectors();
  }

  @Get('mobile/departments')
  @Roles(UserRole.DIRECTOR, UserRole.EMPLOYEE)
  getMobileDepartments() {
    return this.usersService.getMobileDepartments();
  }

  @Post('mobile/avatar')
  @Roles(UserRole.DIRECTOR, UserRole.EMPLOYEE, UserRole.ADMIN)
  @UseInterceptors(
    FileInterceptor('file', {
      limits: { fileSize: 10 * 1024 * 1024 },
      storage: diskStorage({
        destination: process.env.UPLOAD_DIR || 'uploads',
        filename: (_r, f, cb) => cb(null, `${uuid()}${extname(f.originalname)}`),
      }),
    }),
  )
  uploadAvatar(
    @UploadedFile() file: Express.Multer.File,
    @Request() req: { user: { id: string } },
  ) {
    return this.usersService.setAvatar(req.user.id, file.path);
  }

  @Delete('mobile/avatar')
  @Roles(UserRole.DIRECTOR, UserRole.EMPLOYEE, UserRole.ADMIN)
  deleteAvatar(@Request() req: { user: { id: string } }) {
    return this.usersService.setAvatar(req.user.id, null);
  }

  @Get('options/positions')
  @Roles(UserRole.ADMIN)
  getPositionOptions(@Query('q') q?: string) {
    return this.usersService.getFieldOptions('position', q);
  }

  @Get('options/departments')
  @Roles(UserRole.ADMIN)
  getDepartmentOptions(@Query('q') q?: string) {
    return this.usersService.getFieldOptions('department', q);
  }

  @Post()
  @Roles(UserRole.ADMIN)
  create(@Body() dto: CreateUserDto, @Request() req: { user: { id: string } }) {
    return this.usersService.create(dto, req.user.id);
  }

  @Patch(':id')
  @Roles(UserRole.ADMIN)
  update(
    @Param('id') id: string,
    @Body() dto: UpdateUserDto,
    @Request() req: { user: { id: string } },
  ) {
    return this.usersService.update(id, dto, req.user.id);
  }

  @Post(':id/reset-password')
  @Roles(UserRole.ADMIN)
  resetPassword(
    @Param('id') id: string,
    @Body() dto: ResetPasswordDto,
    @Request() req: { user: { id: string } },
  ) {
    return this.usersService.resetPassword(id, dto.newPassword, req.user.id);
  }

  @Post(':id/reset-device')
  @Roles(UserRole.ADMIN)
  resetDevice(@Param('id') id: string, @Request() req: { user: { id: string } }) {
    return this.usersService.resetDevice(id, req.user.id);
  }

  @Post(':id/approve-device')
  @Roles(UserRole.ADMIN)
  approveDevice(
    @Param('id') id: string,
    @Body() dto: ApproveDeviceDto,
    @Request() req: { user: { id: string } },
  ) {
    return this.usersService.approveDevice(id, dto.approve, req.user.id);
  }

  @Delete(':id')
  @Roles(UserRole.ADMIN)
  delete(@Param('id') id: string, @Request() req: { user: { id: string } }) {
    return this.usersService.delete(id, req.user.id);
  }
}
