import { useEffect, useState, useCallback } from 'react';
import { useSearchParams } from 'react-router-dom';
import type {
  JobSummarySearchReq,
  JobSummaryView,
  CareerType,
} from '../types/jobSummary';
import { jdSummaryService } from '../services/jdSummaryService';
import { JobSummaryCard } from '../components/JobSummaryCard';
import { JobSummarySearch } from '../components/JobSummarySearch';
import { Pagination } from '../components/common/Pagination';

const JobSummaryListPage = () => {
  const [searchParams, setSearchParams] = useSearchParams();

  const [jds, setJds] = useState<JobSummaryView[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [totalPages, setTotalPages] = useState(0);

  /* ================== 사이드 상태 ================== */
  const [isSideOpen, setIsSideOpen] = useState(false);
  const [filterOpen, setFilterOpen] = useState(false);
  const [sideFilter, setSideFilter] =
    useState<'MY_POSTED' | 'MY_SAVED' | null>(null);
  const [sideJds, setSideJds] = useState<JobSummaryView[]>([]);
  const [sideLoading, setSideLoading] = useState(false);

  /* ================== 메인 리스트 ================== */

  const fetchJds = useCallback(async () => {
    setIsLoading(true);
    try {
      const params: JobSummarySearchReq = {
        keyword: searchParams.get('keyword') || undefined,
        careerType:
          (searchParams.get('careerType') as CareerType) || undefined,
        page: parseInt(searchParams.get('page') || '0'),
        size: 12,
        sortBy: searchParams.get('sortBy') || 'CREATED_AT_DESC',
      };

      const result = await jdSummaryService.search(params);

      setJds(result?.items || []);
      setTotalPages(result?.totalPages || 0);
    } catch (error) {
      console.error(error);
    } finally {
      setIsLoading(false);
    }
  }, [searchParams]);

  useEffect(() => {
    fetchJds();
  }, [fetchJds]);

  /* ================== 사이드 리스트 ================== */

  const fetchSideList = async (type: 'MY_POSTED' | 'MY_SAVED') => {
    setSideLoading(true);
    try {
      let result;
      if (type === 'MY_POSTED') {
        result = await jdSummaryService.getMyRegistrations(0, 20);
      } else {
        result = await jdSummaryService.getMySaves(0, 20);
      }
      setSideJds(result.items || []);
    } catch (e) {
      console.error(e);
    } finally {
      setSideLoading(false);
    }
  };

  const handleSideFilter = (type: 'MY_POSTED' | 'MY_SAVED') => {
    setSideFilter(type);
    fetchSideList(type);
    setFilterOpen(false);
  };

  /* ================== 렌더 ================== */

  return (
    <div className="min-h-screen bg-[#F8F9FA] pb-20 relative">

      {/* 🔥 사이드 패널 토글용 플로팅 버튼은 리스트 겹침 문제로 본문 상단으로 이동됨 */}

      {/* ================== 사이드 패널 ================== */}
      <div
        className={`fixed top-0 right-0 h-full w-96 bg-white shadow-2xl border-l border-gray-100 transition-transform duration-300 z-40 ${isSideOpen ? 'translate-x-0' : 'translate-x-full'
          }`}
      >
        <div className="h-full flex flex-col">

          {/* ===== 상단 헤더 ===== */}
          <div className="flex items-center justify-between px-6 pt-24 pb-4 border-b border-gray-100">

            <div className="flex items-center gap-3">

              <h3 className="text-sm font-bold text-gray-400 uppercase tracking-widest">
                공고 필터
              </h3>

              <div className="relative">
                <button
                  onClick={() => setFilterOpen(!filterOpen)}
                  className="p-2 rounded-lg hover:bg-gray-100 transition"
                >
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
                    <path
                      d="M3 5h18M6 12h12M10 19h4"
                      stroke="#6B7280"
                      strokeWidth="2"
                      strokeLinecap="round"
                    />
                  </svg>
                </button>

                {filterOpen && (
                  <div className="absolute right-0 mt-2 w-48 bg-white border border-gray-200 rounded-xl shadow-lg z-50">
                    <button
                      onClick={() => handleSideFilter('MY_POSTED')}
                      className="block w-full text-left px-4 py-3 hover:bg-gray-50 text-sm"
                    >
                      내가 등록한 공고
                    </button>

                    <button
                      onClick={() => handleSideFilter('MY_SAVED')}
                      className="block w-full text-left px-4 py-3 hover:bg-gray-50 text-sm"
                    >
                      내가 저장한 공고
                    </button>

                    <button
                      onClick={() => {
                        setSideFilter(null);
                        setSideJds([]);
                        setFilterOpen(false);
                      }}
                      className="block w-full text-left px-4 py-3 hover:bg-gray-50 text-sm text-rose-500"
                    >
                      전체 보기
                    </button>
                  </div>
                )}
              </div>

              {sideFilter && (
                <span className="px-3 py-1 bg-[#3FB6B2]/10 text-[#3FB6B2] text-xs font-bold rounded-full">
                  {sideFilter === 'MY_POSTED'
                    ? '등록한 공고'
                    : '저장한 공고'}
                </span>
              )}
            </div>

            <button
              onClick={() => setIsSideOpen(false)}
              className="text-gray-400 hover:text-gray-700 text-lg font-bold"
            >
              ✕
            </button>
          </div>

          {/* ===== 리스트 ===== */}
          <div className="flex-1 overflow-y-auto p-6 space-y-4">
            {sideLoading ? (
              <div className="text-center text-gray-400 text-sm">
                불러오는 중...
              </div>
            ) : sideJds.length > 0 ? (
              sideJds.map((jd) => (
                <div
                  key={jd.id}
                  className="border border-gray-100 rounded-xl p-4 hover:shadow-sm transition"
                >
                  <p className="font-bold text-sm mb-1">{jd.brandName}</p>
                  <p className="text-xs text-gray-500">
                    {jd.brandPositionName}
                  </p>
                </div>
              ))
            ) : (
              <div className="text-center text-gray-400 text-sm mt-10">
                선택된 공고가 없습니다.
              </div>
            )}
          </div>
        </div>
      </div>

      {/* ================== 메인 영역 ================== */}

      <div className="pt-32 pb-10 px-6">
        <div className="max-w-7xl mx-auto">
          <JobSummarySearch
            onSearch={(params) => {
              const nextParams = new URLSearchParams();
              Object.entries(params).forEach(([key, value]) => {
                if (value) nextParams.set(key, value.toString());
              });
              nextParams.set('page', '0');
              setSearchParams(nextParams);
            }}
            initialParams={{
              keyword: searchParams.get('keyword') || '',
              careerType:
                (searchParams.get('careerType') as CareerType) || undefined,
              sortBy: searchParams.get('sortBy') || 'CREATED_AT_DESC',
            }}
          />
        </div>
      </div>

      <div className="max-w-7xl mx-auto px-6">
        <div className="flex justify-end mb-6">
          <button
            onClick={() => setIsSideOpen(true)}
            className="group flex items-center gap-2 bg-white border border-gray-200 text-gray-700 px-5 py-2.5 rounded-full shadow-sm hover:shadow-md hover:border-[#3FB6B2] hover:text-[#3FB6B2] transition-all duration-300 font-medium text-sm"
          >
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="text-gray-400 group-hover:text-[#3FB6B2] transition-colors">
              <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
              <polyline points="14 2 14 8 20 8"></polyline>
              <line x1="16" y1="13" x2="8" y2="13"></line>
              <line x1="16" y1="17" x2="8" y2="17"></line>
              <polyline points="10 9 9 9 8 9"></polyline>
            </svg>
            등록 / 저장 공고 보기
          </button>
        </div>

        {isLoading ? (
          <div className="text-center py-20">Loading...</div>
        ) : jds.length > 0 ? (
          <div className="grid gap-6 sm:grid-cols-1 md:grid-cols-2 lg:grid-cols-3">
            {jds.map((jd) => (
              <JobSummaryCard key={jd.id} summary={jd} />
            ))}
          </div>
        ) : (
          <div className="text-center py-32 bg-white rounded-2xl border border-dashed border-gray-200">
            검색 결과가 없습니다.
          </div>
        )}

        {totalPages > 1 && (
          <div className="mt-20">
            <Pagination
              currentPage={parseInt(searchParams.get('page') || '0')}
              totalPages={totalPages}
              onPageChange={(page) => {
                const nextParams = new URLSearchParams(searchParams);
                nextParams.set('page', page.toString());
                setSearchParams(nextParams);
              }}
            />
          </div>
        )}
      </div>
    </div>
  );
};

export default JobSummaryListPage;
