import {
  IsArray,
  IsDateString,
  IsEnum,
  IsNotEmpty,
  IsOptional,
  IsString,
  IsUUID,
} from 'class-validator';
import { TaskPriority, TaskStatus } from '../../common/enums';

export class CreateTaskDto {
  @IsString()
  @IsNotEmpty()
  title: string;

  @IsOptional()
  @IsString()
  description?: string;

  @IsEnum(TaskPriority)
  priority: TaskPriority;

  @IsArray()
  @IsUUID('4', { each: true })
  assigneeIds: string[];

  @IsDateString()
  startAt: string;

  @IsDateString()
  deadlineAt: string;
}

export class UpdateTaskDto {
  @IsOptional()
  @IsString()
  title?: string;

  @IsOptional()
  @IsString()
  description?: string;

  @IsOptional()
  @IsEnum(TaskPriority)
  priority?: TaskPriority;

  @IsOptional()
  @IsDateString()
  startAt?: string;

  @IsOptional()
  @IsDateString()
  deadlineAt?: string;

  @IsOptional()
  @IsEnum(TaskStatus)
  status?: TaskStatus;
}

export class UpdateAssignmentStatusDto {
  @IsEnum(TaskStatus)
  status: TaskStatus;
}

export class AddCommentDto {
  @IsString()
  @IsNotEmpty()
  body: string;
}

export class SendChatMessageDto {
  @IsUUID()
  receiverId: string;

  @IsOptional()
  @IsString()
  body?: string;
}
