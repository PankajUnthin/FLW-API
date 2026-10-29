package com.iemr.flw.dto.iemr;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NikshayMissingBeneficiaryDTO {

    private Long beneficiaryId;

    @JsonProperty("isUpdated")
    private Boolean isUpdated;

    private List<String> reason;

    private String name;

    private Integer villageID;

    private String villageName;
}
