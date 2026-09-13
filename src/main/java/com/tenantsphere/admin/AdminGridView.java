package com.tenantsphere.admin;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import java.util.function.Consumer;

public abstract class AdminGridView<T> extends VerticalLayout {

    protected final Grid<T> grid;
    private final TextField search = new TextField();

    protected AdminGridView(String title, Class<T> beanType) {
        this.grid = new Grid<>(beanType, false);

        setSizeFull();
        add(new H2(title));

        search.setPlaceholder("Search");
        search.setClearButtonVisible(true);
        search.setValueChangeMode(ValueChangeMode.LAZY);
        search.setWidth("320px");
        add(search);

        grid.setSizeFull();
        grid.setColumnReorderingAllowed(true);
        add(grid);
        expand(grid);
    }

    protected void onSearch(Consumer<String> handler) {
        search.addValueChangeListener(event -> handler.accept(event.getValue()));
    }
}
