import { Injectable, Logger } from '@nestjs/common';
import { Cron, CronExpression } from '@nestjs/schedule';
import { TasksService } from '../tasks/tasks.service';
import { NotificationsService } from './notifications.service';
import { hoursUntil, nowTashkent } from '../common/utils/time';
import {
  deadlineWarningText,
  hourlyReminderText,
  overdueText,
} from './push-i18n';

/** Qolgan muddatga qarab eslatma oralig'i (daqiqada). */
function reminderIntervalMinutes(hoursLeft: number): number {
  if (hoursLeft > 48) return 360; // 48 soatdan ko'p — har 6 soatda
  if (hoursLeft > 24) return 60; // 24–48 soat — har 1 soatda
  if (hoursLeft > 12) return 30; // 12–24 soat — har 30 daqiqada
  return 15; // 12 soatgacha — har 15 daqiqada
}

@Injectable()
export class RemindersService {
  private readonly logger = new Logger(RemindersService.name);
  private sentDeadlines = new Set<string>();
  private lastReminderAt = new Map<string, number>();

  constructor(
    private tasksService: TasksService,
    private notifications: NotificationsService,
  ) {}

  /** Har 15 daqiqada faol (bajarilmagan) vazifalar bo'yicha eslatma yuboradi. */
  @Cron('*/15 * * * *')
  async taskReminders() {
    const assignments = await this.tasksService.getActiveAssignments();
    const now = nowTashkent();
    let sent = 0;

    // Tugagan vazifalarning yozuvlarini xotiradan tozalash.
    const activeIds = new Set(assignments.map((a) => a.id));
    for (const id of this.lastReminderAt.keys()) {
      if (!activeIds.has(id)) this.lastReminderAt.delete(id);
    }

    for (const a of assignments) {
      if (!a.assignee?.notificationsEnabled) continue;
      // Hali boshlanmagan vazifalar uchun eslatma yubormaymiz.
      if (a.task.startAt > now) continue;

      const h = hoursUntil(a.task.deadlineAt);
      if (h <= 0) continue; // Muddati o'tganlar overdueReminders da yuritiladi.

      // Muddat yaqinlashganda alohida ogohlantirish (takrorlanmaydi).
      const key = `${a.id}-${Math.round(h)}`;
      if ([24, 12, 6, 1].some((t) => Math.abs(h - t) < 0.5) && !this.sentDeadlines.has(key)) {
        this.sentDeadlines.add(key);
        const text = deadlineWarningText(a.task.title, Math.round(h), a.assignee.language);
        await this.notifications.notifyUser(
          a.assignee.id,
          text.title,
          text.body,
          { taskId: a.taskId, type: 'deadline_warning' },
        );
        sent++;
      }

      // Qolgan muddatga mos oraliqda eslatma.
      const intervalMs = reminderIntervalMinutes(h) * 60 * 1000;
      const last = this.lastReminderAt.get(a.id) ?? 0;
      if (now.getTime() - last < intervalMs) continue;

      const text = hourlyReminderText(a.task.title, a.assignee.language);
      await this.notifications.notifyUser(
        a.assignee.id,
        text.title,
        text.body,
        { taskId: a.taskId, type: 'hourly_reminder' },
      );
      this.lastReminderAt.set(a.id, now.getTime());
      sent++;
    }

    if (sent > 0) {
      this.logger.log(
        `Vazifa eslatmasi: ${sent} ta yuborildi (faol vazifalar: ${assignments.length})`,
      );
    }
  }

  /** Har 30 daqiqada muddati o'tgan vazifalar bo'yicha eslatma yuboradi. */
  @Cron('*/30 * * * *')
  async overdueReminders() {
    const overdue = await this.tasksService.getOverdueAssignments();
    let sent = 0;
    for (const a of overdue) {
      if (!a.assignee?.notificationsEnabled) continue;
      const text = overdueText(a.task.title, a.assignee.language);
      await this.notifications.notifyUser(
        a.assignee.id,
        text.title,
        text.body,
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
