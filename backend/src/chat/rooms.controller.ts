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
  UseGuards,
} from '@nestjs/common';
import { AuthGuard } from '@nestjs/passport';
import { RoomsService } from './rooms.service';
import { ChatGateway } from './chat.gateway';
import {
  AddMembersDto,
  CreateRoomDto,
  SendRoomMessageDto,
  UpdateRoomDto,
} from './dto/room.dto';
import { HistoryQueryDto, ReactDto, EditMessageDto } from './dto/chat.dto';
import { RolesGuard, Roles } from '../common/guards/roles.guard';
import { NotificationsGuard } from '../common/guards/notifications.guard';
import { UserRole } from '../common/enums';
import { ChatRoomMemberRole } from './entities/chat-room.entity';
import { User } from '../users/entities/user.entity';

@UseGuards(AuthGuard('jwt'), NotificationsGuard, RolesGuard)
@Roles(UserRole.DIRECTOR, UserRole.EMPLOYEE)
@Controller('rooms')
export class RoomsController {
  constructor(
    private rooms: RoomsService,
    private gateway: ChatGateway,
  ) {}

  @Get()
  list(@Request() req: { user: User }) {
    return this.rooms.listForUser(req.user.id);
  }

  @Post()
  async create(@Body() dto: CreateRoomDto, @Request() req: { user: User }) {
    const summary = await this.rooms.create(req.user, dto);
    const memberIds = await this.rooms.memberIds(summary.id);
    this.gateway.relayRoomCreated(summary.id, memberIds, summary);
    return summary;
  }

  @Get(':id')
  get(@Param('id') id: string, @Request() req: { user: User }) {
    return this.rooms.getRoom(id, req.user.id);
  }

  @Patch(':id')
  update(
    @Param('id') id: string,
    @Body() dto: UpdateRoomDto,
    @Request() req: { user: User },
  ) {
    return this.rooms.updateRoom(id, req.user.id, dto);
  }

  @Delete(':id')
  remove(@Param('id') id: string, @Request() req: { user: User }) {
    return this.rooms.deleteRoom(id, req.user.id);
  }

  @Get(':id/members')
  members(@Param('id') id: string, @Request() req: { user: User }) {
    return this.rooms.getMembers(id, req.user.id);
  }

  @Post(':id/members')
  async addMembers(
    @Param('id') id: string,
    @Body() dto: AddMembersDto,
    @Request() req: { user: User },
  ) {
    const added = await this.rooms.addMembers(id, req.user.id, dto.memberIds);
    if (added.length) this.gateway.joinRoomSockets(id, added);
    return { added };
  }

  @Delete(':id/members/:userId')
  async removeMember(
    @Param('id') id: string,
    @Param('userId') userId: string,
    @Request() req: { user: User },
  ) {
    const res = await this.rooms.removeMember(id, req.user.id, userId);
    this.gateway.leaveRoomSockets(id, userId);
    return res;
  }

  @Patch(':id/members/:userId/role')
  setRole(
    @Param('id') id: string,
    @Param('userId') userId: string,
    @Body() body: { role: ChatRoomMemberRole },
    @Request() req: { user: User },
  ) {
    return this.rooms.setMemberRole(id, req.user.id, userId, body.role);
  }

  @Get(':id/messages')
  history(
    @Param('id') id: string,
    @Query() query: HistoryQueryDto,
    @Request() req: { user: User },
  ) {
    return this.rooms.getHistory(
      id,
      req.user.id,
      query.before,
      query.limit ? Number(query.limit) : 40,
    );
  }

  @Post(':id/messages')
  async send(
    @Param('id') id: string,
    @Body() dto: SendRoomMessageDto,
    @Request() req: { user: User },
  ) {
    const message = await this.rooms.createMessage(req.user, id, dto);
    if (message) {
      const room = await this.rooms.getRoom(id, req.user.id);
      await this.gateway.relayRoomMessage(req.user, id, message, room.title);
    }
    return message;
  }

  @Post(':id/read')
  read(@Param('id') id: string, @Request() req: { user: User }) {
    return this.rooms.markRead(id, req.user.id);
  }

  @Patch('messages/:msgId')
  async edit(
    @Param('msgId') msgId: string,
    @Body() dto: EditMessageDto,
    @Request() req: { user: User },
  ) {
    const msg = await this.rooms.editMessage(req.user.id, msgId, dto.body);
    if (msg) this.gateway.relayRoomUpdated(msg.roomId, msg);
    return msg;
  }

  @Delete('messages/:msgId')
  async deleteMessage(@Param('msgId') msgId: string, @Request() req: { user: User }) {
    const res = await this.rooms.deleteMessage(req.user.id, msgId);
    this.gateway.relayRoomDeleted(res.roomId, res.id);
    return res;
  }

  @Post('messages/:msgId/react')
  async react(
    @Param('msgId') msgId: string,
    @Body() dto: ReactDto,
    @Request() req: { user: User },
  ) {
    const msg = await this.rooms.react(req.user.id, msgId, dto.emoji);
    if (msg) this.gateway.relayRoomUpdated(msg.roomId, msg);
    return msg;
  }

  @Post('messages/:msgId/pin')
  async pin(@Param('msgId') msgId: string, @Request() req: { user: User }) {
    const msg = await this.rooms.pin(req.user.id, msgId);
    if (msg) this.gateway.relayRoomUpdated(msg.roomId, msg);
    return msg;
  }
}
