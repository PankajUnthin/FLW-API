package com.iemr.flw.repo.iemr;

import com.iemr.flw.domain.iemr.NikshayBeneficiaryPhaseStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NikshayBeneficiaryPhaseStatusRepo extends JpaRepository<NikshayBeneficiaryPhaseStatus, Long> {

    Optional<NikshayBeneficiaryPhaseStatus> findByBenRegIDAndPhase(Long benRegID, String phase);

    List<NikshayBeneficiaryPhaseStatus> findByBenRegID(Long benRegID);

    List<NikshayBeneficiaryPhaseStatus> findByStatus(String status);

    List<NikshayBeneficiaryPhaseStatus> findByPhaseAndStatus(String phase, String status);

    List<NikshayBeneficiaryPhaseStatus> findByStatusAndFailureReason(String status, String failureReason);

    List<NikshayBeneficiaryPhaseStatus> findByIndexBenRegID(Long indexBenRegID);
}
