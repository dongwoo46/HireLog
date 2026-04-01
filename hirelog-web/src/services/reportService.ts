import { apiClient } from '../utils/apiClient';
import type { ReportWriteReq } from '../types/report';

export const reportService = {
  createReport: async (payload: ReportWriteReq): Promise<{ id: number }> => {
    const response = await apiClient.post('/reports', payload);
    return response.data;
  },
};
