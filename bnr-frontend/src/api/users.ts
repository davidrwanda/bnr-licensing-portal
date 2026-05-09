// @author David NTAMAKEMWA

import client from './client';
import { ApiResponse } from '../types';

export interface UserSummary {
  id: string;
  fullName: string;
  email: string;
  role: string;
}

export const getReviewers = () =>
  client
    .get<ApiResponse<UserSummary[]>>('/api/users?role=REVIEWER')
    .then((r) => r.data);
