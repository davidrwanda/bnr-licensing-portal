// @author David NTAMAKEMWA

import client from './client';
import { AuditEntry, ApiResponse } from '../types';

export interface PagedAuditResponse {
  content: AuditEntry[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

export const getAuditLog = (applicationId: string) =>
  client
    .get<ApiResponse<AuditEntry[]>>(`/api/applications/${applicationId}/audit`)
    .then((r) => r.data);

export const getGlobalAuditLog = (page = 0, size = 50) =>
  client
    .get<ApiResponse<PagedAuditResponse>>(`/api/audit-logs?page=${page}&size=${size}`)
    .then((r) => r.data);
