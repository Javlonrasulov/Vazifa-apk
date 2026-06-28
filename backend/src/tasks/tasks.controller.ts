import {
  Body,
  Controller,
  Get,
  NotFoundException,
  Param,
  Patch,
  Post,
  Delete,
  StreamableFile,
  UseGuards,
  Request,
  UseInterceptors,
  UploadedFile,
} from '@nestjs/common';
import { createReadStream, existsSync } from 'fs';
import { FileInterceptor } from '@nestjs/platform-express';
import { AuthGuard } from '@nestjs/passport';
import { diskStorage } from 'multer';
import { extname, isAbsolute, join } from 'path';
import { v4 as uuid } from 'uuid';
import { TasksService } from './tasks.service';
import {
  AddCommentDto,
  CreateTaskDto,
  UpdateAssignmentStatusDto,
  UpdateTaskDto,
} from './dto/task.dto';
import { RolesGuard, Roles } from '../common/guards/roles.guard';
import { NotificationsGuard } from '../common/guards/notifications.guard';
import { UserRole } from '../common/enums';
import { User } from '../users/entities/user.entity';
import { AuditService } from '../audit/audit.service';

const uploadDir = process.env.UPLOAD_DIR || 'uploads';

@UseGuards(AuthGuard('jwt'), NotificationsGuard)
@Controller('tasks')
export class TasksController {
  constructor(
    private tasksService: TasksService,
    private audit: AuditService,
  ) {}

  @Get()
  @Roles(UserRole.DIRECTOR, UserRole.EMPLOYEE)
  @UseGuards(RolesGuard)
  findAll(@Request() req: { user: User }) {
    return this.tasksService.findAll(req.user);
  }

  @Get('department')
  @Roles(UserRole.DIRECTOR, UserRole.EMPLOYEE)
  @UseGuards(RolesGuard)
  findDepartmentTasks(@Request() req: { user: User }) {
    return this.tasksService.findDepartmentTasks(req.user);
  }

  @Get('dashboard/stats')
  @Roles(UserRole.DIRECTOR, UserRole.EMPLOYEE)
  @UseGuards(RolesGuard)
  dashboardStats(@Request() req: { user: User }) {
    return this.tasksService.getDashboardStats(req.user);
  }

  @Get('attachments/:attachmentId/file')
  @Roles(UserRole.DIRECTOR, UserRole.EMPLOYEE)
  @UseGuards(RolesGuard)
  async downloadAttachment(
    @Param('attachmentId') attachmentId: string,
    @Request() req: { user: User },
  ) {
    const att = await this.tasksService.getAttachmentFile(attachmentId, req.user);
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
    return this.tasksService.findOne(id, req.user);
  }

  @Post()
  @Roles(UserRole.DIRECTOR, UserRole.EMPLOYEE)
  @UseGuards(RolesGuard)
  create(@Body() dto: CreateTaskDto, @Request() req: { user: User }) {
    return this.tasksService.create(dto, req.user);
  }

  @Patch(':id')
  @Roles(UserRole.DIRECTOR, UserRole.EMPLOYEE)
  @UseGuards(RolesGuard)
  update(
    @Param('id') id: string,
    @Body() dto: UpdateTaskDto,
    @Request() req: { user: User },
  ) {
    return this.tasksService.update(id, dto, req.user);
  }

  @Delete(':id')
  @Roles(UserRole.DIRECTOR, UserRole.EMPLOYEE)
  @UseGuards(RolesGuard)
  cancel(@Param('id') id: string, @Request() req: { user: User }) {
    return this.tasksService.cancel(id, req.user);
  }

  @Patch(':taskId/assignments/:assignmentId/status')
  @Roles(UserRole.DIRECTOR, UserRole.EMPLOYEE)
  @UseGuards(RolesGuard)
  updateStatus(
    @Param('taskId') taskId: string,
    @Param('assignmentId') assignmentId: string,
    @Body() dto: UpdateAssignmentStatusDto,
    @Request() req: { user: User },
  ) {
    return this.tasksService.updateAssignmentStatus(taskId, assignmentId, dto, req.user);
  }

  @Post(':id/comments')
  @Roles(UserRole.DIRECTOR, UserRole.EMPLOYEE)
  @UseGuards(RolesGuard)
  addComment(
    @Param('id') id: string,
    @Body() dto: AddCommentDto,
    @Request() req: { user: User },
  ) {
    return this.tasksService.addComment(id, dto, req.user);
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
    return this.tasksService.addAttachment(id, req.user, file);
  }
}
