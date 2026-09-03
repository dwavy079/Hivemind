package com.filesharing.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * One uploaded copy of a FILE-type FileItem. Re-uploading the same file creates a new
 * version rather than overwriting the S3 object, so old versions stay downloadable.
 */
@Entity
@Table(name = "file_version")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_item_id", nullable = false)
    private FileItem fileItem;

    @Column(nullable = false)
    private int versionNumber;

    /** Key of the object in the S3 bucket, e.g. users/3/files/17/v2-report.pdf */
    @Column(nullable = false)
    private String s3Key;

    @Column(nullable = false)
    private String originalFileName;

    @Column(nullable = false)
    private String contentType;

    @Column(nullable = false)
    private long sizeBytes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by_id", nullable = false)
    private User uploadedBy;

    @Column(nullable = false, updatable = false)
    private Instant uploadedAt;

    @PrePersist
    void onCreate() {
        this.uploadedAt = Instant.now();
    }
}
