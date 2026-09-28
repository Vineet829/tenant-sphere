import { Instrument_Serif, Manrope } from "next/font/google";

export const estateSans = Manrope({ subsets: ["latin"], variable: "--estate-sans", display: "swap" });
export const estateEditorial = Instrument_Serif({
	subsets: ["latin"],
	weight: "400",
	style: ["normal", "italic"],
	variable: "--estate-editorial",
	display: "swap",
	adjustFontFallback: false,
	fallback: ["Georgia", "serif"],
});
