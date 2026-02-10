export interface MemberDetailView {
  id: number;
  email: string;
  username: string;
  name: string;
  currentPositionId?: number;
  careerYears?: number;
  summary?: string;
  avatarUrl?: string;
}

export interface UpdateProfileReq {
  currentPositionId?: number;
  careerYears?: number;
  summary?: string;
}

export interface UpdateUsernameReq {
  username: string;
}
