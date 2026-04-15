export type RagIntent = 'DOCUMENT_SEARCH' | 'EXPERIENCE_ANALYSIS' | 'STATISTICS' | 'SUMMARY';

export type RagEvidenceType = 'DOCUMENT' | 'AGGREGATION' | 'EXPERIENCE';

export interface RagEvidence {
  type: RagEvidenceType;
  summary: string;
  detail: string | null;
  sourceId: number | null;
}

export interface RagSource {
  id: number;
  positionName: string;
}

export interface RagAnswer {
  answer: string;
  intent: RagIntent;
  reasoning: string | null;
  evidences: RagEvidence[] | null;
  sources: RagSource[] | null;
}

export interface RagQueryReq {
  question: string;
}
