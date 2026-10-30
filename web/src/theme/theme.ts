/**
 * LifePath Design Tokens
 * Base unit: 8px. All spacing derives from this scale.
 */

export const spacing = {
  xs: 4,
  sm: 8,
  md: 16,
  lg: 24,
  xl: 32,
} as const;

export const tapTarget = {
  minHeight: 44,
  minWidth: 44,
} as const;

export const borderRadius = {
  sm: 8,
  md: 12,
  lg: 16,
  xl: 24,
  pill: 24,
} as const;

export const typography = {
  fontFamily: '"Inter", system-ui, -apple-system, sans-serif',
  sizes: {
    xs: 12,
    sm: 13,
    md: 14,
    lg: 16,
    xl: 20,
    '2xl': 28,
  },
  weights: {
    normal: 400,
    medium: 500,
    semibold: 600,
    bold: 700,
    extrabold: 800,
  },
  lineHeights: {
    tight: 1.2,
    normal: 1.4,
    relaxed: 1.5,
  },
} as const;

export const breakpoints = {
  mobile: 600,
  tablet: 1024,
} as const;

export const layout = {
  appShell: {
    sidebarWidth: 250,
    contentMaxWidth: 1200,
    bottomNavHeight: 80,
  },
  card: {
    shadow: '0px 4px 12px rgba(0,0,0,0.05)',
  },
} as const;

export type ThemeColors = {
  background: string;
  surface: string;
  border: string;
  primary: string;
  primaryHover: string;
  secondary: string;
  text: string;
  textSecondary: string;
  textMuted: string;
  success: string;
  warning: string;
  destructive: string;
};

export const lightTheme: ThemeColors = {
  background: '#F8F6F2',
  surface: '#FFFFFF',
  border: 'rgba(0,0,0,0.08)',
  primary: '#C4785A',
  primaryHover: '#B0684A',
  secondary: '#7A9B76',
  text: '#2C2A26',
  textSecondary: '#6B6560',
  textMuted: '#8B8680',
  success: '#6B8E6B',
  warning: '#D4A84A',
  destructive: '#B85C4A',
};

export const darkTheme: ThemeColors = {
  background: '#1E1C19',
  surface: '#2C2A26',
  border: 'rgba(255,255,255,0.12)',
  primary: '#D4937A',
  primaryHover: '#E4A38A',
  secondary: '#8BAB87',
  text: '#F0EDE8',
  textSecondary: '#9A948C',
  textMuted: '#7A7570',
  success: '#7A9E7A',
  warning: '#D4A84A',
  destructive: '#C86C5A',
};

export type Theme = {
  colors: ThemeColors;
  spacing: typeof spacing;
  tapTarget: typeof tapTarget;
  borderRadius: typeof borderRadius;
  typography: typeof typography;
  breakpoints: typeof breakpoints;
  layout: typeof layout;
};

export const createTheme = (colors: ThemeColors): Theme => ({
  colors,
  spacing,
  tapTarget,
  borderRadius,
  typography,
  breakpoints,
  layout,
});
