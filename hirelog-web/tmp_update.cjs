const fs = require('fs');

try {
  const path = 'c:/Users/dw/Desktop/HireLog/hirelog-web/src/pages/JobSummaryDetailPage.tsx';
  let content = fs.readFileSync(path, 'utf8');

  // Replace imports safely
  if (content.includes("import { TbChevronLeft, TbDeviceFloppy, TbReload } from 'react-icons/tb';")) {
    content = content.replace("import { TbChevronLeft, TbDeviceFloppy, TbReload } from 'react-icons/tb';", `import type { ElementType } from 'react';\nimport { TbChevronLeft, TbDeviceFloppy, TbReload, TbNotes, TbBriefcase, TbUserCheck, TbStar, TbBulb, TbDiscountCheck, TbMessages } from 'react-icons/tb';`);
  } else if (!content.includes('TbNotes')) {
    // maybe it's partially there or different format
    console.log("Could not find import statement to replace in the exact expected format.");
  }

  const oldText = `const DetailSection = ({ jd }: { jd: JobSummaryDetailView }) => (
  <div className="space-y-5">
    <Block title="요약" content={jd.summaryText} />
    <Block title="주요 업무" content={jd.responsibilities} />
    <Block title="자격 요건" content={jd.requiredQualifications} />
    <Block title="우대 사항" content={jd.preferredQualifications} />
    <Block title="준비 포인트" content={jd.preparationFocus} />
    <Block title="증명 포인트" content={jd.proofPointsAndMetrics} />
    <Block title="면접 질문" content={jd.questionsToAsk} />
  </div>
);

const Block = ({ title, content }: { title: string; content?: string | null }) =>
  content ? (
    <div className="rounded-2xl border bg-white p-6">
      <div className="mb-2 font-bold text-[#3FB6B2]">{title}</div>
      <div className="whitespace-pre-line text-sm text-gray-700">{content}</div>
    </div>
  ) : null;`;

  const escapeRegExp = (string) => string.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  const tokens = oldText.split(/\s+/);
  const regexPattern = tokens.map(escapeRegExp).join('\\s+');
  const regex = new RegExp(regexPattern);

  const newBlock = `const DetailSection = ({ jd }: { jd: JobSummaryDetailView }) => (
  <div className="grid gap-6 md:grid-cols-2">
    <div className="col-span-1 md:col-span-2">
      <Block title="요약" content={jd.summaryText} icon={TbNotes} delay={0} />
    </div>
    <div className="col-span-1 md:col-span-2">
      <Block title="주요 업무" content={jd.responsibilities} icon={TbBriefcase} delay={100} />
    </div>
    <div className="col-span-1">
      <Block title="자격 요건" content={jd.requiredQualifications} icon={TbUserCheck} delay={200} />
    </div>
    <div className="col-span-1">
      <Block title="우대 사항" content={jd.preferredQualifications} icon={TbStar} delay={300} />
    </div>
    <div className="col-span-1">
      <Block title="준비 포인트" content={jd.preparationFocus} icon={TbBulb} delay={400} />
    </div>
    <div className="col-span-1">
      <Block title="증명 포인트" content={jd.proofPointsAndMetrics} icon={TbDiscountCheck} delay={500} />
    </div>
    <div className="col-span-1 md:col-span-2">
      <Block title="면접 질문" content={jd.questionsToAsk} icon={TbMessages} delay={600} />
    </div>
  </div>
);

const highlightKeywords = (text: string) => {
  const keywords = [
    'Python', 'TypeScript', 'JavaScript', 'Java', 'Spring', 'React', 'Vue', 'Node.js', 'Go', 'C\\\\+\\\\+', 
    'AWS', 'GCP', 'Docker', 'Kubernetes', 'CI\\\\/CD', 'Git', 'SQL', 'NoSQL', 'RDBMS', 'RESTful', 'API', 
    'AI', '데이터', '플랫폼', '인프라', '파이프라인', '백엔드', '프론트엔드', '풀스택', '아키텍처',
    '글로벌'
  ];
  const splitRegex = new RegExp('(' + keywords.join('|') + ')', 'gi');
  const matchRegex = new RegExp('^(' + keywords.join('|') + ')$', 'i');
  
  const parts = text.split(splitRegex);
  
  return (
    <>
      {parts.map((part, i) => {
        if (matchRegex.test(part)) {
          return (
            <span key={i} className="inline-block px-1.5 py-0.5 mx-0.5 -my-0.5 align-baseline text-[13px] font-extrabold text-[#3FB6B2] bg-[#3FB6B2]/10 rounded-md shadow-sm ring-1 ring-[#3FB6B2]/20">
              {part}
            </span>
          );
        }
        return part;
      })}
    </>
  );
};

const Block = ({
  title,
  content,
  icon: Icon,
  delay = 0,
}: {
  title: string;
  content?: string | null;
  icon?: ElementType;
  delay?: number;
}) => {
  if (!content) return null;

  const lines = content.split('\\n').filter((line) => line.trim() !== '');

  return (
    <div 
      className="group flex flex-col h-full rounded-3xl border border-gray-100 bg-white p-7 shadow-[0_4px_20px_-4px_rgba(0,0,0,0.05)] transition-all duration-300 hover:-translate-y-1 hover:border-[#3FB6B2]/40 hover:shadow-[0_8px_30px_-4px_rgba(63,182,178,0.15)] overflow-hidden relative"
      style={{ animationDelay: \`\${delay}ms\`, animationFillMode: 'both', animationName: 'fadeUp', animationDuration: '0.6s' }}
    >
      <div className="absolute top-0 left-0 w-full h-1 bg-gradient-to-r from-[#3FB6B2] to-[#6EC8A7] opacity-0 group-hover:opacity-100 transition-opacity duration-300"></div>
      
      <div className="mb-5 flex items-center gap-3 text-xl font-bold text-gray-800">
        {Icon && (
          <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-2xl bg-gradient-to-br from-[#3FB6B2]/10 to-[#6EC8A7]/10 text-2xl text-[#3FB6B2] transition-all duration-300 group-hover:scale-110 group-hover:bg-[#3FB6B2] group-hover:text-white group-hover:shadow-md">
            <Icon />
          </div>
        )}
        {title}
      </div>
      <ul className="space-y-4 grow">
        {lines.map((line, idx) => {
          const cleanLine = line.replace(/^[-•*]\\s*/, '').trim();
          if (!cleanLine) return null;

          return (
            <li key={idx} className="flex items-start gap-3.5 rounded-xl px-2 py-2 transition-colors hover:bg-gray-50/80 -mx-2">
              <span className="mt-2.5 flex h-1.5 w-1.5 shrink-0 items-center justify-center rounded-full bg-[#3FB6B2]/40 ring-4 ring-[#3FB6B2]/10 transition-all duration-300 group-hover:bg-[#3FB6B2] group-hover:ring-[#3FB6B2]/20" />
              <div className="text-[15px] leading-[1.6] text-gray-600 font-medium break-keep">
                {highlightKeywords(cleanLine)}
              </div>
            </li>
          );
        })}
      </ul>
      <style>{\`
        @keyframes fadeUp {
          from { opacity: 0; transform: translateY(20px); }
          to { opacity: 1; transform: translateY(0); }
        }
      \`}</style>
    </div>
  );
};`;

  if (regex.test(content)) {
    content = content.replace(regex, newBlock);
    fs.writeFileSync(path, content, 'utf8');
    console.log('Success Block Replaced');
  } else {
    console.log('Could not test block.');
  }

} catch (e) {
  console.error(e);
}
