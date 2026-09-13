package com.tenantsphere.admin;

import com.tenantsphere.apartment.Apartment;
import com.tenantsphere.apartment.ApartmentRepository;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import java.util.List;

@Route(value = "admin/apartments", layout = AdminLayout.class)
@PageTitle("Apartments | Tenant Sphere Admin")
@RolesAllowed({"STAFF", "SUPERUSER"})
public class ApartmentAdminView extends AdminGridView<Apartment> {

    private final ApartmentRepository apartmentRepository;

    public ApartmentAdminView(ApartmentRepository apartmentRepository) {
        super("Apartments", Apartment.class);
        this.apartmentRepository = apartmentRepository;

        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        grid.addColumn(Apartment::getId).setHeader("id").setAutoWidth(true);
        grid.addColumn(Apartment::getUnitNumber).setHeader("Unit number").setAutoWidth(true).setSortable(true);
        grid.addColumn(Apartment::getBuilding).setHeader("Building").setAutoWidth(true).setSortable(true);
        grid.addColumn(Apartment::getFloor).setHeader("Floor").setAutoWidth(true).setSortable(true);
        grid.addColumn(apartment -> apartment.getTenant() == null
                        ? "-" : apartment.getTenant().getUsername())
                .setHeader("Tenant").setAutoWidth(true);

        onSearch(this::refresh);
        refresh("");
    }

    private void refresh(String term) {
        List<Apartment> apartments = apartmentRepository.findAllBy().stream()
                .filter(apartment -> term.isBlank()
                        || apartment.getUnitNumber().toLowerCase().contains(term.toLowerCase()))
                .sorted((a, b) -> a.getBuilding().compareTo(b.getBuilding()))
                .toList();
        grid.setItems(apartments);
    }
}
