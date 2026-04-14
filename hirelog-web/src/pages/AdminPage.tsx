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
    { id: 'job-summary', label: '\ucc44\uc6a9\uacf5\uace0', icon: TbBriefcase },
    { id: 'reports', label: '\uc2e0\uace0 \ucc98\ub9ac', icon: TbAlertTriangle },
    { id: 'brand', label: '\ube0c\ub79c\ub4dc', icon: TbShieldCheck },
    { id: 'user-request', label: '\uc0ac\uc6a9\uc790 \ubb38\uc758', icon: TbMessageDots },
    { id: 'reviews', label: '\ub9ac\ubdf0 \uad00\ub9ac', icon: TbStar },
    { id: 'members', label: '\uc0ac\uc6a9\uc790 \uad00\ub9ac', icon: TbUsers },
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
    <div className="min-h-screen bg-gray-50 pb-10 pt-20 sm:pb-20 sm:pt-24">
      <div className="mx-auto max-w-7xl px-4 sm:px-6">
        <h1 className="mb-4 text-2xl font-bold text-gray-900 md:hidden">{'\uad00\ub9ac\uc790 \uc13c\ud130'}</h1>

        <div className="mb-4 overflow-x-auto md:hidden">
          <div className="flex min-w-max gap-2 pb-1">
            {tabs.map((tab) => {
              const Icon = tab.icon;
              const selected = activeTab === tab.id;
              return (
                <button
                  key={tab.id}
                  onClick={() => setActiveTab(tab.id as AdminTab)}
                  className={`inline-flex items-center gap-2 whitespace-nowrap rounded-xl px-3 py-2 text-sm font-medium transition-all ${
                    selected
                      ? 'border border-gray-100 bg-white text-[#3FB6B2] shadow-sm'
                      : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                  }`}
                >
                  <Icon size={16} />
                  {tab.label}
                </button>
              );
            })}
          </div>
        </div>

        <div className="flex flex-col gap-6 md:flex-row md:gap-8">
          <aside className="hidden w-full space-y-2 md:block md:w-64">
            <h1 className="mb-8 px-4 text-2xl font-bold text-gray-900">{'\uad00\ub9ac\uc790 \uc13c\ud130'}</h1>
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

          <main className="min-h-[520px] flex-1 overflow-hidden rounded-2xl border border-gray-100 bg-white shadow-sm md:rounded-[2.5rem]">
            <div className="p-4 sm:p-6 lg:p-8">{renderTabContent()}</div>
          </main>
        </div>
      </div>
    </div>
  );
}