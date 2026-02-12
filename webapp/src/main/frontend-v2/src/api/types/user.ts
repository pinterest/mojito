export type Role = "ROLE_ADMIN" | "ROLE_TRANSLATOR" | "ROLE_USER" | "ROLE_PM";

export interface User {
  username: string;
  givenName: string | null;
  surname: string | null;
  commonName: string | null;
  role: Role;
}
