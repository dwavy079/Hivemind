package com.filesharing.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * A node in the user's file tree. Either a FOLDER (a grouping container, no S3 object)
 * or a FILE (has one or more FileVersion rows, the latest being what downloads/shares point to).
 */
@Entity
@Table(name = "file_item", indexes = {
        @Index(name = "idx_file_item_owner_parent", columnList = "owner_id, parent_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileItem {

    public enum Type { FILE, FOLDER }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Type type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    /** Null means "lives at the root of the user's space". */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private FileItem parent;

    @OneToMany(mappedBy = "fileItem", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<FileVersion> versions = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    @Transient
    public FileVersion latestVersion() {
        return versions.stream()
                .max((a, b) -> Integer.compare(a.getVersionNumber(), b.getVersionNumber()))
                .orElse(null);
    }
}
