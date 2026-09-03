package com.filesharing.controller;

import com.filesharing.dto.ShareDtos.*;
import com.filesharing.model.FileVersion;
import com.filesharing.model.User;
import com.filesharing.service.FileService.DownloadPayload;
import com.filesharing.service.ShareLinkService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class ShareController {

    private final ShareLinkService shareLinkService;

    // ---- Authenticated: managing your own share links ----

    @PostMapping("/api/v1/files/share-links")
    public ResponseEntity<ShareLinkResponse> create(
            @AuthenticationPrincipal User user,
            @RequestBody CreateShareLinkRequest request
    ) {
        return ResponseEntity.ok(shareLinkService.create(user, request));
    }

    @GetMapping("/api/v1/files/{fileItemId}/share-links")
    public ResponseEntity<List<ShareLinkResponse>> listForFile(
            @AuthenticationPrincipal User user,
            @PathVariable Long fileItemId
    ) {
        return ResponseEntity.ok(shareLinkService.listForFile(user, fileItemId));
    }

    @DeleteMapping("/api/v1/files/share-links/{shareLinkId}")
    public ResponseEntity<Void> revoke(@AuthenticationPrincipal User user, @PathVariable Long shareLinkId) {
        shareLinkService.revoke(user, shareLinkId);
        return ResponseEntity.noContent().build();
    }

    // ---- Public: anyone with the link ----

    @GetMapping("/api/v1/share/{token}")
    public ResponseEntity<SharedFileInfoResponse> info(@PathVariable String token) {
        return ResponseEntity.ok(shareLinkService.info(token));
    }

    @GetMapping("/api/v1/share/{token}/download")
    public ResponseEntity<InputStreamResource> download(@PathVariable String token) {
        DownloadPayload payload = shareLinkService.download(token);
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
