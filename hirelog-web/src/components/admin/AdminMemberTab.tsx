import { useState, useEffect, useCallback } from 'react';
import { adminService } from '../../services/adminService';
import type { MemberSummaryView } from '../../types/admin';
import { TbUserPause, TbUserCheck, TbTrash } from 'react-icons/tb';
import { toast } from 'react-toastify';

export default function AdminMemberTab() {
    const [members, setMembers] = useState<MemberSummaryView[]>([]);
    const [isLoading, setIsLoading] = useState(true);
    const [page, setPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);

    const fetchMembers = useCallback(async () => {
        setIsLoading(true);
        try {
            const result = await adminService.getAllMembers(page, 10);
            setMembers(result.items);
            setTotalPages(result.totalPages);
        } catch (error) {
            console.error('Failed to fetch members', error);
            toast.error('회원 목록을 불러오는데 실패했습니다.');
        } finally {
            setIsLoading(false);
        }
    }, [page]);

    useEffect(() => {
        fetchMembers();
    }, [fetchMembers]);

    const handleStatusChange = async (memberId: number, currentStatus: string) => {
        try {
            if (currentStatus === 'ACTIVE') {
                await adminService.suspendMember(memberId);
                toast.info('회원이 정지되었습니다.');
            } else {
                await adminService.activateMember(memberId);
                toast.success('회원이 활성화되었습니다.');
            }
            fetchMembers();
        } catch (error) {
            toast.error('상태 변경에 실패했습니다.');
        }
    };

    const handleDelete = async (memberId: number) => {
        if (!confirm('정말 이 회원을 삭제하시겠습니까?')) return;
        try {
            await adminService.deleteMember(memberId);
            toast.success('회원이 삭제되었습니다.');
            fetchMembers();
        } catch (error) {
            toast.error('회원 삭제에 실패했습니다.');
        }
    };

    return (
        <div className="space-y-6">
            <div className="flex justify-between items-center mb-6">
                <h2 className="text-xl font-bold text-gray-900">멤버스 관리</h2>
            </div>

            <div className="overflow-x-auto">
                <table className="w-full text-left">
                    <thead>
                        <tr className="border-b border-gray-100 text-sm font-semibold text-gray-400">
                            <th className="pb-4 pl-4">ID</th>
                            <th className="pb-4">이름 / 이메일</th>
                            <th className="pb-4">역할</th>
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
                        ) : members.length > 0 ? (
                            members.map((member) => (
                                <tr key={member.id} className="hover:bg-gray-50/50 transition-colors group">
                                    <td className="py-4 pl-4 text-gray-500">{member.id}</td>
                                    <td className="py-4">
                                        <div className="font-medium text-gray-900">{member.username}</div>
                                        <div className="text-xs text-gray-400">{member.email}</div>
                                    </td>
                                    <td className="py-4">
                                        <span className={`px-2 py-1 rounded-md text-[10px] font-bold uppercase tracking-wider ${member.role === 'ADMIN' ? 'bg-purple-50 text-purple-600' : 'bg-blue-50 text-blue-600'
                                            }`}>
                                            {member.role}
                                        </span>
                                    </td>
                                    <td className="py-4">
                                        <span className={`px-2 py-1 rounded-full text-xs font-medium ${member.status === 'ACTIVE'
                                            ? 'bg-green-50 text-green-600'
                                            : member.status === 'SUSPENDED'
                                                ? 'bg-yellow-50 text-yellow-600'
                                                : 'bg-red-50 text-red-600'
                                            }`}>
                                            {member.status === 'ACTIVE' ? '정상' : member.status === 'SUSPENDED' ? '정지' : '삭제됨'}
                                        </span>
                                    </td>
                                    <td className="py-4 text-right pr-4">
                                        <div className="flex justify-end gap-2 opacity-0 group-hover:opacity-100 transition-opacity">
                                            <button
                                                onClick={() => handleStatusChange(member.id, member.status)}
                                                className={`p-2 rounded-lg transition-colors ${member.status === 'ACTIVE'
                                                    ? 'text-yellow-500 hover:bg-yellow-50'
                                                    : 'text-green-500 hover:bg-green-50'
                                                    }`}
                                                title={member.status === 'ACTIVE' ? '정지하기' : '활성화하기'}
                                            >
                                                {member.status === 'ACTIVE' ? <TbUserPause size={18} /> : <TbUserCheck size={18} />}
                                            </button>
                                            <button
                                                onClick={() => handleDelete(member.id)}
                                                className="p-2 text-red-500 hover:bg-red-50 rounded-lg transition-colors"
                                                title="삭제하기"
                                            >
                                                <TbTrash size={18} />
                                            </button>
                                        </div>
                                    </td>
                                </tr>
                            ))
                        ) : (
                            <tr>
                                <td colSpan={5} className="py-12 text-center text-gray-500">회원이 없습니다.</td>
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
