export interface JobSummaryResult {
  summary: string;
  responsibilities: string;
  requiredQualifications: string;
  preferredQualifications?: string | null;
  techStack?: string | null;
  recruitmentProcess?: string | null;
}
