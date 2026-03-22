/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  theme: {
    extend: {
      colors: {
        soc: {
          bg:      '#0a0e1a',
          surface: '#111827',
          card:    '#1a2235',
          border:  '#1e2d45',
          accent:  '#06b6d4',
          muted:   '#4b5563',
        },
      },
      fontFamily: {
        mono: ['JetBrains Mono', 'Fira Code', 'monospace'],
      },
    },
  },
  plugins: [],
}
