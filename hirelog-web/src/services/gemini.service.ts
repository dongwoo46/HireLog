import type { JobSummaryResult } from '../types/JobSummaryResult';
import { apiClient } from './apiClient';

export const summaryJobDescription = async (
  jdText: string
): Promise<JobSummaryResult> => {
  const res = await apiClient.post<JobSummaryResult>('/gemini/summary', {
    jdText,
  });

  return res.data;
};
