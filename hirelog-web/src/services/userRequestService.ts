import { apiClient } from '../utils/apiClient';
import type { 
  UserRequestCreateReq, 
  UserRequestListRes, 
  UserRequestDetailRes, 
  UserRequestStatusUpdateReq, 
  UserRequestCommentCreateReq 
} from '../types/userRequest';

export const userRequestService = {
  /**
   * 사용자 요청 생성
   */
  create: async (request: UserRequestCreateReq) => {
    const response = await apiClient.post('/user-requests', request);
    return response.data;
  },

  /**
   * 내 요청 목록 조회
   */
  getMyRequests: async () => {
    const response = await apiClient.get<UserRequestListRes[]>('/user-requests/my');
    return response.data;
  },

  /**
   * 요청 상세 조회
   */
  getDetail: async (id: number) => {
    const response = await apiClient.get<UserRequestDetailRes>(`/user-requests/${id}`);
    return response.data;
  },

  /**
   * 전체 요청 목록 조회 (관리자)
   */
  getAllRequests: async (status?: string, page = 0, size = 20) => {
    const response = await apiClient.get('/user-requests/admin', {
      params: { status, page, size }
    });
    return response.data;
  },

  /**
   * 요청 상태 변경 (관리자)
   */
  updateStatus: async (id: number, status: UserRequestStatusUpdateReq) => {
    const response = await apiClient.patch(`/user-requests/${id}/status`, status);
    return response.data;
  },

  /**
   * 댓글 작성
   */
  addComment: async (id: number, request: UserRequestCommentCreateReq) => {
    const response = await apiClient.post(`/user-requests/${id}/comments`, request);
    return response.data;
  }
};
