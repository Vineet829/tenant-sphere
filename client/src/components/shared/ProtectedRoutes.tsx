"use client";
import Spinner from "@/components/shared/Spinner";
import { setAuth, setLogout } from "@/lib/redux/features/auth/authSlice";
import { useAppDispatch } from "@/lib/redux/hooks/typedHooks";
import { getCookie } from "cookies-next";
import { useRouter } from "next/navigation";
import React, { useEffect, useState } from "react";

function ProtectedRoute({ children }: { children: React.ReactNode }) {
	const dispatch = useAppDispatch();
	const router = useRouter();
	const [isLoading, setIsLoading] = useState(true);

	useEffect(() => {
		const handleAuthState = async () => {
			const isLoggedIn = getCookie("logged_in") === "true";

			if (isLoggedIn) {
				dispatch(setAuth());
			} else {
				dispatch(setLogout());
				router.push("/login");
			}
			setIsLoading(false);
		};
		handleAuthState();
	}, [dispatch, router]);

	if (isLoading) {
		return (
			<div className="es-gate">
				<Spinner size="md" label="Checking your session…" />
			</div>
		);
	}

	return <>{children}</>;
}

export default ProtectedRoute;
