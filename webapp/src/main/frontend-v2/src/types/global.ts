import type { AppConfig } from "./appConfig";

declare global {
    // Injected in by Mustache template (see index.html)
    var APP_CONFIG: AppConfig;
    var CSRF_TOKEN: string;
}

export {};