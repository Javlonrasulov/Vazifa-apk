const pairs = [["o'", "ў"], ["g'", "ғ"], ["sh", "ш"], ["ch", "ч"]];
const map = {
  A: "А", B: "Б", C: "С", D: "Д", E: "Е", F: "Ф", G: "Г", H: "Ҳ", I: "И", J: "Ж", K: "К", L: "Л", M: "М", N: "Н", O: "О", P: "П", Q: "Қ", R: "Р", S: "С", T: "Т", U: "У", V: "В", X: "Х", Y: "Й", Z: "З",
  a: "а", b: "б", c: "с", d: "д", e: "е", f: "ф", g: "г", h: "ҳ", i: "и", j: "ж", k: "к", l: "л", m: "м", n: "н", o: "о", p: "п", q: "қ", r: "р", s: "с", t: "т", u: "у", v: "в", x: "х", y: "й", z: "з",
};

function conv(s) {
  for (const [a, b] of pairs) s = s.split(a).join(b);
  return [...s].map((c) => map[c] ?? c).join("");
}

const uz = {
  app_subtitle: "Xodimlar vazifalar tizimi",
  login_error: "Login yoki parol noto'g'ri",
  show_password: "Parolni ko'rsatish",
  hide_password: "Yashirish",
  login_device_pending: "Qurilma admin tasdig'ini kutmoqda",
  login_network_error: "Ulanish xatoligi",
  notif_title: "Ilovadan foydalanish uchun bildirishnomalarni yoqing",
  notif_desc: "Vazifalar va muddat eslatmalari push orqali yuboriladi. Ruxsatsiz ilova ishlamaydi.",
  notif_open_settings: "Sozlamalarni ochish",
  notif_check: "Tekshirish",
  nav_home: "Asosiy",
  nav_tasks: "Vazifalar",
  nav_profile: "Profil",
  dash_title: "Asosiy",
  dash_employees: "Xodimlar",
  dash_active: "Faol",
  dash_completed: "Bajarilgan",
  dash_overdue: "Kechikkan",
  dash_recent_tasks: "So'nggi vazifalar",
  tasks_title: "Vazifalarim",
  task_detail: "Vazifa",
  task_deadline: "Muddat",
  task_create: "Yangi vazifa",
  task_name: "Nomi",
  task_desc: "Tavsif",
  task_deadline_hours: "Muddat (soat)",
  task_assignees: "Mas'ul xodimlar",
  task_create_btn: "Yaratish",
  profile_title: "Profil",
  profile_theme: "Tema o'zgartirish",
  profile_language: "Til",
  profile_logout: "Chiqish",
  com_settings: "Sozlamalar",
  com_theme_dark: "Qorong'u rejim",
  com_theme_light: "Yorug' rejim",
  com_theme_system: "Tizim bo'yicha",
  role_director: "Direktor",
  role_employee: "Xodim",
  status_new: "Yangi",
  status_accepted: "Qabul qilindi",
  status_in_progress: "Jarayonda",
  status_in_review: "Tekshiruvda",
  status_completed: "Bajarildi",
  status_rework: "Qayta ishlash",
  status_cancelled: "Bekor qilindi",
  com_cancel: "Bekor qilish",
  com_back: "Orqaga",
  com_save: "Saqlash",
  com_loading: "Yuklanmoqda...",
};

for (const [k, v] of Object.entries(uz)) {
  console.log(`"${k}" to "${conv(v)}",`);
}
