# Mock Export API Server

A lightweight Node.js server that simulates an export API for testing large file downloads with configurable delays and network throttling.

## Features

- **Large File Support**: Generate files from MB to GB sizes (tested up to 10GB+)
- **Memory Efficient**: Streaming architecture keeps memory usage constant (~10-50MB) regardless of file size
- **Configurable Delays**: Simulate processing time before download starts (seconds to hours)
- **Network Throttling**: Simulate slow network connections with bandwidth limiting
- **On-the-fly Generation**: No disk space required - files generated during streaming
- **Docker Support**: Easy integration with Docker Compose

## Quick Start

### Using Docker Compose (Recommended)

```bash
# Start the mock API
docker-compose up -d mock-api

# Test the endpoint
curl http://localhost:8081/exports/test-001/download -o test.zip
```

### Running Locally

```bash
cd mock-api

# Install dependencies
npm install

# Start the server
npm start

# Or with nodemon for development
npm run dev
```

## Configuration

### Environment Variables

| Variable | Description | Default | Example |
|----------|-------------|---------|---------|
| `PORT` | Server port | `8081` | `8081` |
| `INITIAL_DELAY_MS` | Delay before response starts (ms) | `0` | `120000` (2 min) |
| `FILE_SIZE_MB` | Size of generated ZIP file (MB) | `100` | `5120` (5GB) |
| `THROTTLE_KBPS` | Download speed limit (KB/s) | `0` (disabled) | `512` |

### Query Parameters

Override environment variables per request:

| Parameter | Description | Example |
|-----------|-------------|---------|
| `delay` | Initial delay in milliseconds | `?delay=300000` (5 min) |
| `sizeMB` | File size in MB | `?sizeMB=1024` (1GB) |
| `throttleKBps` | Download speed limit in KB/s | `?throttleKBps=256` |

## Usage Examples

### Small File (Fast Download)

```bash
# 10MB file, no delay, full speed
curl "http://localhost:8081/exports/small/download?delay=0&sizeMB=10" -o small.zip
```

### Medium File (2-minute delay)

```bash
# 100MB file, 2-minute initial delay
curl "http://localhost:8081/exports/medium/download?delay=120000&sizeMB=100" -o medium.zip
```

### Large File (1GB)

```bash
# 1GB file, 2-minute delay, no throttle
curl "http://localhost:8081/exports/large/download?delay=120000&sizeMB=1024" -o large.zip
```

### Huge File (5GB with slow connection)

```bash
# 5GB file, 5-minute delay, 512 KB/s throttle
curl "http://localhost:8081/exports/huge/download?delay=300000&sizeMB=5120&throttleKBps=512" -o huge.zip
```

**Estimated time:** ~2.8 hours at 512 KB/s

### Extreme Test (10GB)

```bash
# 10GB file, 10-minute delay, 1 MB/s throttle
curl "http://localhost:8081/exports/extreme/download?delay=600000&sizeMB=10240&throttleKBps=1024" -o extreme.zip
```

**Estimated time:** ~2.8 hours at 1 MB/s

## Endpoints

### Download Export

```
GET /exports/:exportId/download
```

**Parameters:**
- `exportId` (path): Unique identifier for the export
- `delay` (query): Initial delay in ms
- `sizeMB` (query): File size in MB
- `throttleKBps` (query): Bandwidth limit in KB/s

**Response:**
- Content-Type: `application/zip`
- Content-Disposition: `attachment; filename="export-{exportId}.zip"`

### Health Check

```
GET /health
```

**Response:**
```json
{
  "status": "UP",
  "timestamp": "2025-01-15T10:30:00.000Z",
  "config": {
    "defaultDelayMs": 0,
    "defaultSizeMB": 100,
    "defaultThrottleKBps": 0
  }
}
```

### Info

```
GET /
```

Returns service information and usage examples.

## Testing Scenarios

### Scenario 1: Fast Export (Development Testing)

```yaml
environment:
  - INITIAL_DELAY_MS=0
  - FILE_SIZE_MB=10
  - THROTTLE_KBPS=0
```

**Use case:** Quick smoke tests during development

### Scenario 2: Normal Production-like Export

```yaml
environment:
  - INITIAL_DELAY_MS=120000    # 2 minutes
  - FILE_SIZE_MB=150           # 150MB
  - THROTTLE_KBPS=0            # Full speed
```

**Use case:** Simulating typical export processing time

### Scenario 3: Large Export with Slow Connection

```yaml
environment:
  - INITIAL_DELAY_MS=300000    # 5 minutes
  - FILE_SIZE_MB=5120          # 5GB
  - THROTTLE_KBPS=512          # 512 KB/s
```

**Use case:** Testing timeout handling and resilience for large exports

### Scenario 4: Extreme Load Test

```yaml
environment:
  - INITIAL_DELAY_MS=600000    # 10 minutes
  - FILE_SIZE_MB=10240         # 10GB
  - THROTTLE_KBPS=1024         # 1 MB/s
```

**Use case:** Stress testing for maximum supported file size

## Download Time Calculator

| File Size | Throttle | Estimated Time | Recommended Timeout |
|-----------|----------|----------------|---------------------|
| 100MB | No limit | <1 min | 300s (5 min) |
| 100MB | 1 MB/s | ~1.7 min | 300s (5 min) |
| 500MB | 512 KB/s | ~17 min | 1800s (30 min) |
| 1GB | 1 MB/s | ~17 min | 1800s (30 min) |
| 5GB | 512 KB/s | ~2.8 hours | 12000s (3.3 hours) |
| 10GB | 1 MB/s | ~2.8 hours | 12000s (3.3 hours) |

**Note:** Add initial delay time to total download time when calculating required timeouts.

## Docker Compose Configuration

### Development (Fast)

```yaml
mock-api:
  build:
    context: ./mock-api
  ports:
    - "8081:8081"
  environment:
    - INITIAL_DELAY_MS=0
    - FILE_SIZE_MB=10
```

### Testing (Realistic)

```yaml
mock-api:
  build:
    context: ./mock-api
  ports:
    - "8081:8081"
  environment:
    - INITIAL_DELAY_MS=120000    # 2 minutes
    - FILE_SIZE_MB=150           # 150MB
    - THROTTLE_KBPS=0
```

### Load Testing (Large Files)

```yaml
mock-api:
  build:
    context: ./mock-api
  ports:
    - "8081:8081"
  environment:
    - INITIAL_DELAY_MS=300000    # 5 minutes
    - FILE_SIZE_MB=5120          # 5GB
    - THROTTLE_KBPS=512          # 512 KB/s
```

## Memory Usage

The server maintains constant low memory usage regardless of file size:

- **Idle**: ~20-30MB
- **Generating 100MB**: ~30-40MB
- **Generating 1GB**: ~30-50MB
- **Generating 10GB**: ~30-50MB

Memory efficiency is achieved through:
1. Streaming data generation (1MB chunks)
2. Direct pipe from generator to archiver to HTTP response
3. No buffering of complete file in memory

## Troubleshooting

### Downloads are too slow

Check throttling settings:
```bash
# Verify health endpoint shows correct config
curl http://localhost:8081/health

# Try without throttle parameter
curl "http://localhost:8081/exports/test/download?sizeMB=100" -o test.zip
```

### Timeout errors

Increase client timeout to match expected download time:

**For Java HttpClient:**
```yaml
app:
  export-api:
    timeout-seconds: 12000  # 3.3 hours for large files
```

**For curl:**
```bash
curl --max-time 7200 "http://localhost:8081/exports/test/download?sizeMB=5120" -o test.zip
```

### Container memory issues

The server is designed for low memory usage. If you experience issues:

1. Check Docker memory limits: `docker stats`
2. Ensure archiver compression level is set to 1 (minimal)
3. Reduce file size or increase container memory allocation

## Technical Details

### File Structure

Generated ZIP files contain multiple files organized in a `/data` directory:

```
export-test.zip
└── data/
    ├── file_000000.dat
    ├── file_000001.dat
    ├── file_000002.dat
    └── ...
```

- **Files per GB**: 100 files
- **File size**: Dynamically calculated to reach target total size
- **Compression**: Level 1 (minimal) for performance

### Architecture

1. **Request Handler**: Parses parameters and validates input
2. **Delay Simulator**: Waits specified time before starting response
3. **Data Generator**: Creates dummy data in 1MB chunks
4. **Archiver**: Streams data directly to ZIP format
5. **Throttle (optional)**: Limits bandwidth if configured
6. **Response**: Streams ZIP to client

## Development

### Running Tests

```bash
# Install dev dependencies
npm install

# Run the server in development mode
npm run dev

# Test with curl in another terminal
curl http://localhost:8081/exports/test/download -o test.zip
```

### Building Docker Image

```bash
# Build
docker build -t mock-export-api ./mock-api

# Run
docker run -p 8081:8081 \
  -e INITIAL_DELAY_MS=60000 \
  -e FILE_SIZE_MB=100 \
  mock-export-api

# Test
curl http://localhost:8081/exports/test/download -o test.zip
```

## License

MIT
