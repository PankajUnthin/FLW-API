package com.iemr.flw.repo.iemr;

import com.iemr.flw.domain.iemr.NikshayBeneficiarySyncStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface NikshayBeneficiarySyncStatusRepo extends JpaRepository<NikshayBeneficiarySyncStatus, Long> {

    Optional<NikshayBeneficiarySyncStatus> findByBenRegIDAndPhase(Long benRegID, String phase);

    List<NikshayBeneficiarySyncStatus> findByBenRegID(Long benRegID);

    List<NikshayBeneficiarySyncStatus> findByPhaseAndStatus(String phase, String status);

    List<NikshayBeneficiarySyncStatus> findByIsUpdated(Boolean isUpdated);

    List<NikshayBeneficiarySyncStatus> findByBenRegIDIn(Collection<Long> benRegIDs);

    // updated_at is refreshed by the column's ON UPDATE CURRENT_TIMESTAMP
    @Transactional
    @Modifying
    @Query("UPDATE NikshayBeneficiarySyncStatus s SET s.isUpdated = :isUpdated WHERE s.beneficiaryID = :beneficiaryID")
    int updateIsUpdatedByBeneficiaryID(@Param("beneficiaryID") Long beneficiaryID, @Param("isUpdated") Boolean isUpdated);

    @Transactional
    @Modifying
    @Query("UPDATE NikshayBeneficiarySyncStatus s SET s.isUpdated = :isUpdated WHERE s.benRegID = :benRegID")
    int updateIsUpdatedByBenRegID(@Param("benRegID") Long benRegID, @Param("isUpdated") Boolean isUpdated);

    // One entry per beneficiary (a beneficiary can have a row per phase). The beneficiary must have
    // an RMNCH record whose household belongs to the given provider service map and, when given,
    // village. Native SQL because the RMNCH tables live in db_identity, outside this persistence unit.
    @Query(nativeQuery = true,
            value = "SELECT DISTINCT s.ben_reg_id FROM db_iemr.nikshay_beneficiary_sync_status s " +
                    "JOIN db_identity.i_beneficiarydetails_rmnch r ON r.BeneficiaryRegID = s.ben_reg_id " +
                    "AND (r.Deleted IS NULL OR r.Deleted = 0) " +
                    "JOIN db_identity.i_householddetails h ON h.houseoldId = r.houseoldId " +
                    "AND (h.Deleted IS NULL OR h.Deleted = 0) " +
                    "WHERE h.ProviderServiceMapID = :psmId AND (:villageId IS NULL OR h.villageid = :villageId) " +
                    "ORDER BY s.ben_reg_id DESC",
            countQuery = "SELECT COUNT(DISTINCT s.ben_reg_id) FROM db_iemr.nikshay_beneficiary_sync_status s " +
                    "JOIN db_identity.i_beneficiarydetails_rmnch r ON r.BeneficiaryRegID = s.ben_reg_id " +
                    "AND (r.Deleted IS NULL OR r.Deleted = 0) " +
                    "JOIN db_identity.i_householddetails h ON h.houseoldId = r.houseoldId " +
                    "AND (h.Deleted IS NULL OR h.Deleted = 0) " +
                    "WHERE h.ProviderServiceMapID = :psmId AND (:villageId IS NULL OR h.villageid = :villageId)")
    Page<Number> findBenRegIDsByProviderServiceMapIDAndVillageID(@Param("psmId") Integer psmId,
                                                                 @Param("villageId") Integer villageId,
                                                                 Pageable pageable);
}
