import { useEffect, useState } from 'react';
import { TbCheck, TbTrash, TbX, TbZoomCheck } from 'react-icons/tb';
import { toast } from 'react-toastify';
import { adminService } from '../../services/adminService';
import type {
  AdminPagedResult,
  AdminReportView,
  ReportProcessType,
  ReportStatus,
  ReportTargetType,
} from '../../types/admin';

const STATUS_LABEL: Record<ReportStatus, string> = {
  PENDING: '대기',
  REVIEWED: '검토',
  RESOLVED: '처리완료',
  REJECTED: '반려',
};

const TARGET_LABEL: Record<ReportTargetType, string> = {
  JOB_SUMMARY: '요약',
  JOB_SUMMARY_REVIEW: '리뷰',
  MEMBER: '회원',
  BOARD: '게시글',
  COMMENT: '댓글',
};

const RESOLVE_DELETE_TARGET_TYPES: ReportTargetType[] = ['JOB_SUMMARY', 'BOARD', 'COMMENT'];

export default function AdminReportTab() {
  const [status, setStatus] = useState<'' | ReportStatus>('');
  const [targetType, setTargetType] = useState<'' | ReportTargetType>('');
  const [page, setPage] = useState(0);
  const [size] = useState(20);
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState<AdminPagedResult<AdminReportView> | null>(null);

  const fetchReports = async (targetPage = page, targetStatus = status, targetTypeFilter = targetType) => {
    try {
      setLoading(true);
      const data = await adminService.getAllReports(targetPage, size, {
        status: targetStatus || undefined,
        targetType: targetTypeFilter || undefined,
      });
      setResult(data);
    } catch {
      toast.error('신고 목록 조회에 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchReports(0, status, targetType);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const handleSearch = async () => {
    setPage(0);
    await fetchReports(0, status, targetType);
  };

  const handleProcess = async (report: AdminReportView, processType: ReportProcessType) => {
    const actionLabel =
      processType === 'REVIEW'
        ? '검토'
        : processType === 'RESOLVE'
          ? '처리완료'
          : processType === 'REJECT'
            ? '반려'
            : '처리+대상삭제';

    if (processType === 'RESOLVE_AND_DELETE_TARGET') {
      const ok = confirm('신고 처리와 함께 대상 콘텐츠를 삭제합니다. 계속할까요?');
      if (!ok) return;
    }

    try {
      await adminService.processReport(report.id, processType);
      toast.success(`신고 ${actionLabel} 처리 완료`);
      await fetchReports(page, status, targetType);
    } catch {
      toast.error(`신고 ${actionLabel} 처리에 실패했습니다.`);
    }
  };

  const items = result?.items || [];
  const totalPages = result?.totalPages ?? 1;

  return (
    <div className="space-y-6">
      <div className="rounded-2xl border border-gray-100 bg-white p-5">
        <h2 className="mb-3 text-lg font-bold text-gray-900">신고 처리</h2>
        <div className="grid gap-2 md:grid-cols-3">
          <select
            value={status}
            onChange={(e) => setStatus((e.target.value || '') as '' | ReportStatus)}
            className="rounded-xl border border-gray-200 px-3 py-2 text-sm"
          >
            <option value="">전체 상태</option>
            <option value="PENDING">대기</option>
            <option value="REVIEWED">검토</option>
            <option value="RESOLVED">처리완료</option>
            <option value="REJECTED">반려</option>
          </select>

          <select
            value={targetType}
            onChange={(e) => setTargetType((e.target.value || '') as '' | ReportTargetType)}
            className="rounded-xl border border-gray-200 px-3 py-2 text-sm"
          >
            <option value="">전체 대상</option>
            <option value="JOB_SUMMARY">요약</option>
            <option value="JOB_SUMMARY_REVIEW">리뷰</option>
            <option value="MEMBER">회원</option>
            <option value="BOARD">게시글</option>
            <option value="COMMENT">댓글</option>
          </select>

          <button
            onClick={handleSearch}
            className="rounded-xl bg-[#3FB6B2] px-4 py-2 text-sm font-semibold text-white"
          >
            필터 적용
          </button>
        </div>
      </div>

      {loading ? (
        <div className="py-10 text-center text-sm text-gray-400">신고 목록을 불러오는 중...</div>
      ) : !items.length ? (
        <div className="rounded-2xl border border-dashed border-gray-200 bg-gray-50 py-16 text-center text-sm text-gray-400">
          신고 데이터가 없습니다.
        </div>
      ) : (
        <div className="space-y-4">
          {items.map((report) => (
            <div key={report.id} className="rounded-2xl border border-gray-100 bg-white p-5">
              <div className="mb-2 flex items-center justify-between">
                <div className="text-sm font-semibold text-gray-900">
                  신고 #{report.id} · {TARGET_LABEL[report.targetType]} #{report.targetId}
                </div>
                <div className="rounded-full bg-gray-100 px-2.5 py-1 text-xs font-semibold text-gray-700">
                  {STATUS_LABEL[report.status]}
                </div>
              </div>

              <div className="text-sm text-gray-600">
                신고자: {report.reporterUsername} (#{report.reporterId})
              </div>
              <div className="text-sm text-gray-600">
                대상: {report.targetLabel || '-'}
              </div>
              <div className="text-sm text-gray-600">사유: {report.reason}</div>
              {report.detail && <div className="text-sm text-gray-600">상세: {report.detail}</div>}
              <div className="mt-2 text-xs text-gray-500">
                접수: {new Date(report.createdAt).toLocaleString()}
                {report.reviewedAt ? ` · 처리: ${new Date(report.reviewedAt).toLocaleString()}` : ''}
              </div>

              <div className="mt-4 flex flex-wrap justify-end gap-2">
                {report.status === 'PENDING' && (
                  <button
                    onClick={() => handleProcess(report, 'REVIEW')}
                    className="inline-flex items-center gap-1 rounded-lg border border-blue-200 px-3 py-1.5 text-xs font-semibold text-blue-700 hover:bg-blue-50"
                  >
                    <TbZoomCheck size={14} />
                    검토
                  </button>
                )}

                {report.status !== 'RESOLVED' && (
                  <button
                    onClick={() => handleProcess(report, 'RESOLVE')}
                    className="inline-flex items-center gap-1 rounded-lg border border-emerald-200 px-3 py-1.5 text-xs font-semibold text-emerald-700 hover:bg-emerald-50"
                  >
                    <TbCheck size={14} />
                    처리완료
                  </button>
                )}

                {report.status !== 'REJECTED' && (
                  <button
                    onClick={() => handleProcess(report, 'REJECT')}
                    className="inline-flex items-center gap-1 rounded-lg border border-rose-200 px-3 py-1.5 text-xs font-semibold text-rose-700 hover:bg-rose-50"
                  >
                    <TbX size={14} />
                    반려
                  </button>
                )}

                {RESOLVE_DELETE_TARGET_TYPES.includes(report.targetType) && (
                  <button
                    onClick={() => handleProcess(report, 'RESOLVE_AND_DELETE_TARGET')}
                    className="inline-flex items-center gap-1 rounded-lg border border-red-200 px-3 py-1.5 text-xs font-semibold text-red-700 hover:bg-red-50"
                  >
                    <TbTrash size={14} />
                    대상 삭제 + 처리
                  </button>
                )}
              </div>
            </div>
          ))}
        </div>
      )}

      <div className="flex items-center justify-end gap-2">
        <button
          type="button"
          disabled={page <= 0}
          onClick={() => {
            const next = Math.max(0, page - 1);
            setPage(next);
            fetchReports(next, status, targetType);
          }}
          className="rounded-lg border border-gray-200 px-3 py-1.5 text-xs font-semibold disabled:opacity-50"
        >
          이전
        </button>
        <span className="text-xs text-gray-500">
          {page + 1} / {Math.max(1, totalPages)}
        </span>
        <button
          type="button"
          disabled={page + 1 >= totalPages}
          onClick={() => {
            const next = page + 1;
            setPage(next);
            fetchReports(next, status, targetType);
          }}
          className="rounded-lg border border-gray-200 px-3 py-1.5 text-xs font-semibold disabled:opacity-50"
        >
          다음
        </button>
      </div>
    </div>
  );
}
