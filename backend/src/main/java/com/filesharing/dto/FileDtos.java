package com.filesharing.dto;

import com.filesharing.model.FileItem;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.List;

public class FileDtos {

    public record CreateFolderRequest(
            @NotBlank String name,
            Long parentId
    ) {}

    public record RenameRequest(
            @NotBlank String newName
    ) {}

    public record MoveRequest(
            Long newParentId
    ) {}

    public record FileVersionResponse(
            Long id,
            int versionNumber,
            String originalFileName,
            String contentType,
            long sizeBytes,
            String uploadedByEmail,
            Instant uploadedAt
    ) {}

    public record FileItemResponse(
            Long id,
            String name,
            FileItem.Type type,
            Long parentId,
            Instant createdAt,
            Instant updatedAt,
            Long latestVersionId,
            Long sizeBytes,
            Integer versionCount
    ) {}

    public record FolderContentsResponse(
            Long folderId,
            String folderName,
            List<BreadcrumbEntry> breadcrumb,
            List<FileItemResponse> items
    ) {}

    public record BreadcrumbEntry(Long id, String name) {}
}
