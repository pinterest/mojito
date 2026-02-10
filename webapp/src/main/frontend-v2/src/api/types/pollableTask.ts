import type { User } from "./user";

export interface PollableTask {
  id?: number;
  name?: string;
  message?: string;
  expectedSubTaskNumber?: number;
  allFinished?: boolean;
  finished?: boolean;
  finishedDate?: string;
  error?: boolean;
  errorMessage?: string;
  timeout?: boolean;
  timeoutDuration?: number;
  createdDate?: string;
  lastModifiedDate?: string;
  createdByUser?: User;
  // Parent task information
  pollableTask?: PollableTask;
  subTasks?: PollableTask[];
}
