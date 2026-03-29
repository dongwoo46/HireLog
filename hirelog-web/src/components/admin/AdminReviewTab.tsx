import { useState } from 'react';
import { TbRotateClockwise, TbSearch, TbTrash } from 'react-icons/tb';
import { toast } from 'react-toastify';
import { adminService } from '../../services/adminService';
import { jdSummaryService } from '../../services/jdSummaryService';
import { HIRING_STAGE_LABELS, type HiringStage } from '../../types/jobSummary';
import type { AdminReviewView } from '../../types/admin';

export default function AdminReviewTab() {
  const [jobSummaryId, setJobSummaryId] = useState('');
  const [reviews, setReviews] = useState<AdminReviewView[]>([]);
  const [loading, setLoading] = useState(false);

  const search = async () => {
    const summaryId = Number(jobSummaryId);
    if (!summaryId) {
      toast.warn('조회할 Job Summary ID를 입력해 주세요.');
      return;
    }

    try {
      setLoading(true);
      const data = await jdSummaryService.getReviews(summaryId, 0, 100);
      setReviews(data.items || []);
      if (!data.items?.length) {
        toast.info('해당 공고의 리뷰가 없습니다.');
      }
    } catch {
      toast.error('리뷰를 조회하지 못했습니다.');
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (id: number) => {
    if (!confirm('이 리뷰를 삭제할까요?')) return;
    try {
      await adminService.deleteReview(id);
      toast.success('리뷰를 삭제했습니다.');
      await search();
    } catch {
      toast.error('리뷰 삭제에 실패했습니다.');
    }
  };

  const handleRestore = async (id: number) => {
    try {
      await adminService.restoreReview(id);
      toast.success('리뷰를 복구했습니다.');
      await search();
    } catch {
      toast.error('리뷰 복구에 실패했습니다.');
    }
  };

  return (
    <div className="space-y-6">
      <div className="rounded-2xl border border-gray-100 bg-white p-5">
        <h2 className="mb-3 text-lg font-bold text-gray-900">리뷰 관리</h2>
        <p className="mb-4 text-sm text-gray-500">
          현재 API는 전체 리뷰 목록 엔드포인트를 제공하지 않아, Job Summary ID 기준으로 조회 후 관리합니다.
        </p>
        <div className="flex gap-2">
          <input
            type="number"
            value={jobSummaryId}
            onChange={(e) => setJobSummaryId(e.target.value)}
            placeholder="예: 123"
            className="w-56 rounded-xl border border-gray-200 px-3 py-2 text-sm"
          />
          <button
            onClick={search}
            className="flex items-center gap-2 rounded-xl bg-[#3FB6B2] px-4 py-2 text-sm font-semibold text-white"
          >
            <TbSearch size={16} /> 조회
          </button>
        </div>
      </div>

      {loading ? (
        <div className="py-10 text-center text-sm text-gray-400">리뷰를 불러오는 중...</div>
      ) : !reviews.length ? (
        <div className="rounded-2xl border border-dashed border-gray-200 bg-gray-50 py-16 text-center text-sm text-gray-400">
          조회된 리뷰가 없습니다.
        </div>
      ) : (
        <div className="overflow-x-auto rounded-2xl border border-gray-100 bg-white">
          <table className="w-full text-left">
            <thead>
              <tr className="border-b border-gray-100 bg-gray-50 text-xs font-semibold uppercase tracking-wider text-gray-500">
                <th className="px-4 py-3">상태</th>
                <th className="px-4 py-3">단계/평점</th>
                <th className="px-4 py-3">내용</th>
                <th className="px-4 py-3">작성자</th>
                <th className="px-4 py-3 text-right">관리</th>
              </tr>
            </thead>
            <tbody>
              {reviews.map((review) => (
                <tr key={review.id} className={`border-b border-gray-50 text-sm ${review.deleted ? 'opacity-60' : ''}`}>
                  <td className="px-4 py-3">
                    <span
                      className={`rounded-md px-2 py-1 text-[11px] font-bold ${
                        review.deleted ? 'bg-red-50 text-red-600' : 'bg-[#3FB6B2]/10 text-[#3FB6B2]'
                      }`}
                    >
                      {review.deleted ? '삭제됨' : '활성'}
                    </span>
                  </td>
                  <td className="px-4 py-3">
                    <div className="font-semibold text-gray-900">
                      {HIRING_STAGE_LABELS[review.hiringStage as HiringStage] || review.hiringStage}
                    </div>
                    <div className="text-xs text-gray-500">
                      난이도 {review.difficultyRating} / 만족도 {review.satisfactionRating}
                    </div>
                  </td>
                  <td className="max-w-[420px] px-4 py-3 text-gray-700">
                    <p className="line-clamp-2">{review.experienceComment}</p>
                  </td>
                  <td className="px-4 py-3">
                    <div className="text-gray-900">{review.memberName || '익명'}</div>
                    <div className="text-xs text-gray-500">{new Date(review.createdAt).toLocaleDateString()}</div>
                  </td>
                  <td className="px-4 py-3">
                    <div className="flex justify-end gap-2">
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
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
