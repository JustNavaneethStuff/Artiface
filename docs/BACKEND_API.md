# ARTIFACE backend API (proposed)

Proposed REST contract for a future caricature-generation backend.
The Android app ships with a **local fake generator by default**. Remote calls are opt-in via `BuildConfig.USE_REMOTE_GENERATOR`.

Base URL is configured with `BuildConfig.ARTIFACE_BASE_URL` (must end with `/`).

Do **not** embed API keys in the Android application. Prefer short-lived tokens or a BFF if auth is required later.

---

## Status values

| API `status` | App `GenerationStatus` |
|--------------|------------------------|
| `preparing_image` | `PreparingImage` |
| `uploading` | `Uploading` |
| `waiting_for_processing` | `WaitingForProcessing` |
| `downloading_result` | `DownloadingResult` |
| `completed` | `Completed` |
| `failed` | `Failed` |

---

## `POST /api/v1/generations`

Starts a generation job.

**Request:** `multipart/form-data`

| Part | Type | Required | Notes |
|------|------|----------|-------|
| `image` | file (JPEG/PNG) | yes | Selfie bytes |
| `style_id` | text | yes | e.g. `comic_burst` |
| `expression` | text | yes | Enum name, e.g. `Joyful` |
| `time_of_day` | text | yes | Enum name, e.g. `Night` |
| `broad_location_label` | text | no | User-friendly label only — never precise GPS |
| `client_job_id` | text | no | Client-generated id for correlation |

**Response:** `201 Created` — `GenerationJobDto`

```json
{
  "job_id": "job_abc",
  "status": "preparing_image",
  "progress": 0.0,
  "created_at": "2026-07-18T12:00:00Z",
  "updated_at": "2026-07-18T12:00:00Z",
  "error_message": null,
  "result": null
}
```

---

## `GET /api/v1/generations/{jobId}`

Polls job status.

**Response:** `200 OK` — `GenerationJobDto`

When `status` is `completed`, `result` is populated:

```json
{
  "job_id": "job_abc",
  "status": "completed",
  "progress": 1.0,
  "created_at": "2026-07-18T12:00:00Z",
  "updated_at": "2026-07-18T12:00:05Z",
  "error_message": null,
  "result": {
    "result_id": "res_123",
    "style_id": "comic_burst",
    "title": "The Midnight Schemer",
    "expression": "Joyful",
    "time_of_day": "Night",
    "broad_location_label": null,
    "result_image_url": "https://cdn.example/results/res_123.jpg",
    "created_at": "2026-07-18T12:00:05Z"
  }
}
```

---

## `POST /api/v1/generations/{jobId}/retry`

Retries a failed (or interrupted) job.

**Response:** `200 OK` — `GenerationJobDto` with status reset toward processing.

---

## `DELETE /api/v1/generations/{jobId}`

Cancels or deletes a remote job (best-effort). Local gallery deletion remains a client-side Room concern.

**Response:** `204 No Content`

---

## Error shape

```json
{
  "error_code": "generation_failed",
  "message": "The colour spirits declined this negotiation"
}
```

Use standard HTTP statuses (`400`, `404`, `409`, `429`, `5xx`).

---

## Client behaviour (Android)

1. `RemoteCaricatureGenerator` uploads the selfie (`Uploading`).
2. Polls `GET` until `completed` or `failed` (`WaitingForProcessing`).
3. Downloads `result_image_url` to app-scoped storage (`DownloadingResult`).
4. Returns a local `CaricatureResult` for Room persistence / gallery.

WorkManager (Phase 6) continues to own process-death resilience around this flow.
