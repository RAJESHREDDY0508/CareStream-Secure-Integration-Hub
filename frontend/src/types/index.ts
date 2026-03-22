// ── Vulnerability types ──────────────────────────────────────────────────

export type Severity = 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'LOW'
export type VulnStatus = 'OPEN' | 'IN_PROGRESS' | 'REMEDIATED' | 'ACCEPTED' | 'OVERDUE'
export type SlaStatus = 'ON_TRACK' | 'AT_RISK' | 'BREACHED' | 'COMPLETED'

export interface Vulnerability {
  id: string
  findingId: string
  scanId: string
  cveId: string
  severity: Severity
  affectedComponent: string
  affectedService: string
  description: string
  cvssScore: number
  slaDeadline: string
  slaStatus: SlaStatus
  status: VulnStatus
  assigneeId: string | null
  ticketId: string | null
  detectedAt: string
  remediatedAt: string | null
  createdAt: string
  updatedAt: string
}

export interface VulnStats {
  total: number
  open: number
  inProgress: number
  remediated: number
  overdue: number
  bySeverity: Record<Severity, number>
  slaCompliancePercent: number
}

export interface Scan {
  id: string
  scanId: string
  scanType: string
  targetService: string | null
  status: string
  findingsCount: number
  criticalCount: number
  highCount: number
  mediumCount: number
  lowCount: number
  startedAt: string
  completedAt: string | null
}

export interface Page<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}

// ── Incident types ───────────────────────────────────────────────────────

export type IncidentSeverity = 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'LOW'
export type IncidentStatus = 'OPEN' | 'INVESTIGATING' | 'CONTAINED' | 'RESOLVED' | 'FALSE_POSITIVE'
export type ThreatType =
  | 'BRUTE_FORCE'
  | 'PRIVILEGE_ESCALATION'
  | 'DATA_EXFILTRATION'
  | 'SQL_INJECTION'
  | 'SLA_BREACH'
  | 'VULNERABILITY_EXPLOIT'
  | 'ANOMALOUS_ACCESS'
  | 'ACCOUNT_COMPROMISE'
  | 'DENIAL_OF_SERVICE'
  | 'INSIDER_THREAT'

export interface Incident {
  id: number
  incidentId: string
  title: string
  description: string
  severity: IncidentSeverity
  status: IncidentStatus
  threatType: ThreatType
  sourceService: string | null
  sourceAlertId: string | null
  affectedResource: string | null
  assigneeId: string | null
  resolutionNotes: string | null
  detectedAt: string
  investigationStartedAt: string | null
  containedAt: string | null
  resolvedAt: string | null
  createdAt: string
  updatedAt: string
}

export interface IncidentStats {
  total: number
  open: number
  investigating: number
  contained: number
  resolved: number
  falsePositive: number
  active: number
  bySeverity: Record<IncidentSeverity, number>
  openCriticalCount: number
  mttrHours: number
}

// ── Simulation types ─────────────────────────────────────────────────────

export type AttackScenario =
  | 'BRUTE_FORCE'
  | 'PRIVILEGE_ESCALATION'
  | 'DATA_EXFILTRATION'
  | 'RANSOMWARE_PREP'
  | 'INSIDER_THREAT'
  | 'FULL_ATTACK_CHAIN'
  | 'RANDOM'

export interface SimulateVulnRequest {
  count: number
  targetService?: string
  severityDistribution?: Record<Severity, number>
}

export interface SimulateAttackRequest {
  scenario: AttackScenario
  targetService?: string
}
