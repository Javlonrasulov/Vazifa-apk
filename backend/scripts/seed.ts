import 'reflect-metadata';
import { DataSource } from 'typeorm';
import * as bcrypt from 'bcrypt';
import { User } from '../src/users/entities/user.entity';
import { Task } from '../src/tasks/entities/task.entity';
import { TaskAssignment } from '../src/tasks/entities/task-assignment.entity';
import { TaskAttachment } from '../src/tasks/entities/task-attachment.entity';
import { TaskComment } from '../src/tasks/entities/task-comment.entity';
import { AuditLog } from '../src/audit/entities/audit-log.entity';
import { ChatMessage } from '../src/chat/entities/chat-message.entity';
import { UserFieldOption } from '../src/users/entities/user-field-option.entity';
import { Department } from '../src/departments/entities/department.entity';
import { UserRole } from '../src/common/enums';

async function seed() {
  const ds = new DataSource({
    type: 'postgres',
    host: process.env.DB_HOST || 'localhost',
    port: +(process.env.DB_PORT || 5432),
    username: process.env.DB_USERNAME || 'vazifa',
    password: process.env.DB_PASSWORD || 'vazifa123',
    database: process.env.DB_DATABASE || 'vazifa',
    entities: [User, Task, TaskAssignment, TaskAttachment, TaskComment, AuditLog, ChatMessage, UserFieldOption, Department],
    synchronize: true,
  });

  await ds.initialize();
  const repo = ds.getRepository(User);

  const admin = await repo.findOne({ where: { login: 'admin' } });
  if (!admin) {
    await repo.save(
      repo.create({
        login: 'admin',
        passwordHash: await bcrypt.hash('admin123', 10),
        passwordPlain: 'admin123',
        fullName: 'Administrator',
        role: UserRole.ADMIN,
        department: 'IT',
        position: 'Admin',
        adminPermissions: ['employees', 'system_users'],
      }),
    );
    console.log('Admin: admin / admin123');
  }

  const director = await repo.findOne({ where: { login: 'director1' } });
  if (!director) {
    await repo.save(
      repo.create({
        login: 'director1',
        passwordHash: await bcrypt.hash('director123', 10),
        passwordPlain: 'director123',
        fullName: 'Direktor Namuna',
        role: UserRole.DIRECTOR,
        canAssignTasks: true,
        department: 'Boshqaruv',
        position: 'Direktor',
      }),
    );
    console.log('Director: director1 / director123');
  }

  const employee = await repo.findOne({ where: { login: 'xodim1' } });
  if (employee) {
    await repo.delete({ login: 'xodim1' });
    console.log('Namuna xodim (xodim1) olib tashlandi');
  }

  await repo.update(
    { role: UserRole.DIRECTOR },
    { canAssignTasks: true },
  );
  await repo.update(
    { role: UserRole.EMPLOYEE },
    { canAssignTasks: true },
  );

  await ds.destroy();
  console.log('Seed completed');
}

seed().catch(console.error);
