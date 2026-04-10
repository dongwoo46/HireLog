import type { PagedResult } from './jobSummary';

export type BoardType = 'FREE';

export interface BoardItem {
  id: number;
  boardType: BoardType;
  title: string;
  content: string;
  authorUsername?: string | null;
  anonymous: boolean;
  notice: boolean;
  pinned: boolean;
  likeCount: number;
  commentCount: number;
  deleted: boolean;
  createdAt: string;
}

export interface BoardWriteReq {
  boardType: BoardType;
  title: string;
  content: string;
  anonymous: boolean;
  guestPassword?: string;
  notice?: boolean;
  pinned?: boolean;
}

export interface BoardLikeRes {
  likeCount: number;
  liked: boolean;
}

export interface CommentItem {
  id: number;
  boardId: number;
  authorUsername?: string | null;
  anonymous: boolean;
  content: string;
  likeCount: number;
  deleted: boolean;
  createdAt: string;
}

export interface CommentWriteReq {
  content: string;
  anonymous: boolean;
  guestPassword?: string;
}

export type BoardPagedResult = PagedResult<BoardItem>;
export type CommentPagedResult = PagedResult<CommentItem>;
