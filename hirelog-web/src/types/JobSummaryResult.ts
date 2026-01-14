export interface JobSummaryResult {
  brandName: string;
  position: string;
  summary: string;
  responsibilities: string;
  requiredQualifications: string;
  preferredQualifications?: string | null;
  techStack?: string | null;
  recruitmentProcess?: string | null;
}
