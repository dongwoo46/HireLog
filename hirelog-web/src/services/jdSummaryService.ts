import { apiClient } from '../utils/apiClient';
import type {
  JobSummarySearchReq,
  JobSummarySearchResult,
  JobSummaryDetailView,
  JobSummaryTextReq,
  JobSummaryUrlReq,
  JobSummaryUrlRes,
  ReviewWriteReq
} from '../types/jobSummary';

export const jdSummaryService = {

  /* ---------------------- 검색 ---------------------- */

  search: async (
    params: JobSummarySearchReq
  ): Promise<JobSummarySearchResult> => {
    const response = await apiClient.get('/job-summary/search', { params });
    return response.data;
  },

  /* ---------------------- 상세 ---------------------- */

  getDetail: async (id: number): Promise<JobSummaryDetailView> => {
    const response = await apiClient.get(`/job-summary/${id}`);
    return response.data;
  },

  /* ---------------------- JD 요청 ---------------------- */

  requestText: async (data: JobSummaryTextReq): Promise<void> => {
    await apiClient.post('/job-summary/text', data);
  },

  requestOcr: async (data: {
    brandName: string;
    brandPositionName: string;
    images: File[];
  }): Promise<any> => {

    const formData = new FormData();
    formData.append('brandName', data.brandName);
    formData.append('brandPositionName', data.brandPositionName);

    data.images.forEach((image) => {
      formData.append('images', image);
    });

    const response = await apiClient.post(
      '/job-summary/ocr',
      formData,
      { headers: { 'Content-Type': 'multipart/form-data' } }
    );

    return response.data;
  },

  requestUrl: async (
    data: JobSummaryUrlReq
  ): Promise<JobSummaryUrlRes> => {
    const response = await apiClient.post('/job-summary/url', data);
    return response.data;
  },

  /* ---------------------- 저장 ---------------------- */

  save: async (summary: {
    id: number;
    brandName: string;
    brandPositionName: string;
    brandPositionId?: number;
    positionName?: string;
    positionCategoryName?: string;
  }): Promise<void> => {
    await apiClient.post('/member-job-summary', {
      memberId: 0,
      jobSummaryId: summary.id,
      brandName: summary.brandName,
      brandPositionName: summary.brandPositionName,
      positionName: summary.positionName || summary.brandPositionName,
      positionCategoryName: summary.positionCategoryName || 'Unknown'
    });
  },

  unsave: async (id: number): Promise<void> => {
    await apiClient.patch(`/member-job-summary/${id}/save-type`, {
      saveType: 'UNSAVED'
    });
  },

  /* ---------------------- 메모 ---------------------- */

  getMemos: async (id: number): Promise<any[]> => {
    const response = await apiClient.get(`/job-summary/${id}/memos`);
    return response.data;
  },

  addMemo: async (id: number, content: string): Promise<any> => {
    const response = await apiClient.post(
      `/job-summary/${id}/memo`,
      { content }
    );
    return response.data;
  },

  updateMemo: async (memoId: number, content: string): Promise<void> => {
    await apiClient.patch(`/memo/${memoId}`, { content });
  },

  deleteMemo: async (memoId: number): Promise<void> => {
    await apiClient.delete(`/memo/${memoId}`);
  },

  /* ---------------------- 리뷰 ---------------------- */

  addReview: async (id: number, data: ReviewWriteReq): Promise<void> => {
    await apiClient.post(`/job-summary/review/${id}`, data);
  },

  // ✅ 여기만 추가됨 (API 건드린 거 아님)
  getReviews: async (
    summaryId: number,
    page = 0,
    size = 20
  ): Promise<{
    items: any[];
    page: number;
    size: number;
    totalElements: number;
    totalPages: number;
    hasNext: boolean;
  }> => {
    const response = await apiClient.get(
      `/job-summary/review/${summaryId}`,
      { params: { page, size } }
    );
    return response.data;
  },

  /* ---------------------- 아카이브 ---------------------- */

  getMyRegistrations: async (page = 0, size = 10): Promise<JobSummarySearchResult> => {
    const response = await apiClient.get('/job-summary/my', {
      params: { page, size }
    });
    const items = response.data.items.map((item: any) => ({
      ...item,
      id: item.jobSummaryId || item.id,
      isSaved: true
    }));
    return { ...response.data, items };
  },

  getMySaves: async (page = 0, size = 10): Promise<JobSummarySearchResult> => {
    const response = await apiClient.get('/job-summary/saved', {
      params: { page, size }
    });
    const items = response.data.items.map((item: any) => ({
      ...item,
      id: item.jobSummaryId || item.id,
      isSaved: true
    }));
    return { ...response.data, items };
  },

  /* ---------------------- 마스터 데이터 ---------------------- */

  searchBrands: async (name?: string): Promise<any[]> => {
    const response = await apiClient.get('/brand', {
      params: { name, size: 50 }
    });
    return response.data.items || [];
  },

  searchPositions: async (name?: string): Promise<any[]> => {
    const response = await apiClient.get('/position', {
      params: { name, size: 50 }
    });
    return response.data.items || [];
  },

  searchCategories: async (name?: string): Promise<any[]> => {
    const response = await apiClient.get('/position-category', {
      params: { name, size: 50 }
    });
    return response.data.items || [];
  },

};
