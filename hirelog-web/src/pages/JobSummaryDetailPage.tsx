import { useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { TbChevronLeft, TbDeviceFloppy, TbReload } from 'react-icons/tb';
import { toast } from 'react-toastify';
import { jdSummaryService } from '../services/jdSummaryService';
import { useAuthStore } from '../store/authStore';
import {
  HIRING_STAGE_LABELS,
  type HiringStage,
  type HiringStageView,
  type JobSummaryDetailView,
} from '../types/jobSummary';

const tabs = ['detail', 'review', 'prep'] as const;
type TabType = (typeof tabs)[number];

const stageOrder: HiringStage[] = [
  'DOCUMENT',
  'CODING_TEST',
  'ASSIGNMENT',
  'INTERVIEW_1',
  'INTERVIEW_2',
  'FINAL_INTERVIEW',
];

const JobSummaryDetailPage = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const [jd, setJd] = useState<JobSummaryDetailView | null>(null);
  const [loading, setLoading] = useState(true);
  const [loadFailed, setLoadFailed] = useState(false);
  const [activeTab, setActiveTab] = useState<TabType>('detail');
  const { isAuthenticated } = useAuthStore();

  const [reviewPage, setReviewPage] = useState<any>(null);
  const [reviewLoading, setReviewLoading] = useState(false);

  const [stages, setStages] = useState<Record<HiringStage, HiringStageView | undefined>>({} as any);
  const [activeStage, setActiveStage] = useState<HiringStage>('DOCUMENT');
  const [note, setNote] = useState('');
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

  useEffect(() => {
    if (!jd || !isAuthenticated || activeTab !== 'review') return;
    setReviewLoading(true);
    jdSummaryService
      .getReviews(jd.id)
      .then(setReviewPage)
      .finally(() => setReviewLoading(false));
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
  }, [jd, activeTab, isAuthenticated]);

  useEffect(() => {
    setNote(stages[activeStage]?.note || '');
  }, [activeStage, stages]);

  const savePreparation = async () => {
    if (!jd) return;
    if (!note.trim()) {
      toast.warn('내용을 입력해 주세요.');
      return;
    }

    setSavingPrep(true);
    try {
      await jdSummaryService.saveStageNote(jd.id, activeStage, note.trim());
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

  const prepSummary = useMemo(() => {
    const document = stages.DOCUMENT?.note;
    const coding = stages.CODING_TEST?.note;
    return {
      document: document && document.length > 0,
      coding: coding && coding.length > 0,
    };
  }, [stages]);

  if (loading) {
    return <div className="min-h-screen bg-[#F8F9FA] pt-24" />;
  }

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
          <TabButton
            label="리뷰"
            active={activeTab === 'review'}
            onClick={() => setActiveTab('review')}
            disabled={!isAuthenticated}
          />
          <TabButton
            label="준비 기록"
            active={activeTab === 'prep'}
            onClick={() => setActiveTab('prep')}
            disabled={!isAuthenticated}
          />
        </div>

        {!isAuthenticated && (
          <div className="mb-6 rounded-xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-800">
            비로그인 상태에서는 JD 상세 조회만 가능합니다. 리뷰/준비기록 기능은 로그인 후 이용해 주세요.
          </div>
        )}

        {activeTab === 'detail' && <DetailSection jd={jd} />}

        {activeTab === 'review' && isAuthenticated && (
          <ReviewSection reviewPage={reviewPage} loading={reviewLoading} />
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
                  {stageOrder.map((stage) => (
                    <button
                      key={stage}
                      onClick={() => setActiveStage(stage)}
                      className={`w-full rounded-xl px-3 py-2 text-left text-sm font-semibold transition ${
                        activeStage === stage
                          ? 'bg-[#3FB6B2] text-white'
                          : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                      }`}
                    >
                      {HIRING_STAGE_LABELS[stage]}
                    </button>
                  ))}
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

const ReviewSection = ({ reviewPage, loading }: { reviewPage: any; loading: boolean }) => {
  if (loading) {
    return <div className="py-10 text-center text-sm text-gray-400">리뷰를 불러오는 중...</div>;
  }

  if (!reviewPage?.items?.length) {
    return <div className="rounded-2xl border bg-white p-8 text-center text-sm text-gray-400">등록된 리뷰가 없습니다.</div>;
  }

  return (
    <div className="space-y-4">
      {reviewPage.items.map((r: any) => (
        <div key={r.id} className="rounded-2xl border bg-white p-6">
          <div className="mb-2 text-xs text-gray-400">
            {HIRING_STAGE_LABELS[r.hiringStage as HiringStage] || r.hiringStage}
          </div>
          <div className="whitespace-pre-line text-sm text-gray-700">{r.experienceComment}</div>
        </div>
      ))}
    </div>
  );
};

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
    className={`pb-2 ${
      active ? 'border-b-2 border-[#3FB6B2] text-[#3FB6B2]' : 'text-gray-400'
    } ${disabled ? 'cursor-not-allowed opacity-50' : ''}`}
  >
    {label}
  </button>
);

const StatusBadge = ({ label, done }: { label: string; done: boolean }) => (
  <div className="flex items-center justify-between rounded-xl bg-white px-4 py-3">
    <span className="text-sm font-semibold text-gray-700">{label}</span>
    <span
      className={`rounded-full px-2.5 py-1 text-xs font-semibold ${
        done ? 'bg-[#3FB6B2]/10 text-[#3FB6B2]' : 'bg-gray-100 text-gray-500'
      }`}
    >
      {done ? '저장됨' : '미저장'}
    </span>
  </div>
);

export default JobSummaryDetailPage;
