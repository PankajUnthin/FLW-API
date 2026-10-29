package com.iemr.flw.service;

import com.iemr.flw.dto.iemr.NikshayMissingBeneficiaryDTO;
import org.springframework.data.domain.Page;

import java.util.Map;

public interface NikshaySyncStatusService {

    Map<String, Object> syncFailedPhaseStatuses();

    Page<NikshayMissingBeneficiaryDTO> getMissingBeneficiaries(Integer providerServiceMapID, Integer villageID,
                                                               int pageNo, int pageSize);

    int updateIsUpdatedByBeneficiaryId(Long beneficiaryId, Boolean isUpdated);
}
