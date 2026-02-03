import i18n from "i18next";
import { initReactI18next } from "react-i18next";
import resourcesToBackend from "i18next-resources-to-backend";
import detector from "i18next-browser-languagedetector";

i18n.use(initReactI18next)
    .use(detector)
    .use(
        resourcesToBackend(
            (lang: string, namespace: string) =>
                import(`./public/locales/${lang}/${namespace}.json`),
        ),
    )
    .init({
        fallbackLng: "en-US",
        defaultNS: "common",
        debug: import.meta.env.PROD ? false : true,
        interpolation: { escapeValue: false },
    });

export default i18n;
