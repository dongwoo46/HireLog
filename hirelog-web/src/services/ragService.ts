import axios from 'axios';
import { apiClient } from '../utils/apiClient';
import type { RagAnswer, RagQueryReq } from '../types/rag';

interface ApiErrorPayload {
  code?: string;
  message?: string;
}

export const ragService = {
  query: async (payload: RagQueryReq): Promise<RagAnswer> => {
    const response = await apiClient.post<RagAnswer>('/rag/query', payload);
    return response.data;
  },
};

export const isRagRateLimitError = (error: unknown): boolean => {
  if (!axios.isAxiosError<ApiErrorPayload>(error)) return false;
  return error.response?.status === 429 && error.response?.data?.code === 'RAG_RATE_LIMIT_EXCEEDED';
};

export const getRagErrorMessage = (error: unknown, fallback = '\uC9C8\uBB38 \uCC98\uB9AC \uC911 \uC624\uB958\uAC00 \uBC1C\uC0DD\uD588\uC2B5\uB2C8\uB2E4.'): string => {
  if (!axios.isAxiosError<ApiErrorPayload>(error)) return fallback;
  return error.response?.data?.message || fallback;
};
