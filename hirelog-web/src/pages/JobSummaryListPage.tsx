import { useEffect, useRef, useState, useMemo, useCallback } from 'react';
import { useSearchParams } from 'react-router-dom';
import type { CareerType, JobSummarySearchReq, JobSummaryView } from '../types/jobSummary';
import { jdSummaryService } from '../services/jdSummaryService';
import { JobSummaryCard } from '../components/JobSummaryCard';
import { JobSummarySearch } from '../components/JobSummarySearch';
import { useAuthStore } from '../store/authStore';

const JobSummaryListPage = () => {
  const [searchParams, setSearchParams] = useSearchParams();
  const { isAuthenticated } = useAuthStore();

  const [jds, setJds] = useState<JobSummaryView[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isFetchingMore, setIsFetchingMore] = useState(false);
  const [nextCursor, setNextCursor] = useState<string | null>(null);
  const [hasNext, setHasNext] = useState(false);

  const [isSideOpen, setIsSideOpen] = useState(false);
  const [sideFilter, setSideFilter] = useState<'SAVED' | 'APPLY'>('SAVED');
  const [sideJds, setSideJds] = useState<JobSummaryView[]>([]);
  const [sideLoading, setSideLoading] = useState(false);

  const sentinelRef = useRef<HTMLDivElement>(null);
  const fetchMoreRef = useRef<() => void>(() => { });

  const queryKey = useMemo(
    () =>
      JSON.stringify({
        keyword: searchParams.get('keyword') || '',
        careerType: searchParams.get('careerType') || '',
        sortBy: searchParams.get('sortBy') || 'CREATED_AT_DESC',
      }),
    [searchParams],
  );

  const buildParams = useCallback(
    (cursor?: string | null): JobSummarySearchReq => ({
      keyword: searchParams.get('keyword') || undefined,
      careerType: (searchParams.get('careerType') as CareerType) || undefined,
      sortBy: searchParams.get('sortBy') || 'CREATED_AT_DESC',
      size: 12,
      cursor: cursor || undefined,
    }),
    [searchParams],
  );

  // 검색 조건 변경 시 초기화 후 첫 페이지 로드
  useEffect(() => {
    let cancelled = false;

    const fetchFirst = async () => {
      setIsLoading(true);
      setJds([]);
      setNextCursor(null);
      setHasNext(false);

      try {
        const result = await jdSummaryService.search(buildParams(null));
        if (cancelled) return;
        setJds(result?.items || []);
        setHasNext(result?.hasNext || false);
        setNextCursor(result?.nextCursor || null);
      } catch (error) {
        console.error(error);
      } finally {
        if (!cancelled) setIsLoading(false);
      }
    };

    fetchFirst();
    return () => {
      cancelled = true;
    };
  }, [queryKey]);

  // 추가 페이지 로드
  const fetchMore = useCallback(async () => {
    if (!hasNext || isFetchingMore || isLoading) return;

    setIsFetchingMore(true);
    try {
      const result = await jdSummaryService.search(buildParams(nextCursor));
      setJds((prev) => [...prev, ...(result?.items || [])]);
      setHasNext(result?.hasNext || false);
      setNextCursor(result?.nextCursor || null);
    } catch (error) {
      console.error(error);
    } finally {
      setIsFetchingMore(false);
    }
  }, [hasNext, isFetchingMore, isLoading, nextCursor, buildParams]);

  // fetchMore 최신 참조 유지 (Observer 재구독 방지)
  useEffect(() => {
    fetchMoreRef.current = fetchMore;
  }, [fetchMore]);

  // IntersectionObserver (마운트 시 1회만 등록)
  useEffect(() => {
    const sentinel = sentinelRef.current;
    if (!sentinel) return;

    const observer = new IntersectionObserver(
      (entries) => {
        if (entries[0].isIntersecting) fetchMoreRef.current();
      },
      { threshold: 0.1 },
    );

    observer.observe(sentinel);
    return () => observer.disconnect();
  }, []);

  useEffect(() => {
    if (!isSideOpen) return;

    const fetchSideList = async () => {
      setSideLoading(true);
      try {
        const result =
          sideFilter === 'SAVED'
            ? await jdSummaryService.getMySaves(0, 20)
            : await jdSummaryService.getMyApplies(0, 20);
        setSideJds(result.items || []);
      } catch (e) {
        console.error(e);
      } finally {
        setSideLoading(false);
      }
    };

    fetchSideList();
  }, [isSideOpen, sideFilter]);

  return (
    <div className="min-h-screen bg-[#F8F9FA] pb-20 relative">

      {/* 🔥 사이드 열려있을 때 버튼 숨김 */}
      {!isSideOpen && (
        <button
          onClick={() => setIsSideOpen(true)}
          className="fixed right-0 top-48 z-50 bg-[#3FB6B2] text-white px-4 py-3 rounded-l-2xl shadow-lg hover:bg-[#35A09D] transition"
        >
          〈 등록한 공고 보기
        </button>
      )}

      <div
        className={`fixed right-0 top-0 z-40 h-full w-full max-w-md border-l border-gray-100 bg-white shadow-2xl transition-transform duration-300 ${isAuthenticated && isSideOpen ? 'translate-x-0' : 'translate-x-full'
          }`}
      >
        <div className="flex h-full flex-col">
          <div className="border-b border-gray-100 px-6 pb-4 pt-24">
            <div className="mb-4 flex items-center justify-between">
              <h3 className="text-base font-bold text-gray-900">내 공고 보드</h3>
              <button
                onClick={() => setIsSideOpen(false)}
                className="rounded-lg px-2 py-1 text-sm text-gray-500 hover:bg-gray-100"
              >
                닫기
              </button>
            </div>

            <div className="grid grid-cols-2 gap-2 rounded-xl bg-gray-100 p-1">
              <button
                onClick={() => setSideFilter('SAVED')}
                className={`rounded-lg px-3 py-2 text-sm font-semibold transition ${sideFilter === 'SAVED' ? 'bg-white text-[#3FB6B2] shadow-sm' : 'text-gray-500'
                  }`}
              >
                저장한 공고
              </button>
              <button
                onClick={() => setSideFilter('APPLY')}
                className={`rounded-lg px-3 py-2 text-sm font-semibold transition ${sideFilter === 'APPLY' ? 'bg-white text-[#3FB6B2] shadow-sm' : 'text-gray-500'
                  }`}
              >
                지원한 공고
              </button>
            </div>
          </div>

          <div className="flex-1 space-y-3 overflow-y-auto p-6">
            {sideLoading ? (
              <div className="text-center text-sm text-gray-400">목록을 불러오는 중...</div>
            ) : sideJds.length > 0 ? (
              sideJds.map((jd) => (
                <a
                  key={`${jd.id}-${jd.memberSaveType}`}
                  href={`/jd/${jd.id}`}
                  className="block rounded-xl border border-gray-100 p-4 transition hover:border-[#3FB6B2]/30 hover:bg-[#3FB6B2]/5"
                >
                  <p className="text-sm font-bold text-gray-900">{jd.brandName}</p>
                  <p className="mt-1 text-xs text-gray-500">{jd.brandPositionName}</p>
                  <p className="mt-2 text-[11px] text-gray-400">
                    {jd.createdAt ? new Date(jd.createdAt).toLocaleDateString() : ''}
                  </p>
                </a>
              ))
            ) : (
              <div className="mt-10 text-center text-sm text-gray-400">표시할 공고가 없습니다.</div>
            )}
          </div>
        </div>
      </div>

      <div className="px-6 pb-10 pt-32">
        <div className="mx-auto max-w-7xl">
          <JobSummarySearch
            onSearch={(params) => {
              const nextParams = new URLSearchParams();
              Object.entries(params).forEach(([key, value]) => {
                if (value) nextParams.set(key, value.toString());
              });
              setSearchParams(nextParams);
            }}
            initialParams={{
              keyword: searchParams.get('keyword') || '',
              careerType: (searchParams.get('careerType') as CareerType) || undefined,
              sortBy: searchParams.get('sortBy') || 'CREATED_AT_DESC',
            }}
          />
        </div>
      </div>

      <div className="max-w-7xl mx-auto px-6">
        {isLoading ? (
          <div className="py-20 text-center">Loading...</div>
        ) : jds.length > 0 ? (
          <div className="grid gap-6 sm:grid-cols-1 md:grid-cols-2 lg:grid-cols-3">
            {jds.map((jd) => (
              <JobSummaryCard key={jd.id} summary={jd} />
            ))}
          </div>
        ) : (
          <div className="rounded-2xl border border-dashed border-gray-200 bg-white py-32 text-center">
            검색 결과가 없습니다.
          </div>
        )}

        {/* 무한스크롤 센티넬 */}
        <div ref={sentinelRef} className="h-10" />

        {isFetchingMore && (
          <div className="py-6 text-center text-sm text-gray-400">불러오는 중...</div>
        )}

        {!hasNext && !isLoading && jds.length > 0 && (
          <div className="py-6 text-center text-sm text-gray-300">모든 공고를 불러왔습니다.</div>
        )}
      </div>
    </div>
  );
};

export default JobSummaryListPage;
