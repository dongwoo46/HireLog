import { useState, useEffect, useCallback } from 'react';
import { adminService } from '../../services/adminService';
import { TbMessageCheck, TbMessageX, TbMessageForward, TbEye } from 'react-icons/tb';
import { toast } from 'react-toastify';
import { useNavigate } from 'react-router-dom';

export default function AdminUserRequestTab() {
    const navigate = useNavigate();
    const [requests, setRequests] = useState<any[]>([]);
    const [isLoading, setIsLoading] = useState(true);
    const [page, setPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);
    const [filterStatus, setFilterStatus] = useState<string>('');

    const fetchRequests = useCallback(async () => {
        setIsLoading(true);
        try {
            const result = await adminService.getAllUserRequests(page, 10, filterStatus || undefined);
            setRequests(result.items);
            setTotalPages(result.totalPages);
        } catch (error) {
            console.error('Failed to fetch requests', error);
            toast.error('요청 목록을 불러오는데 실패했습니다.');
        } finally {
            setIsLoading(false);
        }
    }, [page, filterStatus]);

    useEffect(() => {
        fetchRequests();
    }, [fetchRequests]);

    const handleStatusChange = async (id: number, status: string) => {
        try {
            await adminService.updateUserRequestStatus(id, status);
            toast.success(`상태가 ${status}로 변경되었습니다.`);
            fetchRequests();
        } catch (error) {
            toast.error('상태 변경에 실패했습니다.');
        }
    };

    return (
        <div className="space-y-6">
            <div className="flex justify-between items-center mb-6">
                <h2 className="text-xl font-bold text-gray-900">사용자 요청 관리</h2>
                <select
                    value={filterStatus}
                    onChange={(e) => {
                        setFilterStatus(e.target.value);
                        setPage(0);
                    }}
                    className="bg-gray-50 border border-gray-100 text-gray-900 text-sm rounded-xl focus:ring-[#3FB6B2] focus:border-[#3FB6B2] block p-2.5"
                >
                    <option value="">전체 상태</option>
                    <option value="PENDING">대기중</option>
                    <option value="IN_PROGRESS">처리중</option>
                    <option value="RESOLVED">완료</option>
                    <option value="REJECTED">거절</option>
                </select>
            </div>

            <div className="overflow-x-auto">
                <table className="w-full text-left">
                    <thead>
                        <tr className="border-b border-gray-100 text-sm font-semibold text-gray-400">
                            <th className="pb-4 pl-4">ID</th>
                            <th className="pb-4">제목 / 유형</th>
                            <th className="pb-4">작성자</th>
                            <th className="pb-4">상태</th>
                            <th className="pb-4 text-right pr-4">작업</th>
                        </tr>
                    </thead>
                    <tbody className="divide-y divide-gray-50 text-sm">
                        {isLoading ? (
                            [...Array(5)].map((_, i) => (
                                <tr key={i} className="animate-pulse">
                                    <td colSpan={5} className="py-6 bg-gray-50/50 rounded-lg mb-2"></td>
                                </tr>
                            ))
                        ) : requests.length > 0 ? (
                            requests.map((req) => (
                                <tr key={req.id} className="hover:bg-gray-50/50 transition-colors group">
                                    <td className="py-4 pl-4 text-gray-500">{req.id}</td>
                                    <td className="py-4">
                                        <div className="font-medium text-gray-900">{req.title}</div>
                                        <div className="text-xs text-gray-400 uppercase tracking-wider">{req.requestType}</div>
                                    </td>
                                    <td className="py-4 text-gray-500">{req.username || 'Anonymous'}</td>
                                    <td className="py-4">
                                        <span className={`px-2 py-1 rounded-full text-xs font-medium ${req.status === 'RESOLVED' || req.status === 'COMPLETED'
                                            ? 'bg-green-50 text-green-600'
                                            : req.status === 'REJECTED'
                                                ? 'bg-red-50 text-red-600'
                                                : req.status === 'IN_PROGRESS'
                                                    ? 'bg-blue-50 text-blue-600'
                                                    : 'bg-yellow-50 text-yellow-600'
                                            }`}>
                                            {req.status === 'RESOLVED' ? '완료' : req.status === 'PENDING' ? '대기중' : req.status === 'IN_PROGRESS' ? '처리중' : req.status === 'REJECTED' ? '거절' : req.status}
                                        </span>
                                    </td>
                                    <td className="py-4 text-right pr-4">
                                        <div className="flex justify-end gap-2 opacity-0 group-hover:opacity-100 transition-opacity">
                                            <button
                                                onClick={() => navigate(`/requests/${req.id}`)}
                                                className="p-2 text-blue-500 hover:bg-blue-50 rounded-lg transition-colors"
                                                title="상세보기"
                                            >
                                                <TbEye size={18} />
                                            </button>
                                            <button
                                                onClick={() => handleStatusChange(req.id, 'IN_PROGRESS')}
                                                className="p-2 text-blue-400 hover:bg-blue-50 rounded-lg transition-colors"
                                                title="처리중으로 변경"
                                            >
                                                <TbMessageForward size={18} />
                                            </button>
                                            <button
                                                onClick={() => handleStatusChange(req.id, 'RESOLVED')}
                                                className="p-2 text-green-500 hover:bg-green-50 rounded-lg transition-colors"
                                                title="완료로 변경"
                                            >
                                                <TbMessageCheck size={18} />
                                            </button>
                                            <button
                                                onClick={() => handleStatusChange(req.id, 'REJECTED')}
                                                className="p-2 text-red-500 hover:bg-red-50 rounded-lg transition-colors"
                                                title="거절로 변경"
                                            >
                                                <TbMessageX size={18} />
                                            </button>
                                        </div>
                                    </td>
                                </tr>
                            ))
                        ) : (
                            <tr>
                                <td colSpan={5} className="py-12 text-center text-gray-500">요청이 없습니다.</td>
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
