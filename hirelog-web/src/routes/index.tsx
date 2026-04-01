import { createBrowserRouter, useRouteError, Link } from 'react-router-dom';
import { RootLayout } from '../layouts/RootLayout';
import MainPage from '../pages/MainPage';
import LoginPage from '../pages/LoginPage';
import SignupPage from '../pages/SignupPage';
import RecoveryPage from '../pages/RecoveryPage';
import ServiceIntroPage from '../pages/ServiceIntroPage';
import JobSummaryListPage from '../pages/JobSummaryListPage';
import JobSummaryDetailPage from '../pages/JobSummaryDetailPage';
import JobSummaryRequestPage from '../pages/JobSummaryRequestPage';
import UserRequestListPage from '../pages/UserRequestListPage';
import UserRequestDetailPage from '../pages/UserRequestDetailPage';
import UserRequestCreatePage from '../pages/UserRequestCreatePage';
import ProfilePage from '../pages/ProfilePage';
import JobSummaryArchivePage from '../pages/JobSummaryArchivePage';
import JdListPage from '../pages/JdListPage';
import BoardListPage from '../pages/BoardListPage';
import BoardDetailPage from '../pages/BoardDetailPage';
import AdminPage from '../pages/AdminPage';
import AdminJobSummaryRequestPage from '../pages/AdminJobSummaryRequestPage';
import { ProtectedRoute } from '../components/common/ProtectedRoute';

function ErrorBoundary() {
  const error = useRouteError() as any;
  console.error(error);
  return (
    <div className="flex flex-col items-center justify-center min-h-screen bg-gray-50 text-center p-6">
      <div className="bg-white p-8 rounded-2xl shadow-sm border border-gray-100 max-w-md w-full">
        <h1 className="text-4xl font-black text-rose-500 mb-4">Oops!</h1>
        <p className="text-gray-600 mb-6 font-medium">Sorry, an unexpected error has occurred.</p>
        <p className="bg-rose-50 text-rose-600 px-4 py-3 rounded-xl text-sm mb-6 overflow-hidden text-ellipsis whitespace-nowrap">
          <i>{error?.statusText || error?.message || "Unknown error"}</i>
        </p>
        <Link to="/" className="inline-flex justify-center items-center px-6 py-3 bg-[#4CDFD5] text-white font-bold rounded-xl hover:bg-[#3ccfc5] transition-colors w-full">
          Return to Home
        </Link>
      </div>
    </div>
  );
}

export const router = createBrowserRouter([
  {
    path: '/',
    element: <RootLayout />,
    errorElement: <ErrorBoundary />,
    children: [
      {
        index: true,
        element: <MainPage />,
      },
      {
        path: 'login',
        element: <LoginPage />,
      },
      {
        path: 'signup',
        element: <SignupPage />,
      },
      {
        path: 'recovery',
        element: <RecoveryPage />,
      },
      {
        path: 'service-intro',
        element: <ServiceIntroPage />,
      },
      {
        path: 'board',
        element: <BoardListPage />,
      },
      {
        path: 'board/:id',
        element: <BoardDetailPage />,
      },
      {
        path: 'jd',
        element: <JobSummaryListPage />,
      },
      {
        path: 'jd/:id',
        element: <JobSummaryDetailPage />,
      },

      // Protected Routes
      {
        element: <ProtectedRoute />,
        children: [
          {
            path: 'jd/request',
            element: <JobSummaryRequestPage />,
          },
          {
            path: 'requests',
            element: <UserRequestListPage />,
          },
          {
            path: 'requests/new',
            element: <UserRequestCreatePage />,
          },
          {
            path: 'requests/:id',
            element: <UserRequestDetailPage />,
          },
          {
            path: 'profile',
            element: <ProfilePage />,
          },
          {
            path: 'archive',
            element: <JobSummaryArchivePage />,
          },
          {
            path: 'history',
            element: <JdListPage />,
          },
          {
            path: 'admin',
            element: <AdminPage />,
          },
          {
            path: 'admin/jd/request',
            element: <AdminJobSummaryRequestPage />,
          },
        ]
      }
    ],
  },
]);
