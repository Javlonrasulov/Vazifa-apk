import {
  Body,
  Controller,
  Get,
  NotFoundException,
  Param,
  Post,
  Request,
  StreamableFile,
  UploadedFile,
  UseGuards,
  UseInterceptors,
} from '@nestjs/common';
import { createReadStream, existsSync } from 'fs';
import { FileInterceptor } from '@nestjs/platform-express';
import { AuthGuard } from '@nestjs/passport';
import { diskStorage } from 'multer';
import { extname, isAbsolute, join } from 'path';
import { v4 as uuid } from 'uuid';
import { AnnouncementsService } from './announcements.service';
import { CreateAnnouncementDto } from './dto/announcement.dto';
import { RolesGuard, Roles } from '../common/guards/roles.guard';
import { NotificationsGuard } from '../common/guards/notifications.guard';
import { UserRole } from '../common/enums';
import { User } from '../users/entities/user.entity';

const uploadDir = process.env.UPLOAD_DIR || 'uploads';

@UseGuards(AuthGuard('jwt'), NotificationsGuard)
@Controller('announcements')
export class AnnouncementsController {
  constructor(private announcementsService: AnnouncementsService) {}

  @Post()
  @Roles(UserRole.DIRECTOR, UserRole.EMPLOYEE)
  @UseGuards(RolesGuard)
  create(@Body() dto: CreateAnnouncementDto, @Request() req: { user: User }) {
    return this.announcementsService.create(dto, req.user);
  }

  @Get('sent')
  @Roles(UserRole.DIRECTOR, UserRole.EMPLOYEE)
  @UseGuards(RolesGuard)
  findSent(@Request() req: { user: User }) {
    return this.announcementsService.findSent(req.user);
  }

  @Get('received')
  @Roles(UserRole.DIRECTOR, UserRole.EMPLOYEE)
  @UseGuards(RolesGuard)
  findReceived(@Request() req: { user: User }) {
    return this.announcementsService.findReceived(req.user);
  }

  @Get('attachments/:attachmentId/file')
  @Roles(UserRole.DIRECTOR, UserRole.EMPLOYEE)
  @UseGuards(RolesGuard)
  async downloadAttachment(
    @Param('attachmentId') attachmentId: string,
    @Request() req: { user: User },
  ) {
    const att = await this.announcementsService.getAttachmentFile(attachmentId, req.user);
    const storedPath = att.filePath.replace(/\\/g, '/');
    const fullPath = isAbsolute(storedPath) ? storedPath : join(process.cwd(), storedPath);
    if (!existsSync(fullPath)) throw new NotFoundException('Fayl topilmadi');
    return new StreamableFile(createReadStream(fullPath), {
      type: att.mimeType,
      disposition: `inline; filename="${att.fileName.replace(/"/g, '')}"`,
    });
  }

  @Get(':id')
  @Roles(UserRole.DIRECTOR, UserRole.EMPLOYEE)
  @UseGuards(RolesGuard)
  findOne(@Param('id') id: string, @Request() req: { user: User }) {
    return this.announcementsService.findOne(id, req.user);
  }

  @Post(':id/acknowledge')
  @Roles(UserRole.DIRECTOR, UserRole.EMPLOYEE)
  @UseGuards(RolesGuard)
  acknowledge(@Param('id') id: string, @Request() req: { user: User }) {
    return this.announcementsService.acknowledge(id, req.user);
  }

  @Post(':id/cancel')
  @Roles(UserRole.DIRECTOR, UserRole.EMPLOYEE)
  @UseGuards(RolesGuard)
  cancel(@Param('id') id: string, @Request() req: { user: User }) {
    return this.announcementsService.cancel(id, req.user);
  }

  @Post(':id/attachments')
  @Roles(UserRole.DIRECTOR, UserRole.EMPLOYEE)
  @UseGuards(RolesGuard)
  @UseInterceptors(
    FileInterceptor('file', {
      storage: diskStorage({
        destination: uploadDir,
        filename: (_req, file, cb) => {
          cb(null, `${uuid()}${extname(file.originalname)}`);
        },
      }),
      limits: { fileSize: 50 * 1024 * 1024 },
    }),
  )
  upload(
    @Param('id') id: string,
    @UploadedFile() file: Express.Multer.File,
    @Request() req: { user: User },
  ) {
    if (!file) throw new Error('Fayl topilmadi');
    return this.announcementsService.addAttachment(id, req.user, file);
  }
}
