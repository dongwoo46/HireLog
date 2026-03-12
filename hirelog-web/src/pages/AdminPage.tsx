import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import {
    TbBriefcase,
    TbBuilding,
    TbUsers,
    TbMessageDots,
    TbShieldCheck,
    TbStar
} from 'react-icons/tb';
import { useAuthStore } from '../store/authStore';
import { toast } from 'react-toastify';
import AdminJobSummaryTab from '../components/admin/AdminJobSummaryTab';
import AdminBrandTab from '../components/admin/AdminBrandTab';
import AdminUserRequestTab from '../components/admin/AdminUserRequestTab';
import AdminMemberTab from '../components/admin/AdminMemberTab';
import AdminCompanyTab from '../components/admin/AdminCompanyTab';
import AdminReviewTab from '../components/admin/AdminReviewTab';

type AdminTab = 'job-summary' | 'brand' | 'user-request' | 'members' | 'company' | 'reviews';

export default function AdminPage() {
    const navigate = useNavigate();
    const { isInitialized, user } = useAuthStore();
    const [activeTab, setActiveTab] = useState<AdminTab>('job-summary');

    useEffect(() => {
        if (isInitialized) {
            if (!user || user.role !== 'ADMIN') {
                toast.error('관리자 권한이 없습니다.');
                navigate('/');
            }
        }
    }, [isInitialized, user, navigate]);

    const tabs = [
        { id: 'job-summary', label: 'Job Summary', icon: TbBriefcase },
        { id: 'brand', label: '브랜드', icon: TbShieldCheck },
        { id: 'user-request', label: '사용자 요청', icon: TbMessageDots },
        { id: 'reviews', label: '리뷰 관리', icon: TbStar },
        { id: 'members', label: '멤버스', icon: TbUsers },
        { id: 'company', label: '컴퍼니', icon: TbBuilding },
    ] as const;

    if (!isInitialized || !user || user.role !== 'ADMIN') {
        return null; // Or a loading spinner
    }

    const renderTabContent = () => {
        switch (activeTab) {
            case 'job-summary': return <AdminJobSummaryTab />;
            case 'brand': return <AdminBrandTab />;
            case 'user-request': return <AdminUserRequestTab />;
            case 'reviews': return <AdminReviewTab />;
            case 'members': return <AdminMemberTab />;
            case 'company': return <AdminCompanyTab />;
            default: return null;
        }
    };

    return (
        <div className="min-h-screen bg-gray-50 pt-24 pb-20">
            <div className="max-w-7xl mx-auto px-6">
                <div className="flex flex-col md:flex-row gap-8">
                    {/* Sidebar Navigation */}
                    <aside className="w-full md:w-64 space-y-2">
                        <h1 className="text-2xl font-bold text-gray-900 mb-8 px-4">관리자 센터</h1>
                        {tabs.map((tab) => {
                            const Icon = tab.icon;
                            return (
                                <button
                                    key={tab.id}
                                    onClick={() => setActiveTab(tab.id as AdminTab)}
                                    className={`w-full flex items-center gap-3 px-4 py-3 rounded-xl text-sm font-medium transition-all ${activeTab === tab.id
                                        ? 'bg-white text-[#3FB6B2] shadow-sm border border-gray-100'
                                        : 'text-gray-500 hover:bg-gray-100 hover:text-gray-900'
                                        }`}
                                >
                                    <Icon size={18} />
                                    {tab.label}
                                </button>
                            );
                        })}
                    </aside>

                    {/* Main Content */}
                    <main className="flex-1 bg-white rounded-[2.5rem] border border-gray-100 shadow-sm min-h-[600px] overflow-hidden">
                        <div className="p-8">
                            {renderTabContent()}
                        </div>
                    </main>
                </div>
            </div>
        </div>
    );
}
