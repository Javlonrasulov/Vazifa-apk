import { Injectable, Logger } from '@nestjs/common';
import { Cron, CronExpression } from '@nestjs/schedule';
import { TasksService } from '../tasks/tasks.service';
import { NotificationsService } from './notifications.service';
import { hoursUntil, nowTashkent } from '../common/utils/time';

@Injectable()
export class RemindersService {
  private readonly logger = new Logger(RemindersService.name);
  private sentDeadlines = new Set<string>();

  constructor(
    private tasksService: TasksService,
    private notifications: NotificationsService,
  ) {}

  /** Har soatda faol (bajarilmagan) vazifalar bo'yicha eslatma yuboradi. */
  @Cron(CronExpression.EVERY_HOUR)
  async hourlyReminders() {
    const assignments = await this.tasksService.getActiveAssignments();
    const now = nowTashkent();
    let sent = 0;

    for (const a of assignments) {
      if (!a.assignee?.fcmToken || !a.assignee.notificationsEnabled) continue;
      // Hali boshlanmagan vazifalar uchun eslatma yubormaymiz.
      if (a.task.startAt > now) continue;

      const h = hoursUntil(a.task.deadlineAt);

      // Muddat yaqinlashganda alohida ogohlantirish (takrorlanmaydi).
      const key = `${a.id}-${Math.round(h)}`;
      if ([24, 12, 6, 1].some((t) => Math.abs(h - t) < 0.5) && !this.sentDeadlines.has(key)) {
        this.sentDeadlines.add(key);
        await this.notifications.sendToToken(
          a.assignee.fcmToken,
          'Vazifa muddati yaqinlashmoqda',
          `"${a.task.title}" — ${Math.round(h)} soat qoldi`,
          { taskId: a.taskId, type: 'deadline_warning' },
        );
        sent++;
      }

      // Muddati o'tmagan faol vazifa uchun har soatlik eslatma.
      if (h > 0) {
        await this.notifications.sendToToken(
          a.assignee.fcmToken,
          'Vazifa eslatmasi',
          `"${a.task.title}" hali bajarilmagan`,
          { taskId: a.taskId, type: 'hourly_reminder' },
        );
        sent++;
      }
    }

    this.logger.log(
      `Soatlik eslatma: ${sent} ta yuborildi (faol vazifalar: ${assignments.length})`,
    );
  }

  /** Har 30 daqiqada muddati o'tgan vazifalar bo'yicha eslatma yuboradi. */
  @Cron('*/30 * * * *')
  async overdueReminders() {
    const overdue = await this.tasksService.getOverdueAssignments();
    let sent = 0;
    for (const a of overdue) {
      if (!a.assignee?.fcmToken || !a.assignee.notificationsEnabled) continue;
      await this.notifications.sendToToken(
        a.assignee.fcmToken,
        'Muddat o\'tdi!',
        `"${a.task.title}" — kechikmoqda`,
        { taskId: a.taskId, type: 'overdue' },
      );
      sent++;
    }
    if (overdue.length) {
      this.logger.log(`Kechikkan eslatma: ${sent} ta yuborildi (kechikkan: ${overdue.length})`);
    }
  }

  /** Takrorlanishni oldini oluvchi keshni har kuni tozalaydi. */
  @Cron(CronExpression.EVERY_DAY_AT_MIDNIGHT)
  clearDedupeCache() {
    this.sentDeadlines.clear();
  }
}
