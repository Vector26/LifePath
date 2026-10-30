import { createBrowserRouter, Navigate, RouterProvider } from 'react-router-dom';
import { AppShell } from './components/layout/AppShell';
import { AuthLayout } from './components/layout/AuthLayout';
import { QuestHUD } from './screens/home/QuestHUD';
import { QuestList } from './screens/quests/QuestList';
import { CreateQuest } from './screens/quests/CreateQuest';
import { EditQuest } from './screens/quests/EditQuest';
import { HabitList } from './screens/habits/HabitList';
import { CreateHabit } from './screens/habits/CreateHabit';
import { EditHabit } from './screens/habits/EditHabit';
import { CalendarToday } from './screens/calendar/CalendarToday';
import { GardenView } from './screens/city/GardenView';
import { Settings } from './screens/settings/Settings';
import { Login } from './screens/auth/Login';
import { Register } from './screens/auth/Register';

const router = createBrowserRouter([
  { path: '/login', element: <AuthLayout />, children: [{ index: true, element: <Login /> }] },
  { path: '/register', element: <AuthLayout />, children: [{ index: true, element: <Register /> }] },
  {
    path: '/',
    element: <AppShell />,
    children: [
      { index: true, element: <Navigate to="/quest" replace /> },
      { path: 'quest', element: <QuestHUD /> },
      { path: 'quests', element: <QuestList /> },
      { path: 'quests/new', element: <CreateQuest /> },
      { path: 'quests/:id/edit', element: <EditQuest /> },
      { path: 'habits', element: <HabitList /> },
      { path: 'habits/new', element: <CreateHabit /> },
      { path: 'habits/:id/edit', element: <EditHabit /> },
      { path: 'calendar', element: <CalendarToday /> },
      { path: 'city', element: <GardenView /> },
      { path: 'settings', element: <Settings /> },
    ],
  },
]);

export default function App() {
  return <RouterProvider router={router} />;
}
