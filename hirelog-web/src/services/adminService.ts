import { apiClient } from '../utils/apiClient';
import type {
  AdminJobSummaryDirectCreateReq,
  AdminPagedResult,
  BrandListView,
  CompanyView,
  MemberSummaryView,
} from '../types/admin';

export const adminService = {
  getAllMembers: async (page = 0, size = 20): Promise<AdminPagedResult<MemberSummaryView>> => {
    const response = await apiClient.get('/admin/member', {
      params: { page, size },
    });
    return response.data;
  },

  suspendMember: async (memberId: number): Promise<void> => {
    await apiClient.post(`/admin/member/${memberId}/suspend`);
  },

  activateMember: async (memberId: number): Promise<void> => {
    await apiClient.post(`/admin/member/${memberId}/activate`);
  },

  deleteMember: async (memberId: number): Promise<void> => {
    await apiClient.delete(`/admin/member/${memberId}`);
  },

  activateJob: async (id: number): Promise<void> => {
    await apiClient.patch(`/job-summary/${id}/activate`);
  },

  deactivateJob: async (id: number): Promise<void> => {
    await apiClient.patch(`/job-summary/${id}/deactivate`);
  },

  createJobSummaryDirectly: async (payload: AdminJobSummaryDirectCreateReq): Promise<{ summaryId: number }> => {
    const response = await apiClient.post('/admin/job-summary/direct', payload);
    return response.data;
  },

  getAllBrands: async (page = 0, size = 20): Promise<AdminPagedResult<BrandListView>> => {
    const response = await apiClient.get('/brand', {
      params: { page, size },
    });
    return response.data;
  },

  verifyBrand: async (brandId: number): Promise<void> => {
    await apiClient.patch(`/brand/${brandId}/verify`);
  },

  rejectBrand: async (brandId: number): Promise<void> => {
    await apiClient.patch(`/brand/${brandId}/reject`);
  },

  activateBrand: async (brandId: number): Promise<void> => {
    await apiClient.patch(`/brand/${brandId}/activate`);
  },

  deactivateBrand: async (brandId: number): Promise<void> => {
    await apiClient.patch(`/brand/${brandId}/deactivate`);
  },

  getAllCompanies: async (page = 0, size = 20): Promise<AdminPagedResult<CompanyView>> => {
    const response = await apiClient.get('/companies', {
      params: { page, size },
    });
    return response.data;
  },

  activateCompany: async (companyId: number): Promise<void> => {
    await apiClient.patch(`/companies/${companyId}/activate`);
  },

  deactivateCompany: async (companyId: number): Promise<void> => {
    await apiClient.patch(`/companies/${companyId}/deactivate`);
  },

  getAllUserRequests: async (page = 0, size = 20, status?: string): Promise<AdminPagedResult<any>> => {
    const response = await apiClient.get('/user-requests/admin', {
      params: { page, size, status },
    });
    return response.data;
  },

  updateUserRequestStatus: async (requestId: number, status: string): Promise<void> => {
    await apiClient.patch(`/user-requests/${requestId}/status`, { status });
  },

  deleteReview: async (reviewId: number): Promise<void> => {
    await apiClient.delete(`/job-summary/review/${reviewId}`);
  },

  restoreReview: async (reviewId: number): Promise<void> => {
    await apiClient.patch(`/job-summary/review/${reviewId}/restore`);
  },
};
