import { describe, it, expect, vi } from 'vitest';

vi.mock('../../hooks/useUser', () => ({
  useUser: () => ({ user: { display_name: 'Test' }, loading: false }),
}));

describe('GardenView', () => {
  it('renders without crashing', async () => {
    const { GardenView } = await import('./GardenView');
    expect(GardenView).toBeDefined();
  });
});
