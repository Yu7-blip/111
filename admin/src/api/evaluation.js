import request from './request'

export function adminGetEvaluationList(params) {
  return request.get('/admin/evaluations', { params })
}

export function adminRevokeEvaluation(id) {
  return request.put(`/admin/evaluations/${id}/revoke`)
}
