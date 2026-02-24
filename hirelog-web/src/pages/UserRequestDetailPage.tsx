import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { userRequestService } from '../services/userRequestService';
import type { UserRequestDetailRes } from '../types/userRequest';
import { TbChevronLeft, TbSend, TbInfoCircle, TbBug, TbQuestionMark, TbDotsCircleHorizontal } from 'react-icons/tb';
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
      console.error('Failed to fetch request detail', error);
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
      fetchDetail(); // Refresh
    } catch (error) {
      console.error('Failed to add comment', error);
      toast.error('댓글 등록 중 오류가 발생했습니다.');
    } finally {
      setIsSubmitting(false);
    }
  };

  if (isLoading) {
    return (
      <div className="min-h-screen bg-[#F8F9FA] pt-32 px-6 flex justify-center items-center">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-[#89cbb6]"></div>
      </div>
    );
  }

  if (!request) return null;

  return (
    <div className="min-h-screen bg-[#F8F9FA] pt-24 pb-20">
      <div className="max-w-3xl mx-auto px-6">
        <button 
          onClick={() => navigate('/requests')}
          className="flex items-center gap-2 text-gray-400 hover:text-gray-600 mb-8 transition-colors group"
        >
          <TbChevronLeft size={20} className="group-hover:-translate-x-1 transition-transform" />
          <span className="font-bold">목록으로 돌아가기</span>
        </button>

        <div className="space-y-8">
          {/* Main Request Content */}
          <div className="bg-white rounded-3xl border border-gray-100 shadow-sm overflow-hidden">
            <div className="p-8 md:p-12 border-b border-gray-50 flex justify-between items-start">
              <div>
                <div className="flex items-center gap-3 mb-4">
                  <span className={`px-3 py-1 rounded-full text-[10px] font-black uppercase tracking-wider ${getStatusColor(request.status)}`}>
                    {getStatusLabel(request.status)}
                  </span>
                  <span className="text-xs font-bold text-gray-400 uppercase tracking-widest flex items-center gap-1">
                    {getTypeIcon(request.requestType)}
                    {getTypeLabel(request.requestType)}
                  </span>
                </div>
                <h1 className="text-3xl font-black text-gray-900 mb-2">{request.title}</h1>
                <p className="text-sm text-gray-400 font-medium">
                  {request.authorName}님이 {new Date(request.createdAt).toLocaleString()}에 작성
                </p>
              </div>
            </div>
            
            <div className="p-8 md:p-12">
              <div className="text-gray-700 leading-relaxed text-lg whitespace-pre-line font-medium">
                {request.content}
              </div>
            </div>
          </div>

          {/* Comments Section */}
          <div className="space-y-6">
            <h2 className="text-xl font-black text-gray-900 border-l-4 border-[#89cbb6] pl-4 italic">
              Comments ({(request.comments || []).length})
            </h2>

            <div className="space-y-4">
              {(request.comments || []).map((comment) => (
                <div 
                  key={comment.id}
                  className={`p-6 rounded-2xl border ${
                    comment.isAdmin 
                      ? 'bg-[#276db8]/5 border-[#276db8]/10 ml-8' 
                      : 'bg-white border-gray-100'
                  }`}
                >
                  <div className="flex justify-between items-center mb-3">
                    <div className="flex items-center gap-2">
                      <span className={`text-sm font-black ${comment.isAdmin ? 'text-[#276db8]' : 'text-gray-700'}`}>
                        {comment.authorName}
                      </span>
                      {comment.isAdmin && (
                        <span className="bg-[#276db8] text-white text-[8px] px-1.5 py-0.5 rounded font-black uppercase">Admin</span>
                      )}
                    </div>
                    <span className="text-[10px] text-gray-400 font-bold">
                      {new Date(comment.createdAt).toLocaleString()}
                    </span>
                  </div>
                  <p className="text-gray-600 leading-relaxed font-medium">
                    {comment.content}
                  </p>
                </div>
              ))}

              {(request.comments || []).length === 0 && (
                <div className="text-center py-12 bg-gray-50 rounded-3xl border border-dashed border-gray-200">
                  <p className="text-gray-400 font-bold">아직 등록된 답변이 없습니다.</p>
                </div>
              )}
            </div>

            {/* Add Comment Form */}
            <form onSubmit={handleAddComment} className="bg-white rounded-3xl p-6 border border-gray-100 shadow-sm">
              <textarea
                placeholder="답변이나 추가 의견을 작성해 주세요."
                className="w-full h-24 px-4 py-3 rounded-2xl border border-gray-50 bg-gray-50 focus:bg-white focus:border-[#89cbb6] focus:ring-4 focus:ring-[#89cbb6]/10 outline-none transition-all resize-none mb-4 font-medium"
                value={commentContent}
                onChange={(e) => setCommentContent(e.target.value)}
              />
              <div className="flex justify-end">
                <button
                  type="submit"
                  disabled={isSubmitting || !commentContent.trim()}
                  className="flex items-center gap-2 px-6 py-3 bg-gray-900 text-white font-bold rounded-xl hover:bg-black transition-all disabled:opacity-30"
                >
                  <TbSend size={18} />
                  보내기
                </button>
              </div>
            </form>
          </div>
        </div>
      </div>
    </div>
  );
};

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
    case 'FEATURE': return <TbInfoCircle size={14} />;
    case 'BUG': return <TbBug size={14} />;
    case 'QUESTION': return <TbQuestionMark size={14} />;
    default: return <TbDotsCircleHorizontal size={14} />;
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

export default UserRequestDetailPage;
