import { useState } from 'react';
import { Link } from 'react-router-dom';
import { TbMessageCircle, TbEye, TbSearch, TbPencil } from 'react-icons/tb';

export interface BoardPost {
    id: number;
    category: string;
    title: string;
    content: string;
    author: string;
    createdAt: any;
    views: number;
    commentCount: number;
}

export const formatBoardDate = (dateValue: any) => {
    if (!dateValue) return '';
    if (Array.isArray(dateValue)) {
        const year = dateValue[0];
        const month = dateValue[1];
        const day = dateValue[2];
        return `${year}년 ${month}월 ${day}일`;
    }
    try {
        const d = new Date(dateValue);
        if (!isNaN(d.getTime())) {
            return `${d.getFullYear()}년 ${d.getMonth() + 1}월 ${d.getDate()}일`;
        }
        return String(dateValue).substring(0, 10);
    } catch {
        return String(dateValue);
    }
};

export const MOCK_POSTS: BoardPost[] = [
    {
        id: 1,
        category: '이직/커리어',
        title: '3년차 프론트엔드 이직 고민이 있습니다.',
        content: '안녕하세요, 현재 스타트업에서 3년차로 근무 중인 프론트엔드 개발자입니다.\n요즘 이직을 고민 중인데 시장 상황이 어떤가요? 주로 어떤 기술 스택을 준비하면 좋을지 선배님들의 조언 부탁드립니다.',
        author: '이직가즈아',
        createdAt: '2026-04-01',
        views: 124,
        commentCount: 5,
    },
    {
        id: 2,
        category: '면접/합격',
        title: '네카라쿠배 면접 후기 (합격)',
        content: '운 좋게 이번에 합격하게 되었습니다!\n전반적으로 코딩테스트 난이도는 평이했고 시스템 디자인 면접이 꽤 까다로웠습니다.\n관련해서 질문 있으신 분들 답변해 드릴게요.',
        author: '퇴사준비생',
        createdAt: '2026-03-30',
        views: 532,
        commentCount: 12,
    },
    {
        id: 3,
        category: '회사생활',
        title: '다들 재택근무 아직 하시나요?',
        content: '저희 회사는 다음 달부터 전면 출근으로 바뀐다고 하네요 ㅠㅠ\n판교 쪽 다른 회사들 상황은 어떤지 궁금합니다.',
        author: '판교지박령',
        createdAt: '2026-03-29',
        views: 89,
        commentCount: 3,
    },
];

const BoardListPage = () => {
    const [searchTerm, setSearchTerm] = useState('');
    const [activeCategory, setActiveCategory] = useState<'전체' | '이직/커리어' | '면접/합격' | '회사생활'>('전체');

    const filteredPosts = MOCK_POSTS.filter((post) => {
        const matchCategory = activeCategory === '전체' || post.category === activeCategory;
        const matchSearch = post.title.includes(searchTerm) || post.content.includes(searchTerm);
        return matchCategory && matchSearch;
    });

    return (
        <div className="min-h-screen bg-[#F8F9FA] pt-24 pb-20">
            <div className="max-w-5xl mx-auto px-6">

                {/* 헤더 역영 */}
                <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 mb-8">
                    <div>
                        <h1 className="text-3xl font-black text-gray-900 mb-2">커뮤니티</h1>
                        <p className="text-gray-500 text-sm">현업자들과 다양한 커리어 이야기를 나눠보세요.</p>
                    </div>

                    <button
                        className="flex items-center justify-center gap-2 bg-[#3FB6B2] text-white px-5 py-3 rounded-xl font-bold shadow-sm hover:bg-[#35A09D] hover:shadow-md transition-all sm:w-auto w-full"
                        onClick={() => alert('프론트엔드 임시 과제: 현재 글쓰기 기능은 구현되어 있지 않습니다.')}
                    >
                        <TbPencil size={20} />
                        새 글 쓰기
                    </button>
                </div>

                {/* 필터 및 검색 */}
                <div className="bg-white p-4 rounded-2xl shadow-sm border border-gray-100 flex flex-col md:flex-row gap-4 mb-8 items-center justify-between">
                    <div className="flex gap-2 overflow-x-auto w-full md:w-auto pb-2 md:pb-0 scrollbar-hide shrink-0">
                        {['전체', '이직/커리어', '면접/합격', '회사생활'].map((cat) => (
                            <button
                                key={cat}
                                onClick={() => setActiveCategory(cat as any)}
                                className={`px-4 py-2 rounded-xl text-sm font-bold whitespace-nowrap transition-colors ${activeCategory === cat
                                    ? 'bg-[#3FB6B2] text-white'
                                    : 'bg-gray-50 text-gray-500 hover:bg-gray-100'
                                    }`}
                            >
                                {cat}
                            </button>
                        ))}
                    </div>

                    <div className="relative w-full md:w-72 shrink-0">
                        <TbSearch className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" size={20} />
                        <input
                            type="text"
                            placeholder="게시글 검색..."
                            value={searchTerm}
                            onChange={(e) => setSearchTerm(e.target.value)}
                            className="w-full pl-10 pr-4 py-2.5 bg-gray-50 border border-transparent focus:border-[#3FB6B2] focus:bg-white rounded-xl text-sm outline-none transition"
                        />
                    </div>
                </div>

                {/* 게시글 목록 */}
                <div className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
                    {filteredPosts.length === 0 ? (
                        <div className="py-20 text-center text-gray-400">
                            <TbMessageCircle size={48} className="mx-auto mb-4 opacity-50" />
                            <p>검색 결과가 없습니다.</p>
                        </div>
                    ) : (
                        <div className="divide-y divide-gray-50">
                            {filteredPosts.map((post) => (
                                <Link
                                    key={post.id}
                                    to={`/board/${post.id}`}
                                    className="block p-6 hover:bg-gray-50 transition"
                                >
                                    <div className="flex items-center gap-2 mb-2">
                                        <span className="text-xs font-bold text-[#3FB6B2] bg-[#3FB6B2]/10 px-2 py-1 rounded-md">
                                            {post.category}
                                        </span>
                                        <h3 className="text-lg font-bold text-gray-900 group-hover:text-[#3FB6B2] transition-colors">{post.title}</h3>
                                    </div>

                                    <p className="text-sm text-gray-500 line-clamp-2 mb-4 leading-relaxed">
                                        {post.content}
                                    </p>

                                    <div className="flex items-center justify-between text-xs text-gray-400 font-medium">
                                        <div className="flex items-center gap-3">
                                            <span>{post.author}</span>
                                            <span>·</span>
                                            <span>{formatBoardDate(post.createdAt)}</span>
                                        </div>
                                        <div className="flex items-center gap-4">
                                            <span className="flex items-center gap-1"><TbEye size={16} /> {post.views}</span>
                                            <span className="flex items-center gap-1"><TbMessageCircle size={16} /> {post.commentCount}</span>
                                        </div>
                                    </div>
                                </Link>
                            ))}
                        </div>
                    )}
                </div>

            </div>
        </div>
    );
};

export default BoardListPage;
