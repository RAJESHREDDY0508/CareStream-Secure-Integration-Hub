import client from './client'
import type { Page, Vulnerability, VulnStats, Scan, SimulateVulnRequest } from '../types'

export const vulnApi = {
  list: (params?: {
    severity?: string
    status?: string
    service?: string
    page?: number
    size?: number
  }) =>
    client.get<Page<Vulnerability>>('/vulnerabilities', { params }).then((r) => r.data),

  get: (findingId: string) =>
    client.get<Vulnerability>(`/vulnerabilities/${findingId}`).then((r) => r.data),

  stats: () =>
    client.get<VulnStats>('/vulnerabilities/stats').then((r) => r.data),

  assign: (findingId: string, assigneeId: string) =>
    client
      .patch<Vulnerability>(`/vulnerabilities/${findingId}/assign`, { assigneeId })
      .then((r) => r.data),

  remediate: (findingId: string, notes: string) =>
    client
      .patch<Vulnerability>(`/vulnerabilities/${findingId}/remediate`, { notes })
      .then((r) => r.data),

  accept: (findingId: string, reason: string) =>
    client
      .patch<Vulnerability>(`/vulnerabilities/${findingId}/accept`, { reason })
      .then((r) => r.data),

  listScans: (page = 0, size = 10) =>
    client.get<Page<Scan>>('/vulnerabilities/scans', { params: { page, size } }).then((r) => r.data),

  simulate: (req: SimulateVulnRequest) =>
    client.post('/simulate/vulnerabilities', req).then((r) => r.data),
}
