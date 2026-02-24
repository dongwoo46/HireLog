import { useNavigate, useParams } from 'react-router-dom';
import { useEffect, useState } from 'react';
import { jdSummaryService } from '../services/jdSummaryService';
import {
  HIRING_STAGE_LABELS,
  type JobSummaryDetailView,
  type HiringStage,
  type ReviewWriteReq,
} from '../types/jobSummary';
import {
  TbChevronLeft,
  TbStar,
  TbStarFilled,
  TbExternalLink,
  TbBulb,
  TbBriefcase,
  TbFileText,
  TbListCheck,
  TbSparkles,
  TbLink,
  TbBrandGithub,
} from 'react-icons/tb';
import { toast } from 'react-toastify';

interface Memo {
  id: number;
  content: string;
  createdAt: string;
}

const CAREER_LABELS: Record<string, string> = {
  NEW: '신입',
  EXPERIENCED: '경력',
  ANY: '경력 무관',
};

const JobSummaryDetailPage = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const [jd, setJd] = useState<JobSummaryDetailView | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [activeTab, setActiveTab] = useState<'detail' | 'review'>('detail');
  const [isMemoOpen, setIsMemoOpen] = useState(false);

  const [memos, setMemos] = useState<Memo[]>([]);
  const [editingMemoId, setEditingMemoId] = useState<number | null>(null);
  const [memoText, setMemoText] = useState('');

  const [reviewForm, setReviewForm] = useState<ReviewWriteReq>({
    hiringStage: 'DOCUMENT',
    anonymous: true,
    difficultyRating: 3,
    satisfactionRating: 3,
    experienceComment: '',
    interviewTip: '',
  });
  const [reviewError, setReviewError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  // JD 불러오기
  useEffect(() => {
    const fetchJd = async () => {
      if (!id) return;
      try {
        setIsLoading(true);
        const data = await jdSummaryService.getDetail(Number(id));
        // 백엔드가 jobSummaryId 필드로 반환하는 경우도 처리
        const normalized = {
          ...data,
          id: data.id || (data as any).jobSummaryId || Number(id),
        };
        setJd(normalized);
      } catch (e: any) {
        setError('공고를 불러오는데 실패했습니다.');
        toast.error('공고 정보를 불러올 수 없습니다.');
      } finally {
        setIsLoading(false);
      }
    };
    fetchJd();
  }, [id]);

  // 메모 불러오기
  useEffect(() => {
    const fetchMemos = async () => {
      if (!jd || !isMemoOpen) return;
      try {
        const data = await jdSummaryService.getMemos(jd.id);
        setMemos(data);
      } catch {
        toast.error('메모를 불러올 수 없습니다.');
      }
    };
    fetchMemos();
  }, [isMemoOpen, jd]);

  const handleSaveMemo = async () => {
    if (!memoText.trim() || !jd) return;
    try {
      if (editingMemoId) {
        await jdSummaryService.updateMemo(editingMemoId, memoText);
      } else {
        await jdSummaryService.addMemo(jd.id, memoText);
      }
      const updated = await jdSummaryService.getMemos(jd.id);
      setMemos(updated);
      setMemoText('');
      setEditingMemoId(null);
      toast.success('메모가 저장되었습니다.');
    } catch {
      toast.error('메모 저장에 실패했습니다.');
    }
  };

  const handleDeleteMemo = async (memoId: number) => {
    try {
      await jdSummaryService.deleteMemo(memoId);
      setMemos(prev => prev.filter(m => m.id !== memoId));
      toast.success('메모가 삭제되었습니다.');
    } catch {
      toast.error('메모 삭제에 실패했습니다.');
    }
  };

  const handleAddReview = async () => {
    const targetId = jd?.id || Number(id);
    if (!targetId) return;
    setReviewError(null);

    if (reviewForm.experienceComment.trim().length < 10) {
      setReviewError('경험 후기는 10자 이상 작성해 주세요.');
      return;
    }

    setIsSubmitting(true);
    try {
      await jdSummaryService.addReview(targetId, reviewForm);
      toast.success('리뷰가 성공적으로 등록되었습니다.');
      setReviewForm({
        hiringStage: 'DOCUMENT',
        anonymous: true,
        difficultyRating: 3,
        satisfactionRating: 3,
        experienceComment: '',
        interviewTip: '',
      });
      const updated = await jdSummaryService.getDetail(targetId);
      const normalized = { ...updated, id: updated.id || (updated as any).jobSummaryId || targetId };
      setJd(normalized);
    } catch (e: any) {
      const message = e.response?.data?.message || '리뷰 등록에 실패했습니다.';
      setReviewError(message);
      toast.error(message);
    } finally {
      setIsSubmitting(false);
    }
  };

  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-[#F8F9FA]">
        <div className="h-10 w-10 border-4 border-gray-200 border-t-[#3FB6B2] rounded-full animate-spin" />
      </div>
    );
  }

  if (error || !jd) {
    return (
      <div className="min-h-screen flex flex-col items-center justify-center bg-[#F8F9FA] gap-4">
        <p className="text-gray-500 font-medium">{error || '공고를 찾을 수 없습니다.'}</p>
        <button onClick={() => navigate(-1)} className="text-[#3FB6B2] font-bold underline underline-offset-4">
          돌아가기
        </button>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-[#F8F9FA] pt-24 pb-20">
      <div className="max-w-4xl mx-auto px-6">

        {/* 뒤로가기 */}
        <div className="mb-6">
          <button
            onClick={() => navigate(-1)}
            className="flex items-center gap-2 text-sm text-gray-500 hover:text-gray-800 transition-colors"
          >
            <TbChevronLeft size={18} />
            목록으로
          </button>
        </div>

        {/* 헤더 카드 */}
        <div className="rounded-3xl overflow-hidden mb-8 shadow-lg">
          <div className="bg-gradient-to-r from-[#3FB6B2] to-[#6EC8A7] px-10 py-12 relative">

            <button
              onClick={() => setIsMemoOpen(true)}
              className="absolute top-6 right-6 bg-white/90 backdrop-blur px-4 py-2 text-sm font-bold rounded-xl shadow-sm hover:bg-white transition-colors"
            >
              📝 메모
            </button>

            {/* 경력 배지 */}
            {jd.careerType && (
              <span className="inline-block mb-4 px-3 py-1 bg-white/20 text-white text-xs font-bold rounded-full">
                {CAREER_LABELS[jd.careerType] || jd.careerType}
                {jd.careerYears ? ` ${jd.careerYears}년` : ''}
              </span>
            )}

            <h1 className="text-5xl font-black text-white mb-2">{jd.brandName}</h1>
            <h2 className="text-xl font-semibold text-white/90 mb-1">{jd.brandPositionName}</h2>

            {jd.positionName && jd.positionName !== jd.brandPositionName && (
              <p className="text-white/70 text-sm mb-2">{jd.positionCategoryName && `${jd.positionCategoryName} · `}{jd.positionName}</p>
            )}

            {/* 기술 스택 */}
            {jd.techStack && (
              <div className="flex flex-wrap gap-2 mt-6">
                {jd.techStack.split(',').map((tech, i) => (
                  <span
                    key={i}
                    className="px-3 py-1 bg-white/20 text-white text-xs font-medium rounded-full backdrop-blur-sm"
                  >
                    {tech.trim()}
                  </span>
                ))}
              </div>
            )}

            {/* 원본 링크 */}
            {jd.sourceUrl && (
              <a
                href={jd.sourceUrl}
                target="_blank"
                rel="noopener noreferrer"
                className="inline-flex items-center gap-1.5 mt-6 text-white/80 hover:text-white text-sm font-medium transition-colors"
              >
                <TbLink size={16} />
                원본 공고 보기
                <TbExternalLink size={14} />
              </a>
            )}
          </div>
        </div>

        {/* 탭 */}
        <div className="flex gap-6 border-b border-gray-200 mb-8 text-sm font-bold">
          <button
            onClick={() => setActiveTab('detail')}
            className={`pb-3 transition-colors ${activeTab === 'detail'
              ? 'border-b-2 border-[#3FB6B2] text-[#3FB6B2]'
              : 'text-gray-400 hover:text-gray-600'
              }`}
          >
            상세정보
          </button>
          <button
            onClick={() => setActiveTab('review')}
            className={`pb-3 transition-colors ${activeTab === 'review'
              ? 'border-b-2 border-[#3FB6B2] text-[#3FB6B2]'
              : 'text-gray-400 hover:text-gray-600'
              }`}
          >
            리뷰 보기 ({jd.reviews?.length || 0})
          </button>
        </div>

        {/* ─────────────── 상세정보 탭 ─────────────── */}
        {activeTab === 'detail' && (
          <div className="space-y-5">

            {/* AI 인사이트 */}
            {jd.insights && (
              <div className="bg-gradient-to-br from-[#3FB6B2]/10 to-[#6EC8A7]/10 border border-[#3FB6B2]/20 rounded-2xl p-6">
                <div className="flex items-center gap-2 mb-4">
                  <TbSparkles size={20} className="text-[#3FB6B2]" />
                  <h2 className="text-base font-black text-[#3FB6B2] uppercase tracking-widest">AI 인사이트</h2>
                </div>
                <p className="text-sm text-gray-700 leading-relaxed whitespace-pre-line">{jd.insights}</p>
              </div>
            )}

            {/* 요약 */}
            {jd.summaryText && (
              <ContentSection icon={<TbFileText />} title="요약">
                {jd.summaryText}
              </ContentSection>
            )}

            {/* 주요 업무 */}
            {jd.responsibilities && (
              <ContentSection icon={<TbBriefcase />} title="주요 업무">
                {jd.responsibilities}
              </ContentSection>
            )}

            {/* 자격 요건 */}
            {jd.requiredQualifications && (
              <ContentSection icon={<TbListCheck />} title="자격 요건">
                {jd.requiredQualifications}
              </ContentSection>
            )}

            {/* 우대 사항 */}
            {jd.preferredQualifications && (
              <ContentSection icon={<TbBulb />} title="우대 사항">
                {jd.preferredQualifications}
              </ContentSection>
            )}

            {/* 채용 프로세스 */}
            {jd.recruitmentProcess && (
              <div className="bg-white border border-gray-100 rounded-2xl p-6 shadow-sm">
                <div className="flex items-center gap-2 mb-4">
                  <span className="text-gray-400"><TbListCheck size={18} /></span>
                  <h2 className="text-sm font-black text-gray-400 uppercase tracking-widest">채용 프로세스</h2>
                </div>
                <div className="flex flex-wrap items-center gap-2">
                  {jd.recruitmentProcess.split(/[→\-,\n]/).map((step, i, arr) => (
                    <div key={i} className="flex items-center gap-2">
                      <span className="px-4 py-2 bg-gray-50 border border-gray-100 rounded-full text-sm font-bold text-gray-700">
                        {step.trim()}
                      </span>
                      {i < arr.length - 1 && (
                        <span className="text-gray-300 font-bold">→</span>
                      )}
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* 원본 공고 링크 */}
            {jd.sourceUrl && (
              <div className="flex justify-center pt-4">
                <a
                  href={jd.sourceUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="inline-flex items-center gap-2 px-6 py-3 bg-white border border-gray-200 rounded-2xl text-sm font-bold text-gray-600 hover:border-[#3FB6B2] hover:text-[#3FB6B2] transition-all shadow-sm"
                >
                  <TbBrandGithub size={16} />
                  원본 공고 보러가기
                  <TbExternalLink size={14} />
                </a>
              </div>
            )}
          </div>
        )}

        {/* ─────────────── 리뷰 탭 ─────────────── */}
        {activeTab === 'review' && (
          <div className="space-y-8">

            {/* 리뷰 작성 폼 */}
            <div className="bg-white border border-gray-100 rounded-3xl p-8 shadow-sm">
              <h3 className="text-xl font-black text-gray-900 mb-8 flex items-center gap-2">
                <span className="w-1.5 h-7 bg-[#3FB6B2] rounded-full" />
                경험 기록하기
              </h3>

              {/* 전형 단계 + 익명 */}
              <div className="grid md:grid-cols-2 gap-6 mb-8">
                <div className="space-y-2">
                  <label className="text-xs font-black text-gray-400 uppercase tracking-widest">전형 단계</label>
                  <select
                    value={reviewForm.hiringStage}
                    onChange={(e) => setReviewForm({ ...reviewForm, hiringStage: e.target.value as HiringStage })}
                    className="w-full h-12 px-4 bg-gray-50 border border-gray-100 rounded-xl outline-none focus:border-[#3FB6B2] font-bold text-sm"
                  >
                    {Object.entries(HIRING_STAGE_LABELS).map(([value, label]) => (
                      <option key={value} value={value}>{label}</option>
                    ))}
                  </select>
                </div>

                <div className="flex items-center justify-between px-5 py-4 bg-gray-50 rounded-xl border border-gray-100 self-end">
                  <span className="text-sm font-bold text-gray-600">익명으로 작성</span>
                  <button
                    type="button"
                    onClick={() => setReviewForm({ ...reviewForm, anonymous: !reviewForm.anonymous })}
                    className={`w-12 h-6 rounded-full transition-all relative ${reviewForm.anonymous ? 'bg-[#3FB6B2]' : 'bg-gray-200'}`}
                  >
                    <div className={`absolute top-1 w-4 h-4 bg-white rounded-full shadow transition-all ${reviewForm.anonymous ? 'left-7' : 'left-1'}`} />
                  </button>
                </div>
              </div>

              {/* 별점 */}
              <div className="grid md:grid-cols-2 gap-6 mb-8">
                <StarRating
                  label="난이도"
                  value={reviewForm.difficultyRating}
                  onChange={(v) => setReviewForm({ ...reviewForm, difficultyRating: v })}
                />
                <StarRating
                  label="만족도"
                  value={reviewForm.satisfactionRating}
                  onChange={(v) => setReviewForm({ ...reviewForm, satisfactionRating: v })}
                />
              </div>

              {/* 텍스트 입력 */}
              <div className="space-y-5">
                <div className="space-y-2">
                  <label className="text-xs font-black text-gray-400 uppercase tracking-widest">경험 후기 (10자 이상)</label>
                  <textarea
                    value={reviewForm.experienceComment}
                    onChange={(e) => setReviewForm({ ...reviewForm, experienceComment: e.target.value })}
                    placeholder="면접 분위기, 질문 내용, 전반적인 경험을 들려주세요."
                    className="w-full h-32 p-4 bg-gray-50 border border-gray-100 rounded-xl outline-none focus:border-[#3FB6B2] resize-none text-sm font-medium"
                  />
                </div>

                <div className="space-y-2">
                  <label className="text-xs font-black text-gray-400 uppercase tracking-widest">나만의 면접 팁 (선택)</label>
                  <textarea
                    value={reviewForm.interviewTip}
                    onChange={(e) => setReviewForm({ ...reviewForm, interviewTip: e.target.value })}
                    placeholder="다음 지원자에게 도움이 될만한 꿀팁이 있다면 적어주세요."
                    className="w-full h-24 p-4 bg-gray-50 border border-gray-100 rounded-xl outline-none focus:border-[#3FB6B2] resize-none text-sm font-medium"
                  />
                </div>
              </div>

              <div className="flex flex-col items-center gap-3 mt-8">
                {reviewError && (
                  <p className="text-sm font-bold text-rose-500 bg-rose-50 px-4 py-2 rounded-lg">
                    {reviewError}
                  </p>
                )}
                <button
                  onClick={handleAddReview}
                  disabled={isSubmitting}
                  className="px-12 py-3.5 bg-[#3FB6B2] text-white font-black rounded-2xl shadow-lg shadow-[#3FB6B2]/20 hover:bg-[#35A09D] active:scale-95 transition-all disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  {isSubmitting ? '등록 중...' : '기록 완료하기'}
                </button>
              </div>
            </div>

            {/* 리뷰 목록 */}
            {jd.reviews && jd.reviews.length > 0 ? (
              <div className="space-y-4">
                {jd.reviews.map((review: any, i: number) => (
                  <div key={i} className="bg-white border border-gray-100 rounded-2xl p-7 shadow-sm hover:shadow-md transition-shadow">

                    <div className="flex justify-between items-start mb-5">
                      <div className="space-y-2">
                        <span className="inline-block px-3 py-1 bg-[#3FB6B2]/10 text-[#3FB6B2] text-[10px] font-black rounded-lg uppercase tracking-wider">
                          {HIRING_STAGE_LABELS[review.hiringStage as HiringStage] || review.hiringStage}
                        </span>
                        <div className="flex items-center gap-4">
                          <div className="flex items-center gap-1.5">
                            <span className="text-[10px] font-black text-gray-400 uppercase tracking-wider">난이도</span>
                            <StarDisplay value={review.difficultyRating} max={5} />
                          </div>
                          <div className="flex items-center gap-1.5">
                            <span className="text-[10px] font-black text-gray-400 uppercase tracking-wider">만족도</span>
                            <StarDisplay value={review.satisfactionRating} max={5} />
                          </div>
                        </div>
                      </div>
                      <span className="text-[10px] text-gray-300 font-bold uppercase tracking-widest">
                        {new Date(review.createdAt).toLocaleDateString('ko-KR')}
                      </span>
                    </div>

                    <p className="text-gray-700 leading-relaxed text-sm font-medium whitespace-pre-line mb-5">
                      {review.experienceComment}
                    </p>

                    {review.interviewTip && (
                      <div className="bg-amber-50 rounded-xl p-4 border border-amber-100">
                        <p className="text-xs font-black text-amber-500 uppercase tracking-widest mb-2">💡 Interview Tip</p>
                        <p className="text-sm text-gray-600 font-medium italic">
                          "{review.interviewTip}"
                        </p>
                      </div>
                    )}

                    <div className="mt-5 pt-4 border-t border-gray-50">
                      <span className="text-xs text-gray-400 font-bold">
                        by {review.anonymous ? 'Anonymous' : (review.memberName || 'Hunter')}
                      </span>
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <div className="text-center py-20 bg-white rounded-3xl border border-dashed border-gray-200">
                <p className="text-gray-400 font-medium">아직 등록된 리뷰가 없습니다.</p>
                <p className="text-gray-300 text-sm mt-1">첫 번째 리뷰를 남겨보세요!</p>
              </div>
            )}
          </div>
        )}

        {/* 메모 모달 */}
        {isMemoOpen && (
          <div
            className="fixed inset-0 bg-black/40 backdrop-blur-sm flex items-center justify-center z-50"
            onClick={(e) => { if (e.target === e.currentTarget) setIsMemoOpen(false); }}
          >
            <div className="bg-white w-full max-w-xl rounded-3xl p-8 relative shadow-2xl mx-4">

              <button
                onClick={() => setIsMemoOpen(false)}
                className="absolute top-5 right-5 text-gray-400 hover:text-gray-700 text-xl font-bold transition-colors"
              >
                ✕
              </button>

              <h3 className="text-lg font-black mb-6">📝 메모 관리</h3>

              <textarea
                value={memoText}
                onChange={(e) => setMemoText(e.target.value)}
                placeholder="메모를 입력하세요..."
                className="w-full border border-gray-200 rounded-2xl p-4 h-28 resize-none outline-none focus:border-[#3FB6B2] text-sm"
              />

              <div className="flex justify-end gap-3 mt-4">
                {editingMemoId && (
                  <button
                    onClick={() => { setEditingMemoId(null); setMemoText(''); }}
                    className="px-4 py-2 text-gray-400 text-sm font-bold hover:text-gray-600"
                  >
                    취소
                  </button>
                )}
                <button
                  onClick={handleSaveMemo}
                  className="px-5 py-2 bg-[#3FB6B2] text-white text-sm font-bold rounded-xl hover:bg-[#35A09D] transition-colors"
                >
                  {editingMemoId ? '수정 저장' : '메모 저장'}
                </button>
              </div>

              <div className="mt-6 space-y-3 max-h-60 overflow-y-auto">
                {memos.length === 0 && (
                  <p className="text-sm text-gray-400 text-center py-4">메모가 없습니다.</p>
                )}
                {memos.map((memo) => (
                  <div key={memo.id} className="border border-gray-100 p-4 rounded-xl bg-gray-50">
                    <p className="text-sm text-gray-700">{memo.content}</p>
                    <div className="flex justify-end gap-4 mt-2 text-xs">
                      <button
                        onClick={() => { setEditingMemoId(memo.id); setMemoText(memo.content); }}
                        className="text-[#3FB6B2] font-bold hover:underline"
                      >
                        수정
                      </button>
                      <button
                        onClick={() => handleDeleteMemo(memo.id)}
                        className="text-red-400 font-bold hover:underline"
                      >
                        삭제
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

/* ─── Sub-Components ─── */

const ContentSection: React.FC<{ icon: React.ReactNode; title: string; children: any }> = ({ icon, title, children }) => (
  <div className="bg-white border border-gray-100 rounded-2xl p-6 shadow-sm">
    <div className="flex items-center gap-2 mb-4">
      <span className="text-gray-400">{icon}</span>
      <h2 className="text-sm font-black text-gray-400 uppercase tracking-widest">{title}</h2>
    </div>
    <div className="text-sm text-gray-700 leading-relaxed whitespace-pre-line">{children}</div>
  </div>
);

const StarRating = ({ label, value, onChange }: { label: string; value: number; onChange: (v: number) => void }) => {
  const [hovered, setHovered] = useState(0);
  return (
    <div className="space-y-2">
      <label className="text-xs font-black text-gray-400 uppercase tracking-widest">{label}</label>
      <div className="flex items-center gap-1.5">
        {[1, 2, 3, 4, 5].map((star) => (
          <button
            key={star}
            type="button"
            onClick={() => onChange(star)}
            onMouseEnter={() => setHovered(star)}
            onMouseLeave={() => setHovered(0)}
            className="transition-transform hover:scale-110 active:scale-95"
          >
            {(hovered || value) >= star
              ? <TbStarFilled size={28} className="text-[#3FB6B2]" />
              : <TbStar size={28} className="text-gray-200" />
            }
          </button>
        ))}
        <span className="ml-2 text-lg font-black text-gray-700">{value}<span className="text-sm font-medium text-gray-400">/5</span></span>
      </div>
    </div>
  );
};

const StarDisplay = ({ value, max = 5 }: { value: number; max?: number }) => (
  <div className="flex gap-0.5">
    {Array.from({ length: max }).map((_, i) => (
      i < value
        ? <TbStarFilled key={i} size={12} className="text-[#3FB6B2]" />
        : <TbStar key={i} size={12} className="text-gray-200" />
    ))}
  </div>
);

export default JobSummaryDetailPage;
