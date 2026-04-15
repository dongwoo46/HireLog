import { apiClient } from '../utils/apiClient';
import type {
  AdminJobSummaryDirectCreateReq,
  AdminJobSummaryDirectUrlCreateReq,
  AdminJobSummaryView,
  AdminPagedResult,
  AdminRagIntent,
  AdminRagLogView,
  AdminReportView,
  AdminReviewView,
  BrandListView,
  CompanyView,
  MemberSummaryView,
  ReportProcessType,
  ReportStatus,
  ReportTargetType,
} from '../types/admin';
import type { HiringStage, ReviewSortType } from '../types/jobSummary';

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

  createJobSummaryFromUrlDirectly: async (
    payload: AdminJobSummaryDirectUrlCreateReq
  ): Promise<{ summaryId: number }> => {
    const response = await apiClient.post('/admin/job-summary/direct-url', payload);
    return response.data;
  },

  reindexAllJobSummaries: async (batchSize = 50): Promise<number> => {
    const response = await apiClient.post<{ successCount: number }>('/admin/job-summary/reindex-all', null, {
      params: { batchSize },
    });
    return response.data.successCount;
  },

  reindexMissingEmbeddings: async (batchSize = 50): Promise<number> => {
    const response = await apiClient.post<{ successCount: number }>('/admin/job-summary/reindex-embedding', null, {
      params: { batchSize },
    });
    return response.data.successCount;
  },

  getAllJobSummaries: async (
    page = 0,
    size = 20,
    isActive?: boolean,
    brandName?: string
  ): Promise<AdminPagedResult<AdminJobSummaryView>> => {
    const response = await apiClient.get('/admin/job-summary', {
      params: { page, size, isActive, brandName },
    });

    const data = response.data;

    return {
      items: data.content ?? [],
      totalElements: data.totalElements ?? 0,
      totalPages: data.totalPages ?? 0,
      size: data.size ?? size,
      number: data.number ?? page,
    };
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

  getAllUserRequests: async (page = 0, size = 20, status?: string): Promise<AdminPagedResult<unknown>> => {
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

  getAllReviews: async (
    page = 0,
    size = 20,
    params?: {
      jobSummaryId?: number;
      memberName?: string;
      hiringStage?: HiringStage;
      minDifficultyRating?: number;
      maxDifficultyRating?: number;
      minSatisfactionRating?: number;
      maxSatisfactionRating?: number;
      sortBy?: ReviewSortType;
      createdFrom?: string;
      createdTo?: string;
      includeDeleted?: boolean;
    },
  ): Promise<AdminPagedResult<AdminReviewView>> => {
    const response = await apiClient.get('/job-summary/review/admin', {
      params: {
        page,
        size,
        jobSummaryId: params?.jobSummaryId,
        memberName: params?.memberName,
        hiringStage: params?.hiringStage,
        minDifficultyRating: params?.minDifficultyRating,
        maxDifficultyRating: params?.maxDifficultyRating,
        minSatisfactionRating: params?.minSatisfactionRating,
        maxSatisfactionRating: params?.maxSatisfactionRating,
        sortBy: params?.sortBy,
        createdFrom: params?.createdFrom,
        createdTo: params?.createdTo,
        includeDeleted: params?.includeDeleted,
      },
    });
    return response.data;
  },

  getAllReports: async (
    page = 0,
    size = 20,
    params?: {
      status?: ReportStatus;
      targetType?: ReportTargetType;
    },
  ): Promise<AdminPagedResult<AdminReportView>> => {
    const response = await apiClient.get('/admin/reports', {
      params: {
        page,
        size,
        status: params?.status,
        targetType: params?.targetType,
      },
    });
    return response.data;
  },

  processReport: async (reportId: number, processType: ReportProcessType): Promise<void> => {
    await apiClient.patch(`/admin/reports/${reportId}/process`, { processType });
  },

  getAllRagLogs: async (
    page = 0,
    size = 20,
    params?: {
      memberId?: number;
      intent?: AdminRagIntent;
      dateFrom?: string;
      dateTo?: string;
    },
  ): Promise<AdminPagedResult<AdminRagLogView>> => {
    const response = await apiClient.get('/admin/rag/logs', {
      params: {
        page,
        size,
        memberId: params?.memberId,
        intent: params?.intent,
        dateFrom: params?.dateFrom,
        dateTo: params?.dateTo,
      },
    });

    const data = response.data;
    return {
      items: data.items ?? [],
      totalElements: data.totalElements ?? 0,
      totalPages: data.totalPages ?? 0,
      size: data.size ?? size,
      number: data.page ?? page,
    };
  },

  getRagLogById: async (id: number): Promise<AdminRagLogView> => {
    const response = await apiClient.get(`/admin/rag/logs/${id}`);
    return response.data;
  },
};
