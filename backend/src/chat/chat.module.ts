import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { ChatContactAlias } from './entities/chat-contact-alias.entity';
import { ChatMessage } from './entities/chat-message.entity';
import { ChatRoom, ChatRoomMember } from './entities/chat-room.entity';
import { ChatRoomMessage } from './entities/chat-room-message.entity';
import { ChatService } from './chat.service';
import { RoomsService } from './rooms.service';
import { ChatController } from './chat.controller';
import { RoomsController } from './rooms.controller';
import { ChatGateway } from './chat.gateway';
import { NotificationsModule } from '../notifications/notifications.module';
import { UsersModule } from '../users/users.module';
import { AuthModule } from '../auth/auth.module';
import { User } from '../users/entities/user.entity';

@Module({
  imports: [
    TypeOrmModule.forFeature([
      ChatMessage,
      ChatContactAlias,
      ChatRoom,
      ChatRoomMember,
      ChatRoomMessage,
      User,
    ]),
    NotificationsModule,
    UsersModule,
    AuthModule,
  ],
  providers: [ChatService, RoomsService, ChatGateway],
  controllers: [ChatController, RoomsController],
})
export class ChatModule {}
