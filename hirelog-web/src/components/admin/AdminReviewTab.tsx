import { useEffect, useState } from 'react';
import { TbRotateClockwise, TbSearch, TbTrash } from 'react-icons/tb';
import { toast } from 'react-toastify';
import { adminService } from '../../services/adminService';
import { HIRING_STAGE_LABELS, type HiringStage, type ReviewSortType } from '../../types/jobSummary';
import type { AdminPagedResult, AdminReviewView } from '../../types/admin';

type ReviewFilter = {
  jobSummaryId?: number;
  memberName?: string;
  sortBy: ReviewSortType;
  includeDeleted: boolean;
};

export default function AdminReviewTab() {
  const [jobSummaryIdInput, setJobSummaryIdInput] = useState('');
  const [memberNameInput, setMemberNameInput] = useState('');
  const [filters, setFilters] = useState<ReviewFilter>({
    sortBy: 'LATEST',
    includeDeleted: false,
  });
  const [page, setPage] = useState(0);
  const [size] = useState(20);
  const [result, setResult] = useState<AdminPagedResult<AdminReviewView> | null>(null);
  const [loading, setLoading] = useState(false);

  const fetchReviews = async (targetPage = page, targetFilters = filters) => {
    try {
      setLoading(true);
      const data = await adminService.getAllReviews(targetPage, size, targetFilters);
      setResult(data);
    } catch {
      toast.error('리뷰 조회에 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchReviews(0, filters);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const handleSearch = async () => {
    const nextFilters: ReviewFilter = {
      sortBy: filters.sortBy,
      includeDeleted: filters.includeDeleted,
      jobSummaryId: jobSummaryIdInput.trim() ? Number(jobSummaryIdInput) : undefined,
      memberName: memberNameInput.trim() || undefined,
    };
    setFilters(nextFilters);
    setPage(0);
    await fetchReviews(0, nextFilters);
  };

  const handleDelete = async (id: number) => {
    if (!confirm('리뷰를 삭제할까요?')) return;
    try {
      await adminService.deleteReview(id);
      toast.success('리뷰를 삭제했습니다.');
      await fetchReviews(page, filters);
    } catch {
      toast.error('리뷰 삭제에 실패했습니다.');
    }
  };

  const handleRestore = async (id: number) => {
    try {
      await adminService.restoreReview(id);
      toast.success('리뷰를 복구했습니다.');
      await fetchReviews(page, filters);
    } catch {
      toast.error('리뷰 복구에 실패했습니다.');
    }
  };

  const items = result?.items || [];
  const totalPages = result?.totalPages ?? 1;

  return (
    <div className="space-y-6">
      <div className="rounded-2xl border border-gray-100 bg-white p-5">
        <h2 className="mb-3 text-lg font-bold text-gray-900">리뷰 관리</h2>
        <div className="grid gap-2 md:grid-cols-4">
          <input
            type="number"
            value={jobSummaryIdInput}
            onChange={(e) => setJobSummaryIdInput(e.target.value)}
            placeholder="JobSummary ID"
            className="rounded-xl border border-gray-200 px-3 py-2 text-sm"
          />
          <input
            type="text"
            value={memberNameInput}
            onChange={(e) => setMemberNameInput(e.target.value)}
            placeholder="작성자명"
            className="rounded-xl border border-gray-200 px-3 py-2 text-sm"
          />
          <select
            value={filters.sortBy}
            onChange={(e) => setFilters((prev) => ({ ...prev, sortBy: e.target.value as ReviewSortType }))}
            className="rounded-xl border border-gray-200 px-3 py-2 text-sm"
          >
            <option value="LATEST">LATEST</option>
            <option value="LIKES">LIKES</option>
            <option value="RATING">RATING</option>
            <option value="DIFFICULTY">DIFFICULTY</option>
            <option value="SATISFACTION">SATISFACTION</option>
          </select>
          <label className="flex items-center gap-2 rounded-xl border border-gray-200 px-3 py-2 text-sm">
            <input
              type="checkbox"
              checked={filters.includeDeleted}
              onChange={(e) => setFilters((prev) => ({ ...prev, includeDeleted: e.target.checked }))}
            />
            삭제 포함
          </label>
        </div>
        <div className="mt-3">
          <button
            onClick={handleSearch}
            className="flex items-center gap-2 rounded-xl bg-[#3FB6B2] px-4 py-2 text-sm font-semibold text-white"
          >
            <TbSearch size={16} /> 조회
          </button>
        </div>
      </div>

      {loading ? (
        <div className="py-10 text-center text-sm text-gray-400">리뷰를 불러오는 중...</div>
      ) : !items.length ? (
        <div className="rounded-2xl border border-dashed border-gray-200 bg-gray-50 py-16 text-center text-sm text-gray-400">
          조회된 리뷰가 없습니다.
        </div>
      ) : (
        <div className="space-y-4">
          {items.map((review) => (
            <div key={review.id} className={`rounded-2xl border bg-white p-5 ${review.deleted ? 'opacity-70' : ''}`}>
              <div className="mb-3 flex items-center justify-between">
                <div className="text-sm font-semibold text-gray-800">
                  JD #{review.jobSummaryId} {review.brandPositionName ? `· ${review.brandPositionName}` : ''}
                </div>
                <div className="text-xs text-gray-500">
                  좋아요 {review.likeCount} · {new Date(review.createdAt).toLocaleDateString()}
                </div>
              </div>

              <div className="mb-2 text-sm text-gray-700">
                {HIRING_STAGE_LABELS[review.hiringStage as HiringStage] || review.hiringStage}
                {' · '}난이도 {review.difficultyRating}
                {' · '}만족도 {review.satisfactionRating}
              </div>
              <div className="mb-2 text-sm text-gray-600">
                작성자: {review.memberName || '익명'} ({review.memberId ?? '-'})
              </div>

              <div className="space-y-2 rounded-xl border border-gray-100 bg-gray-50 p-3 text-sm">
                <div><strong>장점:</strong> {review.prosComment}</div>
                <div><strong>단점:</strong> {review.consComment}</div>
                {review.tip && <div><strong>팁:</strong> {review.tip}</div>}
              </div>

              <div className="mt-3 flex justify-end gap-2">
                {review.deleted ? (
                  <button
                    onClick={() => handleRestore(review.id)}
                    className="rounded-lg p-2 text-gray-500 hover:bg-[#3FB6B2]/10 hover:text-[#3FB6B2]"
                    title="복구"
                  >
                    <TbRotateClockwise size={18} />
                  </button>
                ) : (
                  <button
                    onClick={() => handleDelete(review.id)}
                    className="rounded-lg p-2 text-gray-500 hover:bg-red-50 hover:text-red-500"
                    title="삭제"
                  >
                    <TbTrash size={18} />
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
            fetchReviews(next, filters);
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
            fetchReviews(next, filters);
          }}
          className="rounded-lg border border-gray-200 px-3 py-1.5 text-xs font-semibold disabled:opacity-50"
        >
          다음
        </button>
      </div>
    </div>
  );
}
