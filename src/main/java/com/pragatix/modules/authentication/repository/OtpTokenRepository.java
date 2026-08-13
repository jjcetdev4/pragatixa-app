package jjcet.PragatiX.modules.authentication.repository;

import jjcet.PragatiX.entity.OtpToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OtpTokenRepository extends JpaRepository<OtpToken, Long> {
    Optional<OtpToken> findByEmailAndOtp(String email, String otp);

    Optional<OtpToken> findByEmail(String email);

    void deleteByEmail(String email);
}
