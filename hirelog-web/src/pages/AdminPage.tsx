import { useState } from 'react';
import {
  TbAlertTriangle,
  TbBriefcase,
  TbMessageDots,
  TbShieldCheck,
  TbStar,
  TbUsers,
} from 'react-icons/tb';
import AdminBrandTab from '../components/admin/AdminBrandTab';
import AdminJobSummaryTab from '../components/admin/AdminJobSummaryTab';
import AdminMemberTab from '../components/admin/AdminMemberTab';
import AdminReportTab from '../components/admin/AdminReportTab';
import AdminReviewTab from '../components/admin/AdminReviewTab';
import AdminUserRequestTab from '../components/admin/AdminUserRequestTab';

type AdminTab = 'job-summary' | 'reports' | 'brand' | 'user-request' | 'reviews' | 'members';

export default function AdminPage() {
  const [activeTab, setActiveTab] = useState<AdminTab>('job-summary');

  const tabs = [
    { id: 'job-summary', label: '채용공고', icon: TbBriefcase },
    { id: 'reports', label: '신고 처리', icon: TbAlertTriangle },
    { id: 'brand', label: '브랜드', icon: TbShieldCheck },
    { id: 'user-request', label: '사용자 문의', icon: TbMessageDots },
    { id: 'reviews', label: '리뷰 관리', icon: TbStar },
    { id: 'members', label: '사용자 관리', icon: TbUsers },
  ] as const;

  const renderTabContent = () => {
    switch (activeTab) {
      case 'job-summary':
        return <AdminJobSummaryTab />;
      case 'reports':
        return <AdminReportTab />;
      case 'brand':
        return <AdminBrandTab />;
      case 'user-request':
        return <AdminUserRequestTab />;
      case 'reviews':
        return <AdminReviewTab />;
      case 'members':
        return <AdminMemberTab />;
      default:
        return null;
    }
  };

  return (
    <div className="min-h-screen bg-gray-50 pt-24 pb-20">
      <div className="mx-auto max-w-7xl px-6">
        <div className="flex flex-col gap-8 md:flex-row">
          <aside className="w-full space-y-2 md:w-64">
            <h1 className="mb-8 px-4 text-2xl font-bold text-gray-900">관리자 센터</h1>
            {tabs.map((tab) => {
              const Icon = tab.icon;
              return (
                <button
                  key={tab.id}
                  onClick={() => setActiveTab(tab.id as AdminTab)}
                  className={`w-full rounded-xl px-4 py-3 text-left text-sm font-medium transition-all ${
                    activeTab === tab.id
                      ? 'border border-gray-100 bg-white text-[#3FB6B2] shadow-sm'
                      : 'text-gray-500 hover:bg-gray-100 hover:text-gray-900'
                  }`}
                >
                  <span className="flex items-center gap-3">
                    <Icon size={18} />
                    {tab.label}
                  </span>
                </button>
              );
            })}
          </aside>

          <main className="min-h-[600px] flex-1 overflow-hidden rounded-[2.5rem] border border-gray-100 bg-white shadow-sm">
            <div className="p-8">{renderTabContent()}</div>
          </main>
        </div>
      </div>
    </div>
  );
}
