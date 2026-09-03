package com.filesharing.service;

import com.filesharing.dto.ShareDtos.CreateShareLinkRequest;
import com.filesharing.dto.ShareDtos.ShareLinkResponse;
import com.filesharing.dto.ShareDtos.SharedFileInfoResponse;
import com.filesharing.exception.ApiExceptions.*;
import com.filesharing.model.FileItem;
import com.filesharing.model.FileVersion;
import com.filesharing.model.ShareLink;
import com.filesharing.model.User;
import com.filesharing.repository.FileVersionRepository;
import com.filesharing.repository.ShareLinkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ShareLinkService {

    private final ShareLinkRepository shareLinkRepository;
    private final FileVersionRepository fileVersionRepository;
    private final FileService fileService;

    @Value("${app.share-link.base-url}")
    private String baseUrl;

    private static final SecureRandom RANDOM = new SecureRandom();

    @Transactional
    public ShareLinkResponse create(User owner, CreateShareLinkRequest request) {
        FileItem fileItem = fileService.getOwnedFile(owner, request.fileItemId());

        FileVersion pinned = null;
        if (request.versionId() != null) {
            pinned = fileVersionRepository.findByFileItemOrderByVersionNumberDesc(fileItem).stream()
                    .filter(v -> v.getId().equals(request.versionId()))
                    .findFirst()
                    .orElseThrow(() -> new NotFoundException("Version not found"));
        }

        Instant expiresAt = request.expiresInHours() != null
                ? Instant.now().plus(request.expiresInHours(), ChronoUnit.HOURS)
                : null;

        ShareLink link = ShareLink.builder()
                .token(generateToken())
                .fileItem(fileItem)
                .pinnedVersion(pinned)
                .createdBy(owner)
                .expiresAt(expiresAt)
                .build();

        return toResponse(shareLinkRepository.save(link));
    }

    @Transactional(readOnly = true)
    public List<ShareLinkResponse> listForFile(User owner, Long fileItemId) {
        FileItem fileItem = fileService.getOwnedFile(owner, fileItemId);
        return shareLinkRepository.findByFileItemOrderByCreatedAtDesc(fileItem).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void revoke(User owner, Long shareLinkId) {
        ShareLink link = shareLinkRepository.findById(shareLinkId)
                .orElseThrow(() -> new NotFoundException("Share link not found"));
        if (!link.getCreatedBy().getId().equals(owner.getId())) {
            throw new ForbiddenException("You don't own this share link");
        }
        link.setRevoked(true);
        shareLinkRepository.save(link);
    }

    @Transactional(readOnly = true)
    public SharedFileInfoResponse info(String token) {
        ShareLink link = resolveActive(token);
        FileVersion version = resolveVersion(link);
        return new SharedFileInfoResponse(version.getOriginalFileName(), version.getContentType(), version.getSizeBytes(), true);
    }

    @Transactional
    public FileService.DownloadPayload download(String token) {
        ShareLink link = resolveActive(token);
        FileVersion version = resolveVersion(link);
        link.setDownloadCount(link.getDownloadCount() + 1);
        shareLinkRepository.save(link);

        InputStream stream = fileService.openStream(version);
        return new FileService.DownloadPayload(stream, version);
    }

    private ShareLink resolveActive(String token) {
        ShareLink link = shareLinkRepository.findByToken(token)
                .orElseThrow(() -> new NotFoundException("This share link doesn't exist"));
        if (!link.isActive()) {
            throw new ForbiddenException("This share link has expired or been revoked");
        }
        return link;
    }

    private FileVersion resolveVersion(ShareLink link) {
        FileVersion version = link.getPinnedVersion() != null ? link.getPinnedVersion() : link.getFileItem().latestVersion();
        if (version == null) throw new NotFoundException("This file has no content");
        return version;
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes); // 43 chars
    }

    private ShareLinkResponse toResponse(ShareLink link) {
        return new ShareLinkResponse(
                link.getId(),
                link.getToken(),
                baseUrl + "/" + link.getToken(),
                link.getFileItem().getId(),
                link.getFileItem().getName(),
                link.getExpiresAt(),
                link.isRevoked(),
                link.getDownloadCount(),
                link.getCreatedAt()
        );
    }
}
