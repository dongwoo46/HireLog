export interface MemberDetailView {
  id: number;
  email: string;
  username: string;
  name: string;
  role: 'ADMIN' | 'USER';
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

export interface UpdateEmailReq {
  email: string;
}

export interface ChangePasswordReq {
  currentPassword?: string;
  newPassword?: string;
}
