package com.filesharing.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * A public, unguessable link to one FileItem's latest (or a pinned) version.
 * Anyone with the token can download it until it expires or is revoked.
 */
@Entity
@Table(name = "share_link")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShareLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 43)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_item_id", nullable = false)
    private FileItem fileItem;

    /** If set, the link always serves this specific version rather than "whatever is latest". */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pinned_version_id")
    private FileVersion pinnedVersion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id", nullable = false)
    private User createdBy;

    /** Null means "never expires". */
    private Instant expiresAt;

    @Builder.Default
    private boolean revoked = false;

    @Builder.Default
    private long downloadCount = 0;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    @Transient
    public boolean isActive() {
        return !revoked && (expiresAt == null || expiresAt.isAfter(Instant.now()));
    }
}
