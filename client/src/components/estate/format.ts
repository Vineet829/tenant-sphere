import { format, formatDistanceToNow, isValid, parseISO } from "date-fns";

function toDate(value?: string | null) {
	if (!value) return null;
	const parsed = parseISO(value);
	if (isValid(parsed)) return parsed;
	const fallback = new Date(value);
	return isValid(fallback) ? fallback : null;
}

/** "3 days ago" */
export function timeAgo(value?: string | null) {
	const date = toDate(value);
	return date ? formatDistanceToNow(date, { addSuffix: true }) : "";
}

/** "24 Sep 2026" */
export function shortDate(value?: string | null) {
	const date = toDate(value);
	return date ? format(date, "d MMM yyyy") : "";
}

/** "24 September 2026" */
export function longDate(value?: string | null) {
	const date = toDate(value);
	return date ? format(date, "d MMMM yyyy") : "";
}

/** "September 2026" */
export function monthYear(value?: string | null) {
	const date = toDate(value);
	return date ? format(date, "MMMM yyyy") : "";
}

export function plural(count: number | undefined, one: string, many = `${one}s`) {
	const total = count ?? 0;
	return `${total} ${total === 1 ? one : many}`;
}

/** Net score stays signed: a downvote on a zero-score post produces -1. */
export function voteScore({ upvotes = 0, downvotes = 0 }: { upvotes?: number; downvotes?: number }) {
	return upvotes - downvotes;
}

/** Mirrors the slug the API stores for a tag name. */
export function tagSlug(name: string) {
	return name
		.normalize("NFKD")
		.replace(/\p{M}/gu, "")
		.toLowerCase()
		.replace(/[^a-z0-9]+/g, "-")
		.replace(/(^-|-$)/g, "");
}

const UPPERCASE = new Set(["hvac"]);

/** "plumber" → "Plumber", "hvac" → "HVAC", "in_progress" → "In progress" */
export function label(value?: string | null) {
	if (!value) return "";
	if (UPPERCASE.has(value.toLowerCase())) return value.toUpperCase();
	const text = value.replace(/_/g, " ");
	return text.charAt(0).toUpperCase() + text.slice(1);
}

/** Joins the parts that exist: place("Pune", "India") → "Pune, India" */
export function joined(parts: Array<string | number | null | undefined>, separator = ", ") {
	return parts.filter((part) => part !== null && part !== undefined && `${part}`.trim() !== "").join(separator);
}
