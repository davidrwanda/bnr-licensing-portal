// @author David NTAMAKEMWA

import client from './client';
import { Document, ApiResponse } from '../types';

export const getDocuments = (applicationId: string) =>
  client
    .get<ApiResponse<Document[]>>(`/api/applications/${applicationId}/documents`)
    .then((r) => r.data);

export const getDocumentHistory = (applicationId: string) =>
  client
    .get<ApiResponse<Document[]>>(`/api/applications/${applicationId}/documents/history`)
    .then((r) => r.data);

export const uploadDocument = (applicationId: string, file: File) => {
  const form = new FormData();
  form.append('file', file);
  return client
    .post<ApiResponse<Document>>(`/api/applications/${applicationId}/documents`, form, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    .then((r) => r.data);
};

export const downloadUrl = (documentId: string) =>
  `/api/documents/${documentId}/download`;
