import { useEffect, useState } from 'react';
import type { ElementType } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import {
  TbChevronLeft,
  TbDeviceFloppy,
  TbReload,
  TbNotes,
  TbBriefcase,
  TbUserCheck,
  TbStar,
  TbBulb,
  TbDiscount,
  TbMessages,
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
import {
  TbChevronLeft,
} from 'react-icons/tb';
import { toast } from 'react-toastify';

/* ========================================================= */

const JobSummaryDetailPage = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { isAuthenticated, user } = useAuthStore();
  const isAdmin = user?.role === 'ADMIN';

  const [jd, setJd] = useState<JobSummaryDetailView | null>(null);

  // ✅ 탭 확장
  const [activeTab, setActiveTab] =
    useState<'detail' | 'review' | 'memo' | 'resume'>('detail');

  const [reviewPage, setReviewPage] = useState<any>(null);


  /* ---------------- JD 조회 ---------------- */

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

  /* ---------------- 리뷰 조회 ---------------- */

  useEffect(() => {
    if (!jd || activeTab !== 'review') return;
    jdSummaryService.getReviews(jd.id).then(setReviewPage);
  }, [jd, activeTab]);

  if (!jd) return null;

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

        {/* 탭 */}
        <div className="flex gap-6 border-b mb-8 font-bold text-sm">
          <TabButton label="상세정보" active={activeTab === 'detail'} onClick={() => setActiveTab('detail')} />
          <TabButton label="리뷰" active={activeTab === 'review'} onClick={() => setActiveTab('review')} />
          <TabButton label="준비 기록" active={activeTab === 'prep'} onClick={() => setActiveTab('prep')} disabled={!isAuthenticated} />
        </div>

        {activeTab === 'detail' && <DetailSection jd={jd} />}
        {activeTab === 'review' && <ReviewSection reviewPage={reviewPage} />}
        {activeTab === 'memo' && <PreparationSection jd={jd} />}
        {activeTab === 'resume' && <ResumeSection jd={jd} />}

      </div>
    </div>
  );
};

/* ========================================================= */
/* 상세 탭 */
/* ========================================================= */

const DetailSection = ({ jd }: { jd: JobSummaryDetailView }) => (
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
    <div className="col-span-1">
      <Block title="준비 포인트" content={jd.preparationFocus} icon={TbBulb} delay={400} />
    </div>
    <div className="col-span-1">
      <Block title="증명 포인트" content={jd.proofPointsAndMetrics} icon={TbDiscount} useProofMetricsLayout delay={500} />
    </div>
    <div className="col-span-1 md:col-span-2">
      <Block title="면접 질문" content={jd.questionsToAsk} icon={TbMessages} delay={600} />
    </div>
  </div>
);

const Block = ({ title, content }: any) =>
  content ? (
    <div className="bg-white p-6 rounded-2xl border">
      <div className="font-bold mb-2 text-[#3FB6B2]">{title}</div>
      <div className="text-sm whitespace-pre-line">{content}</div>
    </div>
  ) : null;

/* ========================================================= */
/* 리뷰 탭 */
/* ========================================================= */

const ReviewSection = ({ reviewPage }: any) => {
  if (!reviewPage?.items) return null;

  return (
    <div className="space-y-6">
      {reviewPage.items.map((r: any) => (
        <div key={r.id} className="bg-white p-6 rounded-2xl border">
          <div className="text-sm whitespace-pre-line">
            {r.experienceComment}
          </div>
        </div>
      ))}
    </div>
  );
};

/* ========================================================= */
/* 준비기록 탭 (작성 + 정리보기) */
/* ========================================================= */

const stages: HiringStage[] = [
  'DOCUMENT',
  'CODING_TEST',
  'ASSIGNMENT',
  'INTERVIEW_1',
  'INTERVIEW_2',
  'FINAL_INTERVIEW',
];

const PreparationSection = ({ jd }: { jd: JobSummaryDetailView }) => {
  const key = `prep-${jd.id}`;
  const [data, setData] = useState<Record<string, string>>({});
  const [activeStage, setActiveStage] = useState<HiringStage>('DOCUMENT');
  const [mode, setMode] = useState<'edit' | 'overview'>('edit');

  useEffect(() => {
    const saved = localStorage.getItem(key);
    if (saved) setData(JSON.parse(saved));
  }, [key]);

  const save = () => {
    localStorage.setItem(key, JSON.stringify(data));
    toast.success('저장 완료');
  };

  if (mode === 'overview') {
    return (
      <div className="space-y-6">
        <button onClick={() => setMode('edit')} className="text-[#3FB6B2] text-sm">
          ← 다시 작성하기
        </button>

        {stages.map((s) => (
          <div key={s} className="bg-white p-6 rounded-2xl border">
            <div className="font-bold text-[#3FB6B2] mb-2">
              {HIRING_STAGE_LABELS[s]}
            </div>
            <div className="text-sm whitespace-pre-line">
              {data[s] || '기록 없음'}
            </div>
          </div>
        ))}
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap gap-2">
        {stages.map((s) => (
          <button
            key={s}
            onClick={() => setActiveStage(s)}
            className={`px-4 py-2 rounded-xl text-sm ${activeStage === s ? 'bg-[#3FB6B2] text-white' : 'bg-gray-100'
              }`}
          >
            {HIRING_STAGE_LABELS[s]}
          </button>
        ))}
      </div>

      <textarea
        className="w-full border rounded-2xl p-4 min-h-[200px]"
        value={data[activeStage] || ''}
        onChange={(e) =>
          setData({ ...data, [activeStage]: e.target.value })
        }
        placeholder="이 단계에서 받은 질문, 개선점, 전략 기록"
      />

      <div className="flex gap-4">
        <button onClick={save} className="px-6 py-2 bg-[#3FB6B2] text-white rounded-xl">
          저장
        </button>
        <button onClick={() => setMode('overview')} className="px-6 py-2 border rounded-xl">
          전체 기록 보기
        </button>
      </div>
    </div>
  );
};

/* ========================================================= */
/* 자소서/이력서 탭 */
/* ========================================================= */

const ResumeSection = ({ jd }: { jd: JobSummaryDetailView }) => {
  const key = `resume-${jd.id}`;
  const [content, setContent] = useState('');

  useEffect(() => {
    const saved = localStorage.getItem(key);
    if (saved) setContent(saved);
  }, [key]);

  const save = () => {
    localStorage.setItem(key, content);
    toast.success('저장 완료');
  };

  return (
    <div className="space-y-6">
      <textarea
        className="w-full border rounded-2xl p-4 min-h-[300px]"
        value={content}
        onChange={(e) => setContent(e.target.value)}
        placeholder="이 공고에 제출한 자소서/이력서 전체 내용 기록"
      />

      <button onClick={save} className="px-6 py-2 bg-[#3FB6B2] text-white rounded-xl">
        저장
      </button>
    </div>
  );
};

/* ========================================================= */

const TabButton = ({ label, active, onClick }: any) => (
  <button
    onClick={onClick}
    className={`pb-2 ${active
      ? 'border-b-2 border-[#3FB6B2] text-[#3FB6B2]'
      : 'text-gray-400'
      }`}
  >
    {label}
  </button>
);

export default JobSummaryDetailPage;