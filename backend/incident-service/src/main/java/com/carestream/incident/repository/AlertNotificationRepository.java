package com.carestream.incident.repository;

import com.carestream.incident.entity.AlertNotification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlertNotificationRepository extends JpaRepository<AlertNotification, Long> {

    List<AlertNotification> findByIncidentId(String incidentId);

    long countByIncidentId(String incidentId);
}
