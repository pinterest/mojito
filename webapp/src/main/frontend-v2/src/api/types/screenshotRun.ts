export interface ScreenshotRun {
  id: number;
  repository: { id: number } | undefined;
  name: string | undefined;
  screenshots: Screenshot[];
}

export interface Screenshot {
  name: string;
  locale: Locale;
  src: string;
  branch: Branch;
  textUnits: TmTextUnit[];
}

interface Locale {
  id: number;
}

interface TmTextUnit {
  tmTextUnit: { id: number };
}

interface Branch {
  id: number;
}
