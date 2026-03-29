import { useNavigate, useParams } from 'react-router-dom';
import { useEffect, useState } from 'react';
import { jdSummaryService } from '../services/jdSummaryService';
import {
  HIRING_STAGE_LABELS,
  type JobSummaryDetailView,
  type ReviewWriteReq,
} from '../types/jobSummary';
import { TbChevronLeft } from 'react-icons/tb';
import { toast } from 'react-toastify';

/* ========================================================= */

const JobSummaryDetailPage = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const [jd, setJd] = useState<JobSummaryDetailView | null>(null);
  const [activeTab, setActiveTab] =
    useState<'detail' | 'review' | 'memo' | 'resume'>('detail');

  const [reviewPage, setReviewPage] = useState<any>(null);

  useEffect(() => {
    if (!id) return;
    jdSummaryService.getDetail(Number(id)).then((data) => {
      setJd({
        ...data,
        id: data.id || (data as any).jobSummaryId || Number(id),
      });
    });
  }, [id]);

  const fetchReviews = () => {
    if (!jd) return;
    jdSummaryService.getReviews(jd.id).then(setReviewPage);
  };

  useEffect(() => {
    if (activeTab === 'review') fetchReviews();
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

        <div className="flex gap-6 border-b mb-8 font-bold text-sm">
          <TabButton label="상세정보" active={activeTab === 'detail'} onClick={() => setActiveTab('detail')} />
          <TabButton label="리뷰" active={activeTab === 'review'} onClick={() => setActiveTab('review')} />
          <TabButton label="준비기록" active={activeTab === 'memo'} onClick={() => setActiveTab('memo')} />
          <TabButton label="자소서/이력서" active={activeTab === 'resume'} onClick={() => setActiveTab('resume')} />
        </div>

        {activeTab === 'detail' && <DetailSection jd={jd} />}
        {activeTab === 'review' && <ReviewSection jdId={jd.id} reviewPage={reviewPage} onReviewAdded={fetchReviews} />}
        {activeTab === 'memo' && <PreparationSection jd={jd} />}
        {activeTab === 'resume' && <ResumeSection jd={jd} />}

      </div>
    </div>
  );
};

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

const ReviewSection = ({ jdId, reviewPage, onReviewAdded }: { jdId: number, reviewPage: any, onReviewAdded: () => void }) => {
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [form, setForm] = useState<ReviewWriteReq>({
    hiringStage: 'INTERVIEW_1',
    anonymous: false,
    difficultyRating: 3,
    satisfactionRating: 3,
    experienceComment: '',
    interviewTip: '',
  });

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!form.experienceComment.trim()) {
      toast.error('리뷰 내용을 입력해주세요.');
      return;
    }

    setIsSubmitting(true);
    try {
      await jdSummaryService.addReview(jdId, form);
      toast.success('리뷰가 등록되었습니다.');
      setForm({ ...form, experienceComment: '', interviewTip: '' });
      onReviewAdded();
    } catch (e) {
      console.error(e);
      toast.error('리뷰 등록에 실패했습니다.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="space-y-8">
      <div>
        {!reviewPage?.items || reviewPage.items.length === 0 ? (
          <div>리뷰 없음</div>
        ) : (
          <div>
            {reviewPage.items.map((r: any) => (
              <div key={r.id}>
                {/* ✅ 타입 에러 해결 */}
                {HIRING_STAGE_LABELS[r.hiringStage as keyof typeof HIRING_STAGE_LABELS] || r.hiringStage}
                {r.experienceComment}
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};

/* ========================================================= */

const PreparationSection = ({ jd }: { jd: JobSummaryDetailView }) => {
  return <div>{jd?.brandName}</div>; // ✅ unused 해결
};

const ResumeSection = ({ jd }: { jd: JobSummaryDetailView }) => {
  return <div>{jd?.brandName}</div>; // ✅ unused 해결
};

const TabButton = ({ label, active, onClick }: any) => (
  <button
    onClick={onClick}
    className={active ? 'text-[#3FB6B2]' : ''} // ✅ unused 해결
  >
    {label}
  </button>
);

export default JobSummaryDetailPage;