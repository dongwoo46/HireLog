import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { userRequestService } from '../services/userRequestService';
import type { UserRequestListRes } from '../types/userRequest';
import { TbPlus, TbChevronRight, TbInfoCircle, TbBug, TbQuestionMark, TbDotsCircleHorizontal } from 'react-icons/tb';

const UserRequestListPage = () => {
  const navigate = useNavigate();
  const [requests, setRequests] = useState<UserRequestListRes[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const fetchRequests = async () => {
      setIsLoading(true);
      try {
        const data = await userRequestService.getMyRequests();
        setRequests(data || []);
      } catch (error) {
        console.error('Failed to fetch requests', error);
      } finally {
        setIsLoading(false);
      }
    };
    fetchRequests();
  }, []);

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'PENDING': return 'bg-gray-100 text-gray-500';
      case 'IN_PROGRESS': return 'bg-[#276db8]/10 text-[#276db8]';
      case 'COMPLETED': return 'bg-[#89cbb6]/10 text-[#276db8]';
      case 'REJECTED': return 'bg-red-50 text-red-500';
      default: return 'bg-gray-100 text-gray-500';
    }
  };

  const getStatusLabel = (status: string) => {
    switch (status) {
      case 'PENDING': return '대기 중';
      case 'IN_PROGRESS': return '처리 중';
      case 'COMPLETED': return '진행 완료';
      case 'REJECTED': return '반려됨';
      default: return status;
    }
  };

  const getTypeIcon = (type: string) => {
    switch (type) {
      case 'FEATURE': return <TbInfoCircle size={18} className="text-[#276db8]" />;
      case 'BUG': return <TbBug size={18} className="text-red-400" />;
      case 'QUESTION': return <TbQuestionMark size={18} className="text-[#89cbb6]" />;
      default: return <TbDotsCircleHorizontal size={18} className="text-gray-400" />;
    }
  };

  const getTypeLabel = (type: string) => {
    switch (type) {
      case 'FEATURE': return '기능 제안';
      case 'BUG': return '버그 리포트';
      case 'QUESTION': return '문의 사항';
      default: return '기타';
    }
  };

  return (
    <div className="min-h-screen bg-[#F8F9FA] pt-24 pb-20 px-6">
      <div className="max-w-4xl mx-auto">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-6 mb-12">
          <div>
            <h1 className="text-3xl font-black text-gray-900 mb-2 italic">User Requests</h1>
            <p className="text-gray-500 font-medium">서비스 개선을 위한 여러분의 목소리를 들려주세요.</p>
          </div>
          
          <button
            onClick={() => navigate('/requests/new')}
            className="flex items-center justify-center gap-2 px-6 py-3 bg-gradient-to-r from-[#276db8] to-[#89cbb6] text-white font-bold rounded-2xl shadow-lg shadow-[#89cbb6]/20 hover:scale-[1.02] transition-all"
          >
            <TbPlus size={20} />
            새로운 요청 작성
          </button>
        </div>

        {isLoading ? (
          <div className="space-y-4">
            {[...Array(5)].map((_, i) => (
              <div key={i} className="bg-white rounded-2xl h-24 border border-gray-100 animate-pulse" />
            ))}
          </div>
        ) : requests.length > 0 ? (
          <div className="bg-white rounded-3xl border border-gray-100 shadow-sm overflow-hidden">
            <div className="divide-y divide-gray-50">
              {requests.map((request) => (
                <div 
                  key={request.id}
                  onClick={() => navigate(`/requests/${request.id}`)}
                  className="flex items-center justify-between p-6 hover:bg-gray-50 transition-colors cursor-pointer group"
                >
                  <div className="flex items-center gap-6">
                    <div className="hidden sm:flex flex-col items-center justify-center w-12 h-12 rounded-xl bg-gray-50 border border-gray-100 group-hover:border-[#89cbb6]/30 transition-colors">
                      {getTypeIcon(request.requestType)}
                    </div>
                    <div>
                      <div className="flex items-center gap-2 mb-1">
                        <span className={`px-2.5 py-0.5 rounded-full text-[10px] font-black uppercase tracking-wider ${getStatusColor(request.status)}`}>
                          {getStatusLabel(request.status)}
                        </span>
                        <span className="text-xs font-bold text-gray-400">
                          {getTypeLabel(request.requestType)}
                        </span>
                      </div>
                      <h3 className="text-lg font-bold text-gray-900 group-hover:text-[#276db8] transition-colors">
                        {request.title}
                      </h3>
                      <p className="text-xs text-gray-400 mt-1">
                        {new Date(request.createdAt).toLocaleDateString()} 에 작성됨
                      </p>
                    </div>
                  </div>
                  <TbChevronRight size={20} className="text-gray-300 group-hover:text-[#276db8] group-hover:translate-x-1 transition-all" />
                </div>
              ))}
            </div>
          </div>
        ) : (
          <div className="text-center py-32 bg-white rounded-3xl border border-dashed border-gray-200">
            <p className="text-gray-400 text-lg mb-4">작성된 요청이 없습니다.</p>
            <button 
              onClick={() => navigate('/requests/new')}
              className="text-[#276db8] font-bold hover:underline"
            >
              첫 번째 요청 작성하기
            </button>
          </div>
        )}
      </div>
    </div>
  );
};

export default UserRequestListPage;
