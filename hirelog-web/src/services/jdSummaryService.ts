import { apiClient } from '../utils/apiClient';
import type {
  JobSummarySearchReq,
  JobSummarySearchResult,
  JobSummaryDetailView,
  JobSummaryTextReq,
  JobSummaryUrlReq,
  JobSummaryUrlRes
} from '../types/jobSummary';

export const jdSummaryService = {
  search: async (params: JobSummarySearchReq): Promise<JobSummarySearchResult> => {
    // If backend expects sortBy but we pass sort, mapping is needed. 
    // Given user's DTO uses sortBy, and our previous code used sort.
    const response = await apiClient.get<JobSummarySearchResult>('/job-summary/search', { params });
    return response.data;
  },

  getDetail: async (id: number): Promise<JobSummaryDetailView> => {
    const response = await apiClient.get<JobSummaryDetailView>(`/job-summary/${id}`);
    return response.data;
  },

  requestText: async (data: JobSummaryTextReq): Promise<void> => {
    await apiClient.post('/job-summary/text', data);
  },

  requestOcr: async (data: { brandName: string; brandPositionName: string; images: File[] }): Promise<{ requestId: string }> => {
    const formData = new FormData();
    formData.append('brandName', data.brandName);
    formData.append('brandPositionName', data.brandPositionName);
    data.images.forEach((image) => {
      formData.append('images', image);
    });

    const response = await apiClient.post<{ requestId: string }>('/job-summary/ocr', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    return response.data;
  },

  requestUrl: async (data: JobSummaryUrlReq): Promise<JobSummaryUrlRes> => {
    const response = await apiClient.post<JobSummaryUrlRes>('/job-summary/url', data);
    return response.data;
  },

  deactivate: async (id: number): Promise<void> => {
    await apiClient.patch(`/job-summary/${id}/deactivate`);
  },

  activate: async (id: number): Promise<void> => {
    await apiClient.patch(`/job-summary/${id}/activate`);
  },
};
