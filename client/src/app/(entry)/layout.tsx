import { Instrument_Serif, Manrope } from "next/font/google";
import type { ReactNode } from "react";
import styles from "@/components/auth/auth.module.css";

const sans = Manrope({ subsets: ["latin"], variable: "--auth-sans", display: "swap" });
const editorial = Instrument_Serif({ subsets: ["latin"], weight: "400", style: ["normal", "italic"], variable: "--auth-editorial", display: "swap", adjustFontFallback: false, fallback: ["Georgia", "serif"] });

export default function EntryLayout({ children }: { children: ReactNode }) {
	return <div className={`${styles.root} ${sans.variable} ${editorial.variable}`}>{children}</div>;
}
