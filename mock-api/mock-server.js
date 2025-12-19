const express = require('express');
const archiver = require('archiver');

const app = express();
const PORT = process.env.PORT || 8081;

// Configuration from environment variables
const DEFAULT_DELAY_MS = parseInt(process.env.INITIAL_DELAY_MS || '0');
const DEFAULT_SIZE_MB = parseInt(process.env.FILE_SIZE_MB || '100');
const DEFAULT_THROTTLE_KBPS = parseInt(process.env.THROTTLE_KBPS || '0');

/**
 * Generate dummy data chunk of specified size
 * Returns a Buffer filled with pseudo-random content
 */
function generateChunk(chunkIndex, sizeBytes) {
  const buffer = Buffer.alloc(sizeBytes);
  const content = `Chunk ${chunkIndex} - `.repeat(Math.ceil(sizeBytes / 20));
  buffer.write(content.substring(0, sizeBytes));
  return buffer;
}

/**
 * Create a readable stream that generates dummy data on-the-fly
 * This avoids holding large amounts of data in memory
 */
function createDummyDataStream(totalSizeMB, chunkIndex) {
  const { Readable } = require('stream');
  const chunkSizeBytes = 1024 * 1024; // 1MB chunks
  const totalChunks = totalSizeMB;
  let currentChunk = 0;

  return new Readable({
    read() {
      if (currentChunk < totalChunks) {
        // Generate 1MB chunk
        const chunk = generateChunk(chunkIndex * totalChunks + currentChunk, chunkSizeBytes);
        this.push(chunk);
        currentChunk++;
      } else {
        // End of stream
        this.push(null);
      }
    }
  });
}

/**
 * Main download endpoint
 * Supports query parameters to override defaults:
 * - delay: Initial delay in milliseconds
 * - sizeMB: Total size of ZIP file in MB (can be GB-size, e.g., 1024 = 1GB)
 * - throttleKBps: Download speed limit in KB/s
 */
app.get('/exports/:exportId/download', async (req, res) => {
  const { exportId } = req.params;

  // Get configuration from query params or defaults
  const initialDelayMs = parseInt(req.query.delay || DEFAULT_DELAY_MS);
  const fileSizeMB = parseInt(req.query.sizeMB || DEFAULT_SIZE_MB);
  const throttleKBps = parseInt(req.query.throttleKBps || DEFAULT_THROTTLE_KBPS);

  const timestamp = new Date().toISOString();
  console.log(`[${timestamp}] Download requested:`);
  console.log(`  Export ID: ${exportId}`);
  console.log(`  Initial delay: ${initialDelayMs}ms (${(initialDelayMs / 1000 / 60).toFixed(2)} minutes)`);
  console.log(`  File size: ${fileSizeMB}MB (${(fileSizeMB / 1024).toFixed(2)}GB)`);
  console.log(`  Throttle: ${throttleKBps > 0 ? throttleKBps + ' KB/s' : 'disabled'}`);

  // Simulate initial processing delay
  if (initialDelayMs > 0) {
    console.log(`[${new Date().toISOString()}] Waiting ${initialDelayMs}ms before starting response...`);
    await new Promise(resolve => setTimeout(resolve, initialDelayMs));
  }

  // Set response headers
  res.setHeader('Content-Type', 'application/zip');
  res.setHeader('Content-Disposition', `attachment; filename="export-${exportId}.zip"`);

  // Create ZIP archive
  const archive = archiver('zip', {
    zlib: { level: 1 } // Minimal compression for speed (important for GB files)
  });

  archive.on('error', (err) => {
    console.error(`[${new Date().toISOString()}] Archive error:`, err);
    if (!res.headersSent) {
      res.status(500).send('Error creating archive');
    }
  });

  archive.on('end', () => {
    console.log(`[${new Date().toISOString()}] Archive finalized for ${exportId}`);
  });

  // Apply throttling if requested
  if (throttleKBps > 0) {
    const { Throttle } = require('stream-throttle');
    const throttle = new Throttle({ rate: throttleKBps * 1024 }); // Convert to bytes/sec
    archive.pipe(throttle).pipe(res);
    console.log(`[${new Date().toISOString()}] Throttling enabled: ${throttleKBps} KB/s`);

    // Calculate estimated time
    const estimatedSeconds = (fileSizeMB * 1024) / throttleKBps;
    console.log(`  Estimated download time: ${(estimatedSeconds / 60).toFixed(2)} minutes`);
  } else {
    archive.pipe(res);
  }

  // Generate files to reach desired total size
  // Create multiple files to make the ZIP structure realistic
  const filesPerGB = 100; // 100 files per GB
  const fileSizeGB = fileSizeMB / 1024;
  const totalFiles = Math.max(1, Math.ceil(fileSizeGB * filesPerGB));
  const mbPerFile = fileSizeMB / totalFiles;

  console.log(`[${new Date().toISOString()}] Generating ${totalFiles} files (${mbPerFile.toFixed(2)}MB each)...`);

  // Add files to archive using streaming to keep memory usage constant
  for (let i = 0; i < totalFiles; i++) {
    const fileName = `data/file_${i.toString().padStart(6, '0')}.dat`;
    const fileStream = createDummyDataStream(Math.ceil(mbPerFile), i);

    archive.append(fileStream, { name: fileName });

    // Log progress for large files
    if (totalFiles > 10 && i % Math.ceil(totalFiles / 10) === 0) {
      const progress = ((i / totalFiles) * 100).toFixed(0);
      console.log(`[${new Date().toISOString()}]   Progress: ${progress}% (${i}/${totalFiles} files)`);
    }
  }

  // Finalize the archive (no more files will be added)
  archive.finalize();

  console.log(`[${new Date().toISOString()}] Archive streaming started for ${exportId}`);
});

/**
 * Health check endpoint for Docker healthcheck
 */
app.get('/health', (req, res) => {
  res.json({
    status: 'UP',
    timestamp: new Date().toISOString(),
    config: {
      defaultDelayMs: DEFAULT_DELAY_MS,
      defaultSizeMB: DEFAULT_SIZE_MB,
      defaultThrottleKBps: DEFAULT_THROTTLE_KBPS
    }
  });
});

/**
 * Root endpoint with usage information
 */
app.get('/', (req, res) => {
  res.json({
    service: 'Mock Export API',
    version: '1.0.0',
    endpoints: {
      download: 'GET /exports/:exportId/download',
      health: 'GET /health'
    },
    queryParameters: {
      delay: 'Initial delay in milliseconds (e.g., 120000 for 2 minutes)',
      sizeMB: 'File size in MB (e.g., 1024 for 1GB, 5120 for 5GB)',
      throttleKBps: 'Download speed limit in KB/s (e.g., 512)'
    },
    examples: {
      small: '/exports/test/download?delay=0&sizeMB=10',
      medium: '/exports/test/download?delay=60000&sizeMB=100',
      large: '/exports/test/download?delay=120000&sizeMB=1024',
      huge: '/exports/test/download?delay=300000&sizeMB=5120&throttleKBps=512'
    }
  });
});

// Start server
app.listen(PORT, '0.0.0.0', () => {
  console.log('='.repeat(60));
  console.log('Mock Export API Server');
  console.log('='.repeat(60));
  console.log(`Listening on port ${PORT}`);
  console.log('');
  console.log('Default Configuration:');
  console.log(`  Initial Delay: ${DEFAULT_DELAY_MS}ms (${(DEFAULT_DELAY_MS / 1000 / 60).toFixed(2)} minutes)`);
  console.log(`  File Size: ${DEFAULT_SIZE_MB}MB (${(DEFAULT_SIZE_MB / 1024).toFixed(2)}GB)`);
  console.log(`  Throttle: ${DEFAULT_THROTTLE_KBPS > 0 ? DEFAULT_THROTTLE_KBPS + ' KB/s' : 'disabled'}`);
  console.log('');
  console.log('Environment Variables:');
  console.log(`  INITIAL_DELAY_MS: ${process.env.INITIAL_DELAY_MS || 'not set'}`);
  console.log(`  FILE_SIZE_MB: ${process.env.FILE_SIZE_MB || 'not set'}`);
  console.log(`  THROTTLE_KBPS: ${process.env.THROTTLE_KBPS || 'not set'}`);
  console.log('');
  console.log('Endpoints:');
  console.log(`  GET http://localhost:${PORT}/exports/:exportId/download`);
  console.log(`  GET http://localhost:${PORT}/health`);
  console.log('='.repeat(60));
});
