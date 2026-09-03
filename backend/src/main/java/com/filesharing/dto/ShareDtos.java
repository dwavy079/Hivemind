package com.filesharing.dto;

import java.time.Instant;

public class ShareDtos {

    public record CreateShareLinkRequest(
            Long fileItemId,
            /** Optional: pin the link to a specific version instead of "always latest". */
            Long versionId,
            /** Optional: hours until the link expires. Null/omitted = never expires. */
            Long expiresInHours
    ) {}

    public record ShareLinkResponse(
            Long id,
            String token,
            String url,
            Long fileItemId,
            String fileName,
            Instant expiresAt,
            boolean revoked,
            long downloadCount,
            Instant createdAt
    ) {}

    public record SharedFileInfoResponse(
            String fileName,
            String contentType,
            long sizeBytes,
            boolean active
    ) {}
}
