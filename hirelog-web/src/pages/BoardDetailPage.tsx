import { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { TbChevronLeft, TbEye, TbUser, TbSend } from 'react-icons/tb';
import { MOCK_POSTS } from './BoardListPage';

export interface PostComment {
    id: number;
    author: string;
    content: string;
    createdAt: string;
}

const MOCK_COMMENTS: Record<number, PostComment[]> = {
    1: [
        { id: 101, author: '네카라쿠배', content: '요즘 프론트엔드 시장이 어렵긴 하지만 3년차면 수요가 꽤 있습니다. React 깊이를 좀 더 파보시는 걸 추천해요.', createdAt: '2026-04-01 10:20' },
        { id: 102, author: '스프링러버', content: '백엔드도 마찬가지네요 화이팅입니다!', createdAt: '2026-04-01 11:05' }
    ],
    2: [
        { id: 201, author: '이직가즈아', content: '축하드립니다! 디자인 패턴 질문도 많이 나왔나요?', createdAt: '2026-03-31 09:12' }
    ],
    3: []
};

const BoardDetailPage = () => {
    const { id } = useParams<{ id: string }>();
    const navigate = useNavigate();
    const postId = Number(id);

    const post = MOCK_POSTS.find(p => p.id === postId);

    // 상태 관리
    const [comments, setComments] = useState<PostComment[]>(MOCK_COMMENTS[postId] || []);
    const [newComment, setNewComment] = useState('');

    if (!post) {
        return (
            <div className="min-h-screen bg-[#F8F9FA] pt-32 pb-20 text-center">
                <h2 className="text-2xl font-bold text-gray-700">게시글을 찾을 수 없습니다.</h2>
                <button onClick={() => navigate('/board')} className="mt-4 text-[#3FB6B2] font-bold">목록으로 돌아가기</button>
            </div>
        );
    }

    const handleAddComment = (e: React.FormEvent) => {
        e.preventDefault();
        if (!newComment.trim()) return;

        const comment: PostComment = {
            id: Date.now(),
            author: '익명사용자',
            content: newComment,
            createdAt: new Date().toLocaleString('ko-KR', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
        };

        setComments([...comments, comment]);
        setNewComment('');
    };

    return (
        <div className="min-h-screen bg-[#F8F9FA] pt-24 pb-20">
            <div className="max-w-4xl mx-auto px-6">

                <button onClick={() => navigate('/board')} className="mb-6 flex items-center gap-2 text-gray-500 hover:text-gray-900 transition-colors font-semibold text-sm">
                    <TbChevronLeft size={18} /> 커뮤니티 목록으로
                </button>

                {/* 게시글 본문 */}
                <div className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden mb-8">
                    <div className="p-8 border-b border-gray-50">
                        <span className="text-xs font-bold text-[#3FB6B2] bg-[#3FB6B2]/10 px-2.5 py-1.5 rounded-lg mb-4 inline-block">
                            {post.category}
                        </span>
                        <h1 className="text-2xl font-black text-gray-900 mb-6">{post.title}</h1>

                        <div className="flex items-center justify-between text-sm text-gray-500 pb-6 border-b border-gray-100 mb-6">
                            <div className="flex items-center gap-2 font-medium">
                                <div className="w-8 h-8 rounded-full bg-gray-100 flex items-center justify-center text-gray-400">
                                    <TbUser size={18} />
                                </div>
                                {post.author}
                            </div>
                            <div className="flex items-center gap-4">
                                <span>{post.createdAt}</span>
                                <span className="flex items-center gap-1"><TbEye size={18} /> {post.views}</span>
                            </div>
                        </div>

                        <div className="text-gray-800 leading-relaxed whitespace-pre-line text-[15px] min-h-[150px]">
                            {post.content}
                        </div>
                    </div>
                </div>

                {/* 댓글 영역 */}
                <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-8">
                    <h3 className="font-black text-lg text-gray-900 mb-6 flex items-center gap-2">
                        댓글 <span className="text-[#3FB6B2]">{comments.length}</span>
                    </h3>

                    {/* 댓글 작성 폼 */}
                    <form onSubmit={handleAddComment} className="flex gap-3 mb-8">
                        <input
                            type="text"
                            value={newComment}
                            onChange={(e) => setNewComment(e.target.value)}
                            placeholder="댓글을 남겨보세요."
                            className="flex-1 bg-gray-50 border border-transparent focus:border-[#3FB6B2] focus:bg-white rounded-xl px-4 py-3 text-sm outline-none transition"
                        />
                        <button
                            type="submit"
                            disabled={!newComment.trim()}
                            className="bg-[#3FB6B2] text-white px-6 py-3 rounded-xl font-bold hover:bg-[#35A09D] transition-colors disabled:opacity-50 flex items-center gap-2"
                        >
                            등록 <TbSend size={18} />
                        </button>
                    </form>

                    {/* 댓글 목록 */}
                    <div className="space-y-6">
                        {comments.length === 0 ? (
                            <div className="text-center py-10 text-gray-400 text-sm">
                                첫 번째 댓글을 남겨주세요!
                            </div>
                        ) : (
                            comments.map((comment) => (
                                <div key={comment.id} className="flex gap-4">
                                    <div className="w-10 h-10 rounded-full bg-gray-100 shrink-0 flex items-center justify-center text-gray-400">
                                        <TbUser size={20} />
                                    </div>
                                    <div className="flex-1">
                                        <div className="flex items-center gap-2 mb-1">
                                            <span className="font-bold text-gray-900 text-sm">{comment.author}</span>
                                            <span className="text-xs text-gray-400">{comment.createdAt}</span>
                                        </div>
                                        <p className="text-gray-700 text-[15px] p-4 bg-gray-50 rounded-r-2xl rounded-bl-2xl">
                                            {comment.content}
                                        </p>
                                    </div>
                                </div>
                            ))
                        )}
                    </div>

                </div>

            </div>
        </div>
    );
};

export default BoardDetailPage;
