import { Link } from 'react-router-dom';
import './index.css';

function App() {
  return (
    <div className="min-h-screen bg-gray-100 flex items-center justify-center">
      <div className="rounded-lg bg-white p-8 shadow-md space-y-4 w-[420px]">
        <h1 className="text-2xl font-bold text-gray-800">Hirelog Web</h1>

        <p className="text-gray-600">내부 테스트용 JD Summary 도구</p>

        <Link
          to="/tools/jd-summary"
          className="block text-center rounded bg-blue-600 px-4 py-2 text-white hover:bg-blue-700"
        >
          JD Summary 페이지로 이동
        </Link>
      </div>
    </div>
  );
}

export default App;
