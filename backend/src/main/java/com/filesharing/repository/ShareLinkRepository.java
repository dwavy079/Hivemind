package com.filesharing.repository;

import com.filesharing.model.FileItem;
import com.filesharing.model.ShareLink;
import com.filesharing.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ShareLinkRepository extends JpaRepository<ShareLink, Long> {
    Optional<ShareLink> findByToken(String token);
    List<ShareLink> findByFileItemOrderByCreatedAtDesc(FileItem fileItem);
    List<ShareLink> findByCreatedByOrderByCreatedAtDesc(User createdBy);
}
