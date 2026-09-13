package com.tenantsphere.admin;

import com.tenantsphere.common.ContentView;
import com.tenantsphere.common.ContentViewRepository;
import com.tenantsphere.post.Post;
import com.tenantsphere.post.PostRepository;
import com.tenantsphere.post.PostService;
import com.tenantsphere.post.Tag;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import java.util.List;
import java.util.stream.Collectors;

@Route(value = "admin/posts", layout = AdminLayout.class)
@PageTitle("Posts | Tenant Sphere Admin")
@RolesAllowed({"STAFF", "SUPERUSER"})
public class PostAdminView extends AdminGridView<Post> {

    private final PostRepository postRepository;
    private final ContentViewRepository contentViewRepository;

    public PostAdminView(
            PostRepository postRepository, ContentViewRepository contentViewRepository) {
        super("Posts", Post.class);
        this.postRepository = postRepository;
        this.contentViewRepository = contentViewRepository;

        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        grid.addColumn(Post::getTitle).setHeader("Title").setAutoWidth(true).setSortable(true);
        grid.addColumn(this::tagList).setHeader("Tag list").setAutoWidth(true);
        grid.addColumn(this::totalViews).setHeader("Total Views").setAutoWidth(true);
        grid.addColumn(post -> post.getAuthor().getUsername())
                .setHeader("Author").setAutoWidth(true).setSortable(true);
        grid.addColumn(Post::getUpvotes).setHeader("Upvotes").setAutoWidth(true).setSortable(true);

        grid.setItemDetailsRenderer(new com.vaadin.flow.data.renderer.ComponentRenderer<>(
                this::contentViewInline));

        onSearch(this::refresh);
        refresh("");
    }

    private String tagList(Post post) {
        return post.getTags().stream().map(Tag::getName).collect(Collectors.joining(", "));
    }

    private long totalViews(Post post) {
        return contentViewRepository.countByContentTypeAndObjectPkid(
                PostService.CONTENT_TYPE, post.getPkid());
    }

    private VerticalLayout contentViewInline(Post post) {
        List<ContentView> views = contentViewRepository
                .findByContentTypeAndObjectPkidOrderByLastViewedDesc(
                        PostService.CONTENT_TYPE, post.getPkid());

        Grid<ContentView> inline = new Grid<>(ContentView.class, false);
        inline.addColumn(view -> view.getUser() == null ? "Anonymous" : view.getUser().getUsername())
                .setHeader("User").setAutoWidth(true);
        inline.addColumn(ContentView::getViewerIp).setHeader("Viewer IP").setAutoWidth(true);
        inline.addColumn(ContentView::getCreatedAt).setHeader("Created at").setAutoWidth(true);
        inline.setItems(views);
        inline.setAllRowsVisible(true);

        VerticalLayout wrapper = new VerticalLayout(new H4("Content views"), inline);
        wrapper.setPadding(false);
        return wrapper;
    }

    private void refresh(String term) {
        List<Post> posts = postRepository.findAllBy().stream()
                .filter(post -> term.isBlank()
                        || post.getTitle().toLowerCase().contains(term.toLowerCase()))
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .toList();
        grid.setItems(posts);
    }
}
