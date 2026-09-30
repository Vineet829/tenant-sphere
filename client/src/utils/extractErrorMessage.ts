export default function extractErrorMessage(error: unknown): string | null {
	if (typeof error !== "object" || error === null || !("data" in error)) return null;
	const data: unknown = error.data;
	if (typeof data !== "object" || data === null || Array.isArray(data)) return null;
	if ("detail" in data && typeof data.detail === "string") return data.detail;
	const messages: string[] = [];
	for (const [key, value] of Object.entries(data)) {
		if (key === "status_code") continue;
		if (Array.isArray(value)) messages.push(...value.filter((message): message is string => typeof message === "string"));
		else if (typeof value === "object" && value !== null) {
			if ("detail" in value && typeof value.detail === "string") messages.push(value.detail);
			for (const nested of Object.values(value)) {
				if (Array.isArray(nested)) messages.push(...nested.filter((message): message is string => typeof message === "string"));
			}
		}
	}
	return messages.length ? messages.join(", ") : null;
}
