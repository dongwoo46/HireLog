import axios from 'axios';
import { apiClient } from '../utils/apiClient';
import type {
  CoverLetterView,
  HiringStage,
  HiringStageView,
  JobSummaryDetailView,
  JobSummarySearchReq,
  JobSummarySearchResult,
  JobSummaryTextReq,
  JobSummaryUrlReq,
  JobSummaryUrlRes,
  JobSummaryView,
  MemberJobSummaryListItem,
  MemberJobSummarySaveType,
  PagedResult,
  ReviewWriteReq,
} from '../types/jobSummary';

const toJobSummaryView = (item: MemberJobSummaryListItem): JobSummaryView => ({
  id: item.jobSummaryId,
  brandName: item.brandName,
  brandPositionName: item.brandPositionName,
  positionName: item.positionName,
  positionCategoryName: item.positionCategoryName,
  careerType: 'ANY',
  summaryText: '',
  createdAt: item.createdAt,
  isSaved: item.saveType !== 'UNSAVED',
  memberJobSummaryId: item.memberJobSummaryId,
  memberSaveType: item.saveType,
});

const getMemberSummaries = async (
  saveType?: MemberJobSummarySaveType,
  page = 0,
  size = 10,
): Promise<PagedResult<JobSummaryView>> => {
  const response = await apiClient.get<PagedResult<MemberJobSummaryListItem>>('/member-job-summary', {
    params: { saveType, page, size },
  });

  return {
    ...response.data,
    items: (response.data.items || []).map(toJobSummaryView),
  };
};

export const jdSummaryService = {
  search: async (params: JobSummarySearchReq): Promise<JobSummarySearchResult> => {
    const response = await apiClient.get('/job-summary/search', { params });
    return response.data;
  },

  getDetail: async (id: number): Promise<JobSummaryDetailView> => {
    const response = await apiClient.get(`/job-summary/${id}`);
    const data = response.data;

    return {
      ...data,
      id: data.id || data.summaryId || id,
    };
  },

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

    const response = await apiClient.post('/job-summary/ocr', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });

    return response.data;
  },

  requestUrl: async (data: JobSummaryUrlReq): Promise<JobSummaryUrlRes> => {
    const response = await apiClient.post('/job-summary/url', data);
    return response.data;
  },

  save: async (summary: {
    id: number;
    brandName: string;
    brandPositionName: string;
    positionName?: string;
    positionCategoryName?: string;
  }): Promise<void> => {
    await apiClient.post('/member-job-summary', {
      jobSummaryId: summary.id,
      brandName: summary.brandName,
      brandPositionName: summary.brandPositionName,
      positionName: summary.positionName || summary.brandPositionName,
      positionCategoryName: summary.positionCategoryName || '기타',
    });
  },

  unsave: async (jobSummaryId: number): Promise<void> => {
    try {
      await apiClient.patch(`/member-job-summary/${jobSummaryId}/save-type`, {
        saveType: 'UNSAVED',
      });
    } catch (error) {
      if (
        axios.isAxiosError(error) &&
        error.response?.status === 400 &&
        typeof error.response.data?.message === 'string' &&
        error.response.data.message.includes('MemberJobSummary not found')
      ) {
        return;
      }
      throw error;
    }
  },

  addReview: async (id: number, data: ReviewWriteReq): Promise<void> => {
    await apiClient.post(`/job-summary/review/${id}`, data);
  },

  updateReview: async (reviewId: number, data: ReviewWriteReq): Promise<void> => {
    await apiClient.patch(`/job-summary/review/${reviewId}`, data);
  },

  deleteReview: async (reviewId: number): Promise<void> => {
    await apiClient.delete(`/job-summary/review/${reviewId}`);
  },

  restoreReview: async (reviewId: number): Promise<void> => {
    await apiClient.patch(`/job-summary/review/${reviewId}/restore`);
  },

  getReviews: async (
    summaryId: number,
    page = 0,
    size = 20,
    options?: { includeDeleted?: boolean },
  ): Promise<PagedResult<any>> => {
    const response = await apiClient.get(`/job-summary/review/${summaryId}`, {
      params: { page, size, includeDeleted: options?.includeDeleted || undefined },
    });
    return response.data;
  },

  getMyRegistrations: async (page = 0, size = 10): Promise<PagedResult<JobSummaryView>> => {
    return getMemberSummaries(undefined, page, size);
  },

  getMySaves: async (page = 0, size = 10): Promise<PagedResult<JobSummaryView>> => {
    return getMemberSummaries('SAVED', page, size);
  },

  getMyApplies: async (page = 0, size = 10): Promise<PagedResult<JobSummaryView>> => {
    return getMemberSummaries('APPLY', page, size);
  },

  getStages: async (jobSummaryId: number): Promise<HiringStageView[]> => {
    const response = await apiClient.get(`/member-job-summary/${jobSummaryId}/stages`);
    return response.data;
  },

  saveStageNote: async (
    jobSummaryId: number,
    stage: HiringStage,
    note: string,
    result?: import('../types/jobSummary').HiringStageResult | null,
    _currentSaveType?: MemberJobSummarySaveType,
  ): Promise<void> => {
    try {
      await apiClient.post(`/member-job-summary/${jobSummaryId}/stages`, { stage, note, result });
    } catch (error) {
      if (
        axios.isAxiosError(error) &&
        error.response?.status === 400 &&
        typeof error.response.data?.message === 'string' &&
        error.response.data.message.includes('already exists')
      ) {
        await apiClient.patch(`/member-job-summary/${jobSummaryId}/stages`, { stage, note, result });
        return;
      }
      throw error;
    }
  },

  getCoverLetters: async (jobSummaryId: number): Promise<CoverLetterView[]> => {
    const response = await apiClient.get(`/member-job-summary/${jobSummaryId}/cover-letters`);
    return response.data;
  },

  saveCoverLetter: async (
    jobSummaryId: number,
    payload: { question: string; content: string; sortOrder?: number },
  ): Promise<void> => {
    const coverLetters = await jdSummaryService.getCoverLetters(jobSummaryId);
    const target = coverLetters.find((item) => item.sortOrder === (payload.sortOrder ?? 1));

    if (target) {
      await apiClient.patch(`/member-job-summary/${jobSummaryId}/cover-letters/${target.id}`, {
        question: payload.question,
        content: payload.content,
        sortOrder: payload.sortOrder ?? target.sortOrder,
      });
      return;
    }

    await apiClient.post(`/member-job-summary/${jobSummaryId}/cover-letters`, {
      question: payload.question,
      content: payload.content,
      sortOrder: payload.sortOrder ?? 1,
    });
  },

  searchBrands: async (name?: string): Promise<any[]> => {
    const response = await apiClient.get('/brand', {
      params: { name, size: 50 },
    });
    return response.data.items || [];
  },

  searchPositions: async (name?: string): Promise<any[]> => {
    const response = await apiClient.get('/position', {
      params: { name, size: 50 },
    });
    return response.data.items || [];
  },

  searchCategories: async (name?: string): Promise<any[]> => {
    const response = await apiClient.get('/position-category', {
      params: { name, size: 50 },
    });
    return response.data.items || [];
  },
};
