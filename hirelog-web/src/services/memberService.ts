import { apiClient } from '../utils/apiClient';
import type {
  MemberDetailView,
  UpdateProfileReq,
  UpdateUsernameReq,
  UpdateEmailReq,
  ChangePasswordReq
} from '../types/member';

export const memberService = {
  getMe: async (): Promise<MemberDetailView> => {
    const response = await apiClient.get<MemberDetailView>('/member/me');
    return response.data;
  },

  updateProfile: async (data: UpdateProfileReq): Promise<void> => {
    await apiClient.patch('/member/me/profile', data);
  },

  updateUsername: async (data: UpdateUsernameReq): Promise<void> => {
    await apiClient.patch('/member/me/username', data);
  },

  updateEmail: async (data: UpdateEmailReq): Promise<void> => {
    await apiClient.patch('/member/me/email', data);
  },

  changePassword: async (data: ChangePasswordReq): Promise<void> => {
    await apiClient.patch('/member/me/password', data);
  },

  withdraw: async (): Promise<void> => {
    await apiClient.delete('/member/me');
  },
};
