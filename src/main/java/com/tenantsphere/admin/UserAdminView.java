package com.tenantsphere.admin;

import com.tenantsphere.profile.Profile;
import com.tenantsphere.profile.ProfileRepository;
import com.tenantsphere.user.User;
import com.tenantsphere.user.UserRepository;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import java.util.List;
import com.tenantsphere.profile.Gender;
import com.tenantsphere.profile.Occupation;

@Route(value = "admin/users", layout = AdminLayout.class)
@PageTitle("Users | Tenant Sphere Admin")
@RolesAllowed({"STAFF", "SUPERUSER"})
public class UserAdminView extends AdminGridView<User> {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;

    public UserAdminView(UserRepository userRepository, ProfileRepository profileRepository) {
        super("Users", User.class);
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;

        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        grid.addColumn(User::getPkid).setHeader("pkid").setAutoWidth(true).setSortable(true);
        grid.addColumn(User::getId).setHeader("id").setAutoWidth(true);
        grid.addColumn(User::getEmail).setHeader("Email").setAutoWidth(true).setSortable(true);
        grid.addColumn(User::getFirstName).setHeader("First name").setAutoWidth(true);
        grid.addColumn(User::getLastName).setHeader("Last name").setAutoWidth(true);
        grid.addColumn(User::getUsername).setHeader("Username").setAutoWidth(true).setSortable(true);
        grid.addColumn(user -> user.isSuperuser() ? "yes" : "no")
                .setHeader("Superuser").setAutoWidth(true);
        grid.addColumn(user -> user.isStaff() ? "yes" : "no").setHeader("Staff").setAutoWidth(true);
        grid.addColumn(user -> user.isActive() ? "yes" : "no").setHeader("Active").setAutoWidth(true);

        grid.asSingleSelect().addValueChangeListener(event -> {
            if (event.getValue() != null) {
                openEditor(event.getValue());
            }
        });

        onSearch(this::refresh);
        refresh("");
    }

    private void refresh(String term) {
        List<User> users = userRepository.findAll().stream()
                .filter(user -> term.isBlank()
                        || user.getEmail().toLowerCase().contains(term.toLowerCase())
                        || user.getFirstName().toLowerCase().contains(term.toLowerCase())
                        || user.getLastName().toLowerCase().contains(term.toLowerCase()))
                .sorted((a, b) -> Long.compare(a.getPkid(), b.getPkid()))
                .toList();
        grid.setItems(users);
    }

    private void openEditor(User user) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Change user");
        dialog.setWidth("640px");

        Binder<User> userBinder = new Binder<>(User.class);

        TextField email = new TextField("Email");
        TextField username = new TextField("Username");
        TextField firstName = new TextField("First name");
        TextField lastName = new TextField("Last name");
        userBinder.forField(email).bind(User::getEmail, User::setEmail);
        userBinder.forField(username).bind(User::getUsername, User::setUsername);
        userBinder.forField(firstName).bind(User::getFirstName, User::setFirstName);
        userBinder.forField(lastName).bind(User::getLastName, User::setLastName);

        Checkbox active = new Checkbox("Active");
        Checkbox staff = new Checkbox("Staff status");
        Checkbox superuser = new Checkbox("Superuser status");
        userBinder.forField(active).bind(User::isActive, User::setActive);
        userBinder.forField(staff).bind(User::isStaff, User::setStaff);
        userBinder.forField(superuser).bind(User::isSuperuser, User::setSuperuser);

        Profile profile = profileRepository.findByUserPkid(user.getPkid()).orElse(null);
        Binder<Profile> profileBinder = new Binder<>(Profile.class);
        Select<Occupation> occupation = new Select<>();
        occupation.setLabel("Occupation");
        occupation.setItems(Occupation.values());
        occupation.setItemLabelGenerator(Occupation::getValue);
        Select<Gender> gender = new Select<>();
        gender.setLabel("Gender");
        gender.setItems(Gender.values());
        gender.setItemLabelGenerator(Gender::getValue);

        if (profile != null) {
            profileBinder.forField(occupation).bind(Profile::getOccupation, Profile::setOccupation);
            profileBinder.forField(gender).bind(Profile::getGender, Profile::setGender);
            profileBinder.readBean(profile);
        } else {
            occupation.setEnabled(false);
            gender.setEnabled(false);
        }

        userBinder.readBean(user);

        FormLayout credentials = new FormLayout(email, username);
        FormLayout personal = new FormLayout(firstName, lastName, gender, occupation);
        FormLayout permissions = new FormLayout(active, staff, superuser);

        VerticalLayout body = new VerticalLayout(
                new H4("Login credentials"), credentials,
                new H4("Personal info"), personal,
                new H4("Permissions"), permissions);
        body.setPadding(false);
        dialog.add(body);

        Button save = new Button("Save", event -> {
            try {
                userBinder.writeBean(user);
                userRepository.save(user);
                if (profile != null) {
                    profileBinder.writeBean(profile);
                    profileRepository.save(profile);
                }
                Notification.show("Saved " + user.getEmail());
                dialog.close();
                refresh("");
            } catch (Exception e) {
                Notification.show("Could not save: " + e.getMessage());
            }
        });
        Button cancel = new Button("Cancel", event -> dialog.close());
        dialog.getFooter().add(new HorizontalLayout(cancel, save));

        dialog.addOpenedChangeListener(event -> {
            if (!event.isOpened()) {
                grid.deselectAll();
            }
        });
        dialog.open();
    }
}
