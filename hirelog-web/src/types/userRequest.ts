export type UserRequestType = 'MODIFY_REQUEST' | 'ERROR_REPORT' | 'FEATURE_REQUEST' | 'REPROCESS_REQUEST';
export type UserRequestStatus = 'PENDING' | 'IN_PROGRESS' | 'COMPLETED' | 'REJECTED';

export interface UserRequestCreateReq {
  requestType: UserRequestType;
  title: string;
  content: string;
}

export interface UserRequestListRes {
  id: number;
  title: string;
  requestType: UserRequestType;
  status: UserRequestStatus;
  createdAt: string;
}

export interface UserRequestComment {
  id: number;
  content: string;
  authorName: string;
  createdAt: string;
  isAdmin: boolean;
}

export interface UserRequestDetailRes {
  id: number;
  title: string;
  content: string;
  requestType: UserRequestType;
  status: UserRequestStatus;
  createdAt: string;
  authorName: string;
  comments: UserRequestComment[];
}

export interface UserRequestStatusUpdateReq {
  status: UserRequestStatus;
}

export interface UserRequestCommentCreateReq {
  content: string;
}
