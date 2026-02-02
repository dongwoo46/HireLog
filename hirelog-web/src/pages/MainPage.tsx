import { useState } from 'react';
import { TbSearch, TbBriefcase, TbClock, TbMapPin } from 'react-icons/tb';

// Mock Data for JDs
const MOCK_JDS = [
  {
    id: 1,
    title: 'Senior Frontend Developer',
    company: 'TechCorp Inc.',
    location: 'Seoul, Korea (Remote)',
    type: 'Full-time',
    summary: 'We are looking for an experienced Frontend Developer to lead our web team...',
    tags: ['React', 'TypeScript', 'Next.js'],
    postedAt: '2025-02-01',
  },
  {
    id: 2,
    title: 'Product Designer',
    company: 'DesignStudio',
    location: 'Gangnam, Seoul',
    type: 'Contract',
    summary: 'Seeking a creative Product Designer to work on our new mobile application...',
    tags: ['Figma', 'UI/UX', 'Mobile'],
    postedAt: '2025-01-28',
  },
  {
    id: 3,
    title: 'Backend Engineer (Java/Kotlin)',
    company: 'FinService',
    location: 'Yeouido, Seoul',
    type: 'Full-time',
    summary: 'Join our core platform team building high-performance financial systems...',
    tags: ['Java', 'Spring Boot', 'AWS'],
    postedAt: '2025-01-25',
  },
  {
    id: 4,
    title: 'Data Analyst',
    company: 'CommerceBig',
    location: 'Pangyo',
    type: 'Full-time',
    summary: 'Analyze user behavior data to drive business growth and product improvements...',
    tags: ['SQL', 'Python', 'Tableau'],
    postedAt: '2025-01-20',
  },
];

const MainPage = () => {
  const [searchTerm, setSearchTerm] = useState('');

  const filteredJDs = MOCK_JDS.filter((jd) =>
    jd.title.toLowerCase().includes(searchTerm.toLowerCase()) ||
    jd.company.toLowerCase().includes(searchTerm.toLowerCase()) ||
    jd.tags.some(tag => tag.toLowerCase().includes(searchTerm.toLowerCase()))
  );

  return (
    <div className="min-h-screen bg-gray-50 text-gray-900 font-sans">
      <div className="max-w-5xl mx-auto px-6 pt-32 pb-20">
        
        {/* Header / Search Section */}
        <div className="text-center mb-12">
          <h1 className="text-3xl font-bold mb-6 text-gray-900">
            Search Job Descriptions
          </h1>
          
          <div className="max-w-2xl mx-auto relative group">
            <div className="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none">
              <TbSearch className="h-5 w-5 text-gray-400 group-focus-within:text-blue-500 transition-colors" />
            </div>
            <input
              type="text"
              className="block w-full pl-11 pr-4 py-4 bg-white border border-gray-200 rounded-xl text-gray-900 placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 shadow-sm transition-all"
              placeholder="Search by job title, company, or keywords..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
            />
          </div>
        </div>

        {/* JD List Section */}
        <div className="grid gap-4">
          {filteredJDs.length > 0 ? (
            filteredJDs.map((jd) => (
              <div 
                key={jd.id} 
                className="bg-white border border-gray-100 rounded-xl p-6 hover:shadow-md transition-shadow cursor-pointer group"
                onClick={() => alert(`Navigating to JD: ${jd.title}`)} // Placeholder for navigation
              >
                <div className="flex justify-between items-start mb-2">
                  <div>
                    <h3 className="text-lg font-bold text-gray-900 group-hover:text-blue-600 transition-colors">
                      {jd.title}
                    </h3>
                    <p className="text-gray-500 font-medium">{jd.company}</p>
                  </div>
                  <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-blue-50 text-blue-700">
                    {jd.type}
                  </span>
                </div>
                
                <div className="flex items-center gap-4 text-sm text-gray-400 mb-4">
                  <span className="flex items-center gap-1">
                    <TbMapPin className="w-4 h-4" />
                    {jd.location}
                  </span>
                  <span className="flex items-center gap-1">
                    <TbClock className="w-4 h-4" />
                    {jd.postedAt}
                  </span>
                </div>

                <p className="text-gray-600 text-sm mb-4 line-clamp-2">
                  {jd.summary}
                </p>

                <div className="flex flex-wrap gap-2">
                  {jd.tags.map(tag => (
                    <span key={tag} className="px-2 py-1 bg-gray-100 text-gray-600 text-xs rounded-md">
                      {tag}
                    </span>
                  ))}
                </div>
              </div>
            ))
          ) : (
            <div className="text-center py-12 text-gray-500 bg-white rounded-xl border border-dashed border-gray-200">
              <TbBriefcase className="w-12 h-12 mx-auto mb-3 text-gray-300" />
              <p>No job descriptions found matching "{searchTerm}"</p>
            </div>
          )}
        </div>

      </div>
    </div>
  );
};

export default MainPage;
