import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

const target = process.env.VITE_GATEWAY_TARGET || 'http://localhost:8080';

export default defineConfig({
  plugins: [react()],
  server: { proxy: { '/api': target } },
  preview: { proxy: { '/api': target } },
});
