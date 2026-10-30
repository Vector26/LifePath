// Enums matching backend
export type QuestStatus = 'pending' | 'blocked' | 'completed';
export type PriorityLevel = 'P0' | 'P1' | 'P2' | 'P3' | 'P4';
export type TimeOfDay = 'morning' | 'afternoon' | 'evening' | 'any';
export type ContextTag = 'focus' | 'quick' | 'low_energy';
export type UnblockTrigger = 'manual' | 'time' | 'location';

// Quest – matches QuestResponse DTO (snake_case for JSON)
export interface Quest {
  id: string;
  title: string;
  notes: string | null;
  priority_level: PriorityLevel;
  parent_id: string | null;
  status: QuestStatus;
  time_of_day: TimeOfDay;
  context_tag: ContextTag | null;
  created_at: string;
  updated_at: string;
  completed_at: string | null;
}

// Habit – matches HabitResponse DTO
export interface Habit {
  id: string;
  title: string;
  currencyPerCompletion: number;
  streakTracked: boolean;
  created_at?: string;
  updated_at?: string;
}

// User – matches UserResponse DTO (snake_case where specified)
export interface User {
  id: string;
  email: string;
  display_name: string | null;
  timezone: string;
  total_xp: number;
  level: number;
  created_at: string;
  updated_at: string;
}

// Responses
export interface CurrentQuestResponse {
  quest: Quest;
  reasons: string[];
}

export interface CompleteQuestResponse {
  quest: Quest;
  xp_earned: number;
}

export interface BlockQuestResponse {
  quest: Quest;
  next_quest: Quest | null;
  xp_earned: number;
}

export interface TodayStatsResponse {
  quests_completed_today: number;
  habits_completed_today: number;
  date: string;
}

// Calendar
export interface CalendarEvent {
  id: string;
  title: string;
  external_id: string | null;
  start_time: string;
  end_time: string;
  completed: boolean;
}

export interface CalendarStatus {
  configured: boolean;
  connected: boolean;
}

// Objective
export type ObjectiveType = 'quest' | 'habit' | 'calendar_event';

export interface CurrentObjectiveResponse {
  type: ObjectiveType;
  payload: CurrentQuestResponse | Habit | CalendarEvent;
  reasons: string[];
}

// User settings
export interface UserSettings {
  notifications_enabled: boolean;
  haptic_feedback: boolean;
  theme: string;
}

// Request bodies (camelCase for backend)
export interface CreateQuestRequest {
  title: string;
  notes?: string;
  priorityLevel: PriorityLevel;
  parentId?: string;
  timeOfDay?: TimeOfDay;
  contextTag?: ContextTag;
}

export interface UpdateQuestRequest {
  title?: string;
  notes?: string;
  priorityLevel?: PriorityLevel;
  parentId?: string;
  timeOfDay?: TimeOfDay;
  contextTag?: ContextTag;
}

export interface BlockQuestRequest {
  reason?: string;
  unblockTrigger: UnblockTrigger;
  unblockAt?: string;
  unblockLocation?: string;
}

export interface BlockTaskRequest {
  taskType: ObjectiveType;
  taskId: string;
  reason?: string;
  unblockTrigger: UnblockTrigger;
  unblockAt?: string;
  unblockLocation?: string;
}

export interface CreateHabitRequest {
  title: string;
  currencyPerCompletion?: number;
  streakTracked?: boolean;
}

export interface UpdateHabitRequest {
  title?: string;
  currencyPerCompletion?: number;
  streakTracked?: boolean;
}

export interface UpdateUserRequest {
  displayName?: string;
  timezone?: string;
}

// City – matches backend models/DTOs
export interface BuildingDefinition {
  type: string;
  name: string;
  cost: number;
  sprite_key: string;
  tile_gid: number;
  ttl_seconds: number;
  decay_grace_seconds: number;
  maintenance_cost: number;
}

export interface UserBuilding {
  id: string;
  type: string;
  sprite_key: string;
  tile_gid: number;
  grid_x: number;
  grid_y: number;
  placed_at: string;
  expires_at: string;
  decayed_at: string | null;
  remove_after_at: string | null;
  maintenance_cost: number;
}

export interface RoadCell {
  id: string;
  user_id: string;
  grid_x: number;
  grid_y: number;
  placed_at: string;
}

export interface CityState {
  user_id?: string;
  coins: number;
  premium_currency?: number;
  city_name: string;
  population: number;
  created_at?: string;
  updated_at?: string;
}

// Auth responses
export interface TokenResponse {
  access_token: string;
  refresh_token?: string;
  expires_in?: number;
}

export interface RegisterResponse {
  user: User;
  access_token: string;
  refresh_token?: string;
  expires_in?: number;
}

// Streak
export interface Streak {
  habitId: string;
  current_streak_days: number;
  longest_streak_days: number;
  last_completed_date: string | null;
}

// Habit completion
export interface HabitCompletionResponse {
  currency_earned: number;
  streak?: {
    current_streak_days: number;
    longest_streak_days: number;
    last_completed_date: string | null;
  };
}
