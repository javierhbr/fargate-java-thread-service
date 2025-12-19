# Use Node.js 21 Alpine for small image size
FROM node:21-alpine

# Set working directory
WORKDIR /app

# Copy package files
COPY package*.json ./

# Install production dependencies only
RUN npm ci --only=production && \
    npm cache clean --force

# Copy application code
COPY mock-server.js ./

# Expose port
EXPOSE 8081

# Health check
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8081/health || exit 1

# Run as non-root user for security
USER node

# Start the server
CMD ["node", "mock-server.js"]
