import { apiClient } from '../utils/apiClient';

export interface CheckEmailRequest {
  email: string;
}

export interface CheckEmailResponse {
  exists: boolean;
  username: string | null;
}

export interface BindRequest {
  email: string;
}

export interface SignupCompleteRequest {
  email: string;
  username: string;
  currentPositionId?: number;
  careerYears?: number;
  summary?: string;
}

export const authService = {
  /**
   * 이메일 중복 체크 및 가입 가능 여부 확인
   * POST /auth/signup/check-email
   */
  checkEmail: async (data: CheckEmailRequest): Promise<CheckEmailResponse> => {
    const response = await apiClient.post<CheckEmailResponse>('/auth/signup/check-email', data, {
      withCredentials: true,
    });
    return response.data;
  },

  /**
   * 기존 회원 계정 연동 (Binding)
   * POST /auth/signup/bind
   */
  bind: async (data: BindRequest): Promise<void> => {
    await apiClient.post('/auth/signup/bind', data, {
      withCredentials: true,
    });
  },

  /**
   * 신규 회원 가입 완료 (Provisioning)
   * POST /auth/signup/complete
   */
  complete: async (data: SignupCompleteRequest): Promise<void> => {
    await apiClient.post('/auth/signup/complete', data, {
      withCredentials: true,
    });
  },

  /**
   * Access Token 재발급
   * POST /auth/refresh
   * Cookie(refresh_token) 사용 (Body 없음)
   */
  refreshToken: async (): Promise<{ accessToken: string }> => {
    const response = await apiClient.post<{ accessToken: string; refreshToken: string }>(
      '/auth/refresh',
      {}, // Empty body
      {
        withCredentials: true,
      }
    );
    return response.data;
  },

  /**
   * 로그아웃
   * POST /auth/logout
   */
  logout: async (): Promise<void> => {
    await apiClient.post('/auth/logout', {}, {
      withCredentials: true,
    });
  },
};
