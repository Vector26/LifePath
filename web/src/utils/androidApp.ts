/** Notify the Android app that widget data has changed (e.g. after completing a quest/habit). No-op when running in browser. */
export function notifyWidgetRefresh(): void {
  const bridge = (window as unknown as Record<string, unknown>).LifePathAndroid;
  if (bridge && typeof (bridge as { onDataChanged?: () => void }).onDataChanged === 'function') {
    (bridge as { onDataChanged: () => void }).onDataChanged();
  }
}
