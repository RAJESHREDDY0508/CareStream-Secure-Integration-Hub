package com.carestream.incident.service;

import com.carestream.incident.entity.AlertNotification;
import com.carestream.incident.entity.Incident;
import com.carestream.incident.entity.IncidentSeverity;
import com.carestream.incident.repository.AlertNotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Simulated alerting service.
 *
 * In production this would call:
 *  - SendGrid / SES for email
 *  - Twilio for SMS
 *  - PagerDuty API for on-call
 *  - Slack webhook for #security-alerts
 *
 * Here we persist a SIMULATED record so the audit trail and demo dashboard
 * show realistic notification activity without real external calls.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertNotificationService {

    private final AlertNotificationRepository notificationRepository;

    /**
     * Send alerts for a newly created or escalated incident.
     * Channel selection is severity-driven:
     *  CRITICAL  → EMAIL + SMS + PAGERDUTY + SLACK
     *  HIGH      → EMAIL + PAGERDUTY + SLACK
     *  MEDIUM    → EMAIL + SLACK
     *  LOW       → SLACK only
     */
    @Transactional
    public List<AlertNotification> sendIncidentAlerts(Incident incident) {
        List<AlertNotification> sent = new ArrayList<>();

        List<String> channels = channelsForSeverity(incident.getSeverity());
        for (String channel : channels) {
            AlertNotification notif = buildNotification(incident, channel);
            notif = notificationRepository.save(notif);
            sent.add(notif);
            log.info("[ALERT][{}] Simulated {} notification for incident {} — {}",
                    incident.getSeverity(), channel, incident.getIncidentId(), incident.getTitle());
        }
        return sent;
    }

    @Transactional
    public List<AlertNotification> sendEscalationAlert(Incident incident, String reason) {
        List<AlertNotification> sent = new ArrayList<>();
        for (String channel : List.of("EMAIL", "PAGERDUTY")) {
            AlertNotification notif = buildEscalationNotification(incident, channel, reason);
            notif = notificationRepository.save(notif);
            sent.add(notif);
            log.info("[ESCALATION][{}] {} escalation alert for incident {} — {}",
                    incident.getSeverity(), channel, incident.getIncidentId(), reason);
        }
        return sent;
    }

    public List<AlertNotification> getNotificationsForIncident(String incidentId) {
        return notificationRepository.findByIncidentId(incidentId);
    }

    // ── Private helpers ───────────────────────────────────────────────────

    private List<String> channelsForSeverity(IncidentSeverity severity) {
        return switch (severity) {
            case CRITICAL -> List.of("EMAIL", "SMS", "PAGERDUTY", "SLACK");
            case HIGH     -> List.of("EMAIL", "PAGERDUTY", "SLACK");
            case MEDIUM   -> List.of("EMAIL", "SLACK");
            case LOW      -> List.of("SLACK");
        };
    }

    private AlertNotification buildNotification(Incident incident, String channel) {
        String subject = String.format("[CareStream SOC][%s] New Incident: %s",
                incident.getSeverity(), incident.getIncidentId());
        String body = String.format(
                "Incident ID   : %s\n" +
                "Title         : %s\n" +
                "Severity      : %s\n" +
                "Threat Type   : %s\n" +
                "Source Service: %s\n" +
                "Status        : %s\n" +
                "Detected At   : %s\n\n" +
                "Description:\n%s\n\n" +
                "Investigate at: http://carestream-soc/incidents/%s",
                incident.getIncidentId(),
                incident.getTitle(),
                incident.getSeverity(),
                incident.getThreatType(),
                incident.getSourceService(),
                incident.getStatus(),
                incident.getDetectedAt(),
                incident.getDescription(),
                incident.getIncidentId()
        );

        return AlertNotification.builder()
                .incidentId(incident.getIncidentId())
                .channel(channel)
                .recipient(recipientForChannel(channel, incident.getSeverity()))
                .subject(subject)
                .body(body)
                .status("SIMULATED")
                .sentAt(Instant.now())
                .build();
    }

    private AlertNotification buildEscalationNotification(Incident incident, String channel, String reason) {
        String subject = String.format("[CareStream SOC][ESCALATION] %s requires attention",
                incident.getIncidentId());
        String body = String.format(
                "Escalation Reason : %s\n" +
                "Incident          : %s\n" +
                "Severity          : %s\n" +
                "Current Status    : %s\n" +
                "Investigate at    : http://carestream-soc/incidents/%s",
                reason, incident.getIncidentId(), incident.getSeverity(),
                incident.getStatus(), incident.getIncidentId()
        );

        return AlertNotification.builder()
                .incidentId(incident.getIncidentId())
                .channel(channel)
                .recipient(recipientForChannel(channel, incident.getSeverity()))
                .subject(subject)
                .body(body)
                .status("SIMULATED")
                .sentAt(Instant.now())
                .build();
    }

    private String recipientForChannel(String channel, IncidentSeverity severity) {
        return switch (channel) {
            case "EMAIL"     -> severity == IncidentSeverity.CRITICAL
                                 ? "soc-oncall@carestream.com, ciso@carestream.com"
                                 : "soc-team@carestream.com";
            case "SMS"       -> "+1-555-SOC-0001";
            case "PAGERDUTY" -> "carestream-soc-service";
            case "SLACK"     -> "#security-alerts";
            default          -> "soc-team@carestream.com";
        };
    }
}
