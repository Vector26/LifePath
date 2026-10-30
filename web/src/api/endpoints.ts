import { apiRequest } from './client';
import type {
  Quest,
  Habit,
  User,
  CurrentQuestResponse,
  CompleteQuestResponse,
  BlockQuestResponse,
  TodayStatsResponse,
  CalendarEvent,
  CalendarStatus,
  CurrentObjectiveResponse,
  UserSettings,
  CreateQuestRequest,
  UpdateQuestRequest,
  BlockQuestRequest,
  BlockTaskRequest,
  CreateHabitRequest,
  UpdateHabitRequest,
  UpdateUserRequest,
  TokenResponse,
  RegisterResponse,
  Streak,
  CityState,
  BuildingDefinition,
  UserBuilding,
  HabitCompletionResponse,
} from './types';

type ContextTag = 'focus' | 'quick' | 'low_energy';

const auth = {
  login: (email: string, password: string) =>
    apiRequest<TokenResponse>('/auth/login', {
      method: 'POST',
      body: { email, password },
    }),
  register: (body: {
    email: string;
    password: string;
    displayName?: string;
    timezone: string;
  }) =>
    apiRequest<RegisterResponse>('/auth/register', {
      method: 'POST',
      body,
    }),
  logout: () => apiRequest<void>('/auth/logout', { method: 'POST' }),
};

const quests = {
  getCurrent: (context?: ContextTag) => {
    const qs = context ? `?context=${context}` : '';
    return apiRequest<CurrentQuestResponse | undefined>(
      `/quests/current${qs}`,
      {}
    ).then((r) => r ?? null);
  },
  list: (status?: string, limit = 50) => {
    const params = new URLSearchParams();
    if (status) params.set('status', status);
    params.set('limit', String(Math.min(limit, 100)));
    return apiRequest<Quest[]>(`/quests?${params}`);
  },
  get: (id: string) => apiRequest<Quest>(`/quests/${id}`),
  create: (body: CreateQuestRequest) =>
    apiRequest<Quest>('/quests', { method: 'POST', body }),
  update: (id: string, body: UpdateQuestRequest) =>
    apiRequest<Quest>(`/quests/${id}`, { method: 'PATCH', body }),
  delete: (id: string) =>
    apiRequest<void>(`/quests/${id}`, { method: 'DELETE' }),
  complete: (id: string) =>
    apiRequest<CompleteQuestResponse>(`/quests/${id}/complete`, {
      method: 'POST',
    }),
  block: (id: string, body: BlockQuestRequest) =>
    apiRequest<BlockQuestResponse>(`/quests/${id}/block`, {
      method: 'POST',
      body,
    }),
  unblock: (id: string) =>
    apiRequest<Quest>(`/quests/${id}/unblock`, { method: 'POST' }),
};

const habits = {
  list: () => apiRequest<Habit[]>('/habits'),
  get: (id: string) => apiRequest<Habit>(`/habits/${id}`),
  create: (body: CreateHabitRequest) =>
    apiRequest<Habit>('/habits', { method: 'POST', body }),
  update: (id: string, body: UpdateHabitRequest) =>
    apiRequest<Habit>(`/habits/${id}`, { method: 'PATCH', body }),
  delete: (id: string) =>
    apiRequest<void>(`/habits/${id}`, { method: 'DELETE' }),
  complete: (id: string, idempotencyKey?: string) =>
    apiRequest<HabitCompletionResponse>(`/habits/${id}/complete`, {
      method: 'POST',
      idempotencyKey,
    }),
};

const stats = {
  today: () => apiRequest<TodayStatsResponse>('/stats/today'),
};

const streaks = {
  list: () => apiRequest<Streak[]>('/streaks'),
};

const objectives = {
  getCurrent: (context?: ContextTag) => {
    const qs = context ? `?context=${context}` : '';
    return apiRequest<CurrentObjectiveResponse | undefined>(
      `/objectives/current${qs}`,
      {}
    ).then((r) => r ?? null);
  },
  block: (body: BlockTaskRequest) =>
    apiRequest<void>('/objectives/block', {
      method: 'POST',
      body,
    }),
};

const calendar = {
  getStatus: () => apiRequest<CalendarStatus>('/calendar/status'),
  getAuthUrl: () => apiRequest<{ url: string }>('/calendar/auth-url'),
  disconnect: () =>
    apiRequest<void>('/calendar/disconnect', { method: 'POST' }),
  getTodayEvents: (sync = false) =>
    apiRequest<CalendarEvent[]>(
      `/calendar/events/today?sync=${sync}`
    ),
  completeEvent: (id: string) =>
    apiRequest<CalendarEvent>(`/calendar/events/${id}/complete`, {
      method: 'POST',
    }),
};

const users = {
  me: () => apiRequest<User>('/users/me'),
  updateMe: (body: UpdateUserRequest) =>
    apiRequest<User>('/users/me', { method: 'PATCH', body }),
  getSettings: () => apiRequest<UserSettings>('/users/me/settings'),
};

const city = {
  getState: () => apiRequest<CityState>('/city/state'),
  getBuildings: () => apiRequest<UserBuilding[]>('/city/buildings'),
  getShop: () => apiRequest<BuildingDefinition[]>('/city/shop'),
  placeBuilding: (body: { type: string; gridX: number; gridY: number }) =>
    apiRequest<UserBuilding>('/city/buildings', { method: 'POST', body }),
  maintainBuilding: (id: string) =>
    apiRequest<UserBuilding>(`/city/buildings/${id}/maintain`, { method: 'POST' }),
  demolishBuilding: (id: string) =>
    apiRequest<{ coins_refunded?: number }>(
      `/city/buildings/${id}`,
      { method: 'DELETE' }
    ),
  syncCoins: (coins: number) =>
    apiRequest<CityState>('/city/state/coins', { method: 'PATCH', body: { coins } }),
};

export const api = {
  auth,
  quests,
  habits,
  stats,
  streaks,
  objectives,
  calendar,
  users,
  city,
};
