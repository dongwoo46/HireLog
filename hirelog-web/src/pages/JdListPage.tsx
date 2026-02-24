import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { FaSearch, FaChevronLeft } from 'react-icons/fa';
import { jdService, type JdListResponse } from '../services/jd';

export default function JdListPage() {
    const navigate = useNavigate();

    // -- State --
    const [data, setData] = useState<JdListResponse | null>(null);
    const [loading, setLoading] = useState(false);
    const [page, setPage] = useState(0);
    const [searchTerm, setSearchTerm] = useState('');
    const [filterType, setFilterType] = useState('company'); // 'company' or 'position' (visual only for now)

    // -- Fetch Data --
    useEffect(() => {
        fetchData();
    }, [page]); // Re-fetch when page changes

    const fetchData = async () => {
        setLoading(true);
        try {
            // Mock API call
            const response = await jdService.getList(page, 5, searchTerm);
            setData(response);
        } catch (err) {
            console.error(err);
        } finally {
            setLoading(false);
        }
    };

    const handleSearch = (e: React.FormEvent) => {
        e.preventDefault();
        setPage(0); // Reset to first page
        fetchData();
    };

    const handlePageChange = (newPage: number) => {
        if (newPage >= 0 && newPage < (data?.totalPages || 1)) {
            setPage(newPage);
        }
    };

    return (
        <div className="min-h-screen bg-white pt-24 pb-12 px-6 font-sans">
            <div className="max-w-5xl mx-auto">

                {/* Header Section */}
                <div className="relative mb-12 text-center">
                    <h1 className="text-3xl font-bold flex justify-center items-center gap-2">
                        <span className="text-teal-400">JD</span>
                        <span className="text-gray-900">이력조회</span>
                    </h1>

                    {/* "Back" Link - Top Right */}
                    <div className="absolute top-1/2 -translate-y-1/2 right-0 hidden md:flex items-center text-gray-400 hover:text-gray-600 text-sm cursor-pointer" onClick={() => navigate('/')}>
                        <FaChevronLeft className="mr-1 w-3 h-3" />
                        <span>최근 등록 이력</span>
                    </div>
                </div>

                {/* Filter / Search Bar */}
                <div className="flex flex-col md:flex-row gap-4 mb-4 items-center justify-between border-b border-gray-200 pb-6">
                    <form onSubmit={handleSearch} className="flex gap-4 w-full md:w-auto flex-1">
                        {/* Search Input */}
                        <div className="relative w-full max-w-md">
                            <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                                <FaSearch className="text-gray-400 w-4 h-4" />
                            </div>
                            <input
                                type="text"
                                value={searchTerm}
                                onChange={(e) => setSearchTerm(e.target.value)}
                                placeholder="회사명, 직무명 등 검색"
                                className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-md focus:outline-none focus:border-teal-400 text-sm"
                            />
                        </div>

                        {/* Dropdowns (Visual only for mock) */}
                        <select
                            className="border border-gray-300 rounded-md py-2 px-3 text-sm text-gray-600 focus:outline-none"
                            value={filterType}
                            onChange={(e) => setFilterType(e.target.value)}
                        >
                            <option value="company">회사명</option>
                            <option value="position">직무명</option>
                        </select>
                        <select className="border border-gray-300 rounded-md py-2 px-3 text-sm text-gray-600 focus:outline-none">
                            <option>직무명</option>
                            {/* Just replicating the screenshot logic where there are two dropdowns, 
                        though usually one is enough. Keeping it simple. */}
                        </select>
                    </form>

                    {/* Total Count */}
                    <div className="text-sm text-gray-900 font-bold whitespace-nowrap">
                        총 {data?.totalElements || 0}건
                    </div>
                </div>

                {/* List Content */}
                <div className="space-y-4 mb-12 min-h-[400px]">
                    {loading ? (
                        <div className="text-center py-20 text-gray-400">Loading...</div>
                    ) : (
                        data?.content.map((item) => (
                            <div key={item.id} className="bg-gray-50 rounded-lg p-6 border border-gray-100 hover:shadow-sm transition-shadow">
                                <div className="flex flex-col gap-2 mb-4">
                                    <h2 className="text-lg font-bold text-gray-900 uppercase tracking-wide">{item.company}</h2>
                                    <h3 className="text-base text-gray-800 font-medium">{item.position}</h3>

                                    {/* Tags */}
                                    <div className="flex gap-2 mt-1">
                                        {item.tags.map((tag, idx) => (
                                            <span key={idx} className="px-3 py-1 bg-cyan-50 text-cyan-500 text-xs font-bold rounded-full border border-cyan-100">
                                                {tag}
                                            </span>
                                        ))}
                                    </div>
                                </div>

                                {/* Footer (Date + Review Count) */}
                                <div className="flex justify-between items-center pt-4 border-t border-gray-200 text-xs text-gray-400 font-medium">
                                    <span>{item.date}</span>
                                    <span>리뷰({item.reviewCount || '20+'})</span>
                                </div>
                            </div>
                        ))
                    )}

                    {!loading && data?.content.length === 0 && (
                        <div className="text-center py-20 text-gray-400">검색 결과가 없습니다.</div>
                    )}
                </div>

                {/* Pagination */}
                {!loading && (data?.totalPages || 0) > 0 && (
                    <div className="flex justify-center items-center gap-4">
                        <button
                            onClick={() => handlePageChange(page - 1)}
                            disabled={page === 0}
                            className="p-2 text-gray-400 hover:text-gray-800 disabled:opacity-30 disabled:cursor-not-allowed"
                        >
                            &lt;
                        </button>

                        {Array.from({ length: data!.totalPages }).map((_, idx) => (
                            <button
                                key={idx}
                                onClick={() => handlePageChange(idx)}
                                className={`text-sm font-bold w-8 h-8 rounded-full flex items-center justify-center transition-colors 
                            ${page === idx
                                        ? 'text-black border-b-2 border-black rounded-none' // Active state style mimic (underline) or simple bold
                                        : 'text-gray-400 hover:text-gray-600'
                                    }`}
                            >
                                {idx + 1}
                            </button>
                        ))}

                        <button
                            onClick={() => handlePageChange(page + 1)}
                            disabled={page === (data?.totalPages || 1) - 1}
                            className="p-2 text-gray-400 hover:text-gray-800 disabled:opacity-30 disabled:cursor-not-allowed"
                        >
                            &gt;
                        </button>
                    </div>
                )}

            </div>
        </div>
    );
}
