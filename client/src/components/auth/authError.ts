export function authErrorMessage(error: unknown): string {
	const fallback = "Something went wrong. Please try again.";
	if (!error || typeof error !== "object") return fallback;
	if ("status" in error && error.status === "FETCH_ERROR") {
		return "We couldn’t connect. Check your connection and try again.";
	}
	if (!("data" in error) || !error.data || typeof error.data !== "object") return fallback;
	const data = error.data as Record<string, unknown>;
	if (typeof data.detail === "string") return data.detail;
	if (typeof data.message === "string") return data.message;
	const messages: string[] = [];
	for (const [field, value] of Object.entries(data)) {
		if (["status_code", "status", "timestamp", "path"].includes(field)) continue;
		if (typeof value === "string") messages.push(value);
		if (Array.isArray(value)) messages.push(...value.filter((item): item is string => typeof item === "string"));
		if (value && typeof value === "object" && !Array.isArray(value)) {
			for (const nested of Object.values(value)) {
				if (typeof nested === "string") messages.push(nested);
				if (Array.isArray(nested)) messages.push(...nested.filter((item): item is string => typeof item === "string"));
			}
		}
	}
	return messages.join(" ") || fallback;
}
