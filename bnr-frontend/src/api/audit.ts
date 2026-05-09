// @author David NTAMAKEMWA

import client from './client';
import { AuditEntry, ApiResponse } from '../types';

export const getAuditLog = (applicationId: string) =>
  client
    .get<ApiResponse<AuditEntry[]>>(`/api/applications/${applicationId}/audit`)
    .then((r) => r.data);
