import {
  ArrayMinSize,
  IsArray,
  IsDateString,
  IsInt,
  IsOptional,
  IsString,
  IsUUID,
  Max,
  Min,
  MinLength,
} from 'class-validator';

export class CreateAnnouncementDto {
  @IsString()
  @MinLength(1)
  title: string;

  @IsOptional()
  @IsString()
  description?: string;

  @IsDateString()
  deadlineAt: string;

  @IsInt()
  @Min(1)
  @Max(10080)
  reminderIntervalMinutes: number;

  @IsArray()
  @ArrayMinSize(1)
  @IsUUID('4', { each: true })
  recipientIds: string[];
}

export class UpdateAnnouncementDto {
  @IsOptional()
  @IsString()
  @MinLength(1)
  title?: string;

  @IsOptional()
  @IsString()
  description?: string;

  @IsOptional()
  @IsDateString()
  deadlineAt?: string;

  @IsOptional()
  @IsInt()
  @Min(1)
  @Max(10080)
  reminderIntervalMinutes?: number;
}
