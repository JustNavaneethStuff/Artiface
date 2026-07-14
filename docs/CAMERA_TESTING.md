# Camera testing notes (Phase 3)

## Emulator

CameraX capture works on Android Emulators that expose a virtual camera.

1. Create/start an AVD with a camera (front + back if available).
2. In Extended Controls → Camera, set virtual scene or webcam passthrough.
3. Install the debug APK and complete onboarding.
4. Allow camera permission when prompted.
5. Verify:
   - Preview fills the screen
   - Switch camera toggles lenses (if both available)
   - Flash control appears only when the active lens reports a flash unit
   - Shutter shows a capturing overlay and navigates to preview
   - Retake returns to camera; Continue advances to style selection (Phase 4 placeholder)

Permission denial paths:

- Deny once → rationale / allow-again screen
- Deny permanently (Don't ask again) → system-settings CTA
- App settings gear still opens in-app settings

## Physical device

Required for confidence in:

- Front-camera mirroring (`isReversedHorizontal`)
- Real EXIF orientation values
- Flash hardware reporting
- Portrait vs landscape control placement under rotation
- Capture latency and storage under low free space

Suggested checklist on a physical phone:

1. Cold start with permission not granted
2. Capture with front camera, inspect preview orientation
3. Switch to rear, capture again
4. Rotate to landscape and confirm shutter sits near the right-hand centre
5. Background the app mid-preview, return, confirm selfie still loads (file + meta on disk)
6. Revoke camera permission in system settings, reopen ARTIFACE, confirm permanent-denial UX

## Known Phase 3 limitations

- Emulators without a camera surface show the unavailable-camera pane
- Zoom/pan on preview is local visual only (not a destructive crop write-back)
- Gallery button navigates to the Phase 5 gallery placeholder
