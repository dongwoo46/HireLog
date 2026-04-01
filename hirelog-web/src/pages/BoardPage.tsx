import { useEffect, useMemo, useState } from 'react';
import { toast } from 'react-toastify';
import { TbFlag3, TbHeart, TbHeartFilled, TbPencil, TbPlus, TbTrash, TbX } from 'react-icons/tb';
import { boardService } from '../services/boardService';
import { reportService } from '../services/reportService';
import { useAuthStore } from '../store/authStore';
import type { BoardItem, BoardType, CommentItem } from '../types/board';
import type { ReportReason, ReportTargetType } from '../types/report';

type BoardForm = { boardType: BoardType; title: string; content: string; anonymous: boolean };
type CommentForm = { content: string; anonymous: boolean };

const emptyBoard: BoardForm = { boardType: 'FREE', title: '', content: '', anonymous: true };
const emptyComment: CommentForm = { content: '', anonymous: true };
const reportTargets: ReportTargetType[] = ['JOB_SUMMARY', 'JOB_SUMMARY_REVIEW', 'MEMBER', 'BOARD', 'COMMENT'];
const reportReasons: ReportReason[] = ['SPAM', 'INAPPROPRIATE', 'FALSE_INFO', 'COPYRIGHT', 'OTHER'];

const errMsg = (e: any, fallback: string) => e?.response?.data?.message || e?.message || fallback;

export default function BoardPage() {
  const { user } = useAuthStore();
  const isAdmin = user?.role === 'ADMIN';
  const likeKey = useMemo(() => `comment-liked:${user?.id ?? 'guest'}`, [user?.id]);

  const [boards, setBoards] = useState<BoardItem[]>([]);
  const [selectedBoardId, setSelectedBoardId] = useState<number | null>(null);
  const [selectedBoard, setSelectedBoard] = useState<BoardItem | null>(null);
  const [comments, setComments] = useState<CommentItem[]>([]);

  const [boardForm, setBoardForm] = useState<BoardForm>(emptyBoard);
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
    const data = await boardService.getBoards({ boardType: 'FREE', page: 0, size: 20 });
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

  useEffect(() => { loadBoards().catch((e) => toast.error(errMsg(e, '게시글 목록 조회 실패'))); }, []);
  useEffect(() => {
    if (!selectedBoardId) return;
    Promise.all([loadBoard(selectedBoardId), loadComments(selectedBoardId)]).catch((e) => toast.error(errMsg(e, '게시글 조회 실패')));
  }, [selectedBoardId]);

  const canBoard = selectedBoard ? isAdmin || !!ownedBoardIds[selectedBoard.id] || (!!user?.username && selectedBoard.authorUsername === user.username) : false;
  const canComment = (c: CommentItem) => isAdmin || !!ownedCommentIds[c.id] || (!!user?.username && c.authorUsername === user.username);

  const submitBoard = async () => {
    if (!boardForm.title.trim() || !boardForm.content.trim()) return toast.warn('제목/내용 입력 필요');
    try {
      if (editingBoard && selectedBoardId) {
        await boardService.updateBoard(selectedBoardId, boardForm);
      } else {
        const res = await boardService.createBoard(boardForm);
        setOwnedBoardIds((p) => ({ ...p, [res.id]: true }));
        setSelectedBoardId(res.id);
      }
      setBoardForm(emptyBoard); setEditingBoard(false); await loadBoards();
    } catch (e) { toast.error(errMsg(e, '게시글 저장 실패')); }
  };

  const submitComment = async () => {
    if (!selectedBoardId || !commentForm.content.trim()) return toast.warn('댓글 내용 입력 필요');
    try {
      const res = await boardService.createComment(selectedBoardId, commentForm);
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
    if (!selectedBoardId || !canComment(comment)) return;
    if (!window.confirm('댓글을 삭제할까요?')) return;
    try {
      await boardService.deleteComment(selectedBoardId, comment.id);
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
    <div className="min-h-screen bg-[#F8F9FA] pb-20 pt-24">
      <div className="mx-auto grid max-w-7xl gap-6 px-6 lg:grid-cols-[360px_1fr]">
        <section className="space-y-4">
          <div className="rounded-2xl border bg-white p-4">
            <h2 className="mb-2 text-sm font-black">{editingBoard ? '게시글 수정' : '게시글 작성'}</h2>
            <input className="mb-2 w-full rounded-xl border px-3 py-2 text-sm" placeholder="제목" value={boardForm.title} onChange={(e) => setBoardForm((p) => ({ ...p, title: e.target.value }))} />
            <textarea className="min-h-[100px] w-full rounded-xl border px-3 py-2 text-sm" placeholder="내용" value={boardForm.content} onChange={(e) => setBoardForm((p) => ({ ...p, content: e.target.value }))} />
            <label className="mt-2 flex items-center gap-2 text-xs"><input type="checkbox" checked={boardForm.anonymous} onChange={(e) => setBoardForm((p) => ({ ...p, anonymous: e.target.checked }))} />익명</label>
            <button onClick={submitBoard} className="mt-2 inline-flex items-center gap-1 rounded-xl bg-[#3FB6B2] px-3 py-2 text-xs font-semibold text-white"><TbPlus size={14} />저장</button>
          </div>
          <div className="rounded-2xl border bg-white p-4">
            <h2 className="mb-2 text-sm font-black">게시글 목록</h2>
            <div className="space-y-2">
              {boards.map((b) => (
                <button key={b.id} onClick={() => setSelectedBoardId(b.id)} className={`w-full rounded-xl border px-3 py-2 text-left ${selectedBoardId === b.id ? 'border-[#3FB6B2] bg-[#3FB6B2]/10' : ''}`}>
                  <p className="line-clamp-1 text-sm font-bold">{b.title}</p>
                  <p className="text-xs text-gray-500">{b.authorUsername || '익명'}</p>
                </button>
              ))}
            </div>
          </div>
        </section>

        <section className="space-y-4">
          <div className="rounded-2xl border bg-white p-5">
            {!selectedBoard ? <p className="text-sm text-gray-400">게시글 선택</p> : (
              <>
                <div className="mb-2 flex items-center justify-between">
                  <h1 className="text-lg font-black">{selectedBoard.title}</h1>
                  <button onClick={() => openReport('BOARD', selectedBoard.id)} className="inline-flex items-center gap-1 rounded-lg border border-rose-200 px-2 py-1 text-xs text-rose-600"><TbFlag3 size={12} />신고</button>
                </div>
                <p className="mb-2 text-xs text-gray-500">{selectedBoard.authorUsername || '익명'}</p>
                <p className="whitespace-pre-line rounded-xl border bg-gray-50 p-3 text-sm">{selectedBoard.content}</p>
                <div className="mt-2 flex gap-2">
                  <button onClick={toggleBoardLike} className="rounded-lg border px-2 py-1 text-xs">{boardLiked ? <TbHeartFilled className="inline" size={12} /> : <TbHeart className="inline" size={12} />} 좋아요 {selectedBoard.likeCount}</button>
                  {canBoard && <button onClick={() => { setEditingBoard(true); setBoardForm({ boardType: selectedBoard.boardType, title: selectedBoard.title, content: selectedBoard.content, anonymous: selectedBoard.anonymous }); }} className="rounded-lg border px-2 py-1 text-xs"><TbPencil className="inline" size={12} />수정</button>}
                  {canBoard && <button onClick={async () => { if (!selectedBoardId) return; await boardService.deleteBoard(selectedBoardId); setSelectedBoardId(null); await loadBoards(); }} className="rounded-lg border border-red-200 px-2 py-1 text-xs text-red-600"><TbTrash className="inline" size={12} />삭제</button>}
                </div>
              </>
            )}
          </div>

          <div className="rounded-2xl border bg-white p-5">
            <div className="mb-2 flex items-center justify-between">
              <h2 className="text-sm font-black">댓글</h2>
              <button onClick={() => openReport()} className="inline-flex items-center gap-1 rounded-lg border border-rose-200 px-2 py-1 text-xs text-rose-600"><TbFlag3 size={12} />신고하기</button>
            </div>
            {selectedBoard && (
              <>
                <textarea className="min-h-[80px] w-full rounded-xl border px-3 py-2 text-sm" value={commentForm.content} onChange={(e) => setCommentForm((p) => ({ ...p, content: e.target.value }))} placeholder="댓글" />
                <div className="mt-2 flex items-center justify-between">
                  <label className="flex items-center gap-2 text-xs"><input type="checkbox" checked={commentForm.anonymous} onChange={(e) => setCommentForm((p) => ({ ...p, anonymous: e.target.checked }))} />익명</label>
                  <button onClick={submitComment} className="rounded-lg bg-[#3FB6B2] px-3 py-1 text-xs font-semibold text-white">등록</button>
                </div>
                <div className="mt-3 space-y-2">
                  {comments.map((c) => (
                    <div key={c.id} className="rounded-xl border p-3">
                      <div className="mb-1 flex items-center justify-between">
                        <p className="text-xs text-gray-500">{c.authorUsername || '익명'}</p>
                        <button onClick={() => openReport('COMMENT', c.id)} className="text-xs text-rose-600">신고</button>
                      </div>
                      {editingCommentId === c.id ? (
                        <>
                          <textarea className="w-full rounded-lg border px-2 py-1 text-sm" value={editingCommentForm.content} onChange={(e) => setEditingCommentForm((p) => ({ ...p, content: e.target.value }))} />
                          <div className="mt-1 flex gap-2">
                            <button onClick={async () => { if (!selectedBoardId) return; await boardService.updateComment(selectedBoardId, c.id, editingCommentForm); setEditingCommentId(null); await loadComments(selectedBoardId); }} className="rounded border px-2 py-1 text-xs">저장</button>
                            <button onClick={() => setEditingCommentId(null)} className="rounded border px-2 py-1 text-xs">취소</button>
                          </div>
                        </>
                      ) : (
                        <>
                          <p className="whitespace-pre-line text-sm">{c.content}</p>
                          <div className="mt-1 flex gap-2">
                            <button onClick={() => toggleCommentLike(c.id)} className="rounded border px-2 py-1 text-xs">{commentLikedByMe[c.id] ? '좋아요 취소' : '좋아요'} ({c.likeCount})</button>
                            {canComment(c) && <button onClick={() => { setEditingCommentId(c.id); setEditingCommentForm({ content: c.content, anonymous: c.anonymous }); }} className="rounded border px-2 py-1 text-xs">수정</button>}
                            {canComment(c) && <button onClick={() => deleteComment(c)} className="rounded border border-red-200 px-2 py-1 text-xs text-red-600">삭제</button>}
                          </div>
                        </>
                      )}
                    </div>
                  ))}
                </div>
              </>
            )}
          </div>
        </section>
      </div>

      {reportOpen && (
        <div className="fixed inset-0 z-[100] flex items-center justify-center bg-black/35 px-4">
          <div className="w-full max-w-lg rounded-2xl border bg-white p-5 shadow-2xl">
            <div className="mb-2 flex items-center justify-between"><h3 className="font-black">신고 접수</h3><button onClick={() => setReportOpen(false)}><TbX size={16} /></button></div>
            <div className="grid gap-2 md:grid-cols-3">
              <select className="rounded-xl border px-3 py-2 text-sm" value={reportForm.targetType} onChange={(e) => setReportForm((p) => ({ ...p, targetType: e.target.value as ReportTargetType }))}>{reportTargets.map((t) => <option key={t} value={t}>{t}</option>)}</select>
              <input className="rounded-xl border px-3 py-2 text-sm" value={reportForm.targetId} onChange={(e) => setReportForm((p) => ({ ...p, targetId: e.target.value }))} placeholder="대상 ID" />
              <select className="rounded-xl border px-3 py-2 text-sm" value={reportForm.reason} onChange={(e) => setReportForm((p) => ({ ...p, reason: e.target.value as ReportReason }))}>{reportReasons.map((r) => <option key={r} value={r}>{r}</option>)}</select>
            </div>
            <textarea className="mt-2 min-h-[90px] w-full rounded-xl border px-3 py-2 text-sm" value={reportForm.detail} onChange={(e) => setReportForm((p) => ({ ...p, detail: e.target.value }))} placeholder="상세 사유" />
            <div className="mt-2 flex justify-end gap-2">
              <button className="rounded-xl border px-3 py-2 text-xs" onClick={() => setReportOpen(false)}>닫기</button>
              <button className="rounded-xl bg-rose-500 px-3 py-2 text-xs font-semibold text-white" onClick={submitReport}>신고 접수</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
