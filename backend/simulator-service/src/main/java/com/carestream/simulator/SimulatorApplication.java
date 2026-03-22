package com.carestream.simulator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * CareStream Real-Time Event Simulator
 *
 * Generates a continuous stream of realistic healthcare events:
 *  - Patient ADT events  → patient.admission / patient.discharge / patient.transfer
 *  - Vulnerability scans → vulnerability.scan.results (consumed by vulnerability-service)
 *  - Security threats    → security.alerts (consumed by incident-service)
 *
 * All downstream consumers (audit-service, patient-service, vulnerability-service,
 * incident-service) process these events and update their databases in real time,
 * which the frontend polls every 30 seconds.
 */
@SpringBootApplication
@EnableScheduling
public class SimulatorApplication {
    public static void main(String[] args) {
        SpringApplication.run(SimulatorApplication.class, args);
    }
}
