import { Module, forwardRef } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { NotificationsService } from './notifications.service';
import { RemindersService } from './reminders.service';
import { UsersModule } from '../users/users.module';
import { TasksModule } from '../tasks/tasks.module';
import { PushOutbox } from './entities/push-outbox.entity';

@Module({
  imports: [
    UsersModule,
    TypeOrmModule.forFeature([PushOutbox]),
    forwardRef(() => TasksModule),
  ],
  providers: [NotificationsService, RemindersService],
  exports: [NotificationsService],
})
export class NotificationsModule {}
