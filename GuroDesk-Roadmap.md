# GuroDesk — Build Roadmap & Master Spec
### A teaching command center for a DepEd Senior High School teacher (and his students)

> Codename: **GURO** · Package: `com.gurodesk.app` · Brand name swappable later (like GRACE → Royals).
> Stack reused from the Royals/GRACE project: Kotlin + Jetpack Compose + Hilt + Room + Supabase, offline-first.
> This single file is the source of truth for the new project. Lift **Part 4** into `CLAUDE.md` and **Part 5** into `docs/build-prompts.md`.

---

## How to use this document
1. Create a new Android Studio project (package `com.gurodesk.app`).
2. Copy **Part 4 — Master Spec** into the project root as `CLAUDE.md`.
3. Copy **Part 5 — Build Prompts** into `docs/build-prompts.md`.
4. Paste prompts one per session, in order (0 → 8).
5. Phases 0–5 give a fully usable **teacher-only** app. Phases 6–8 add the **student** experience.

---

# Part 1 — Concept & End Users

**What it is:** A personal teaching command center. A Senior High teacher manages his sections,
students, daily attendance, and a by-the-book **DepEd SHS class record** that auto-computes
quarterly + semester grades. Students log in to see *their own* grades (when released), attendance,
schedule, announcements, and to turn in assignments.

**End users — two roles, one app (one APK):**

| Role | What they do |
|------|--------------|
| **Teacher** | Owns all class data. Setup, attendance, DepEd grading, releases grades, posts announcements & assignments, approves student link requests. |
| **Student** | Sees only their OWN grades (after release), attendance, schedule, announcements; submits assignments. Never sees other students' data. |

Role is chosen at sign-up and gates the whole navigation graph (TeacherGraph vs StudentGraph) —
same pattern as the Royals member/leader split.

**Locked design decisions:**
- Grading system: **DepEd SHS** (DepEd Order 8, s. 2015) — see Part 4 grading engine.
- Grade levels: **Senior High (G11–12)**.
- Teacher role: **adviser + subject teacher**.
- Platform: **Android**, reusing the Royals/GRACE stack.
- Student join method: **join code + claim + teacher approval** (LRN auto-match).
- First student release scope: **View + assignments**.

**Scope guardrail:** the app only knows grades for subjects the teacher personally teaches.
Full SF9 report-card consolidation (needs all subject teachers) is deferred to Phase 9+.

---

# Part 2 — Core Feature Set (the MVP, teacher side)

Six modules in dependency order — each maps to a build prompt.

| # | Module | What it does |
|---|--------|--------------|
| **A** | **Auth + Term Setup** | Sign in; teacher profile; set active School Year + Semester (SHS = 2 semesters). |
| **B** | **Sections · Subjects · Classes · Students** | Sections (level/track/strand), subjects (with type → weight preset), classes (teaching loads), student rosters (LRN, name, sex, guardian), bulk add. |
| **C** | **Schedule + Home/Today** | Weekly timetable; "Today" dashboard with one-tap to attendance/gradebook. |
| **D** | **Attendance** | Per class/day: Present/Late/Absent/Excused; running %; homeroom attendance for advisory; offline-first. |
| **E** | **Class Record (DepEd SHS gradebook)** | WW/PT/QA items → scores → Initial Grade → Transmuted Quarterly → Semester Final; weights by subject type; pass/fail coloring; class averages. |
| **F** | **Exports + Settings + Deploy** | CSV export of class record & attendance; settings; signed release APK; Drive distribution. |

---

# Part 3 — Full Roadmap

| Phase | Ships | Est. (at your GRACE pace) |
|-------|-------|---------------------------|
| **0** | Bootstrap (reuse GRACE skeleton, new package/theme/CLAUDE.md) | ~½ session |
| **1** | Foundation: Term, Sections, Subjects, Classes, Students (Modules A+B) | 1–2 sessions |
| **2** | Schedule + Home/Today (Module C) | ~1 session |
| **3** | Attendance (Module D) | ~1 session |
| **4** | DepEd SHS Gradebook (Module E) — the big one | 2–3 sessions |
| **5** | Exports + Settings + **Deployment** (Module F) — *usable teacher app* | ~1 session |
| **6** | Roles + student onboarding (join code, claim/approve, student RLS) | 1–2 sessions |
| **7** | Student dashboard (My Grades/Attendance/Schedule/Announcements) + grade release + push | 1–2 sessions |
| **8** | Assignments + submissions (Storage, due dates, marking) | 1–2 sessions |
| **9+** | Later: messaging, SF9/SF2/SF5 PDFs, advisory consolidation, analytics, QR attendance, seating, behavior logs, classroom tools | TBD |

**Dependency note:** student features (6–8) come after teacher core because students consume data
the teacher creates. This is sequencing, not deprioritization.

**Deployment model:** GitHub repo (no Claude attribution, brand assets gitignored) + signed APK in
Google Drive. No Play Store. Supabase = live cloud backup.

---

# Part 4 — Master Spec  (copy this into `CLAUDE.md`)

```markdown
# GuroDesk — Master Spec (codename GURO)
# A teaching command center for one DepEd Senior High teacher and his students.

## ROLE
Senior Android engineer (Kotlin + Jetpack Compose), UI/UX designer, debugger, and QA
— all four at once. Clean Architecture + MVVM, offline-first (Room = source of truth,
Supabase = sync/backup). Production-grade Kotlin that compiles first try. No TODOs,
no stubs, full files. State plan before coding. End each task with a QA checklist.

## PRODUCT
Two roles in ONE app: TEACHER and STUDENT (role on profiles, chosen at sign-up, gates
the NavGraph). SHS context: 2 semesters/year, 2 quarters/semester. Passing grade = 75.
A teacher owns all their class data. A student sees ONLY their own grades (after release),
attendance, schedule, announcements, and assignments. Students NEVER see other students'
data — enforced by RLS at the database, not just the UI.

## TECH STACK (reuse from the Royals/GRACE project verbatim)
Kotlin 2.0 · Compose BOM 2024.06 · Hilt 2.51 · Room 2.6 · Supabase BOM 2.5.4
(Postgrest/Auth/Realtime/Storage/Functions) · DataStore · WorkManager · Coil ·
Navigation Compose · Firebase Messaging (push). Secrets via BuildConfig from
local.properties. KSP for Room/Hilt.

## ARCHITECTURE RULES
- Layers: presentation → domain → data. Domain has ZERO Android/Supabase imports.
- Every screen: UiState data class + Event sealed interface + (optional) Effect + one ViewModel.
- StateFlow + collectAsStateWithLifecycle. viewModelScope in VMs, Dispatchers.IO in repos.
- Every suspend fn wrapped in try/catch → Result.Error(human-readable message).
- Explicit mappers per layer: dto.toDomain(), entity.toDomain(), domain.toEntity().
- Offline-first repos: emit Room first, then sync Supabase when online; queue writes offline.
- All user-facing strings in strings.xml. No hardcoded secrets. No Claude attribution anywhere.
- Every Realtime/Flow subscription cancelled in onCleared().

## DEPED SHS GRADING ENGINE  (DepEd Order 8, s. 2015 — implement EXACTLY)

### Subject types → component weights (WW / PT / QA)
| subject_type     | Written Work | Performance Task | Quarterly Assessment |
|------------------|:---:|:---:|:---:|
| CORE             | 25% | 50% | 25% |
| ACAD_OTHER       | 25% | 45% | 30% |
| ACAD_IMMERSION*  | 35% | 40% | 25% |
| TVL_OTHER        | 20% | 60% | 20% |
| TVL_IMMERSION*   | 20% | 60% | 20% |
*Immersion = Work Immersion / Research / Business Enterprise Simulation / Exhibit / Performance.

### Per-quarter computation (per student, per class)
1. For each component C in {WW, PT, QA}:
   totalRaw  = sum of student's raw scores across all items in C
   totalHigh = sum of highest-possible scores across all items in C
   PS(C)     = (totalRaw / totalHigh) * 100      // Percentage Score; skip empty components
   WS(C)     = PS(C) * weight(C)                  // Weighted Score
2. InitialGrade  = WS(WW) + WS(PT) + WS(QA)
3. QuarterlyGrade = transmute(InitialGrade)        // table below; floor 60
- Blank score = not yet entered → exclude that item's high score from totals (DEFAULT).
- An explicit 0 counts as 0. "Excused" item for a student → exclude that item from both totals
  for that student. Provide a per-class setting "count missing work as 0" (default OFF).

### Semester grade
SemesterFinalGrade = round( (Quarter1Grade + Quarter2Grade) / 2 )
GeneralAverage = average of all subjects' SemesterFinalGrades.
With Honors 90–94 · With High Honors 95–97 · With Highest Honors 98–100.

### Transmutation table (Initial Grade range → Quarterly Grade) — hardcode as lookup
100 →100 · 98.40-99.99→99 · 96.80-98.39→98 · 95.20-96.79→97 · 93.60-95.19→96 ·
92.00-93.59→95 · 90.40-91.99→94 · 88.80-90.39→93 · 87.20-88.79→92 · 85.60-87.19→91 ·
84.00-85.59→90 · 82.40-83.99→89 · 80.80-82.39→88 · 79.20-80.79→87 · 77.60-79.19→86 ·
76.00-77.59→85 · 74.40-75.99→84 · 72.80-74.39→83 · 71.20-72.79→82 · 69.60-71.19→81 ·
68.00-69.59→80 · 66.40-67.99→79 · 64.80-66.39→78 · 63.20-64.79→77 · 61.60-63.19→76 ·
60.00-61.59→75 · 56.00-59.99→74 · 52.00-55.99→73 · 48.00-51.99→72 · 44.00-47.99→71 ·
40.00-43.99→70 · 36.00-39.99→69 · 32.00-35.99→68 · 28.00-31.99→67 · 24.00-27.99→66 ·
20.00-23.99→65 · 16.00-19.99→64 · 12.00-15.99→63 · 8.00-11.99→62 · 4.00-7.99→61 · 0-3.99→60
(Make transmutation a Settings toggle, default ON — some schools report raw initial grades.)

## DATA SCHEMA (Supabase Postgres; mirror each in Room)

### Teacher-owned (RLS: teacher_id = auth.uid())
- teachers(id=auth.uid, name, email, position, school_name, avatar_url, created_at)
- school_terms(id, teacher_id, school_year, semester[1|2], is_active, start_date, end_date)
- sections(id, teacher_id, term_id, name, grade_level[11|12], track, strand, is_advisory, join_code)
- subjects(id, teacher_id, term_id, name, code, subject_type[CORE|ACAD_OTHER|ACAD_IMMERSION|TVL_OTHER|TVL_IMMERSION])
- classes(id, teacher_id, term_id, section_id, subject_id, room, is_homeroom)   -- a teaching load
- class_schedule(id, teacher_id, class_id, day_of_week[1-7], start_time, end_time, room)
- students(id, teacher_id, section_id, lrn, last_name, first_name, middle_name, sex,
           birthdate, contact, guardian_name, guardian_contact, photo_url, auth_user_id NULL)
- enrollments(class_id, student_id)                          -- PK(class_id, student_id)
- attendance_sessions(id, teacher_id, class_id, date)        -- one per class/day
- attendance_records(session_id, student_id, status[PRESENT|LATE|ABSENT|EXCUSED], note)
- assessments(id, teacher_id, class_id, quarter[1|2], component[WW|PT|QA], title,
              highest_possible_score, date, order_index)
- scores(assessment_id, student_id, raw_score NULL, is_excused bool)  -- PK(assessment_id, student_id)
- grade_releases(teacher_id, class_id, quarter, is_published)
- announcements(id, teacher_id, class_id, title, body, created_at)
- assignments(id, teacher_id, class_id, title, instructions, due_at, attachment_url,
              points, linked_assessment_id NULL, created_at)
- offline_sync(id, teacher_id, action, payload_json, created_at, retry_count)

### Identity / cross-role
- profiles(id=auth.uid, role[TEACHER|STUDENT], name)         -- gates the app
- link_requests(id, section_id, student_record_id, requesting_user_id,
                status[PENDING|APPROVED|REJECTED], created_at)
- submissions(id, assignment_id, student_id, status[ASSIGNED|SUBMITTED|LATE|RETURNED],
              file_url, text_note, submitted_at, marked_score NULL, feedback)

Notes:
- Homeroom attendance = a class with is_homeroom=true (subject_id nullable).
- quarter is 1 or 2 RELATIVE TO the active semester (UI labels Sem1→Q1/Q2, Sem2→Q3/Q4).
- Join code lives on SECTIONS (roster is section-scoped); one claim links a student to all
  classes that section is enrolled in for this teacher.

## RLS — STUDENT READ ACCESS (write & verify each; the safeguarding core)
- A STUDENT may SELECT a scores/assessments row only for classes their linked student record
  (students.auth_user_id = auth.uid()) is enrolled in, AND only when grade_releases.is_published
  is true for that class+quarter.
- attendance_records: student SELECT only where the record's student's auth_user_id = auth.uid().
- sections/subjects/classes/enrollments/class_schedule/announcements: student SELECT only their
  own enrolled rows.
- assignments: student SELECT for enrolled classes. submissions: student may read/write ONLY
  their own (student_id linked to auth.uid()). Teacher has full access to their own classes.
- A student must NEVER be able to read another student's score, attendance, or submission.
  Add a QA test that proves a cross-student SELECT returns zero rows.

## DESIGN SYSTEM (clean, calm, professional — Light default)
Primary Indigo #4338CA · Secondary Teal #14B8A6
Bg(light) #F7F8FB · Surface #FFFFFF · TextPrimary #1A1C2A · TextDim #6B7280
Present #16A34A · Late #F59E0B · Absent #DC2626 · Excused #64748B
Grade pass ≥75 #16A34A · fail <75 #DC2626
Fonts: clean sans for UI (Inter/Roboto). Rounded 16dp cards, 16/20dp padding,
48dp min tap targets, AnimatedVisibility/animateFloatAsState for transitions.
Light/Dark/System theming (default Light — classrooms are bright). Text-scaling friendly.

## CONVENTIONS
Kotlin only · Compose only · no LiveData · one VM per screen · no business logic in
Composables · comment WHY not WHAT.
```

---

# Part 5 — Build Prompts  (copy into `docs/build-prompts.md`, paste one per session)

### ▶ PROMPT 0 — Bootstrap (reuse the Royals foundation)
```
Read CLAUDE.md fully. You are bootstrapping GuroDesk (codename GURO), a two-role DepEd SHS app,
reusing the architecture of my existing Royals/GRACE project.

Set up the project foundation:
1. gradle/libs.versions.toml with the exact stack in CLAUDE.md.
2. app/build.gradle.kts: package com.gurodesk.app, compileSdk 35, minSdk 26, versionCode 1,
   versionName "1.0.0", buildConfig fields SUPABASE_URL + SUPABASE_ANON_KEY from local.properties.
3. local.properties template (placeholders only) + .gitignore (local.properties, *.jks, /build,
   brand assets folder).
4. GuroApplication (@HiltAndroidApp) + MainActivity (@AndroidEntryPoint) hosting a NavHost.
   Offline banner via NetworkMonitor.
5. Hilt modules: NetworkModule (SupabaseClient: Postgrest/Auth/Realtime/Storage), DatabaseModule
   (GuroDatabase + all DAOs), RepositoryModule (@Binds), DataStoreModule.
6. Theme from CLAUDE.md design system (Light default, Light/Dark/System).
7. NetworkMonitor + OfflineSyncWorker scaffolding (reuse Royals pattern).
8. Auth: email/password sign-in + sign-up, session persistence, silent refresh.
   (Role split + student flow comes in Prompt 6 — for now everyone is a teacher.)
End with: file list, dependency graph, what Prompt 1 builds.
```

### ▶ PROMPT 1 — Foundation: Term, Sections, Subjects, Classes, Students
```
Read CLAUDE.md. Prompt 0 is done. Build the foundation data layer + setup UI (offline-first).

Schema/Room/DTO/mapper/DAO + repository + use cases + screens for:
- school_terms: create + switch active term (School Year + Semester). First-launch setup flow.
- sections: CRUD (name, grade level 11/12, track, strand, mark one as advisory).
- subjects: CRUD with subject_type picker — show the resulting WW/PT/QA weights live on pick.
- classes (teaching loads): pick section + subject + room → create; auto-enroll the section's
  students. Auto-create a homeroom class for the advisory section.
- students: CRUD (LRN, last/first/middle name, sex, guardian) + BULK ADD (paste one per line,
  "Last, First Middle" parsing, like the Royals member-paste helper).
A "Classes" screen lists teaching loads grouped by section. Empty states with guidance.
Owner-based RLS (teacher_id = auth.uid()) on every table. End with QA checklist + what Prompt 2 builds.
```

### ▶ PROMPT 2 — Schedule + Home/Today dashboard
```
Read CLAUDE.md. Build the weekly schedule and the Home dashboard.
- class_schedule CRUD: day(s)/time/room per class; weekly timetable grid view.
- HomeViewModel/HomeScreen "Today": greeting + date; TODAY's classes in time order
  (section · subject · time · room) each with quick actions "Take Attendance" / "Open Grades".
  Show counts (sections, students, classes this term) and any class with no attendance today.
- Active-term banner; switch-term affordance.
Animations per design system. End with QA checklist + what Prompt 3 builds.
```

### ▶ PROMPT 3 — Attendance
```
Read CLAUDE.md. Build attendance, offline-first.
- attendance_sessions + attendance_records.
- From a class → "Attendance" → pick date (default today) → roster; tap cycles
  PRESENT→LATE→ABSENT→EXCUSED (color-coded), plus "Mark all present". Optional per-student note.
- Save creates/updates the session; queue to offline_sync when offline, sync on reconnect.
- Per-student attendance summary (% present, counts) and per-class daily summary.
- Homeroom attendance for the advisory section (is_homeroom class).
End with QA checklist (incl. offline mark → reconnect → sync) + what Prompt 4 builds.
```

### ▶ PROMPT 4 — DepEd SHS Class Record (the grading engine)
```
Read CLAUDE.md — implement the GRADING ENGINE section EXACTLY.
- assessments + scores tables/Room/DTO/mappers/DAOs/repo/use cases.
- Pure-Kotlin domain GradingCalculator: weights-by-subject-type, percentage→weighted→initial,
  transmutation lookup, quarterly grade, semester final, general average. UNIT-TESTABLE, zero
  Android imports. JUnit tests: each subject_type's weights, a full worked example, transmutation
  boundaries (initial 60.00→75, 59.99→74, 100→100), excused-item exclusion, empty-component handling.
- UI per class: quarter tabs (labeled per active semester). WW/PT/QA sections; add assessment item
  (title, highest score, date). Score-entry grid (students × items) with fast numeric input,
  per-item highest-score header, "excuse" toggle per cell.
- Live computed columns: per-student Initial Grade + Quarterly Grade (transmuted), pass/fail color.
  Semester view: Q1, Q2, Semester Final per student + class average + General Average.
- Transmutation toggle from Settings respected.
End with QA checklist + what Prompt 5 builds.
```

### ▶ PROMPT 5 — Exports, Settings, Deployment
```
Read CLAUDE.md. Final teacher-core phase — make data portable and ship.
- CSV export: a class record (students × items + computed grades) and an attendance sheet,
  written to app storage + shared via Intent (build CSV manually, no extra deps).
- Settings: profile edit, switch active term, theme (Light/Dark/System), transmutation on/off,
  about (BuildConfig.VERSION_NAME + VERSION_CODE), sign out, delete account.
- Deployment prep: signed release config (signingConfigs from keystore in local.properties,
  NEVER committed), ProGuard keep rules for Supabase/Serialization/Room, strip Log in release.
- Pre-ship checklist: builds debug + release clean, no hardcoded strings/secrets, all suspend in
  try/catch, RLS verified owner-only, offline write→sync verified.
Then give exact steps to: (1) generate the keystore, (2) build the signed APK, (3) version-bump
scheme, (4) upload to Google Drive. End with launch-readiness assessment.
--- After this prompt the teacher app is DEPLOYED and usable solo. ---
```

### ▶ PROMPT 6 — Roles + Student Onboarding + RLS
```
Read CLAUDE.md. Teacher core (Prompts 0–5) is done. Make GuroDesk a two-role app.

1. profiles(id, role[TEACHER|STUDENT], name) + RLS. On sign-up, user picks Teacher or Student.
   Teacher → existing setup flow. Student → join flow below. Role gates the NavGraph
   (TeacherGraph vs StudentGraph), like the Royals member/leader split.
2. sections.join_code (auto-generated, regeneratable). Teacher "Class Code" screen shows the code
   per section to share with students.
3. link_requests table + CLAIM FLOW (reuse the Royals claim pattern):
   Student: enter join code → see that section's roster (names only) → pick own name →
   verify LRN or birthdate → submit link_request. AUTO-APPROVE if LRN matches; else pending.
4. Teacher approval inbox (reuse the group-join-requests inbox UI): approve/reject. On approve:
   students.auth_user_id = requester's id.
5. STUDENT RLS (critical — write and verify each policy from CLAUDE.md RLS section):
   - scores/assessments: enrolled-only (release gating added in Prompt 7).
   - attendance_records: own-only. sections/subjects/classes/enrollments/schedule: own enrolled only.
   - A student can NEVER read another student's data — add a QA test proving a cross-student
     SELECT returns zero rows.
End with: claim-flow diagram, full RLS policy list, QA checklist (incl. cross-student leak test).
```

### ▶ PROMPT 7 — Student Dashboard + Grade Release + Announcements + Push
```
Read CLAUDE.md. Prompt 6 done. Build the student experience + the teacher's release controls.

1. grade_releases(teacher_id, class_id, quarter, is_published). Teacher gradebook gets a
   "Release / Unrelease Q grades" toggle per class+quarter. Grades hidden from students until
   released — enforce in BOTH the RLS policy and the UI.
2. Student Home: greeting, today's schedule, latest announcements, "grades released" notices.
3. My Grades: per subject → WW/PT/QA items + computed Quarterly + Semester grade, pass/fail color.
   REUSE the domain GradingCalculator (read-only). Released quarters only.
4. My Attendance: overall % present + per-class breakdown + recent records (color-coded).
5. My Schedule: weekly timetable of the student's enrolled classes.
6. announcements table + teacher composer (post to a class/section) + student feed.
7. Push (reuse the Royals FCM setup): notify student on grade release, on being marked absent,
   on new announcement. Respect per-channel notification toggles in Settings.
End with QA checklist (released vs unreleased visibility, push delivery) + what Prompt 8 builds.
```

### ▶ PROMPT 8 — Assignments + Submissions
```
Read CLAUDE.md. Prompt 7 done. Build assignments with student submissions.

1. assignments + submissions tables + Storage buckets (assignment-files, submissions) + RLS:
   student sees assignments for enrolled classes; student may read/write ONLY their own submission;
   teacher full access to own classes.
2. Teacher: create assignment (class, title, instructions, due date, optional attachment upload,
   optional points, optional link to a PT/WW assessment item). Roster submission tracker
   (Assigned / Submitted / Late / Returned per student). Open a submission → mark score + feedback
   → "Return". If linked_assessment_id is set, writing the score updates the gradebook score row.
3. Student: assignment list with due dates + status badges; open assignment; submit (file upload
   and/or text note); auto-mark LATE if submitted past due_at; see returned feedback + score.
4. Storage uploads atomic: if upload fails, do NOT create the submission/assignment row.
   Offline-aware (queue text-only submissions; uploads require connectivity — show clear state).
End with QA checklist (on-time vs late, upload failure, grade-link writes through) + launch notes.
--- After this prompt: full teacher + student app. ---
```

---

# Part 6 — Reusable patterns from Royals/GRACE (saves time)

| GuroDesk need | Reuse from Royals |
|---------------|-------------------|
| Two roles, role-gated nav | member/leader/admin role gating |
| Student claim-their-record flow | Leader Proxy Mode **claim flow** |
| Teacher approval inbox | **group join requests** approve/reject + push |
| Sign-in (low-friction for students) | **Google OAuth** (same-project clients!) |
| Push notifications | **FCM** setup + notification channels |
| Live updates | Supabase **Realtime** subscription pattern |
| Offline writes | **OfflineSyncWorker** + offline_sync queue |
| Bulk roster paste | member-paste "Last, First" parser |
| Light/Dark/System theming | accessibility redesign theming |

---

# Part 7 — Open questions / flags before building

1. **School policy:** Confirm his school allows students to view grades **in-app** (some mandate
   grades only via official report cards). The **release toggle** mitigates this — grades are
   invisible until he taps "Release."
2. **Missing-work rule:** DepEd teachers differ on whether un-submitted work = blank (ignored) or
   = 0. Default is **blank-ignored** with a per-class "count missing as 0" toggle. Confirm his preference.
3. **Transmutation:** default ON (by-the-book). Some schools report raw initial grades — toggle exists.
4. **Track/strand specifics:** confirm the strands he teaches (STEM/ABM/HUMSS/GAS/TVL…) so subject
   defaults can be pre-seeded.
5. **Brand name + palette:** "GuroDesk" + Indigo/Teal are placeholders — swap before release.

---

*GuroDesk — Kotlin + Jetpack Compose + Supabase · DepEd SHS · Single teacher + students*
*Built on the architecture of the Royals/GRACE project.*
