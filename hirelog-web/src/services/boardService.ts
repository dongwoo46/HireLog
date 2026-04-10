import { apiClient } from '../utils/apiClient';
import type {
  BoardItem,
  BoardLikeRes,
  BoardPagedResult,
  BoardType,
  BoardWriteReq,
  CommentItem,
  CommentPagedResult,
  CommentWriteReq,
} from '../types/board';

export const boardService = {
  getBoards: async (params?: {
    boardType?: BoardType;
    keyword?: string;
    sortBy?: 'LATEST' | 'LIKES';
    page?: number;
    size?: number;
  }): Promise<BoardPagedResult> => {
    const response = await apiClient.get('/boards', { params });
    return response.data;
  },

  getBoardDetail: async (boardId: number): Promise<BoardItem> => {
    const response = await apiClient.get(`/boards/${boardId}`);
    return response.data;
  },

  createBoard: async (payload: BoardWriteReq): Promise<{ id: number }> => {
    const response = await apiClient.post('/boards', payload);
    return response.data;
  },

  updateBoard: async (boardId: number, payload: BoardWriteReq): Promise<void> => {
    await apiClient.patch(`/boards/${boardId}`, payload);
  },

  deleteBoard: async (boardId: number, guestPassword?: string): Promise<void> => {
    await apiClient.delete(`/boards/${boardId}`, {
      params: guestPassword ? { guestPassword } : undefined,
    });
  },

  pinBoard: async (boardId: number, pinned: boolean): Promise<void> => {
    await apiClient.patch(`/admin/boards/${boardId}/pin`, null, { params: { pinned } });
  },

  getBoardLike: async (boardId: number): Promise<BoardLikeRes> => {
    const response = await apiClient.get(`/boards/${boardId}/like`);
    return response.data;
  },

  likeBoard: async (boardId: number): Promise<BoardLikeRes> => {
    const response = await apiClient.post(`/boards/${boardId}/like`);
    return response.data;
  },

  unlikeBoard: async (boardId: number): Promise<BoardLikeRes> => {
    const response = await apiClient.delete(`/boards/${boardId}/like`);
    return response.data;
  },

  getComments: async (boardId: number, params?: { page?: number; size?: number }): Promise<CommentPagedResult> => {
    const response = await apiClient.get(`/boards/${boardId}/comments`, { params });
    return response.data;
  },

  createComment: async (boardId: number, payload: CommentWriteReq): Promise<{ id: number }> => {
    const response = await apiClient.post(`/boards/${boardId}/comments`, payload);
    return response.data;
  },

  updateComment: async (boardId: number, commentId: number, payload: CommentWriteReq): Promise<void> => {
    await apiClient.patch(`/boards/${boardId}/comments/${commentId}`, payload);
  },

  deleteComment: async (boardId: number, commentId: number, guestPassword?: string): Promise<void> => {
    await apiClient.delete(`/boards/${boardId}/comments/${commentId}`, {
      params: guestPassword ? { guestPassword } : undefined,
    });
  },

  likeComment: async (boardId: number, commentId: number): Promise<BoardLikeRes> => {
    const response = await apiClient.post(`/boards/${boardId}/comments/${commentId}/like`);
    return response.data;
  },

  unlikeComment: async (boardId: number, commentId: number): Promise<BoardLikeRes> => {
    const response = await apiClient.delete(`/boards/${boardId}/comments/${commentId}/like`);
    return response.data;
  },
};

export type { BoardItem, CommentItem };
