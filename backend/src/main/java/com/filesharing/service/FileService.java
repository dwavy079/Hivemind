package com.filesharing.service;

import com.filesharing.dto.FileDtos.*;
import com.filesharing.exception.ApiExceptions.*;
import com.filesharing.model.FileItem;
import com.filesharing.model.FileVersion;
import com.filesharing.model.User;
import com.filesharing.repository.FileItemRepository;
import com.filesharing.repository.FileVersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FileService {

    private final FileItemRepository fileItemRepository;
    private final FileVersionRepository fileVersionRepository;
    private final S3StorageService s3StorageService;

    // ---------- Browsing ----------

    @Transactional(readOnly = true)
    public FolderContentsResponse listFolder(User owner, Long folderId) {
        FileItem folder = null;
        List<BreadcrumbEntry> breadcrumb = new ArrayList<>();

        if (folderId != null) {
            folder = getOwnedFolder(owner, folderId);
            for (FileItem f = folder; f != null; f = f.getParent()) {
                breadcrumb.add(0, new BreadcrumbEntry(f.getId(), f.getName()));
            }
        }

        List<FileItem> items = (folderId == null)
                ? fileItemRepository.findByOwnerAndParentIsNullOrderByTypeAscNameAsc(owner)
                : fileItemRepository.findByOwnerAndParent_IdOrderByTypeAscNameAsc(owner, folderId);

        List<FileItemResponse> responses = items.stream().map(this::toResponse).toList();

        return new FolderContentsResponse(
                folderId,
                folder != null ? folder.getName() : "My Files",
                breadcrumb,
                responses
        );
    }

    // ---------- Folders ----------

    @Transactional
    public FileItemResponse createFolder(User owner, CreateFolderRequest request) {
        FileItem parent = request.parentId() != null ? getOwnedFolder(owner, request.parentId()) : null;

        boolean exists = parent != null
                ? fileItemRepository.existsByOwnerAndParentAndNameAndType(owner, parent, request.name(), FileItem.Type.FOLDER)
                : fileItemRepository.existsByOwnerAndParentIsNullAndNameAndType(owner, request.name(), FileItem.Type.FOLDER);
        if (exists) {
            throw new ConflictException("A folder named \"" + request.name() + "\" already exists here");
        }

        FileItem folder = FileItem.builder()
                .name(request.name())
                .type(FileItem.Type.FOLDER)
                .owner(owner)
                .parent(parent)
                .build();

        return toResponse(fileItemRepository.save(folder));
    }

    // ---------- Upload / versions ----------

    @Transactional
    public FileItemResponse uploadFile(User owner, Long parentId, String desiredName, MultipartFile multipartFile) {
        FileItem parent = parentId != null ? getOwnedFolder(owner, parentId) : null;
        String name = (desiredName != null && !desiredName.isBlank()) ? desiredName : multipartFile.getOriginalFilename();

        FileItem fileItem = findExistingFile(owner, parent, name);
        boolean isNewFile = fileItem == null;
        if (isNewFile) {
            fileItem = FileItem.builder()
                    .name(name)
                    .type(FileItem.Type.FILE)
                    .owner(owner)
                    .parent(parent)
                    .build();
            fileItem = fileItemRepository.save(fileItem);
        }

        int nextVersionNumber = fileVersionRepository.countByFileItem(fileItem) + 1;
        String key = "users/%d/files/%d/v%d-%s".formatted(
                owner.getId(), fileItem.getId(), nextVersionNumber, sanitize(name));

        try (InputStream in = multipartFile.getInputStream()) {
            s3StorageService.upload(key, in, multipartFile.getSize(), safeContentType(multipartFile));
        } catch (IOException e) {
            throw new BadRequestException("Could not read the uploaded file");
        }

        FileVersion version = FileVersion.builder()
                .fileItem(fileItem)
                .versionNumber(nextVersionNumber)
                .s3Key(key)
                .originalFileName(multipartFile.getOriginalFilename())
                .contentType(safeContentType(multipartFile))
                .sizeBytes(multipartFile.getSize())
                .uploadedBy(owner)
                .build();
        fileVersionRepository.save(version);

        if (!isNewFile) {
            fileItem.setUpdatedAt(java.time.Instant.now());
            fileItemRepository.save(fileItem);
        }

        fileItem.getVersions().add(version);
        return toResponse(fileItem);
    }

    @Transactional(readOnly = true)
    public List<FileVersionResponse> listVersions(User owner, Long fileItemId) {
        FileItem fileItem = getOwnedFile(owner, fileItemId);
        return fileVersionRepository.findByFileItemOrderByVersionNumberDesc(fileItem).stream()
                .map(v -> new FileVersionResponse(
                        v.getId(), v.getVersionNumber(), v.getOriginalFileName(), v.getContentType(),
                        v.getSizeBytes(), v.getUploadedBy().getEmail(), v.getUploadedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public DownloadPayload downloadLatest(User owner, Long fileItemId) {
        FileItem fileItem = getOwnedFile(owner, fileItemId);
        FileVersion latest = fileItem.latestVersion();
        if (latest == null) throw new NotFoundException("This file has no uploaded content yet");
        return new DownloadPayload(s3StorageService.download(latest.getS3Key()), latest);
    }

    @Transactional(readOnly = true)
    public DownloadPayload downloadVersion(User owner, Long fileItemId, Long versionId) {
        FileItem fileItem = getOwnedFile(owner, fileItemId);
        FileVersion version = fileVersionRepository.findByFileItemOrderByVersionNumberDesc(fileItem).stream()
                .filter(v -> v.getId().equals(versionId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Version not found"));
        return new DownloadPayload(s3StorageService.download(version.getS3Key()), version);
    }

    // ---------- Rename / move / delete ----------

    @Transactional
    public FileItemResponse rename(User owner, Long id, String newName) {
        FileItem item = getOwned(owner, id);
        item.setName(newName);
        return toResponse(fileItemRepository.save(item));
    }

    @Transactional
    public FileItemResponse move(User owner, Long id, Long newParentId) {
        FileItem item = getOwned(owner, id);
        FileItem newParent = newParentId != null ? getOwnedFolder(owner, newParentId) : null;

        if (item.getType() == FileItem.Type.FOLDER && newParent != null) {
            for (FileItem p = newParent; p != null; p = p.getParent()) {
                if (p.getId().equals(item.getId())) {
                    throw new BadRequestException("Cannot move a folder into its own subfolder");
                }
            }
        }
        item.setParent(newParent);
        return toResponse(fileItemRepository.save(item));
    }

    @Transactional
    public void delete(User owner, Long id) {
        FileItem item = getOwned(owner, id);
        deleteRecursive(item);
    }

    private void deleteRecursive(FileItem item) {
        if (item.getType() == FileItem.Type.FOLDER) {
            List<FileItem> children = fileItemRepository.findByOwnerAndParent_IdOrderByTypeAscNameAsc(item.getOwner(), item.getId());
            for (FileItem child : children) {
                deleteRecursive(child);
            }
        } else {
            for (FileVersion v : fileVersionRepository.findByFileItemOrderByVersionNumberDesc(item)) {
                s3StorageService.delete(v.getS3Key());
            }
        }
        fileItemRepository.delete(item);
    }

    // ---------- Helpers ----------

    FileItem getOwnedFolder(User owner, Long id) {
        FileItem item = getOwned(owner, id);
        if (item.getType() != FileItem.Type.FOLDER) throw new BadRequestException("Not a folder");
        return item;
    }

    FileItem getOwnedFile(User owner, Long id) {
        FileItem item = getOwned(owner, id);
        if (item.getType() != FileItem.Type.FILE) throw new BadRequestException("Not a file");
        return item;
    }

    private FileItem getOwned(User owner, Long id) {
        return fileItemRepository.findByIdAndOwner(id, owner)
                .orElseThrow(() -> new NotFoundException("File or folder not found"));
    }

    private FileItem findExistingFile(User owner, FileItem parent, String name) {
        List<FileItem> siblings = (parent == null)
                ? fileItemRepository.findByOwnerAndParentIsNullOrderByTypeAscNameAsc(owner)
                : fileItemRepository.findByOwnerAndParent_IdOrderByTypeAscNameAsc(owner, parent.getId());
        return siblings.stream()
                .filter(i -> i.getType() == FileItem.Type.FILE && i.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    private String sanitize(String name) {
        return name == null ? "file" : name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String safeContentType(MultipartFile file) {
        String ct = file.getContentType();
        return (ct == null || ct.isBlank()) ? "application/octet-stream" : ct;
    }

    FileItemResponse toResponse(FileItem item) {
        FileVersion latest = item.latestVersion();
        return new FileItemResponse(
                item.getId(),
                item.getName(),
                item.getType(),
                item.getParent() != null ? item.getParent().getId() : null,
                item.getCreatedAt(),
                item.getUpdatedAt(),
                latest != null ? latest.getId() : null,
                latest != null ? latest.getSizeBytes() : null,
                item.getType() == FileItem.Type.FILE ? item.getVersions().size() : null
        );
    }

    public record DownloadPayload(InputStream content, FileVersion version) {}

    /** Used by ShareLinkService, which already resolved a specific version. */
    public InputStream openStream(FileVersion version) {
        return s3StorageService.download(version.getS3Key());
    }
}
