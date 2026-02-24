import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { userRequestService } from '../services/userRequestService';
import type { UserRequestDetailRes } from '../types/userRequest';
import {
  TbChevronLeft,
  TbSend,
  TbInfoCircle,
  TbBug,
  TbDotsCircleHorizontal
} from 'react-icons/tb';
import { toast } from 'react-toastify';

const UserRequestDetailPage = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [request, setRequest] = useState<UserRequestDetailRes | null>(null);
  const [commentContent, setCommentContent] = useState('');
  const [isLoading, setIsLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const fetchDetail = async () => {
    if (!id) return;
    setIsLoading(true);
    try {
      const data = await userRequestService.getDetail(parseInt(id));
      setRequest(data);
    } catch (error) {
      toast.error('요청 내용을 불러오지 못했습니다.');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchDetail();
  }, [id]);

  const handleAddComment = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!id || !commentContent.trim()) return;

    setIsSubmitting(true);
    try {
      await userRequestService.addComment(parseInt(id), { content: commentContent });
      setCommentContent('');
      fetchDetail();
    } catch (error) {
      toast.error('댓글 등록 중 오류가 발생했습니다.');
    } finally {
      setIsSubmitting(false);
    }
  };

  if (isLoading) {
    return (
      <div className="min-h-screen bg-[#F8F9FA] pt-32 flex justify-center items-center">
        <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-[#4CDFD5]" />
      </div>
    );
  }

  if (!request) return null;

  return (
    <div className="min-h-screen bg-[#F8F9FA] pt-24 pb-20">
      <div className="max-w-3xl mx-auto px-6">

        {/* 뒤로가기 */}
        <button
          onClick={() => navigate('/requests')}
          className="flex items-center gap-2 text-gray-400 hover:text-gray-700 mb-8 group"
        >
          <TbChevronLeft size={20} className="group-hover:-translate-x-1 transition-transform" />
          <span className="font-semibold">목록으로</span>
        </button>

        {/* 요청 카드 */}
        <div className="bg-white rounded-xl border border-gray-100 shadow-sm overflow-hidden mb-10">

          <div className="px-8 py-6 border-b border-gray-100">
            <div className="flex items-center gap-3 mb-4">

              <span className={`px-3 py-1 rounded-full text-xs font-semibold ${getStatusStyle(request.status)}`}>
                {getStatusLabel(request.status)}
              </span>

              <span className="text-xs text-gray-400 flex items-center gap-1">
                {getTypeIcon(request.requestType)}
                {getTypeLabel(request.requestType)}
              </span>
            </div>

            <h1 className="text-xl font-semibold text-gray-900 mb-2">
              {request.title}
            </h1>

            <p className="text-xs text-gray-400">
              {request.authorName} · {new Date(request.createdAt).toLocaleString()}
            </p>
          </div>

          <div className="px-8 py-6">
            <p className="text-gray-700 leading-relaxed whitespace-pre-line">
              {request.content}
            </p>
          </div>
        </div>

        {/* 댓글 영역 */}
        <div className="space-y-6">

          <h2 className="text-lg font-semibold text-gray-900">
            댓글 ({request.comments?.length || 0})
          </h2>

          <div className="space-y-4">

            {(request.comments || []).map((comment) => (
              <div
                key={comment.id}
                className={`p-5 rounded-xl border ${comment.isAdmin
                  ? 'bg-[#4CDFD5]/10 border-[#4CDFD5]/20'
                  : 'bg-white border-gray-100'
                  }`}
              >
                <div className="flex justify-between items-center mb-2">
                  <div className="flex items-center gap-2">
                    <span className="text-sm font-semibold text-gray-800">
                      {comment.authorName}
                    </span>
                    {comment.isAdmin && (
                      <span className="text-[10px] bg-[#4CDFD5] text-white px-2 py-0.5 rounded-full font-semibold">
                        ADMIN
                      </span>
                    )}
                  </div>
                  <span className="text-xs text-gray-400">
                    {new Date(comment.createdAt).toLocaleString()}
                  </span>
                </div>

                <p className="text-sm text-gray-700 leading-relaxed">
                  {comment.content}
                </p>
              </div>
            ))}

            {(request.comments || []).length === 0 && (
              <div className="text-center py-16 border border-dashed border-gray-200 rounded-xl bg-white">
                <p className="text-gray-400 text-sm">
                  아직 등록된 답변이 없습니다.
                </p>
              </div>
            )}

          </div>

          {/* 댓글 작성 */}
          <form
            onSubmit={handleAddComment}
            className="bg-white rounded-xl border border-gray-100 p-6 shadow-sm"
          >
            <textarea
              placeholder="답변이나 추가 의견을 작성해 주세요."
              className="w-full h-24 px-4 py-3 rounded-lg border border-gray-200 focus:border-[#4CDFD5] focus:ring-4 focus:ring-[#4CDFD5]/20 outline-none resize-none mb-4 text-sm"
              value={commentContent}
              onChange={(e) => setCommentContent(e.target.value)}
            />

            <div className="flex justify-end">
              <button
                type="submit"
                disabled={isSubmitting || !commentContent.trim()}
                className="flex items-center gap-2 px-5 py-2.5 bg-[#4CDFD5] hover:bg-[#3CCFC5] active:bg-[#35C3BA] text-white text-sm font-semibold rounded-lg transition-all disabled:opacity-40"
              >
                <TbSend size={16} />
                {isSubmitting ? '전송 중...' : '보내기'}
              </button>
            </div>
          </form>

        </div>
      </div>
    </div>
  );
};

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
      return <TbInfoCircle size={14} />;
    case 'ERROR_REPORT':
      return <TbBug size={14} />;
    case 'MODIFY_REQUEST':
      return <TbDotsCircleHorizontal size={14} />;
    case 'REPROCESS_REQUEST':
      return <TbDotsCircleHorizontal size={14} />;
    default:
      return <TbDotsCircleHorizontal size={14} />;
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

export default UserRequestDetailPage;
