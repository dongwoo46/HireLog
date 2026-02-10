import { useEffect, useState, useCallback } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { JobSummarySearch } from '../components/JobSummarySearch';
import { JobSummaryCard } from '../components/JobSummaryCard';
import type { JobSummarySearchReq, JobSummaryView, CareerType } from '../types/jobSummary';
import { jdSummaryService } from '../services/jdSummaryService';
import { TbPlus } from 'react-icons/tb';

const JobSummaryListPage = () => {
  const [searchParams, setSearchParams] = useSearchParams();
  const navigate = useNavigate();
  const [jds, setJds] = useState<JobSummaryView[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [totalElements, setTotalElements] = useState(0);

  const fetchJds = useCallback(async () => {
    setIsLoading(true);
    try {
      const isSaved = searchParams.get('filter') === 'saved';
      const params: JobSummarySearchReq = {
        keyword: searchParams.get('keyword') || undefined,
        careerType: (searchParams.get('careerType') as CareerType) || undefined,
        brandId: searchParams.get('brandId') ? parseInt(searchParams.get('brandId')!) : undefined,
        positionId: searchParams.get('positionId') ? parseInt(searchParams.get('positionId')!) : undefined,
        brandPositionId: searchParams.get('brandPositionId') ? parseInt(searchParams.get('brandPositionId')!) : undefined,
        positionCategoryId: searchParams.get('positionCategoryId') ? parseInt(searchParams.get('positionCategoryId')!) : undefined,
        brandName: searchParams.get('brandName') || undefined,
        positionName: searchParams.get('positionName') || undefined,
        brandPositionName: searchParams.get('brandPositionName') || undefined,
        positionCategoryName: searchParams.get('positionCategoryName') || undefined,
        sortBy: searchParams.get('sortBy') || 'CREATED_AT_DESC',
        page: parseInt(searchParams.get('page') || '0'),
        size: 12,
        isSaved: isSaved || undefined,
      };
      const result = await jdSummaryService.search(params);
      setJds(result?.items || []);
      setTotalElements(result?.totalElements || 0);
    } catch (error) {
      console.error('Failed to fetch JDs', error);
    } finally {
      setIsLoading(false);
    }
  }, [searchParams]);

  const isSavedFilter = searchParams.get('filter') === 'saved';

  useEffect(() => {
    fetchJds();
  }, [fetchJds]);

  const handleSearch = (params: JobSummarySearchReq) => {
    const newParams = new URLSearchParams();
    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '') {
        newParams.append(key, value.toString());
      }
    });
    // Preserve the filter=saved if applicable
    if (isSavedFilter) {
      newParams.set('filter', 'saved');
    }
    setSearchParams(newParams);
  };

  return (
    <div className="min-h-screen bg-[#F8F9FA] pt-24 pb-12 px-6">
      <div className="max-w-7xl mx-auto">
        {/* Header Area */}
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-6 mb-8">
          <div>
            <h1 className="text-3xl font-black text-gray-900 mb-2">
              {isSavedFilter ? '저장된 JD 조회' : 'JD 요약 조회'}
            </h1>
            <p className="text-gray-500">
              {isSavedFilter ? '내가 저장한 핵심 JD 목록입니다.' : `인공지능이 분석한 ${totalElements}개의 공고가 있습니다.`}
            </p>
          </div>
          
          <button
            onClick={() => navigate('/jd/request')}
            className="flex items-center justify-center gap-2 px-6 py-3 bg-white border-2 border-[#89cbb6] text-[#276db8] font-bold rounded-2xl hover:bg-[#89cbb6]/5 transition-colors"
          >
            <TbPlus size={20} />
            새로운 JD 요청하기
          </button>
        </div>

        {/* Search & Filters */}
        <div className="mb-12">
          <JobSummarySearch 
            variant="small" 
            onSearch={handleSearch} 
            initialParams={{
              keyword: searchParams.get('keyword') || '',
              careerType: searchParams.get('careerType') as CareerType,
              brandId: searchParams.get('brandId') ? parseInt(searchParams.get('brandId')!) : undefined,
              positionId: searchParams.get('positionId') ? parseInt(searchParams.get('positionId')!) : undefined,
              brandPositionId: searchParams.get('brandPositionId') ? parseInt(searchParams.get('brandPositionId')!) : undefined,
              positionCategoryId: searchParams.get('positionCategoryId') ? parseInt(searchParams.get('positionCategoryId')!) : undefined,
              brandName: searchParams.get('brandName') || '',
              positionName: searchParams.get('positionName') || '',
              brandPositionName: searchParams.get('brandPositionName') || '',
              positionCategoryName: searchParams.get('positionCategoryName') || '',
              sortBy: searchParams.get('sortBy') || 'CREATED_AT_DESC',
            }} 
          />
        </div>

        {/* Results */}
        {isLoading ? (
          <div className="grid grid-cols-1 md:grid-cols-3 lg:grid-cols-4 gap-6">
            {[...Array(8)].map((_, i) => (
              <div key={i} className="bg-white rounded-2xl h-72 border border-gray-100 animate-pulse" />
            ))}
          </div>
        ) : jds.length > 0 ? (
          <div className="grid grid-cols-1 md:grid-cols-3 lg:grid-cols-4 gap-6">
            {jds.map((jd) => (
              <JobSummaryCard key={jd.id} summary={jd} />
            ))}
          </div>
        ) : (
          <div className="text-center py-32 bg-white rounded-3xl border border-dashed border-gray-200">
            <p className="text-gray-400 text-lg mb-4">검색 결과가 없습니다.</p>
            <button 
              onClick={() => handleSearch({})}
              className="text-[#276db8] font-bold hover:underline"
            >
              전체 보기
            </button>
          </div>
        )}

        {/* Simple Pagination could go here */}
      </div>
    </div>
  );
};

export default JobSummaryListPage;
