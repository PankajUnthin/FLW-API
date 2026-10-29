package com.iemr.flw.domain.iemr;

import lombok.Data;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.sql.Timestamp;

@Entity
@Table(name = "nikshay_beneficiary_sync_status", schema = "db_iemr")
@Data
public class NikshayBeneficiarySyncStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ben_reg_id", nullable = false)
    private Long benRegID;

    // BeneficiaryID from db_identity.m_beneficiaryregidmapping; null if not found
    @Column(name = "beneficiary_id")
    private Long beneficiaryID;

    // INDEX | CONTACT | ALL_NEGATIVE
    @Column(name = "beneficiary_type", length = 20, nullable = false)
    private String beneficiaryType;

    // For a CONTACT: BenRegId of their index patient
    @Column(name = "index_ben_reg_id")
    private Long indexBenRegID;

    // ENROLMENT | CONTACTS | XRAY_UPLOAD | NAAT_UPLOAD | TRANSFER_OUT
    @Column(name = "phase", length = 20, nullable = false)
    private String phase;

    // PENDING | IN_PROGRESS | DONE | FAILED | NOT_APPLICABLE
    @Column(name = "status", length = 20, nullable = false)
    private String status;

    @Column(name = "failure_reason", length = 50)
    private String failureReason;

    @Column(name = "nikshay_id", length = 50)
    private String nikshayId;

    @Column(name = "episode_id", length = 50)
    private String episodeId;

    @Column(name = "last_attempt_at")
    private Timestamp lastAttemptAt;

    @Column(name = "isUpdated", nullable = false)
    private Boolean isUpdated = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Timestamp createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Timestamp updatedAt;
}
