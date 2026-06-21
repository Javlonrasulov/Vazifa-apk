import { Injectable, Logger } from '@nestjs/common';
import { Cron, CronExpression } from '@nestjs/schedule';
import { TasksService } from '../tasks/tasks.service';
import { NotificationsService } from './notifications.service';
import { hoursUntil, minutesUntil } from '../common/utils/time';
import { TaskStatus } from '../common/enums';

@Injectable()
export class RemindersService {
  private readonly logger = new Logger(RemindersService.name);
  private sentDeadlines = new Set<string>();

  constructor(
    private tasksService: TasksService,
    private notifications: NotificationsService,
  ) {}

  @Cron(CronExpression.EVERY_HOUR)
  async hourlyReminders() {
    const assignments = await this.tasksService.getActiveAssignments();
    for (const a of assignments) {
      if (!a.assignee?.fcmToken || !a.assignee.notificationsEnabled) continue;
      const h = hoursUntil(a.task.deadlineAt);
      const key = `${a.id}-${Math.floor(h)}`;
      if ([24, 12, 6, 1].some((t) => Math.abs(h - t) < 0.5) && !this.sentDeadlines.has(key)) {
        this.sentDeadlines.add(key);
        await this.notifications.sendToToken(
          a.assignee.fcmToken,
          'Vazifa muddati yaqinlashmoqda',
          `"${a.task.title}" — ${Math.round(h)} soat qoldi`,
          { taskId: a.taskId, type: 'deadline_warning' },
        );
      }
      if (h > 0 && a.status !== TaskStatus.COMPLETED) {
        await this.notifications.sendToToken(
          a.assignee.fcmToken,
          'Vazifa eslatmasi',
          `"${a.task.title}" hali bajarilmagan`,
          { taskId: a.taskId, type: 'hourly_reminder' },
        );
      }
    }
  }

  @Cron('*/30 * * * *')
  async overdueReminders() {
    const overdue = await this.tasksService.getOverdueAssignments();
    for (const a of overdue) {
      if (!a.assignee?.fcmToken) continue;
      const m = minutesUntil(a.task.deadlineAt);
      await this.notifications.sendToToken(
        a.assignee.fcmToken,
        'Muddat o\'tdi!',
        `"${a.task.title}" — kechikmoqda`,
        { taskId: a.taskId, type: 'overdue' },
      );
    }
    if (overdue.length) this.logger.log(`Overdue reminders: ${overdue.length}`);
  }
}
