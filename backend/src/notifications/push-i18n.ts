import { TaskStatus } from '../common/enums';

export type PushLang = 'uz' | 'uz_kril' | 'ru';

export const DEFAULT_PUSH_LANG: PushLang = 'uz_kril';

export function normalizeLang(code?: string | null): PushLang {
  switch (code) {
    case 'uz':
    case 'uz_latn':
      return 'uz';
    case 'ru':
      return 'ru';
    case 'uz_kril':
    case 'uz_cyr':
    case 'uz_cyrl':
    case 'cy':
      return 'uz_kril';
    default:
      return DEFAULT_PUSH_LANG;
  }
}

export interface PushText {
  title: string;
  body: string;
}

const STATUS_LABELS: Record<PushLang, Record<TaskStatus, string>> = {
  uz: {
    [TaskStatus.NEW]: 'yangi',
    [TaskStatus.ACCEPTED]: 'qabul qilindi',
    [TaskStatus.IN_PROGRESS]: 'jarayonda',
    [TaskStatus.IN_REVIEW]: 'tekshiruvda',
    [TaskStatus.COMPLETED]: 'bajarildi',
    [TaskStatus.REWORK]: 'qayta ishlash',
    [TaskStatus.CANCELLED]: 'bekor qilindi',
  },
  uz_kril: {
    [TaskStatus.NEW]: 'янги',
    [TaskStatus.ACCEPTED]: 'қабул қилинди',
    [TaskStatus.IN_PROGRESS]: 'жараёнда',
    [TaskStatus.IN_REVIEW]: 'текширувда',
    [TaskStatus.COMPLETED]: 'бажарилди',
    [TaskStatus.REWORK]: 'қайта ишлаш',
    [TaskStatus.CANCELLED]: 'бекор қилинди',
  },
  ru: {
    [TaskStatus.NEW]: 'новая',
    [TaskStatus.ACCEPTED]: 'принята',
    [TaskStatus.IN_PROGRESS]: 'в процессе',
    [TaskStatus.IN_REVIEW]: 'на проверке',
    [TaskStatus.COMPLETED]: 'выполнена',
    [TaskStatus.REWORK]: 'на доработку',
    [TaskStatus.CANCELLED]: 'отменена',
  },
};

export function statusLabel(status: TaskStatus, code?: string | null): string {
  const lang = normalizeLang(code);
  return STATUS_LABELS[lang][status] ?? status;
}

export function newTaskText(title: string, code?: string | null): PushText {
  const lang = normalizeLang(code);
  switch (lang) {
    case 'uz':
      return { title: 'Yangi vazifa', body: `"${title}" — sizga topshirildi` };
    case 'ru':
      return { title: 'Новая задача', body: `"${title}" — назначена вам` };
    default:
      return { title: 'Янги вазифа', body: `"${title}" — сизга топширилди` };
  }
}

export function taskCompletedText(
  employeeName: string,
  title: string,
  code?: string | null,
): PushText {
  const lang = normalizeLang(code);
  switch (lang) {
    case 'uz':
      return {
        title: 'Vazifa bajarildi',
        body: `${employeeName} "${title}" vazifasini bajardi`,
      };
    case 'ru':
      return {
        title: 'Задача выполнена',
        body: `${employeeName} выполнил задачу "${title}"`,
      };
    default:
      return {
        title: 'Вазифа бажарилди',
        body: `${employeeName} "${title}" вазифасини бажарди`,
      };
  }
}

export function taskStatusText(
  employeeName: string,
  title: string,
  status: TaskStatus,
  code?: string | null,
): PushText {
  const lang = normalizeLang(code);
  const label = statusLabel(status, lang);
  switch (lang) {
    case 'uz':
      return {
        title: 'Vazifa holati yangilandi',
        body: `${employeeName}: "${title}" — ${label}`,
      };
    case 'ru':
      return {
        title: 'Статус задачи изменён',
        body: `${employeeName}: "${title}" — ${label}`,
      };
    default:
      return {
        title: 'Вазифа ҳолати янгиланди',
        body: `${employeeName}: "${title}" — ${label}`,
      };
  }
}

export function deadlineWarningText(
  title: string,
  hours: number,
  code?: string | null,
): PushText {
  const lang = normalizeLang(code);
  switch (lang) {
    case 'uz':
      return {
        title: 'Vazifa muddati yaqinlashmoqda',
        body: `"${title}" — ${hours} soat qoldi`,
      };
    case 'ru':
      return {
        title: 'Приближается срок задачи',
        body: `"${title}" — осталось ${hours} ч`,
      };
    default:
      return {
        title: 'Вазифа муддати яқинлашмоқда',
        body: `"${title}" — ${hours} соат қолди`,
      };
  }
}

export function hourlyReminderText(title: string, code?: string | null): PushText {
  const lang = normalizeLang(code);
  switch (lang) {
    case 'uz':
      return { title: 'Vazifa eslatmasi', body: `"${title}" hali bajarilmagan` };
    case 'ru':
      return { title: 'Напоминание о задаче', body: `"${title}" ещё не выполнена` };
    default:
      return { title: 'Вазифа эслатмаси', body: `"${title}" ҳали бажарилмаган` };
  }
}

export function overdueText(title: string, code?: string | null): PushText {
  const lang = normalizeLang(code);
  switch (lang) {
    case 'uz':
      return { title: 'Muddat o\'tdi!', body: `"${title}" — kechikmoqda` };
    case 'ru':
      return { title: 'Срок истёк!', body: `"${title}" — просрочена` };
    default:
      return { title: 'Муддат ўтди!', body: `"${title}" — кечикмоқда` };
  }
}
