package com.tenantsphere.admin;

import com.tenantsphere.profile.Profile;
import com.tenantsphere.profile.ProfileRepository;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import java.util.List;

@Route(value = "admin/profiles", layout = AdminLayout.class)
@PageTitle("Profiles | Tenant Sphere Admin")
@RolesAllowed({"STAFF", "SUPERUSER"})
public class ProfileAdminView extends AdminGridView<Profile> {

    private final ProfileRepository profileRepository;

    public ProfileAdminView(ProfileRepository profileRepository) {
        super("Profiles", Profile.class);
        this.profileRepository = profileRepository;

        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        grid.addColumn(Profile::getId).setHeader("id").setAutoWidth(true);
        grid.addColumn(profile -> profile.getUser().getUsername())
                .setHeader("User").setAutoWidth(true).setSortable(true);
        grid.addColumn(profile -> profile.getGender().getValue())
                .setHeader("Gender").setAutoWidth(true).setSortable(true);
        grid.addColumn(profile -> profile.getOccupation().getValue())
                .setHeader("Occupation").setAutoWidth(true).setSortable(true);
        grid.addColumn(Profile::getReputation).setHeader("Reputation").setAutoWidth(true).setSortable(true);
        grid.addColumn(Profile::getReportCount).setHeader("Report count").setAutoWidth(true).setSortable(true);
        grid.addColumn(Profile::getSlug).setHeader("Slug").setAutoWidth(true);

        onSearch(this::refresh);
        refresh("");
    }

    private void refresh(String term) {
        List<Profile> profiles = profileRepository.findAllBy().stream()
                .filter(profile -> term.isBlank()
                        || profile.getUser().getUsername().toLowerCase().contains(term.toLowerCase()))
                .toList();
        grid.setItems(profiles);
    }
}
