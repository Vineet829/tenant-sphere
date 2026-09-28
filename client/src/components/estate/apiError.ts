/** True when the API said the thing does not exist, rather than failing to answer. */
export function isMissing(error: unknown) {
	if (!error || typeof error !== "object" || !("status" in error)) return false;
	const status = (error as { status: unknown }).status;
	return status === 404 || status === 400;
}
