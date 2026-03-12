import { useState, useEffect, useCallback } from 'react';
import { adminService } from '../../services/adminService';
import type { BrandListView } from '../../types/admin';
import { TbCheck, TbX, TbEye, TbEyeOff } from 'react-icons/tb';
import { toast } from 'react-toastify';

export default function AdminBrandTab() {
    const [brands, setBrands] = useState<BrandListView[]>([]);
    const [isLoading, setIsLoading] = useState(true);
    const [page, setPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);

    const fetchBrands = useCallback(async () => {
        setIsLoading(true);
        try {
            const result = await adminService.getAllBrands(page, 10);
            setBrands(result.items);
            setTotalPages(result.totalPages);
        } catch (error) {
            console.error('Failed to fetch brands', error);
            toast.error('브랜드 목록을 불러오는데 실패했습니다.');
        } finally {
            setIsLoading(false);
        }
    }, [page]);

    useEffect(() => {
        fetchBrands();
    }, [fetchBrands]);

    const handleVerify = async (id: number) => {
        try {
            await adminService.verifyBrand(id);
            toast.success('브랜드가 승인되었습니다.');
            fetchBrands();
        } catch (error) {
            toast.error('승인에 실패했습니다.');
        }
    };

    const handleReject = async (id: number) => {
        try {
            await adminService.rejectBrand(id);
            toast.info('브랜드가 거절되었습니다.');
            fetchBrands();
        } catch (error) {
            toast.error('거절에 실패했습니다.');
        }
    };

    const handleToggleActive = async (id: number, currentActive: boolean) => {
        try {
            if (currentActive) {
                await adminService.deactivateBrand(id);
                toast.info('브랜드가 비활성화되었습니다.');
            } else {
                await adminService.activateBrand(id);
                toast.success('브랜드가 활성화되었습니다.');
            }
            fetchBrands();
        } catch (error) {
            toast.error('상태 변경에 실패했습니다.');
        }
    };

    return (
        <div className="space-y-6">
            <div className="flex justify-between items-center mb-6">
                <h2 className="text-xl font-bold text-gray-900">브랜드 관리</h2>
            </div>

            <div className="overflow-x-auto">
                <table className="w-full text-left">
                    <thead>
                        <tr className="border-b border-gray-100 text-sm font-semibold text-gray-400">
                            <th className="pb-4 pl-4">ID</th>
                            <th className="pb-4">브랜드명</th>
                            <th className="pb-4">출처</th>
                            <th className="pb-4">검증 상태</th>
                            <th className="pb-4">활성</th>
                            <th className="pb-4 text-right pr-4">작업</th>
                        </tr>
                    </thead>
                    <tbody className="divide-y divide-gray-50 text-sm">
                        {isLoading ? (
                            [...Array(5)].map((_, i) => (
                                <tr key={i} className="animate-pulse">
                                    <td colSpan={6} className="py-6 bg-gray-50/50 rounded-lg mb-2"></td>
                                </tr>
                            ))
                        ) : brands.length > 0 ? (
                            brands.map((brand) => (
                                <tr key={brand.id} className="hover:bg-gray-50/50 transition-colors group">
                                    <td className="py-4 pl-4 text-gray-500">{brand.id}</td>
                                    <td className="py-4 font-medium text-gray-900">{brand.name}</td>
                                    <td className="py-4 text-gray-500">{brand.source}</td>
                                    <td className="py-4">
                                        <span className={`px-2 py-1 rounded-full text-xs font-medium ${brand.verificationStatus === 'VERIFIED'
                                            ? 'bg-green-50 text-green-600'
                                            : brand.verificationStatus === 'REJECTED'
                                                ? 'bg-red-50 text-red-600'
                                                : 'bg-yellow-50 text-yellow-600'
                                            }`}>
                                            {brand.verificationStatus}
                                        </span>
                                    </td>
                                    <td className="py-4">
                                        <span className={`w-2 h-2 rounded-full inline-block mr-2 ${brand.isActive ? 'bg-green-400' : 'bg-gray-300'}`}></span>
                                        {brand.isActive ? '활성' : '비활성'}
                                    </td>
                                    <td className="py-4 text-right pr-4">
                                        <div className="flex justify-end gap-2 opacity-0 group-hover:opacity-100 transition-opacity">
                                            {brand.verificationStatus === 'UNVERIFIED' && (
                                                <>
                                                    <button
                                                        onClick={() => handleVerify(brand.id)}
                                                        className="p-2 text-green-500 hover:bg-green-50 rounded-lg transition-colors"
                                                        title="승인"
                                                    >
                                                        <TbCheck size={18} />
                                                    </button>
                                                    <button
                                                        onClick={() => handleReject(brand.id)}
                                                        className="p-2 text-red-500 hover:bg-red-50 rounded-lg transition-colors"
                                                        title="거절"
                                                    >
                                                        <TbX size={18} />
                                                    </button>
                                                </>
                                            )}
                                            <button
                                                onClick={() => handleToggleActive(brand.id, brand.isActive)}
                                                className="p-2 text-gray-500 hover:bg-gray-100 rounded-lg transition-colors"
                                                title={brand.isActive ? '비활성화' : '활성화'}
                                            >
                                                {brand.isActive ? <TbEyeOff size={18} /> : <TbEye size={18} />}
                                            </button>
                                        </div>
                                    </td>
                                </tr>
                            ))
                        ) : (
                            <tr>
                                <td colSpan={6} className="py-12 text-center text-gray-500">브랜드가 없습니다.</td>
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
