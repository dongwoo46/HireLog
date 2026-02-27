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
