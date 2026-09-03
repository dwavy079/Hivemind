package com.filesharing.repository;

import com.filesharing.model.FileItem;
import com.filesharing.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FileItemRepository extends JpaRepository<FileItem, Long> {

    List<FileItem> findByOwnerAndParentIsNullOrderByTypeAscNameAsc(User owner);

    List<FileItem> findByOwnerAndParent_IdOrderByTypeAscNameAsc(User owner, Long parentId);

    Optional<FileItem> findByIdAndOwner(Long id, User owner);

    boolean existsByOwnerAndParentAndNameAndType(User owner, FileItem parent, String name, FileItem.Type type);

    boolean existsByOwnerAndParentIsNullAndNameAndType(User owner, String name, FileItem.Type type);
}
