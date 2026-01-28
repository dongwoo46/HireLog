import { Link } from 'react-router-dom';

const MainPage = () => {
  return (
    <div className="min-h-screen bg-slate-900 text-white font-sans selection:bg-purple-500 selection:text-white">
      {/* Navbar */}
      <nav className="fixed w-full z-50 bg-slate-900/80 backdrop-blur-md border-b border-white/10">
        <div className="max-w-7xl mx-auto px-6 h-16 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <div className="w-8 h-8 bg-gradient-to-tr from-blue-500 to-purple-600 rounded-lg flex items-center justify-center font-bold text-lg">
              H
            </div>
            <span className="text-xl font-bold bg-clip-text text-transparent bg-gradient-to-r from-blue-400 to-purple-500">
              HireLog
            </span>
          </div>
          
          <div className="flex items-center gap-6">
            <Link to="/login" className="px-4 py-2 bg-white text-slate-900 rounded-full text-sm font-bold hover:bg-slate-200 transition-all transform hover:scale-105">
              Log in
            </Link>
          </div>
        </div>
      </nav>

      {/* Hero Section */}
      <div className="relative pt-32 pb-20 lg:pt-48 lg:pb-32 overflow-hidden">
        {/* Background Gradients */}
        <div className="absolute top-0 left-1/2 -translate-x-1/2 w-full h-full z-0 pointer-events-none">
          <div className="absolute top-20 left-10 w-96 h-96 bg-blue-600/20 rounded-full blur-3xl mix-blend-screen animate-pulse"></div>
          <div className="absolute top-40 right-10 w-96 h-96 bg-purple-600/20 rounded-full blur-3xl mix-blend-screen animate-pulse delay-700"></div>
        </div>

        <div className="relative z-10 max-w-7xl mx-auto px-6 text-center">
          <h1 className="text-5xl lg:text-7xl font-bold tracking-tight mb-8">
            Master Your <br/>
            <span className="bg-clip-text text-transparent bg-gradient-to-r from-blue-400 to-purple-600">
              Hiring Process
            </span>
          </h1>
          <p className="text-lg lg:text-xl text-slate-400 max-w-2xl mx-auto mb-10 leading-relaxed">
            Streamline your recruitment with intelligent tools. Analyze Job Descriptions, track candidates, and make better hiring decisions faster.
          </p>
          
          <div className="flex flex-col sm:flex-row items-center justify-center gap-4">
            <Link 
              to="/login"
              className="w-full sm:w-auto px-8 py-4 bg-gradient-to-r from-blue-600 to-purple-600 rounded-full font-bold text-lg hover:shadow-lg hover:shadow-blue-500/30 transition-all transform hover:-translate-y-1"
            >
              Get Started for Free
            </Link>
            <Link 
              to="/tools/jd-summary"
              className="w-full sm:w-auto px-8 py-4 bg-slate-800 border border-slate-700 rounded-full font-bold text-lg text-slate-300 hover:bg-slate-700 hover:text-white transition-all"
            >
              Try JD Summary
            </Link>
          </div>
        </div>
      </div>
      
      {/* Features Grid */}
      <div className="relative z-10 max-w-7xl mx-auto px-6 py-20 border-t border-white/5">
        <div className="grid md:grid-cols-3 gap-8">
          {[
            { title: "Smart Analysis", desc: "AI-powered insights for every job description you process." },
            { title: "Collaborative", desc: "Share notes and ratings with your hiring team seamlessly." },
            { title: "Organized", desc: "Keep all your hiring data in one secure, accessible place." }
          ].map((item, i) => (
            <div key={i} className="p-6 rounded-2xl bg-white/5 border border-white/10 hover:border-white/20 transition-colors">
              <h3 className="text-xl font-bold mb-3 text-blue-400">{item.title}</h3>
              <p className="text-slate-400">{item.desc}</p>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};

export default MainPage;
