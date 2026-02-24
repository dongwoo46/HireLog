import { useState } from 'react';
import { useSummaryJobDescription } from '../hooks/useSummaryJobDescription';
import { Link } from 'react-router-dom';
import { FaChevronLeft } from 'react-icons/fa';

export default function JdRegistrationPage() {
  const [brandName, setBrandName] = useState('');
  const [positionName, setPositionName] = useState('');
  const [jdUrl, setJdUrl] = useState('');
  const [jdImage, setJdImage] = useState<File | null>(null);
  const [jdText, setJdText] = useState('');

  const summaryMutation = useSummaryJobDescription();

  const handleRegister = () => {
    // Basic validation
    if (!brandName.trim() || !positionName.trim() || !jdText.trim()) {
      alert("기업명, 포지션, 공고본문은 필수 입력입니다.");
      return;
    }
    // Logic to handle image and url would go here, updating mutation as needed
    // For now passing existing fields + new ones if the hook supports it, or just preserving existing behavior for the demo
    // summaryMutation.mutate({ brandName, positionName, jdText, jdUrl, jdImage });
    // Assuming the hook only accepts the original structure for now.
    summaryMutation.mutate({ brandName, positionName, jdText });
  };

  const handleImageChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      setJdImage(e.target.files[0]);
    }
  };

  return (
    <div className="min-h-screen bg-white font-sans text-gray-900">
      {/* Header removed - using global Header */}

      {/* Main Content */}
      <main className="max-w-4xl mx-auto px-6 py-12">

        {/* Title Section */}
        <div className="text-center mb-16 relative">
          <h1 className="text-3xl font-bold mb-4 flex justify-center items-center gap-2">
            <span className="text-teal-400">JD</span>
            <span>등록</span>
          </h1>
          <p className="text-gray-500 text-sm">
            분석할 채용 공고의 상세 정보를 입력해 주세요.<br />
            등록 즉시 AI가 핵심 정보를 요약해 드립니다.
          </p>

          {/* Back Button positioned absolutely to the right of the title block roughly */}
          <div className="absolute top-0 right-0 hidden md:block">
            <Link to="/" className="flex items-center text-gray-500 hover:text-gray-800 text-sm font-medium">
              <FaChevronLeft className="mr-1 w-3 h-3" />
              최근 등록 이력
            </Link>
          </div>
        </div>

        {/* Form */}
        <div className="space-y-12">

          {/* Row 1: Company & Position */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-12">
            <div className="space-y-2">
              <label className="block text-xs font-bold text-gray-500">기업명</label>
              <input
                type="text"
                value={brandName}
                onChange={(e) => setBrandName(e.target.value)}
                placeholder="예: 토스"
                className="w-full border-b border-gray-300 py-2 focus:border-teal-400 outline-none transition-colors placeholder-gray-300 text-lg"
              />
            </div>
            <div className="space-y-2">
              <label className="block text-xs font-bold text-gray-500">포지션</label>
              <input
                type="text"
                value={positionName}
                onChange={(e) => setPositionName(e.target.value)}
                placeholder="예: 백엔드 개발자"
                className="w-full border-b border-gray-300 py-2 focus:border-teal-400 outline-none transition-colors placeholder-gray-300 text-lg"
              />
            </div>
          </div>

          {/* Row 2: URL & Image */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-12">
            <div className="space-y-2">
              <label className="block text-xs font-bold text-gray-500">공고 URL</label>
              <input
                type="text"
                value={jdUrl}
                onChange={(e) => setJdUrl(e.target.value)}
                placeholder="https://..."
                className="w-full border-b border-gray-300 py-2 focus:border-teal-400 outline-none transition-colors placeholder-gray-300 text-lg"
              />
            </div>
            <div className="space-y-2">
              <label className="block text-xs font-bold text-gray-500">이미지 첨부</label>
              <div className="flex items-center pt-2">
                <label className="cursor-pointer w-full flex items-center justify-between border-b border-gray-300 pb-2 hover:border-teal-400 transition-colors">
                  <span className={`text-lg truncate ${jdImage ? 'text-gray-900' : 'text-gray-300'}`}>
                    {jdImage ? jdImage.name : '이미지 파일을 선택하세요'}
                  </span>
                  <input
                    type="file"
                    accept="image/*"
                    onChange={handleImageChange}
                    className="hidden"
                  />
                  {/* Simple text or icon indicating action */}
                </label>
              </div>
            </div>
          </div>

          {/* Row 3: Textarea */}
          <div className="space-y-2">
            <label className="block text-xs font-bold text-gray-500">공고문문</label>
            <div className="border border-gray-300 rounded-lg p-4 focus-within:border-teal-400 transition-colors">
              <textarea
                value={jdText}
                onChange={(e) => setJdText(e.target.value)}
                placeholder="JD 본문을 여기에 붙여넣으세요."
                className="w-full h-80 outline-none resize-none placeholder-gray-300 text-base"
              />
            </div>
          </div>

          {/* Submit Button */}
          <div className="flex justify-center pt-8 pb-20">
            <button
              onClick={handleRegister}
              disabled={summaryMutation.isPending}
              className="px-10 py-3 rounded-full border border-teal-500 text-teal-600 font-bold hover:bg-teal-50 transition-colors disabled:opacity-50 disabled:cursor-not-allowed shadow-sm"
            >
              {summaryMutation.isPending ? '분석 중...' : 'JD 등록하기'}
            </button>
          </div>
        </div>
      </main>
    </div>
  );
}
