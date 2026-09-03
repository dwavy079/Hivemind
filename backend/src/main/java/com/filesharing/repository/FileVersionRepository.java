package com.filesharing.repository;

import com.filesharing.model.FileItem;
import com.filesharing.model.FileVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FileVersionRepository extends JpaRepository<FileVersion, Long> {
    List<FileVersion> findByFileItemOrderByVersionNumberDesc(FileItem fileItem);
    int countByFileItem(FileItem fileItem);
}
