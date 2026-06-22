import { NestFactory } from '@nestjs/core';
import { ValidationPipe } from '@nestjs/common';
import { NestExpressApplication } from '@nestjs/platform-express';
import { join } from 'path';
import { existsSync, mkdirSync } from 'fs';
import { AppModule } from './app.module';

async function bootstrap() {
  const uploadDir = process.env.UPLOAD_DIR || 'uploads';
  if (!existsSync(uploadDir)) mkdirSync(uploadDir, { recursive: true });

  const app = await NestFactory.create<NestExpressApplication>(AppModule);

  app.setGlobalPrefix('api/v1');
  app.useGlobalPipes(
    new ValidationPipe({ whitelist: true, forbidNonWhitelisted: true, transform: true }),
  );

  const origins = (process.env.CORS_ORIGINS || 'http://localhost:5173').split(',');
  app.enableCors({ origin: origins, credentials: true });

  app.useStaticAssets(join(process.cwd(), uploadDir), { prefix: '/uploads/' });

  const port = process.env.PORT || 3000;
  await app.listen(port);
  console.log(`API: http://localhost:${port}/api/v1`);
}

bootstrap();
