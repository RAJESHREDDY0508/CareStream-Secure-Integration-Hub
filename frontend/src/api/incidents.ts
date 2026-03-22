import client from './client'
import type { Incident, IncidentStats, Page, SimulateAttackRequest } from '../types'

export const incidentApi = {
  list: (params?: {
    status?: string
    severity?: string
    threatType?: string
    service?: string
    page?: number
    size?: number
  }) =>
    client.get<Page<Incident>>('/incidents', { params }).then((r) => r.data),

  get: (incidentId: string) =>
    client.get<Incident>(`/incidents/${incidentId}`).then((r) => r.data),

  stats: () =>
    client.get<IncidentStats>('/incidents/stats').then((r) => r.data),

  investigate: (incidentId: string, assigneeId?: string) =>
    client
      .patch<Incident>(`/incidents/${incidentId}/investigate`, { assigneeId })
      .then((r) => r.data),

  contain: (incidentId: string, notes?: string) =>
    client
      .patch<Incident>(`/incidents/${incidentId}/contain`, { notes })
      .then((r) => r.data),

  resolve: (incidentId: string, resolutionNotes: string) =>
    client
      .patch<Incident>(`/incidents/${incidentId}/resolve`, { resolutionNotes })
      .then((r) => r.data),

  markFalsePositive: (incidentId: string, notes: string) =>
    client
      .patch<Incident>(`/incidents/${incidentId}/false-positive`, { notes })
      .then((r) => r.data),

  simulateAttack: (req: SimulateAttackRequest) =>
    client.post('/simulate/attack', req).then((r) => r.data),
}
