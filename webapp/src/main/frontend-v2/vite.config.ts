import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import fs from "node:fs";
import path from "node:path";

const OUTPUT_PATH_RELATIVE = "../resources";
const OUTPUT_PATH = path.resolve(__dirname, OUTPUT_PATH_RELATIVE);

function moveIndexToTemplates() {
  return {
    name: "move-index-to-templates",
    closeBundle() {
      const srcIndexHtmlPath = path.join(OUTPUT_PATH, "static", "index.html");
      const outputDir = path.join(OUTPUT_PATH, "templates", "frontend-v2");
      const finalIndexHtmlPath = path.join(outputDir, "index.html");

      // eslint-disable-next-line no-console
      console.log("Moving files:");
      // eslint-disable-next-line no-console
      console.log(
        `${path.join(
          OUTPUT_PATH_RELATIVE,
          "static",
          "index.html",
        )} -> ${path.join(OUTPUT_PATH_RELATIVE, "templates", "frontend-v2")}`,
      );

      fs.mkdirSync(outputDir, { recursive: true });
      fs.renameSync(srcIndexHtmlPath, finalIndexHtmlPath);
    },
  };
}

export default defineConfig(({ mode }) => {
  const isDev = mode === "development";

  return {
    plugins: [react(), moveIndexToTemplates()],
    base: "/",

    resolve: {
      alias: {
        "@": path.resolve(__dirname, "./src"),
      },
    },

    server: {
      proxy: isDev
        ? {
            "/api": {
              target: "http://localhost:8080",
              changeOrigin: true,
            },
          }
        : undefined,
    },

    build: {
      outDir: `${OUTPUT_PATH}/static`,

      rollupOptions: {
        output: {
          entryFileNames: "assets/[name]-[hash].js",
          chunkFileNames: "assets/[name]-[hash].js",
          assetFileNames: "assets/[name]-[hash][extname]",
        },
      },
    },
  };
});
