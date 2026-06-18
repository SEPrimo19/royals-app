# Royals App — Software Development Life Cycle (SDLC) & System Analysis

**Application:** Royals (internal code name: GRACE)
**Ministry:** Royals — The Kingdom Builders (Church of the Nazarene youth ministry, Philippines)
**Platform:** Android (Kotlin · Jetpack Compose)
**Backend:** Supabase (PostgreSQL · Auth · Realtime · Storage · Edge Functions) + Firebase Cloud Messaging
**Architecture:** Clean Architecture + MVVM, offline-first
**Distribution:** Direct APK via Google Drive + GitHub (not the Play Store)
**Document owner:** Jhon Clarence B. Rulona
**Date:** June 2026 · **Doc version:** 1.0

---

## 1. Executive Summary

Royals is a purpose-built youth-ministry platform that consolidates devotionals,
prayer, community life-sharing, leader mentorship, Bible study, gamified Scripture
learning, attendance, and Compassion compliance reporting into one safe, offline-first
Android app for Filipino church youth (ages 13–30) and their leaders.

The system has progressed well beyond a minimum viable product. The core spiritual-growth
loop (devotional → reflection → streak → prayer → community → leader follow-up) is shipped
and stable, and the platform has since absorbed multiple v2 capability sets — an offline KJV
Bible, a Bible Study suite, a six-mode Bible Games engine with leaderboards, a Discipleship
library, Leader Proxy Mode for members without smartphones, Council roles, group join
requests, and a full Compassion participant-tracking and monthly-compliance pipeline.

This document records the project against the seven SDLC phases, presents a system analysis,
and reports the **overall status from UI frontend to backend** and the **system security**
posture as of June 2026.

---

## 2. SDLC Methodology

Royals was not built with a single big-bang waterfall pass. It follows an
**iterative–incremental (agile-leaning)** model:

- A solid foundation was laid first (auth, theme, navigation, the devotional/prayer/feed
  core) following a fixed architecture and design system.
- Every subsequent capability shipped as a vertical, end-to-end **feature slice**
  (UI → ViewModel → use case → repository → Room → Supabase → RLS → optional Edge Function),
  released, then hardened.
- A standing policy of *"log all issues, fix later in a dedicated pass"* keeps feature
  velocity high while preserving a known-issues backlog for scheduled bug-audit sprints.

Even within this iterative model, each slice still moves through the classic phases below.
The phases are therefore described both as the project's overall journey and as the
repeatable cycle applied to each new feature.

---

## 3. SDLC Phases

### Phase 1 — Planning & Feasibility

**Objectives.** Define the problem, the audience, the platform, and the constraints.

**Activities & artifacts.**
- Problem framing: youth disengagement, fragmented spiritual tools, weak leader↔member
  follow-up, and the administrative burden of Compassion compliance reporting.
- Audience definition: members, cell leaders, youth president, pastors, admin, and a
  **Council** tier for officers not attached to a cell.
- Platform decision: **Android-only** (the youth's dominant device class) with a
  **direct-distribution** model (Google Drive + GitHub) to avoid Play Store friction and cost.
- Backend decision: **Supabase** as a managed Postgres + Auth + Realtime + Storage + Functions
  stack — minimizing DevOps for a solo developer while providing Row Level Security.
- Tech-stack pinning: a deliberately fixed 2024 dependency set (Kotlin 2.0, Compose BOM
  2024.06, Hilt, Room, Supabase BOM 2.5.4, Gradle 8.9) for reproducible builds.

**Analysis & insights.**
- Choosing a managed BaaS was the single highest-leverage feasibility decision: it made an
  ambitious, security-sensitive, multi-role app tractable for one developer.
- Direct distribution traded auto-update convenience for control; this later required an
  **in-app version check + update banner** to compensate (now shipped).
- Pinning versions early prevented the toolchain drift that typically derails long solo builds.

---

### Phase 2 — Requirements Analysis (System Analysis)

**Objectives.** Convert the vision into concrete functional and non-functional requirements
and a data model.

**Functional requirements (by module).**
- **Identity:** email/password + one-tap Google sign-in; profile setup; role-based access.
- **Devotional:** daily "Today's Word", private reflection journal, completion ring, streaks.
- **Prayer Wall:** post (optionally anonymous), categorize, intercede, mark answered, 30-day expiry.
- **Life Feed:** text/photo/scripture posts, reactions, comments, leader highlighting, moderation.
- **Leader Connect:** private member↔leader messaging and check-ins.
- **Bible:** offline KJV reader, search, Study Mode (highlights, tap-a-verse, split-screen
  notes, sermon notes), verse-image creator.
- **Bible Games:** Trivia, Fill-in-the-Blank, Who Am I, Memory Cards, Verse Scramble,
  Timeline Sort; daily lock with calendar-midnight reset; cell-group weekly + monthly
  cell-vs-cell Team leaderboards.
- **Discipleship:** curated daily activities, algorithmic picker + swap, completion + reflection, streaks.
- **Events & Attendance:** event listing/RSVP, QR-based attendance.
- **Compassion:** participant tracking, weekly meditation, monthly compliance PDF reports.
- **Leadership tooling:** Leader Proxy Mode (add member, proxy attendance/prayer/reflection,
  claim flow), group join requests (request→approve), bulk email, birthday greetings.

**Non-functional requirements.**
- **Offline-first** (Room is the source of truth; Supabase is the sync layer).
- **Security & privacy by default** (RLS everywhere, private journals, anonymous prayer).
- **Accessibility** (text scaling, Light/Dark/System theming, large tap targets, contrast).
- **Performance** (no ANRs — a hard lesson after oversized assets and per-frame canvas work).
- **Maintainability** (strict layer separation, explicit mappers, one ViewModel per screen).

**Analysis & insights.**
- The requirement that *anonymous prayers must never leak identity* and *journals must be
  private* pushed security into the data model from day one rather than bolting it on later.
- A real-world inclusion constraint — youth **without smartphones** — produced a genuinely
  novel requirement (Leader Proxy Mode) that a generic spec would have missed.
- Compassion compliance reporting reframed the app from "engagement tool" to
  "operational system of record," raising the bar for data integrity and exports.

---

### Phase 3 — System Design

**Objectives.** Define architecture, layers, data flow, schema, and the design system.

**Architecture.** Clean Architecture with MVVM:

```
Composable (UI)
   → ViewModel (UiState / Event / Effect, StateFlow)
      → UseCase (domain)
         → Repository interface (domain)
            → RepositoryImpl (data)
               → Room DAO (local, source of truth)
               → Supabase (remote, sync)
```

- **Offline-first data flow:** repositories emit local Room data immediately, then refresh
  from Supabase when online, writing back to Room. The UI always has data to render.
- **Explicit mappers** at every boundary: `dto.toDomain()`, `entity.toDomain()`,
  `domain.toEntity()` — no cross-layer model sharing.
- **Dependency injection** via Hilt modules (Network, Database, Repository, DataStore).
- **Navigation** via a single-activity, type-safe `Screen` sealed hierarchy + `NavGraph`.

**Data design.** A normalized PostgreSQL schema (users, groups, devotionals, prayers,
posts, reactions, comments, messages, check-ins, events, RSVPs, mood check-ins, challenges,
Bible-games content/attempts, discipleship, notes, join-requests, app-versions, etc.),
with **Row Level Security policies on every sensitive table**.

**Design system.** A documented brand language — GraceGold (#C9A84C) accent, deep-blue
backgrounds, Cormorant Garamond for scripture/display + Lato for UI, 16/20dp spacing,
16–24dp radii, and mandated micro-animations (AnimatedVisibility, animateFloatAsState,
animateContentSize, Crossfade) for a warm, polished feel.

**Analysis & insights.**
- Making Room the source of truth (not a cache) is what makes the app usable on the
  unreliable connectivity typical of the target context.
- Designing RLS as part of the schema (not the app) means the security boundary holds even
  if a client is compromised or a query is malformed.
- The strict per-screen UiState/Event/Effect pattern made each feature slice predictable to
  build and to debug end-to-end.

---

### Phase 4 — Implementation / Development

**Objectives.** Build the features to the design, completely and idiomatically.

**Key implementation facts.**
- ~324 Kotlin files across presentation/domain/data, plus ~47 SQL migrations and ~9 Deno/
  TypeScript Supabase Edge Functions.
- 100% Jetpack Compose UI (zero XML layouts); StateFlow + `collectAsStateWithLifecycle`
  (no LiveData); coroutines scoped to `viewModelScope` (UI) and `Dispatchers.IO` (data).
- Offline KJV Bible bundled as a Room/SQLite asset (`assets/kjv.db`, 31,102 verses) —
  KJV chosen because NKJV is copyrighted.
- Background work via WorkManager (devotional sync, streak checks, reminders, offline sync);
  push via Firebase Cloud Messaging; home-screen verse widget via Glance.
- Secrets are injected through `BuildConfig` from `local.properties` (never committed);
  Edge Function secrets come from Deno environment / Supabase Vault.

**Analysis & insights.**
- The feature-slice discipline paid off: new game modes, for example, follow a repeatable
  checklist (mode CHECK constraint + content-table FK) that eliminated whole classes of
  silent-insert bugs after the first mode was built.
- Performance lessons were learned the hard way and codified: avoid oversized PNGs per
  `Image`, never render per-frame canvas QR — both caused ANRs and are now anti-patterns.
- A late decision to store devotional reflections as **plaintext-under-RLS** instead of
  device-encrypted fixed a real data-loss bug (Keystore key wiped on uninstall) — a pragmatic
  trade of "perfect on-device secrecy" for "recoverable from the cloud under RLS."

---

### Phase 5 — Testing & Quality Assurance

**Objectives.** Verify correctness, resilience, and accessibility per feature and overall.

**Approach.**
- **Feature-specific QA checklists** per slice: happy path, empty state, error state,
  offline state, and edge cases (expired session, empty feed, zero prayers, very long text).
- **Mental dry-runs / data-flow tracing** from Supabase → Repository → UseCase → ViewModel →
  Composable for each feature before sign-off.
- **Build-level verification:** the project compiles cleanly (`compileDebugKotlin` + KSP
  codegen for Room/Hilt) as a gate.
- **Periodic bug audits:** a full audit (e.g., June 2026) found 0 compile errors / 113
  warnings, fixed 3 P0 bugs (locale-cached formatters, a duplicate role parser, a Realtime
  cleanup leak), and deferred a triaged P1/P2 punch list.
- **Accessibility checks:** content descriptions on icons, color contrast, text scaling,
  configuration-change/rotation resilience, dark-mode rendering.

**Analysis & insights.**
- For a solo project, codified per-feature checklists plus disciplined dry-runs substitute
  effectively for a large automated suite — though automated tests remain the top
  quality-investment opportunity (see Technical Debt).
- Treating "log all issues, fix in a dedicated pass" as policy prevents context-switching
  churn but **requires** the scheduled audit to actually happen, or debt accumulates.

---

### Phase 6 — Deployment & Release

**Objectives.** Ship runnable builds and operable backend services.

**Client deployment.**
- Release APKs distributed via Google Drive + GitHub.
- An in-app **version check + update banner** (AppVersion module) notifies users when a newer
  build is available — the compensating control for not being on the Play Store.
- New bundled data (e.g., the KJV database) ships inside the APK.

**Backend deployment.**
- SQL migrations applied through the Supabase SQL editor (a backlog of feature/fix/release
  migration files is versioned in the repo).
- Edge Functions deployed to Supabase (delete-account, notify-event-created,
  notify-monthly-report-ready, notify-prayer-posted, send-bulk-email, welcome-on-checkin,
  notify-birthdays, notify-join-request-created/decided, plus shared dedup/fcm/resend helpers).
- Scheduled jobs via **pg_cron** (monthly game-attempts purge; birthday greetings at 08:00 PHT;
  monthly Compassion-compliance push).
- Transactional email via **Resend** (currently sandbox-limited until a sending domain is verified).

**Analysis & insights.**
- The most fragile part of release is the **manual coupling** between an app build and its
  required SQL/Edge-Function/secret deployment; several features are "code-merged, deploy
  pending." A release checklist (or migration automation) is the clear next maturity step.
- Sandbox/email and key-deprecation items are operational, not code, risks — they must be
  tracked as deployment tasks so a shipped feature isn't silently inert in production.

---

### Phase 7 — Maintenance & Evolution

**Objectives.** Keep the system healthy and grow it responsibly.

**Activities.**
- Bug-audit sprints against the known-issues backlog.
- Dependency and platform watch (e.g., Supabase anon/service-role key deprecation — still
  functional today, with a migration recipe prepared for the cutover).
- Incremental feature growth gated behind audits (the v2 capability sets were added this way).
- Memory/operational notes maintained per feature for fast re-onboarding to any subsystem.

**Analysis & insights.**
- The project's biggest long-term risk is **single-maintainer knowledge concentration**;
  the disciplined per-feature notes and this document mitigate but do not eliminate it.
- Evolution has been healthy precisely because each addition respected the original
  architecture and design system rather than bypassing them.

---

## 4. System Analysis (Deeper View)

### 4.1 Stakeholders & Roles
| Role | Capability scope |
|------|------------------|
| Member | Personal spiritual tools; community participation; own data only. |
| Cell Leader | Member tooling + cell oversight: proxy actions, join-request approval, moderation, reports. |
| Council | Officer tier without a fixed cell; can join/create groups; cross-cell visibility per policy. |
| Youth President | Elevated content/authoring and oversight. |
| Pastor | Devotional/content authority and pastoral oversight. |
| Admin | System administration, bulk email, user management. |

### 4.2 Module Map (functional decomposition)
Identity · Devotional/Journal · Prayer · Feed · Leader Connect · Bible Reader/Search/Study ·
Verse Image · Bible Games + Leaderboards · Discipleship · Events/Attendance · Compassion
(participants, meditation, compliance) · Leadership tooling (proxy, join requests, bulk email,
birthdays) · Settings/Accessibility · Notifications.

### 4.3 Non-Functional Profile
Offline-first · Secure-by-default (RLS) · Accessible · ANR-free · Maintainable ·
Direct-distributable with in-app update awareness.

---

## 5. Overall Status of the App — Frontend to Backend

Legend: ✅ Shipped & stable · 🟡 Shipped, hardening / partial · 🟠 Code merged, deploy/config pending · 🔭 Planned

### 5.1 Presentation / UI (Frontend)
| Area | Status | Notes |
|------|:---:|------|
| Compose UI + design system | ✅ | 100% Compose, brand theming, micro-animations. |
| Theming (Light/Dark/System) | ✅ | Default Light; text scaling; burger-menu navigation. |
| Auth & profile screens | ✅ | Email + one-tap Google sign-in; profile setup. |
| Home / Devotional / Journal | ✅ | Today's Word, completion ring, streak, private reflection. |
| Prayer Wall / Life Feed | ✅ | Post/anonymous, intercede, reactions, comments, moderation. |
| Leader Connect (chat) | ✅ | Private messaging + check-ins. (Messenger-style replacement is roadmap.) |
| Bible reader / search / study | ✅ | Offline KJV, highlights, split-screen notes, sermon notes. |
| Verse image creator | ✅ | Backgrounds + font styles, share/save (client-only). |
| Bible Games + leaderboards | ✅ | 6 modes; daily reset; cell weekly + monthly Team boards. |
| Discipleship | ✅ | Daily curated activities, swap, completion, streak. |
| Events / QR attendance | ✅ | Listing/RSVP + QR attendance (canvas perf fixed). |
| Compassion + compliance UI | ✅ | Participant tracking, meditation, compliance report + PDF export. |
| Leader Proxy Mode | ✅ | Add member, proxy attendance/prayer/reflection, claim flow. |
| Settings / Accessibility | ✅ | Slim settings, scaling, theming. |
| 24-hour Notes | ✅ | Ephemeral notes, heart reactions, leader-hide moderation. |
| Update banner | ✅ | In-app new-version notice (direct-distribution compensator). |

### 5.2 Domain Layer
| Area | Status | Notes |
|------|:---:|------|
| Use cases / models / repo interfaces | ✅ | Clean separation; explicit mappers; one ViewModel per screen. |

### 5.3 Data Layer
| Area | Status | Notes |
|------|:---:|------|
| Room (source of truth) | ✅ | Entities, DAOs, converters; bundled KJV asset DB. |
| Repository sync (offline-first reads) | ✅ | Local-first emit, remote refresh on connectivity. |
| Offline cold-start resilience | ✅ | Network vs auth errors distinguished (no false logout offline). |
| Offline **writes** queue | 🟡 | Phase 2 of offline-first still pending. |
| Bible Games offline cache | 🟡 | Phase 3 of offline-first still pending. |
| DataStore preferences | ✅ | User prefs / theme / settings. |

### 5.4 Backend (Supabase + Firebase)
| Area | Status | Notes |
|------|:---:|------|
| PostgreSQL schema + migrations | ✅ | Broad normalized schema; versioned migration files. |
| Row Level Security | ✅ | Enabled on sensitive tables (some migrations need deploy — see below). |
| Auth (email + Google OAuth) | ✅ | Web + Android OAuth clients in the same Google Cloud project. |
| Realtime | ✅ | Live updates (profile/notes/etc.); cleanup leak fixed. |
| Storage | ✅ | Media/assets. |
| Edge Functions | 🟠 | Several functions shipped; a few features are code-merged with deploy + Vault secrets pending. |
| Scheduled jobs (pg_cron) | 🟠 | Monthly purge, birthdays 08:00 PHT, monthly compliance push — need deploy/secrets. |
| FCM push | ✅ | Token storage + notification handling (prayer/birthday/report/join-request flows). |
| Email (Resend) | 🟡 | Works to verified address; full sending blocked until domain verification. |
| Account deletion (data rights) | ✅ | `delete-account` Edge Function. |

### 5.5 Cross-Cutting Integrations
| Area | Status | Notes |
|------|:---:|------|
| WorkManager background jobs | ✅ | Sync, streak, reminders. |
| Glance home-screen widget | ✅ | Verse of the day. |
| Bible verse external API | 🔭 | Deferred; verses served from bundled DB (NKJV copyright blocked the preferred translation). |

**Bottom line:** the application is **functionally complete and stable on the client across
all major modules**; the open items are concentrated in **backend deployment/configuration**
(deploying a handful of Edge Functions, cron jobs, and Vault secrets, finishing email-domain
verification) and in **offline-write hardening**, not in missing app capability.

---

## 6. System Security

### 6.1 Authentication & Authorization
- **Supabase Auth** for email/password and **one-tap Google OAuth** (Web + Android clients
  provisioned in the same Google Cloud project — a hard-won configuration lesson).
- **Role-based access** (member → cell_leader → council → youth_president → pastor → admin)
  enforced both in the app's capability gating and, authoritatively, in the database.

### 6.2 Row Level Security (authoritative boundary)
- RLS is the primary security control: policies restrict reads/writes to the authenticated
  owner (e.g., `auth.uid() = user_id`), with role-scoped exceptions for leaders/pastors/admin.
- Because enforcement lives in Postgres, a compromised or buggy client cannot exceed a user's
  rights. **Operational note:** a few newer RLS migrations are *code-complete but await
  deployment* — these must be run so production matches the intended policy set.

### 6.3 Data Privacy
- **Private journals:** devotional reflections are visible only to their author (RLS), stored
  as plaintext-under-RLS so they survive reinstalls (a deliberate trade vs. on-device
  encryption that was losing data when the Keystore key was wiped on uninstall).
- **Anonymous prayers:** when a prayer is posted anonymously, the author's identity is hidden
  from the UI layer; user_id is never surfaced to other users for anonymous posts.
- **Minors:** the audience includes ages 13+; least-privilege defaults, leader moderation
  (post/note hide), and private-by-default personal data align with child-safety expectations.
- **Ephemeral content:** 24-hour notes auto-expire; prayers expire after 30 days.

### 6.4 Secrets Management
- App secrets (Supabase URL/keys, OAuth client IDs, etc.) are injected via **BuildConfig from
  `local.properties`**, which is git-ignored and **never committed**. A `local.properties.example`
  ships placeholders only.
- **Edge Function secrets** are read from the **Deno environment / Supabase Vault**, never
  hardcoded in the function source.
- Verified during this maintenance pass: no live keys are present in committed source; the only
  JWT-looking token in the repo is a header-only placeholder in `local.properties.example`.

### 6.5 Transport & Storage Security
- All client↔backend traffic is HTTPS/TLS (Supabase + FCM + Resend endpoints).
- Sensitive local material uses Android's secure storage primitives; the journal change above
  intentionally moved long-term secrecy of reflections to server-side RLS for recoverability.

### 6.6 Account & Data Rights
- Users can delete their account/data via the `delete-account` Edge Function — supporting a
  basic data-subject deletion right.

### 6.7 Known Security / Operational Items
| Item | Status | Action |
|------|:---:|------|
| Pending RLS migrations | 🟠 | Deploy outstanding policy migrations to production. |
| Supabase anon/service-role key deprecation | 🟡 | Still valid; migration recipe prepared for cutover. |
| Resend domain verification | 🟡 | Verify domain to lift sandbox send restriction. |
| Edge Function / cron secrets (Vault) | 🟠 | Set secrets + deploy for birthday/compliance/join-request flows. |
| Automated security/regression tests | 🔭 | Add to CI to guard RLS and auth invariants. |

---

## 7. Risks, Constraints & Technical Debt

- **Single-maintainer concentration** — primary continuity risk; mitigated by per-feature
  notes and this document.
- **Manual release coupling** — app builds depend on out-of-band SQL/Edge/secret deployment;
  a release checklist or migration automation is the top process fix.
- **Thin automated test coverage** — strongest quality-investment opportunity; start with
  RLS/auth invariants and the offline-first read/write paths.
- **Offline writes not yet queued** — reads are offline-first; writes still assume connectivity.
- **Platform reach** — Android-only by design; iOS remains deferred.
- **Third-party watch items** — Supabase key deprecation and Resend domain verification.

---

## 8. Roadmap (Selected)

- Finish offline-first Phases 2–3 (offline writes, Bible Games cache).
- Deploy outstanding Edge Functions, cron jobs, RLS migrations, and Vault secrets.
- Verify email sending domain; complete bulk-email rollout.
- Migrate off deprecated Supabase keys when required.
- Introduce automated tests + a formal release checklist/CI.
- Evolve Leader Connect toward a richer messaging experience.

---

## 9. Conclusion

Royals is a mature, security-conscious, offline-first youth-ministry platform whose **client
application is functionally complete and stable from UI through the data layer**, backed by a
**Supabase backend whose remaining work is largely deployment and configuration rather than
missing capability**. Its security model is sound by design — Row Level Security as the
authoritative boundary, private journals, anonymous prayer, disciplined secret handling, and a
data-deletion path. The clearest paths to the next maturity level are **operational**
(automated deployment/release discipline and automated tests) rather than architectural — a
strong position for a platform built and maintained by a single developer in service of a
real ministry.

---

*Royals — The Kingdom Builders · Android · Kotlin + Jetpack Compose + Supabase · June 2026*
