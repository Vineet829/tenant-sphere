import { ChevronLeft, ChevronRight } from "lucide-react";

interface Props {
	page: number;
	totalPages: number;
	onChange: (page: number) => void;
	pending?: boolean;
}

export default function PageControls({ page, totalPages, onChange, pending = false }: Props) {
	if (totalPages <= 1 && page <= 1) return null;
	return <nav className="es-pagination" aria-label="Pagination">
		<button type="button" onClick={() => onChange(page - 1)} disabled={pending || page <= 1}>
			<ChevronLeft aria-hidden="true" />Previous
		</button>
		<p className="es-page-indicator" aria-live="polite">Page <strong>{page}</strong> of {Math.max(totalPages, 1)}</p>
		<button type="button" onClick={() => onChange(page + 1)} disabled={pending || page >= totalPages}>
			Next<ChevronRight aria-hidden="true" />
		</button>
	</nav>;
}
