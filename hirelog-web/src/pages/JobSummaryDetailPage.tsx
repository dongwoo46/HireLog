import { useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { TbChevronLeft, TbDeviceFloppy, TbReload } from 'react-icons/tb';
import { toast } from 'react-toastify';
import { jdSummaryService } from '../services/jdSummaryService';
import { useAuthStore } from '../store/authStore';
import {
  HIRING_STAGE_LABELS,
  HIRING_STAGE_RESULT_LABELS,
  type HiringStage,
  type HiringStageResult,
  type HiringStageView,
  type JobSummaryDetailView,
  type ReviewWriteReq,
} from '../types/jobSummary';

const tabs = ['detail', 'review', 'prep'] as const;
type TabType = (typeof tabs)[number];

const stageOrder: HiringStage[] = [
  'DOCUMENT',
  'CODING_TEST',
  'ASSIGNMENT',
  'INTERVIEW_1',
  'INTERVIEW_2',
  'INTERVIEW_3',
  'FINAL_INTERVIEW',
  'COFFEE_CHAT',
];

const reviewDefaultForm: ReviewWriteReq = {
  hiringStage: 'DOCUMENT',
  anonymous: true,
  difficultyRating: 5,
  satisfactionRating: 5,
  experienceComment: '',
  interviewTip: '',
};

const JobSummaryDetailPage = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { isAuthenticated, user } = useAuthStore();

  const [jd, setJd] = useState<JobSummaryDetailView | null>(null);
  const [loading, setLoading] = useState(true);
  const [loadFailed, setLoadFailed] = useState(false);
  const [activeTab, setActiveTab] = useState<TabType>('detail');

  const [reviewPage, setReviewPage] = useState<any>(null);
  const [reviewLoading, setReviewLoading] = useState(false);
  const [reviewSubmitting, setReviewSubmitting] = useState(false);
  const [showReviewComposer, setShowReviewComposer] = useState(false);
  const [alreadyReviewed, setAlreadyReviewed] = useState(false);
  const [reviewForm, setReviewForm] = useState<ReviewWriteReq>(reviewDefaultForm);

  const [stages, setStages] = useState<Record<HiringStage, HiringStageView | undefined>>({} as any);
  const [activeStage, setActiveStage] = useState<HiringStage>('DOCUMENT');
  const [note, setNote] = useState('');
  const [stageResult, setStageResult] = useState<HiringStageResult | null>(null);
  const [prepLoading, setPrepLoading] = useState(false);
  const [savingPrep, setSavingPrep] = useState(false);

  const [coverQuestion, setCoverQuestion] = useState('자기소개서 메모');
  const [coverContent, setCoverContent] = useState('');
  const [savingCover, setSavingCover] = useState(false);

  useEffect(() => {
    if (!id) return;
    setLoading(true);
    setLoadFailed(false);
    jdSummaryService
      .getDetail(Number(id))
      .then(setJd)
      .catch(() => {
        setLoadFailed(true);
        toast.error('상세 정보를 불러오지 못했습니다.');
      })
      .finally(() => setLoading(false));
  }, [id]);

  const loadReviews = async () => {
    if (!jd) return;
    setReviewLoading(true);
    try {
      const data = await jdSummaryService.getReviews(jd.id);
      setReviewPage(data);

      if (user?.id) {
        const hasOwnVisibleReview = (data?.items || []).some((item: any) => item.memberId === user.id);
        if (hasOwnVisibleReview) {
          setAlreadyReviewed(true);
          setShowReviewComposer(false);
        }
      }
    } catch {
      toast.error('리뷰를 불러오지 못했습니다.');
    } finally {
      setReviewLoading(false);
    }
  };

  useEffect(() => {
    if (!jd || activeTab !== 'review') return;
    loadReviews();
  }, [jd, activeTab]);

  const loadPreparationData = async () => {
    if (!jd) return;
    setPrepLoading(true);
    try {
      const [stageItems, coverLetters] = await Promise.all([
        jdSummaryService.getStages(jd.id),
        jdSummaryService.getCoverLetters(jd.id),
      ]);

      const nextStages = stageItems.reduce((acc, item) => {
        acc[item.stage] = item;
        return acc;
      }, {} as Record<HiringStage, HiringStageView | undefined>);

      setStages(nextStages);
      setNote(nextStages[activeStage]?.note || '');
      setStageResult(nextStages[activeStage]?.result ?? null);

      if (coverLetters.length > 0) {
        const first = [...coverLetters].sort((a, b) => a.sortOrder - b.sortOrder)[0];
        setCoverQuestion(first.question);
        setCoverContent(first.content);
      }
    } catch {
      toast.info('아직 저장된 준비 기록이 없습니다.');
    } finally {
      setPrepLoading(false);
    }
  };

  useEffect(() => {
    if (!jd || !isAuthenticated || activeTab !== 'prep') return;
    loadPreparationData();
  }, [jd, isAuthenticated, activeTab]);

  useEffect(() => {
    setNote(stages[activeStage]?.note || '');
    setStageResult(stages[activeStage]?.result ?? null);
  }, [activeStage, stages]);

  const savePreparation = async () => {
    if (!jd) return;
    if (!note.trim()) {
      toast.warn('내용을 입력해 주세요.');
      return;
    }

    setSavingPrep(true);
    try {
      await jdSummaryService.saveStageNote(jd.id, activeStage, note.trim(), stageResult);
      toast.success(`${HIRING_STAGE_LABELS[activeStage]} 준비 기록을 저장했습니다.`);
      await loadPreparationData();
    } catch {
      toast.error('준비 기록 저장에 실패했습니다.');
    } finally {
      setSavingPrep(false);
    }
  };

  const saveCoverLetter = async () => {
    if (!jd) return;
    if (!coverQuestion.trim() || !coverContent.trim()) {
      toast.warn('제목과 내용을 모두 입력해 주세요.');
      return;
    }

    setSavingCover(true);
    try {
      await jdSummaryService.saveCoverLetter(jd.id, {
        question: coverQuestion.trim(),
        content: coverContent.trim(),
        sortOrder: 1,
      });
      toast.success('자소서 메모를 저장했습니다.');
    } catch {
      toast.error('자소서 메모 저장에 실패했습니다.');
    } finally {
      setSavingCover(false);
    }
  };

  const handleReviewSubmit = async () => {
    if (!jd) return;
    if (!reviewForm.experienceComment.trim()) {
      toast.warn('리뷰 내용을 입력해 주세요.');
      return;
    }

    setReviewSubmitting(true);
    try {
      await jdSummaryService.addReview(jd.id, {
        ...reviewForm,
        experienceComment: reviewForm.experienceComment.trim(),
        interviewTip: reviewForm.interviewTip?.trim() || undefined,
      });
      toast.success('리뷰가 등록되었습니다.');
      setAlreadyReviewed(true);
      setShowReviewComposer(false);
      setReviewForm(reviewDefaultForm);
      await loadReviews();
    } catch (error: any) {
      const message = error?.response?.data?.message || error?.message || '';
      if (typeof message === 'string' && message.includes('이미')) {
        setAlreadyReviewed(true);
        setShowReviewComposer(false);
        toast.info('이미 이 공고에 리뷰를 작성하셨습니다.');
      } else {
        toast.error('리뷰 등록에 실패했습니다.');
      }
    } finally {
      setReviewSubmitting(false);
    }
  };

  const prepSummary = useMemo(() => {
    const document = stages.DOCUMENT?.note;
    const coding = stages.CODING_TEST?.note;
    return {
      document: document && document.length > 0,
      coding: coding && coding.length > 0,
    };
  }, [stages]);

  if (loading) return <div className="min-h-screen bg-[#F8F9FA] pt-24" />;

  if (!jd || loadFailed) {
    return (
      <div className="min-h-screen bg-[#F8F9FA] px-6 pt-24">
        <div className="mx-auto max-w-3xl rounded-2xl border border-gray-200 bg-white p-8 text-center text-sm text-gray-600">
          공고 상세를 불러올 수 없습니다.
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-[#F8F9FA] pb-20 pt-24">
      <div className="mx-auto max-w-5xl px-6">
        <button onClick={() => navigate(-1)} className="mb-6 flex items-center gap-2 text-gray-500 hover:text-gray-700">
          <TbChevronLeft /> 목록으로
        </button>

        <div className="mb-10 rounded-3xl bg-gradient-to-r from-[#3FB6B2] to-[#6EC8A7] p-8 text-white shadow-lg">
          <h1 className="mb-1 text-3xl font-black">{jd.brandName}</h1>
          <h2 className="text-lg font-semibold">{jd.brandPositionName}</h2>
        </div>

        <div className="mb-8 flex gap-6 border-b text-sm font-bold">
          <TabButton label="상세 정보" active={activeTab === 'detail'} onClick={() => setActiveTab('detail')} />
          <TabButton label="리뷰" active={activeTab === 'review'} onClick={() => setActiveTab('review')} />
          <TabButton label="준비 기록" active={activeTab === 'prep'} onClick={() => setActiveTab('prep')} disabled={!isAuthenticated} />
        </div>

        {!isAuthenticated && (
          <div className="mb-6 rounded-xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-800">
            비로그인 상태에서는 JD/리뷰 조회만 가능합니다. 리뷰 작성과 준비기록 저장은 로그인 후 이용해 주세요.
          </div>
        )}

        {activeTab === 'detail' && <DetailSection jd={jd} />}

        {activeTab === 'review' && (
          <ReviewSection
            reviewPage={reviewPage}
            loading={reviewLoading}
            form={reviewForm}
            submitting={reviewSubmitting}
            onChange={setReviewForm}
            onSubmit={handleReviewSubmit}
            showComposer={showReviewComposer}
            setShowComposer={setShowReviewComposer}
            alreadyReviewed={alreadyReviewed}
            isAuthenticated={isAuthenticated}
          />
        )}

        {activeTab === 'prep' && isAuthenticated && (
          <div className="space-y-6">
            <div className="grid gap-4 rounded-2xl border border-[#3FB6B2]/20 bg-[#3FB6B2]/5 p-4 md:grid-cols-2">
              <StatusBadge label="서류 전형" done={!!prepSummary.document} />
              <StatusBadge label="코딩 테스트" done={!!prepSummary.coding} />
            </div>

            <div className="grid gap-6 lg:grid-cols-[220px_1fr]">
              <div className="rounded-2xl border bg-white p-4">
                <p className="mb-3 text-xs font-semibold uppercase tracking-wider text-gray-400">단계 선택</p>
                <div className="space-y-2">
                  {stageOrder.map((stage) => {
                    const r = stages[stage]?.result;
                    return (
                      <button
                        key={stage}
                        onClick={() => setActiveStage(stage)}
                        className={`flex w-full items-center justify-between rounded-xl px-3 py-2 text-left text-sm font-semibold transition ${
                          activeStage === stage
                            ? 'bg-[#3FB6B2] text-white'
                            : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                        }`}
                      >
                        <span>{HIRING_STAGE_LABELS[stage]}</span>
                        {r && <span className="rounded bg-white/80 px-1.5 py-0.5 text-[10px] text-gray-600">{HIRING_STAGE_RESULT_LABELS[r]}</span>}
                      </button>
                    );
                  })}
                </div>
              </div>

              <div className="space-y-4 rounded-2xl border bg-white p-5">
                <div className="flex items-center justify-between">
                  <h3 className="text-base font-bold text-gray-900">{HIRING_STAGE_LABELS[activeStage]} 준비 메모</h3>
                  <button
                    onClick={loadPreparationData}
                    className="flex items-center gap-1 rounded-lg border border-gray-200 px-3 py-1.5 text-xs font-semibold text-gray-600 hover:bg-gray-50"
                  >
                    <TbReload size={14} /> 동기화
                  </button>
                </div>

                {prepLoading ? (
                  <div className="py-10 text-center text-sm text-gray-400">불러오는 중...</div>
                ) : (
                  <>
                    <div className="flex items-center gap-2">
                      <span className="text-xs font-semibold text-gray-500">결과</span>
                      {(['PASSED', 'FAILED', 'PENDING'] as HiringStageResult[]).map((r) => (
                        <button
                          key={r}
                          type="button"
                          onClick={() => setStageResult(stageResult === r ? null : r)}
                          className={`rounded-lg px-3 py-1 text-xs font-bold transition ${
                            stageResult === r ? 'bg-[#3FB6B2] text-white' : 'bg-gray-100 text-gray-500 hover:bg-gray-200'
                          }`}
                        >
                          {HIRING_STAGE_RESULT_LABELS[r]}
                        </button>
                      ))}
                    </div>
                    <textarea
                      className="min-h-[220px] w-full rounded-xl border border-gray-200 p-4 text-sm"
                      value={note}
                      onChange={(e) => setNote(e.target.value)}
                      placeholder="이 단계에서 받은 질문, 부족했던 점, 다음 액션을 기록해 주세요."
                    />
                    <button
                      onClick={savePreparation}
                      disabled={savingPrep}
                      className="flex items-center gap-2 rounded-xl bg-[#3FB6B2] px-5 py-2.5 text-sm font-semibold text-white disabled:opacity-60"
                    >
                      <TbDeviceFloppy size={16} />
                      {savingPrep ? '저장 중...' : '단계 메모 저장'}
                    </button>
                  </>
                )}
              </div>
            </div>

            <div className="space-y-3 rounded-2xl border bg-white p-5">
              <h3 className="text-base font-bold text-gray-900">자소서 메모</h3>
              <input
                value={coverQuestion}
                onChange={(e) => setCoverQuestion(e.target.value)}
                className="w-full rounded-xl border border-gray-200 px-4 py-2 text-sm"
                placeholder="메모 제목"
              />
              <textarea
                value={coverContent}
                onChange={(e) => setCoverContent(e.target.value)}
                className="min-h-[180px] w-full rounded-xl border border-gray-200 p-4 text-sm"
                placeholder="자소서 핵심 메시지, 사례, 숫자 근거 등을 정리해 주세요."
              />
              <button
                onClick={saveCoverLetter}
                disabled={savingCover}
                className="rounded-xl border border-[#3FB6B2] px-5 py-2.5 text-sm font-semibold text-[#3FB6B2] disabled:opacity-60"
              >
                {savingCover ? '저장 중...' : '자소서 메모 저장'}
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

const DetailSection = ({ jd }: { jd: JobSummaryDetailView }) => (
  <div className="space-y-5">
    <Block title="요약" content={jd.summaryText} />
    <Block title="주요 업무" content={jd.responsibilities} />
    <Block title="자격 요건" content={jd.requiredQualifications} />
    <Block title="우대 사항" content={jd.preferredQualifications} />
    <Block title="준비 포인트" content={jd.preparationFocus} />
    <Block title="증명 포인트" content={jd.proofPointsAndMetrics} />
    <Block title="면접 질문" content={jd.questionsToAsk} />
  </div>
);

const Block = ({ title, content }: { title: string; content?: string | null }) =>
  content ? (
    <div className="rounded-2xl border bg-white p-6">
      <div className="mb-2 font-bold text-[#3FB6B2]">{title}</div>
      <div className="whitespace-pre-line text-sm text-gray-700">{content}</div>
    </div>
  ) : null;

const ReviewSection = ({
  reviewPage,
  loading,
  form,
  submitting,
  onChange,
  onSubmit,
  showComposer,
  setShowComposer,
  alreadyReviewed,
  isAuthenticated,
}: {
  reviewPage: any;
  loading: boolean;
  form: ReviewWriteReq;
  submitting: boolean;
  onChange: (value: ReviewWriteReq) => void;
  onSubmit: () => void;
  showComposer: boolean;
  setShowComposer: (value: boolean) => void;
  alreadyReviewed: boolean;
  isAuthenticated: boolean;
}) => (
  <div className="space-y-5">
    {!isAuthenticated && (
      <div className="rounded-2xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-800">
        비로그인 상태에서는 리뷰를 조회만 할 수 있습니다.
      </div>
    )}

    {isAuthenticated && !alreadyReviewed && !showComposer && (
      <div className="rounded-2xl border bg-white p-6">
        <div className="flex items-center justify-between">
          <div>
            <h3 className="text-base font-bold text-gray-900">리뷰 작성</h3>
            <p className="mt-1 text-xs text-gray-500">이 공고에 대한 실제 경험을 남겨주세요.</p>
          </div>
          <button
            type="button"
            onClick={() => setShowComposer(true)}
            className="rounded-xl bg-[#3FB6B2] px-4 py-2 text-sm font-semibold text-white"
          >
            리뷰 작성
          </button>
        </div>
      </div>
    )}

    {isAuthenticated && alreadyReviewed && (
      <div className="rounded-2xl border border-[#3FB6B2]/30 bg-[#3FB6B2]/5 px-4 py-3 text-sm text-[#2f8e8a]">
        이 공고에는 이미 리뷰를 작성하셨습니다. 리뷰 수정/삭제는 관리자 권한에서만 가능합니다.
      </div>
    )}

    {isAuthenticated && showComposer && !alreadyReviewed && (
      <div className="rounded-2xl border bg-white p-6">
        <div className="mb-4 flex items-center justify-between">
          <h3 className="text-base font-bold text-gray-900">리뷰 작성</h3>
          <button
            type="button"
            onClick={() => setShowComposer(false)}
            className="rounded-lg border border-gray-200 px-3 py-1.5 text-xs font-semibold text-gray-600"
          >
            닫기
          </button>
        </div>

        <div className="grid gap-4 md:grid-cols-2">
          <label className="space-y-2">
            <span className="text-xs font-semibold text-gray-500">채용 단계</span>
            <select
              value={form.hiringStage}
              onChange={(e) => onChange({ ...form, hiringStage: e.target.value as HiringStage })}
              className="w-full rounded-xl border border-gray-200 px-3 py-2 text-sm"
            >
              {stageOrder.map((stage) => (
                <option key={stage} value={stage}>
                  {HIRING_STAGE_LABELS[stage]}
                </option>
              ))}
            </select>
          </label>

          <button
            type="button"
            onClick={() => onChange({ ...form, anonymous: !form.anonymous })}
            className={`flex h-[74px] items-center justify-between rounded-xl border px-4 transition ${
              form.anonymous ? 'border-[#3FB6B2] bg-[#3FB6B2]/5' : 'border-gray-200 bg-white'
            }`}
          >
            <div className="text-left">
              <p className="text-xs font-semibold text-gray-500">작성 방식</p>
              <p className="text-sm font-semibold text-gray-800">익명으로 작성</p>
            </div>
            <span
              className={`inline-flex h-6 w-11 items-center rounded-full p-1 transition ${
                form.anonymous ? 'bg-[#3FB6B2]' : 'bg-gray-300'
              }`}
            >
              <span className={`h-4 w-4 rounded-full bg-white transition ${form.anonymous ? 'translate-x-5' : 'translate-x-0'}`} />
            </span>
          </button>
        </div>

        <div className="mt-4 grid gap-4 md:grid-cols-2">
          <ScoreSelector
            label="난이도"
            value={form.difficultyRating}
            onChange={(value) => onChange({ ...form, difficultyRating: value })}
          />
          <ScoreSelector
            label="만족도"
            value={form.satisfactionRating}
            onChange={(value) => onChange({ ...form, satisfactionRating: value })}
          />
        </div>

        <div className="mt-4 space-y-3">
          <textarea
            value={form.experienceComment}
            onChange={(e) => onChange({ ...form, experienceComment: e.target.value })}
            className="min-h-[130px] w-full rounded-xl border border-gray-200 p-4 text-sm"
            placeholder="면접/전형 경험을 공유해 주세요. (최소 10자)"
          />
          <textarea
            value={form.interviewTip || ''}
            onChange={(e) => onChange({ ...form, interviewTip: e.target.value })}
            className="min-h-[90px] w-full rounded-xl border border-gray-200 p-4 text-sm"
            placeholder="도움이 된 팁이 있다면 작성해 주세요. (선택)"
          />
        </div>

        <div className="mt-4 flex justify-end">
          <button
            onClick={onSubmit}
            disabled={submitting}
            className="rounded-xl bg-[#3FB6B2] px-5 py-2.5 text-sm font-semibold text-white disabled:opacity-60"
          >
            {submitting ? '등록 중...' : '리뷰 등록'}
          </button>
        </div>
      </div>
    )}

    {loading ? (
      <div className="py-10 text-center text-sm text-gray-400">리뷰를 불러오는 중...</div>
    ) : !reviewPage?.items?.length ? (
      <div className="rounded-2xl border bg-white p-8 text-center text-sm text-gray-400">등록된 리뷰가 없습니다.</div>
    ) : (
      <div className="space-y-4">
        {reviewPage.items.map((r: any) => (
          <div key={r.id} className="rounded-2xl border bg-white p-6">
            <div className="mb-1 text-xs text-gray-400">{HIRING_STAGE_LABELS[r.hiringStage as HiringStage] || r.hiringStage}</div>
            <div className="mb-3 text-xs text-gray-500">난이도 {r.difficultyRating} / 만족도 {r.satisfactionRating}</div>
            <div className="whitespace-pre-line text-sm text-gray-700">{r.experienceComment}</div>
            {r.interviewTip && <div className="mt-3 text-xs text-[#3FB6B2]">TIP: {r.interviewTip}</div>}
          </div>
        ))}
      </div>
    )}
  </div>
);

const ScoreSelector = ({
  label,
  value,
  onChange,
}: {
  label: string;
  value: number;
  onChange: (value: number) => void;
}) => (
  <div className="space-y-2">
    <div className="text-xs font-semibold text-gray-500">
      {label} <span className="text-[#3FB6B2]">{value}</span>
    </div>
    <div>
      <div className="mb-2 flex items-center gap-1">
        {[0, 1, 2, 3, 4].map((idx) => {
          const starValue = value / 2;
          const fill = Math.max(0, Math.min(1, starValue - idx));
          return (
            <div key={idx} className="relative h-8 w-8">
              <StarIcon className="text-gray-300" />
              {fill > 0 && (
                <div className="absolute inset-0 overflow-hidden" style={{ width: `${fill * 100}%` }}>
                  <StarIcon className="text-[#F59E0B]" />
                </div>
              )}
              <button
                type="button"
                aria-label={`${label} ${idx * 2 + 1}점`}
                onClick={() => onChange(idx * 2 + 1)}
                className="absolute left-0 top-0 h-full w-1/2"
              />
              <button
                type="button"
                aria-label={`${label} ${idx * 2 + 2}점`}
                onClick={() => onChange(idx * 2 + 2)}
                className="absolute right-0 top-0 h-full w-1/2"
              />
            </div>
          );
        })}
      </div>
      <p className="text-[11px] text-gray-400">반별(0.5별) 단위로 선택됩니다. 0.5별 = 1점</p>
    </div>
  </div>
);

const StarIcon = ({ className }: { className?: string }) => (
  <svg viewBox="0 0 24 24" className={`h-8 w-8 ${className || ''}`} fill="currentColor" aria-hidden="true">
    <path d="M12 2.6l2.9 5.88 6.49.94-4.7 4.58 1.11 6.46L12 17.43l-5.8 3.05 1.11-6.46-4.7-4.58 6.49-.94L12 2.6z" />
  </svg>
);

const TabButton = ({
  label,
  active,
  onClick,
  disabled,
}: {
  label: string;
  active: boolean;
  onClick: () => void;
  disabled?: boolean;
}) => (
  <button
    onClick={onClick}
    disabled={disabled}
    className={`pb-2 ${active ? 'border-b-2 border-[#3FB6B2] text-[#3FB6B2]' : 'text-gray-400'} ${disabled ? 'cursor-not-allowed opacity-50' : ''}`}
  >
    {label}
  </button>
);

const StatusBadge = ({ label, done }: { label: string; done: boolean }) => (
  <div className="flex items-center justify-between rounded-xl bg-white px-4 py-3">
    <span className="text-sm font-semibold text-gray-700">{label}</span>
    <span className={`rounded-full px-2.5 py-1 text-xs font-semibold ${done ? 'bg-[#3FB6B2]/10 text-[#3FB6B2]' : 'bg-gray-100 text-gray-500'}`}>
      {done ? '저장됨' : '미저장'}
    </span>
  </div>
);

export default JobSummaryDetailPage;
