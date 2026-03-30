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
    anonymous: boolean;
    memberId?: number;
    memberName?: string;
    hiringStage: string;
    difficultyRating: number;
    satisfactionRating: number;
    experienceComment: string;
    interviewTip?: string;
    createdAt: string;
    deleted: boolean;
}

export interface AdminJobSummaryDirectCreateReq {
    brandName: string;
    positionName: string;
    jdText: string;
    sourceUrl?: string;
}
