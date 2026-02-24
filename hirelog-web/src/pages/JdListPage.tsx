import { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { TbSearch } from 'react-icons/tb';
import { jdSummaryService } from '../services/jdSummaryService';
import type { JobSummaryView } from '../types/jobSummary';
import { JobSummaryCompactCard } from '../components/JobSummaryCompactCard';
import { Pagination } from '../components/common/Pagination';

export default function JdListPage() {
    const navigate = useNavigate();

    // -- State --
    const [items, setItems] = useState<JobSummaryView[]>([]);
    const [isLoading, setIsLoading] = useState(true);
    const [page, setPage] = useState(0);
    const [totalElements, setTotalElements] = useState(0);
    const [totalPages, setTotalPages] = useState(0);

    // -- Fetch Data --
    const fetchData = useCallback(async () => {
        setIsLoading(true);
        try {
            const result = await jdSummaryService.getMyRegistrations(page, 6);
            setItems(result?.items || []);
            setTotalElements(result?.totalElements || 0);
            setTotalPages(result?.totalPages || 0);
        } catch (err) {
            console.error('Failed to fetch registration history', err);
        } finally {
            setIsLoading(false);
        }
    }, [page]);

    useEffect(() => {
        fetchData();
    }, [fetchData]);

    const handlePageChange = (newPage: number) => {
        setPage(newPage);
        window.scrollTo({ top: 0, behavior: 'smooth' });
    };

    return (
        <div className="min-h-screen bg-[#F9FAFB] pt-32 pb-20 px-6 font-sans">
            <div className="max-w-5xl mx-auto">

                {/* Header Section */}
                <div className="flex flex-col md:flex-row justify-between items-start md:items-end gap-6 mb-12">
                    <div>
                        <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-[#4CDFD5]/10 border border-[#4CDFD5]/20 text-[#4CDFD5] text-[10px] font-bold uppercase tracking-widest mb-4">
                            Registration History
                        </div>
                        <h1 className="text-3xl font-black text-gray-900 tracking-tight flex items-center gap-3">
                            JD LOG <span className="text-[#4CDFD5]">HISTORY</span>
                        </h1>
                        <p className="text-gray-400 text-sm mt-2 font-medium">
                            최근 등록된 <span className="text-gray-900 font-bold">{totalElements}건</span>의 채용 공고 분석 이력입니다.
                        </p>
                    </div>

                    <button
                        onClick={() => navigate('/jd/request')}
                        className="flex items-center gap-2 px-6 py-3 bg-white text-[#4CDFD5] font-bold rounded-2xl border border-[#4CDFD5]/20 hover:bg-gray-50 transition-all shadow-sm active:scale-95 whitespace-nowrap"
                    >
                        새로운 JD 등록
                    </button>
                </div>

                {/* List Content */}
                <div className="min-h-[400px]">
                    {isLoading ? (
                        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                            {[...Array(4)].map((_, i) => (
                                <div key={i} className="h-64 bg-white rounded-2xl border border-gray-100 animate-pulse" />
                            ))}
                        </div>
                    ) : items.length > 0 ? (
                        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                            {items.map((item) => (
                                <JobSummaryCompactCard key={item.id} summary={item} />
                            ))}
                        </div>
                    ) : (
                        <div className="text-center py-32 bg-white rounded-3xl border border-dashed border-gray-200 shadow-sm">
                            <div className="w-16 h-16 bg-gray-50 rounded-full flex items-center justify-center mx-auto mb-4">
                                <TbSearch size={32} className="text-gray-300" />
                            </div>
                            <p className="text-gray-400 font-medium">등록된 공고 이력이 없습니다.</p>
                            <button
                                onClick={() => navigate('/jd/request')}
                                className="mt-6 text-[#4CDFD5] font-bold hover:underline"
                            >
                                첫 번째 공고 등록하기
                            </button>
                        </div>
                    )}
                </div>

                {/* Pagination Section */}
                {!isLoading && totalPages > 1 && (
                    <div className="mt-16">
                        <Pagination
                            currentPage={page}
                            totalPages={totalPages}
                            onPageChange={handlePageChange}
                        />
                    </div>
                )}

            </div>
        </div>
    );
}
