import { describe, expect, it } from 'vitest';
import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { spacing } from './theme';

const WEB_ROOT = resolve(process.cwd(), 'src');
const ANDROID_LAYOUT_ROOT = resolve(process.cwd(), '../android/app/src/main/res/layout');
const ANDROID_DIMENS = resolve(process.cwd(), '../android/app/src/main/res/values/dimens.xml');

describe('spacing scale math', () => {
  it('uses a deterministic 8px-based spacing scale', () => {
    expect(spacing.xs).toBe(4);
    expect(spacing.sm).toBe(8);
    expect(spacing.md).toBe(16);
    expect(spacing.lg).toBe(24);
    expect(spacing.xl).toBe(32);

    // Mathematical checks to keep spacing consistent over time.
    expect(spacing.sm).toBe(spacing.xs * 2);
    expect(spacing.md).toBe(spacing.sm * 2);
    expect(spacing.lg).toBe(spacing.sm * 3);
    expect(spacing.xl).toBe(spacing.sm * 4);
  });

  it('keeps global CSS spacing variables aligned with theme tokens', () => {
    const css = readFileSync(resolve(WEB_ROOT, 'index.css'), 'utf-8');
    expect(css).toContain(`--spacing-xs: ${spacing.xs}px;`);
    expect(css).toContain(`--spacing-sm: ${spacing.sm}px;`);
    expect(css).toContain(`--spacing-md: ${spacing.md}px;`);
    expect(css).toContain(`--spacing-lg: ${spacing.lg}px;`);
    expect(css).toContain(`--spacing-xl: ${spacing.xl}px;`);
  });
});

describe('android spacing parity', () => {
  const spacingRefs = [
    '@dimen/spacing_xs',
    '@dimen/spacing_sm',
    '@dimen/spacing_md',
    '@dimen/spacing_lg',
    '@dimen/spacing_xl',
  ];

  const layoutFiles = [
    'activity_auth.xml',
    'activity_server_config.xml',
    'widget_small.xml',
    'widget_medium.xml',
    'widget_large.xml',
  ];

  it('defines expected spacing dimen resources', () => {
    const xml = readFileSync(ANDROID_DIMENS, 'utf-8');
    expect(xml).toContain('<dimen name="spacing_xs">4dp</dimen>');
    expect(xml).toContain('<dimen name="spacing_sm">8dp</dimen>');
    expect(xml).toContain('<dimen name="spacing_md">16dp</dimen>');
    expect(xml).toContain('<dimen name="spacing_lg">24dp</dimen>');
    expect(xml).toContain('<dimen name="spacing_xl">32dp</dimen>');
  });

  it('uses spacing dimen tokens for Android paddings and margins', () => {
    const attrPattern =
      /android:(?:padding|paddingStart|paddingEnd|paddingTop|paddingBottom|layout_marginTop|layout_marginBottom|layout_marginStart|layout_marginEnd)="([^"]+)"/g;

    for (const layoutFile of layoutFiles) {
      const xml = readFileSync(resolve(ANDROID_LAYOUT_ROOT, layoutFile), 'utf-8');
      const values = [...xml.matchAll(attrPattern)].map((m) => m[1]);
      expect(values.length).toBeGreaterThan(0);
      for (const value of values) {
        expect(spacingRefs).toContain(value);
      }
    }
  });
});
