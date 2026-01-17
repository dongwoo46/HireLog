import type { JobSummaryResult } from '../types/JobSummaryResult';
import type { SummaryTextRequest } from '../types/SummaryTextRequest';
import { apiClient } from '../utils/apiClient';

export const summaryJobDescription = async (
  payload: SummaryTextRequest
): Promise<JobSummaryResult> => {
  const res = await apiClient.post<JobSummaryResult>(
    '/gemini/summary/text',
    payload
  );
  return res.data;
};
