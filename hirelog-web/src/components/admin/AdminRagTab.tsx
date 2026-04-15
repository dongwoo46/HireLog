import { useEffect, useMemo, useState } from 'react';
import { TbEye, TbRotateClockwise, TbSearch } from 'react-icons/tb';
import { toast } from 'react-toastify';
import { adminService } from '../../services/adminService';
import type { AdminPagedResult, AdminRagIntent, AdminRagLogView } from '../../types/admin';
import { Modal } from '../common/Modal';

type RagLogFilter = {
  memberId: string;
  intent: '' | AdminRagIntent;
  dateFrom: string;
  dateTo: string;
};

const INTENT_OPTIONS: Array<{ value: AdminRagIntent; label: string }> = [
  { value: 'DOCUMENT_SEARCH', label: '\uBB38\uC11C \uAC80\uC0C9' },
  { value: 'PATTERN_ANALYSIS', label: '\uD328\uD134 \uBD84\uC11D' },
  { value: 'EXPERIENCE_ANALYSIS', label: '\uACBD\uD5D8 \uBD84\uC11D' },
  { value: 'STATISTICS', label: '\uD1B5\uACC4 \uBD84\uC11D' },
  { value: 'KEYWORD_SEARCH', label: '\uD0A4\uC6CC\uB4DC \uAC80\uC0C9' },
  { value: 'SUMMARY', label: '\uC694\uC57D' },
];

const getErrorMessage = (error: unknown, fallback: string) => {
  const maybeError = error as { response?: { data?: { message?: string } }; message?: string };
  return maybeError?.response?.data?.message || maybeError?.message || fallback;
};

const formatJsonText = (value?: string | null) => {
  if (!value) return '-';
  try {
    return JSON.stringify(JSON.parse(value), null, 2);
  } catch {
    return value;
  }
};

export default function AdminRagTab() {
  const [filters, setFilters] = useState<RagLogFilter>({
    memberId: '',
    intent: '',
    dateFrom: '',
    dateTo: '',
  });
  const [page, setPage] = useState(0);
  const [size] = useState(20);
  const [result, setResult] = useState<AdminPagedResult<AdminRagLogView> | null>(null);
  const [loading, setLoading] = useState(false);

  const [selectedLog, setSelectedLog] = useState<AdminRagLogView | null>(null);
  const [isDetailOpen, setIsDetailOpen] = useState(false);
  const [loadingDetailId, setLoadingDetailId] = useState<number | null>(null);

  const fetchLogs = async (targetPage = page, targetFilters = filters) => {
    try {
      setLoading(true);
      const parsedMemberId = targetFilters.memberId.trim();
      const data = await adminService.getAllRagLogs(targetPage, size, {
        memberId: parsedMemberId ? Number(parsedMemberId) : undefined,
        intent: targetFilters.intent || undefined,
        dateFrom: targetFilters.dateFrom || undefined,
        dateTo: targetFilters.dateTo || undefined,
      });
      setResult(data);
    } catch (error) {
      toast.error(getErrorMessage(error, '\uCC44\uC6A9 \uB3C4\uC6B0\uBBF8 \uB85C\uADF8 \uC870\uD68C\uC5D0 \uC2E4\uD328\uD588\uC2B5\uB2C8\uB2E4.'));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchLogs(0, filters);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const handleSearch = async () => {
    setPage(0);
    await fetchLogs(0, filters);
  };

  const handleRefresh = async () => {
    await fetchLogs(page, filters);
  };

  const openDetail = async (id: number) => {
    try {
      setLoadingDetailId(id);
      const detail = await adminService.getRagLogById(id);
      setSelectedLog(detail);
      setIsDetailOpen(true);
    } catch (error) {
      toast.error(getErrorMessage(error, '\uB85C\uADF8 \uC0C1\uC138 \uC870\uD68C\uC5D0 \uC2E4\uD328\uD588\uC2B5\uB2C8\uB2E4.'));
    } finally {
      setLoadingDetailId(null);
    }
  };

  const totalPages = Math.max(1, result?.totalPages ?? 1);
  const items = result?.items ?? [];

  const intentLabelMap = useMemo(
    () => Object.fromEntries(INTENT_OPTIONS.map((option) => [option.value, option.label])) as Record<AdminRagIntent, string>,
    [],
  );

  return (
    <div className="space-y-6">
      <div className="rounded-2xl border border-gray-100 bg-white p-5">
        <h2 className="mb-3 text-lg font-bold text-gray-900">{'\uCC44\uC6A9 \uB3C4\uC6B0\uBBF8 \uB85C\uADF8 \uC870\uD68C'}</h2>
        <div className="grid gap-2 md:grid-cols-6">
          <input
            value={filters.memberId}
            onChange={(event) => setFilters((prev) => ({ ...prev, memberId: event.target.value.replace(/[^0-9]/g, '') }))}
            placeholder={'\uBA64\uBC84 ID'}
            className="rounded-xl border border-gray-200 px-3 py-2 text-sm"
          />
          <select
            value={filters.intent}
            onChange={(event) => setFilters((prev) => ({ ...prev, intent: event.target.value as RagLogFilter['intent'] }))}
            className="rounded-xl border border-gray-200 px-3 py-2 text-sm"
          >
            <option value="">{'\uC758\uB3C4 \uC804\uCCB4'}</option>
            {INTENT_OPTIONS.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
          <input
            type="date"
            value={filters.dateFrom}
            onChange={(event) => setFilters((prev) => ({ ...prev, dateFrom: event.target.value }))}
            className="rounded-xl border border-gray-200 px-3 py-2 text-sm"
          />
          <input
            type="date"
            value={filters.dateTo}
            onChange={(event) => setFilters((prev) => ({ ...prev, dateTo: event.target.value }))}
            className="rounded-xl border border-gray-200 px-3 py-2 text-sm"
          />
          <button
            type="button"
            onClick={handleSearch}
            className="flex items-center justify-center gap-2 rounded-xl bg-[#3FB6B2] px-4 py-2 text-sm font-semibold text-white"
          >
            <TbSearch size={16} />
            {'\uC870\uD68C'}
          </button>
          <button
            type="button"
            onClick={handleRefresh}
            className="flex items-center justify-center gap-2 rounded-xl border border-gray-200 bg-white px-4 py-2 text-sm font-semibold text-gray-700 transition hover:bg-gray-50"
          >
            <TbRotateClockwise size={16} />
            {'\uC0C8\uB85C\uACE0\uCE68'}
          </button>
        </div>
      </div>

      {loading ? (
        <div className="py-10 text-center text-sm text-gray-400">{'\uCC44\uC6A9 \uB3C4\uC6B0\uBBF8 \uB85C\uADF8\uB97C \uBD88\uB7EC\uC624\uB294 \uC911...'}</div>
      ) : !items.length ? (
        <div className="rounded-2xl border border-dashed border-gray-200 bg-gray-50 py-16 text-center text-sm text-gray-400">
          {'\uC870\uD68C\uB41C \uB85C\uADF8\uAC00 \uC5C6\uC2B5\uB2C8\uB2E4.'}
        </div>
      ) : (
        <div className="overflow-hidden rounded-2xl border border-gray-100">
          <div className="overflow-x-auto">
            <table className="min-w-full text-sm">
              <thead className="bg-gray-50 text-left text-xs font-semibold uppercase tracking-wide text-gray-500">
                <tr>
                  <th className="px-4 py-3">ID</th>
                  <th className="px-4 py-3">{'\uBA64\uBC84'}</th>
                  <th className="px-4 py-3">{'\uC758\uB3C4'}</th>
                  <th className="px-4 py-3">{'\uC9C8\uBB38'}</th>
                  <th className="px-4 py-3">{'\uC0DD\uC131\uC77C'}</th>
                  <th className="px-4 py-3">{'\uC561\uC158'}</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100 bg-white">
                {items.map((item) => (
                  <tr key={item.id} className="hover:bg-gray-50">
                    <td className="px-4 py-3 font-semibold text-gray-700">#{item.id}</td>
                    <td className="px-4 py-3 text-gray-600">{item.memberId}</td>
                    <td className="px-4 py-3 text-gray-600">{intentLabelMap[item.intent] ?? item.intent}</td>
                    <td className="max-w-[500px] truncate px-4 py-3 text-gray-700">{item.question}</td>
                    <td className="whitespace-nowrap px-4 py-3 text-gray-600">
                      {new Date(item.createdAt).toLocaleString()}
                    </td>
                    <td className="px-4 py-3">
                      <button
                        type="button"
                        onClick={() => openDetail(item.id)}
                        disabled={loadingDetailId === item.id}
                        className="inline-flex items-center gap-1 rounded-lg border border-gray-200 px-2.5 py-1.5 text-xs font-semibold text-gray-700 transition hover:bg-gray-50 disabled:opacity-60"
                      >
                        <TbEye size={15} />
                        {loadingDetailId === item.id ? '\uB85C\uB529 \uC911...' : '\uC0C1\uC138'}
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      <div className="flex items-center justify-end gap-2">
        <button
          type="button"
          disabled={page <= 0}
          onClick={() => {
            const next = Math.max(0, page - 1);
            setPage(next);
            fetchLogs(next, filters);
          }}
          className="rounded-lg border border-gray-200 px-3 py-1.5 text-xs font-semibold disabled:opacity-50"
        >
          {'\uC774\uC804'}
        </button>
        <span className="text-xs text-gray-500">
          {page + 1} / {totalPages}
        </span>
        <button
          type="button"
          disabled={page + 1 >= totalPages}
          onClick={() => {
            const next = page + 1;
            setPage(next);
            fetchLogs(next, filters);
          }}
          className="rounded-lg border border-gray-200 px-3 py-1.5 text-xs font-semibold disabled:opacity-50"
        >
          {'\uB2E4\uC74C'}
        </button>
      </div>

      <Modal
        isOpen={isDetailOpen}
        onClose={() => setIsDetailOpen(false)}
        title={'\uCC44\uC6A9 \uB3C4\uC6B0\uBBF8 \uB85C\uADF8 \uC0C1\uC138'}
        maxWidth="max-w-5xl"
      >
        {selectedLog ? (
          <div className="space-y-5 text-sm">
            <div className="grid gap-3 md:grid-cols-4">
              <div className="rounded-xl border border-gray-100 bg-gray-50 p-3">
                <p className="text-xs text-gray-500">ID</p>
                <p className="mt-1 font-semibold text-gray-800">#{selectedLog.id}</p>
              </div>
              <div className="rounded-xl border border-gray-100 bg-gray-50 p-3">
                <p className="text-xs text-gray-500">{'\uBA64\uBC84 ID'}</p>
                <p className="mt-1 font-semibold text-gray-800">{selectedLog.memberId}</p>
              </div>
              <div className="rounded-xl border border-gray-100 bg-gray-50 p-3">
                <p className="text-xs text-gray-500">{'\uC758\uB3C4'}</p>
                <p className="mt-1 font-semibold text-gray-800">{intentLabelMap[selectedLog.intent] ?? selectedLog.intent}</p>
              </div>
              <div className="rounded-xl border border-gray-100 bg-gray-50 p-3">
                <p className="text-xs text-gray-500">{'\uC0DD\uC131\uC77C'}</p>
                <p className="mt-1 font-semibold text-gray-800">{new Date(selectedLog.createdAt).toLocaleString()}</p>
              </div>
            </div>

            <div className="space-y-2">
              <p className="font-semibold text-gray-700">{'\uC9C8\uBB38'}</p>
              <div className="rounded-xl border border-gray-100 bg-white p-3 text-gray-800">{selectedLog.question}</div>
            </div>

            <div className="space-y-2">
              <p className="font-semibold text-gray-700">{'\uB2F5\uBCC0'}</p>
              <div className="rounded-xl border border-gray-100 bg-white p-3 whitespace-pre-wrap text-gray-800">{selectedLog.answer}</div>
            </div>

            <div className="grid gap-3 md:grid-cols-2">
              <JsonPanel title={'\uD30C\uC2F1 \uD14D\uC2A4\uD2B8'} value={selectedLog.parsedText || '-'} />
              <JsonPanel title={'\uCD94\uB860 \uC0AC\uC720'} value={selectedLog.reasoning || '-'} />
              <JsonPanel title={'\uD30C\uC2F1 \uD544\uD130 JSON'} value={formatJsonText(selectedLog.parsedFiltersJson)} />
              <JsonPanel title={'\uCEE8\uD14D\uC2A4\uD2B8 JSON'} value={formatJsonText(selectedLog.contextJson)} />
              <JsonPanel title={'\uADFC\uAC70 JSON'} value={formatJsonText(selectedLog.evidencesJson)} />
              <JsonPanel title={'\uCD9C\uCC98 JSON'} value={formatJsonText(selectedLog.sourcesJson)} />
            </div>
          </div>
        ) : null}
      </Modal>
    </div>
  );
}

function JsonPanel({ title, value }: { title: string; value: string }) {
  return (
    <div className="space-y-2 rounded-xl border border-gray-100 bg-gray-50 p-3">
      <p className="font-semibold text-gray-700">{title}</p>
      <pre className="max-h-72 overflow-auto whitespace-pre-wrap break-words rounded-lg bg-white p-2 text-xs text-gray-700">
        {value}
      </pre>
    </div>
  );
}
