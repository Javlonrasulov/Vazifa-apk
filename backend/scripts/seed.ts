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
import { UserRole } from '../src/common/enums';

async function seed() {
  const ds = new DataSource({
    type: 'postgres',
    host: process.env.DB_HOST || 'localhost',
    port: +(process.env.DB_PORT || 5432),
    username: process.env.DB_USERNAME || 'vazifa',
    password: process.env.DB_PASSWORD || 'vazifa123',
    database: process.env.DB_DATABASE || 'vazifa',
    entities: [User, Task, TaskAssignment, TaskAttachment, TaskComment, AuditLog, ChatMessage],
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
        fullName: 'Administrator',
        role: UserRole.ADMIN,
        department: 'IT',
        position: 'Admin',
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
        fullName: 'Direktor Namuna',
        role: UserRole.DIRECTOR,
        department: 'Boshqaruv',
        position: 'Direktor',
      }),
    );
    console.log('Director: director1 / director123');
  }

  const employee = await repo.findOne({ where: { login: 'xodim1' } });
  if (!employee) {
    await repo.save(
      repo.create({
        login: 'xodim1',
        passwordHash: await bcrypt.hash('xodim123', 10),
        fullName: 'Xodim Namuna',
        role: UserRole.EMPLOYEE,
        department: 'Sotuv',
        position: 'Menejer',
      }),
    );
    console.log('Employee: xodim1 / xodim123');
  }

  await ds.destroy();
  console.log('Seed completed');
}

seed().catch(console.error);
