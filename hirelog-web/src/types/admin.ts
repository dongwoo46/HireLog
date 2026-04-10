export type VerificationStatus = 'VERIFIED' | 'UNVERIFIED' | 'REJECTED';
export type BrandSource = 'USER' | 'ADMIN' | 'OFFICIAL' | 'EXTERNAL_DATA' | 'INFERRED';
export type CompanySource = 'FAIR_TRADE_COMMISSION' | 'NATIONAL_TAX_SERVICE' | 'ADMIN' | 'LLM';
export type MemberRole = 'ADMIN' | 'USER';
export type MemberStatus = 'ACTIVE' | 'SUSPENDED' | 'DELETED';

export interface BrandListView {
    id: number;
    name: string;
    verificationStatus: VerificationStatus;
    source: BrandSource;
    isActive: boolean;
}

export interface CompanyView {
    id: number;
    name: string;
    source: CompanySource;
    isActive: boolean;
}

export interface MemberSummaryView {
    id: number;
    email: string;
    username: string;
    role: MemberRole;
    status: MemberStatus;
}

export interface AdminPagedResult<T> {
    items: T[];
    totalElements: number;
    totalPages: number;
    size: number;
    number: number;
}

export interface AdminReviewView {
    id: number;
    jobSummaryId: number;
    brandName?: string | null;
    brandPositionName?: string | null;
    anonymous: boolean;
    memberId?: number;
    memberName?: string;
    hiringStage: string;
    difficultyRating: number;
    satisfactionRating: number;
    prosComment: string;
    consComment: string;
    tip?: string;
    likeCount: number;
    createdAt: string;
    deleted: boolean;
}

export interface AdminJobSummaryDirectCreateReq {
    brandName: string;
    positionName: string;
    jdText: string;
    sourceUrl?: string;
}

export interface AdminJobSummaryView {
    summaryId: number;
    brandId: number;
    brandName: string;
    positionId: number;
    positionName: string;
    positionCategoryName: string;
    careerType: string;
    careerYears?: string | null;
    isActive: boolean;
    sourceUrl?: string | null;
    createdAt: string;
}

export type ReportStatus = 'PENDING' | 'REVIEWED' | 'RESOLVED' | 'REJECTED';
export type ReportTargetType = 'JOB_SUMMARY' | 'JOB_SUMMARY_REVIEW' | 'MEMBER' | 'BOARD' | 'COMMENT';
export type ReportProcessType = 'REVIEW' | 'RESOLVE' | 'RESOLVE_AND_DELETE_TARGET' | 'REJECT';

export interface AdminReportView {
    id: number;
    reporterId: number;
    reporterUsername: string;
    targetType: ReportTargetType;
    targetId: number;
    targetLabel?: string | null;
    reason: string;
    detail?: string | null;
    status: ReportStatus;
    reviewedAt?: string | null;
    createdAt: string;
}
