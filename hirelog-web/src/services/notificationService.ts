import { apiClient } from '../utils/apiClient';
import type { PagedResult } from '../types/jobSummary';

export type NotificationReferenceType = 'JOB_SUMMARY' | 'USER_REQUEST' | null;

export interface NotificationItem {
  id: number;
  type: string;
  title: string;
  message?: string | null;
  referenceType?: NotificationReferenceType;
  referenceId?: number | null;
  metadata?: Record<string, unknown>;
  isRead: boolean;
  createdAt: string;
}

export const notificationService = {
  getNotifications: async (page = 0, size = 20, isRead?: boolean): Promise<PagedResult<NotificationItem>> => {
    const response = await apiClient.get<PagedResult<NotificationItem>>('/notification', {
      params: { page, size, isRead },
    });
    return response.data;
  },

  getUnreadCount: async (): Promise<number> => {
    const response = await apiClient.get<{ unreadCount: number }>('/notification/unread-count');
    return response.data.unreadCount;
  },

  markAsRead: async (notificationIds: number[]): Promise<void> => {
    if (!notificationIds.length) return;
    await apiClient.patch('/notification/read', notificationIds);
  },
};
