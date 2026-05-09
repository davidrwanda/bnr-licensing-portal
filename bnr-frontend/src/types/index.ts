// @author David NTAMAKEMWA

export type Role = 'APPLICANT' | 'REVIEWER' | 'APPROVER' | 'ADMIN';

export type ApplicationStatus =
  | 'DRAFT'
  | 'SUBMITTED'
  | 'UNDER_REVIEW'
  | 'INFO_REQUESTED'
  | 'RESUBMITTED'
  | 'REVIEW_COMPLETE'
  | 'APPROVED'
  | 'REJECTED'
  | 'WITHDRAWN';

export interface AuthUser {
  userId: string;
  email: string;
  fullName: string;
  role: Role;
  accessToken: string;
  refreshToken: string;
}

export interface UserSummary {
  id: string;
  fullName: string;
  email: string;
}

export interface Application {
  id: string;
  institutionName: string;
  institutionType: string;
  contactAddress: string | null;
  businessDescription: string | null;
  status: ApplicationStatus;
  version: number;
  reviewerNotes: string | null;
  decisionReason: string | null;
  applicant: UserSummary | null;
  reviewer: UserSummary | null;
  approver: UserSummary | null;
  createdAt: string;
  submittedAt: string | null;
  decidedAt: string | null;
}

export interface Document {
  id: string;
  applicationId: string;
  fileName: string;
  fileSize: number;
  mimeType: string;
  documentVersion: number;
  superseded: boolean;
  uploaderEmail: string;
  uploadedAt: string;
}

export interface AuditEntry {
  id: number;
  actorEmail: string;
  action: string;
  previousStatus: string | null;
  newStatus: string | null;
  metadata: string | null;
  createdAt: string;
}

export interface ApiResponse<T> {
  success: boolean;
  data: T | null;
  error: { code: string; message: string } | null;
}
