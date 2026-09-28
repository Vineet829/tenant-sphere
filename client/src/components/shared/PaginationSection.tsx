import { setCurrentPage as setPostCurrentPage } from "@/lib/redux/features/posts/postSlice";
import { setCurrentPage as setUserCurrentPage } from "@/lib/redux/features/users/userSlice";
import { useAppDispatch, useAppSelector } from "@/lib/redux/hooks/typedHooks";
import { ChevronLeft, ChevronRight } from "lucide-react";

interface PaginationSectionProps {
	totalPages: number;
	entityType: "user" | "post";
}

const PaginationSection = ({ totalPages, entityType }: PaginationSectionProps) => {
	const dispatch = useAppDispatch();

	const currentPage = useAppSelector((state) => (entityType === "user" ? state.user.page : state.post.page));

	const setCurrentPageAction = entityType === "user" ? setUserCurrentPage : setPostCurrentPage;

	const goTo = (page: number) => {
		dispatch(setCurrentPageAction(page));
		window.scrollTo({ top: 0, behavior: "smooth" });
	};

	const handlePreviousClick = () => {
		if (currentPage > 1) goTo(currentPage - 1);
	};

	const handleNextClick = () => {
		if (currentPage < totalPages) goTo(currentPage + 1);
	};

	// A single page needs no controls, unless we are stranded beyond it.
	if (totalPages <= 1 && currentPage <= 1) return null;

	return (
		<nav className="es-pagination" aria-label="Pagination">
			<button type="button" onClick={handlePreviousClick} disabled={currentPage <= 1}>
				<ChevronLeft aria-hidden="true" />
				Previous
			</button>
			<p className="es-page-indicator" aria-live="polite">
				Page <strong>{currentPage}</strong> of {Math.max(totalPages, 1)}
			</p>
			<button type="button" onClick={handleNextClick} disabled={currentPage >= totalPages}>
				Next
				<ChevronRight aria-hidden="true" />
			</button>
		</nav>
	);
};

export default PaginationSection;
