import React from 'react';
import { useNavigate } from 'react-router-dom';
import { 
  TbTarget, TbFileSearch, TbChartBar, 
  TbArrowRight, TbCheck, TbClipboardCheck, TbTimeline 
} from 'react-icons/tb';

export const GuestLanding: React.FC = () => {
  const navigate = useNavigate();

  return (
    <div className="min-h-screen bg-[#0f172a] text-white selection:bg-[#89cbb6]/30">
      {/* Hero Section */}
      <section className="relative pt-48 pb-32 overflow-hidden">
        {/* Animated Background Elements */}
        <div className="absolute top-0 left-1/2 -translate-x-1/2 w-full h-full pointer-events-none opacity-20">
          <div className="absolute top-1/4 left-1/4 w-96 h-96 bg-[#276db8] rounded-full blur-[160px] animate-pulse" />
          <div className="absolute bottom-1/4 right-1/4 w-96 h-96 bg-[#89cbb6] rounded-full blur-[160px] animate-pulse delay-1000" />
        </div>

        <div className="max-w-7xl mx-auto px-6 relative z-10 text-center">
          <div className="inline-flex items-center gap-2 px-4 py-2 rounded-full bg-white/5 border border-white/10 mb-8 backdrop-blur-md">
            <span className="w-2 h-2 rounded-full bg-[#89cbb6] animate-pulse" />
            <span className="text-[10px] font-black uppercase tracking-[0.3em] text-[#89cbb6]">데이터 기반 채용 관리 전략</span>
          </div>
          
          <h1 className="text-6xl md:text-8xl font-black mb-10 tracking-tight leading-[0.95] italic">
            기억은 휘발되지만,<br />
            <span className="text-[#89cbb6]">기록은 자산</span>이 됩니다.
          </h1>
          
          <p className="text-xl md:text-2xl text-gray-400 max-w-2xl mx-auto mb-16 font-medium leading-relaxed">
            무의미한 지원의 반복을 멈추세요. <br />
            HireLog는 당신의 채용 과정을 데이터베이스화하여 <br />
            다음 면접의 정답을 미리 준비하게 합니다.
          </p>

          <div className="flex flex-col sm:flex-row items-center justify-center gap-6">
            <button 
              onClick={() => navigate('/login')}
              className="group px-12 py-6 bg-white text-[#0f172a] rounded-2xl font-black text-xl hover:scale-105 transition-all shadow-2xl flex items-center gap-3"
            >
              지금 시작하기
              <TbArrowRight className="group-hover:translate-x-1 transition-transform" />
            </button>
            <button 
              onClick={() => document.getElementById('intro')?.scrollIntoView({ behavior: 'smooth' })}
              className="px-12 py-6 bg-white/5 border border-white/10 rounded-2xl font-bold text-xl hover:bg-white/10 transition-all backdrop-blur-sm"
            >
              서비스 기능 둘러보기
            </button>
          </div>
        </div>
      </section>

      {/* Strategic Features Section */}
      <section id="intro" className="py-32 bg-white/5 border-y border-white/5">
        <div className="max-w-7xl mx-auto px-6">
          <div className="grid grid-cols-1 md:grid-cols-3 gap-12">
            <FeatureCard 
              icon={<TbTarget size={32} />}
              title="오답 노트 전략"
              description="탈락한 면접에서 놓쳤던 질문과 아쉬운 답변을 복기하세요. 같은 실수를 반복하지 않는 것이 합격의 첫 걸음입니다."
            />
            <FeatureCard 
              icon={<TbFileSearch size={32} />}
              title="JD 핵심 추출"
              description="AI가 수많은 채용 공고 속에서 당신이 어필해야 할 핵심 역량만 골라냅니다. 공고 해석에 들이는 시간을 절반으로 줄이세요."
            />
            <FeatureCard 
              icon={<TbTimeline size={32} />}
              title="히스토리 자산화"
              description="과거 유사 기업/직무의 전형 과정을 불러와 면접관의 의도를 미리 파악합니다. 기록이 쌓일수록 당신의 전략은 날카로워집니다."
            />
          </div>
        </div>
      </section>

      {/* Visual Guide / Content Section */}
      <section id="guide" className="py-40">
        <div className="max-max-w-7xl mx-auto px-6">
          <div className="flex flex-col lg:flex-row items-center gap-24">
            <div className="lg:w-1/2 space-y-10">
              <h2 className="text-4xl md:text-5xl font-black tracking-tight leading-tight italic">
                왜 전문가는 <br />
                <span className="text-[#89cbb6]">기록에 집착</span>할까요?
              </h2>
              <div className="space-y-6">
                <StepItem 
                  title="실수 상기 및 개선" 
                  desc="면접장에서의 긴장감 속에서 저질렀던 실수를 냉정하게 기록하고 다음 전형의 체크리스트로 만듭니다."
                />
                <StepItem 
                  title="유사 전형 대비" 
                  desc="A기업에서 받았던 까다로운 기술 질문은 B기업에서도 나올 수 있습니다. 당신의 로그북은 이미 정답을 알고 있습니다."
                />
                <StepItem 
                  title="커리어 히스토리 관리" 
                  desc="단순히 '어디 지원했다'가 아닌, 어떤 고민을 했고 어떻게 대응했는지에 대한 데이터가 당신의 실력이 됩니다."
                />
              </div>
            </div>
            
            <div className="lg:w-1/2 relative">
              <div className="aspect-square bg-gradient-to-tr from-[#276db8] to-[#89cbb6] rounded-[3rem] shadow-2xl rotate-3 relative overflow-hidden group">
                {/* Mockup UI representation */}
                <div className="absolute inset-4 bg-[#0f172a] rounded-[2rem] border border-white/10 p-8 shadow-inner">
                  <div className="w-1/2 h-4 bg-white/10 rounded-full mb-8" />
                  <div className="space-y-4">
                    <div className="w-full h-24 bg-[#89cbb6]/10 border border-[#89cbb6]/20 rounded-2xl flex items-center justify-center">
                      <TbChartBar size={32} className="text-[#89cbb6]" />
                    </div>
                    <div className="grid grid-cols-2 gap-4">
                      <div className="h-20 bg-white/5 rounded-2xl" />
                      <div className="h-20 bg-white/5 rounded-2xl" />
                    </div>
                    <div className="w-full h-12 bg-white/5 rounded-2xl mt-4" />
                  </div>
                </div>
                <div className="absolute -right-8 -bottom-8 w-48 h-48 bg-white/10 rounded-full blur-3xl pointer-events-none group-hover:bg-[#89cbb6]/20 transition-all duration-700" />
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Final CTA */}
      <section className="py-32 bg-white text-[#0f172a] rounded-t-[4rem]">
        <div className="max-w-4xl mx-auto px-6 text-center">
          <h2 className="text-5xl font-black mb-8 italic tracking-tighter uppercase">Ready to Log?</h2>
          <p className="text-xl text-gray-500 mb-12 font-medium">
            당신의 노력이 헛되지 않도록, <br />
            지금 바로 첫 번째 커리어 로그를 남기고 전략적으로 대비하세요.
          </p>
          <button 
            onClick={() => navigate('/login')}
            className="px-16 py-6 bg-[#0f172a] text-white rounded-[2rem] font-black text-2xl hover:scale-105 transition-all shadow-2xl flex items-center justify-center gap-4 mx-auto"
          >
            기록 시작하기
            <TbClipboardCheck size={32} />
          </button>
        </div>
      </section>

      {/* Simple Footer */}
      <footer className="py-20 border-t border-white/5 bg-[#0f172a]">
        <div className="max-w-7xl mx-auto px-6 flex flex-col md:flex-row justify-between items-center gap-8">
          <div className="flex items-center gap-2">
            <div className="w-8 h-8 bg-gradient-to-tr from-[#276db8] to-[#89cbb6] rounded-lg" />
            <span className="text-xl font-black tracking-tighter italic">HireLog</span>
          </div>
          <p className="text-gray-500 text-sm font-bold">© 2026 HireLog. For Strategic Career Management.</p>
          <div className="flex gap-8 text-gray-400 font-bold text-sm">
            <span className="hover:text-white cursor-pointer transition-colors">Privacy</span>
            <span className="hover:text-white cursor-pointer transition-colors">Terms</span>
            <span className="hover:text-white cursor-pointer transition-colors">Help</span>
          </div>
        </div>
      </footer>
    </div>
  );
};

const FeatureCard = ({ icon, title, description }: { icon: any, title: string, description: string }) => (
  <div className="p-10 rounded-[2.5rem] bg-white/5 border border-white/5 hover:border-[#89cbb6]/30 hover:bg-white/[0.07] transition-all group">
    <div className="w-16 h-16 rounded-2xl bg-[#89cbb6]/10 flex items-center justify-center text-[#89cbb6] mb-8 group-hover:scale-110 transition-transform">
      {icon}
    </div>
    <h3 className="text-2xl font-black mb-4 italic">{title}</h3>
    <p className="text-gray-400 font-medium leading-relaxed">{description}</p>
  </div>
);

const StepItem = ({ title, desc }: { title: string, desc: string }) => (
  <div className="flex gap-6">
    <div className="shrink-0 w-8 h-8 rounded-full bg-[#89cbb6] flex items-center justify-center text-[#0f172a]">
      <TbCheck size={18} strokeWidth={4} />
    </div>
    <div>
      <h4 className="text-xl font-black mb-2 italic">{title}</h4>
      <p className="text-gray-400 font-medium leading-relaxed">{desc}</p>
    </div>
  </div>
);
