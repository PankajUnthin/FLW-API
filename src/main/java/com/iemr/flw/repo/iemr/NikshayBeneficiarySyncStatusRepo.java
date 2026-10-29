package com.iemr.flw.repo.iemr;

import com.iemr.flw.domain.iemr.NikshayBeneficiarySyncStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NikshayBeneficiarySyncStatusRepo extends JpaRepository<NikshayBeneficiarySyncStatus, Long> {

    Optional<NikshayBeneficiarySyncStatus> findByBenRegIDAndPhase(Long benRegID, String phase);

    List<NikshayBeneficiarySyncStatus> findByBenRegID(Long benRegID);

    List<NikshayBeneficiarySyncStatus> findByPhaseAndStatus(String phase, String status);
}
