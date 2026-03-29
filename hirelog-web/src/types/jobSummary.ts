export type CareerType = 'NEW' | 'EXPERIENCED' | 'ANY';
export type MemberJobSummarySaveType = 'SAVED' | 'APPLY' | 'UNSAVED';

export type JobPlatformType =
  | 'WANTED'
  | 'REMEMBER'
  | 'SARAMIN'
  | 'JOBKOREA'
  | 'ROCKETPUNCH'
  | 'PROGRAMMERS'
  | 'JUMPIT'
  | 'RALLIT'
  | 'CATCH'
  | 'INCRUIT'
  | 'GREPP'
  | 'LINKEDIN'
  | 'OTHER';

export const JOB_PLATFORM_LABELS: Record<JobPlatformType, string> = {
  WANTED: '원티드',
  REMEMBER: '리멤버',
  SARAMIN: '사람인',
  JOBKOREA: '잡코리아',
  ROCKETPUNCH: '로켓펀치',
  PROGRAMMERS: '프로그래머스',
  JUMPIT: '점핏',
  RALLIT: '랠릿',
  CATCH: '캐치',
  INCRUIT: '인크루트',
  GREPP: '그렙',
  LINKEDIN: '링크드인',
  OTHER: '기타',
};

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
  cursor?: string;
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
  memberJobSummaryId?: number;
  memberSaveType?: MemberJobSummarySaveType;
}

export interface JobSummarySearchResult {
  items: JobSummaryView[];
  size: number;
  hasNext: boolean;
  nextCursor: string | null;
}

export interface JobSummaryDetailView extends JobSummaryView {
  summaryId?: number;
  responsibilities: string;
  requiredQualifications: string;
  preferredQualifications?: string;
  techStack?: string;
  recruitmentProcess?: string;
  sourceUrl?: string;
  memberJobSummaryId?: number;
  memberSaveType?: MemberJobSummarySaveType;
  insights?: string;
  reviews?: any[];
  preparationFocus?: string;
  proofPointsAndMetrics?: string;
  questionsToAsk?: string;
}

export interface JobSummaryTextReq {
  brandName: string;
  brandPositionName: string;
  jdText: string;
  platform: JobPlatformType;
}

export interface JobSummaryUrlReq {
  brandName: string;
  brandPositionName: string;
  url: string;
  platform: JobPlatformType;
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

export interface PagedResult<T> {
  items: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
}

export interface MemberJobSummaryListItem {
  memberJobSummaryId: number;
  jobSummaryId: number;
  brandName: string;
  positionName: string;
  brandPositionName: string;
  positionCategoryName: string;
  saveType: MemberJobSummarySaveType;
  createdAt: string;
}

export type HiringStageResult = 'PASSED' | 'FAILED' | 'PENDING';

export const HIRING_STAGE_RESULT_LABELS: Record<HiringStageResult, string> = {
  PASSED: '합격',
  FAILED: '불합격',
  PENDING: '대기중',
};

export interface HiringStageView {
  stage: HiringStage;
  note: string;
  result?: HiringStageResult | null;
  recordedAt: string;
}

export interface CoverLetterView {
  id: number;
  question: string;
  content: string;
  sortOrder: number;
}
