// @author David NTAMAKEMWA

import client from './client';
import { Application, ApiResponse } from '../types';

export const getApplications = () =>
  client.get<ApiResponse<Application[]>>('/api/applications').then((r) => r.data);

export const getApplication = (id: string) =>
  client.get<ApiResponse<Application>>(`/api/applications/${id}`).then((r) => r.data);

export const createApplication = (body: {
  institutionName: string;
  institutionType: string;
  contactAddress?: string;
  businessDescription?: string;
}) => client.post<ApiResponse<Application>>('/api/applications', body).then((r) => r.data);

export const updateApplication = (
  id: string,
  body: { institutionName: string; institutionType: string; contactAddress?: string; businessDescription?: string }
) => client.put<ApiResponse<Application>>(`/api/applications/${id}`, body).then((r) => r.data);

export const submitApplication = (id: string) =>
  client.patch<ApiResponse<Application>>(`/api/applications/${id}/submit`).then((r) => r.data);

export const withdrawApplication = (id: string) =>
  client.patch<ApiResponse<Application>>(`/api/applications/${id}/withdraw`).then((r) => r.data);

export const resubmitApplication = (id: string) =>
  client.patch<ApiResponse<Application>>(`/api/applications/${id}/resubmit`).then((r) => r.data);

export const assignReviewer = (id: string, reviewerId: string) =>
  client
    .patch<ApiResponse<Application>>(`/api/applications/${id}/assign-reviewer`, { reviewerId })
    .then((r) => r.data);

export const requestInfo = (id: string, notes: string) =>
  client
    .patch<ApiResponse<Application>>(`/api/applications/${id}/request-info`, { notes })
    .then((r) => r.data);

export const completeReview = (id: string, notes: string) =>
  client
    .patch<ApiResponse<Application>>(`/api/applications/${id}/complete-review`, { notes })
    .then((r) => r.data);

export const approveApplication = (id: string, reason: string) =>
  client
    .patch<ApiResponse<Application>>(`/api/applications/${id}/approve`, { reason })
    .then((r) => r.data);

export const rejectApplication = (id: string, reason: string) =>
  client
    .patch<ApiResponse<Application>>(`/api/applications/${id}/reject`, { reason })
    .then((r) => r.data);
