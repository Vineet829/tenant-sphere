package com.tenantsphere.admin;

import com.tenantsphere.rating.Rating;
import com.tenantsphere.rating.RatingRepository;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;

@Route(value = "admin/ratings", layout = AdminLayout.class)
@PageTitle("Ratings | Tenant Sphere Admin")
@RolesAllowed({"STAFF", "SUPERUSER"})
public class RatingAdminView extends AdminGridView<RatingAdminView.Row> {

    public record Row(Rating rating, Double averageRating) {}

    private final RatingRepository ratingRepository;

    public RatingAdminView(RatingRepository ratingRepository) {
        super("Ratings", Row.class);
        this.ratingRepository = ratingRepository;

        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        grid.addColumn(row -> row.rating().getRatedUser().getUsername())
                .setHeader("Rated user").setAutoWidth(true).setSortable(true);
        grid.addColumn(row -> row.rating().getRatingUser().getUsername())
                .setHeader("Rating user").setAutoWidth(true).setSortable(true);
        grid.addColumn(row -> row.rating().getRating())
                .setHeader("Rating").setAutoWidth(true).setSortable(true);
        grid.addColumn(row -> row.rating().getComment()).setHeader("Comment").setAutoWidth(true);
        grid.addColumn(this::formatAverage)
                .setHeader("Average Rating")
                .setAutoWidth(true)
                .setSortable(true)
                .setComparator(Comparator.comparing(
                        row -> row.averageRating() == null ? 0d : row.averageRating()));

        onSearch(this::refresh);
        refresh("");
    }

    private String formatAverage(Row row) {
        if (row.averageRating() == null) {
            return "-";
        }
        return BigDecimal.valueOf(row.averageRating())
                .setScale(2, RoundingMode.HALF_UP)
                .toPlainString();
    }

    private void refresh(String term) {
        List<Row> rows = ratingRepository.findAllBy().stream()
                .filter(rating -> term.isBlank()
                        || rating.getRatedUser().getUsername().toLowerCase().contains(term.toLowerCase())
                        || rating.getRatingUser().getUsername().toLowerCase().contains(term.toLowerCase()))
                .map(rating -> new Row(
                        rating,
                        ratingRepository.averageForUser(rating.getRatedUser().getPkid()).orElse(null)))
                .toList();
        grid.setItems(rows);
    }
}
