import { setAuth } from "@/lib/redux/features/auth/authSlice";
import { useAppDispatch } from "@/lib/redux/hooks/typedHooks";
import { useRouter, useSearchParams } from "next/navigation";
import { useEffect, useRef } from "react";
import { toast } from "react-toastify";
import extractErrorMessage from "@/utils/extractErrorMessage";

export default function useSocialAuth(authenticate: any, provider: string) {
	const dispatch = useAppDispatch();
	const router = useRouter();
	const searchParams = useSearchParams();
	const started = useRef(false);
	useEffect(() => {
		if (started.current) return;
		started.current = true;
		const state = searchParams.get("state");
		const code = searchParams.get("code");
		if (!state || !code || searchParams.has("error")) {
			toast.error("Google sign-in was cancelled or could not be completed.");
			router.replace("/login");
			return;
		}
		authenticate({ provider, state, code }).unwrap().then(() => {
			dispatch(setAuth());
			toast.success("Logged in successfully");
			router.replace("/welcome");
		}).catch((error: unknown) => {
			toast.error(extractErrorMessage(error) || "Login failed. Please try again.");
			router.replace("/login");
		});
	}, [authenticate, dispatch, provider, router, searchParams]);
}
