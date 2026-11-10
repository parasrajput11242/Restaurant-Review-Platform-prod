/** @type {import('next').NextConfig} */
const backendUrl = process.env.BACKEND_URL ?? "http://localhost:8080";

const nextConfig = {
  // Small self-contained server for Docker (see Dockerfile)
  output: "standalone",
  images: {
    // Photos are served by the API and proxied below, so there is nothing for Next to optimise
    unoptimized: true,
  },
  async rewrites() {
    // The browser only ever talks to this origin; Next forwards /api to Spring Boot.
    return [{ source: "/api/:path*", destination: `${backendUrl}/api/:path*` }];
  },
};

export default nextConfig;
