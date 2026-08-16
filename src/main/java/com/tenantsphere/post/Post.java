package com.tenantsphere.post;

import com.tenantsphere.common.BaseEntity;
import com.tenantsphere.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "posts")
@Getter
@Setter
public class Post extends BaseEntity {

    @Column(name = "title", nullable = false, length = 250)
    private String title;

    @Column(name = "slug", nullable = false, unique = true)
    private String slug;

    @Column(name = "body", nullable = false)
    private String body;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_pkid", nullable = false)
    private User author;

    @Column(name = "upvotes", nullable = false)
    private int upvotes = 0;

    @Column(name = "downvotes", nullable = false)
    private int downvotes = 0;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "post_tags",
            joinColumns = @JoinColumn(name = "post_pkid"),
            inverseJoinColumns = @JoinColumn(name = "tag_pkid"))
    @OrderBy("name ASC")
    private Set<Tag> tags = new LinkedHashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "post_upvotes",
            joinColumns = @JoinColumn(name = "post_pkid"),
            inverseJoinColumns = @JoinColumn(name = "user_pkid"))
    private Set<User> upvotedBy = new LinkedHashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "post_downvotes",
            joinColumns = @JoinColumn(name = "post_pkid"),
            inverseJoinColumns = @JoinColumn(name = "user_pkid"))
    private Set<User> downvotedBy = new LinkedHashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "post_bookmarks",
            joinColumns = @JoinColumn(name = "post_pkid"),
            inverseJoinColumns = @JoinColumn(name = "user_pkid"))
    private Set<User> bookmarkedBy = new LinkedHashSet<>();

    @OneToMany(mappedBy = "post", fetch = FetchType.LAZY)
    @OrderBy("createdAt ASC")
    private List<Reply> replies = new java.util.ArrayList<>();
}
