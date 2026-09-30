// Only permit local destinations; never trust a return URL supplied in a link.
export function safeLoginDestination(value: string | null): string {
	if (!value || !value.startsWith("/") || value.startsWith("//")) return "/welcome";
	if (Array.from(value).some(char => char === "\\" || char.charCodeAt(0) <= 32)) return "/welcome";
	const target = new URL(value, "https://tenantsphere.invalid");
	return target.origin === "https://tenantsphere.invalid" && !["/login", "/register"].includes(target.pathname)
		? `${target.pathname}${target.search}${target.hash}` : "/welcome";
}

export function loginDestination(): string {
	return safeLoginDestination(typeof window === "undefined" ? null : new URLSearchParams(window.location.search).get("next"));
}
