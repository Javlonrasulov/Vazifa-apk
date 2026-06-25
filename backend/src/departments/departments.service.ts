import {
  BadRequestException,
  ConflictException,
  Injectable,
  NotFoundException,
  OnModuleInit,
} from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { Department } from './entities/department.entity';
import { User } from '../users/entities/user.entity';
import { UserFieldOption } from '../users/entities/user-field-option.entity';
import { UserRole } from '../common/enums';
import { CreateDepartmentDto, UpdateDepartmentDto } from './dto/department.dto';

@Injectable()
export class DepartmentsService implements OnModuleInit {
  constructor(
    @InjectRepository(Department)
    private deptRepo: Repository<Department>,
    @InjectRepository(User)
    private userRepo: Repository<User>,
    @InjectRepository(UserFieldOption)
    private optionRepo: Repository<UserFieldOption>,
  ) {}

  async onModuleInit() {
    const users = await this.userRepo.find({ select: ['department'] });
    for (const user of users) {
      await this.ensureDepartment(user.department);
    }

    const options = await this.optionRepo.find({ where: { type: 'department' } });
    for (const option of options) {
      await this.ensureDepartment(option.name);
    }
  }

  private normalize(name: string) {
    return name.trim().toLowerCase();
  }

  async ensureDepartment(name?: string | null) {
    const trimmed = name?.trim();
    if (!trimmed) return null;

    const nameNormalized = this.normalize(trimmed);
    const existing = await this.deptRepo.findOne({ where: { nameNormalized } });
    if (existing) return existing;

    return this.deptRepo.save(
      this.deptRepo.create({ name: trimmed, nameNormalized }),
    );
  }

  private async countEmployees(departmentName: string): Promise<number> {
    const normalized = this.normalize(departmentName);
    const users = await this.userRepo.find({
      where: [
        { role: UserRole.EMPLOYEE, isActive: true },
        { role: UserRole.DIRECTOR, isActive: true },
      ],
      select: ['department'],
    });
    return users.filter(
      (u) => u.department && this.normalize(u.department) === normalized,
    ).length;
  }

  async findAll() {
    const departments = await this.deptRepo.find({ order: { name: 'ASC' } });
    return Promise.all(
      departments.map(async (d) => ({
        id: d.id,
        name: d.name,
        employeeCount: await this.countEmployees(d.name),
        createdAt: d.createdAt,
        updatedAt: d.updatedAt,
      })),
    );
  }

  async findNames(q?: string): Promise<string[]> {
    const qb = this.deptRepo.createQueryBuilder('d').orderBy('d.name', 'ASC').limit(50);
    const query = q?.trim().toLowerCase();
    if (query) qb.where('d.nameNormalized LIKE :q', { q: `%${query}%` });
    const rows = await qb.getMany();
    return rows.map((r) => r.name);
  }

  async create(dto: CreateDepartmentDto) {
    const name = dto.name.trim();
    const nameNormalized = this.normalize(name);
    const exists = await this.deptRepo.findOne({ where: { nameNormalized } });
    if (exists) throw new ConflictException('Bu bo\'lim allaqachon mavjud');

    const saved = await this.deptRepo.save(this.deptRepo.create({ name, nameNormalized }));
    await this.syncFieldOption(name);
    return { ...saved, employeeCount: 0 };
  }

  async update(id: string, dto: UpdateDepartmentDto) {
    const department = await this.deptRepo.findOne({ where: { id } });
    if (!department) throw new NotFoundException('Bo\'lim topilmadi');

    const newName = dto.name.trim();
    const newNormalized = this.normalize(newName);
    if (newNormalized !== department.nameNormalized) {
      const exists = await this.deptRepo.findOne({ where: { nameNormalized: newNormalized } });
      if (exists) throw new ConflictException('Bu bo\'lim allaqachon mavjud');
    }

    const oldName = department.name;
    department.name = newName;
    department.nameNormalized = newNormalized;
    const saved = await this.deptRepo.save(department);

    if (oldName !== newName) {
      await this.userRepo
        .createQueryBuilder()
        .update(User)
        .set({ department: newName })
        .where('LOWER(TRIM(department)) = LOWER(:old)', { old: oldName.trim() })
        .execute();

      await this.optionRepo.delete({
        type: 'department',
        nameNormalized: this.normalize(oldName),
      });
    }

    await this.syncFieldOption(newName);
    return {
      ...saved,
      employeeCount: await this.countEmployees(saved.name),
    };
  }

  async delete(id: string) {
    const department = await this.deptRepo.findOne({ where: { id } });
    if (!department) throw new NotFoundException('Bo\'lim topilmadi');

    const employeeCount = await this.countEmployees(department.name);
    if (employeeCount > 0) {
      throw new BadRequestException(`Bu bo'limda ${employeeCount} ta xodim bor`);
    }

    await this.optionRepo.delete({
      type: 'department',
      nameNormalized: department.nameNormalized,
    });
    await this.deptRepo.remove(department);
    return { success: true };
  }

  private async syncFieldOption(name: string) {
    const trimmed = name.trim();
    const nameNormalized = this.normalize(trimmed);
    const exists = await this.optionRepo.findOne({
      where: { type: 'department', nameNormalized },
    });
    if (!exists) {
      await this.optionRepo.save(
        this.optionRepo.create({ type: 'department', name: trimmed, nameNormalized }),
      );
    }
  }
}
