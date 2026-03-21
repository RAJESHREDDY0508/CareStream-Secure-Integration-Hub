package com.carestream.patient.service;

import com.carestream.patient.entity.DlqEntry;
import com.carestream.patient.repository.DlqEntryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DlqService {

    private final DlqEntryRepository dlqEntryRepository;

    public Page<DlqEntry> listEntries(String status, Pageable pageable) {
        if (status != null) {
            return dlqEntryRepository.findByStatus(status, pageable);
        }
        return dlqEntryRepository.findAll(pageable);
    }

    public Map<String, Object> getStats() {
        long pending      = dlqEntryRepository.countByStatus("PENDING");
        long reprocessed  = dlqEntryRepository.countByStatus("REPROCESSED");
        long discarded    = dlqEntryRepository.countByStatus("DISCARDED");
        long total        = dlqEntryRepository.count();

        return Map.of(
                "total",       total,
                "pending",     pending,
                "reprocessed", reprocessed,
                "discarded",   discarded
        );
    }

    /**
     * Mark a DLQ entry as DISCARDED (permanently skip — won't be reprocessed).
     */
    @Transactional
    public DlqEntry discard(UUID id, String note) {
        DlqEntry entry = dlqEntryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("DLQ entry not found: " + id));
        entry.setStatus("DISCARDED");
        entry.setResolutionNote(note);
        entry.setReprocessedAt(Instant.now());
        log.info("[DLQ-SVC] Entry {} discarded", id);
        return dlqEntryRepository.save(entry);
    }

    /**
     * Mark a DLQ entry as REPROCESSED.
     * In a production system, this would republish the original message to its topic.
     * For Phase 3, we record the intent — actual republishing is Phase 5+.
     */
    @Transactional
    public DlqEntry markReprocessed(UUID id, String note) {
        DlqEntry entry = dlqEntryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("DLQ entry not found: " + id));
        entry.setStatus("REPROCESSED");
        entry.setResolutionNote(note);
        entry.setReprocessedAt(Instant.now());
        log.info("[DLQ-SVC] Entry {} marked as reprocessed", id);
        return dlqEntryRepository.save(entry);
    }
}
