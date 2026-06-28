import {
  ArrayNotEmpty,
  IsArray,
  IsEnum,
  IsObject,
  IsOptional,
  IsString,
  IsUUID,
  MaxLength,
  MinLength,
} from 'class-validator';
import { ChatMessageMeta, ChatMessageType } from '../entities/chat-message.entity';
import { ChatRoomType } from '../entities/chat-room.entity';

export class CreateRoomDto {
  @IsEnum(ChatRoomType)
  type: ChatRoomType;

  @IsString()
  @MinLength(1)
  @MaxLength(120)
  title: string;

  @IsOptional()
  @IsString()
  @MaxLength(500)
  description?: string;

  @IsOptional()
  @IsString()
  avatarUrl?: string;

  @IsOptional()
  @IsArray()
  @IsUUID('all', { each: true })
  memberIds?: string[];
}

export class UpdateRoomDto {
  @IsOptional()
  @IsString()
  @MaxLength(120)
  title?: string;

  @IsOptional()
  @IsString()
  @MaxLength(500)
  description?: string;

  @IsOptional()
  @IsString()
  avatarUrl?: string | null;
}

export class AddMembersDto {
  @IsArray()
  @ArrayNotEmpty()
  @IsUUID('all', { each: true })
  memberIds: string[];
}

export class SendRoomMessageDto {
  @IsOptional()
  @IsEnum(ChatMessageType)
  type?: ChatMessageType;

  @IsOptional()
  @IsString()
  body?: string;

  @IsOptional()
  @IsString()
  filePath?: string;

  @IsOptional()
  @IsString()
  fileName?: string;

  @IsOptional()
  @IsString()
  mimeType?: string;

  @IsOptional()
  @IsObject()
  meta?: ChatMessageMeta;

  @IsOptional()
  @IsUUID()
  replyToId?: string;

  @IsOptional()
  @IsString()
  forwardedFrom?: string;

  @IsOptional()
  @IsString()
  clientId?: string;
}
