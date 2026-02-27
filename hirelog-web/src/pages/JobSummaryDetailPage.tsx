import { useNavigate, useParams } from 'react-router-dom';
import { useEffect, useState, useMemo } from 'react';
import { jdSummaryService } from '../services/jdSummaryService';
import {
  HIRING_STAGE_LABELS,
  type JobSummaryDetailView,
  type ReviewWriteReq,
} from '../types/jobSummary';
import {
  TbChevronLeft,
  TbStar,
  TbStarFilled,
  TbStarHalfFilled,
  TbSparkles,
  TbFileText,
  TbBriefcase,
  TbListCheck,
  TbBulb,
} from 'react-icons/tb';
import { toast } from 'react-toastify';

/* =========================================================
   Main Component
========================================================= */

const JobSummaryDetailPage = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const [jd, setJd] = useState<JobSummaryDetailView | null>(null);
  const [activeTab, setActiveTab] = useState<'detail' | 'review'>('detail');

  const [reviewPage, setReviewPage] = useState<any>(null);
  const [isReviewModalOpen, setIsReviewModalOpen] = useState(false);
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

  /* ---------------- 평균 계산 ---------------- */

  const averages = useMemo(() => {
    if (!reviewPage?.items?.length) return null;

    const diff =
      reviewPage.items.reduce((sum: number, r: any) => sum + r.difficultyRating, 0) /
      reviewPage.items.length;

    const sat =
      reviewPage.items.reduce((sum: number, r: any) => sum + r.satisfactionRating, 0) /
      reviewPage.items.length;

    return {
      difficulty: diff.toFixed(1),
      satisfaction: sat.toFixed(1),
    };
  }, [reviewPage]);

  /* ---------------- 정렬 ---------------- */

  const sortedReviews = useMemo(() => {
    if (!reviewPage?.items) return [];

    const arr = [...reviewPage.items];

    if (sortType === 'high') {
      return arr.sort((a, b) => b.satisfactionRating - a.satisfactionRating);
    }

    return arr.sort(
      (a, b) =>
        new Date(b.createdAt).getTime() -
        new Date(a.createdAt).getTime()
    );
  }, [reviewPage, sortType]);

  /* ---------------- 리뷰 등록 ---------------- */

  const handleAddReview = async () => {
    if (!jd) return;

    if (reviewForm.experienceComment.length < 5) {
      toast.error('5자 이상 작성해주세요.');
      return;
    }

    try {
      await jdSummaryService.addReview(jd.id, reviewForm);
      toast.success('리뷰가 등록되었습니다.');

      const res = await jdSummaryService.getReviews(jd.id);
      setReviewPage(res);

      setIsReviewModalOpen(false);

      setReviewForm({
        hiringStage: 'DOCUMENT',
        anonymous: true,
        difficultyRating: 6,
        satisfactionRating: 6,
        experienceComment: '',
        interviewTip: '',
      });
    } catch {
      toast.error('리뷰 등록 실패');
    }
  };

  if (!jd) return null;

  return (
    <div className="min-h-screen bg-[#F8F9FA] pt-24 pb-20">
      <div className="max-w-4xl mx-auto px-6">

        {/* 뒤로가기 */}
        <button onClick={() => navigate(-1)} className="mb-6 flex gap-2 text-gray-500">
          <TbChevronLeft /> 목록으로
        </button>

        {/* 헤더 */}
        <div className="bg-gradient-to-r from-[#3FB6B2] to-[#6EC8A7] p-10 rounded-3xl mb-10 text-white shadow-lg">
          <h1 className="text-4xl font-black mb-2">{jd.brandName}</h1>
          <h2>{jd.brandPositionName}</h2>
        </div>

        {/* 탭 */}
        <div className="flex gap-6 border-b mb-8 font-bold text-sm">
          <button
            onClick={() => setActiveTab('detail')}
            className={activeTab === 'detail'
              ? 'border-b-2 border-[#3FB6B2] text-[#3FB6B2] pb-2'
              : 'text-gray-400 pb-2'}
          >
            상세정보
          </button>

          <button
            onClick={() => setActiveTab('review')}
            className={activeTab === 'review'
              ? 'border-b-2 border-[#3FB6B2] text-[#3FB6B2] pb-2'
              : 'text-gray-400 pb-2'}
          >
            리뷰 ({reviewPage?.totalElements || 0})
          </button>
        </div>

        {/* ================= 상세 ================= */}
        {activeTab === 'detail' && (
          <div className="space-y-8">

            {jd.insights && (
              <ContentSection icon={<TbSparkles />} title="AI 인사이트">
                {jd.insights}
              </ContentSection>
            )}

            {jd.summaryText && (
              <ContentSection icon={<TbFileText />} title="요약">
                {jd.summaryText}
              </ContentSection>
            )}

            {jd.responsibilities && (
              <ContentSection icon={<TbBriefcase />} title="주요 업무">
                {jd.responsibilities}
              </ContentSection>
            )}

            {jd.requiredQualifications && (
              <ContentSection icon={<TbListCheck />} title="자격 요건">
                {jd.requiredQualifications}
              </ContentSection>
            )}

            {jd.preferredQualifications && (
              <ContentSection icon={<TbBulb />} title="우대 사항">
                {jd.preferredQualifications}
              </ContentSection>
            )}

          </div>
        )}

        {/* ================= 리뷰 ================= */}
        {activeTab === 'review' && (
          <div className="space-y-10">

            {averages && (
              <div className="bg-white p-8 rounded-3xl shadow-sm border flex justify-between">
                <div>
                  <div className="text-sm text-gray-400">평균 만족도</div>
                  <div className="text-3xl font-black text-[#3FB6B2]">
                    {averages.satisfaction}점
                  </div>
                </div>
                <div>
                  <div className="text-sm text-gray-400">평균 난이도</div>
                  <div className="text-3xl font-black text-[#3FB6B2]">
                    {averages.difficulty}점
                  </div>
                </div>
              </div>
            )}

            <div className="flex justify-between items-center">
              <div className="flex gap-4 text-sm font-bold">
                <button onClick={() => setSortType('latest')}>최신순</button>
                <button onClick={() => setSortType('high')}>평점 높은순</button>
              </div>

              <button
                onClick={() => setIsReviewModalOpen(true)}
                className="px-5 py-2 bg-[#3FB6B2] text-white rounded-xl"
              >
                리뷰 작성하기
              </button>
            </div>

            {sortedReviews.map((review: any) => (
              <ReviewCard key={review.id} review={review} />
            ))}
          </div>
        )}

        {/* ================= 모달 ================= */}
        {isReviewModalOpen && (
          <ReviewModal
            reviewForm={reviewForm}
            setReviewForm={setReviewForm}
            onClose={() => setIsReviewModalOpen(false)}
            onSubmit={handleAddReview}
          />
        )}

      </div>
    </div>
  );
};

/* =========================================================
   Sub Components
========================================================= */

const ContentSection = ({ icon, title, children }: any) => (
  <div className="bg-white p-6 rounded-2xl shadow-sm border">
    <div className="flex items-center gap-2 mb-3 text-gray-500">
      {icon}
      <h2 className="text-sm font-bold">{title}</h2>
    </div>
    <div className="text-sm whitespace-pre-line">{children}</div>
  </div>
);

/* ---------------- 리뷰 카드 ---------------- */

const ReviewCard = ({ review }: any) => (
  <div className="bg-white p-7 rounded-3xl shadow-sm border">
    <div className="flex justify-between mb-4">
      <span className="text-xs font-bold text-[#3FB6B2]">
        {/* {HIRING_STAGE_LABELS[review.hiringStage]} */}
      </span>
      <span className="text-xs text-gray-400">
        {new Date(review.createdAt).toLocaleDateString('ko-KR')}
      </span>
    </div>

    <StarDisplay10 value={review.satisfactionRating} label="만족도" />
    <StarDisplay10 value={review.difficultyRating} label="난이도" />

    <p className="mt-4 text-sm whitespace-pre-line">
      {review.experienceComment}
    </p>

    {review.interviewTip && (
      <div className="mt-4 bg-amber-50 p-4 rounded-2xl">
        💡 {review.interviewTip}
      </div>
    )}
  </div>
);

/* ---------------- 별점 표시 ---------------- */

const StarDisplay10 = ({ value, label }: any) => (
  <div className="flex items-center gap-3 mt-2">
    <span className="text-xs font-bold text-gray-400">{label}</span>
    <div className="flex gap-1">
      {Array.from({ length: 5 }).map((_, i) => {
        const full = (i + 1) * 2;
        const half = full - 1;

        if (value >= full)
          return <TbStarFilled key={i} className="text-[#3FB6B2]" />;
        if (value === half)
          return <TbStarHalfFilled key={i} className="text-[#3FB6B2]" />;
        return <TbStar key={i} className="text-gray-300" />;
      })}
    </div>
    <span className="text-sm font-bold">{value}점</span>
  </div>
);

/* ---------------- 리뷰 모달 ---------------- */

const ReviewModal = ({ reviewForm, setReviewForm, onClose, onSubmit }: any) => {
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleSubmit = async () => {
    if (isSubmitting) return;
    try {
      setIsSubmitting(true);
      await onSubmit();
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div
      className="fixed inset-0 bg-black/40 backdrop-blur-sm flex items-center justify-center z-50"
      onClick={(e) => {
        if (e.target === e.currentTarget) onClose();
      }}
    >
      <div
        className="bg-white w-full max-w-2xl p-10 rounded-3xl shadow-2xl relative"
        onClick={(e) => e.stopPropagation()}
      >
        <button
          onClick={onClose}
          className="absolute top-6 right-6 text-gray-400 hover:text-gray-700 text-xl font-bold"
        >
          ✕
        </button>

        <h3 className="text-xl font-black mb-8">
          리뷰 작성하기
        </h3>

        <div className="space-y-6">

          <StarRating10
            label="만족도"
            value={reviewForm.satisfactionRating}
            onChange={(v: number) =>
              setReviewForm({ ...reviewForm, satisfactionRating: v })
            }
          />

          <StarRating10
            label="난이도"
            value={reviewForm.difficultyRating}
            onChange={(v: number) =>
              setReviewForm({ ...reviewForm, difficultyRating: v })
            }
          />

          <textarea
            className="w-full border rounded-2xl p-4"
            placeholder="경험을 작성해주세요"
            value={reviewForm.experienceComment}
            onChange={(e) =>
              setReviewForm({
                ...reviewForm,
                experienceComment: e.target.value,
              })
            }
          />

          <textarea
            className="w-full border rounded-2xl p-4"
            placeholder="면접 팁 (선택)"
            value={reviewForm.interviewTip}
            onChange={(e) =>
              setReviewForm({
                ...reviewForm,
                interviewTip: e.target.value,
              })
            }
          />

        </div>

        <div className="flex justify-end gap-4 mt-10">
          <button onClick={onClose}>취소</button>

          <button
            onClick={handleSubmit}
            disabled={isSubmitting}
            className="px-8 py-3 bg-[#3FB6B2] text-white rounded-2xl font-bold disabled:opacity-50"
          >
            {isSubmitting ? '등록 중...' : '등록하기'}
          </button>
        </div>
      </div>
    </div>
  );
};

/* ---------------- 별점 입력 ---------------- */

const StarRating10 = ({ value, onChange, label }: any) => (
  <div>
    <div className="text-sm font-bold mb-2">{label}</div>
    <div className="flex gap-1">
      {Array.from({ length: 5 }).map((_, i) => {
        const full = (i + 1) * 2;
        const half = full - 1;

        return (
          <div key={i} className="relative flex">
            <button
              className="absolute left-0 w-1/2 h-full"
              onClick={() => onChange(half)}
            />
            <button
              className="absolute right-0 w-1/2 h-full"
              onClick={() => onChange(full)}
            />
            {value >= full ? (
              <TbStarFilled size={28} className="text-[#3FB6B2]" />
            ) : value === half ? (
              <TbStarHalfFilled size={28} className="text-[#3FB6B2]" />
            ) : (
              <TbStar size={28} className="text-gray-300" />
            )}
          </div>
        );
      })}
      <span className="ml-2 font-bold">{value}점</span>
    </div>
  </div>
);

export default JobSummaryDetailPage;
