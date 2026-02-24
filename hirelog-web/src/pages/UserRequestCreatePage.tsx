import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { userRequestService } from '../services/userRequestService';
import type { UserRequestType } from '../types/userRequest';
import { TbChevronLeft, TbSend, TbInfoCircle, TbBug, TbDotsCircleHorizontal, TbEdit } from 'react-icons/tb';
import { toast } from 'react-toastify';

const UserRequestCreatePage = () => {
  const navigate = useNavigate();
  const [isLoading, setIsLoading] = useState(false);
  
  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [requestType, setRequestType] = useState<UserRequestType>('FEATURE_REQUEST');

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!title || !content) {
      toast.warning('제목과 내용을 모두 입력해주세요.');
      return;
    }

    setIsLoading(true);
    try {
      await userRequestService.create({ title, content, requestType });
      navigate('/requests');
    } catch (error: any) {
      toast.error(error.message || '요청 등록 중 오류가 발생했습니다.');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-[#F8F9FA] pt-24 pb-20">
      <div className="max-w-2xl mx-auto px-6">
        <button 
          onClick={() => navigate(-1)}
          className="flex items-center gap-2 text-gray-400 hover:text-gray-600 mb-8 transition-colors group"
        >
          <TbChevronLeft size={20} className="group-hover:-translate-x-1 transition-transform" />
          <span className="font-bold">뒤로가기</span>
        </button>

        <div className="bg-white rounded-3xl shadow-xl shadow-[#89cbb6]/5 border border-gray-100 overflow-hidden">
          <div className="p-10 border-b border-gray-50 bg-gradient-to-r from-[#276db8]/5 to-[#89cbb6]/5">
            <h1 className="text-3xl font-black text-gray-900 mb-2 italic">New Request</h1>
            <p className="text-gray-500">HireLog를 더 좋게 만들기 위한 의견을 적어주세요.</p>
          </div>

          <form onSubmit={handleSubmit} className="p-10 space-y-8">
            <div className="space-y-4">
              <label className="text-sm font-black text-gray-700 uppercase tracking-wider ml-1">요청 유형</label>
              <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
                <TypeOption 
                  active={requestType === 'MODIFY_REQUEST'} 
                  onClick={() => setRequestType('MODIFY_REQUEST')} 
                  icon={<TbEdit size={20} />} 
                  label="수정 요청" 
                />
                <TypeOption 
                  active={requestType === 'ERROR_REPORT'} 
                  onClick={() => setRequestType('ERROR_REPORT')} 
                  icon={<TbBug size={20} />} 
                  label="오류 신고" 
                />
                <TypeOption 
                  active={requestType === 'FEATURE_REQUEST'} 
                  onClick={() => setRequestType('FEATURE_REQUEST')} 
                  icon={<TbInfoCircle size={20} />} 
                  label="기능 제안" 
                />
                <TypeOption 
                  active={requestType === 'REPROCESS_REQUEST'} 
                  onClick={() => setRequestType('REPROCESS_REQUEST')} 
                  icon={<TbDotsCircleHorizontal size={20} />} 
                  label="재처리 요청" 
                />
              </div>
            </div>

            <div className="space-y-2">
              <label className="text-sm font-black text-gray-700 uppercase tracking-wider ml-1">제목</label>
              <input
                type="text"
                placeholder="간결하고 명확한 제목을 입력해주세요."
                className="w-full px-6 py-4 rounded-2xl border border-gray-100 bg-gray-50 focus:bg-white focus:border-[#89cbb6] focus:ring-4 focus:ring-[#89cbb6]/10 outline-none transition-all font-bold"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                required
              />
            </div>

            <div className="space-y-2">
              <label className="text-sm font-black text-gray-700 uppercase tracking-wider ml-1">상세 내용</label>
              <textarea
                placeholder="구체적인 내용을 작성해주시면 확인 후 빠르게 반영하도록 노력하겠습니다."
                className="w-full h-64 px-6 py-5 rounded-2xl border border-gray-100 bg-gray-50 focus:bg-white focus:border-[#89cbb6] focus:ring-4 focus:ring-[#89cbb6]/10 outline-none transition-all resize-none leading-relaxed font-medium"
                value={content}
                onChange={(e) => setContent(e.target.value)}
                required
              />
            </div>

            <button
              type="submit"
              disabled={isLoading}
              className="w-full py-4 bg-gradient-to-r from-[#276db8] to-[#89cbb6] text-white font-black text-xl rounded-2xl shadow-xl shadow-[#89cbb6]/20 hover:scale-[1.02] active:scale-[0.98] transition-all disabled:opacity-50 flex items-center justify-center gap-3"
            >
              <TbSend size={24} />
              {isLoading ? '전송 중...' : '요청 보내기'}
            </button>
          </form>
        </div>
      </div>
    </div>
  );
};

const TypeOption: React.FC<{ active: boolean; onClick: () => void; icon: React.ReactNode; label: string }> = ({ active, onClick, icon, label }) => (
  <button
    type="button"
    onClick={onClick}
    className={`flex flex-col items-center justify-center gap-2 p-4 rounded-2xl border-2 transition-all ${
      active 
        ? 'border-[#89cbb6] bg-[#89cbb6]/5 text-[#276db8]' 
        : 'border-gray-50 bg-gray-50 text-gray-400 hover:border-gray-200'
    }`}
  >
    {icon}
    <span className="text-[10px] font-black uppercase tracking-tighter">{label}</span>
  </button>
);

export default UserRequestCreatePage;
