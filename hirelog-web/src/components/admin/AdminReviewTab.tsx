import { useState, useEffect, useCallback } from 'react';
import { adminService } from '../../services/adminService';
import type { AdminReviewView } from '../../types/admin';
import { TbTrash, TbRotateClockwise } from 'react-icons/tb';
import { toast } from 'react-toastify';
import { HIRING_STAGE_LABELS, type HiringStage } from '../../types/jobSummary';

export default function AdminReviewTab() {
    const [reviews, setReviews] = useState<AdminReviewView[]>([]);
    const [loading, setLoading] = useState(true);
    const [page, setPage] = useState(0);
    const [totalElements, setTotalElements] = useState(0);

    const fetchReviews = useCallback(async () => {
        try {
            setLoading(true);
            const data = await adminService.getAllReviews(page, 20);
            setReviews(data.items);
            setTotalElements(data.totalElements);
        } catch (error) {
            toast.error('리뷰 목록을 불러오는데 실패했습니다.');
        } finally {
            setLoading(false);
        }
    }, [page]);

    useEffect(() => {
        fetchReviews();
    }, [fetchReviews]);

    const handleDelete = async (id: number) => {
        if (!confirm('이 리뷰를 숨기시겠습니까?')) return;
        try {
            await adminService.deleteReview(id);
            toast.success('리뷰가 숨겨졌습니다.');
            fetchReviews();
        } catch (error) {
            toast.error('작업에 실패했습니다.');
        }
    };

    const handleRestore = async (id: number) => {
        try {
            await adminService.restoreReview(id);
            toast.success('리뷰가 복구되었습니다.');
            fetchReviews();
        } catch (error) {
            toast.error('작업에 실패했습니다.');
        }
    };

    return (
        <div className="space-y-6">
            <div className="flex justify-between items-center px-4">
                <h2 className="text-xl font-bold text-gray-900">전체 리뷰 관리</h2>
                {!loading && !reviews.length && <span className="text-sm text-gray-400">데이터가 없습니다.</span>}
                <span className="text-sm text-gray-500">총 {totalElements}개</span>
            </div>

            {loading ? (
                <div className="flex flex-col justify-center items-center h-96 gap-4">
                    <div className="w-12 h-12 border-4 border-gray-100 border-t-[#3FB6B2] rounded-full animate-spin" />
                    <p className="text-sm text-gray-400 font-bold animate-pulse">데이터를 안전하게 불러오는 중...</p>
                </div>
            ) : !reviews.length ? (
                <div className="flex flex-col items-center justify-center h-96 bg-gray-50/50 rounded-[2.5rem] border border-dashed border-gray-200">
                    <div className="w-16 h-16 bg-gray-100 rounded-2xl flex items-center justify-center text-gray-300 mb-4">
                        <TbTrash size={32} />
                    </div>
                    <h3 className="text-lg font-bold text-gray-900 mb-1">리뷰 목록이 비어있습니다</h3>
                    <p className="text-sm text-gray-400 mb-6">아직 작성된 리뷰가 없거나 목록을 불러오지 못했습니다.</p>
                    <button
                        onClick={() => fetchReviews()}
                        className="px-6 py-2 bg-white border border-gray-200 text-gray-600 rounded-xl text-sm font-bold hover:bg-gray-50 transition-all flex items-center gap-2"
                    >
                        <TbRotateClockwise size={16} />
                        다시 시도하기
                    </button>
                </div>
            ) : (
                <div className="overflow-x-auto">
                    <table className="w-full text-left border-collapse">
                        <thead>
                            <tr className="bg-gray-50 border-y border-gray-100">
                                <th className="px-6 py-4 text-xs font-semibold text-gray-600 uppercase tracking-wider">상태</th>
                                <th className="px-6 py-4 text-xs font-semibold text-gray-600 uppercase tracking-wider">단계/평점</th>
                                <th className="px-6 py-4 text-xs font-semibold text-gray-600 uppercase tracking-wider">내용</th>
                                <th className="px-6 py-4 text-xs font-semibold text-gray-600 uppercase tracking-wider">작성자</th>
                                <th className="px-6 py-4 text-xs font-semibold text-gray-600 uppercase tracking-wider text-right">관리</th>
                            </tr>
                        </thead>
                        <tbody className="divide-y divide-gray-50">
                            {reviews.map((review) => (
                                <tr key={review.id} className={`group hover:bg-gray-50/50 transition-colors ${review.deleted ? 'opacity-50' : ''}`}>
                                    <td className="px-6 py-4">
                                        {review.deleted ? (
                                            <span className="px-2 py-1 bg-red-50 text-red-600 text-[10px] font-bold rounded-md">숨김</span>
                                        ) : (
                                            <span className="px-2 py-1 bg-[#3FB6B2]/10 text-[#3FB6B2] text-[10px] font-bold rounded-md">활성</span>
                                        )}
                                    </td>
                                    <td className="px-6 py-4">
                                        <div className="text-sm font-semibold text-gray-900">
                                            {HIRING_STAGE_LABELS[review.hiringStage as HiringStage] || review.hiringStage}
                                        </div>
                                        <div className="text-xs text-gray-400 mt-1">난이도 {review.difficultyRating} · 만족도 {review.satisfactionRating}</div>
                                    </td>
                                    <td className="px-6 py-4 max-w-md">
                                        <p className="text-sm text-gray-600 line-clamp-2">{review.experienceComment}</p>
                                        {review.interviewTip && (
                                            <p className="text-[11px] text-gray-400 mt-1 line-clamp-1 italic">TIP: {review.interviewTip}</p>
                                        )}
                                    </td>
                                    <td className="px-6 py-4">
                                        <div className="text-sm text-gray-900">{review.memberName || '익명'}</div>
                                        <div className="text-[11px] text-gray-400">{new Date(review.createdAt).toLocaleDateString()}</div>
                                    </td>
                                    <td className="px-6 py-4 text-right">
                                        <div className="flex justify-end gap-1 opacity-100 md:opacity-0 md:group-hover:opacity-100 transition-opacity">
                                            {review.deleted ? (
                                                <button
                                                    onClick={() => handleRestore(review.id)}
                                                    className="p-2 text-gray-400 hover:text-[#3FB6B2] hover:bg-[#3FB6B2]/10 rounded-lg transition-all"
                                                    title="복구"
                                                >
                                                    <TbRotateClockwise size={18} />
                                                </button>
                                            ) : (
                                                <button
                                                    onClick={() => handleDelete(review.id)}
                                                    className="p-2 text-gray-400 hover:text-red-500 hover:bg-red-50 rounded-lg transition-all"
                                                    title="숨기기"
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

            {/* Pagination placeholder */}
            <div className="p-8 flex justify-center border-t border-gray-50">
                <div className="flex gap-2">
                    {Array.from({ length: Math.ceil(totalElements / 20) }).map((_, i) => (
                        <button
                            key={i}
                            onClick={() => setPage(i)}
                            className={`w-10 h-10 rounded-xl text-sm font-bold transition-all ${page === i
                                ? 'bg-[#3FB6B2] text-white shadow-lg shadow-[#3FB6B2]/20'
                                : 'bg-white text-gray-500 border border-gray-100 hover:bg-gray-50'
                                }`}
                        >
                            {i + 1}
                        </button>
                    ))}
                </div>
            </div>
        </div>
    );
}
