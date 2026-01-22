export interface Page<T> {
    first: boolean;
    hasNext: boolean;
    hasPrevious: boolean;
    last: boolean;
    totalElements: number;
    totalPages: number;
    number: number;
    size: number;
    numberOfElements: number;
    content: T[];
}