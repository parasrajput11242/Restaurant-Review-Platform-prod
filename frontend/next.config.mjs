/** @type {import('next').NextConfig} */
const onNetlify = Boolean(process.env.NETLIFY);

// Docker / local: Next.js forwards /api to Spring Boot itself.
// Netlify: the /api proxy is done by netlify.toml instead, so no Next rewrite is added there.
const backendUrl = process.env.BACKEND_URL ?? (onNetlify ? undefined : "http://localhost:8080");

const nextConfig = {
  // Small self-contained server for Docker (not wanted on Netlify, which has its own adapter)
  output: onNetlify ? undefined : "standalone",
  images: {
    // Photos are served by the API and proxied, so there is nothing for Next to optimise
    unoptimized: true,
  },
  async rewrites() {
    if (!backendUrl) return [];
    return [{ source: "/api/:path*", destination: `${backendUrl}/api/:path*` }];
  },
};

export default nextConfig;
