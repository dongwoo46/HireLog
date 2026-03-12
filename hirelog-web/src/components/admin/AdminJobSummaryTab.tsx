import { useState, useEffect, useCallback } from 'react';
import { adminService } from '../../services/adminService';
import { jdSummaryService } from '../../services/jdSummaryService';
import type { JobSummaryView } from '../../types/jobSummary';
import { TbEye, TbExternalLink } from 'react-icons/tb';
import { toast } from 'react-toastify';

export default function AdminJobSummaryTab() {
    const [items, setItems] = useState<JobSummaryView[]>([]);
    const [isLoading, setIsLoading] = useState(true);
    const [page, setPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);

    const fetchData = useCallback(async () => {
        setIsLoading(true);
        try {
            // Use existing search API for backend (Admin can just search everything)
            // If there's a specific admin search, we can use that.
            // For now, let's use jdSummaryService.search with default params.
            const result = await jdSummaryService.search({
                page,
                size: 10,
                sortBy: 'CREATED_AT_DESC'
            });
            setItems(result?.items || []);
            setTotalPages(result?.totalPages || 0);
        } catch (error) {
            console.error('Failed to fetch JD list', error);
            toast.error('JD 목록을 불러오는데 실패했습니다.');
        } finally {
            setIsLoading(false);
        }
    }, [page]);

    useEffect(() => {
        fetchData();
    }, [fetchData]);

    const handleToggleActive = async (id: number, currentActive: boolean) => {
        try {
            if (currentActive) {
                await adminService.deactivateJob(id);
                toast.info('JD가 비활성화되었습니다.');
            } else {
                await adminService.activateJob(id);
                toast.success('JD가 활성화되었습니다.');
            }
            fetchData();
        } catch (error) {
            toast.error('상태 변경에 실패했습니다.');
        }
    };

    return (
        <div className="space-y-6">
            <div className="flex justify-between items-center mb-6">
                <h2 className="text-xl font-bold text-gray-900">Job Summary 관리</h2>
                <button
                    onClick={() => window.open('/jd/request', '_blank')}
                    className="px-4 py-2 bg-[#3FB6B2] text-white rounded-xl text-sm font-medium hover:bg-[#34a09c] transition-colors"
                >
                    JD 수동 등록
                </button>
            </div>

            <div className="overflow-x-auto">
                <table className="w-full text-left">
                    <thead>
                        <tr className="border-b border-gray-100 text-sm font-semibold text-gray-400">
                            <th className="pb-4 pl-4">ID</th>
                            <th className="pb-4">회사 / 포지션</th>
                            <th className="pb-4">등록일</th>
                            <th className="pb-4 text-right pr-4">작업</th>
                        </tr>
                    </thead>
                    <tbody className="divide-y divide-gray-50 text-sm">
                        {isLoading ? (
                            [...Array(5)].map((_, i) => (
                                <tr key={i} className="animate-pulse">
                                    <td colSpan={4} className="py-6 bg-gray-50/50 rounded-lg mb-2"></td>
                                </tr>
                            ))
                        ) : items.length > 0 ? (
                            items.map((item) => (
                                <tr key={item.id} className="hover:bg-gray-50/50 transition-colors group">
                                    <td className="py-4 pl-4 text-gray-500">{item.id}</td>
                                    <td className="py-4">
                                        <div className="font-medium text-gray-900">{item.brandName}</div>
                                        <div className="text-xs text-gray-400">{item.brandPositionName}</div>
                                    </td>
                                    <td className="py-4 text-gray-500">
                                        {item.createdAt ? new Date(item.createdAt).toLocaleDateString() : '-'}
                                    </td>
                                    <td className="py-4 text-right pr-4">
                                        <div className="flex justify-end gap-2 opacity-0 group-hover:opacity-100 transition-opacity">
                                            <button
                                                onClick={() => window.open(`/jd/${item.id}`, '_blank')}
                                                className="p-2 text-blue-500 hover:bg-blue-50 rounded-lg transition-colors"
                                                title="상세보기"
                                            >
                                                <TbExternalLink size={18} />
                                            </button>
                                            <button
                                                onClick={() => handleToggleActive(item.id, true)} // Placeholder for is_active from API
                                                className="p-2 text-gray-500 hover:bg-gray-100 rounded-lg transition-colors"
                                                title="활성/비활성"
                                            >
                                                <TbEye size={18} />
                                            </button>
                                        </div>
                                    </td>
                                </tr>
                            ))
                        ) : (
                            <tr>
                                <td colSpan={4} className="py-12 text-center text-gray-500">데이터가 없습니다.</td>
                            </tr>
                        )}
                    </tbody>
                </table>
            </div>

            {/* Pagination */}
            {totalPages > 1 && (
                <div className="flex justify-center mt-8 gap-2">
                    {Array.from({ length: totalPages }, (_, i) => i).map((p) => (
                        <button
                            key={p}
                            onClick={() => setPage(p)}
                            className={`w-8 h-8 rounded-lg text-sm font-medium transition-all ${p === page
                                ? 'bg-[#3FB6B2] text-white shadow-md'
                                : 'text-gray-400 hover:bg-gray-100'
                                }`}
                        >
                            {p + 1}
                        </button>
                    ))}
                </div>
            )}
        </div>
    );
}
