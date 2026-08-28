package com.internshipplatform.repository;

import com.internshipplatform.entity.Resume;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResumeRepository extends JpaRepository<Resume, Long> {

    List<Resume> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<Resume> findByIdAndUserId(Long id, Long userId);

    long countByUserId(Long userId);

    @Modifying
    @Query("UPDATE Resume r SET r.active = false WHERE r.user.id = :userId AND r.active = true")
    void deactivateAllByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE Resume r SET r.active = true WHERE r.id = :id AND r.user.id = :userId")
    void activateResume(@Param("id") Long id, @Param("userId") Long userId);
}
