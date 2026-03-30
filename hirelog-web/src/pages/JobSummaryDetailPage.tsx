import { useEffect, useState } from 'react';
import type { ElementType } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import {
  TbChevronLeft,
  TbChevronDown,
  TbChevronUp,
  TbDeviceFloppy,
  TbReload,
  TbNotes,
  TbBriefcase,
  TbUserCheck,
  TbStar,
  TbBulb,
  TbDiscount,
  TbMessages,
  TbCode,
  TbRoute,
  TbEye,
  TbExternalLink,
  TbTarget,
  TbShieldCheck,
  TbScale,
  TbStars,
  TbMountain,
  TbCpu,
  TbAlertCircle,
} from 'react-icons/tb';
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
  const isAdmin = user?.role === 'ADMIN';

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
  const [editingReviewId, setEditingReviewId] = useState<number | null>(null);
  const [adminReviewForm, setAdminReviewForm] = useState<ReviewWriteReq>(reviewDefaultForm);
  const [adminReviewSubmitting, setAdminReviewSubmitting] = useState(false);
  const [adminReviewDeletingId, setAdminReviewDeletingId] = useState<number | null>(null);

  const [stages, setStages] = useState<Record<HiringStage, HiringStageView | undefined>>({} as any);
  const [activeStage, setActiveStage] = useState<HiringStage>('DOCUMENT');
  const [note, setNote] = useState('');
  const [stageResult, setStageResult] = useState<HiringStageResult | null>(null);
  const [prepLoading, setPrepLoading] = useState(false);
  const [savingPrep, setSavingPrep] = useState(false);
  const [isEditingPrep, setIsEditingPrep] = useState(false);
  const [isStageSelectorCollapsed, setIsStageSelectorCollapsed] = useState(false);
  const [isPrepMemoCollapsed, setIsPrepMemoCollapsed] = useState(false);

  const [coverQuestion, setCoverQuestion] = useState('자기소개서 메모');
  const [coverContent, setCoverContent] = useState('');
  const [savingCover, setSavingCover] = useState(false);
  const [isEditingCover, setIsEditingCover] = useState(true);

  const latestRecordedStage = stageOrder
    .map((stage) => stages[stage])
    .filter((stage): stage is HiringStageView => Boolean(stage))
    .sort((a, b) => new Date(b.recordedAt).getTime() - new Date(a.recordedAt).getTime())[0];
  const latestStageLabel = latestRecordedStage ? HIRING_STAGE_LABELS[latestRecordedStage.stage] : null;
  const latestStageResultLabel = latestRecordedStage?.result ? HIRING_STAGE_RESULT_LABELS[latestRecordedStage.result] : null;
  const latestStageNoteSummary = latestRecordedStage?.note?.trim()
    ? `${latestRecordedStage.note.trim().slice(0, 100)}${latestRecordedStage.note.trim().length > 100 ? '...' : ''}`
    : '메모가 아직 없습니다.';

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
    if (!jd || !isAuthenticated) return;
    setReviewLoading(true);
    try {
      const data = await jdSummaryService.getReviews(jd.id, 0, 20, { includeDeleted: isAdmin });
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
    if (!jd || activeTab !== 'review' || !isAuthenticated) return;
    loadReviews();
  }, [jd, activeTab, isAuthenticated]);

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
        setIsEditingCover(false);
      } else {
        setIsEditingCover(true);
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
    setIsEditingPrep(!stages[activeStage]);
  }, [activeStage, stages]);

  const savePreparation = async () => {
    if (!jd) return;
    if (!note.trim()) {
      toast.warn('내용을 입력해 주세요.');
      return;
    }

    setSavingPrep(true);
    try {
      await jdSummaryService.saveStageNote(
        jd.id,
        activeStage,
        note.trim(),
        stageResult,
        jd.memberSaveType,
      );
      setJd((prev) => (prev ? { ...prev, memberSaveType: 'APPLY' } : prev));
      toast.success(`${HIRING_STAGE_LABELS[activeStage]} 준비 기록을 저장했습니다.`);
      await loadPreparationData();
      setIsEditingPrep(false);
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
      setIsEditingCover(false);
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

  const startAdminEditReview = (review: any) => {
    setEditingReviewId(review.id);
    setAdminReviewForm({
      hiringStage: review.hiringStage as HiringStage,
      anonymous: !!review.anonymous,
      difficultyRating: review.difficultyRating,
      satisfactionRating: review.satisfactionRating,
      experienceComment: review.experienceComment || '',
      interviewTip: review.interviewTip || '',
    });
  };

  const cancelAdminEditReview = () => {
    setEditingReviewId(null);
    setAdminReviewForm(reviewDefaultForm);
  };

  const submitAdminEditReview = async () => {
    if (!editingReviewId) return;
    if (!adminReviewForm.experienceComment.trim()) {
      toast.warn('리뷰 내용을 입력해 주세요.');
      return;
    }

    setAdminReviewSubmitting(true);
    try {
      await jdSummaryService.updateReview(editingReviewId, {
        ...adminReviewForm,
        experienceComment: adminReviewForm.experienceComment.trim(),
        interviewTip: adminReviewForm.interviewTip?.trim() || undefined,
      });
      toast.success('리뷰를 수정했습니다.');
      await loadReviews();
      cancelAdminEditReview();
    } catch {
      toast.error('리뷰 수정에 실패했습니다.');
    } finally {
      setAdminReviewSubmitting(false);
    }
  };

  const deleteReviewAsAdmin = async (reviewId: number) => {
    const ok = window.confirm('리뷰를 삭제할까요?');
    if (!ok) return;

    setAdminReviewDeletingId(reviewId);
    try {
      await jdSummaryService.deleteReview(reviewId);
      toast.success('리뷰를 삭제했습니다.');
      await loadReviews();
      if (editingReviewId === reviewId) {
        cancelAdminEditReview();
      }
    } catch {
      toast.error('리뷰 삭제에 실패했습니다.');
    } finally {
      setAdminReviewDeletingId(null);
    }
  };

  const restoreReviewAsAdmin = async (reviewId: number) => {
    setAdminReviewDeletingId(reviewId);
    try {
      await jdSummaryService.restoreReview(reviewId);
      toast.success('리뷰를 복구했습니다.');
      await loadReviews();
    } catch {
      toast.error('리뷰 복구에 실패했습니다.');
    } finally {
      setAdminReviewDeletingId(null);
    }
  };

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
            isAdmin={isAdmin}
            editingReviewId={editingReviewId}
            adminReviewForm={adminReviewForm}
            adminReviewSubmitting={adminReviewSubmitting}
            adminReviewDeletingId={adminReviewDeletingId}
            onStartAdminEdit={startAdminEditReview}
            onCancelAdminEdit={cancelAdminEditReview}
            onAdminFormChange={setAdminReviewForm}
            onSubmitAdminEdit={submitAdminEditReview}
            onDeleteAsAdmin={deleteReviewAsAdmin}
            onRestoreAsAdmin={restoreReviewAsAdmin}
          />
        )}

        {activeTab === 'prep' && isAuthenticated && (
          <div className="space-y-6">
            <div className="grid gap-6 lg:grid-cols-[220px_1fr]">
              <div className="rounded-2xl border bg-white p-4">
                <button
                  type="button"
                  onClick={() => setIsStageSelectorCollapsed((prev) => !prev)}
                  className="mb-3 flex w-full items-center justify-between rounded-xl px-1 py-1 text-left"
                >
                  <p className="text-xs font-semibold uppercase tracking-wider text-gray-400">단계 선택</p>
                  {isStageSelectorCollapsed ? (
                    <TbChevronDown className="text-gray-500" size={16} />
                  ) : (
                    <TbChevronUp className="text-gray-500" size={16} />
                  )}
                </button>

                {isStageSelectorCollapsed ? (
                  <div className="rounded-xl border border-gray-200 bg-gray-50/70 p-3">
                    <p className="text-xs font-semibold text-gray-500">최신 전형 상태</p>
                    {latestRecordedStage ? (
                      <div className="mt-2 flex items-center justify-between gap-2">
                        <span className="text-sm font-semibold text-gray-700">{latestStageLabel}</span>
                        {latestStageResultLabel ? (
                          <span className="rounded-full bg-[#3FB6B2]/15 px-2.5 py-1 text-xs font-bold text-[#237b78]">
                            {latestStageResultLabel}
                          </span>
                        ) : (
                          <span className="rounded-full bg-gray-100 px-2.5 py-1 text-xs text-gray-500">미설정</span>
                        )}
                      </div>
                    ) : (
                      <p className="mt-2 text-sm text-gray-500">아직 기록된 전형이 없습니다.</p>
                    )}
                  </div>
                ) : (
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
                          {r && (
                            <span
                              className={`rounded-full px-2 py-0.5 text-[10px] font-bold ${
                                r === 'PENDING'
                                  ? 'bg-gray-200 text-gray-600'
                                  : 'bg-[#3FB6B2]/20 text-[#237b78]'
                              }`}
                            >
                              {HIRING_STAGE_RESULT_LABELS[r]}
                            </span>
                          )}
                        </button>
                      );
                    })}
                  </div>
                )}
              </div>

              <div className="space-y-4 rounded-2xl border bg-white p-5">
                <div className="flex items-center justify-between">
                  <h3 className="text-base font-bold text-gray-900">{HIRING_STAGE_LABELS[activeStage]} 준비 메모</h3>
                  <div className="flex items-center gap-2">
                    <button
                      onClick={loadPreparationData}
                      className="flex items-center gap-1 rounded-lg border border-gray-200 px-3 py-1.5 text-xs font-semibold text-gray-600 hover:bg-gray-50"
                    >
                      <TbReload size={14} /> 새로고침
                    </button>
                    <button
                      type="button"
                      onClick={() => setIsPrepMemoCollapsed((prev) => !prev)}
                      className="flex items-center gap-1 rounded-lg border border-gray-200 px-3 py-1.5 text-xs font-semibold text-gray-600 hover:bg-gray-50"
                    >
                      {isPrepMemoCollapsed ? (
                        <>
                          펼치기 <TbChevronDown size={14} />
                        </>
                      ) : (
                        <>
                          접기 <TbChevronUp size={14} />
                        </>
                      )}
                    </button>
                  </div>
                </div>

                {isPrepMemoCollapsed ? (
                  <div className="space-y-3 rounded-xl border border-gray-200 bg-gray-50/70 p-4">
                    <div className="flex items-center justify-between gap-2">
                      <p className="text-sm font-semibold text-gray-700">최신 전형 상태</p>
                      {latestRecordedStage ? (
                        <div className="flex items-center gap-2">
                          <span className="text-sm font-semibold text-gray-700">{latestStageLabel}</span>
                          {latestStageResultLabel ? (
                            <span className="rounded-full bg-[#3FB6B2]/15 px-2.5 py-1 text-xs font-bold text-[#237b78]">
                              {latestStageResultLabel}
                            </span>
                          ) : (
                            <span className="rounded-full bg-gray-100 px-2.5 py-1 text-xs text-gray-500">미설정</span>
                          )}
                        </div>
                      ) : (
                        <span className="text-xs text-gray-500">기록 없음</span>
                      )}
                    </div>
                    <p className="text-sm leading-6 text-gray-600">{latestStageNoteSummary}</p>
                  </div>
                ) : prepLoading ? (
                  <div className="py-10 text-center text-sm text-gray-400">불러오는 중...</div>
                ) : isEditingPrep ? (
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
                    {stages[activeStage] && (
                      <button
                        type="button"
                        onClick={() => setIsEditingPrep(false)}
                        className="rounded-xl border border-gray-200 px-5 py-2.5 text-sm font-semibold text-gray-600 hover:bg-gray-50"
                      >
                        취소
                      </button>
                    )}
                  </>
                ) : (
                  <div className="space-y-4">
                    <div className="flex items-center gap-2">
                      <span className="text-xs font-semibold text-gray-500">결과</span>
                      {stageResult ? (
                        <span
                          className={`rounded-full px-2.5 py-1 text-xs font-bold ${
                            stageResult === 'PENDING'
                              ? 'bg-gray-100 text-gray-600'
                              : 'bg-[#3FB6B2]/15 text-[#237b78]'
                          }`}
                        >
                          {HIRING_STAGE_RESULT_LABELS[stageResult]}
                        </span>
                      ) : (
                        <span className="rounded-full bg-gray-100 px-2.5 py-1 text-xs text-gray-500">미설정</span>
                      )}
                    </div>
                    <div className="rounded-xl border border-gray-200 bg-gray-50/60 p-4">
                      <p className="whitespace-pre-wrap text-sm leading-6 text-gray-700">
                        {stages[activeStage]?.note}
                      </p>
                    </div>
                    <button
                      type="button"
                      onClick={() => setIsEditingPrep(true)}
                      className="rounded-xl border border-[#3FB6B2] px-5 py-2.5 text-sm font-semibold text-[#2b8f8c] hover:bg-[#3FB6B2]/5"
                    >
                      수정하기
                    </button>
                  </div>
                )}
              </div>
            </div>

            <div className="space-y-3 rounded-2xl border bg-white p-5">
              <div className="flex items-center justify-between gap-2">
                <h3 className="text-base font-bold text-gray-900">자소서 메모</h3>
                {!isEditingCover && (
                  <button
                    type="button"
                    onClick={() => setIsEditingCover(true)}
                    className="rounded-xl border border-[#3FB6B2] px-4 py-2 text-sm font-semibold text-[#2b8f8c] hover:bg-[#3FB6B2]/5"
                  >
                    수정하기
                  </button>
                )}
              </div>

              {isEditingCover ? (
                <>
                  <input
                    value={coverQuestion}
                    onChange={(e) => setCoverQuestion(e.target.value)}
                    className="w-full rounded-xl border border-gray-200 px-4 py-2 text-sm"
                    placeholder="메모 제목"
                  />
                  <textarea
                    value={coverContent}
                    onChange={(e) => setCoverContent(e.target.value)}
                    className="min-h-[260px] w-full resize-y rounded-xl border border-gray-200 p-4 text-sm"
                    placeholder="자소서 핵심 메시지, 사례, 숫자 근거 등을 정리해 주세요."
                  />
                  <div className="flex items-center gap-2">
                    <button
                      onClick={saveCoverLetter}
                      disabled={savingCover}
                      className="rounded-xl border border-[#3FB6B2] px-5 py-2.5 text-sm font-semibold text-[#3FB6B2] disabled:opacity-60"
                    >
                      {savingCover ? '저장 중...' : '자소서 메모 저장'}
                    </button>
                    {!!coverContent.trim() && (
                      <button
                        type="button"
                        onClick={() => {
                          setIsEditingCover(false);
                          void loadPreparationData();
                        }}
                        className="rounded-xl border border-gray-200 px-5 py-2.5 text-sm font-semibold text-gray-600 hover:bg-gray-50"
                      >
                        취소
                      </button>
                    )}
                  </div>
                </>
              ) : (
                <div className="space-y-3">
                  <div className="rounded-xl border border-gray-200 bg-gray-50/60 p-4">
                    <p className="text-xs font-semibold text-gray-500">메모 제목</p>
                    <p className="mt-1 text-sm font-semibold text-gray-800">{coverQuestion}</p>
                  </div>
                  <div className="rounded-xl border border-gray-200 bg-gray-50/60 p-4">
                    <p className="whitespace-pre-wrap text-sm leading-6 text-gray-700">{coverContent}</p>
                  </div>
                </div>
              )}
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

const TechStackBlock = ({ tags, delay = 0 }: { tags: string[]; delay?: number }) => (
  <div
    className="group flex flex-col rounded-3xl border border-gray-100 bg-white p-7 shadow-[0_4px_20px_-4px_rgba(0,0,0,0.05)] transition-all duration-300 hover:-translate-y-1 hover:border-[#3FB6B2]/40 hover:shadow-[0_8px_30px_-4px_rgba(63,182,178,0.15)] overflow-hidden relative"
    style={{ animationDelay: `${delay}ms`, animationFillMode: 'both', animationName: 'fadeUp', animationDuration: '0.6s' }}
  >
    <div className="absolute top-0 left-0 w-full h-1 bg-gradient-to-r from-[#3FB6B2] to-[#6EC8A7] opacity-0 group-hover:opacity-100 transition-opacity duration-300" />
    <div className="mb-5 flex items-center gap-3 text-xl font-bold text-gray-800">
      <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-2xl bg-gradient-to-br from-[#3FB6B2]/10 to-[#6EC8A7]/10 text-2xl text-[#3FB6B2] transition-all duration-300 group-hover:scale-110 group-hover:bg-[#3FB6B2] group-hover:text-white group-hover:shadow-md">
        <TbCode />
      </div>
      기술 스택
    </div>
    <div className="flex flex-wrap gap-2">
      {tags.map((tag, i) => (
        <span key={i} className="inline-flex items-center rounded-xl bg-[#3FB6B2]/10 px-3 py-1.5 text-sm font-bold text-[#3FB6B2] ring-1 ring-[#3FB6B2]/20">
          {tag}
        </span>
      ))}
    </div>
  </div>
);

const RecruitmentProcessBlock = ({ content, delay = 0 }: { content: string; delay?: number }) => {
  const steps = content
    .split(/\n|→|->/)
    .map((s) => s.replace(/^[-•*\d.]\s*/, '').trim())
    .filter(Boolean);

  return (
    <div
      className="group flex flex-col rounded-3xl border border-gray-100 bg-white p-7 shadow-[0_4px_20px_-4px_rgba(0,0,0,0.05)] transition-all duration-300 hover:-translate-y-1 hover:border-[#3FB6B2]/40 hover:shadow-[0_8px_30px_-4px_rgba(63,182,178,0.15)] overflow-hidden relative"
      style={{ animationDelay: `${delay}ms`, animationFillMode: 'both', animationName: 'fadeUp', animationDuration: '0.6s' }}
    >
      <div className="absolute top-0 left-0 w-full h-1 bg-gradient-to-r from-[#3FB6B2] to-[#6EC8A7] opacity-0 group-hover:opacity-100 transition-opacity duration-300" />
      <div className="mb-5 flex items-center gap-3 text-xl font-bold text-gray-800">
        <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-2xl bg-gradient-to-br from-[#3FB6B2]/10 to-[#6EC8A7]/10 text-2xl text-[#3FB6B2] transition-all duration-300 group-hover:scale-110 group-hover:bg-[#3FB6B2] group-hover:text-white group-hover:shadow-md">
          <TbRoute />
        </div>
        채용 프로세스
      </div>
      <div className="flex flex-wrap items-center gap-2">
        {steps.map((step, i) => (
          <div key={i} className="flex items-center gap-2">
            <div className="flex items-center gap-2 rounded-xl bg-gray-50 px-4 py-2">
              <span className="flex h-5 w-5 shrink-0 items-center justify-center rounded-full bg-[#3FB6B2] text-[11px] font-black text-white">{i + 1}</span>
              <span className="text-sm font-semibold text-gray-700">{step}</span>
            </div>
            {i < steps.length - 1 && <span className="text-sm font-bold text-[#3FB6B2]">→</span>}
          </div>
        ))}
      </div>
    </div>
  );
};

const SectionDivider = ({ label }: { label: string }) => (
  <div className="flex items-center gap-3 pt-2">
    <span className="text-xs font-black uppercase tracking-widest text-gray-400">{label}</span>
    <div className="flex-1 border-t border-gray-100" />
  </div>
);

const DetailSection = ({ jd }: { jd: JobSummaryDetailView }) => {
  const techTags = jd.techStackParsed?.length
    ? jd.techStackParsed
    : jd.techStack
      ? jd.techStack.split(/[,\n]/).map((s) => s.trim()).filter(Boolean)
      : [];

  return (
    <div className="space-y-6">
      {jd.sourceUrl && (
        <a
          href={jd.sourceUrl}
          target="_blank"
          rel="noopener noreferrer"
          className="flex items-center gap-2 w-fit rounded-2xl border border-[#3FB6B2]/30 bg-[#3FB6B2]/5 px-4 py-2.5 text-sm font-bold text-[#3FB6B2] transition-all hover:bg-[#3FB6B2]/10 hover:border-[#3FB6B2]/50"
        >
          <TbExternalLink size={16} />
          원본 공고 보기
        </a>
      )}

      {/* 공고 핵심 정보 */}
      <SectionDivider label="공고 핵심 정보" />
      <div className="grid gap-6 md:grid-cols-2">
        <div className="col-span-1 md:col-span-2">
          <Block title="요약" content={jd.summaryText} icon={TbNotes} delay={0} />
        </div>
        <div className="col-span-1 md:col-span-2">
          <Block title="주요 업무" content={jd.responsibilities} icon={TbBriefcase} delay={100} />
        </div>
        <div className="col-span-1">
          <Block title="자격 요건" content={jd.requiredQualifications} icon={TbUserCheck} delay={200} />
        </div>
        <div className="col-span-1">
          <Block title="우대 사항" content={jd.preferredQualifications} icon={TbStar} delay={300} />
        </div>
        {techTags.length > 0 && (
          <div className="col-span-1 md:col-span-2">
            <TechStackBlock tags={techTags} delay={350} />
          </div>
        )}
        {jd.recruitmentProcess && (
          <div className="col-span-1 md:col-span-2">
            <RecruitmentProcessBlock content={jd.recruitmentProcess} delay={380} />
          </div>
        )}
        <div className="col-span-1">
          <Block title="기술 맥락" content={jd.technicalContext} icon={TbCpu} delay={400} />
        </div>
        <div className="col-span-1">
          <Block title="핵심 도전과제" content={jd.keyChallenges} icon={TbMountain} delay={420} />
        </div>
      </div>

      {/* 지원 전략 */}
      <SectionDivider label="지원 전략" />
      <div className="grid gap-6 md:grid-cols-2">
        <div className="col-span-1 md:col-span-2">
          <Block title="이상적인 후보자" content={jd.idealCandidate} icon={TbTarget} delay={440} />
        </div>
        <div className="col-span-1">
          <Block title="필수 신호" content={jd.mustHaveSignals} icon={TbShieldCheck} delay={460} />
        </div>
        <div className="col-span-1">
          <Block title="고려사항" content={jd.considerations} icon={TbAlertCircle} delay={480} />
        </div>
        <div className="col-span-1">
          <Block title="준비 포인트" content={jd.preparationFocus} icon={TbBulb} delay={500} />
        </div>
        <div className="col-span-1">
          <Block title="증명 포인트" content={jd.proofPointsAndMetrics} icon={TbDiscount} useProofMetricsLayout delay={520} />
        </div>
        <div className="col-span-1 md:col-span-2">
          <Block title="강점 및 보완점" content={jd.transferableStrengthsAndGapPlan} icon={TbScale} delay={540} />
        </div>
      </div>

      {/* 면접 준비 */}
      <SectionDivider label="면접 준비" />
      <div className="grid gap-6 md:grid-cols-2">
        <div className="col-span-1 md:col-span-2">
          <Block title="스토리 앵글" content={jd.storyAngles} icon={TbStars} delay={560} />
        </div>
        {jd.insights && (
          <div className="col-span-1 md:col-span-2">
            <Block title="인사이트" content={jd.insights} icon={TbEye} delay={580} />
          </div>
        )}
        <div className="col-span-1 md:col-span-2">
          <Block title="면접 질문" content={jd.questionsToAsk} icon={TbMessages} delay={600} />
        </div>
      </div>
    </div>
  );
};

const highlightKeywords = (text: string) => {
  const keywords = [
    'Python', 'TypeScript', 'JavaScript', 'Java', 'Spring', 'React', 'Vue', 'Node.js', 'Go', 'C++',
    'AWS', 'GCP', 'Docker', 'Kubernetes', 'CI/CD', 'Git', 'SQL', 'NoSQL', 'RDBMS', 'RESTful', 'API',
    'AI', '데이터', '플랫폼', '인프라', '파이프라인', '백엔드', '프론트엔드', '풀스택', '아키텍처', '글로벌'
  ];

  const escapeRegex = (value: string) => value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  const joined = keywords.map(escapeRegex).join('|');
  const tokenRegex = new RegExp(`(${joined})`, 'gi');
  const isAsciiWordChar = (char: string) => /[A-Za-z0-9_]/.test(char);

  const parts: Array<{ value: string; highlighted: boolean }> = [];
  let lastIndex = 0;

  for (const match of text.matchAll(tokenRegex)) {
    const value = match[0];
    const start = match.index ?? 0;
    const end = start + value.length;
    const prev = start > 0 ? text[start - 1] : '';
    const next = end < text.length ? text[end] : '';

    // Prevent false positives like 'tailwind' -> 'ai'.
    if (isAsciiWordChar(prev) || isAsciiWordChar(next)) {
      continue;
    }

    if (start > lastIndex) {
      parts.push({ value: text.slice(lastIndex, start), highlighted: false });
    }
    parts.push({ value, highlighted: true });
    lastIndex = end;
  }

  if (lastIndex < text.length) {
    parts.push({ value: text.slice(lastIndex), highlighted: false });
  }

  if (parts.length === 0) return <>{text}</>;

  return (
    <>
      {parts.map((part, i) => (
        part.highlighted ? (
          <span key={i} className="inline-block px-1.5 py-0.5 mx-0.5 -my-0.5 align-baseline text-[13px] font-extrabold text-[#3FB6B2] bg-[#3FB6B2]/10 rounded-md shadow-sm ring-1 ring-[#3FB6B2]/20">
            {part.value}
          </span>
        ) : part.value
      ))}
    </>
  );
};

const parseProofMetricsSections = (content: string) => {
  const lines = content
    .split('\n')
    .map((line) => line.trim())
    .filter((line) => line.length > 0);

  const proofs: string[] = [];
  const metrics: string[] = [];
  let currentSection: 'proof' | 'metric' | null = null;

  const pushToCurrent = (value: string) => {
    if (!value) return;
    if (currentSection === 'metric') {
      metrics.push(value);
      return;
    }
    proofs.push(value);
  };

  for (const rawLine of lines) {
    const cleanLine = rawLine.replace(/^[-•*]\s*/, '').trim();
    if (!cleanLine) continue;

    const bracketMatch = cleanLine.match(/^\[(.*?)\]\s*(.*)$/);
    if (bracketMatch) {
      const label = bracketMatch[1].trim();
      const value = bracketMatch[2].trim();
      if (/지표|metric/i.test(label)) {
        currentSection = 'metric';
        if (value) metrics.push(value);
        continue;
      }
      if (/증명|증거|proof/i.test(label)) {
        currentSection = 'proof';
        if (value) proofs.push(value);
        continue;
      }
    }

    const labeledMatch = cleanLine.match(/^(증명|증거|proof|지표|metric|metrics)\s*[:：]\s*(.+)$/i);
    if (labeledMatch) {
      const label = labeledMatch[1];
      const value = labeledMatch[2].trim();
      if (/지표|metric/i.test(label)) {
        currentSection = 'metric';
        metrics.push(value);
      } else {
        currentSection = 'proof';
        proofs.push(value);
      }
      continue;
    }

    if (/^(증명|증거|proof)\s*[:：]?$/i.test(cleanLine)) {
      currentSection = 'proof';
      continue;
    }

    if (/^(지표|metric|metrics)\s*[:：]?$/i.test(cleanLine)) {
      currentSection = 'metric';
      continue;
    }

    pushToCurrent(cleanLine);
  }

  return { proofs, metrics };
};

const Block = ({
  title,
  content,
  icon: Icon,
  useProofMetricsLayout = false,
  delay = 0,
}: {
  title: string;
  content?: string | null;
  icon?: ElementType;
  useProofMetricsLayout?: boolean;
  delay?: number;
}) => {
  if (!content) return null;

  const lines = content.split('\n').filter((line) => line.trim() !== '');
  const proofMetrics = useProofMetricsLayout ? parseProofMetricsSections(content) : null;

  return (
    <div 
      className="group flex flex-col h-full rounded-3xl border border-gray-100 bg-white p-7 shadow-[0_4px_20px_-4px_rgba(0,0,0,0.05)] transition-all duration-300 hover:-translate-y-1 hover:border-[#3FB6B2]/40 hover:shadow-[0_8px_30px_-4px_rgba(63,182,178,0.15)] overflow-hidden relative"
      style={{ animationDelay: `${delay}ms`, animationFillMode: 'both', animationName: 'fadeUp', animationDuration: '0.6s' }}
    >
      <div className="absolute top-0 left-0 w-full h-1 bg-gradient-to-r from-[#3FB6B2] to-[#6EC8A7] opacity-0 group-hover:opacity-100 transition-opacity duration-300"></div>
      
      <div className="mb-5 flex items-center gap-3 text-xl font-bold text-gray-800">
        {Icon && (
          <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-2xl bg-gradient-to-br from-[#3FB6B2]/10 to-[#6EC8A7]/10 text-2xl text-[#3FB6B2] transition-all duration-300 group-hover:scale-110 group-hover:bg-[#3FB6B2] group-hover:text-white group-hover:shadow-md">
            <Icon />
          </div>
        )}
        {title}
      </div>
      {useProofMetricsLayout && proofMetrics ? (
        <div className="grow space-y-4">
          {proofMetrics.proofs.length > 0 && (
            <div className="rounded-2xl bg-gray-50/80 p-4">
              <div className="mb-2 text-sm font-extrabold text-gray-800">- 증명</div>
              <ul className="space-y-2 pl-4">
                {proofMetrics.proofs.map((item, idx) => (
                  <li key={`proof-${idx}`} className="text-[15px] leading-[1.6] text-gray-600 font-medium break-keep">
                    <span className="mr-1.5 text-[#3FB6B2]">-</span>
                    {highlightKeywords(item)}
                  </li>
                ))}
              </ul>
            </div>
          )}

          {proofMetrics.metrics.length > 0 && (
            <div className="rounded-2xl bg-gray-50/80 p-4">
              <div className="mb-2 text-sm font-extrabold text-gray-800">- 지표</div>
              <ul className="space-y-2 pl-4">
                {proofMetrics.metrics.map((item, idx) => (
                  <li key={`metric-${idx}`} className="text-[15px] leading-[1.6] text-gray-600 font-medium break-keep">
                    <span className="mr-1.5 text-[#3FB6B2]">-</span>
                    {highlightKeywords(item)}
                  </li>
                ))}
              </ul>
            </div>
          )}

          {proofMetrics.proofs.length === 0 && proofMetrics.metrics.length === 0 && (
            <ul className="space-y-4">
              {lines.map((line, idx) => {
                const cleanLine = line.replace(/^[-•*]\s*/, '').trim();
                if (!cleanLine) return null;

                return (
                  <li key={idx} className="flex items-start gap-3.5 rounded-xl px-2 py-2 transition-colors hover:bg-gray-50/80 -mx-2">
                    <span className="mt-2.5 flex h-1.5 w-1.5 shrink-0 items-center justify-center rounded-full bg-[#3FB6B2]/40 ring-4 ring-[#3FB6B2]/10 transition-all duration-300 group-hover:bg-[#3FB6B2] group-hover:ring-[#3FB6B2]/20" />
                    <div className="text-[15px] leading-[1.6] text-gray-600 font-medium break-keep">
                      {highlightKeywords(cleanLine)}
                    </div>
                  </li>
                );
              })}
            </ul>
          )}
        </div>
      ) : (
      <ul className="space-y-4 grow">
        {lines.map((line, idx) => {
          const cleanLine = line.replace(/^[-•*]\s*/, '').trim();
          if (!cleanLine) return null;

          const bracketMatch = cleanLine.match(/^\[(.*?)\]\s*(.*)$/);

          return (
            <li key={idx} className="flex items-start gap-3.5 rounded-xl px-2 py-2 transition-colors hover:bg-gray-50/80 -mx-2">
              <span className="mt-2.5 flex h-1.5 w-1.5 shrink-0 items-center justify-center rounded-full bg-[#3FB6B2]/40 ring-4 ring-[#3FB6B2]/10 transition-all duration-300 group-hover:bg-[#3FB6B2] group-hover:ring-[#3FB6B2]/20" />
              {bracketMatch ? (
                <div className="flex flex-col gap-1.5 w-full mt-[-2px]">
                  <span className="text-[12px] font-black tracking-wide text-[#3FB6B2] bg-[#3FB6B2]/10 self-start px-2 py-1.5 rounded-lg leading-none">
                    {bracketMatch[1]}
                  </span>
                  <div className="text-[15px] leading-[1.6] text-gray-600 font-medium break-keep">
                    {highlightKeywords(bracketMatch[2])}
                  </div>
                </div>
              ) : (
                <div className="text-[15px] leading-[1.6] text-gray-600 font-medium break-keep">
                  {highlightKeywords(cleanLine)}
                </div>
              )}
            </li>
          );
        })}
      </ul>
      )}
      <style>{`
        @keyframes fadeUp {
          from { opacity: 0; transform: translateY(20px); }
          to { opacity: 1; transform: translateY(0); }
        }
      `}</style>
    </div>
  );
};

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
  isAdmin,
  editingReviewId,
  adminReviewForm,
  adminReviewSubmitting,
  adminReviewDeletingId,
  onStartAdminEdit,
  onCancelAdminEdit,
  onAdminFormChange,
  onSubmitAdminEdit,
  onDeleteAsAdmin,
  onRestoreAsAdmin,
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
  isAdmin: boolean;
  editingReviewId: number | null;
  adminReviewForm: ReviewWriteReq;
  adminReviewSubmitting: boolean;
  adminReviewDeletingId: number | null;
  onStartAdminEdit: (review: any) => void;
  onCancelAdminEdit: () => void;
  onAdminFormChange: (value: ReviewWriteReq) => void;
  onSubmitAdminEdit: () => void;
  onDeleteAsAdmin: (reviewId: number) => void;
  onRestoreAsAdmin: (reviewId: number) => void;
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

    {!isAuthenticated ? null : loading ? (
      <div className="py-10 text-center text-sm text-gray-400">리뷰를 불러오는 중...</div>
    ) : !reviewPage?.items?.length ? (
      <div className="rounded-2xl border bg-white p-8 text-center text-sm text-gray-400">등록된 리뷰가 없습니다.</div>
    ) : (
      <div className="space-y-4">
        {reviewPage.items.map((r: any) => (
          <div
            key={r.id}
            className="rounded-2xl border border-[#3FB6B2]/20 bg-gradient-to-b from-[#f9fffe] to-white p-6 shadow-[0_10px_24px_rgba(16,24,40,0.06)]"
          >
            <div className="mb-2">
              <span className="inline-flex items-center rounded-full border border-[#3FB6B2]/35 bg-[#3FB6B2]/10 px-3 py-1 text-xs font-bold text-[#1f6f6c]">
                {HIRING_STAGE_LABELS[r.hiringStage as HiringStage] || r.hiringStage}
              </span>
            </div>
            <div className="mb-4 grid gap-2 rounded-xl border border-[#3FB6B2]/10 bg-[#f6fcfb] p-3">
              <RatingStarsDisplay label="난이도" value={r.difficultyRating} />
              <RatingStarsDisplay label="만족도" value={r.satisfactionRating} />
            </div>

            {r.deleted ? (
              <div className="space-y-4">
                <div className="rounded-xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-700">
                  삭제된 리뷰입니다.
                </div>
                {isAdmin && (
                  <button
                    type="button"
                    onClick={() => onRestoreAsAdmin(r.id)}
                    disabled={adminReviewDeletingId === r.id}
                    className="rounded-lg border border-[#3FB6B2]/40 px-3 py-1.5 text-xs font-semibold text-[#2b8f8c] hover:bg-[#3FB6B2]/5 disabled:opacity-60"
                  >
                    {adminReviewDeletingId === r.id ? '복구 중...' : '복구'}
                  </button>
                )}
              </div>
            ) : isAdmin && editingReviewId === r.id ? (
              <div className="space-y-4">
                <div className="grid gap-4 md:grid-cols-2">
                  <label className="space-y-2">
                    <span className="text-xs font-semibold text-gray-500">채용 단계</span>
                    <select
                      value={adminReviewForm.hiringStage}
                      onChange={(e) => onAdminFormChange({ ...adminReviewForm, hiringStage: e.target.value as HiringStage })}
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
                    onClick={() => onAdminFormChange({ ...adminReviewForm, anonymous: !adminReviewForm.anonymous })}
                    className={`flex h-[74px] items-center justify-between rounded-xl border px-4 transition ${
                      adminReviewForm.anonymous ? 'border-[#3FB6B2] bg-[#3FB6B2]/5' : 'border-gray-200 bg-white'
                    }`}
                  >
                    <div className="text-left">
                      <p className="text-xs font-semibold text-gray-500">작성 방식</p>
                      <p className="text-sm font-semibold text-gray-800">익명으로 작성</p>
                    </div>
                    <span
                      className={`inline-flex h-6 w-11 items-center rounded-full p-1 transition ${
                        adminReviewForm.anonymous ? 'bg-[#3FB6B2]' : 'bg-gray-300'
                      }`}
                    >
                      <span className={`h-4 w-4 rounded-full bg-white transition ${adminReviewForm.anonymous ? 'translate-x-5' : 'translate-x-0'}`} />
                    </span>
                  </button>
                </div>

                <div className="grid gap-4 md:grid-cols-2">
                  <ScoreSelector
                    label="난이도"
                    value={adminReviewForm.difficultyRating}
                    onChange={(value) => onAdminFormChange({ ...adminReviewForm, difficultyRating: value })}
                  />
                  <ScoreSelector
                    label="만족도"
                    value={adminReviewForm.satisfactionRating}
                    onChange={(value) => onAdminFormChange({ ...adminReviewForm, satisfactionRating: value })}
                  />
                </div>

                <textarea
                  value={adminReviewForm.experienceComment}
                  onChange={(e) => onAdminFormChange({ ...adminReviewForm, experienceComment: e.target.value })}
                  className="min-h-[120px] w-full rounded-xl border border-gray-200 p-4 text-sm"
                  placeholder="리뷰 내용을 입력해 주세요."
                />
                <textarea
                  value={adminReviewForm.interviewTip || ''}
                  onChange={(e) => onAdminFormChange({ ...adminReviewForm, interviewTip: e.target.value })}
                  className="min-h-[90px] w-full rounded-xl border border-gray-200 p-4 text-sm"
                  placeholder="면접 팁 (선택)"
                />

                <div className="flex items-center gap-2">
                  <button
                    type="button"
                    onClick={onSubmitAdminEdit}
                    disabled={adminReviewSubmitting}
                    className="rounded-xl bg-[#3FB6B2] px-4 py-2 text-sm font-semibold text-white disabled:opacity-60"
                  >
                    {adminReviewSubmitting ? '수정 중...' : '수정 저장'}
                  </button>
                  <button
                    type="button"
                    onClick={onCancelAdminEdit}
                    className="rounded-xl border border-gray-200 px-4 py-2 text-sm font-semibold text-gray-600"
                  >
                    취소
                  </button>
                </div>
              </div>
            ) : (
              <>
                <div className="whitespace-pre-line text-sm text-gray-700">{r.experienceComment}</div>
                {r.interviewTip && <div className="mt-3 text-xs text-[#3FB6B2]">TIP: {r.interviewTip}</div>}
                {isAdmin && !r.deleted && (
                  <div className="mt-4 flex items-center gap-2">
                    <button
                      type="button"
                      onClick={() => onStartAdminEdit(r)}
                      className="rounded-lg border border-gray-200 px-3 py-1.5 text-xs font-semibold text-gray-700 hover:bg-gray-50"
                    >
                      수정
                    </button>
                    <button
                      type="button"
                      onClick={() => onDeleteAsAdmin(r.id)}
                      disabled={adminReviewDeletingId === r.id}
                      className="rounded-lg border border-red-200 px-3 py-1.5 text-xs font-semibold text-red-600 hover:bg-red-50 disabled:opacity-60"
                    >
                      {adminReviewDeletingId === r.id ? '삭제 중...' : '삭제'}
                    </button>
                  </div>
                )}
              </>
            )}
          </div>
        ))}
      </div>
    )}
  </div>
);

const normalizeScore = (value: number) => {
  if (!Number.isFinite(value)) return 1;
  return Math.min(10, Math.max(1, Math.round(value)));
};

const RatingStarsDisplay = ({ label, value }: { label: string; value: number }) => (
  <div className="grid grid-cols-[72px_minmax(0,1fr)_66px] items-center gap-3 rounded-lg border border-gray-100 bg-white px-3 py-2.5">
    <span className="text-xs font-semibold text-gray-700">{label}</span>
    <div className="flex min-w-[120px] items-center gap-1">
      {[0, 1, 2, 3, 4].map((idx) => {
        const starValue = normalizeScore(value) / 2;
        const fill = Math.max(0, Math.min(1, starValue - idx));
        return (
          <div key={`${label}-${idx}`} className="relative h-5 w-5">
            <StarIcon className="h-5 w-5 text-gray-300" />
            {fill > 0 && (
              <div className="absolute inset-0 overflow-hidden" style={{ width: `${fill * 100}%` }}>
                <StarIcon className="h-5 w-5 text-[#F59E0B]" />
              </div>
            )}
          </div>
        );
      })}
    </div>
    <span className="inline-flex h-6 items-center justify-center rounded-full bg-amber-50 px-2.5 text-sm font-bold tabular-nums text-amber-700">
      {(normalizeScore(value) / 2).toFixed(1)}
    </span>
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
  <div className="rounded-xl border border-[#3FB6B2]/20 bg-white p-4">
    <div className="mb-3 flex items-center justify-between">
      <span className="text-xs font-semibold text-gray-600">{label}</span>
      <span className="rounded-full bg-[#3FB6B2]/10 px-2 py-0.5 text-xs font-bold tabular-nums text-[#2d918d]">
        {(normalizeScore(value) / 2).toFixed(1)} / 5.0
      </span>
    </div>
    <div>
      <div className="mb-1 flex items-center gap-1.5">
        {[0, 1, 2, 3, 4].map((idx) => {
          const starValue = normalizeScore(value) / 2;
          const fill = Math.max(0, Math.min(1, starValue - idx));
          return (
            <div key={idx} className="relative h-9 w-9">
              <StarIcon className="h-9 w-9 text-gray-300" />
              {fill > 0 && (
                <div className="absolute inset-0 overflow-hidden" style={{ width: `${fill * 100}%` }}>
                  <StarIcon className="h-9 w-9 text-[#F59E0B]" />
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
      <p className="text-[11px] text-gray-500">반별(0.5별) 단위로 선택됩니다. 0.5별 = 1점</p>
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

export default JobSummaryDetailPage;

