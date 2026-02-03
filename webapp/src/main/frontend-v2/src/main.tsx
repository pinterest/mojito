import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { BrowserRouter } from "react-router";
import "./index.css";
import App from "./App.tsx";
import "./i18n.ts";

createRoot(document.getElementById("root")!).render(
    <StrictMode>
        <BrowserRouter basename={import.meta.env.PROD ? "/v2" : ""}>
            <App />
        </BrowserRouter>
    </StrictMode>,
);
