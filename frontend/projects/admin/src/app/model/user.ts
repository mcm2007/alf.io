import { Organization } from "./organization";

export interface UserInfo {
    id: number;
    type: UserType;
    username: string;
    description: string;
    firstName: string;
    lastName: string;
    emailAddress: string;
    role: RoleType;
}

export enum UserType {
    INTERNAL = 'INTERNAL',
    DEMO = 'DEMO',
    API_KEY = 'API_KEY'
}

export enum RoleType {
    ADMIN = 'ADMIN',
    OWNER = 'OWNER',
    SUPERVISOR = 'SUPERVISOR',
    OPERATOR = 'OPERATOR',
    SPONSOR = 'SPONSOR',
    API_CONSUMER = 'API_CONSUMER'
}

export interface User {
  id: number;
  type: UserType;
  enabled: boolean;
  validTo: string | null;
  username: string;
  firstName: string;
  lastName: string;
  emailAddress: string;
  validToEpochSecond: number | null;
  description: string | null;
  roles: RoleType[];
  memberOf: Organization[];
}

