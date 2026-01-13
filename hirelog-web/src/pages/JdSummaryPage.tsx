import { useState } from 'react';
import { useSummaryJobDescription } from '../hooks/useSummaryJobDescription';
import SectionCard from '../components/SectionCard';

export default function JdSummaryPage() {
  const [jdText, setJdText] = useState('');
  const summaryMutation = useSummaryJobDescription();

  const handleSummary = () => {
    if (!jdText.trim()) return;
    summaryMutation.mutate(jdText);
  };

  const result = summaryMutation.data ?? null;

  return (
    <div className="min-h-screen bg-gray-100 p-6">
      <div className="mx-auto max-w-5xl space-y-6">
        <h1 className="text-2xl font-bold">Hirelog · JD Summary (Internal)</h1>

        {/* JD 입력 */}
        <textarea
          className="h-64 w-full resize-none rounded border border-gray-300 p-4
                     font-mono text-sm leading-relaxed
                     focus:outline-none focus:ring-2 focus:ring-blue-500"
          placeholder="Job Description 원문을 그대로 붙여 넣으세요"
          value={jdText}
          onChange={(e) => setJdText(e.target.value)}
        />

        {/* 액션 */}
        <div className="flex items-center gap-3">
          <button
            onClick={handleSummary}
            disabled={summaryMutation.isPending || !jdText.trim()}
            className="rounded bg-blue-600 px-5 py-2 text-white
                       disabled:cursor-not-allowed disabled:opacity-50"
          >
            {summaryMutation.isPending ? 'Analyzing...' : 'Summarize'}
          </button>

          {summaryMutation.isError && (
            <span className="text-red-600">분석 중 오류가 발생했습니다</span>
          )}
        </div>

        {/* 결과 */}
        {!result && (
          <div className="rounded border border-dashed border-gray-300 bg-white p-6 text-gray-500">
            분석 결과가 여기에 표시됩니다
          </div>
        )}

        {result && (
          <div className="space-y-5">
            <SectionCard title="요약">{result.summary}</SectionCard>

            <SectionCard title="주요 업무">
              {result.responsibilities}
            </SectionCard>

            <SectionCard title="필수 자격 요건">
              {result.requiredQualifications}
            </SectionCard>

            {result.preferredQualifications && (
              <SectionCard title="우대 사항">
                {result.preferredQualifications}
              </SectionCard>
            )}

            {result.techStack && (
              <SectionCard title="기술 스택">{result.techStack}</SectionCard>
            )}

            {result.recruitmentProcess && (
              <SectionCard title="채용 절차">
                {result.recruitmentProcess}
              </SectionCard>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
