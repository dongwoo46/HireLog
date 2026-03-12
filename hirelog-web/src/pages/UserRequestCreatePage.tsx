import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { userRequestService } from '../services/userRequestService';
import type { UserRequestType } from '../types/userRequest';
import {
  TbChevronLeft,
  TbSend,
  TbInfoCircle,
  TbBug,
  TbDotsCircleHorizontal,
  TbEdit
} from 'react-icons/tb';
import { toast } from 'react-toastify';

const UserRequestCreatePage = () => {
  const navigate = useNavigate();
  const [isLoading, setIsLoading] = useState(false);

  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [requestType, setRequestType] =
    useState<UserRequestType>('FEATURE_REQUEST');

  // ✅ 커스텀 에러 상태
  const [titleError, setTitleError] = useState('');
  const [contentError, setContentError] = useState('');

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    let hasError = false;

    if (!title.trim()) {
      setTitleError('제목을 입력해주세요.');
      hasError = true;
    }

    if (!content.trim()) {
      setContentError('내용을 입력해주세요.');
      hasError = true;
    }

    if (hasError) return;

    setIsLoading(true);

    try {
      await userRequestService.create({ title, content, requestType });
      toast.success('요청이 성공적으로 등록되었습니다.');
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

        {/* 뒤로가기 */}
        <button
          onClick={() => navigate(-1)}
          className="flex items-center gap-2 text-gray-400 hover:text-gray-700 mb-8 transition-all group"
        >
          <TbChevronLeft
            size={20}
            className="group-hover:-translate-x-1 transition-transform"
          />
          <span className="font-semibold">뒤로가기</span>
        </button>

        <div className="bg-white rounded-2xl border border-gray-100 shadow-sm overflow-hidden">

          {/* 상단 */}
          <div className="px-10 py-8 bg-[#4CDFD5]/10 border-b border-gray-100">
            <h1 className="text-2xl font-bold text-gray-900">
              사용자 의견 보내기
            </h1>
            <p className="text-sm text-gray-500 mt-2">
              더 나은 HireLog를 만들기 위한 소중한 의견을 남겨주세요.
            </p>
          </div>

          <form onSubmit={handleSubmit} className="p-10 space-y-8">

            {/* 요청 유형 */}
            <div className="space-y-3">
              <label className="text-xs font-bold text-gray-500 uppercase tracking-wider">
                요청 유형
              </label>

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

            {/* 제목 */}
            <div className="space-y-2">
              <label className="text-xs font-bold text-gray-500 uppercase tracking-wider">
                제목
              </label>
              <input
                type="text"
                placeholder="간결하고 명확한 제목을 입력해주세요."
                value={title}
                onChange={(e) => {
                  setTitle(e.target.value);
                  setTitleError('');
                }}
                className={`w-full px-5 py-4 rounded-xl border outline-none transition-all font-medium
                  ${titleError
                    ? 'border-red-400 bg-red-50'
                    : 'border-gray-200 focus:border-[#4CDFD5] focus:ring-4 focus:ring-[#4CDFD5]/20'
                  }
                `}
              />
              {titleError && (
                <p className="text-sm text-red-500 font-medium animate-fadeIn">
                  {titleError}
                </p>
              )}
            </div>

            {/* 내용 */}
            <div className="space-y-2">
              <label className="text-xs font-bold text-gray-500 uppercase tracking-wider">
                상세 내용
              </label>
              <textarea
                placeholder="구체적인 내용을 작성해주세요."
                value={content}
                onChange={(e) => {
                  setContent(e.target.value);
                  setContentError('');
                }}
                className={`w-full h-64 px-5 py-5 rounded-xl border outline-none transition-all resize-none leading-relaxed
                  ${contentError
                    ? 'border-red-400 bg-red-50'
                    : 'border-gray-200 focus:border-[#4CDFD5] focus:ring-4 focus:ring-[#4CDFD5]/20'
                  }
                `}
              />
              {contentError && (
                <p className="text-sm text-red-500 font-medium animate-fadeIn">
                  {contentError}
                </p>
              )}
            </div>

            {/* 버튼 */}
            <button
              type="submit"
              disabled={isLoading}
              className="w-full py-4 bg-[#4CDFD5] hover:bg-[#3CCFC5] active:bg-[#35C3BA] text-white font-bold text-lg rounded-xl transition-all disabled:opacity-50 flex items-center justify-center gap-3"
            >
              <TbSend size={22} />
              {isLoading ? '전송 중...' : '요청 보내기'}
            </button>

          </form>
        </div>
      </div>
    </div>
  );
};

const TypeOption: React.FC<{
  active: boolean;
  onClick: () => void;
  icon: React.ReactNode;
  label: string;
}> = ({ active, onClick, icon, label }) => (
  <button
    type="button"
    onClick={onClick}
    className={`flex flex-col items-center justify-center gap-2 p-4 rounded-xl border transition-all text-xs font-semibold
      ${active
        ? 'border-[#4CDFD5] bg-[#4CDFD5]/10 text-[#0f172a]'
        : 'border-gray-200 text-gray-400 hover:border-[#4CDFD5]/40'
      }
    `}
  >
    {icon}
    {label}
  </button>
);

export default UserRequestCreatePage;
