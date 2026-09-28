import type { Config } from "tailwindcss";
import plugin from "tailwindcss/plugin";
import { estateVariables, tenantTheme } from "./tailwind.theme";

const config = {
	darkMode: ["class"],
	content: ["./pages/**/*.{ts,tsx}", "./components/**/*.{ts,tsx}", "./app/**/*.{ts,tsx}", "./src/**/*.{ts,tsx}"],
	prefix: "",
	theme: {
		container: {
			center: true,
			padding: "2rem",
			screens: {
				"2xl": "1400px",
			},
		},
		extend: {
			...tenantTheme,
			colors: {
				...tenantTheme.colors,
				googleBlue: "#4285F4",
				asparagus: "#60992D",
				deepBlueGrey: "#263238",
				gray: "rgba(125,144,201,0.34)",
				lightGray: "#7B8EC8",
				electricIndigo: "#6610F2",
				richBlack: "#0D1317",
				veryBlack: "#000000",
				babyPowder: "#FBFEF9",
				pumpkin: "#F17105",
				platinum: "#DDE1E4",
				lightGrey: "#EEEEEE",
				pear: "#C2E812",
				eerieBlack: "#171D1C",
				ballonWhite: "#dfe6f6",
			},

			keyframes: {
				...tenantTheme.keyframes,
				"accordion-down": {
					from: { height: "0" },
					to: { height: "var(--radix-accordion-content-height)" },
				},
				"accordion-up": {
					from: { height: "var(--radix-accordion-content-height)" },
					to: { height: "0" },
				},
			},
			animation: {
				...tenantTheme.animation,
				"accordion-down": "accordion-down 0.2s ease-out",
				"accordion-up": "accordion-up 0.2s ease-out",
			},
			fontFamily: {
				...tenantTheme.fontFamily,
				openSans: ["var(--font-openSans)"],
				robotoSlab: ["var(--font-robotoSlab)"],
			},
		},
	},
	plugins: [
		require("tailwindcss-animate"),
		plugin(({ addBase }) => {
			addBase({
				".estate-shell, .estate-popover, .estate-mobile-panel, body:has(.estate-shell) .Toastify__toast-container":
					estateVariables,
			});
		}),
	],
} satisfies Config;

export default config;
