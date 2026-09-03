package com.filesharing.controller;

import com.filesharing.dto.FileDtos.*;
import com.filesharing.model.FileVersion;
import com.filesharing.model.User;
import com.filesharing.service.FileService;
import com.filesharing.service.FileService.DownloadPayload;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @GetMapping
    public ResponseEntity<FolderContentsResponse> list(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) Long folderId
    ) {
        return ResponseEntity.ok(fileService.listFolder(user, folderId));
    }

    @PostMapping("/folders")
    public ResponseEntity<FileItemResponse> createFolder(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CreateFolderRequest request
    ) {
        return ResponseEntity.ok(fileService.createFolder(user, request));
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FileItemResponse> upload(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) Long parentId,
            @RequestParam(required = false) String name,
            @RequestParam("file") MultipartFile file
    ) {
        return ResponseEntity.ok(fileService.uploadFile(user, parentId, name, file));
    }

    @GetMapping("/{id}/versions")
    public ResponseEntity<List<FileVersionResponse>> versions(
            @AuthenticationPrincipal User user,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(fileService.listVersions(user, id));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<InputStreamResource> download(
            @AuthenticationPrincipal User user,
            @PathVariable Long id
    ) {
        DownloadPayload payload = fileService.downloadLatest(user, id);
        return streamResponse(payload);
    }

    @GetMapping("/{id}/versions/{versionId}/download")
    public ResponseEntity<InputStreamResource> downloadVersion(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @PathVariable Long versionId
    ) {
        DownloadPayload payload = fileService.downloadVersion(user, id, versionId);
        return streamResponse(payload);
    }

    @PatchMapping("/{id}/rename")
    public ResponseEntity<FileItemResponse> rename(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @Valid @RequestBody RenameRequest request
    ) {
        return ResponseEntity.ok(fileService.rename(user, id, request.newName()));
    }

    @PatchMapping("/{id}/move")
    public ResponseEntity<FileItemResponse> move(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @RequestBody MoveRequest request
    ) {
        return ResponseEntity.ok(fileService.move(user, id, request.newParentId()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal User user, @PathVariable Long id) {
        fileService.delete(user, id);
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<InputStreamResource> streamResponse(DownloadPayload payload) {
        FileVersion version = payload.version();
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(version.getOriginalFileName(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.parseMediaType(version.getContentType()))
                .contentLength(version.getSizeBytes())
                .body(new InputStreamResource(payload.content()));
    }
}
