"use client";

import Spinner from "@/components/shared/Spinner";
import { useAppSelector } from "@/lib/redux/hooks/typedHooks";
import { usePathname, useRouter } from "next/navigation";
import { useEffect, type ReactNode } from "react";

export default function ProtectedRoute({ children }: { children: ReactNode }) {
	const { isAuthenticated, isInitialized } = useAppSelector((state) => state.auth);
	const router = useRouter();
	const pathname = usePathname();
	useEffect(() => {
		if (isInitialized && !isAuthenticated) {
			const destination = pathname + window.location.search;
			router.replace(`/login?next=${encodeURIComponent(destination)}`);
		}
	}, [isAuthenticated, isInitialized, pathname, router]);
	if (!isInitialized || !isAuthenticated) {
		return <div className="es-gate"><Spinner size="md" label="Checking your session…" /></div>;
	}
	return <>{children}</>;
}
