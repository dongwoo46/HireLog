import { useCallback, useEffect, useState } from 'react';
import { TbExternalLink, TbEyeOff, TbEye } from 'react-icons/tb';
import { toast } from 'react-toastify';
import { adminService } from '../../services/adminService';
import type { AdminJobSummaryView } from '../../types/admin';

type ActiveFilter = 'all' | 'active' | 'inactive';

export default function AdminJobSummaryTab() {
  const [items, setItems] = useState<AdminJobSummaryView[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [activeFilter, setActiveFilter] = useState<ActiveFilter>('all');
  const [brandNameInput, setBrandNameInput] = useState('');
  const [appliedBrandName, setAppliedBrandName] = useState('');

  const isActiveParam = activeFilter === 'all' ? undefined : activeFilter === 'active';

  const fetchData = useCallback(async () => {
    setIsLoading(true);
    try {
      const result = await adminService.getAllJobSummaries(
        page,
        10,
        isActiveParam,
        appliedBrandName || undefined
      );
      setItems(result.items || []);
      setTotalPages(result.totalPages || 0);
    } catch (error) {
      console.error('Failed to fetch JD list', error);
      toast.error('JD 목록을 불러오지 못했습니다.');
    } finally {
      setIsLoading(false);
    }
  }, [page, isActiveParam, appliedBrandName]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  const handleToggleActive = async (id: number, action: 'activate' | 'deactivate') => {
    try {
      if (action === 'deactivate') {
        await adminService.deactivateJob(id);
        toast.info('해당 공고를 비활성화했습니다.');
      } else {
        await adminService.activateJob(id);
        toast.success('해당 공고를 활성화했습니다.');
      }
      fetchData();
    } catch {
      toast.error('상태 변경에 실패했습니다.');
    }
  };

  const applyBrandSearch = () => {
    setPage(0);
    setAppliedBrandName(brandNameInput.trim());
  };

  return (
    <div className="space-y-6">
      <div className="mb-6 flex items-center justify-between">
        <h2 className="text-xl font-bold text-gray-900">Job Summary 관리</h2>
        <div className="flex items-center gap-2">
          <input
            type="text"
            value={brandNameInput}
            onChange={(e) => setBrandNameInput(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === 'Enter') {
                applyBrandSearch();
              }
            }}
            placeholder="브랜드명 검색"
            className="w-44 rounded-xl border border-gray-200 bg-white px-3 py-2 text-sm text-gray-700 focus:border-[#3FB6B2] focus:outline-none"
          />
          <button
            onClick={applyBrandSearch}
            className="rounded-xl border border-gray-200 bg-white px-3 py-2 text-sm text-gray-700 transition-colors hover:bg-gray-50"
          >
            검색
          </button>
          <select
            value={activeFilter}
            onChange={(e) => {
              setActiveFilter(e.target.value as ActiveFilter);
              setPage(0);
            }}
            className="rounded-xl border border-gray-200 bg-white px-3 py-2 text-sm text-gray-700 focus:border-[#3FB6B2] focus:outline-none"
          >
            <option value="all">전체</option>
            <option value="active">활성</option>
            <option value="inactive">비활성</option>
          </select>
          <button
            onClick={() => window.open('/admin/jd/request', '_blank')}
            className="rounded-xl bg-[#3FB6B2] px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-[#34a09c]"
          >
            JD 수동 등록
          </button>
        </div>
      </div>

      <div className="overflow-x-auto rounded-2xl border border-gray-100 bg-white">
        <table className="w-full text-left">
          <thead>
            <tr className="border-b border-gray-100 text-sm font-semibold text-gray-400">
              <th className="pb-4 pl-4 pt-4">ID</th>
              <th className="pb-4 pt-4">회사 / 포지션</th>
              <th className="pb-4 pt-4">등록일</th>
              <th className="pb-4 pr-4 pt-4 text-right">작업</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-50 text-sm">
            {isLoading ? (
              [...Array(5)].map((_, i) => (
                <tr key={i} className="animate-pulse">
                  <td colSpan={4} className="mb-2 py-6">
                    <div className="h-10 rounded bg-gray-50" />
                  </td>
                </tr>
              ))
            ) : items.length > 0 ? (
              items.map((item) => (
                <tr key={item.summaryId} className="group transition-colors hover:bg-gray-50/50">
                  <td className="py-4 pl-4 text-gray-500">{item.summaryId}</td>
                  <td className="py-4">
                    <div className="font-medium text-gray-900">{item.brandName}</div>
                    <div className="text-xs text-gray-400">{item.positionName}</div>
                  </td>
                  <td className="py-4 text-gray-500">
                    {item.createdAt ? new Date(item.createdAt).toLocaleDateString() : '-'}
                  </td>
                  <td className="py-4 pr-4 text-right">
                    <div className="flex justify-end gap-1 opacity-100 transition-opacity md:opacity-0 md:group-hover:opacity-100">
                      <button
                        onClick={() => window.open(`/jd/${item.summaryId}`, '_blank')}
                        className="rounded-lg p-2 text-blue-500 transition-colors hover:bg-blue-50"
                        title="상세보기"
                      >
                        <TbExternalLink size={18} />
                      </button>
                      {item.isActive ? (
                        <button
                          onClick={() => handleToggleActive(item.summaryId, 'deactivate')}
                          className="rounded-lg p-2 text-gray-500 transition-colors hover:bg-gray-100"
                          title="비활성화"
                        >
                          <TbEyeOff size={18} />
                        </button>
                      ) : (
                        <button
                          onClick={() => handleToggleActive(item.summaryId, 'activate')}
                          className="rounded-lg p-2 text-[#3FB6B2] transition-colors hover:bg-[#3FB6B2]/10"
                          title="활성화"
                        >
                          <TbEye size={18} />
                        </button>
                      )}
                    </div>
                  </td>
                </tr>
              ))
            ) : (
              <tr>
                <td colSpan={4} className="py-12 text-center text-gray-500">
                  데이터가 없습니다.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      <div className="flex justify-center gap-2">
        <button
          onClick={() => setPage((prev) => Math.max(0, prev - 1))}
          disabled={page <= 0}
          className="rounded-lg border border-gray-200 px-4 py-2 text-sm text-gray-600 disabled:cursor-not-allowed disabled:opacity-40"
        >
          이전
        </button>
        <button
          onClick={() => setPage((prev) => prev + 1)}
          disabled={totalPages === 0 || page >= totalPages - 1}
          className="rounded-lg bg-[#3FB6B2] px-4 py-2 text-sm text-white disabled:cursor-not-allowed disabled:opacity-40"
        >
          다음
        </button>
      </div>
    </div>
  );
}
