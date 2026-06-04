# Royals

A youth-ministry mobile app for **Royals: The Kingdom Builders**.

Built for the youth of a church in the Philippines — connecting them through
prayer, daily devotionals, life sharing, weekly meditations, leader
mentorship, events, and Bible-knowledge games.

> Internal package: `com.grace.app` (the project was scaffolded under the
> codename GRACE before the brand was finalized).

---

## What's inside

### For the youth
- **Daily Devotional** — a curated verse + reflection + prayer + journal
  step. Streak tracking. Verses align with global Christian events
  (Christmas, Holy Week, Pentecost) and don't repeat within a year.
- **Weekly Meditation** — a themed scripture + reflection prompt, with a
  text field to write a response their cell leader sees.
- **Prayer Wall** — post prayer requests (anonymous or named), pray for
  others (realtime counter), mark answered, filter by newest / oldest /
  answered / most prayed.
- **Life Feed** — share what God is doing. Text + photo posts with
  reactions (🙏 🔥 ✝️). Leaders can spotlight.
- **My Leader (Leader Connect)** — direct private chat with the assigned
  cell leader. Weekly check-in form (once per ISO week, editable until
  Monday rolls over).
- **Events** — RSVP, QR-code check-in for attendance, automatic welcome
  push notification on arrival.
- **Bible Games** — Trivia (Easy/Medium/Hard), Fill in the Blank, Who Am
  I?, Memory Cards, Verse Scramble, Timeline Sort. Two leaderboards:
  weekly cell-group + monthly global. Lifelines (Joshua Effect / Daniel
  Effect 50-50).
- **Offline browsing** — Room-cached content visible without network.
- **Light / Dark / System** themes, text-scaling for accessibility.

### For leaders
- Cell-group management (browse all groupless members, add by tap).
- Member detail view with prayer / reflection / attendance / meditation
  history.
- Leader Proxy Mode — register members who don't have a smartphone,
  proxy-mark their attendance / prayers / meditations. Members can
  later "claim" their record when they sign up.
- Compassion-program participant tracking with monthly compliance PDF
  exports.
- Content curation — manage Bible Games questions, passages,
  characters, pairs, verses, and timeline events.

### For admins
- Monthly compliance push notifications (pg_cron + Edge Function).
- Bulk email to the whole community (via Resend).
- Admin Compliance Report PDF (period-filtered, per-user breakdown).

---

## Tech stack

- **Kotlin** + **Jetpack Compose** (single Activity, Material 3)
- **Hilt** for DI, **Room** for local cache
- **Supabase** for Postgres + Auth + Realtime + Storage + Edge Functions
- **Firebase Cloud Messaging** for push notifications, **Crashlytics**
  for crash reporting
- **Retrofit + OkHttp** for the Bible API
- **WorkManager** for background sync
- **kotlinx.serialization** for DTOs
- **R8 / ProGuard** for release minification + obfuscation

minSdk 26 (Android 8.0 Oreo) · targetSdk 35 (Android 15)

---

## Getting set up

### 1. Clone the repo

```bash
git clone https://github.com/<your-username>/royals-app.git
cd royals-app
```

### 2. Provide your secrets

This project doesn't ship with secrets in the repo. You need:

#### `local.properties`
Copy the template:
```bash
cp local.properties.example local.properties
```
Then edit `local.properties` and fill in:
- `SUPABASE_URL` — from your Supabase Dashboard → Settings → API
- `SUPABASE_ANON_KEY` — same page, "anon public" key
- `BIBLE_API_KEY` — free at <https://api.esv.org> (leave blank to disable)
- `GOOGLE_WEB_CLIENT_ID` — from Google Cloud Console → Credentials, used
  for One Tap sign-in. Leave blank to disable Google sign-in (email/
  password will still work).

#### `app/google-services.json`
Download from your Firebase project:
Firebase Console → Project Settings → General → "Your apps" → Android →
**google-services.json** → place at `app/google-services.json`.

If you don't have a Firebase project yet, create a free one and add an
Android app with package `com.grace.app`.

#### Brand assets (logos / footer / background)

The Royals brand imagery isn't committed to this repo — it's the
ministry's original artwork and not for general redistribution. For the
project to compile, supply your own PNGs at these exact paths:

| File | Used by |
|---|---|
| `app/src/main/assets/logo.png` | Compliance-report PDF header |
| `app/src/main/assets/footer.png` | Compliance-report PDF footer |
| `app/src/main/assets/background.png` | Compliance-report PDF background |
| `app/src/main/res/drawable/royals_logo.jpg` | Adaptive launcher icon + notification small icon |
| `app/src/main/res/drawable/royals_logo_official.png` | Login/SignUp screens + push notification large icon |

For a quick "make-it-compile" workaround, drop any reasonable-sized
square PNG/JPG into each path — the app will build, just with your
placeholder imagery rather than the Royals brand.

### 3. Provision the Supabase backend

Run the SQL migrations in `supabase/` against your Supabase project in
order. Start with `schema.sql`, then apply the `feature-*.sql` files,
then the `security-*.sql` files. Each migration is safe to re-run.

Deploy the Edge Functions (requires the Supabase CLI):
```bash
supabase functions deploy notify-prayer-posted   --no-verify-jwt
supabase functions deploy notify-event-created   --no-verify-jwt
supabase functions deploy notify-monthly-report-ready --no-verify-jwt
supabase functions deploy welcome-on-checkin     --no-verify-jwt
supabase functions deploy send-bulk-email        --no-verify-jwt
supabase functions deploy delete-account
```

(`delete-account` does **not** use `--no-verify-jwt` — it needs the
caller's JWT verified.)

For push notifications, also set the `FCM_SERVICE_ACCOUNT_JSON` Edge
Function secret to the JSON of your Firebase service account.

### 4. Build & run

Open the project in Android Studio (Hedgehog or later) and run on an
Android 8.0+ device or emulator.

---

## Project structure

```
app/src/main/java/com/grace/app/
├── data/
│   ├── local/        Room database, DAOs, entities
│   ├── remote/       Supabase DTOs + mappers, Bible API
│   └── repository/   Concrete repos (offline-first pattern)
├── domain/
│   ├── model/        Pure Kotlin domain models
│   ├── repository/   Repository interfaces
│   └── usecase/      Single-purpose use cases
├── presentation/
│   ├── components/   Reusable composables
│   ├── navigation/   NavGraph, Screen routes
│   ├── screens/      One folder per feature
│   └── theme/        Color, Type, Shape, GraceTheme
├── di/               Hilt modules
├── service/          FCM service
├── worker/           WorkManager jobs (sync, streak, monthly push)
└── widget/           Verse-of-the-day home screen widget

supabase/
├── *.sql             Schema, RLS policies, feature migrations
└── functions/        Edge Functions (TypeScript / Deno)
```

---

## Architecture notes

- **Clean Architecture** — Presentation → Domain → Data, no reverse imports.
- **Offline-first** for reads: every screen serves Room first, then sync
  from Supabase. Cold-start refresh degrades gracefully when offline.
- **MVVM with UiState + Event + Effect** sealed classes per screen.
- **Anonymous prayer safeguard** — the user's `user_id` is stripped
  client-side in `PrayerDto.toDomain()` for anonymous posts so the UI
  never sees who wrote them (RLS is the backstop).
- **Journal entries encrypted** with AES-256-GCM via Android Keystore
  before any Room or Supabase write.
- **Server-side score clamping + per-user rate limits** on game writes
  to prevent tampered-APK leaderboard abuse.
- **RLS on every multi-user table**, with one helper SQL function
  (`set_user_fk_null`) that flipped 15+ FKs to `ON DELETE SET NULL` so
  account deletion actually cascades.

---

## License

[MIT](LICENSE)

Free to use, modify, and distribute. If you build on this for your own
ministry, a star on the repo is welcome but not required.

---

## Status

Active development for one church's youth ministry in the Philippines.
Not currently in the Play Store; shared as APK via Google Drive for the
intended user base. Issues + PRs welcome from other ministries adapting
the code for their own use.
