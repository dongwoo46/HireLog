import { useNavigate, useParams } from 'react-router-dom';
import { useEffect, useState, useMemo } from 'react';
import { jdSummaryService } from '../services/jdSummaryService';
import {
  HIRING_STAGE_LABELS,
  type JobSummaryDetailView,
  type ReviewWriteReq,
  type HiringStage,
} from '../types/jobSummary';
import {
  TbChevronLeft,
  TbStar,
  TbStarFilled,
  TbStarHalfFilled,
} from 'react-icons/tb';
import { toast } from 'react-toastify';

/* ========================================================= */

const JobSummaryDetailPage = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const [jd, setJd] = useState<JobSummaryDetailView | null>(null);

  // ✅ 탭 확장
  const [activeTab, setActiveTab] =
    useState<'detail' | 'review' | 'memo' | 'resume'>('detail');

  const [reviewPage, setReviewPage] = useState<any>(null);
  const [sortType, setSortType] = useState<'latest' | 'high'>('latest');

  const [reviewForm, setReviewForm] = useState<ReviewWriteReq>({
    hiringStage: 'DOCUMENT',
    anonymous: true,
    difficultyRating: 6,
    satisfactionRating: 6,
    experienceComment: '',
    interviewTip: '',
  });

  /* ---------------- JD 조회 ---------------- */

  useEffect(() => {
    if (!id) return;
    jdSummaryService.getDetail(Number(id)).then((data) => {
      setJd({
        ...data,
        id: data.id || (data as any).jobSummaryId || Number(id),
      });
    });
  }, [id]);

  /* ---------------- 리뷰 조회 ---------------- */

  useEffect(() => {
    if (!jd || activeTab !== 'review') return;
    jdSummaryService.getReviews(jd.id).then(setReviewPage);
  }, [jd, activeTab]);

  if (!jd) return null;

  return (
    <div className="min-h-screen bg-[#F8F9FA] pt-24 pb-20">
      <div className="max-w-4xl mx-auto px-6">

        <button onClick={() => navigate(-1)} className="mb-6 flex gap-2 text-gray-500">
          <TbChevronLeft /> 목록으로
        </button>

        <div className="bg-gradient-to-r from-[#3FB6B2] to-[#6EC8A7] p-10 rounded-3xl mb-10 text-white shadow-lg">
          <h1 className="text-4xl font-black mb-2">{jd.brandName}</h1>
          <h2>{jd.brandPositionName}</h2>
        </div>

        {/* 탭 */}
        <div className="flex gap-6 border-b mb-8 font-bold text-sm">
          <TabButton label="상세정보" active={activeTab === 'detail'} onClick={() => setActiveTab('detail')} />
          <TabButton label="리뷰" active={activeTab === 'review'} onClick={() => setActiveTab('review')} />
          <TabButton label="준비기록" active={activeTab === 'memo'} onClick={() => setActiveTab('memo')} />
          <TabButton label="자소서/이력서" active={activeTab === 'resume'} onClick={() => setActiveTab('resume')} />
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
  <div className="space-y-6">
    <Block title="요약" content={jd.summaryText} />
    <Block title="주요 업무" content={jd.responsibilities} />
    <Block title="자격 요건" content={jd.requiredQualifications} />
    <Block title="우대 사항" content={jd.preferredQualifications} />
    <Block title="준비 포인트" content={(jd as any).preparationFocus} />
    <Block title="어필 지표" content={(jd as any).proofPointsAndMetrics} />
    <Block title="물어볼 질문" content={(jd as any).questionsToAsk} />
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
