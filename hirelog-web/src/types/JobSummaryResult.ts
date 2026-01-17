export interface JobSummaryResult {
  brandName: string;
  position: string;

  careerType: CareerType;
  careerYears?: number;

  summary: string;
  responsibilities: string;
  requiredQualifications: string;
  preferredQualifications?: string;
  techStack?: string;
  recruitmentProcess?: string;
}

export type CareerType = 'NEW' | 'EXPERIENCED' | 'ANY';
