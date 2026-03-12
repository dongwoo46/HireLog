import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { jdSummaryService } from '../services/jdSummaryService';
import { JobSummaryCard } from '../components/JobSummaryCard';
import type { JobSummaryView } from '../types/jobSummary';
import { TbChevronLeft, TbBookmark, TbEdit } from 'react-icons/tb';

const JobSummaryArchivePage = () => {
    const navigate = useNavigate();
    const [activeTab, setActiveTab] = useState<'MY' | 'SAVED'>('MY');
    const [items, setItems] = useState<JobSummaryView[]>([]);
    const [isLoading, setIsLoading] = useState(true);

    const fetchItems = async () => {
        setIsLoading(true);
        try {
            const data = activeTab === 'MY'
                ? await jdSummaryService.getMyRegistrations()
                : await jdSummaryService.getMySaves();
            setItems(data.items || []);
        } catch (error) {
            console.error('Failed to fetch archive items', error);
        } finally {
            setIsLoading(false);
        }
    };

    useEffect(() => {
        fetchItems();
    }, [activeTab]);

    return (
        <div className="min-h-screen bg-[#F8F9FA] pt-24 pb-20 px-6">
            <div className="max-w-4xl mx-auto">

                {/* 뒤로가기 */}
                <button
                    onClick={() => navigate('/profile')}
                    className="flex items-center gap-2 text-gray-400 hover:text-gray-700 mb-8 transition-all group"
                >
                    <TbChevronLeft
                        size={20}
                        className="group-hover:-translate-x-1 transition-transform"
                    />
                    <span className="font-semibold">프로필로 돌아가기</span>
                </button>

                <div className="flex flex-col gap-8">
                    <div>
                        <h1 className="text-2xl font-bold text-gray-900">공고 모아보기</h1>
                        <p className="text-gray-500 text-sm mt-1">
                            내가 등록하거나 북마크한 공고들을 한곳에서 확인하세요.
                        </p>
                    </div>

                    {/* 탭 메뉴 */}
                    <div className="flex border-b border-gray-200">
                        <TabButton
                            active={activeTab === 'MY'}
                            onClick={() => setActiveTab('MY')}
                            icon={<TbEdit size={18} />}
                            label="내가 등록한 공고"
                        />
                        <TabButton
                            active={activeTab === 'SAVED'}
                            onClick={() => setActiveTab('SAVED')}
                            icon={<TbBookmark size={18} />}
                            label="북마크한 공고"
                        />
                    </div>

                    {/* 리스트 영역 */}
                    {isLoading ? (
                        <div className="space-y-4">
                            {[...Array(3)].map((_, i) => (
                                <div key={i} className="h-32 bg-white rounded-xl animate-pulse border border-gray-100" />
                            ))}
                        </div>
                    ) : items.length > 0 ? (
                        <div className="grid grid-cols-1 gap-4">
                            {items.map((item) => (
                                <JobSummaryCard key={item.id} summary={item} />
                            ))}
                        </div>
                    ) : (
                        <div className="text-center py-32 bg-white rounded-3xl border border-dashed border-gray-200">
                            <p className="text-gray-400 mb-6">
                                {activeTab === 'MY' ? '아직 직접 등록한 공고가 없습니다.' : '북마크한 공고가 없습니다.'}
                            </p>
                            {activeTab === 'MY' && (
                                <button
                                    onClick={() => navigate('/jd/request')}
                                    className="px-6 py-2.5 bg-[#4CDFD5] text-white font-semibold rounded-xl hover:bg-[#3CCFC5] transition"
                                >
                                    새 공고 등록하기
                                </button>
                            )}
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
};

const TabButton = ({ active, onClick, icon, label }: any) => (
    <button
        onClick={onClick}
        className={`
      flex items-center gap-2 px-6 py-4 font-semibold text-sm transition-all border-b-2
      ${active
                ? 'border-[#4CDFD5] text-[#0f172a]'
                : 'border-transparent text-gray-400 hover:text-gray-600'}
    `}
    >
        {icon}
        {label}
    </button>
);

export default JobSummaryArchivePage;
