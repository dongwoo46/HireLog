export type CareerType = 'NEW' | 'EXPERIENCED' | 'ANY';

export type CompanySizeType =
  | 'SEED'
  | 'EARLY_STARTUP'
  | 'GROWTH_STARTUP'
  | 'SCALE_UP'
  | 'MID_SIZED'
  | 'LARGE_CORP'
  | 'FOREIGN_CORP'
  | 'UNKNOWN';

export const COMPANY_SIZE_LABELS: Record<CompanySizeType, string> = {
  SEED: '시드 스타트업',
  EARLY_STARTUP: '초기 스타트업',
  GROWTH_STARTUP: '성장 스타트업',
  SCALE_UP: '스케일업',
  MID_SIZED: '중소/중견기업',
  LARGE_CORP: '대기업',
  FOREIGN_CORP: '외국계',
  UNKNOWN: '확인 불가',
};
export type MemberJobSummarySaveType = 'SAVED' | 'APPLY' | 'UNSAVED';

export interface JobSummarySearchReq {
  keyword?: string;
  careerType?: CareerType;
  careerTypes?: CareerType[];

  // ID Filters
  brandId?: number;
  companyId?: number;
  positionId?: number;
  brandPositionId?: number;
  positionCategoryId?: number;
  brandIds?: number[];
  companyIds?: number[];
  positionIds?: number[];
  brandPositionIds?: number[];
  positionCategoryIds?: number[];

  // Name Filters
  brandName?: string;
  positionName?: string;
  brandPositionName?: string;
  positionCategoryName?: string;
  brandNames?: string[];
  positionNames?: string[];
  brandPositionNames?: string[];
  positionCategoryNames?: string[];

  techStacks?: string[];
  companyDomains?: string[];
  companySizes?: string[];
  sortBy?: string; // e.g., "CREATED_AT_DESC"
  cursor?: string;
  size?: number;
  isSaved?: boolean;
}

export interface JobSummaryView {
  id: number;
  brandName: string;
  brandPositionName: string;
  companyDomain?: string;
  companySize?: string;
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
  // AI 분석 필드
  idealCandidate?: string;
  mustHaveSignals?: string;
  transferableStrengthsAndGapPlan?: string;
  storyAngles?: string;
  keyChallenges?: string;
  technicalContext?: string;
  considerations?: string;
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
  prosComment: string;
  consComment: string;
  tip?: string;
}

export type ReviewSortType = 'LATEST' | 'LIKES' | 'RATING' | 'DIFFICULTY' | 'SATISFACTION';

export interface JobSummaryReviewView {
  id: number;
  anonymous: boolean;
  memberId?: number | null;
  memberName?: string | null;
  hiringStage: HiringStage | string;
  difficultyRating: number;
  satisfactionRating: number;
  prosComment: string;
  consComment: string;
  tip?: string | null;
  likeCount: number;
  deleted: boolean;
  createdAt: string;
}

export interface ReviewLikeStat {
  reviewId: number;
  likeCount: number;
  likedByMe: boolean;
}

export interface JobSummaryReviewSearchParams {
  hiringStage?: HiringStage;
  minDifficultyRating?: number;
  maxDifficultyRating?: number;
  minSatisfactionRating?: number;
  maxSatisfactionRating?: number;
  sortBy?: ReviewSortType;
  createdFrom?: string;
  createdTo?: string;
  includeDeleted?: boolean;
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
  careerType?: CareerType;
  companyDomain?: string;
  companySize?: string;
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
