const REASON_LABELS: Record<string, string> = {
  high_priority: 'High priority',
  matches_morning: 'Good for morning',
  matches_afternoon: 'Good for afternoon',
  matches_evening: 'Good for evening',
  matches_context: 'Matches your focus',
  time_sensitive: 'Time sensitive',
  long_pending: 'Waiting for a while',
  just_unblocked: 'Just unblocked',
  recently_added: 'Recently added',
  habit_due_today: 'Due today',
  streak_tracked: 'Streak tracked',
  happening_now: 'Happening now',
  starting_soon: 'Starting soon',
  scheduled_today: 'Scheduled today',
};

export function getReasonLabel(code: string): string {
  return REASON_LABELS[code] ?? code.replace(/_/g, ' ');
}
