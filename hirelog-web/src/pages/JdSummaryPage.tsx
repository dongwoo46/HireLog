import { useState } from 'react';
import { useSummaryJobDescription } from '../hooks/useSummaryJobDescription';

export default function JdRegistrationPage() {
  const [brandName, setBrandName] = useState('');
  const [positionName, setPositionName] = useState('');
  const [jdText, setJdText] = useState('');

  const summaryMutation = useSummaryJobDescription();

  const recentJds = [
    { id: 1, brand: '토스', position: 'Backend Engineer', date: '2024.03.14' },
    {
      id: 2,
      brand: '강남언니',
      position: 'Product Designer',
      date: '2024.03.13',
    },
    { id: 3, brand: '라인', position: 'ML Engineer', date: '2024.03.12' },
  ];

  const handleRegister = () => {
    if (!brandName.trim() || !positionName.trim() || !jdText.trim()) return;
    summaryMutation.mutate({ brandName, positionName, jdText });
  };

  return (
    <div className="min-h-screen bg-white font-sans text-[#111827] antialiased">
      {/* Header: 메인 컬러를 딥 블루(#1E40AF)로 변경 */}
      <header className="fixed top-0 z-50 w-full border-b border-gray-100 bg-white/95 backdrop-blur-md">
        <div className="mx-auto flex h-16 w-full items-center justify-between px-10">
          <div className="text-xl font-black tracking-tighter text-[#1E40AF]">
            HireLog
          </div>

          <nav className="flex gap-12 text-[14px] font-bold text-[#64748B]">
            <button className="relative text-[#1E40AF] transition-colors after:absolute after:-bottom-[22px] after:left-0 after:h-[3px] after:w-full after:bg-[#1E40AF]">
              JD 등록
            </button>
            <button className="hover:text-[#1E40AF] transition-colors">
              JD 조회
            </button>
            <button className="hover:text-[#1E40AF] transition-colors">
              통계
            </button>
          </nav>

          <div className="flex items-center gap-3 text-[13px] font-bold text-[#64748B]">
            <span>Admin</span>
            <div className="h-8 w-8 rounded-full bg-[#1E40AF] flex items-center justify-center text-[10px] text-white font-black shadow-inner">
              AD
            </div>
          </div>
        </div>
      </header>

      <main className="flex min-h-screen pt-16">
        {/* 왼쪽: JD 등록 폼 */}
        <section className="w-3/5 border-r border-gray-500/10 px-12 pt-24 pb-32">
          <div className="mx-auto max-w-[600px]">
            <header className="mb-16">
              <h1 className="text-3xl font-black tracking-tight text-[#1E3A8A] mb-4">
                JD 등록
              </h1>
              <p className="text-[#64748B] text-[16px] leading-relaxed font-medium">
                분석할 채용 공고의 상세 정보를 입력해 주세요. <br />
                등록 즉시 AI가 핵심 정보를 요약해 드립니다.
              </p>
            </header>

            <div className="space-y-12">
              <div className="grid grid-cols-2 gap-8">
                <div className="group space-y-2">
                  <label className="text-[12px] font-bold text-[#64748B] group-focus-within:text-[#1E40AF] transition-colors">
                    기업명
                  </label>
                  <input
                    type="text"
                    placeholder="예: 토스"
                    value={brandName}
                    onChange={(e) => setBrandName(e.target.value)}
                    className="w-full border-b-2 border-gray-100 py-2 text-[17px] font-medium outline-none focus:border-[#1E40AF] transition-all placeholder:text-gray-200"
                  />
                </div>
                <div className="group space-y-2">
                  <label className="text-[12px] font-bold text-[#64748B] group-focus-within:text-[#1E40AF] transition-colors">
                    포지션
                  </label>
                  <input
                    type="text"
                    placeholder="예: 백엔드 개발자"
                    value={positionName}
                    onChange={(e) => setPositionName(e.target.value)}
                    className="w-full border-b-2 border-gray-100 py-2 text-[17px] font-medium outline-none focus:border-[#1E40AF] transition-all placeholder:text-gray-200"
                  />
                </div>
              </div>

              <div className="space-y-3">
                <label className="text-[12px] font-bold text-[#64748B]">
                  공고 원문
                </label>
                <div className="rounded-xl bg-[#F8FAFC] p-1 border border-gray-100 focus-within:bg-white focus-within:ring-4 focus-within:ring-[#1E40AF]/5 focus-within:border-[#1E40AF] transition-all">
                  <textarea
                    placeholder="JD 본문을 여기에 붙여넣으세요."
                    value={jdText}
                    onChange={(e) => setJdText(e.target.value)}
                    className="min-h-[400px] w-full resize-none border-none bg-transparent p-5 text-[15px] leading-relaxed text-[#334155] outline-none"
                  />
                </div>
              </div>

              {/* [메인 버튼] 어두운 파란색(#1E40AF) 적용 */}
              <button
                onClick={handleRegister}
                disabled={
                  summaryMutation.isPending ||
                  !brandName ||
                  !positionName ||
                  !jdText
                }
                className="h-16 w-full rounded-xl bg-[#1E40AF] text-[16px] font-bold text-white shadow-lg shadow-blue-900/20 
                           transition-all duration-300
                           hover:bg-[#1E3A8A] hover:translate-y-[-2px] hover:shadow-2xl
                           active:scale-[0.98] 
                           disabled:bg-gray-100 disabled:text-gray-400 disabled:shadow-none"
              >
                {summaryMutation.isPending
                  ? '분석 중...'
                  : 'JD 등록 및 리포트 생성'}
              </button>
            </div>
          </div>
        </section>

        {/* 오른쪽: 등록 이력 요약 */}
        <section className="w-2/5 bg-[#F5F8FF] px-10 pt-24">
          <div className="sticky top-40">
            <header className="mb-8 flex items-center justify-between">
              <h2 className="text-[14px] font-black uppercase tracking-widest text-[#1E40AF]">
                최근 등록 이력
              </h2>
              <span className="text-[11px] font-bold text-[#1E40AF] bg-white px-3 py-1 rounded-full border border-blue-100 shadow-sm">
                Total 128
              </span>
            </header>

            <div className="space-y-4">
              {recentJds.map((jd) => (
                <div
                  key={jd.id}
                  className="group relative overflow-hidden rounded-xl border border-gray-100 bg-white p-5 transition-all hover:border-[#1E40AF]/40 hover:shadow-xl hover:shadow-blue-900/5 cursor-pointer"
                >
                  <div className="flex justify-between items-start mb-1">
                    <span className="text-[13px] font-bold text-[#111827]">
                      {jd.brand}
                    </span>
                    <span className="text-[11px] font-medium text-[#94A3B8]">
                      {jd.date}
                    </span>
                  </div>
                  <p className="text-[14px] text-[#64748B] font-semibold">
                    {jd.position}
                  </p>
                  <div className="mt-3 flex items-center gap-2">
                    <span className="h-1.5 w-1.5 rounded-full bg-[#1E40AF]"></span>
                    <span className="text-[10px] font-bold text-[#1E40AF] uppercase tracking-tighter opacity-80">
                      Analysis Complete
                    </span>
                  </div>
                  <div className="absolute right-4 bottom-5 opacity-0 transition-all group-hover:opacity-100 group-hover:translate-x-1 text-[#1E40AF]">
                    <svg
                      className="w-5 h-5"
                      fill="none"
                      stroke="currentColor"
                      viewBox="0 0 24 24"
                    >
                      <path
                        strokeLinecap="round"
                        strokeLinejoin="round"
                        strokeWidth="2.5"
                        d="M17 8l4 4m0 0l-4 4m4-4H3"
                      ></path>
                    </svg>
                  </div>
                </div>
              ))}
            </div>

            <button className="mt-8 w-full py-4 text-[13px] font-bold text-[#1E40AF] hover:bg-blue-50 transition-all border border-dashed border-blue-200 rounded-xl">
              전체 등록 내역 보기
            </button>
          </div>
        </section>
      </main>
    </div>
  );
}
