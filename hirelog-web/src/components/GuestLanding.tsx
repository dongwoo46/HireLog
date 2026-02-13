import { useNavigate } from 'react-router-dom';
import {
  TbTarget,
  TbFileSearch,
  TbTimeline,
  TbArrowRight,
  TbClipboardCheck,
  TbCheck,
} from 'react-icons/tb';
import { Button } from './common/Button';

export const GuestLanding = () => {
  const navigate = useNavigate();

  return (
    <div className="min-h-screen bg-[#0f172a] text-white">
      {/* Hero */}
      <section className="pt-48 pb-32 text-center relative overflow-hidden">
        <div className="absolute inset-0 opacity-20">
          <div className="absolute top-1/4 left-1/4 w-96 h-96 bg-[#276db8] blur-[160px] rounded-full" />
          <div className="absolute bottom-1/4 right-1/4 w-96 h-96 bg-[#89cbb6] blur-[160px] rounded-full" />
        </div>

        <div className="relative z-10 max-w-4xl mx-auto px-6">
          <h1 className="text-6xl md:text-8xl font-black italic mb-10 leading-[0.95]">
            기억은 휘발되지만,<br />
            <span className="text-[#89cbb6]">기록은 자산</span>입니다
          </h1>

          <p className="text-xl text-gray-400 mb-16">
            HireLog는 당신의 채용 과정을 기록하고 <br />
            다음 합격을 위한 전략으로 바꿉니다.
          </p>

          <div className="flex flex-col sm:flex-row gap-6 justify-center">
            <Button
              variant="secondary"
              size="lg"
              onClick={() => navigate('/login')}
              className="px-12 py-6 text-xl"
            >
              지금 시작하기
              <TbArrowRight className="ml-2" />
            </Button>

            <Button
              variant="ghost"
              size="lg"
              onClick={() =>
                document
                  .getElementById('intro')
                  ?.scrollIntoView({ behavior: 'smooth' })
              }
              className="px-12 py-6 text-xl border border-white/10"
            >
              서비스 기능 보기
            </Button>
          </div>
        </div>
      </section>

      {/* Features */}
      <section id="intro" className="py-32 bg-white/5">
        <div className="max-w-7xl mx-auto px-6 grid md:grid-cols-3 gap-12">
          <Feature
            icon={<TbTarget size={32} />}
            title="오답 노트 전략"
            desc="탈락 원인을 기록하고 같은 실수를 반복하지 않습니다."
          />
          <Feature
            icon={<TbFileSearch size={32} />}
            title="JD 핵심 추출"
            desc="AI가 채용 공고의 핵심만 정리합니다."
          />
          <Feature
            icon={<TbTimeline size={32} />}
            title="히스토리 자산화"
            desc="기록이 쌓일수록 전략은 날카로워집니다."
          />
        </div>
      </section>

      {/* CTA */}
      <section className="py-32 bg-white text-[#0f172a] text-center rounded-t-[4rem]">
        <h2 className="text-5xl font-black mb-8 uppercase italic">
          Ready to Log?
        </h2>
        <p className="text-xl text-gray-500 mb-12">
          지금 바로 첫 커리어 로그를 남겨보세요.
        </p>

        <Button
          variant="primary"
          size="lg"
          onClick={() => navigate('/login')}
          className="px-16 py-6 text-2xl gap-4"
        >
          기록 시작하기
          <TbClipboardCheck size={32} />
        </Button>
      </section>
    </div>
  );
};

const Feature = ({
  icon,
  title,
  desc,
}: {
  icon: React.ReactNode;
  title: string;
  desc: string;
}) => (
  <div className="p-10 rounded-[2.5rem] bg-white/5 border border-white/5 hover:border-[#89cbb6]/30">
    <div className="w-16 h-16 rounded-2xl bg-[#89cbb6]/10 flex items-center justify-center text-[#89cbb6] mb-8">
      {icon}
    </div>
    <h3 className="text-2xl font-black italic mb-4">{title}</h3>
    <p className="text-gray-400">{desc}</p>
  </div>
);
