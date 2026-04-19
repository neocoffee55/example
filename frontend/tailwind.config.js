/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{ts,tsx}"],
  theme: {
    extend: {
      fontFamily: {
        display: ["Georgia", "serif"],
        body: ["'Segoe UI'", "system-ui", "sans-serif"],
      },
      colors: {
        ink: "#132238",
        paper: "#f5f2ea",
        accent: "#a24c2f",
        ocean: "#0b6e72",
        moss: "#5c7251",
      },
      boxShadow: {
        frame: "0 24px 80px rgba(19, 34, 56, 0.12)",
      },
    },
  },
  plugins: [],
};
