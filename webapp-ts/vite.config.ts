import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
})

// Example configuring different routes
// export default defineConfig({
//   resolve: {
//     alias: {
//       '@': fileURLToPath(new URL('./src', import.meta.url))
//     }
//   },
//   build: {
//     outDir: Config.modulesDirRelativePath + "/react-ts-app"
//   },
//   base: "./"
// })