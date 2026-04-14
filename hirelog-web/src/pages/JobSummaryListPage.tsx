import { useEffect, useRef, useState, useMemo, useCallback } from 'react';
import { useSearchParams } from 'react-router-dom';
import { toast } from 'react-toastify';
import type { CareerType, JobSummarySearchReq, JobSummaryView } from '../types/jobSummary';
import { jdSummaryService } from '../services/jdSummaryService';
import { JobSummaryCard } from '../components/JobSummaryCard';
import { JobSummarySearch } from '../components/JobSummarySearch';
import { useAuthStore } from '../store/authStore';

const CAREER_TYPE_LABELS: Record<string, string> = {
  NEW: '신입',
  EXPERIENCED: '경력',
  ANY: '무관',
};

const COMPANY_DOMAIN_LABELS: Record<string, string> = {
  FINTECH: '핀테크',
  E_COMMERCE: '이커머스',
  FOOD_DELIVERY: '배달/음식',
  LOGISTICS: '물류/배송',
  MOBILITY: '모빌리티',
  HEALTHCARE: '헬스케어',
  EDTECH: '에듀테크',
  GAME: '게임',
  MEDIA_CONTENT: '미디어/콘텐츠',
  SOCIAL_COMMUNITY: '소셜/커뮤니티',
  TRAVEL_ACCOMMODATION: '여행/숙박',
  REAL_ESTATE: '부동산',
  HR_RECRUITING: 'HR/채용',
  AD_MARKETING: '광고/마케팅',
  AI_ML: 'AI/ML',
  CLOUD_INFRA: '클라우드/인프라',
  SECURITY: '보안',
  ENTERPRISE_SW: '엔터프라이즈 SW',
  BLOCKCHAIN_CRYPTO: '블록체인/크립토',
  MANUFACTURING_IOT: '제조/IoT',
  PUBLIC_SECTOR: '공공',
  OTHER: '기타',
};

const COMPANY_SIZE_LABELS: Record<string, string> = {
  SEED: '시드 스타트업',
  EARLY_STARTUP: '초기 스타트업',
  GROWTH_STARTUP: '성장 스타트업',
  SCALE_UP: '스케일업',
  MID_SIZED: '중소/중견기업',
  LARGE_CORP: '대기업',
  FOREIGN_CORP: '외국계',
  UNKNOWN: '확인불가',
};

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
  const [sideUnsaveId, setSideUnsaveId] = useState<number | null>(null);

  const sentinelRef = useRef<HTMLDivElement>(null);
  const fetchMoreRef = useRef<() => void>(() => {});

  const parseTechStacks = useCallback((): string[] | undefined => {
    const raw = searchParams.get('techStacks');
    if (!raw) return undefined;
    const items = raw
      .split(',')
      .map((item) => item.trim())
      .filter(Boolean);
    return items.length > 0 ? items : undefined;
  }, [searchParams]);

  const parseCsvParam = useCallback(
    (key: string): string[] | undefined => {
      const raw = searchParams.get(key);
      if (!raw) return undefined;
      const items = raw
        .split(',')
        .map((item) => item.trim())
        .filter(Boolean);
      return items.length > 0 ? items : undefined;
    },
    [searchParams],
  );

  const queryKey = useMemo(
    () =>
      JSON.stringify({
        keyword: searchParams.get('keyword') || '',
        careerType: searchParams.get('careerType') || '',
        careerTypes: searchParams.get('careerTypes') || '',
        brandName: searchParams.get('brandName') || '',
        brandNames: searchParams.get('brandNames') || '',
        positionCategoryName: searchParams.get('positionCategoryName') || '',
        positionCategoryNames: searchParams.get('positionCategoryNames') || '',
        positionName: searchParams.get('positionName') || '',
        positionNames: searchParams.get('positionNames') || '',
        techStacks: searchParams.get('techStacks') || '',
        companyDomains: searchParams.get('companyDomains') || '',
        companySizes: searchParams.get('companySizes') || '',
        sortBy: searchParams.get('sortBy') || 'CREATED_AT_DESC',
      }),
    [searchParams],
  );

  const buildParams = useCallback(
    (cursor?: string | null): JobSummarySearchReq => ({
      keyword: searchParams.get('keyword') || undefined,
      careerType: (searchParams.get('careerType') as CareerType) || undefined,
      careerTypes: parseCsvParam('careerTypes') as CareerType[] | undefined,
      brandName: searchParams.get('brandName') || undefined,
      brandNames: parseCsvParam('brandNames'),
      positionCategoryName: searchParams.get('positionCategoryName') || undefined,
      positionCategoryNames: parseCsvParam('positionCategoryNames'),
      positionName: searchParams.get('positionName') || undefined,
      positionNames: parseCsvParam('positionNames'),
      techStacks: parseTechStacks(),
      companyDomains: parseCsvParam('companyDomains'),
      companySizes: parseCsvParam('companySizes'),
      sortBy: searchParams.get('sortBy') || 'CREATED_AT_DESC',
      size: 12,
      cursor: cursor || undefined,
    }),
    [searchParams, parseTechStacks, parseCsvParam],
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
            : await jdSummaryService.getMyApplies(0, 20, undefined);
        setSideJds(result.items || []);
      } catch (e) {
        console.error(e);
      } finally {
        setSideLoading(false);
      }
    };

    fetchSideList();
  }, [isSideOpen, sideFilter]);

  const handleSideUnsave = useCallback(
    async (jobSummaryId: number) => {
      if (sideUnsaveId === jobSummaryId) return;

      setSideUnsaveId(jobSummaryId);
      try {
        await jdSummaryService.unsave(jobSummaryId);
        setSideJds((prev) => prev.filter((item) => item.id !== jobSummaryId));
        setJds((prev) =>
          prev.map((item) =>
            item.id === jobSummaryId
              ? { ...item, isSaved: false, memberSaveType: 'UNSAVED' }
              : item,
          ),
        );
        toast.info('저장을 해제했습니다.');
      } catch (error) {
        console.error(error);
        toast.error('저장 해제에 실패했습니다.');
      } finally {
        setSideUnsaveId(null);
      }
    },
    [sideUnsaveId],
  );

  return (
    <div className="relative min-h-screen bg-[#F6F8FA] pb-20">
      {isAuthenticated && !isSideOpen && (
        <button
          onClick={() => setIsSideOpen(true)}
          className="fixed right-0 top-48 z-50 rounded-l-2xl bg-[#3FB6B2] px-4 py-3 text-sm font-semibold text-white shadow-lg transition hover:bg-[#35A09D]"
        >
          내 저장/지원 공고
        </button>
      )}

      <div
        className={`fixed right-0 top-0 z-40 h-full w-full max-w-md border-l border-gray-100 bg-white shadow-2xl transition-transform duration-300 ${
          isAuthenticated && isSideOpen ? 'translate-x-0' : 'translate-x-full'
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
                className={`rounded-lg px-3 py-2 text-sm font-semibold transition ${
                  sideFilter === 'SAVED' ? 'bg-white text-[#3FB6B2] shadow-sm' : 'text-gray-500'
                }`}
              >
                저장한 공고
              </button>
              <button
                onClick={() => setSideFilter('APPLY')}
                className={`rounded-lg px-3 py-2 text-sm font-semibold transition ${
                  sideFilter === 'APPLY' ? 'bg-white text-[#3FB6B2] shadow-sm' : 'text-gray-500'
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
                <div
                  key={`${jd.id}-${jd.memberSaveType}`}
                  className="rounded-xl border border-gray-100 p-4 transition hover:border-[#3FB6B2]/30 hover:bg-[#3FB6B2]/5"
                >
                  <a href={`/jd/${jd.id}`} className="block">
                    <p className="text-sm font-bold text-gray-900">{jd.brandName}</p>
                    <p className="mt-1 text-xs text-gray-500">{jd.brandPositionName}</p>
                    <div className="mt-2 flex flex-wrap gap-1.5">
                      <span className="rounded-full bg-gray-100 px-2 py-0.5 text-[11px] font-semibold text-gray-600">
                        {CAREER_TYPE_LABELS[jd.careerType] ?? jd.careerType}
                      </span>
                      {jd.companyDomain && (
                        <span className="rounded-full bg-emerald-50 px-2 py-0.5 text-[11px] font-semibold text-emerald-700">
                          {COMPANY_DOMAIN_LABELS[jd.companyDomain] ?? jd.companyDomain}
                        </span>
                      )}
                      {jd.companySize && (
                        <span className="rounded-full bg-blue-50 px-2 py-0.5 text-[11px] font-semibold text-blue-700">
                          {COMPANY_SIZE_LABELS[jd.companySize] ?? jd.companySize}
                        </span>
                      )}
                    </div>
                    <p className="mt-2 text-[11px] text-gray-400">
                      {jd.createdAt ? new Date(jd.createdAt).toLocaleDateString() : ''}
                    </p>
                  </a>
                  <div className="mt-3 flex justify-end">
                    <button
                      type="button"
                      onClick={() => handleSideUnsave(jd.id)}
                      disabled={sideUnsaveId === jd.id}
                      className="rounded-lg border border-gray-200 px-3 py-1.5 text-xs font-semibold text-gray-700 transition hover:border-red-200 hover:bg-red-50 hover:text-red-600 disabled:cursor-not-allowed disabled:opacity-60"
                    >
                      {sideUnsaveId === jd.id ? '처리 중...' : '저장 취소'}
                    </button>
                  </div>
                </div>
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
                if (!value) return;
                if (Array.isArray(value)) {
                  if (value.length > 0) nextParams.set(key, value.join(','));
                  return;
                }
                nextParams.set(key, value.toString());
              });
              setSearchParams(nextParams);
            }}
            initialParams={{
              keyword: searchParams.get('keyword') || '',
              careerType: (searchParams.get('careerType') as CareerType) || undefined,
              careerTypes: parseCsvParam('careerTypes') as CareerType[] | undefined,
              brandName: searchParams.get('brandName') || undefined,
              brandNames: parseCsvParam('brandNames'),
              positionCategoryName: searchParams.get('positionCategoryName') || undefined,
              positionCategoryNames: parseCsvParam('positionCategoryNames'),
              positionName: searchParams.get('positionName') || undefined,
              positionNames: parseCsvParam('positionNames'),
              techStacks: parseTechStacks(),
              companyDomains: parseCsvParam('companyDomains'),
              companySizes: parseCsvParam('companySizes'),
              sortBy: searchParams.get('sortBy') || 'CREATED_AT_DESC',
            }}
          />
        </div>
      </div>

      <div className="mx-auto max-w-7xl px-4 sm:px-6">
        {isLoading ? (
          <div className="py-20 text-center">Loading...</div>
        ) : jds.length > 0 ? (
          <div className="grid grid-cols-2 gap-3 md:gap-6 lg:grid-cols-3">
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
