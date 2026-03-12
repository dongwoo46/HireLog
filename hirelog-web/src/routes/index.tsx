import { createBrowserRouter } from 'react-router-dom';
import { RootLayout } from '../layouts/RootLayout';
import MainPage from '../pages/MainPage';
import LoginPage from '../pages/LoginPage';
import SignupPage from '../pages/SignupPage';
import RecoveryPage from '../pages/RecoveryPage';
import JobSummaryListPage from '../pages/JobSummaryListPage';
import JobSummaryDetailPage from '../pages/JobSummaryDetailPage';
import JobSummaryRequestPage from '../pages/JobSummaryRequestPage';
import UserRequestListPage from '../pages/UserRequestListPage';
import UserRequestDetailPage from '../pages/UserRequestDetailPage';
import UserRequestCreatePage from '../pages/UserRequestCreatePage';
import ProfilePage from '../pages/ProfilePage';
import JobSummaryArchivePage from '../pages/JobSummaryArchivePage';
import JdListPage from '../pages/JdListPage';
import AdminPage from '../pages/AdminPage';

export const router = createBrowserRouter([
  {
    path: '/',
    element: <RootLayout />,
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
        path: 'jd',
        element: <JobSummaryListPage />,
      },
      {
        path: 'jd/:id',
        element: <JobSummaryDetailPage />,
      },
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
    ],
  },
]);
