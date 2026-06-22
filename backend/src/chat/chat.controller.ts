import {
  Body,
  Controller,
  Get,
  Param,
  Post,
  UseGuards,
  Request,
  UseInterceptors,
  UploadedFile,
} from '@nestjs/common';
import { FileInterceptor } from '@nestjs/platform-express';
import { AuthGuard } from '@nestjs/passport';
import { diskStorage } from 'multer';
import { extname } from 'path';
import { v4 as uuid } from 'uuid';
import { ChatService } from './chat.service';
import { SendChatMessageDto } from '../tasks/dto/task.dto';
import { RolesGuard, Roles } from '../common/guards/roles.guard';
import { NotificationsGuard } from '../common/guards/notifications.guard';
import { UserRole } from '../common/enums';
import { User } from '../users/entities/user.entity';

@UseGuards(AuthGuard('jwt'), NotificationsGuard, RolesGuard)
@Roles(UserRole.DIRECTOR, UserRole.EMPLOYEE)
@Controller('chat')
export class ChatController {
  constructor(private chatService: ChatService) {}

  @Get(':userId')
  getConversation(@Param('userId') userId: string, @Request() req: { user: User }) {
    return this.chatService.getConversation(req.user.id, userId);
  }

  @Post('send')
  @UseInterceptors(
    FileInterceptor('file', {
      storage: diskStorage({
        destination: process.env.UPLOAD_DIR || 'uploads',
        filename: (_r, f, cb) => cb(null, `${uuid()}${extname(f.originalname)}`),
      }),
    }),
  )
  send(
    @Body() dto: SendChatMessageDto,
    @UploadedFile() file: Express.Multer.File,
    @Request() req: { user: User },
  ) {
    return this.chatService.send(req.user, dto, file);
  }
}
