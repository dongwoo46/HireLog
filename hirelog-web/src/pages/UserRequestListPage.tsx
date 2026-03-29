import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { userRequestService } from '../services/userRequestService';
import type { UserRequestListRes } from '../types/userRequest';
import {
  TbPlus,
  TbChevronRight,
  TbInfoCircle,
  TbBug,
  TbDotsCircleHorizontal,
  TbEdit
} from 'react-icons/tb';

const UserRequestListPage = () => {
  const navigate = useNavigate();
  const [requests, setRequests] = useState<UserRequestListRes[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const fetchRequests = async () => {
      setIsLoading(true);
      try {
        const data = await userRequestService.getMyRequests();
        setRequests(data?.items || []);
      } catch (error) {
        console.error('Failed to fetch requests', error);
      } finally {
        setIsLoading(false);
      }
    };
    fetchRequests();
  }, []);

  /* ---------------- 상태 스타일 ---------------- */

  const getStatusStyle = (status: string) => {
    switch (status) {
      case 'PENDING':
        return 'bg-gray-100 text-gray-500';
      case 'IN_PROGRESS':
        return 'bg-[#4CDFD5]/15 text-[#0f172a]';
      case 'COMPLETED':
        return 'bg-[#4CDFD5]/20 text-[#0f172a] font-semibold';
      case 'REJECTED':
        return 'bg-red-50 text-red-500';
      default:
        return 'bg-gray-100 text-gray-500';
    }
  };

  const getStatusLabel = (status: string) => {
    switch (status) {
      case 'PENDING':
        return '대기 중';
      case 'IN_PROGRESS':
        return '처리 중';
      case 'COMPLETED':
        return '완료';
      case 'REJECTED':
        return '반려됨';
      default:
        return status;
    }
  };

  const getTypeIcon = (type: string) => {
    switch (type) {
      case 'FEATURE_REQUEST':
        return <TbInfoCircle size={18} className="text-[#4CDFD5]" />;
      case 'ERROR_REPORT':
        return <TbBug size={18} className="text-red-400" />;
      case 'MODIFY_REQUEST':
        return <TbEdit size={18} className="text-[#3FB6B2]" />;
      case 'REPROCESS_REQUEST':
        return <TbDotsCircleHorizontal size={18} className="text-gray-300" />;
      default:
        return <TbDotsCircleHorizontal size={18} className="text-gray-300" />;
    }
  };

  const getTypeLabel = (type: string) => {
    switch (type) {
      case 'FEATURE_REQUEST':
        return '기능 제안';
      case 'ERROR_REPORT':
        return '오류 신고';
      case 'MODIFY_REQUEST':
        return '수정 요청';
      case 'REPROCESS_REQUEST':
        return '재처리 요청';
      default:
        return '기타';
    }
  };

  return (
    <div className="min-h-screen bg-[#F8F9FA] pt-24 pb-20 px-6">
      <div className="max-w-4xl mx-auto">

        {/* 헤더 */}
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-6 mb-12">
          <div>
            <h1 className="text-2xl font-bold text-gray-900">
              나의 요청 내역
            </h1>
            <p className="text-gray-500 text-sm mt-1">
              내가 보낸 의견과 처리 상태를 확인할 수 있습니다.
            </p>
          </div>

          <button
            onClick={() => navigate('/requests/new')}
            className="flex items-center gap-2 px-6 py-3 bg-[#4CDFD5] hover:bg-[#3CCFC5] active:bg-[#35C3BA] text-white font-semibold rounded-xl transition-all"
          >
            <TbPlus size={20} />
            새로운 요청
          </button>
        </div>

        {/* 로딩 */}
        {isLoading ? (
          <div className="space-y-4">
            {[...Array(4)].map((_, i) => (
              <div
                key={i}
                className="bg-white rounded-xl h-20 border border-gray-100 animate-pulse"
              />
            ))}
          </div>
        ) : requests.length > 0 ? (

          <div className="bg-white rounded-xl border border-gray-100 divide-y divide-gray-100 overflow-hidden">

            {requests.map((request) => (
              <div
                key={request.id}
                onClick={() => navigate(`/requests/${request.id}`)}
                className="flex items-center justify-between px-6 py-5 hover:bg-[#4CDFD5]/5 transition-all cursor-pointer group"
              >
                <div className="flex items-center gap-5">

                  <div className="w-10 h-10 flex items-center justify-center rounded-lg bg-gray-50 border border-gray-100">
                    {getTypeIcon(request.requestType)}
                  </div>

                  <div>
                    <div className="flex items-center gap-2 mb-1">
                      <span
                        className={`px-2.5 py-0.5 rounded-full text-[10px] uppercase tracking-wide font-semibold ${getStatusStyle(
                          request.status
                        )}`}
                      >
                        {getStatusLabel(request.status)}
                      </span>

                      <span className="text-xs text-gray-400">
                        {getTypeLabel(request.requestType)}
                      </span>
                    </div>

                    <h3 className="text-base font-semibold text-gray-900 group-hover:text-[#4CDFD5] transition-colors">
                      {request.title}
                    </h3>

                    <p className="text-xs text-gray-400 mt-1">
                      {new Date(request.createdAt).toLocaleDateString()} 작성
                    </p>
                  </div>
                </div>

                <TbChevronRight
                  size={18}
                  className="text-gray-300 group-hover:text-[#4CDFD5] group-hover:translate-x-1 transition-all"
                />
              </div>
            ))}

          </div>

        ) : (

          <div className="text-center py-28 bg-white rounded-xl border border-dashed border-gray-200">
            <p className="text-gray-400 text-sm mb-4">
              아직 작성된 요청이 없습니다.
            </p>
            <button
              onClick={() => navigate('/requests/new')}
              className="text-[#4CDFD5] font-semibold hover:underline"
            >
              첫 요청 작성하기
            </button>
          </div>

        )}
      </div>
    </div>
  );
};

export default UserRequestListPage;
