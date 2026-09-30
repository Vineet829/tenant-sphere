"use client";

import Spinner from "@/components/shared/Spinner";
import { useActivateUserMutation } from "@/lib/redux/features/auth/authApiSlice";
import { CircleCheck, Info } from "lucide-react";
import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { useEffect, useRef } from "react";
import { toast } from "react-toastify";

export default function ActivationPage() {
	const { uid, token } = useParams<{ uid: string; token: string }>();
	const started = useRef(false);
	const router = useRouter();
	const [activateUser, { isSuccess, isError, error }] = useActivateUserMutation();

	useEffect(() => {
		if (started.current) return;
		started.current = true;
		activateUser({ uid, token });
	}, [activateUser, uid, token]);

	useEffect(() => {
		if (isSuccess) {
			toast.success("Account activated successfully!");
			router.push("/login");
		} else if (isError && error) {
			toast.error("Failed to activate your account");
		}
	}, [isSuccess, isError, error, router]);

	return (
		<div className="es-page es-page--wide">
			<div className="es-statuspage" aria-live="polite">
				{isSuccess ? (
					<>
						<span className="es-empty-icon" data-tone="ok" aria-hidden="true">
							<CircleCheck strokeWidth={1.5} />
						</span>
						<h1 className="es-statuspage-title">Your account is active</h1>
						<p className="es-statuspage-text">Taking you to log in.</p>
					</>
				) : isError ? (
					<>
						<span className="es-empty-icon" aria-hidden="true">
							<Info strokeWidth={1.5} />
						</span>
						<h1 className="es-statuspage-title">We could not activate this account</h1>
						<p className="es-statuspage-text">
							The link may have expired or been used already. If you activated your account before, you can log
							in as usual.
						</p>
						<Link href="/login" className="es-btn es-btn--primary">
							Go to log in
						</Link>
					</>
				) : (
					<>
						<Spinner size="lg" label="Activating your account…" />
						<h1 className="es-statuspage-title">Activating your account</h1>
						<p className="es-statuspage-text">This only takes a moment.</p>
					</>
				)}
			</div>
		</div>
	);
}
