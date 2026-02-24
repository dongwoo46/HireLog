import { useState, useEffect, useCallback } from 'react';
import { adminService } from '../../services/adminService';
import type { CompanyView } from '../../types/admin';
import { TbEye, TbEyeOff } from 'react-icons/tb';
import { toast } from 'react-toastify';

export default function AdminCompanyTab() {
    const [companies, setCompanies] = useState<CompanyView[]>([]);
    const [isLoading, setIsLoading] = useState(true);
    const [page, setPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);

    const fetchCompanies = useCallback(async () => {
        setIsLoading(true);
        try {
            const result = await adminService.getAllCompanies(page, 10);
            setCompanies(result.items);
            setTotalPages(result.totalPages);
        } catch (error) {
            console.error('Failed to fetch companies', error);
            toast.error('회사 목록을 불러오는데 실패했습니다.');
        } finally {
            setIsLoading(false);
        }
    }, [page]);

    useEffect(() => {
        fetchCompanies();
    }, [fetchCompanies]);

    const handleToggleActive = async (id: number, currentActive: boolean) => {
        try {
            if (currentActive) {
                await adminService.deactivateCompany(id);
                toast.info('회사가 비활성화되었습니다.');
            } else {
                await adminService.activateCompany(id);
                toast.success('회사가 활성화되었습니다.');
            }
            fetchCompanies();
        } catch (error) {
            toast.error('상태 변경에 실패했습니다.');
        }
    };

    return (
        <div className="space-y-6">
            <div className="flex justify-between items-center mb-6">
                <h2 className="text-xl font-bold text-gray-900">컴퍼니 관리</h2>
            </div>

            <div className="overflow-x-auto">
                <table className="w-full text-left">
                    <thead>
                        <tr className="border-b border-gray-100 text-sm font-semibold text-gray-400">
                            <th className="pb-4 pl-4">ID</th>
                            <th className="pb-4">회사명</th>
                            <th className="pb-4">출처</th>
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
                        ) : companies.length > 0 ? (
                            companies.map((company) => (
                                <tr key={company.id} className="hover:bg-gray-50/50 transition-colors group">
                                    <td className="py-4 pl-4 text-gray-500">{company.id}</td>
                                    <td className="py-4 font-medium text-gray-900">{company.name}</td>
                                    <td className="py-4 text-gray-500">{company.source}</td>
                                    <td className="py-4">
                                        <span className={`w-2 h-2 rounded-full inline-block mr-2 ${company.isActive ? 'bg-green-400' : 'bg-gray-300'}`}></span>
                                        {company.isActive ? '활성' : '비활성'}
                                    </td>
                                    <td className="py-4 text-right pr-4">
                                        <div className="flex justify-end gap-2 opacity-0 group-hover:opacity-100 transition-opacity">
                                            <button
                                                onClick={() => handleToggleActive(company.id, company.isActive)}
                                                className="p-2 text-gray-500 hover:bg-gray-100 rounded-lg transition-colors"
                                                title={company.isActive ? '비활성화' : '활성화'}
                                            >
                                                {company.isActive ? <TbEyeOff size={18} /> : <TbEye size={18} />}
                                            </button>
                                        </div>
                                    </td>
                                </tr>
                            ))
                        ) : (
                            <tr>
                                <td colSpan={5} className="py-12 text-center text-gray-500">회사가 없습니다.</td>
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
