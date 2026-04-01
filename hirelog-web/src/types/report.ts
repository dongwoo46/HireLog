export type ReportTargetType = 'JOB_SUMMARY' | 'JOB_SUMMARY_REVIEW' | 'MEMBER' | 'BOARD' | 'COMMENT';
export type ReportReason = 'SPAM' | 'INAPPROPRIATE' | 'FALSE_INFO' | 'COPYRIGHT' | 'OTHER';

export interface ReportWriteReq {
  targetType: ReportTargetType;
  targetId: number;
  reason: ReportReason;
  detail?: string;
}
