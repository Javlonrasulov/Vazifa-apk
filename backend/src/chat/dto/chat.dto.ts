import {
  IsArray,
  IsBoolean,
  IsEnum,
  IsInt,
  IsNumber,
  IsObject,
  IsOptional,
  IsString,
  IsUUID,
  Max,
  Min,
} from 'class-validator';
import { Type } from 'class-transformer';
import { ChatMessageMeta, ChatMessageType } from '../entities/chat-message.entity';

export class SendMessageDto {
  @IsUUID()
  receiverId: string;

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

export class EditMessageDto {
  @IsString()
  body: string;
}

export class ReactDto {
  @IsOptional()
  @IsString()
  emoji?: string | null;
}

export class MarkReadDto {
  @IsUUID()
  peerId: string;

  @IsOptional()
  @IsArray()
  @IsUUID('all', { each: true })
  messageIds?: string[];
}

export class TypingDto {
  @IsUUID()
  receiverId: string;

  @IsBoolean()
  typing: boolean;

  @IsOptional()
  @IsString()
  action?: string;
}

export class HistoryQueryDto {
  @IsOptional()
  @IsString()
  before?: string;

  @IsOptional()
  @Type(() => Number)
  @IsInt()
  @Min(1)
  @Max(100)
  limit?: number;
}

export class SetContactAliasDto {
  @IsOptional()
  @IsString()
  alias?: string | null;
}
