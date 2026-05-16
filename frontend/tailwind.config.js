/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,jsx}'],
  theme: {
    extend: {
      fontFamily: {
        sans: ['Inter', 'ui-sans-serif', 'system-ui', '-apple-system', 'Segoe UI', 'Roboto', 'sans-serif'],
        display: ['"Plus Jakarta Sans"', 'Inter', 'ui-sans-serif', 'system-ui', 'sans-serif'],
        mono: ['"JetBrains Mono"', 'ui-monospace', 'SFMono-Regular', 'monospace'],
      },
      colors: {
        // Brand — indigo profond, ton enterprise
        brand: {
          50:  '#eef2ff',
          100: '#e0e7ff',
          200: '#c7d2fe',
          300: '#a5b4fc',
          400: '#818cf8',
          500: '#6366f1',
          600: '#4f46e5',
          700: '#4338ca',
          800: '#3730a3',
          900: '#312e81',
          950: '#1e1b4b',
        },
        // Accent — cyan complémentaire pour CTA/highlights
        accent: {
          400: '#22d3ee',
          500: '#06b6d4',
          600: '#0891b2',
        },
        ink: {
          50:  '#f8fafc',
          100: '#f1f5f9',
          200: '#e2e8f0',
          300: '#cbd5e1',
          400: '#94a3b8',
          500: '#64748b',
          600: '#475569',
          700: '#334155',
          800: '#1e293b',
          900: '#0f172a',
          950: '#020617',
        },
      },
      boxShadow: {
        card:      '0 1px 2px rgba(15,23,42,.04), 0 4px 12px rgba(15,23,42,.04)',
        cardHover: '0 8px 24px rgba(79,70,229,.10), 0 2px 6px rgba(15,23,42,.06)',
        glow:      '0 0 0 1px rgba(99,102,241,.18), 0 8px 32px rgba(99,102,241,.25)',
      },
      backgroundImage: {
        'grid-faint':
          "linear-gradient(to right, rgba(99,102,241,0.06) 1px, transparent 1px), linear-gradient(to bottom, rgba(99,102,241,0.06) 1px, transparent 1px)",
        'radial-brand':
          'radial-gradient(60% 60% at 50% 0%, rgba(99,102,241,0.18) 0%, rgba(99,102,241,0) 60%)',
      },
      keyframes: {
        'fade-in':  { '0%': { opacity: 0, transform: 'translateY(4px)' }, '100%': { opacity: 1, transform: 'translateY(0)' } },
        'slide-up': { '0%': { opacity: 0, transform: 'translateY(8px)' }, '100%': { opacity: 1, transform: 'translateY(0)' } },
        'pulse-dot': {
          '0%, 100%': { transform: 'scale(1)',   opacity: 1 },
          '50%':      { transform: 'scale(1.4)', opacity: .6 },
        },
        shimmer: {
          '0%':   { backgroundPosition: '-200% 0' },
          '100%': { backgroundPosition: '200% 0' },
        },
      },
      animation: {
        'fade-in':  'fade-in .25s ease-out both',
        'slide-up': 'slide-up .35s ease-out both',
        'pulse-dot':'pulse-dot 1.6s ease-in-out infinite',
        shimmer:    'shimmer 1.8s linear infinite',
      },
    },
  },
  plugins: [],
}
