package com.iemr.flw.dto.iemr;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class NikshaySyncStatusUpdateDTO {

    private Long beneficiaryId;

    @JsonProperty("isUpdated")
    private Boolean isUpdated;
}
