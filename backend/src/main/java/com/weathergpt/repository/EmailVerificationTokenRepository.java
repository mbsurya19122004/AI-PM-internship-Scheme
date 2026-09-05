package com.weathergpt.repository;

import com.weathergpt.entity.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {

    Optional<EmailVerificationToken> findByTokenAndPurpose(String token, String purpose);

    @Modifying
    @Query("DELETE FROM EmailVerificationToken t WHERE t.email = :email AND t.purpose = :purpose")
    void deleteByEmailAndPurpose(String email, String purpose);
}
