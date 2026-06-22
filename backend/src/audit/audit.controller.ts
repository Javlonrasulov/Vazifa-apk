import { Controller, Get, UseGuards } from '@nestjs/common';
import { AuthGuard } from '@nestjs/passport';
import { AuditService } from '../audit/audit.service';
import { RolesGuard, Roles } from '../common/guards/roles.guard';
import { UserRole } from '../common/enums';

@UseGuards(AuthGuard('jwt'), RolesGuard)
@Roles(UserRole.DIRECTOR, UserRole.ADMIN)
@Controller('audit')
export class AuditController {
  constructor(private audit: AuditService) {}

  @Get()
  findRecent() {
    return this.audit.findRecent(100);
  }
}
