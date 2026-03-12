export interface MemberDetailView {
  id: number;
  email: string;
  username: string;

  role: 'ADMIN' | 'USER';
  status: 'ACTIVE' | 'INACTIVE' | 'SUSPENDED';

  currentPosition?: {
    id: number;
    name: string;
  };

  careerYears?: number | null;
  summary?: string | null;
  createdAt?: string;
}

export interface UpdateProfileReq {
  careerYears?: number;
  summary?: string;
  currentPositionId?: number;
}

export interface UpdateUsernameReq {
  username: string;
}

export interface UpdateEmailReq {
  email: string;
}

export interface ChangePasswordReq {
  oldPassword?: string;
  newPassword?: string;
}
