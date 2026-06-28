import {
  BadRequestException,
  ForbiddenException,
  Injectable,
  NotFoundException,
} from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository, In, Not, Brackets } from 'typeorm';
import { Task } from './entities/task.entity';
import { TaskAssignment } from './entities/task-assignment.entity';
import { TaskAttachment } from './entities/task-attachment.entity';
import { TaskComment } from './entities/task-comment.entity';
import { User } from '../users/entities/user.entity';
import {
  AddCommentDto,
  CreateTaskDto,
  UpdateAssignmentStatusDto,
  UpdateTaskDto,
} from './dto/task.dto';
import { TaskStatus, UserRole, AuditAction } from '../common/enums';
import { AuditService } from '../audit/audit.service';
import { NotificationsService } from '../notifications/notifications.service';
import { parseTashkent, nowTashkent } from '../common/utils/time';
import { userCanAssignTasks } from '../common/utils/user-permissions';
import { canViewTaskViaDepartment, normalizeDepartmentName, userHasDepartmentVisibility } from '../common/utils/department-visibility';
import { compressImageIfNeeded } from '../common/utils/image';

const IMAGE_MIMES = new Set(['image/jpeg', 'image/png', 'image/webp', 'image/heic', 'image/heif']);
import { UsersService } from '../users/users.service';
import {
  newTaskText,
  taskCompletedText,
  taskStatusText,
} from '../notifications/push-i18n';

@Injectable()
export class TasksService {
  constructor(
    @InjectRepository(Task) private taskRepo: Repository<Task>,
    @InjectRepository(TaskAssignment) private assignRepo: Repository<TaskAssignment>,
    @InjectRepository(TaskAttachment) private attachRepo: Repository<TaskAttachment>,
    @InjectRepository(TaskComment) private commentRepo: Repository<TaskComment>,
    private audit: AuditService,
    private notifications: NotificationsService,
    private usersService: UsersService,
  ) {}

  async create(dto: CreateTaskDto, creator: User) {
    if (!userCanAssignTasks(creator)) {
      throw new ForbiddenException('Vazifa berish huquqi yo\'q');
    }

    const rawAssigneeIds = [...new Set(dto.assigneeIds)];
    const selfIncluded = rawAssigneeIds.includes(creator.id);
    const assigneeIds = rawAssigneeIds.filter((id) => id !== creator.id);
    if (!assigneeIds.length) {
      throw new BadRequestException(
        selfIncluded
          ? 'O\'zingizga vazifa bera olmaysiz'
          : 'Kamida bitta xodim tanlang',
      );
    }

    const task = this.taskRepo.create({
      title: dto.title.trim(),
      description: dto.description ?? null,
      priority: dto.priority,
      startAt: parseTashkent(dto.startAt),
      deadlineAt: parseTashkent(dto.deadlineAt),
      createdById: creator.id,
      status: TaskStatus.NEW,
    });
    const saved = await this.taskRepo.save(task);

    const assignments = assigneeIds.map((assigneeId) =>
      this.assignRepo.create({ taskId: saved.id, assigneeId, status: TaskStatus.NEW }),
    );
    await this.assignRepo.save(assignments);

    await this.audit.log(creator.id, AuditAction.TASK_CREATED, 'task', saved.id);

    const assignees = await Promise.all(
      assigneeIds.map((id) => this.usersService.findById(id)),
    );
    await Promise.all(
      assignees
        .filter((a) => a.notificationsEnabled && a.fcmToken)
        .map((a) => {
          const text = newTaskText(saved.title, a.language);
          return this.notifications.sendToToken(
            a.fcmToken as string,
            text.title,
            text.body,
            { taskId: saved.id, type: 'task_new' },
          );
        }),
    );

    return this.findOne(saved.id, creator);
  }

  async findAll(user: User) {
    const qb = this.taskRepo
      .createQueryBuilder('task')
      .leftJoinAndSelect('task.assignments', 'assignments')
      .leftJoinAndSelect('assignments.assignee', 'assignee')
      .leftJoinAndSelect('task.createdBy', 'createdBy')
      .leftJoinAndSelect('task.attachments', 'attachments')
      .orderBy('task.createdAt', 'DESC')
      .distinct(true);

    if (!userCanAssignTasks(user)) {
      qb.innerJoin('task.assignments', 'mine', 'mine.assigneeId = :uid', { uid: user.id });
    } else {
      qb.andWhere(
        new Brackets((where) => {
          where
            .where('task.createdById = :uid', { uid: user.id })
            .orWhere('assignments.assigneeId = :uid', { uid: user.id });
        }),
      );
    }

    return qb.getMany();
  }

  async findDepartmentTasks(user: User) {
    if (!userHasDepartmentVisibility(user)) return [];

    const depts = (user.visibleDepartments ?? [])
      .map((d) => normalizeDepartmentName(d))
      .filter(Boolean);
    if (!depts.length) return [];

    const qb = this.taskRepo
      .createQueryBuilder('task')
      .leftJoinAndSelect('task.assignments', 'assignments')
      .leftJoinAndSelect('assignments.assignee', 'assignee')
      .leftJoinAndSelect('task.createdBy', 'createdBy')
      .leftJoinAndSelect('task.attachments', 'attachments')
      .orderBy('task.createdAt', 'DESC')
      .distinct(true);

    qb.andWhere(
      new Brackets((sub) => {
        sub.where('LOWER(TRIM(createdBy.department)) IN (:...depts)', { depts });
        sub.orWhere('LOWER(TRIM(assignee.department)) IN (:...depts)', { depts });
      }),
    );

    return qb.getMany();
  }

  private assertCanAccessTask(task: Task, user: User): void {
    const isAssignee = task.assignments?.some((a) => a.assigneeId === user.id);
    if (isAssignee) return;
    if (userCanAssignTasks(user) && task.createdById === user.id) return;
    if (canViewTaskViaDepartment(user, task)) return;
    throw new ForbiddenException('Ruxsat yo\'q');
  }

  async findOne(id: string, user: User) {
    const task = await this.taskRepo.findOne({
      where: { id },
      relations: [
        'assignments',
        'assignments.assignee',
        'createdBy',
        'attachments',
        'attachments.uploadedBy',
        'comments',
        'comments.author',
      ],
    });
    if (!task) throw new NotFoundException('Vazifa topilmadi');

    this.assertCanAccessTask(task, user);
    return task;
  }

  async update(id: string, dto: UpdateTaskDto, user: User) {
    if (!userCanAssignTasks(user)) throw new ForbiddenException();
    const task = await this.findOne(id, user);
    const hasCompleted = task.assignments.some(
      (a) => a.status === TaskStatus.COMPLETED,
    );
    if (hasCompleted) {
      throw new BadRequestException('Bajarilgan vazifani o\'zgartirish mumkin emas');
    }
    if (dto.title) task.title = dto.title.trim();
    if (dto.description !== undefined) task.description = dto.description;
    if (dto.priority) task.priority = dto.priority;
    if (dto.startAt) task.startAt = parseTashkent(dto.startAt);
    if (dto.deadlineAt) task.deadlineAt = parseTashkent(dto.deadlineAt);
    if (dto.status) task.status = dto.status;
    const saved = await this.taskRepo.save(task);
    await this.audit.log(user.id, AuditAction.TASK_UPDATED, 'task', id);
    return saved;
  }

  async cancel(id: string, user: User) {
    return this.update(id, { status: TaskStatus.CANCELLED }, user);
  }

  async updateAssignmentStatus(
    taskId: string,
    assignmentId: string,
    dto: UpdateAssignmentStatusDto,
    user: User,
  ) {
    const assignment = await this.assignRepo.findOne({
      where: { id: assignmentId, taskId },
      relations: ['task', 'assignee'],
    });
    if (!assignment) throw new NotFoundException();

    const isAssignee = assignment.assigneeId === user.id;
    const isTaskManager = userCanAssignTasks(user);
    if (!isAssignee && !isTaskManager) throw new ForbiddenException();
    if (isTaskManager && assignment.task.createdById !== user.id) {
      throw new ForbiddenException('Ruxsat yo\'q');
    }

    assignment.status = dto.status;
    if (dto.status === TaskStatus.ACCEPTED) assignment.acceptedAt = nowTashkent();
    if (dto.status === TaskStatus.COMPLETED) assignment.completedAt = nowTashkent();
    await this.assignRepo.save(assignment);

    const action =
      dto.status === TaskStatus.COMPLETED
        ? AuditAction.TASK_COMPLETED
        : dto.status === TaskStatus.ACCEPTED
          ? AuditAction.TASK_ACCEPTED
          : AuditAction.TASK_UPDATED;
    await this.audit.log(user.id, action, 'task', taskId, { status: dto.status });

    if (isAssignee) {
      const creator = await this.usersService.findById(assignment.task.createdById);
      if (creator.notificationsEnabled && creator.fcmToken) {
        const assigneeName = assignment.assignee?.fullName ?? 'Xodim';
        const text =
          dto.status === TaskStatus.COMPLETED
            ? taskCompletedText(assigneeName, assignment.task.title, creator.language)
            : taskStatusText(
                assigneeName,
                assignment.task.title,
                dto.status,
                creator.language,
              );
        await this.notifications.sendToToken(
          creator.fcmToken,
          text.title,
          text.body,
          { taskId, type: 'task_status', status: dto.status },
        );
      }
    }

    return assignment;
  }

  async addComment(taskId: string, dto: AddCommentDto, user: User) {
    await this.findOne(taskId, user);
    const comment = this.commentRepo.create({
      taskId,
      authorId: user.id,
      body: dto.body.trim(),
    });
    const saved = await this.commentRepo.save(comment);
    await this.audit.log(user.id, AuditAction.COMMENT_ADDED, 'task', taskId);
    return saved;
  }

  async addAttachment(
    taskId: string,
    user: User,
    file: Express.Multer.File,
  ) {
    await this.findOne(taskId, user);
    const compressed = await compressImageIfNeeded(file.path, file.mimetype);
    const isImage = IMAGE_MIMES.has(compressed.mimeType);
    const att = this.attachRepo.create({
      taskId,
      uploadedById: user.id,
      fileName: isImage
        ? (file.originalname || 'photo').replace(/\.[^.]+$/, '.jpg')
        : (file.originalname || 'file'),
      filePath: compressed.filePath,
      mimeType: compressed.mimeType,
      fileSize: compressed.fileSize,
    });
    const saved = await this.attachRepo.save(att);
    await this.audit.log(user.id, AuditAction.FILE_UPLOADED, 'task', taskId);
    return this.attachRepo.findOne({ where: { id: saved.id } });
  }

  async getAttachmentFile(attachmentId: string, user: User) {
    const att = await this.attachRepo.findOne({ where: { id: attachmentId } });
    if (!att) throw new NotFoundException('Fayl topilmadi');
    await this.findOne(att.taskId, user);
    return att;
  }

  async getOverdueAssignments() {
    const now = nowTashkent();
    return this.assignRepo.find({
      where: {
        status: Not(In([TaskStatus.COMPLETED, TaskStatus.CANCELLED])),
      },
      relations: ['task', 'assignee'],
    }).then((items) =>
      items.filter((a) => a.task.deadlineAt < now),
    );
  }

  async getActiveAssignments() {
    return this.assignRepo.find({
      where: {
        status: Not(In([TaskStatus.COMPLETED, TaskStatus.CANCELLED])),
      },
      relations: ['task', 'assignee'],
    });
  }

  async getDashboardStats(user: User) {
    const now = nowTashkent();
    const startOfDay = new Date(now);
    startOfDay.setHours(0, 0, 0, 0);
    const ownedOnly = userCanAssignTasks(user);

    const assignmentQb = () => {
      const qb = this.assignRepo.createQueryBuilder('a');
      if (ownedOnly) {
        qb.innerJoin('a.task', 't', 't.createdById = :uid', { uid: user.id });
      } else {
        qb.where('a.assigneeId = :uid', { uid: user.id });
      }
      return qb;
    };

    const [totalEmployees, activeTasks, completedTasks, overdueTasks, todayTasks] =
      await Promise.all([
        this.usersService.findEmployeesAndDirectors().then(
          (u) => u.filter((x) => x.login !== 'xodim1').length,
        ),
        assignmentQb()
          .andWhere('a.status NOT IN (:...statuses)', {
            statuses: [TaskStatus.COMPLETED, TaskStatus.CANCELLED],
          })
          .getCount(),
        assignmentQb()
          .andWhere('a.status = :status', { status: TaskStatus.COMPLETED })
          .getCount(),
        this.getOverdueAssignments()
          .then((items) =>
            ownedOnly
              ? items.filter((a) => a.task.createdById === user.id)
              : items.filter((a) => a.assigneeId === user.id),
          )
          .then((a) => a.length),
        (ownedOnly
          ? this.taskRepo
              .createQueryBuilder('t')
              .where('t.createdById = :uid', { uid: user.id })
          : this.taskRepo
              .createQueryBuilder('t')
              .innerJoin('t.assignments', 'a', 'a.assigneeId = :uid', { uid: user.id })
        )
          .andWhere('t.deadlineAt >= :start', { start: startOfDay })
          .andWhere('t.deadlineAt <= :end', { end: now })
          .getCount(),
      ]);

    return { totalEmployees, activeTasks, completedTasks, overdueTasks, todayTasks };
  }
}
