export interface User {
    username: string;
    givenName: string | null;
    surname: string | null;
    commonName: string | null;
    role: string;
}
