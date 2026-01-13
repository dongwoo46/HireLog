import { createBrowserRouter } from 'react-router-dom';
import App from '../App';
import JdSummaryPage from '../pages/JdSummaryPage';

export const router = createBrowserRouter([
  {
    path: '/',
    element: <App />,
  },
  {
    path: '/tools/jd-summary',
    element: <JdSummaryPage />,
  },
]);
