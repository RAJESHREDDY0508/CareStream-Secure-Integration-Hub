package com.carestream.patient.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "patients",
       indexes = {
           @Index(name = "idx_patients_patient_id", columnList = "patient_id")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "patient_id", unique = true, nullable = false, length = 50)
    private String patientId;

    @Column(name = "first_name", length = 200)
    private String firstName;

    @Column(name = "last_name", length = 200)
    private String lastName;

    @Column(name = "date_of_birth", length = 100)
    private String dateOfBirth;

    @Column(name = "insurance_id", length = 200)
    private String insuranceId;

    @Column(name = "current_status", nullable = false, length = 50)
    private String currentStatus;

    @Column(name = "current_ward", length = 100)
    private String currentWard;

    @Column(name = "attending_physician", length = 100)
    private String attendingPhysician;

    @Column(name = "source", length = 100)
    private String source;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }
}
