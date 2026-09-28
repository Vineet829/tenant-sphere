import { useUserProfile } from "@/hooks/useUseProfile";

/**
 * Posts and replies only carry a username. When that username is the signed-in
 * person, we know their full name too, so their avatar reads the same everywhere.
 */
export function useDisplayName() {
	const { profile } = useUserProfile();

	return (username?: string) => {
		if (username && profile?.username === username && profile.full_name) return profile.full_name;
		return username ?? "";
	};
}
