import { openSans, robotoSlab } from "@/lib/fonts";
import type { Metadata } from "next";
import React from "react";
import "./globals.css";

import Toast from "@/components/shared/Toast";
import ReduxProvider from "@/lib/redux/provider";
import { PersistAuth } from "@/utils";


export const metadata: Metadata = {
	title: "Home | Tenant Sphere",
	description: "Welcome home",
};

export default function RootLayout({
	children,
}: Readonly<{
	children: React.ReactNode;
}>) {
	return (
		<html lang="en" className="dark [color-scheme:dark]">
			<body className={`${openSans.variable} ${robotoSlab.variable}`}>
				<Toast />
				<ReduxProvider>
					<PersistAuth />
					{children}
				</ReduxProvider>
			</body>
		</html>
	);
}
