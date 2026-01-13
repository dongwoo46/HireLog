import { useMutation } from '@tanstack/react-query';
import { summaryJobDescription } from '../services/gemini.service';

export const useSummaryJobDescription = () => {
  return useMutation({
    mutationFn: summaryJobDescription,
  });
};
