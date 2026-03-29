import { useNavigate } from 'react-router-dom';
import { TbArrowRight, TbCheck } from 'react-icons/tb';

const ServiceIntroPage = () => {
  const navigate = useNavigate();

  return (
    <div className="min-h-screen bg-[#f4f7fb] px-6 pb-24 pt-28">
      <div className="mx-auto max-w-6xl">
        <section className="mb-10 overflow-hidden rounded-3xl bg-[#0f172a] p-10 text-white md:p-14">
          <p className="mb-4 text-xs font-bold uppercase tracking-[0.3em] text-[#8de6df]">Service Intro</p>
          <h1 className="mb-6 text-4xl font-black leading-tight md:text-5xl">HireLog는 채용 과정을 데이터로 남기는 개인 커리어 로그북입니다.</h1>
          <p className="max-w-3xl text-sm leading-relaxed text-gray-200 md:text-base">
            채용 공고를 읽고 끝내지 않고, 지원 준비와 회고까지 이어지는 흐름을 구조화해
            다음 지원에서 바로 활용할 수 있는 실행 가능한 기록으로 바꿔드립니다.
          </p>
        </section>

        <section className="mb-10 grid gap-6 md:grid-cols-3">
          <Card
            title="1. JD 이해를 빠르게"
            description="공고의 핵심 요건과 준비 포인트를 정리해 어떤 역량을 우선 준비해야 할지 빠르게 파악할 수 있습니다."
          />
          <Card
            title="2. 단계별 준비 기록"
            description="서류, 코딩테스트, 면접 단계별로 메모를 남겨 실패/성공 패턴을 누적하고 재사용할 수 있습니다."
          />
          <Card
            title="3. 회고를 전략으로"
            description="후기와 피드백을 모아 다음 지원의 체크리스트와 답변 전략으로 연결할 수 있습니다."
          />
        </section>

        <section className="mb-10 rounded-3xl border border-gray-100 bg-white p-8 md:p-10">
          <h2 className="mb-5 text-2xl font-black text-gray-900">이런 분께 추천해요</h2>
          <ul className="grid gap-3 text-sm text-gray-700 md:grid-cols-2">
            <li className="flex items-center gap-2"><TbCheck className="text-[#3FB6B2]" /> 지원할 때마다 준비 내용을 다시 정리하느라 시간이 많이 드는 분</li>
            <li className="flex items-center gap-2"><TbCheck className="text-[#3FB6B2]" /> 코딩테스트/면접 후 복기를 체계적으로 남기고 싶은 분</li>
            <li className="flex items-center gap-2"><TbCheck className="text-[#3FB6B2]" /> JD 분석과 실제 준비 사이의 연결이 약하다고 느끼는 분</li>
            <li className="flex items-center gap-2"><TbCheck className="text-[#3FB6B2]" /> 취업 준비 과정을 나만의 데이터 자산으로 만들고 싶은 분</li>
          </ul>
        </section>

        <section className="rounded-3xl border border-[#3FB6B2]/20 bg-[#3FB6B2]/5 p-8 text-center md:p-10">
          <h3 className="mb-3 text-2xl font-black text-gray-900">이제 기록을 남기는 것에서 끝내지 마세요.</h3>
          <p className="mb-6 text-sm text-gray-600">HireLog에서 다음 합격을 위한 실행 기록을 쌓아보세요.</p>
          <div className="flex flex-wrap items-center justify-center gap-3">
            <button
              onClick={() => navigate('/jd')}
              className="inline-flex items-center gap-2 rounded-xl bg-[#0f172a] px-5 py-3 text-sm font-semibold text-white"
            >
              JD 둘러보기 <TbArrowRight size={16} />
            </button>
            <button
              onClick={() => navigate('/')}
              className="rounded-xl border border-gray-200 bg-white px-5 py-3 text-sm font-semibold text-gray-700"
            >
              홈으로 돌아가기
            </button>
          </div>
        </section>
      </div>
    </div>
  );
};

function Card({ title, description }: { title: string; description: string }) {
  return (
    <article className="rounded-2xl border border-gray-100 bg-white p-6 shadow-sm">
      <h2 className="mb-3 text-xl font-bold text-gray-900">{title}</h2>
      <p className="text-sm leading-relaxed text-gray-600">{description}</p>
    </article>
  );
}

export default ServiceIntroPage;
