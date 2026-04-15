import { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { TbExternalLink, TbEyeOff, TbEye, TbRefresh } from 'react-icons/tb';
import { toast } from 'react-toastify';
import { adminService } from '../../services/adminService';
import type { AdminJobSummaryView } from '../../types/admin';

type ActiveFilter = 'all' | 'active' | 'inactive';

export default function AdminJobSummaryTab() {
  const navigate = useNavigate();
  const [items, setItems] = useState<AdminJobSummaryView[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [activeFilter, setActiveFilter] = useState<ActiveFilter>('all');
  const [brandNameInput, setBrandNameInput] = useState('');
  const [appliedBrandName, setAppliedBrandName] = useState('');
  const [batchSizeInput, setBatchSizeInput] = useState('50');
  const [reindexLoading, setReindexLoading] = useState<'all' | 'embedding' | null>(null);

  const isActiveParam = activeFilter === 'all' ? undefined : activeFilter === 'active';

  const fetchData = useCallback(async () => {
    setIsLoading(true);
    try {
      const result = await adminService.getAllJobSummaries(page, 10, isActiveParam, appliedBrandName || undefined);
      setItems(result.items || []);
      setTotalPages(result.totalPages || 0);
    } catch (error) {
      console.error('Failed to fetch JD list', error);
      toast.error('\u004a\u0044 \ubaa9\ub85d\uc744 \ubd88\ub7ec\uc624\uc9c0 \ubabb\ud588\uc2b5\ub2c8\ub2e4.');
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
        toast.info('\ud574\ub2f9 \uacf5\uace0\ub97c \ube44\ud65c\uc131\ud654\ud588\uc2b5\ub2c8\ub2e4.');
      } else {
        await adminService.activateJob(id);
        toast.success('\ud574\ub2f9 \uacf5\uace0\ub97c \ud65c\uc131\ud654\ud588\uc2b5\ub2c8\ub2e4.');
      }
      fetchData();
    } catch {
      toast.error('\uc0c1\ud0dc \ubcc0\uacbd\uc5d0 \uc2e4\ud328\ud588\uc2b5\ub2c8\ub2e4.');
    }
  };

  const applyBrandSearch = () => {
    setPage(0);
    setAppliedBrandName(brandNameInput.trim());
  };

  const resolveBatchSize = () => {
    const parsed = Number.parseInt(batchSizeInput.trim(), 10);
    if (Number.isNaN(parsed) || parsed < 1) {
      toast.error('\ubc30\uce58 \ud06c\uae30\ub294 1 \uc774\uc0c1\uc758 \uc22b\uc790\ub85c \uc785\ub825\ud574\uc8fc\uc138\uc694.');
      return null;
    }
    return Math.min(parsed, 500);
  };

  const handleReindex = async (target: 'all' | 'embedding') => {
    const batchSize = resolveBatchSize();
    if (!batchSize) return;

    setReindexLoading(target);
    try {
      const successCount =
        target === 'all'
          ? await adminService.reindexAllJobSummaries(batchSize)
          : await adminService.reindexMissingEmbeddings(batchSize);

      toast.success(
        target === 'all'
          ? `\uc804\uccb4 \uc7ac\uc0c9\uc778 \uc644\ub8cc: ${successCount}\uac74 \ucc98\ub9ac`
          : `\uc784\ubca0\ub529 \ub204\ub77d \uc7ac\uc0c9\uc778 \uc644\ub8cc: ${successCount}\uac74 \ucc98\ub9ac`
      );
      fetchData();
    } catch {
      toast.error(
        target === 'all'
          ? '\uc804\uccb4 \uc7ac\uc0c9\uc778\uc5d0 \uc2e4\ud328\ud588\uc2b5\ub2c8\ub2e4.'
          : '\uc784\ubca0\ub529 \uc7ac\uc0c9\uc778\uc5d0 \uc2e4\ud328\ud588\uc2b5\ub2c8\ub2e4.'
      );
    } finally {
      setReindexLoading(null);
    }
  };

  return (
    <div className="space-y-6">
      <div className="mb-4 flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
        <h2 className="text-xl font-bold text-gray-900">{'Job Summary \uad00\ub9ac'}</h2>

        <div className="flex w-full flex-wrap items-center gap-2 lg:w-auto">
          <input
            type="text"
            value={brandNameInput}
            onChange={(e) => setBrandNameInput(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === 'Enter') applyBrandSearch();
            }}
            placeholder={'\ube0c\ub79c\ub4dc\uba85 \uac80\uc0c9'}
            className="w-full rounded-xl border border-gray-200 bg-white px-3 py-2 text-sm text-gray-700 focus:border-[#3FB6B2] focus:outline-none sm:w-44"
          />

          <button
            onClick={applyBrandSearch}
            className="rounded-xl border border-gray-200 bg-white px-3 py-2 text-sm text-gray-700 transition-colors hover:bg-gray-50"
          >
            {'\uac80\uc0c9'}
          </button>

          <select
            value={activeFilter}
            onChange={(e) => {
              setActiveFilter(e.target.value as ActiveFilter);
              setPage(0);
            }}
            className="rounded-xl border border-gray-200 bg-white px-3 py-2 text-sm text-gray-700 focus:border-[#3FB6B2] focus:outline-none"
          >
            <option value="all">{'\uc804\uccb4'}</option>
            <option value="active">{'\ud65c\uc131'}</option>
            <option value="inactive">{'\ube44\ud65c\uc131'}</option>
          </select>

          <input
            type="number"
            min={1}
            max={500}
            value={batchSizeInput}
            onChange={(e) => setBatchSizeInput(e.target.value)}
            className="w-24 rounded-xl border border-gray-200 bg-white px-3 py-2 text-sm text-gray-700 focus:border-[#3FB6B2] focus:outline-none"
            title={'\uc7ac\uc0c9\uc778 \ubc30\uce58 \ud06c\uae30'}
          />

          <button
            onClick={() => handleReindex('all')}
            disabled={reindexLoading !== null}
            className="inline-flex items-center gap-1 rounded-xl border border-amber-200 bg-amber-50 px-3 py-2 text-sm font-medium text-amber-700 transition-colors hover:bg-amber-100 disabled:cursor-not-allowed disabled:opacity-60"
          >
            <TbRefresh size={16} />
            {reindexLoading === 'all' ? '\ucc98\ub9ac \uc911...' : '\uc804\uccb4 \uc7ac\uc0c9\uc778'}
          </button>

          <button
            onClick={() => handleReindex('embedding')}
            disabled={reindexLoading !== null}
            className="inline-flex items-center gap-1 rounded-xl border border-sky-200 bg-sky-50 px-3 py-2 text-sm font-medium text-sky-700 transition-colors hover:bg-sky-100 disabled:cursor-not-allowed disabled:opacity-60"
          >
            <TbRefresh size={16} />
            {reindexLoading === 'embedding' ? '\ucc98\ub9ac \uc911...' : '\uc784\ubca0\ub529 \uc7ac\uc0c9\uc778'}
          </button>

          <button
            onClick={() => navigate('/admin/jd/request')}
            className="w-full rounded-xl bg-[#3FB6B2] px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-[#34a09c] sm:w-auto"
          >
            {'JD \uc218\ub3d9 \ub4f1\ub85d'}
          </button>
        </div>
      </div>

      <div className="overflow-x-auto rounded-2xl border border-gray-100 bg-white">
        <table className="w-full text-left">
          <thead>
            <tr className="border-b border-gray-100 text-sm font-semibold text-gray-400">
              <th className="pb-4 pl-4 pt-4">ID</th>
              <th className="pb-4 pt-4">{'\ud68c\uc0ac / \ud3ec\uc9c0\uc158'}</th>
              <th className="pb-4 pt-4">{'\ub4f1\ub85d\uc77c'}</th>
              <th className="pb-4 pr-4 pt-4 text-right">{'\uc791\uc5c5'}</th>
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
                        title={'\uc0c1\uc138\ubcf4\uae30'}
                      >
                        <TbExternalLink size={18} />
                      </button>
                      {item.isActive ? (
                        <button
                          onClick={() => handleToggleActive(item.summaryId, 'deactivate')}
                          className="rounded-lg p-2 text-gray-500 transition-colors hover:bg-gray-100"
                          title={'\ube44\ud65c\uc131\ud654'}
                        >
                          <TbEyeOff size={18} />
                        </button>
                      ) : (
                        <button
                          onClick={() => handleToggleActive(item.summaryId, 'activate')}
                          className="rounded-lg p-2 text-[#3FB6B2] transition-colors hover:bg-[#3FB6B2]/10"
                          title={'\ud65c\uc131\ud654'}
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
                  {'\ub370\uc774\ud130\uac00 \uc5c6\uc2b5\ub2c8\ub2e4.'}
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      <div className="flex flex-wrap items-center justify-center gap-2">
        <button
          onClick={() => setPage((prev) => Math.max(0, prev - 1))}
          disabled={page <= 0}
          className="rounded-lg border border-gray-200 px-4 py-2 text-sm text-gray-600 disabled:cursor-not-allowed disabled:opacity-40"
        >
          {'\uc774\uc804'}
        </button>

        <span className="text-sm text-gray-500">
          {totalPages === 0 ? 0 : page + 1} / {totalPages}
        </span>

        <button
          onClick={() => setPage((prev) => prev + 1)}
          disabled={totalPages === 0 || page >= totalPages - 1}
          className="rounded-lg bg-[#3FB6B2] px-4 py-2 text-sm text-white disabled:cursor-not-allowed disabled:opacity-40"
        >
          {'\ub2e4\uc74c'}
        </button>
      </div>
    </div>
  );
}
