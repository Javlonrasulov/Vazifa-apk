import { Module } from '@nestjs/common';
import { ConfigModule, ConfigService } from '@nestjs/config';
import { TypeOrmModule } from '@nestjs/typeorm';
import { ScheduleModule } from '@nestjs/schedule';
import { APP_INTERCEPTOR } from '@nestjs/core';
import { AuthModule } from './auth/auth.module';
import { UsersModule } from './users/users.module';
import { TasksModule } from './tasks/tasks.module';
import { AuditModule } from './audit/audit.module';
import { NotificationsModule } from './notifications/notifications.module';
import { ChatModule } from './chat/chat.module';
import { DepartmentsModule } from './departments/departments.module';
import { PresenceInterceptor } from './common/interceptors/presence.interceptor';

@Module({
  imports: [
    ConfigModule.forRoot({ isGlobal: true }),
    ScheduleModule.forRoot(),
    TypeOrmModule.forRootAsync({
      imports: [ConfigModule],
      inject: [ConfigService],
      useFactory: (config: ConfigService) => ({
        type: 'postgres',
        host: config.get('DB_HOST', 'localhost'),
        port: +config.get('DB_PORT', 5432),
        username: config.get('DB_USERNAME', 'vazifa'),
        password: config.get('DB_PASSWORD', 'vazifa123'),
        database: config.get('DB_DATABASE', 'vazifa'),
        autoLoadEntities: true,
        synchronize: config.get('NODE_ENV') !== 'production',
      }),
    }),
    AuthModule,
    UsersModule,
    TasksModule,
    AuditModule,
    NotificationsModule,
    ChatModule,
    DepartmentsModule,
  ],
  providers: [
    {
      provide: APP_INTERCEPTOR,
      useClass: PresenceInterceptor,
    },
  ],
})
export class AppModule {}
