import { createBrowserRouter } from 'react-router-dom';
import { RootLayout } from '../layouts/RootLayout';
import MainPage from '../pages/MainPage';
import LoginPage from '../pages/LoginPage';
import SignupPage from '../pages/SignupPage';
import RecoveryPage from '../pages/RecoveryPage';
import JdSummaryPage from '../pages/JdSummaryPage';
import JdListPage from '../pages/JdListPage';

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
        path: 'tools/jd-summary',
        element: <JdSummaryPage />,
      },
      {
        path: 'tools/jd-list',
        element: <JdListPage />,
      },
    ],
  },
]);
