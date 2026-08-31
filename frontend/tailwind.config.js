/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        cream: {
          50: '#FDFCF7',
          100: '#F8F6F0',
          200: '#EFECE0',
          300: '#E2DEC9',
        },
        navy: {
          800: '#1E293B',
          900: '#0F172A',
          950: '#090D16',
        },
        brand: {
          cyan: '#0EA5E9',
          blue: '#2563EB',
          teal: '#14B8A6',
        }
      }
    },
  },
  plugins: [],
}
