export type CareerType = 'NEW' | 'EXPERIENCED' | 'ANY';

export interface JobSummarySearchReq {
  keyword?: string;
  careerType?: CareerType;

  // ID Filters
  brandId?: number;
  companyId?: number;
  positionId?: number;
  brandPositionId?: number;
  positionCategoryId?: number;

  // Name Filters
  brandName?: string;
  positionName?: string;
  brandPositionName?: string;
  positionCategoryName?: string;

  techStacks?: string[];
  sortBy?: string; // e.g., "CREATED_AT_DESC"
  page?: number;
  size?: number;
  isSaved?: boolean;
}

export interface JobSummaryView {
  id: number;
  brandName: string;
  brandPositionName: string;
  positionName?: string;
  positionCategoryName?: string;
  careerType: CareerType;
  careerYears?: number;
  summaryText: string;
  techStackParsed?: string[];
  thumbnailUrl?: string;
  createdAt?: string;
  isSaved?: boolean;
}

export interface JobSummarySearchResult {
  items: JobSummaryView[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export interface JobSummaryDetailView extends JobSummaryView {
  responsibilities: string;
  requiredQualifications: string;
  preferredQualifications?: string;
  techStack?: string;
  recruitmentProcess?: string;
  sourceUrl?: string;
  memberJobSummaryId?: number;
  memberSaveType?: 'SAVE' | 'LIKE';
  isActive: boolean;
  insights?: string;
  reviews?: any[];
}

export interface JobSummaryTextReq {
  brandName: string;
  brandPositionName: string;
  jdText: string;
}

export interface JobSummaryUrlReq {
  brandName: string;
  brandPositionName: string;
  url: string;
}

export type HiringStage =
  | 'DOCUMENT'
  | 'CODING_TEST'
  | 'ASSIGNMENT'
  | 'INTERVIEW_1'
  | 'INTERVIEW_2'
  | 'INTERVIEW_3'
  | 'FINAL_INTERVIEW'
  | 'COFFEE_CHAT';

export const HIRING_STAGE_LABELS: Record<HiringStage, string> = {
  DOCUMENT: '서류 전형',
  CODING_TEST: '코딩 테스트',
  ASSIGNMENT: '과제 전형',
  INTERVIEW_1: '1차 면접',
  INTERVIEW_2: '2차 면접',
  INTERVIEW_3: '3차 면접',
  FINAL_INTERVIEW: '최종 면접',
  COFFEE_CHAT: '커피챗',
};

export interface ReviewWriteReq {
  hiringStage: HiringStage;
  anonymous: boolean;
  difficultyRating: number;
  satisfactionRating: number;
  experienceComment: string;
  interviewTip?: string;
}

export interface JobSummaryUrlRes {
  requestId?: string;
  jobSummaryId?: number;
  isDuplicate: boolean;
}
