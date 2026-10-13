package pl.matkmiec.backend.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pl.matkmiec.backend.auth.model.AuthPinCode;
import pl.matkmiec.backend.auth.model.PinType;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AuthPinCodeRepository extends JpaRepository<AuthPinCode, UUID> {

    Optional<AuthPinCode> findFirstByEmailIgnoreCaseAndPinTypeAndIsUsedFalseOrderByCreatedAtDesc(
            String email,
            PinType pinType
    );

    @Modifying
    @Query("UPDATE AuthPinCode p SET p.isUsed = true WHERE p.email = :email AND p.pinType = :pinType")
    void invalidateExistingPins(@Param("email") String email, @Param("pinType") PinType pinType);

    @Modifying
    @Query("DELETE FROM AuthPinCode p WHERE p.expiresAt < :now OR p.isUsed = true")
    void deleteExpiredPins(@Param("now") LocalDateTime now);
}
