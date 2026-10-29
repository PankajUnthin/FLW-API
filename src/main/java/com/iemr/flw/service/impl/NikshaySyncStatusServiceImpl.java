package com.iemr.flw.service.impl;

import com.iemr.flw.domain.identity.RMNCHBeneficiaryDetailsRmnch;
import com.iemr.flw.domain.identity.RMNCHHouseHoldDetails;
import com.iemr.flw.domain.identity.RMNCHMBeneficiarydetail;
import com.iemr.flw.domain.iemr.NikshayBeneficiaryPhaseStatus;
import com.iemr.flw.domain.iemr.NikshayBeneficiarySyncStatus;
import com.iemr.flw.dto.iemr.NikshayMissingBeneficiaryDTO;
import com.iemr.flw.repo.identity.BeneficiaryRepo;
import com.iemr.flw.repo.identity.HouseHoldRepo;
import com.iemr.flw.repo.iemr.NikshayBeneficiaryPhaseStatusRepo;
import com.iemr.flw.repo.iemr.NikshayBeneficiarySyncStatusRepo;
import com.iemr.flw.service.NikshaySyncStatusService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class NikshaySyncStatusServiceImpl implements NikshaySyncStatusService {

    private static final String STATUS_FAILED = "FAILED";

    private final Logger logger = LoggerFactory.getLogger(NikshaySyncStatusServiceImpl.class);

    @Autowired
    private NikshayBeneficiaryPhaseStatusRepo phaseStatusRepo;

    @Autowired
    private NikshayBeneficiarySyncStatusRepo syncStatusRepo;

    @Autowired
    private BeneficiaryRepo beneficiaryRepo;

    @Autowired
    private HouseHoldRepo houseHoldRepo;

    // Copies every FAILED phase-status row into nikshay_beneficiary_sync_status.
    // (ben_reg_id, phase) is unique there, so an existing row is updated instead of duplicated.
    @Override
    @Transactional
    public Map<String, Object> syncFailedPhaseStatuses() {
        List<NikshayBeneficiaryPhaseStatus> failed = phaseStatusRepo.findByStatus(STATUS_FAILED);

        int inserted = 0;
        int updated = 0;
        List<NikshayBeneficiarySyncStatus> toSave = new ArrayList<>();
        for (NikshayBeneficiaryPhaseStatus phaseStatus : failed) {
            NikshayBeneficiarySyncStatus sync = syncStatusRepo
                    .findByBenRegIDAndPhase(phaseStatus.getBenRegID(), phaseStatus.getPhase())
                    .orElse(null);
            if (sync == null) {
                sync = new NikshayBeneficiarySyncStatus();
                inserted++;
            } else {
                updated++;
            }
            sync.setBenRegID(phaseStatus.getBenRegID());
            sync.setBeneficiaryID(phaseStatus.getBeneficiaryID());
            sync.setBeneficiaryType(phaseStatus.getBeneficiaryType());
            sync.setIndexBenRegID(phaseStatus.getIndexBenRegID());
            sync.setPhase(phaseStatus.getPhase());
            sync.setStatus(phaseStatus.getStatus());
            sync.setFailureReason(phaseStatus.getFailureReason());
            sync.setNikshayId(phaseStatus.getNikshayId());
            sync.setEpisodeId(phaseStatus.getEpisodeId());
            sync.setLastAttemptAt(phaseStatus.getLastAttemptAt());
            toSave.add(sync);
        }
        syncStatusRepo.saveAll(toSave);

        logger.info("Nikshay sync: {} FAILED phase rows found, {} inserted, {} updated",
                failed.size(), inserted, updated);

        Map<String, Object> result = new HashMap<>();
        result.put("failedRecordsFound", failed.size());
        result.put("inserted", inserted);
        result.put("updated", updated);
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NikshayMissingBeneficiaryDTO> getMissingBeneficiaries(Integer providerServiceMapID, Integer villageID,
                                                                      int pageNo, int pageSize) {
        Page<Number> benRegIDPage = syncStatusRepo.findBenRegIDsByProviderServiceMapIDAndVillageID(
                providerServiceMapID, villageID, PageRequest.of(pageNo, pageSize));
        List<Long> benRegIDs = benRegIDPage.getContent().stream()
                .map(Number::longValue)
                .collect(Collectors.toList());
        logger.info("Nikshay missing beneficiaries: psmId={}, villageId={}, page={}, size={} -> {} on page, {} total",
                providerServiceMapID, villageID, pageNo, pageSize, benRegIDs.size(), benRegIDPage.getTotalElements());
        if (benRegIDs.isEmpty()) {
            return new PageImpl<>(new ArrayList<>(), benRegIDPage.getPageable(), benRegIDPage.getTotalElements());
        }

        Map<Long, List<NikshayBeneficiarySyncStatus>> syncRowsByBen = new HashMap<>();
        for (NikshayBeneficiarySyncStatus row : syncStatusRepo.findByBenRegIDIn(benRegIDs)) {
            syncRowsByBen.computeIfAbsent(row.getBenRegID(), k -> new ArrayList<>()).add(row);
        }

        // Beneficiary details from i_beneficiarydetails_rmnch
        Map<Long, RMNCHBeneficiaryDetailsRmnch> rmnchByBen = new HashMap<>();
        for (RMNCHBeneficiaryDetailsRmnch rmnch : beneficiaryRepo.findRmnchByBenRegIDIn(benRegIDs)) {
            if (Boolean.TRUE.equals(rmnch.getDeleted())) continue;
            rmnchByBen.putIfAbsent(rmnch.getBenRegId(), rmnch);
        }

        // Village from i_householddetails, matched on the beneficiary's household
        Set<Long> householdIDs = rmnchByBen.values().stream()
                .map(RMNCHBeneficiaryDetailsRmnch::getHouseoldId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        Map<Long, RMNCHHouseHoldDetails> householdByID = new HashMap<>();
        if (!householdIDs.isEmpty()) {
            for (RMNCHHouseHoldDetails household : houseHoldRepo.getByHouseHoldIDIn(householdIDs)) {
                if (Boolean.TRUE.equals(household.getDeleted())) continue;
                // a household can have several rows; prefer the one in the requested village
                RMNCHHouseHoldDetails existing = householdByID.get(household.getHouseoldId());
                if (existing == null || (villageID != null && !villageID.equals(existing.getVillageid())
                        && villageID.equals(household.getVillageid()))) {
                    householdByID.put(household.getHouseoldId(), household);
                }
            }
        }

        // The rmnch table holds no name; it lives in i_beneficiarydetails
        List<BigInteger> benRegIDsBig = benRegIDs.stream().map(BigInteger::valueOf).collect(Collectors.toList());
        Map<Long, String> nameByBen = new HashMap<>();
        for (RMNCHMBeneficiarydetail detail : beneficiaryRepo.findDetailsByBenRegIDIn(benRegIDsBig)) {
            if (detail.getBenRegId() == null || Boolean.TRUE.equals(detail.getDeleted())) continue;
            nameByBen.putIfAbsent(detail.getBenRegId().longValue(), buildName(detail));
        }

        List<NikshayMissingBeneficiaryDTO> data = new ArrayList<>();
        for (Long benRegID : benRegIDs) {
            List<NikshayBeneficiarySyncStatus> rows = syncRowsByBen.getOrDefault(benRegID, new ArrayList<>());
            RMNCHBeneficiaryDetailsRmnch rmnch = rmnchByBen.get(benRegID);
            RMNCHHouseHoldDetails household = rmnch != null && rmnch.getHouseoldId() != null
                    ? householdByID.get(rmnch.getHouseoldId())
                    : null;

            Long beneficiaryID = null;
            boolean isUpdated = !rows.isEmpty();
            Set<String> reasons = new LinkedHashSet<>();
            for (NikshayBeneficiarySyncStatus row : rows) {
                if (beneficiaryID == null) beneficiaryID = row.getBeneficiaryID();
                // a beneficiary counts as updated only once every failed phase has been updated
                if (!Boolean.TRUE.equals(row.getIsUpdated())) isUpdated = false;
                reasons.add(row.getFailureReason() != null
                        ? row.getFailureReason()
                        : row.getPhase() + " sync failed");
            }
            if (beneficiaryID == null && rmnch != null) beneficiaryID = rmnch.getBenficieryid();
            if (rmnch == null) {
                logger.warn("Nikshay missing beneficiaries: no RMNCH record found for benRegID={}", benRegID);
            }

            data.add(new NikshayMissingBeneficiaryDTO(beneficiaryID, isUpdated, new ArrayList<>(reasons),
                    nameByBen.get(benRegID),
                    household != null ? household.getVillageid() : null,
                    household != null ? household.getVillage() : null));
        }
        return new PageImpl<>(data, benRegIDPage.getPageable(), benRegIDPage.getTotalElements());
    }

    // Updates every phase row of the beneficiary. Rows synced before beneficiary_id could be resolved
    // hold it as null, so when nothing matches on beneficiary_id, fall back to the BenRegId.
    @Override
    public int updateIsUpdatedByBeneficiaryId(Long beneficiaryId, Boolean isUpdated) {
        int rows = syncStatusRepo.updateIsUpdatedByBeneficiaryID(beneficiaryId, isUpdated);
//        if (rows == 0) {
//            Long benRegID = beneficiaryRepo.getBenRegIdFromBenId(beneficiaryId);
//            if (benRegID != null) {
//                logger.info("Nikshay sync status: no rows on beneficiaryId={}, retrying with benRegID={}",
//                        beneficiaryId, benRegID);
//                rows = syncStatusRepo.updateIsUpdatedByBenRegID(benRegID, isUpdated);
//            }
//        }
        logger.info("Nikshay sync status: set isUpdated={} for beneficiaryId={} on {} rows",
                isUpdated, beneficiaryId, rows);
        return rows;
    }

    private String buildName(RMNCHMBeneficiarydetail detail) {
        String name = ((detail.getFirstName() != null ? detail.getFirstName() : "") + " "
                + (detail.getLastName() != null ? detail.getLastName() : "")).trim();
        return name.isEmpty() ? null : name;
    }
}
