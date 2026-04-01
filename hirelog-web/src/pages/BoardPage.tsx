import { useEffect, useMemo, useState } from 'react';
import { toast } from 'react-toastify';
import { TbFlag3, TbHeart, TbHeartFilled, TbPencil, TbPlus, TbTrash, TbX, TbMessageCircle, TbSearch } from 'react-icons/tb';
import { boardService } from '../services/boardService';
import { reportService } from '../services/reportService';
import { useAuthStore } from '../store/authStore';
import type { BoardItem, BoardType, CommentItem } from '../types/board';
import type { ReportReason, ReportTargetType } from '../types/report';

type BoardForm = { boardType: BoardType; title: string; content: string; anonymous: boolean; guestPassword: string };
type CommentForm = { content: string; anonymous: boolean; guestPassword: string };

const emptyBoard: BoardForm = { boardType: 'FREE', title: '', content: '', anonymous: true, guestPassword: '' };
const emptyComment: CommentForm = { content: '', anonymous: true, guestPassword: '' };
const reportTargets: ReportTargetType[] = ['JOB_SUMMARY', 'JOB_SUMMARY_REVIEW', 'MEMBER', 'BOARD', 'COMMENT'];
const reportReasons: ReportReason[] = ['SPAM', 'INAPPROPRIATE', 'FALSE_INFO', 'COPYRIGHT', 'OTHER'];
const reportTargetLabels: Record<ReportTargetType, string> = {
  JOB_SUMMARY: '채용공고',
  JOB_SUMMARY_REVIEW: '리뷰',
  MEMBER: '사용자',
  BOARD: '게시글',
  COMMENT: '댓글',
};
const reportReasonLabels: Record<ReportReason, string> = {
  SPAM: '스팸/홍보',
  INAPPROPRIATE: '부적절한 내용',
  FALSE_INFO: '허위 정보',
  COPYRIGHT: '저작권 침해',
  OTHER: '기타',
};

const errMsg = (e: any, fallback: string) => e?.response?.data?.message || e?.message || fallback;

export default function BoardPage() {
  const { user } = useAuthStore();
  const isAdmin = user?.role === 'ADMIN';
  const likeKey = useMemo(() => `comment-liked:${user?.id ?? 'guest'}`, [user?.id]);

  const [boards, setBoards] = useState<BoardItem[]>([]);
  const [selectedBoardId, setSelectedBoardId] = useState<number | null>(null);
  const [selectedBoard, setSelectedBoard] = useState<BoardItem | null>(null);
  const [comments, setComments] = useState<CommentItem[]>([]);
  
  const [keyword, setKeyword] = useState('');
  const [searchInput, setSearchInput] = useState('');
  const [sortBy, setSortBy] = useState<'LATEST' | 'LIKES'>('LATEST');

  const [boardForm, setBoardForm] = useState<BoardForm>(emptyBoard);
  const [isWriting, setIsWriting] = useState(false);
  const [editingBoard, setEditingBoard] = useState(false);
  const [editingCommentId, setEditingCommentId] = useState<number | null>(null);
  const [editingCommentForm, setEditingCommentForm] = useState<CommentForm>(emptyComment);
  const [commentForm, setCommentForm] = useState<CommentForm>(emptyComment);

  const [ownedBoardIds, setOwnedBoardIds] = useState<Record<number, boolean>>({});
  const [ownedCommentIds, setOwnedCommentIds] = useState<Record<number, boolean>>({});
  const [boardLiked, setBoardLiked] = useState(false);
  const [commentLikedByMe, setCommentLikedByMe] = useState<Record<number, boolean>>({});

  const [reportOpen, setReportOpen] = useState(false);
  const [reportForm, setReportForm] = useState<{ targetType: ReportTargetType; targetId: string; reason: ReportReason; detail: string }>({
    targetType: 'BOARD',
    targetId: '',
    reason: 'SPAM',
    detail: '',
  });

  const loadLikes = () => {
    try {
      return JSON.parse(localStorage.getItem(likeKey) || '{}') as Record<number, boolean>;
    } catch {
      return {};
    }
  };
  const saveLikes = (m: Record<number, boolean>) => localStorage.setItem(likeKey, JSON.stringify(m));

  const loadBoards = async () => {
    const data = await boardService.getBoards({ boardType: 'FREE', keyword: keyword || undefined, sortBy, page: 0, size: 20 });
    setBoards(data.items || []);
  };
  const loadBoard = async (id: number) => {
    const [detail, like] = await Promise.all([boardService.getBoardDetail(id), boardService.getBoardLike(id)]);
    setSelectedBoard(detail);
    setBoardLiked(like.liked);
    if (user?.username && detail.authorUsername === user.username) setOwnedBoardIds((p) => ({ ...p, [id]: true }));
  };
  const loadComments = async (id: number) => {
    const data = await boardService.getComments(id, { page: 0, size: 50 });
    const items = data.items || [];
    setComments(items);
    const liked = loadLikes();
    const map: Record<number, boolean> = {};
    items.forEach((c) => {
      if (liked[c.id]) map[c.id] = true;
      if (user?.username && c.authorUsername === user.username) setOwnedCommentIds((p) => ({ ...p, [c.id]: true }));
    });
    setCommentLikedByMe(map);
  };

  useEffect(() => { loadBoards().catch((e) => toast.error(errMsg(e, '게시글 목록 조회 실패'))); }, [keyword, sortBy]);
  useEffect(() => {
    if (!selectedBoardId) return;
    Promise.all([loadBoard(selectedBoardId), loadComments(selectedBoardId)]).catch((e) => toast.error(errMsg(e, '게시글 조회 실패')));
  }, [selectedBoardId]);

  const canBoard = selectedBoard ? isAdmin || !!ownedBoardIds[selectedBoard.id] || (!!user?.username && selectedBoard.authorUsername === user.username) : false;
  const canComment = (c: CommentItem) => isAdmin || !!ownedCommentIds[c.id] || (!!user?.username && c.authorUsername === user.username);
  const canGuestManage = !user;
  const askGuestPassword = () => {
    const value = window.prompt('비로그인 사용자는 비밀번호를 입력해 주세요.');
    if (value == null) return null;
    const trimmed = value.trim();
    if (!trimmed) {
      toast.warn('비밀번호를 입력해 주세요.');
      return null;
    }
    return trimmed;
  };

  const submitBoard = async () => {
    if (!boardForm.title.trim() || !boardForm.content.trim()) return toast.warn('제목/내용 입력 필요');
    if (!user && !boardForm.guestPassword.trim()) return toast.warn('비로그인 작성은 비밀번호가 필요합니다.');
    try {
      if (editingBoard && selectedBoardId) {
        await boardService.updateBoard(selectedBoardId, {
          ...boardForm,
          guestPassword: user ? undefined : boardForm.guestPassword.trim(),
        });
      } else {
        const res = await boardService.createBoard({
          ...boardForm,
          guestPassword: user ? undefined : boardForm.guestPassword.trim(),
        });
        setOwnedBoardIds((p) => ({ ...p, [res.id]: true }));
        setSelectedBoardId(res.id);
      }
      setBoardForm(emptyBoard); setEditingBoard(false); setIsWriting(false); await loadBoards();
    } catch (e) { toast.error(errMsg(e, '게시글 저장 실패')); }
  };

  const submitComment = async () => {
    if (!selectedBoardId || !commentForm.content.trim()) return toast.warn('댓글 내용 입력 필요');
    if (!user && !commentForm.guestPassword.trim()) return toast.warn('비로그인 작성은 비밀번호가 필요합니다.');
    try {
      const res = await boardService.createComment(selectedBoardId, {
        ...commentForm,
        guestPassword: user ? undefined : commentForm.guestPassword.trim(),
      });
      setOwnedCommentIds((p) => ({ ...p, [res.id]: true }));
      setCommentForm(emptyComment);
      await loadComments(selectedBoardId);
    } catch (e) { toast.error(errMsg(e, '댓글 저장 실패')); }
  };

  const toggleBoardLike = async () => {
    if (!selectedBoardId || !selectedBoard) return;
    try {
      const stat = boardLiked ? await boardService.unlikeBoard(selectedBoardId) : await boardService.likeBoard(selectedBoardId);
      setBoardLiked(stat.liked);
      setSelectedBoard((p) => (p ? { ...p, likeCount: stat.likeCount } : p));
    } catch (e) { toast.error(errMsg(e, '좋아요 실패')); }
  };

  const toggleCommentLike = async (id: number) => {
    if (!selectedBoardId) return;
    try {
      const liked = !!commentLikedByMe[id];
      const stat = liked ? await boardService.unlikeComment(selectedBoardId, id) : await boardService.likeComment(selectedBoardId, id);
      const next = { ...commentLikedByMe, [id]: stat.liked };
      setCommentLikedByMe(next); saveLikes(next);
      setComments((p) => p.map((c) => (c.id === id ? { ...c, likeCount: stat.likeCount } : c)));
    } catch (e) { toast.error(errMsg(e, '댓글 좋아요 실패')); }
  };

  const deleteComment = async (comment: CommentItem) => {
    if (!selectedBoardId || (!canComment(comment) && !canGuestManage)) return;
    if (!window.confirm('댓글을 삭제할까요?')) return;
    try {
      const guestPassword = !user ? askGuestPassword() : undefined;
      if (!user && !guestPassword) return;
      await boardService.deleteComment(selectedBoardId, comment.id, guestPassword || undefined);
      await loadComments(selectedBoardId);
    } catch (e) { toast.error(errMsg(e, '댓글 삭제 실패')); }
  };

  const openReport = (t?: ReportTargetType, id?: number) => {
    setReportForm((p) => ({ ...p, targetType: t ?? p.targetType, targetId: id ? String(id) : p.targetId }));
    setReportOpen(true);
  };
  const submitReport = async () => {
    if (!reportForm.targetId) return toast.warn('대상 ID 필요');
    try {
      await reportService.createReport({ targetType: reportForm.targetType, targetId: Number(reportForm.targetId), reason: reportForm.reason, detail: reportForm.detail || undefined });
      setReportOpen(false); setReportForm((p) => ({ ...p, detail: '' }));
      toast.success('신고 접수 완료');
    } catch (e) { toast.error(errMsg(e, '신고 실패')); }
  };

  return (
    <div className="min-h-screen bg-[#F8F9FA] pb-24 pt-32 selection:bg-[#3FB6B2]/20">
      <div className="mx-auto max-w-4xl px-6 mb-12">
        <div className="mb-4 inline-flex items-center gap-2 rounded-full border border-[#3FB6B2]/20 bg-[#3FB6B2]/10 px-4 py-2">
          <span className="h-2 w-2 animate-pulse rounded-full bg-[#3FB6B2]" />
          <span className="text-[10px] font-black uppercase tracking-[0.3em] text-[#3FB6B2]">Community</span>
        </div>
        <h1 className="text-4xl font-black tracking-tight text-gray-900 sm:text-5xl">
          자유게시판
        </h1>
        <p className="mt-4 text-lg text-gray-500 font-medium">
          다양한 채용 경험과 정보를 자유롭게 나누어보세요.
        </p>
      </div>

      <div className="mx-auto max-w-4xl px-6">
        {isWriting || editingBoard ? (
          <div className="overflow-hidden rounded-3xl border border-gray-100 bg-white p-8 shadow-sm transition-shadow hover:shadow-md animate-in fade-in slide-in-from-bottom-4 duration-500">
            <h2 className="mb-6 flex items-center gap-2 text-2xl font-black text-gray-900 pb-6 border-b border-gray-100">
              <TbPencil className="text-[#3FB6B2]" size={28} />
              {editingBoard ? '게시글 수정' : '새 게시글 작성'}
            </h2>
            <div className="space-y-6">
              <div>
                <label className="text-sm font-bold text-gray-700 mb-2 block">제목</label>
                <input
                  className="w-full rounded-2xl border border-gray-200 bg-gray-50/50 px-5 py-4 text-base transition-colors focus:border-[#3FB6B2] focus:bg-white focus:outline-none focus:ring-4 focus:ring-[#3FB6B2]/10"
                  placeholder="제목을 입력하세요"
                  value={boardForm.title}
                  onChange={(e) => setBoardForm((p) => ({ ...p, title: e.target.value }))}
                />
              </div>
              <div>
                <label className="text-sm font-bold text-gray-700 mb-2 block">내용</label>
                <textarea
                  className="min-h-[300px] w-full resize-none rounded-2xl border border-gray-200 bg-gray-50/50 px-5 py-4 text-base transition-colors focus:border-[#3FB6B2] focus:bg-white focus:outline-none focus:ring-4 focus:ring-[#3FB6B2]/10"
                  placeholder="내용을 자유롭게 작성해보세요."
                  value={boardForm.content}
                  onChange={(e) => setBoardForm((p) => ({ ...p, content: e.target.value }))}
                />
              </div>
              {!user && (
                <div>
                  <label className="text-sm font-bold text-gray-700 mb-2 block">비밀번호</label>
                  <input
                    type="password"
                    className="w-full rounded-2xl border border-gray-200 bg-gray-50/50 px-5 py-4 text-base transition-colors focus:border-[#3FB6B2] focus:bg-white focus:outline-none focus:ring-4 focus:ring-[#3FB6B2]/10"
                    placeholder="비로그인 작성/수정/삭제에 사용할 비밀번호"
                    value={boardForm.guestPassword}
                    onChange={(e) => setBoardForm((p) => ({ ...p, guestPassword: e.target.value }))}
                  />
                </div>
              )}

              <div className="flex items-center justify-between pt-6 border-t border-gray-100">
                <label className="flex cursor-pointer items-center gap-2 text-base font-medium text-gray-600 select-none">
                  <input
                    type="checkbox"
                    className="h-5 w-5 rounded border-gray-300 text-[#3FB6B2] focus:ring-transparent"
                    checked={boardForm.anonymous}
                    onChange={(e) => setBoardForm((p) => ({ ...p, anonymous: e.target.checked }))}
                  />
                  익명으로 작성
                </label>
                <div className="flex gap-3">
                  <button
                    onClick={() => { setIsWriting(false); setEditingBoard(false); setBoardForm(emptyBoard); }}
                    className="inline-flex items-center gap-2 rounded-xl border border-gray-200 bg-white px-6 py-3 text-sm font-bold text-gray-600 transition hover:bg-gray-50"
                  >
                    취소
                  </button>
                  <button
                    onClick={submitBoard}
                    className="inline-flex items-center gap-2 rounded-xl bg-[#3FB6B2] px-8 py-3 text-sm font-bold text-white shadow-lg shadow-[#3FB6B2]/30 transition-all hover:-translate-y-0.5 hover:bg-[#32a4a0] hover:shadow-xl hover:shadow-[#3FB6B2]/40 active:translate-y-0"
                  >
                    <TbPlus size={20} />
                    {editingBoard ? '수정 완료' : '게시하기'}
                  </button>
                </div>
              </div>
            </div>
          </div>
        ) : selectedBoard ? (
          <div className="animate-in fade-in slide-in-from-bottom-4 duration-500">
            <button 
              onClick={() => { setSelectedBoardId(null); setSelectedBoard(null); }}
              className="mb-6 inline-flex items-center gap-2 text-sm font-bold text-gray-400 hover:text-gray-900 transition-colors"
            >
              ← 목록으로 돌아가기
            </button>
            <div className="overflow-hidden rounded-3xl border border-gray-100 bg-white shadow-sm mb-6">
              <div className="border-b border-gray-50 bg-[#3FB6B2]/[0.02] p-8">
                <div className="mb-4 flex items-start justify-between">
                  <h1 className="text-3xl font-black leading-tight text-gray-900 break-words pr-8">
                    {selectedBoard.title}
                  </h1>
                  {!selectedBoard.deleted && (
                    <button
                      onClick={() => openReport('BOARD', selectedBoard.id)}
                      className="group flex flex-col items-center justify-center gap-1 rounded-xl border border-gray-100 bg-white px-3 py-2 text-xs font-medium text-gray-400 transition-all hover:border-rose-200 hover:text-rose-600 hover:shadow-sm"
                    >
                      <TbFlag3 size={14} className="group-hover:animate-bounce" />
                    </button>
                  )}
                </div>
                
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <div className="flex h-10 w-10 items-center justify-center rounded-full bg-gradient-to-tr from-[#3FB6B2] to-[#89cbb6] text-white font-bold shadow-inner">
                      {(selectedBoard.authorUsername || '익명').charAt(0).toUpperCase()}
                    </div>
                    <div>
                      <p className="text-sm font-bold text-gray-900">{selectedBoard.authorUsername || '익명'}</p>
                    </div>
                  </div>
                </div>
              </div>

              <div className="p-8">
                <p className="whitespace-pre-line text-lg leading-relaxed text-gray-700 min-h-[160px]">
                  {selectedBoard.content}
                </p>

                <div className="mt-12 flex items-center justify-between border-t border-gray-100 pt-6">
                  <button
                    onClick={toggleBoardLike}
                    className={`flex items-center gap-2 rounded-full border px-5 py-2.5 text-sm font-bold transition-all ${
                      boardLiked
                        ? 'border-rose-200 bg-rose-50 text-rose-500 hover:bg-rose-100'
                        : 'border-gray-200 bg-white text-gray-600 hover:border-rose-200 hover:bg-rose-50 hover:text-rose-500'
                    }`}
                  >
                    {boardLiked ? <TbHeartFilled size={18} className="animate-pulse" /> : <TbHeart size={18} />}
                    좋아요 {selectedBoard.likeCount}
                  </button>

                  <div className="flex gap-2">
                    {(canBoard || canGuestManage) && (
                      <button
                        onClick={() => {
                          setEditingBoard(true);
                          setBoardForm({
                            boardType: selectedBoard.boardType,
                            title: selectedBoard.title,
                            content: selectedBoard.content,
                            anonymous: selectedBoard.anonymous,
                            guestPassword: '',
                          });
                        }}
                        className="flex items-center gap-1.5 rounded-xl border border-gray-200 bg-white px-4 py-2 text-sm font-semibold text-gray-600 transition-all hover:bg-gray-50 focus:ring-4 focus:ring-gray-100"
                      >
                        <TbPencil size={16} /> 수정
                      </button>
                    )}
                    {(canBoard || canGuestManage) && (
                      <button
                        onClick={async () => {
                          if (!window.confirm('정말 삭제하시겠습니까?')) return;
                          if (!selectedBoardId) return;
                          const guestPassword = !user ? askGuestPassword() : undefined;
                          if (!user && !guestPassword) return;
                          await boardService.deleteBoard(selectedBoardId, guestPassword || undefined);
                          setSelectedBoardId(null);
                          await loadBoards();
                        }}
                        className="flex items-center gap-1.5 rounded-xl border border-red-100 bg-red-50/50 px-4 py-2 text-sm font-semibold text-red-600 transition-all hover:bg-red-100 focus:ring-4 focus:ring-red-100"
                      >
                        <TbTrash size={16} /> 삭제
                      </button>
                    )}
                  </div>
                </div>
              </div>
            </div>

            {/* Comments Section */}
            <div className="rounded-3xl border border-gray-100 bg-white p-6 md:p-8 shadow-sm">
              <div className="mb-6 flex items-center justify-between">
                <h2 className="flex items-center gap-2 text-lg font-black text-gray-900">
                  <TbMessageCircle className="text-[#3FB6B2]" size={20} />
                  댓글 <span className="text-[#3FB6B2]">{comments.length}</span>
                </h2>
              </div>

              <div className="mb-8 overflow-hidden rounded-2xl border border-gray-200 focus-within:border-[#3FB6B2] focus-within:ring-4 focus-within:ring-[#3FB6B2]/10 transition-all">
                <textarea
                  className="min-h-[100px] w-full resize-none bg-transparent px-4 py-3 text-sm focus:outline-none"
                  value={commentForm.content}
                  onChange={(e) => setCommentForm((p) => ({ ...p, content: e.target.value }))}
                  placeholder="따뜻한 댓글을 남겨주세요."
                />
                {!user && (
                  <div className="border-t border-gray-100 px-4 py-3">
                    <input
                      type="password"
                      className="w-full rounded-xl border border-gray-200 bg-white px-3 py-2 text-sm focus:border-[#3FB6B2] focus:outline-none focus:ring-2 focus:ring-[#3FB6B2]/20"
                      placeholder="비로그인 댓글 비밀번호"
                      value={commentForm.guestPassword}
                      onChange={(e) => setCommentForm((p) => ({ ...p, guestPassword: e.target.value }))}
                    />
                  </div>
                )}
                <div className="flex items-center justify-between bg-gray-50 px-4 py-3 border-t border-gray-100">
                  <label className="flex cursor-pointer items-center gap-2 text-sm font-medium text-gray-600">
                    <input
                      type="checkbox"
                      className="h-4 w-4 rounded border-gray-300 text-[#3FB6B2] focus:ring-transparent"
                      checked={commentForm.anonymous}
                      onChange={(e) => setCommentForm((p) => ({ ...p, anonymous: e.target.checked }))}
                    />
                    익명
                  </label>
                  <button
                    onClick={submitComment}
                    className="inline-flex items-center gap-2 rounded-xl bg-gray-900 px-5 py-2 text-sm font-bold text-white transition-all hover:bg-gray-800"
                  >
                    <TbMessageCircle size={16} /> 등록
                  </button>
                </div>
              </div>

              <div className="space-y-4">
                {comments.map((c) => (
                  <div key={c.id} className="group rounded-2xl bg-gray-50/50 p-5 transition-colors hover:bg-gray-50">
                    <div className="mb-3 flex items-start justify-between">
                      <div className="flex items-center gap-2">
                        <div className="flex h-8 w-8 items-center justify-center rounded-full bg-gray-200 text-xs font-bold text-gray-600">
                          {(c.authorUsername || '익명').charAt(0).toUpperCase()}
                        </div>
                        <p className="text-sm font-bold text-gray-900">{c.authorUsername || '익명'}</p>
                      </div>
                      {!c.deleted && (
                        <button
                          onClick={() => openReport('COMMENT', c.id)}
                          className="opacity-0 group-hover:opacity-100 transition-opacity text-xs font-medium text-gray-400 hover:text-rose-500"
                        >
                          신고
                        </button>
                      )}
                    </div>

                    {editingCommentId === c.id ? (
                      <div className="mt-2 text-sm">
                        <textarea
                          className="w-full rounded-xl border border-gray-200 px-3 py-2 focus:border-[#3FB6B2] focus:outline-none focus:ring-2 focus:ring-[#3FB6B2]/20"
                          value={editingCommentForm.content}
                          onChange={(e) => setEditingCommentForm((p) => ({ ...p, content: e.target.value }))}
                        />
                        {!user && (
                          <input
                            type="password"
                            className="mt-2 w-full rounded-xl border border-gray-200 px-3 py-2 text-sm focus:border-[#3FB6B2] focus:outline-none focus:ring-2 focus:ring-[#3FB6B2]/20"
                            placeholder="수정 비밀번호"
                            value={editingCommentForm.guestPassword}
                            onChange={(e) => setEditingCommentForm((p) => ({ ...p, guestPassword: e.target.value }))}
                          />
                        )}
                        <div className="mt-2 flex justify-end gap-2">
                          <button
                            onClick={() => setEditingCommentId(null)}
                            className="rounded-lg px-3 py-1.5 text-xs font-semibold text-gray-500 hover:bg-gray-200"
                          >
                            취소
                          </button>
                          <button
                            onClick={async () => {
                              if (!selectedBoardId) return;
                              if (!user && !editingCommentForm.guestPassword.trim()) {
                                toast.warn('비로그인 수정은 비밀번호가 필요합니다.');
                                return;
                              }
                              await boardService.updateComment(selectedBoardId, c.id, {
                                ...editingCommentForm,
                                guestPassword: user ? undefined : editingCommentForm.guestPassword.trim(),
                              });
                              setEditingCommentId(null);
                              await loadComments(selectedBoardId);
                            }}
                            className="rounded-lg bg-[#3FB6B2] px-3 py-1.5 text-xs font-semibold text-white hover:bg-[#32a4a0]"
                          >
                            저장
                          </button>
                        </div>
                      </div>
                    ) : (
                      <>
                        <p className="whitespace-pre-line text-sm text-gray-700 leading-relaxed mb-3">{c.content}</p>
                        <div className="flex items-center justify-between">
                          <button
                            onClick={() => toggleCommentLike(c.id)}
                            className={`flex items-center gap-1 rounded-lg px-2 py-1 text-xs font-semibold transition-colors ${
                              commentLikedByMe[c.id] ? 'text-rose-500 bg-rose-50' : 'text-gray-500 hover:bg-gray-200'
                            }`}
                          >
                            {commentLikedByMe[c.id] ? <TbHeartFilled size={14} /> : <TbHeart size={14} />} {c.likeCount > 0 ? c.likeCount : '좋아요'}
                          </button>
                          <div className="flex items-center gap-3 opacity-0 group-hover:opacity-100 transition-opacity">
                            {(canComment(c) || canGuestManage) && (
                              <button
                                onClick={() => {
                                  setEditingCommentId(c.id);
                                  setEditingCommentForm({ content: c.content, anonymous: c.anonymous, guestPassword: '' });
                                }}
                                className="text-xs font-medium text-gray-500 hover:text-[#3FB6B2]"
                              >
                                수정
                              </button>
                            )}
                            {(canComment(c) || canGuestManage) && (
                              <button
                                onClick={() => deleteComment(c)}
                                className="text-xs font-medium text-gray-500 hover:text-red-500"
                              >
                                삭제
                              </button>
                            )}
                          </div>
                        </div>
                      </>
                    )}
                  </div>
                ))}
                {comments.length === 0 && (
                  <div className="py-6 text-center text-sm text-gray-400">
                    첫 댓글을 남겨보세요.
                  </div>
                )}
              </div>
            </div>
          </div>
        ) : (
          <div className="space-y-4 animate-in fade-in duration-500">
            <div className="mb-6 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
              <h2 className="text-2xl font-black text-gray-900 hidden sm:block">전체 게시글</h2>
              
              <div className="flex flex-1 items-center gap-2 sm:max-w-md">
                <div className="relative flex-1">
                  <div className="pointer-events-none absolute inset-y-0 left-0 flex items-center pl-3 text-gray-400">
                    <TbSearch size={18} />
                  </div>
                  <input
                    type="text"
                    value={searchInput}
                    onChange={(e) => setSearchInput(e.target.value)}
                    onKeyDown={(e) => e.key === 'Enter' && setKeyword(searchInput.trim())}
                    placeholder="관심있는 내용을 검색해보세요"
                    className="w-full rounded-xl border border-gray-200 bg-white py-2 pl-10 pr-4 text-sm transition-colors focus:border-[#3FB6B2] focus:outline-none focus:ring-4 focus:ring-[#3FB6B2]/10"
                  />
                </div>
                <select
                  value={sortBy}
                  onChange={(e) => setSortBy(e.target.value as 'LATEST' | 'LIKES')}
                  className="rounded-xl border border-gray-200 bg-white py-2 pl-3 pr-8 text-sm font-medium text-gray-700 transition-colors focus:border-[#3FB6B2] focus:outline-none focus:ring-4 focus:ring-[#3FB6B2]/10"
                >
                  <option value="LATEST">최신순</option>
                  <option value="LIKES">좋아요순</option>
                </select>
              </div>

              <button
                onClick={() => { setSelectedBoardId(null); setIsWriting(true); }}
                className="inline-flex items-center justify-center gap-2 rounded-xl bg-[#3FB6B2] px-6 py-2.5 text-sm font-bold text-white shadow-lg shadow-[#3FB6B2]/30 transition-all hover:-translate-y-0.5 hover:bg-[#32a4a0] hover:shadow-xl shrink-0"
              >
                <TbPencil size={18} /> 새 글 작성
              </button>
            </div>
            
            {boards.map((b) => (
              <button
                key={b.id}
                onClick={() => { setSelectedBoardId(b.id); setIsWriting(false); setEditingBoard(false); }}
                className="block w-full rounded-3xl border border-gray-100 bg-white p-6 text-left transition-all hover:border-[#3FB6B2]/30 hover:shadow-md hover:-translate-y-1"
              >
                <div className="flex flex-col gap-2">
                  <h3 className="line-clamp-1 text-xl font-bold text-gray-900">{b.title}</h3>
                  <p className="line-clamp-2 text-base text-gray-500 leading-relaxed mb-4">{b.content}</p>
                </div>
                <div className="flex items-center justify-between border-t border-gray-50 pt-4">
                  <div className="flex items-center gap-2">
                     <div className="flex h-7 w-7 items-center justify-center rounded-full bg-gradient-to-tr from-gray-200 to-gray-300 text-xs font-bold text-gray-600 shadow-inner">
                        {(b.authorUsername || '익명').charAt(0).toUpperCase()}
                     </div>
                     <span className="text-sm font-bold text-gray-600">{b.authorUsername || '익명'}</span>
                  </div>
                  <div className="flex items-center gap-4 text-sm font-semibold text-gray-400">
                    <span className="flex items-center gap-1.5"><TbHeart size={18} className={b.likeCount > 0 ? "text-rose-400 stroke-rose-400" : ""} /> {b.likeCount}</span>
                    <span className="flex items-center gap-1.5"><TbMessageCircle size={18} className="text-[#3FB6B2]" /> {b.commentCount || 0}</span>
                  </div>
                </div>
              </button>
            ))}
            
            {boards.length === 0 && (
              <div className="py-24 text-center text-gray-400 flex flex-col items-center">
                <TbMessageCircle size={48} className="mb-4 text-gray-200" />
                <p className="text-lg font-medium">아직 작성된 꿀팁/후기가 없습니다.<br />첫 번째 글의 주인공이 되어보세요!</p>
              </div>
            )}
          </div>
        )}
      </div>

      {/* Report Modal */}
      {reportOpen && (
        <div className="fixed inset-0 z-[100] flex items-center justify-center bg-gray-900/40 px-4 backdrop-blur-sm transition-all duration-300">
          <div className="w-full max-w-lg rounded-3xl border border-gray-100 bg-white p-8 shadow-2xl animate-in fade-in zoom-in-95 duration-200">
            <div className="mb-6 flex items-center justify-between">
              <h3 className="text-xl font-black text-gray-900 flex items-center gap-2">
                <TbFlag3 className="text-rose-500" /> 신고 접수
              </h3>
              <button 
                onClick={() => setReportOpen(false)}
                className="rounded-full p-2 text-gray-400 hover:bg-gray-100 hover:text-gray-600 transition-colors"
              >
                <TbX size={20} />
              </button>
            </div>
            
            <div className="grid gap-4 md:grid-cols-2 mb-4">
              <div className="space-y-1.5">
                <label className="text-xs font-bold text-gray-500">신고 대상 (자동선택)</label>
                <select
                  disabled
                  className="w-full rounded-xl border border-gray-200 bg-gray-50 px-4 py-3 text-sm font-medium text-gray-600"
                  value={reportForm.targetType}
                  onChange={(e) => setReportForm((p) => ({ ...p, targetType: e.target.value as ReportTargetType }))}
                >
                  {reportTargets.map((t) => <option key={t} value={t}>{reportTargetLabels[t]}</option>)}
                </select>
              </div>
              <div className="space-y-1.5">
                <label className="text-xs font-bold text-gray-500">신고 사유</label>
                <select
                  className="w-full rounded-xl border border-gray-200 px-4 py-3 text-sm font-medium text-gray-900 focus:border-[#3FB6B2] focus:outline-none focus:ring-4 focus:ring-[#3FB6B2]/10"
                  value={reportForm.reason}
                  onChange={(e) => setReportForm((p) => ({ ...p, reason: e.target.value as ReportReason }))}
                >
                  {reportReasons.map((r) => <option key={r} value={r}>{reportReasonLabels[r]}</option>)}
                </select>
              </div>
            </div>
            
            <div className="space-y-1.5 mb-8">
              <label className="text-xs font-bold text-gray-500">상세 내용</label>
              <textarea
                className="min-h-[120px] w-full resize-none rounded-xl border border-gray-200 px-4 py-3 text-sm focus:border-[#3FB6B2] focus:outline-none focus:ring-4 focus:ring-[#3FB6B2]/10"
                value={reportForm.detail}
                onChange={(e) => setReportForm((p) => ({ ...p, detail: e.target.value }))}
                placeholder="신고 사유를 구체적으로 작성해주세요. (허위 신고시 불이익이 있을 수 있습니다)"
              />
            </div>
            
            <div className="flex justify-end gap-3">
              <button
                className="rounded-xl border border-gray-200 bg-white px-6 py-3 text-sm font-bold text-gray-600 hover:bg-gray-50 transition-colors"
                onClick={() => setReportOpen(false)}
              >
                취소
              </button>
              <button
                className="rounded-xl bg-rose-500 px-6 py-3 text-sm font-bold text-white shadow-lg shadow-rose-500/30 hover:bg-rose-600 transition-colors"
                onClick={submitReport}
              >
                신고 접수하기
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
