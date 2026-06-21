import { Injectable, Logger } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import * as admin from 'firebase-admin';

@Injectable()
export class NotificationsService {
  private readonly logger = new Logger(NotificationsService.name);
  private initialized = false;

  constructor(private config: ConfigService) {
    this.initFirebase();
  }

  private initFirebase() {
    const projectId = this.config.get('FIREBASE_PROJECT_ID');
    const clientEmail = this.config.get('FIREBASE_CLIENT_EMAIL');
    const privateKey = this.config.get('FIREBASE_PRIVATE_KEY')?.replace(/\\n/g, '\n');

    if (!projectId || !clientEmail || !privateKey) {
      this.logger.warn('Firebase not configured — push notifications disabled');
      return;
    }

    try {
      admin.initializeApp({
        credential: admin.credential.cert({ projectId, clientEmail, privateKey }),
      });
      this.initialized = true;
    } catch (e) {
      this.logger.error('Firebase init failed', e);
    }
  }

  async sendToToken(token: string, title: string, body: string, data?: Record<string, string>) {
    if (!this.initialized || !token) {
      this.logger.debug(`Push (mock): ${title} — ${body}`);
      return;
    }
    try {
      await admin.messaging().send({
        token,
        notification: { title, body },
        data,
        android: { priority: 'high' },
      });
    } catch (e) {
      this.logger.error('FCM send failed', e);
    }
  }

  async sendToMany(tokens: string[], title: string, body: string, data?: Record<string, string>) {
    const valid = tokens.filter(Boolean);
    if (!valid.length) return;
    await Promise.all(valid.map((t) => this.sendToToken(t, title, body, data)));
  }
}
