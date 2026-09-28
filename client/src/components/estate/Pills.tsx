const STATUS: Record<string, { label: string; tone: string }> = {
	reported: { label: "Reported", tone: "info" },
	in_progress: { label: "In progress", tone: "warn" },
	resolved: { label: "Resolved", tone: "ok" },
};

const PRIORITY: Record<string, { label: string; tone?: string }> = {
	low: { label: "Low priority" },
	medium: { label: "Medium priority", tone: "warn" },
	high: { label: "High priority", tone: "danger" },
};

function humanise(value = "") {
	const text = value.replace(/_/g, " ");
	return text.charAt(0).toUpperCase() + text.slice(1);
}

export function StatusPill({ status }: { status?: string }) {
	const entry = STATUS[status ?? ""] ?? { label: humanise(status), tone: "info" };
	return (
		<span className="es-pill" data-tone={entry.tone} data-pulse={status === "in_progress"}>
			{entry.label}
		</span>
	);
}

export function PriorityPill({ priority }: { priority?: string }) {
	const entry = PRIORITY[priority ?? ""] ?? { label: humanise(priority) };
	return (
		<span className="es-pill" data-tone={entry.tone}>
			{entry.label}
		</span>
	);
}
