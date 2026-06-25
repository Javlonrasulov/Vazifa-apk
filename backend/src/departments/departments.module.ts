import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { Department } from './entities/department.entity';
import { DepartmentsService } from './departments.service';
import { DepartmentsController } from './departments.controller';
import { User } from '../users/entities/user.entity';
import { UserFieldOption } from '../users/entities/user-field-option.entity';

@Module({
  imports: [TypeOrmModule.forFeature([Department, User, UserFieldOption])],
  providers: [DepartmentsService],
  controllers: [DepartmentsController],
  exports: [DepartmentsService],
})
export class DepartmentsModule {}
