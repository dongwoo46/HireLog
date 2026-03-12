import { useNavigate } from 'react-router-dom';
import {
  TbTarget,
  TbFileSearch,
  TbArrowRight,
  TbClipboardCheck,
  TbSparkles,
  TbChartBar,
} from 'react-icons/tb';
import { Button } from './common/Button';

export const GuestLanding = () => {
  const navigate = useNavigate();

  return (
    <div className="min-h-screen bg-[#0A0F1E] text-white selection:bg-[#3FB6B2]/30 selection:text-white">
      {/* Dynamic Background Blurs */}
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="absolute top-[-10%] left-[-10%] w-[50%] h-[50%] bg-[#3FB6B2]/10 blur-[120px] rounded-full animate-pulse" />
        <div className="absolute bottom-[-10%] right-[-10%] w-[40%] h-[40%] bg-[#276db8]/10 blur-[120px] rounded-full animate-pulse delay-700" />
      </div>

      {/* Hero Section */}
      <section className="relative pt-48 pb-32 text-center overflow-hidden border-b border-white/5">
        <div className="relative z-10 max-w-5xl mx-auto px-6">
          <div className="inline-flex items-center gap-2 px-4 py-2 rounded-full bg-white/5 border border-white/10 text-[#3FB6B2] text-xs font-bold tracking-widest uppercase mb-8 animate-fade-in">
            <TbSparkles className="animate-spin-slow" />
            Empowering Your Career Journey
          </div>

          <h1 className="text-6xl md:text-8xl font-black mb-10 leading-[0.95] tracking-tight">
            기억은 휘발되지만,<br />
            <span className="text-transparent bg-clip-text bg-gradient-to-r from-[#3FB6B2] to-[#276db8]">기록은 자산</span>입니다
          </h1>

          <p className="text-xl md:text-2xl text-gray-400 mb-16 max-w-2xl mx-auto leading-relaxed">
            HireLog는 당신의 채용 과정을 정교하게 기록하고 <br className="hidden md:block" />
            다음 합격을 향한 데이터 기반의 전략으로 승화시킵니다.
          </p>

          <div className="flex flex-col sm:flex-row gap-6 justify-center items-center">
            <Button
              variant="secondary"
              size="lg"
              onClick={() => navigate('/login')}
              className="px-12 py-7 text-xl shadow-xl shadow-[#3FB6B2]/20 hover:scale-105 transition-transform"
            >
              지금 시작하시겠습니까?
              <TbArrowRight className="ml-3" size={24} />
            </Button>

            <button
              onClick={() =>
                document
                  .getElementById('intro')
                  ?.scrollIntoView({ behavior: 'smooth' })
              }
              className="px-12 py-7 text-lg font-bold text-white/60 hover:text-white transition-colors flex items-center gap-2 group"
            >
              서비스 핵심 가치 알아보기
              <div className="w-8 h-[1px] bg-white/20 group-hover:w-12 group-hover:bg-[#3FB6B2] transition-all" />
            </button>
          </div>
        </div>

        {/* Floating Decorative Elements */}
        <div className="absolute top-1/2 left-10 w-24 h-24 border border-white/5 rounded-full animate-bounce-slow" />
        <div className="absolute bottom-20 right-20 w-16 h-16 border border-[#3FB6B2]/10 rounded-xl rotate-12 animate-float" />
      </section>

      {/* Features Grid */}
      <section id="intro" className="py-32 relative z-10">
        <div className="max-w-7xl mx-auto px-6">
          <div className="text-center mb-24">
            <h2 className="text-4xl font-black italic mb-4 uppercase">Core Philosophy</h2>
            <div className="h-1.5 w-24 bg-gradient-to-r from-[#3FB6B2] to-transparent mx-auto rounded-full" />
          </div>

          <div className="grid md:grid-cols-3 gap-8">
            <Feature
              icon={<TbTarget size={36} />}
              title="데이터 기반 오답 노트"
              desc="탈락의 고배를 성공의 밑거름으로. 원인을 분석하고 빈틈을 메우는 전략적 기록을 제공합니다."
            />
            <Feature
              icon={<TbFileSearch size={36} />}
              title="AI JD 인텔리전스"
              desc="방대한 채용 공고 속에서 당신이 꼭 알아야 할 핵심 역량과 키워드만을 정교하게 추출합니다."
            />
            <Feature
              icon={<TbChartBar size={36} />}
              title="커리어 히스토리 자산화"
              desc="기록이 누적될수록 당신의 커리어 전략은 더 정교해지고, 다음 합격 확률은 비약적으로 높아집니다."
            />
          </div>
        </div>
      </section>

      {/* Final Call to Action */}
      <section className="py-40 bg-white text-[#0A0F1E] text-center rounded-t-[5rem] shadow-[0_-20px_50px_rgba(0,0,0,0.1)] relative z-10">
        <div className="max-w-4xl mx-auto px-6">
          <h2 className="text-5xl md:text-7xl font-black mb-10 leading-[0.9] italic uppercase tracking-tighter">
            Build Your <br />
            Success Archive
          </h2>
          <p className="text-xl text-gray-500 mb-14 font-medium max-w-lg mx-auto leading-relaxed">
            더 이상 운에 맡기지 마세요. <br />
            실패를 기록하고 성공을 설계하는 가장 스마트한 방법.
          </p>

          <Button
            variant="primary"
            size="lg"
            onClick={() => navigate('/login')}
            className="px-20 py-8 text-2xl gap-4 rounded-[2rem] hover:scale-105 active:scale-95 transition-all shadow-2xl shadow-[#3FB6B2]/40 group"
          >
            HireLog 지금 시작하기
            <TbClipboardCheck size={32} className="group-hover:rotate-12 transition-transform" />
          </Button>

          <div className="mt-16 pt-16 border-t border-gray-100 grid grid-cols-2 md:grid-cols-4 gap-8">
            <Stat label="Total Logs" value="12,540+" />
            <Stat label="Active Dreamers" value="3,200+" />
            <Stat label="Success Rate" value="78%" />
            <Stat label="User Rating" value="4.9/5" />
          </div>
        </div>
      </section>

      <footer className="bg-white py-12 text-center text-sm text-gray-400 font-medium">
        © 2026 HireLog. Precision 기록, Powerful 성장.
      </footer>

      {/* Custom Styles for Animations */}
      <style dangerouslySetInnerHTML={{
        __html: `
        @keyframes fade-in {
          from { opacity: 0; transform: translateY(10px); }
          to { opacity: 1; transform: translateY(0); }
        }
        @keyframes float {
          0%, 100% { transform: translate(0, 0) rotate(12deg); }
          50% { transform: translate(-10px, -20px) rotate(15deg); }
        }
        @keyframes bounce-slow {
          0%, 100% { transform: translateY(0); }
          50% { transform: translateY(-30px); }
        }
        .animate-fade-in { animation: fade-in 0.8s ease-out forwards; }
        .animate-spin-slow { animation: spin 4s linear infinite; }
        .animate-float { animation: float 6s ease-in-out infinite; }
        .animate-bounce-slow { animation: bounce-slow 4s ease-in-out infinite; }
      `}} />
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
  <div className="group p-12 rounded-[3.5rem] bg-white/5 border border-white/5 hover:bg-white/10 hover:border-[#3FB6B2]/30 transition-all duration-500 hover:-translate-y-2">
    <div className="w-20 h-20 rounded-3xl bg-[#3FB6B2]/5 flex items-center justify-center text-[#3FB6B2] mb-10 group-hover:scale-110 transition-transform duration-500 shadow-inner">
      {icon}
    </div>
    <h3 className="text-2xl font-black italic mb-6 tracking-tight">{title}</h3>
    <p className="text-gray-400 text-lg leading-relaxed">{desc}</p>
  </div>
);

const Stat = ({ label, value }: { label: string; value: string }) => (
  <div className="flex flex-col gap-1">
    <div className="text-3xl font-black text-[#0A0F1E] tracking-tighter italic">{value}</div>
    <div className="text-xs text-gray-400 font-bold uppercase tracking-widest">{label}</div>
  </div>
);
