package com.bhishi.repository;

import com.bhishi.model.OtpToken;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface OtpTokenRepository extends MongoRepository<OtpToken, String> {
    Optional<OtpToken> findByPhoneAndPurposeAndUsedFalseAndExpiresAtAfter(
            String phone, OtpToken.OtpPurpose purpose, LocalDateTime now);
    void deleteByPhoneAndPurpose(String phone, OtpToken.OtpPurpose purpose);
}
