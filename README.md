# File Storage Service

A minimal, production-ready REST API to store, list, download and delete files with per-user ownership, PUBLIC/PRIVATE visibility and tag filtering.

**Backend**: Spring Boot + MongoDB. Large files are stored on a content-addressable filesystem (CAS) with streaming SHA-256 hashing, so uploads scale from KB to 100+ GB without loading into memory.

---

## TL;DR (Run)

### 1) Run locally with Docker Compose

# From the repo root (dockerfile is at docker/Dockerfile)
```
docker compose up --build
```

This starts:
- mongodb (image: mongo:8)
- app on http://localhost:8080/api


```
docker compose down -v
```

**Compose basics**: `docker compose up` is the standard way to create and start your multi-container app.

### 2) Run the full test suite

**Option A**: locally (Docker required):
./mvnw -q -DskipTests=false clean verify

**Option B**: inside a Maven Docker container:
```bash
docker run --rm \
-v "$PWD":/workspace -w /workspace \
-v /var/run/docker.sock:/var/run/docker.sock \
maven:3.9-eclipse-temurin-21 \
mvn -q -DskipTests=false clean verify
```

Tests use Testcontainers to spin up MongoDB automatically. Docker must be reachable from the build (mapping the Docker socket in Option B enables that).

## API Base URL

The application runs under the Spring context path /api. All endpoints below are relative to:
http://localhost:8080/api

## Authentication model

There is no user management. The caller must provide the user id via a required header on protected endpoints:
X-User-Id: <userId>

## Data model (response DTO)

```json
{
"id": "string", // unique download id (unguessable)
"ownerId": "string",
"fileName": "string",
"contentType": "string",
"visibility": "PUBLIC | USER_PRIVATE",
"tags": ["string"],
"size": 123, // bytes
"createdTs": "2025-09-25T08:44:10Z"
}
```

## Endpoints

### 1) Upload file
```
POST /file/v1 (multipart/form-data)

**Headers**
X-User-Id (required)

**Form fields**
- file (required): the binary file
- filename (optional): override the original file name
- visibility (required): PUBLIC or USER_PRIVATE
- tags (optional, repeated): up to 5 tags; case-insensitive; trimmed & lowercased

**Example**
curl -X POST "http://localhost:8080/api/file/v1" \
-H "X-User-Id: alice" \
-F "file=@/path/to/file.txt" \
-F "filename=my-file.txt" \
-F "visibility=PUBLIC" \
-F "tags=alpha" -F "tags=Beta"

**Responses**
- 201 Created + FileDto JSON
- 409 Conflict if the same filename for this user already exists, or same content already uploaded by this user
- 400/500 for invalid input or unexpected errors
```

### 2) Rename file
```
PATCH /file/v1/{id}/rename

**Headers**
X-User-Id (required)

**Body**
{"filename": "new-name.txt"}

**Example**
curl -X PATCH "http://localhost:8080/api/file/v1/abc123/rename" \
-H "Content-Type: application/json" \
-H "X-User-Id: alice" \
-d '{"filename": "renamed.txt"}' -i

**Responses**
- 204 No Content on success
- 403 Forbidden if file doesn't belong to user
- 409 Conflict if the new name already exists for this user
```

### 3) Delete file
```
DELETE /file/v1/{id}

**Headers**
X-User-Id (required)

**Example**
curl -X DELETE "http://localhost:8080/api/file/v1/abc123" \
-H "X-User-Id: alice" -i

**Responses**
- 204 No Content on success
- 403 Forbidden if file doesn't belong to user
- 404 Not Found if not exists
```

### 4) Download file
```
GET /file/v1/{id}

**Headers**
X-User-Id (required)

**Example**
curl -L "http://localhost:8080/api/file/v1/abc123" \
-H "X-User-Id: alice" \
-o downloaded.bin -D -

Returns 200 OK with Content-Type and Content-Disposition set. If the file is PUBLIC, anyone can download (provide any user id header). If USER_PRIVATE, only the owner can download (403 otherwise).
```

### 5) List public files
```
GET /files/v1/public

**Query params**
- tag (optional) — case-insensitive exact match against normalized tags
- page (default 0)
- size (default 20)
- sort (default filename,asc)

**Sorting fields**
Use camelCase fields of FileDto: fileName, createdTs, contentType, size, tags

**Example**
curl "http://localhost:8080/api/files/v1/public?tag=alpha&sort=fileName,asc&page=0&size=20"

**Response**
[
{
"id": "e63d...",
"ownerId": "bob",
"fileName": "my-file.txt",
"contentType": "text/plain",
"visibility": "PUBLIC",
"tags": ["alpha"],
"size": 15,
"createdTs": "2025-09-25T08:44:10Z"
}
]

Note: controller currently returns page content only (array). Use page/size args to paginate.
```

### 6) List my files
```
GET /files/v1

**Headers**
X-User-Id (required)

**Query params**
tag (optional), page, size, sort — same as public list

**Example**
curl "http://localhost:8080/api/files/v1?tag=alpha&sort=createdTs,desc" \
-H "X-User-Id: alice"

**Response**
[
{
"id": "e63d...",
"ownerId": "alice",
"fileName": "alpha.txt",
"contentType": "text/plain",
"visibility": "USER_PRIVATE",
"tags": ["alpha","beta"],
"size": 42,
"createdTs": "2025-09-25T08:44:10Z"
}
]
```

### 7) List accessible tags
```
GET /files/v1/tags

**Headers**
X-User-Id (required)

**Example**
curl "http://localhost:8080/api/files/v1/tags" -H "X-User-Id: alice"

**Response**
{"tags":["alpha","beta","gamma"]}
```

## Behavior & constraints
- Deduplication per user: The same user cannot upload the same filename or the same content twice.
- Tags: at most 5, normalized to lower-case, no guessing; TAG and tAg are equal.
- Content type detection: if not provided by client or is generic, it's detected post-upload from the stored file on disk (Tika + Files.probeContentType fallback).
- Huge files: upload is streamed; SHA-256 is computed on the fly. No size limit is imposed by the service; use infrastructure limits if needed.
- Download links: Use the returned id (unguessable). No link guessing is possible.
- Visibility: PUBLIC vs USER_PRIVATE. Only owner can manage (rename, delete).
- Sorting & pagination: all list endpoints support both.

## Configuration
Environment variables commonly used (already set in docker-compose.yml):
- SPRING_DATA_MONGODB_URI: e.g. mongodb://mongodb:27017/file-storage
- SPRING_DATA_MONGODB_DATABASE: e.g. file-storage
- filestorage.base-path (optional): CAS base directory 

The Compose mongodb service includes a healthcheck so the app only starts once Mongo is ready. (Healthchecks are executed by the Docker engine and surface in docker ps status.)

## Local build (without Docker)
Requirements: Java 21, Maven 3.9+

```
./mvnw clean package
java -jar target/file-storage-service.jar
```
### App will start on http://localhost:8080/api with default temp CAS

This sharded directory layout keeps folders small and avoids filesystem slowdowns with large cardinality.

## Error handling (common statuses)
- 400 — invalid input
- 403 — operation on a file not owned by the user
- 404 — file not found
- 409 — duplicate filename or duplicate content (per user)
- 500 — unexpected storage errors

## CI
A GitHub Actions workflow (if added) should:
- run ./mvnw -DskipTests=false verify
- optionally build and push the Docker image

## Examples

### Upload, then download
# Upload
ID=$(curl -s -X POST "http://localhost:8080/api/file/v1" \
-H "X-User-Id: demo" \
-F "file=@README.md" \
-F "visibility=PUBLIC" \
| jq -r '.id')

# Download
curl -L "http://localhost:8080/api/file/v1/${ID}" \
-H "X-User-Id: someone" -o out.bin -D -

### List public by tag, sorted by name
curl "http://localhost:8080/api/files/v1/public?tag=alpha&sort=fileName,asc"

## License
MIT
