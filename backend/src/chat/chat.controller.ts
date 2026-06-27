import {
  Body,
  Controller,
  Delete,
  Get,
  Param,
  Patch,
  Post,
  Query,
  Request,
  UploadedFile,
  UseGuards,
  UseInterceptors,
} from '@nestjs/common';
import { FileInterceptor } from '@nestjs/platform-express';
import { AuthGuard } from '@nestjs/passport';
import { diskStorage } from 'multer';
import { extname } from 'path';
import { v4 as uuid } from 'uuid';
import { ChatService } from './chat.service';
import { ChatGateway } from './chat.gateway';
import {
  EditMessageDto,
  HistoryQueryDto,
  MarkReadDto,
  ReactDto,
  SendMessageDto,
} from './dto/chat.dto';
import { RolesGuard, Roles } from '../common/guards/roles.guard';
import { NotificationsGuard } from '../common/guards/notifications.guard';
import { UserRole } from '../common/enums';
import { User } from '../users/entities/user.entity';
import { mediaUrl } from '../common/utils/media-url';

@UseGuards(AuthGuard('jwt'), NotificationsGuard, RolesGuard)
@Roles(UserRole.DIRECTOR, UserRole.EMPLOYEE)
@Controller('chat')
export class ChatController {
  constructor(
    private chatService: ChatService,
    private gateway: ChatGateway,
  ) {}

  @Get('conversations')
  getConversations(@Request() req: { user: User }) {
    return this.chatService.getConversations(req.user.id);
  }

  @Get('unread-count')
  async unreadCount(@Request() req: { user: User }) {
    return { count: await this.chatService.getUnreadCount(req.user.id) };
  }

  @Get('search')
  search(@Query('q') q: string, @Request() req: { user: User }) {
    return this.chatService.search(req.user.id, q ?? '');
  }

  @Post('upload')
  @UseInterceptors(
    FileInterceptor('file', {
      limits: { fileSize: 100 * 1024 * 1024 },
      storage: diskStorage({
        destination: process.env.UPLOAD_DIR || 'uploads',
        filename: (_r, f, cb) => cb(null, `${uuid()}${extname(f.originalname)}`),
      }),
    }),
  )
  upload(@UploadedFile() file: Express.Multer.File) {
    return {
      filePath: file.path.replace(/\\/g, '/'),
      fileName: file.originalname,
      mimeType: file.mimetype,
      fileSize: file.size,
      fileUrl: mediaUrl(file.path),
    };
  }

  @Post('send')
  async send(@Body() dto: SendMessageDto, @Request() req: { user: User }) {
    const online = this.gateway.userOnline(dto.receiverId);
    const message = await this.chatService.createMessage(req.user, dto, online);
    if (message) await this.gateway.relayMessage(req.user, message);
    return message;
  }

  @Post('read')
  async read(@Body() dto: MarkReadDto, @Request() req: { user: User }) {
    const res = await this.chatService.markRead(req.user.id, dto.peerId, dto.messageIds);
    this.gateway.relayRead(req.user.id, dto.peerId, dto.messageIds);
    return res;
  }

  @Patch(':id')
  async edit(
    @Param('id') id: string,
    @Body() dto: EditMessageDto,
    @Request() req: { user: User },
  ) {
    const msg = await this.chatService.editMessage(req.user.id, id, dto.body);
    if (msg) this.gateway.relayUpdated(msg);
    return msg;
  }

  @Delete(':id')
  async remove(@Param('id') id: string, @Request() req: { user: User }) {
    const res = await this.chatService.deleteMessage(req.user.id, id);
    this.gateway.relayDeleted(res.senderId, res.receiverId, res.id);
    return res;
  }

  @Post(':id/react')
  async react(
    @Param('id') id: string,
    @Body() dto: ReactDto,
    @Request() req: { user: User },
  ) {
    const msg = await this.chatService.react(req.user.id, id, dto.emoji);
    if (msg) this.gateway.relayUpdated(msg);
    return msg;
  }

  @Post(':id/pin')
  async pin(@Param('id') id: string, @Request() req: { user: User }) {
    const msg = await this.chatService.pin(req.user.id, id);
    if (msg) this.gateway.relayUpdated(msg);
    return msg;
  }

  @Get(':userId')
  getConversation(
    @Param('userId') userId: string,
    @Query() query: HistoryQueryDto,
    @Request() req: { user: User },
  ) {
    return this.chatService.getHistory(
      req.user.id,
      userId,
      query.before,
      query.limit ? Number(query.limit) : 40,
    );
  }
}
