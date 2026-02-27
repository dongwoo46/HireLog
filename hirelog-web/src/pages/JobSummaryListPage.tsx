// import { useEffect, useState, useCallback } from 'react';
// import { useSearchParams } from 'react-router-dom';
// import type {
//   JobSummarySearchReq,
//   JobSummaryView,
//   CareerType,
// } from '../types/jobSummary';
// import { jdSummaryService } from '../services/jdSummaryService';
// import { JobSummaryCard } from '../components/JobSummaryCard';
// import { JobSummarySearch } from '../components/JobSummarySearch';
// import { Pagination } from '../components/common/Pagination';

// const JobSummaryListPage = () => {
//   const [searchParams, setSearchParams] = useSearchParams();

//   const [jds, setJds] = useState<JobSummaryView[]>([]);
//   const [isLoading, setIsLoading] = useState(true);
//   const [totalPages, setTotalPages] = useState(0);
//   const [totalElements, setTotalElements] = useState(0);

//   /**
//    * JD Fetch
//    */
//   const fetchJds = useCallback(async () => {
//     setIsLoading(true);

//     try {
//       const params: JobSummarySearchReq = {
//         keyword: searchParams.get('keyword') || undefined,
//         careerType: (searchParams.get('careerType') as CareerType) || undefined,

//         brandId: searchParams.get('brandId')
//           ? Number(searchParams.get('brandId'))
//           : undefined,

//         brandName: searchParams.get('brandName') || undefined,

//         positionId: searchParams.get('positionId')
//           ? Number(searchParams.get('positionId'))
//           : undefined,

//         positionName: searchParams.get('positionName') || undefined,

//         positionCategoryId: searchParams.get('positionCategoryId')
//           ? Number(searchParams.get('positionCategoryId'))
//           : undefined,

//         positionCategoryName:
//           searchParams.get('positionCategoryName') || undefined,

//         page: parseInt(searchParams.get('page') || '0'),
//         size: 10,

//         sortBy:
//           (searchParams.get('sortBy') as any) || 'CREATED_AT_DESC',
//       };

//       const result = await jdSummaryService.search(params);

//       setJds(result?.items || []);
//       setTotalPages(result?.totalPages || 0);
//       setTotalElements(result?.totalElements || 0);
//     } catch (error) {
//       console.error('Failed to fetch JDs', error);
//     } finally {
//       setIsLoading(false);
//     }
//   }, [searchParams]);

//   useEffect(() => {
//     fetchJds();
//   }, [fetchJds]);

//   /**
//    * 검색 및 필터 적용
//    */
//   const handleSearch = (newParams: JobSummarySearchReq) => {
//     const nextParams = new URLSearchParams();

//     Object.entries(newParams).forEach(([key, value]) => {
//       if (value !== undefined && value !== '') {
//         nextParams.set(key, value.toString());
//       }
//     });

//     nextParams.set('page', '0');
//     setSearchParams(nextParams);
//   };

//   return (
//     <div className="min-h-screen bg-[#F8F9FA] pb-20">

//       {/* 검색 헤더 */}
//       <div className="pt-32 pb-8 px-6">
//         <div className="max-w-5xl mx-auto">
//           <JobSummarySearch
//             onSearch={handleSearch}
//             initialParams={{
//               keyword: searchParams.get('keyword') || '',
//               careerType:
//                 (searchParams.get('careerType') as CareerType) || undefined,
//               brandId: searchParams.get('brandId')
//                 ? Number(searchParams.get('brandId'))
//                 : undefined,
//               brandName: searchParams.get('brandName') || undefined,
//               positionId: searchParams.get('positionId')
//                 ? Number(searchParams.get('positionId'))
//                 : undefined,
//               positionName: searchParams.get('positionName') || undefined,
//               positionCategoryId: searchParams.get('positionCategoryId')
//                 ? Number(searchParams.get('positionCategoryId'))
//                 : undefined,
//               positionCategoryName:
//                 searchParams.get('positionCategoryName') || undefined,
//               sortBy: searchParams.get('sortBy') || 'CREATED_AT_DESC',
//             }}
//           />
//         </div>
//       </div>

//       {/* 리스트 영역 */}
//       <div className="max-w-5xl mx-auto px-6">
//         <div className="mb-8 flex justify-between items-center">
//           <h2 className="text-sm font-bold text-gray-400 uppercase tracking-widest">
//             Log Archive ({totalElements})
//           </h2>
//         </div>

//         {isLoading ? (
//           <div className="flex flex-col gap-6">
//             {[...Array(4)].map((_, i) => (
//               <div
//                 key={i}
//                 className="h-64 bg-white rounded-[2rem] border border-gray-100 animate-pulse"
//               />
//             ))}
//           </div>
//         ) : jds.length > 0 ? (
//           <div className="flex flex-col gap-6">
//             {jds.map((jd) => (
//               <JobSummaryCard key={jd.id} summary={jd} />
//             ))}
//           </div>
//         ) : (
//           <div className="text-center py-32 bg-white rounded-[2.5rem] border border-dashed border-gray-200">
//             <p className="text-gray-400 font-medium">
//               검색 결과가 없습니다.
//             </p>
//           </div>
//         )}

//         {/* 페이지네이션 */}
//         {totalPages > 1 && (
//           <div className="mt-20">
//             <Pagination
//               currentPage={parseInt(searchParams.get('page') || '0')}
//               totalPages={totalPages}
//               onPageChange={(page) => {
//                 const nextParams = new URLSearchParams(searchParams);
//                 nextParams.set('page', page.toString());
//                 setSearchParams(nextParams);
//                 window.scrollTo({ top: 0, behavior: 'smooth' });
//               }}
//             />
//           </div>
//         )}
//       </div>
//     </div>
//   );
// };

// export default JobSummaryListPage;
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
  const [totalElements, setTotalElements] = useState(0);

  const fetchJds = useCallback(async () => {
    setIsLoading(true);

    try {
      const params: JobSummarySearchReq = {
        keyword: searchParams.get('keyword') || undefined,
        careerType: (searchParams.get('careerType') as CareerType) || undefined,

        brandId: searchParams.get('brandId')
          ? Number(searchParams.get('brandId'))
          : undefined,

        brandName: searchParams.get('brandName') || undefined,

        positionId: searchParams.get('positionId')
          ? Number(searchParams.get('positionId'))
          : undefined,

        positionName: searchParams.get('positionName') || undefined,

        positionCategoryId: searchParams.get('positionCategoryId')
          ? Number(searchParams.get('positionCategoryId'))
          : undefined,

        positionCategoryName:
          searchParams.get('positionCategoryName') || undefined,

        page: parseInt(searchParams.get('page') || '0'),
        size: 12, // 카드형이라 12개가 보기 좋음

        sortBy:
          (searchParams.get('sortBy') as any) || 'CREATED_AT_DESC',
      };

      const result = await jdSummaryService.search(params);

      setJds(result?.items || []);
      setTotalPages(result?.totalPages || 0);
      setTotalElements(result?.totalElements || 0);
    } catch (error) {
      console.error('Failed to fetch JDs', error);
    } finally {
      setIsLoading(false);
    }
  }, [searchParams]);

  useEffect(() => {
    fetchJds();
  }, [fetchJds]);

  const handleSearch = (newParams: JobSummarySearchReq) => {
    const nextParams = new URLSearchParams();

    Object.entries(newParams).forEach(([key, value]) => {
      if (value !== undefined && value !== '') {
        nextParams.set(key, value.toString());
      }
    });

    nextParams.set('page', '0');
    setSearchParams(nextParams);
  };

  return (
    <div className="min-h-screen bg-[#F8F9FA] pb-20">

      {/* 검색 영역 */}
      <div className="pt-32 pb-10 px-6">
        <div className="max-w-7xl mx-auto">
          <JobSummarySearch
            onSearch={handleSearch}
            initialParams={{
              keyword: searchParams.get('keyword') || '',
              careerType:
                (searchParams.get('careerType') as CareerType) || undefined,
              brandId: searchParams.get('brandId')
                ? Number(searchParams.get('brandId'))
                : undefined,
              brandName: searchParams.get('brandName') || undefined,
              positionId: searchParams.get('positionId')
                ? Number(searchParams.get('positionId'))
                : undefined,
              positionName: searchParams.get('positionName') || undefined,
              positionCategoryId: searchParams.get('positionCategoryId')
                ? Number(searchParams.get('positionCategoryId'))
                : undefined,
              positionCategoryName:
                searchParams.get('positionCategoryName') || undefined,
              sortBy: searchParams.get('sortBy') || 'CREATED_AT_DESC',
            }}
          />
        </div>
      </div>

      {/* 카드 그리드 영역 */}
      <div className="max-w-7xl mx-auto px-6">
        <div className="mb-10 flex justify-between items-center">
          <h2 className="text-sm font-bold text-gray-400 uppercase tracking-widest">
            Log Archive ({totalElements})
          </h2>
        </div>

        {isLoading ? (
          <div className="grid gap-6 sm:grid-cols-1 md:grid-cols-2 lg:grid-cols-3">
            {[...Array(6)].map((_, i) => (
              <div
                key={i}
                className="h-60 bg-white rounded-2xl border border-gray-100 animate-pulse"
              />
            ))}
          </div>
        ) : jds.length > 0 ? (
          <div className="grid gap-6 sm:grid-cols-1 md:grid-cols-2 lg:grid-cols-3">
            {jds.map((jd) => (
              <div key={jd.id} className="h-full">
                <JobSummaryCard summary={jd} />
              </div>
            ))}
          </div>
        ) : (
          <div className="text-center py-32 bg-white rounded-2xl border border-dashed border-gray-200">
            <p className="text-gray-400 font-medium">
              검색 결과가 없습니다.
            </p>
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
                window.scrollTo({ top: 0, behavior: 'smooth' });
              }}
            />
          </div>
        )}
      </div>
    </div>
  );
};

export default JobSummaryListPage;
