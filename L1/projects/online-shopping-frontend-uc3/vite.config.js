import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

// L1 UC3 - Vite config. Dev server proxies /api to the API Gateway
// (see L1/UC1 architecture.json: api-gateway routes ALL /api/v1/** to
// downstream services). In dev we point straight at product-service /
// cart-service ports from L1/UC2 since no gateway is running locally yet.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      "/api/v1/products": "http://localhost:8081",
      "/api/v1/cart": "http://localhost:8082",
      "/api/v1/orders": "http://localhost:8083"
    }
  },
  test: {
    environment: "jsdom",
    globals: true,
    setupFiles: "./src/setupTests.js"
  }
});
