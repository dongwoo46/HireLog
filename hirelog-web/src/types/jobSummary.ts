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
  careerType: CareerType;
  careerYears?: number;
  summaryText: string;
  techStackParsed?: string[];
  thumbnailUrl?: string;
  createdAt?: string;
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

export interface JobSummaryUrlRes {
  requestId?: string;
  jobSummaryId?: number;
  isDuplicate: boolean;
}
