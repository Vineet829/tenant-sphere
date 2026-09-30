import { toast } from "react-toastify";

interface SocialAuthResponse {
	authorization_url: string;
}

export default async function InitiateSocialAuth(
	provider: string,
	redirect: string,
) {
	
	try {
		const callback = `${window.location.origin}/${redirect}`;
		const url = `/api/v1/auth/o/${encodeURIComponent(provider)}/?redirect_uri=${encodeURIComponent(callback)}`;

		const res = await fetch(url, {
			method: "GET",
			headers: {
				Accept: "application/json",
			},
			credentials: "include",
		});

		const data: SocialAuthResponse = await res.json();

		if (res.status === 200 && typeof window !== "undefined") {
			window.location.replace(data.authorization_url);
		} else {
			toast.error("Something went wrong with social authentication");
		}
	} catch (e) {
		console.error(e);
		toast.error("An error occurred during social authentication");
	}
}
