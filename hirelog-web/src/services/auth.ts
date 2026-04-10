import axios from 'axios';
import { apiClient } from '../utils/apiClient';

const API_BASE_URL = import.meta.env.DEV ? '' : import.meta.env.VITE_API_BASE_URL;

export interface CheckEmailRequest {
  email: string;
}

export interface CheckUsernameRequest {
  username: string;
}

export interface CheckEmailResponse {
  exists: boolean;
  username: string | null;
}

export interface CheckUsernameResponse {
  exists: boolean;
}

export interface BindRequest {
  email: string;
}

export interface SignupCompleteRequest {
  email: string;
  username: string;
  password?: string;
  currentPositionId?: number;
  careerYears?: number;
  summary?: string;
}

export interface PasswordLoginRequest {
  email: string;
  password: string;
}

export interface PasswordResetRequest {
  email: string;
  newPassword: string;
}

export const authService = {
  /**
   * 이메일 중복 체크 및 가입 가능 여부 확인
   * POST /auth/signup/check-email
   */
  checkEmail: async (data: CheckEmailRequest): Promise<CheckEmailResponse> => {
    const response = await apiClient.post<CheckEmailResponse>('/auth/signup/check-email', data);
    return response.data;
  },

  checkGeneralEmail: async (data: CheckEmailRequest): Promise<CheckEmailResponse> => {
    const response = await apiClient.post<CheckEmailResponse>('/auth/signup/general/check-email', data);
    return response.data;
  },

  checkGeneralUsername: async (data: CheckUsernameRequest): Promise<CheckUsernameResponse> => {
    const response = await apiClient.post<CheckUsernameResponse>('/auth/signup/general/check-username', data);
    return response.data;
  },

  /**
   * 인증코드 발송 (기존 계정 연결 선택 시)
   * POST /auth/signup/send-code
   */
  sendCode: async (data: { email: string }): Promise<void> => {
    await apiClient.post('/auth/signup/send-code', data);
  },

  sendGeneralCode: async (data: { email: string }): Promise<void> => {
    await apiClient.post('/auth/signup/general/send-code', data);
  },

  /**
   * 인증코드 검증
   * POST /auth/signup/verify-code
   */
  verifyCode: async (data: { email: string; code: string }): Promise<{ verified: boolean }> => {
    const response = await apiClient.post<{ verified: boolean }>('/auth/signup/verify-code', data);
    return response.data;
  },

  verifyGeneralCode: async (data: { email: string; code: string }): Promise<void> => {
    await apiClient.post('/auth/signup/general/verify-code', data);
  },

  /**
   * 기존 회원 계정 연동 (Binding)
   * POST /auth/signup/bind
   */
  bind: async (data: BindRequest): Promise<void> => {
    await apiClient.post('/auth/signup/bind', data);
  },

  /**
   * 신규 회원 가입 완료 (Provisioning)
   * POST /auth/signup/complete
   */
  complete: async (data: SignupCompleteRequest): Promise<void> => {
    await apiClient.post('/auth/signup/complete', data);
  },

  completeGeneral: async (data: SignupCompleteRequest): Promise<void> => {
    await apiClient.post('/auth/signup/general/complete', data);
  },

  /**
   * 계정 복구 완료 (Recovery)
   * POST /auth/recovery/complete
   */
  completeRecovery: async (data: SignupCompleteRequest): Promise<void> => {
    await apiClient.post('/auth/signup/recovery/complete', data);
  },

  /**
   * 계정 복구 인증코드 발송
   * POST /auth/recovery/send-code
   */
  sendRecoveryCode: async (data: { email: string }): Promise<void> => {
    await apiClient.post('/auth/signup/recovery/send-code', data);
  },

  /**
   * 계정 복구 인증코드 검증
   * POST /auth/signup/recovery/verify-code
   */
  verifyRecoveryCode: async (data: { email: string; code: string }): Promise<{ verified: boolean }> => {
    const response = await apiClient.post<{ verified: boolean }>('/auth/signup/recovery/verify-code', data);
    return response.data;
  },

  /**
   * Access Token 재발급
   * POST /auth/refresh
   * Cookie(refresh_token) 사용 (Body 없음)
   */
  refreshToken: async (): Promise<{ accessToken: string }> => {
    const response = await axios.post<{ accessToken: string; refreshToken: string }>(
      `${API_BASE_URL}/api/auth/refresh`,
      {},
      { withCredentials: true }
    );
    return response.data;
  },

  /**
   * 로그아웃
   * POST /auth/logout
   */
  logout: async (): Promise<void> => {
    await apiClient.post('/auth/logout', {});
  },

  /**
   * 이메일/비밀번호 로그인
   * POST /auth/login
   */
  loginWithPassword: async (data: PasswordLoginRequest): Promise<{ accessToken: string; refreshToken: string }> => {
    const response = await apiClient.post<{ accessToken: string; refreshToken: string }>('/auth/login', data);
    return response.data;
  },

  sendPasswordResetCode: async (data: { email: string }): Promise<void> => {
    await apiClient.post('/auth/password/send-code', data);
  },

  verifyPasswordResetCode: async (data: { email: string; code: string }): Promise<void> => {
    await apiClient.post('/auth/password/verify-code', data);
  },

  resetPassword: async (data: PasswordResetRequest): Promise<void> => {
    await apiClient.post('/auth/password/reset', data);
  },
};
