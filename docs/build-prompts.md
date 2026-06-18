# GRACE — Build Prompts (Archive)

Historical Claude Code prompts used to scaffold the app during initial build (Prompts 1–10, plus a second pass of Prompts 1–9). Kept for reference only — they describe the project as it was being built. The app is well past Phase 1–8 now, so do **not** treat these as current architecture guidance. For current rules, see `CLAUDE.md` in the project root.

---
## 🚀 COMPLETE CLAUDE CODE PROMPTS — COPY & PASTE DIRECTLY

> Each prompt below is 100% complete and self-contained. Copy the entire block into Claude Code in Android Studio. Do not modify. Do not add to it. Paste and run.

---

### ▶ PROMPT 1 — Project Bootstrap & Hilt Setup

```
Act as a Senior Software Engineer specializing in Android Mobile Application Development, UI/UX Design, Debugging, and Quality Assurance Testing. Apply all four roles simultaneously throughout this task.

You are setting up the GRACE app — a youth ministry Android application built with Kotlin + Jetpack Compose + Supabase for a church in the Philippines. Read CLAUDE.md in this project root for the full specification, architecture rules, design system, database schema, and coding conventions before writing anything.

YOUR TASK: Set up the complete Android project foundation from scratch. Write every file in full — no stubs, no TODOs, no partial implementations.

As Mobile Developer — implement:
1. Create `gradle/libs.versions.toml` using the exact versions in CLAUDE.md TECH STACK section. Include every library. Zero substitutions.
2. Update `settings.gradle.kts` to enable the version catalog with name "libs".
3. Update root `build.gradle.kts` to declare all plugins from the catalog without applying them at root level.
4. Update `app/build.gradle.kts`:
   - Apply plugins: android-application, kotlin-android, kotlin-compose, kotlin-serialization, hilt, ksp, google-services
   - compileSdk = 35, minSdk = 26, targetSdk = 35, versionCode = 1, versionName = "1.0.0"
   - Package: com.grace.app
   - Add all dependencies from the catalog
   - Add buildConfigField for SUPABASE_URL, SUPABASE_ANON_KEY, BIBLE_API_KEY — read from local.properties via Properties()
   - Enable buildConfig = true in buildFeatures
5. Create `local.properties` template with clearly marked placeholder values (never real keys):
   sdk.dir=/path/to/your/android/sdk
   SUPABASE_URL=https://your-project-ref.supabase.co
   SUPABASE_ANON_KEY=your-supabase-anon-key-here
   BIBLE_API_KEY=your-bible-api-key-here
6. Create `GraceApplication.kt` annotated @HiltAndroidApp. In onCreate(), create all four Android notification channels: CHANNEL_PRAYER (HIGH), CHANNEL_DEVOTIONAL (DEFAULT), CHANNEL_MESSAGES (HIGH), CHANNEL_COMMUNITY (LOW). Register DevoSyncWorker as a PeriodicWorkRequest (nightly at 2 AM using ExistingPeriodicWorkPolicy.KEEP).
7. Create `MainActivity.kt` annotated @AndroidEntryPoint. Observe NetworkMonitor.networkState. Show an offline banner (GraceOrange, slide from top via AnimatedVisibility) when offline. Host NavHost inside a Scaffold with BottomNavBar. Switch between AuthGraph and MainGraph based on session state from DataStore.
8. Create all four Hilt modules in di/:
   - NetworkModule.kt: SupabaseClient singleton with Postgrest, Auth, Realtime, Storage, Functions installed. BibleApiService via Retrofit with OkHttpClient (Authorization header + BODY logging in DEBUG only).
   - DatabaseModule.kt: GraceDatabase singleton. Provides DevotionalDao, PrayerDao, PostDao, MessageDao, UserDao, VerseDao, OfflineSyncDao.
   - RepositoryModule.kt: @Binds all repository interfaces to implementations.
   - DataStoreModule.kt: DataStore<Preferences> singleton via preferencesDataStore(name = "grace_prefs").
9. Create `GraceDatabase.kt` with version = 1, exportSchema = true, registered entities: DevotionalEntity, PrayerEntity, PostEntity, MessageEntity, UserEntity, VerseEntity, OfflineSyncEntity.
10. Create all Room entities in data/local/entity/. Each entity mirrors its Supabase table exactly per the schema in CLAUDE.md. Add @Entity, @PrimaryKey(autoGenerate = false), @ColumnInfo annotations.
11. Create stub DAO interfaces in data/local/dao/ — each must have: getAll(): Flow<List<Entity>>, getById(id: String): Entity?, insert(entity), insertAll(list), delete(entity).
12. Create `NetworkMonitor.kt` in data/util/: wraps ConnectivityManager, exposes isOnline: Boolean and networkState: StateFlow<Boolean>. On connectivity restored, enqueue OfflineSyncWorker.
13. Create `.gitignore` ignoring: local.properties, google-services.json, *.jks, /build, /.gradle, /.idea, *.keystore.

As UI/UX Designer — verify:
- MainActivity uses GraceTheme wrapping the entire app content
- BottomNavBar has 5 tabs (Home, Devo, Prayer, Feed, Leaders) with GraceGold active indicator dot
- Offline banner uses GraceOrange color with 📡 icon and is animated with AnimatedVisibility(slide from top)
- Status bar color matches GraceDeepBlue

As Debugger — before finishing, verify:
- GraceApplication is registered as android:name in AndroidManifest.xml
- All DAOs are registered in GraceDatabase's @Database entities array
- All Hilt modules use correct @InstallIn scopes (SingletonComponent for all of these)
- local.properties is listed in .gitignore
- All notification channels are created before any notification is sent

As QA Tester — write a checklist after completing the code:
- [ ] Project syncs without Gradle errors
- [ ] App builds in debug variant without compile errors
- [ ] GraceApplication.onCreate() runs without crash on cold start
- [ ] Offline banner appears when airplane mode is ON
- [ ] Offline banner disappears when connectivity returns
- [ ] All 4 notification channels visible in Android Settings > Apps > GRACE > Notifications
- [ ] .gitignore correctly excludes local.properties (verify with `git status`)

End your response with: files created (list), what to test first, and what Prompt 2 will build.
```

---

### ▶ PROMPT 2 — Core Data Layer: Domain Models, DTOs, Mappers, Use Cases

```
Act as a Senior Software Engineer specializing in Android Mobile Application Development, UI/UX Design, Debugging, and Quality Assurance Testing. Apply all four roles simultaneously.

You are building the GRACE youth ministry Android app. Read CLAUDE.md for full context. Prompt 1 (project bootstrap) is complete — the project compiles with all Hilt modules, Room database, and navigation skeleton in place.

YOUR TASK: Build the complete domain model layer, Supabase DTO layer, mapper functions, repository interfaces, and use case shells. This is the skeleton every feature will depend on. Write every file completely.

As Mobile Developer — implement:

DOMAIN MODELS (domain/model/) — pure Kotlin, zero Android or Supabase imports:
1. `User.kt`: data class with id: String, email: String, name: String, avatarUrl: String?, role: UserRole, groupId: String?, streak: Int, lastDevoAt: LocalDate?, fcmToken: String?
   Create enum class UserRole { MEMBER, CELL_LEADER, YOUTH_PRESIDENT, PASTOR, ADMIN }
2. `Prayer.kt`: id, userId: String? (null when anonymous in UI), userName: String? (null when anonymous), content, isAnonymous: Boolean, category: PrayerCategory, status: PrayerStatus, prayCount: Int, isFlagged: Boolean, createdAt: LocalDateTime
   Create enum PrayerCategory { FAMILY, SCHOOL, FAITH, HEALTH, PERSONAL, NATIONS }
   Create enum PrayerStatus { ACTIVE, ANSWERED, ARCHIVED }
3. `Devotional.kt`: id, scheduledDate: LocalDate, title, verseRef, verseText, reflection, prayerStarter, journalPrompt, planId: String?
4. `Post.kt`: id, userId, userName, userAvatarUrl: String?, type: PostType, content, imageUrl: String?, verseRef: String?, isHighlighted: Boolean, reactions: Map<String, Int>, myReaction: String?, commentCount: Int, createdAt: LocalDateTime
   Create enum PostType { TEXT, PHOTO, SCRIPTURE, PROMPT }
5. `Message.kt`: id, senderId, receiverId, content, isRead: Boolean, sentAt: LocalDateTime, isFailed: Boolean = false
6. `Group.kt`: id, name, leaderId, description: String?

RESULT WRAPPER (domain/util/):
7. `Result.kt`: sealed class Result<out T> { data class Success<T>(val data: T), data class Error(val message: String, val cause: Throwable? = null), object Loading } — add extension functions: Result<T>.onSuccess{}, Result<T>.onError{}, Result<T>.isLoading

SUPABASE DTOs (data/remote/supabase/dto/) — all annotated @Serializable with exact @SerialName snake_case matching Supabase columns:
8. `UserDto.kt`: mirrors users table exactly
9. `PrayerDto.kt`: mirrors prayers table exactly
10. `DevotionalDto.kt`: mirrors devotionals table exactly
11. `PostDto.kt`: mirrors posts table exactly
12. `MessageDto.kt`: mirrors messages table exactly
13. `VerseDto.kt` (Bible API response): data class VerseResponse(@SerialName("passages") val passages: List<String>, @SerialName("query") val query: String)

MAPPERS — create extension functions in data/remote/supabase/dto/mapper/:
14. `PrayerMappers.kt`: PrayerDto.toDomain(), PrayerDto.toEntity(), PrayerEntity.toDomain()
    IMPORTANT: In PrayerDto.toDomain(), if isAnonymous == true, set userId = null and userName = null in the returned Prayer domain object. Add a comment: "Anonymous prayer safeguarding: user identity never surfaces to UI layer for anonymous posts, regardless of what the DTO contains."
15. `DevotionalMappers.kt`: DevotionalDto.toDomain(), DevotionalDto.toEntity(), DevotionalEntity.toDomain()
16. `PostMappers.kt`: PostDto.toDomain(), PostDto.toEntity(), PostEntity.toDomain()
17. `UserMappers.kt`: UserDto.toDomain(), UserDto.toEntity(), UserEntity.toDomain() — map role string to UserRole enum safely with a fallback to UserRole.MEMBER
18. `MessageMappers.kt`: MessageDto.toDomain(), MessageDto.toEntity(), MessageEntity.toDomain()

REPOSITORY INTERFACES (domain/repository/) — interfaces only, no implementation:
19. `AuthRepository.kt`: suspend fun signIn(email, password): Result<User>; suspend fun signUp(email, password, name): Result<Unit>; suspend fun signOut(): Result<Unit>; fun currentUser(): Flow<User?>; suspend fun deleteAccount(): Result<Unit>
20. `DevotionalRepository.kt`: fun getTodayDevotional(): Flow<Result<Devotional>>; suspend fun markComplete(devoId: String, journalEntry: String): Result<Unit>; fun getStreak(): Flow<Int>; suspend fun syncUpcomingDevotionals(): Result<Unit>
21. `PrayerRepository.kt`: fun getPrayers(category: PrayerCategory?): Flow<Result<List<Prayer>>>; suspend fun postPrayer(content: String, isAnonymous: Boolean, category: PrayerCategory): Result<Unit>; suspend fun intercede(prayerId: String): Result<Unit>; suspend fun markAnswered(prayerId: String): Result<Unit>; fun subscribeToPrayCount(prayerId: String): Flow<Int>
22. `FeedRepository.kt`: fun getPosts(): Flow<Result<List<Post>>>; suspend fun createPost(type: PostType, content: String, imageUri: android.net.Uri?, verseRef: String?): Result<Unit>; suspend fun react(postId: String, reactionType: String): Result<Unit>
23. `LeaderRepository.kt`: fun getMyLeader(): Flow<Result<User?>>; fun getMessages(leaderId: String): Flow<Result<List<Message>>>; suspend fun sendMessage(receiverId: String, content: String): Result<Unit>; suspend fun submitCheckIn(answers: Map<String, String>): Result<Unit>; fun getAllLeaders(): Flow<Result<List<User>>>

USE CASES (domain/usecase/) — each is a class with operator fun invoke() and constructor injection of its repository interface. The invoke() function signature must match exactly what the ViewModel will call. Write the complete class but throw NotImplementedError("Will be implemented in Prompt 3+") in the body for now. Do this for every use case listed in CLAUDE.md:
24. auth/: SignInUseCase, SignUpUseCase, SignOutUseCase
25. devotional/: GetTodayDevotionalUseCase, MarkDevotionalCompleteUseCase, SyncDevotionalUseCase
26. prayer/: GetPrayersUseCase, PostPrayerUseCase, IntercedeForPrayerUseCase, MarkPrayerAnsweredUseCase
27. feed/: GetFeedPostsUseCase, CreatePostUseCase, ReactToPostUseCase
28. leader/: GetMyLeaderUseCase, GetAllLeadersUseCase, SendMessageUseCase, SubmitCheckInUseCase

As Debugger — verify before finishing:
- No domain model file contains import android.* or import io.github.jan.tennert.* — if found, that is an architecture violation, remove it immediately
- All @SerialName values in DTOs exactly match Supabase column names (snake_case) from the schema in CLAUDE.md
- UserRole.valueOf() calls use a safe fallback — never throw IllegalArgumentException on unknown role string
- PrayerDto.toDomain() — double-check the anonymous safeguarding is implemented and commented

As QA Tester — write this checklist:
- [ ] All domain model files compile with zero Android imports
- [ ] PrayerDto.toDomain() with isAnonymous=true returns Prayer(userId=null, userName=null)
- [ ] PrayerDto.toDomain() with isAnonymous=false returns Prayer with real userId and userName
- [ ] Result.Success, Result.Error, Result.Loading can all be constructed without errors
- [ ] All mapper functions are callable from a unit test without any Android context
- [ ] RepositoryModule.kt has @Binds for every repository interface defined in this step

End your response with: files created (list with package path), architecture validation result, and what Prompt 3 will build.
```

---

### ▶ PROMPT 3 — Authentication Flow: Login, Sign Up, Profile Setup, Session Management

```
Act as a Senior Software Engineer specializing in Android Mobile Application Development, UI/UX Design, Debugging, and Quality Assurance Testing. Apply all four roles simultaneously.

You are building the GRACE youth ministry Android app. Read CLAUDE.md for full context. Prompts 1 and 2 are complete — project compiles, all domain models, DTOs, mappers, and repository interfaces exist.

YOUR TASK: Build the complete authentication system — backend logic, session management, and all three auth screens. Write every file completely.

As Mobile Developer — implement:

DATA LAYER:
1. `UserPreferences.kt` (data/datastore/): Define all DataStore preference keys as PreferencesDataStore keys: USER_ID, USER_NAME, USER_EMAIL, USER_ROLE, GROUP_ID, FCM_TOKEN, DEVO_STREAK, LAST_DEVO_DATE, DEVO_REMINDER_HOUR (default 7), NOTIF_PRAYER_ENABLED (default true), NOTIF_DEVO_ENABLED (default true), NOTIF_MESSAGES_ENABLED (default true)
2. `UserPreferencesRepo.kt`: Typed getters returning Flow<T> and suspend setter functions for every key. Also: suspend fun clearAll() that clears the entire DataStore (used on sign-out).
3. `AuthRepositoryImpl.kt`:
   - signIn: supabase.auth.signInWithPassword(email, password) → on success save userId, name, email, role, groupId to DataStore → return Result.Success(user.toDomain())
   - signUp: supabase.auth.signUp(email, password) → on success insert new row into Supabase users table with name and default role='member' → return Result.Success
   - signOut: supabase.auth.signOut() → UserPreferencesRepo.clearAll() → return Result.Success
   - currentUser(): Flow<User?> combining supabase.auth.sessionStatus with DataStore USER_ID — emit null if no session
   - deleteAccount: supabase.auth.admin.deleteUser(uid) (use supabase.functions.invoke("delete-account") as a safer alternative) → UserPreferencesRepo.clearAll() → return Result.Success
   - On any Supabase exception: catch RestException and map its message to a user-friendly string (e.g., "Invalid credentials" for 400, "Account not found" for 404)
   Store JWT refresh token in EncryptedSharedPreferences (security-crypto) using MasterKey.Builder with KEY_SCHEME_AES256_GCM. Never in plain DataStore.

DOMAIN (implement use cases from Prompt 2):
4. `SignInUseCase.kt`: validate email not blank + is valid email format (android.util.Patterns.EMAIL_ADDRESS) + password ≥ 8 chars → if invalid return Result.Error with specific field message → call authRepo.signIn()
5. `SignUpUseCase.kt`: validate name ≥ 2 chars, valid email, password ≥ 8 chars, confirmPassword matches → call authRepo.signUp()
6. `SignOutUseCase.kt`: call authRepo.signOut() → return result

NAVIGATION:
7. `Screen.kt`: sealed class with all routes — object Login, object SignUp, object ProfileSetup, object Home, object Devotional, object Prayer, object Feed, object Leaders, data class Chat(val leaderId: String) { fun route() = "chat/$leaderId" }
8. `NavGraph.kt`: startDestination determined by DataStore USER_ID — if empty → Auth graph (Login → SignUp → ProfileSetup → then jump to Main graph), if populated → Main graph (Home, Devotional, Prayer, Feed, Leaders, Chat). Auth graph and Main graph are nested NavGraphs.
9. `BottomNavBar.kt`: NavigationBar with 5 NavigationBarItems. Active tab icon and label in GraceGold. Gold dot below active tab icon. Inactive tabs at 50% alpha. Badge on Prayer tab (unread prayer count) and Leaders tab (unread message count) using BadgedBox.

PRESENTATION:
10. `LoginViewModel.kt`:
    UiState: email, password, isLoading, emailError: String?, passwordError: String?, generalError: String?
    Events: EmailChanged(email), PasswordChanged(password), PasswordVisibilityToggled, LoginClicked, NavigateToSignUp
    Effects: NavigateToHome, ShowError(message)
    In onEvent(LoginClicked): call SignInUseCase → on Success emit NavigateToHome → on Error set generalError

11. `LoginScreen.kt`:
    Full-screen dark background (GraceDeepBlue). Centered column layout.
    - Top: GRACE logo (large ✦ symbol in GraceGold, 64sp) + app name in Cormorant Garamond 36sp GraceGold + tagline "Connect. Pray. Grow." in Lato 14sp GraceCreamDim
    - Email OutlinedTextField: label "Email", keyboardType Email, imeAction Next, error text below if emailError != null, border turns GraceRose on error
    - Password OutlinedTextField: label "Password", visualTransformation based on isPasswordVisible, TrailingIcon = eye toggle IconButton, imeAction Done triggers LoginClicked, error text if passwordError != null
    - generalError shown as red text below password field with ErrorIcon
    - "Sign In" Button: full width, GraceGold background, dark text "Sign In", shows CircularProgressIndicator replacing text when isLoading = true, disabled when isLoading = true
    - "Don't have an account? Sign Up" TextButton centered below, GraceGold text
    - Collect NavigateToHome effect → navController.navigate(Screen.Home) with popUpTo(Screen.Login) { inclusive = true }

12. `SignUpViewModel.kt`:
    UiState: name, email, password, confirmPassword, isLoading, nameError, emailError, passwordError, confirmPasswordError, generalError
    Events: NameChanged, EmailChanged, PasswordChanged, ConfirmPasswordChanged, SignUpClicked, NavigateToLogin
    Effects: NavigateToProfileSetup, ShowError(message)

13. `SignUpScreen.kt`:
    Same visual style as LoginScreen. Fields: Full Name, Email, Password (with toggle), Confirm Password (with toggle). Real-time validation — show error as user types (on field focus lost). "Create Account" button. "Already have an account? Sign In" TextButton.

14. `ProfileSetupViewModel.kt`:
    UiState: selectedRole: UserRole?, availableGroups: List<Group>, selectedGroupId: String?, isLoading, error
    Events: RoleSelected(role), GroupSelected(groupId), CompleteSetup
    On init: fetch groups from supabase.from("groups").select().decodeList<GroupDto>()
    On CompleteSetup: update Supabase users table (role, group_id) → save role and groupId to DataStore → emit NavigateToHome

15. `ProfileSetupScreen.kt`:
    - Header: "Welcome to GRACE 🙏" in Cormorant Garamond 28sp
    - Subtitle: "Tell us a little about yourself so we can connect you to the right people."
    - Role selection: 4 tappable cards in 2×2 grid: Member (👤), Cell Leader (🛡️), Youth President (👑), Pastor (✝️). Selected card has GraceGold border + GraceGold background at 15% alpha.
    - Group dropdown: ExposedDropdownMenuBox showing all available groups fetched from Supabase.
    - "Let's Go →" Button: enabled only when both role and group are selected. GraceGold background.

As UI/UX Designer — verify every screen:
- All text fields have 16dp horizontal padding inside the field
- Error states use GraceRose color for border and error text
- Loading state disables ALL interactive elements (not just the button)
- Screen is scrollable if keyboard pushes content off screen (use verticalScroll on Column with imePadding())
- Transition from ProfileSetup to Home uses navController.navigate with full back stack clear

As Debugger — verify:
- JWT refresh token is in EncryptedSharedPreferences, NOT in DataStore
- On app resume: supabase.auth.currentSessionOrNull() → if token expired → call refreshCurrentSession() → if refresh fails → clear DataStore and navigate to Login
- signOut() clears both EncryptedSharedPreferences AND DataStore — verify both
- ProfileSetupScreen cannot be skipped (NavGraph enforces ProfileSetup before Home for new users)
- All Supabase RestExceptions are caught and mapped to user-friendly messages

As QA Tester — write this checklist:
- [ ] Valid email + correct password → navigates to Home screen
- [ ] Invalid email format → "Please enter a valid email address" error shown inline on email field
- [ ] Wrong password → "Invalid email or password" shown below form, not a crash
- [ ] Empty fields → appropriate field-level error messages shown
- [ ] Sign out → app returns to Login screen, DataStore is cleared, no session persists
- [ ] App killed + reopened with valid session → goes directly to Home without login
- [ ] App killed + reopened with expired session → goes to Login with clean state
- [ ] Profile setup: "Let's Go" button is disabled until both role and group are selected
- [ ] Keyboard overlapping fields: screen scrolls to keep focused field visible
- [ ] Slow network (simulate with Android Emulator throttling): loading spinner shows, button disabled, no double-submit

End your response with: files created, auth flow diagram (text-based), and what Prompt 4 will build.
```

---

### ▶ PROMPT 4 — Daily Devotional, Bible API, Offline Cache, Completion Ring

```
Act as a Senior Software Engineer specializing in Android Mobile Application Development, UI/UX Design, Debugging, and Quality Assurance Testing. Apply all four roles simultaneously.

You are building the GRACE youth ministry Android app. Read CLAUDE.md for full context. Prompts 1–3 are complete. Authentication works. The app navigates correctly between auth and main graphs.

YOUR TASK: Build the complete Daily Devotional feature — the most important screen in GRACE. This includes Bible API integration with Room offline cache, 4-step devotional UI, the animated completion ring, journal encryption, streak tracking, and background sync. Write every file completely.

As Mobile Developer — implement:

DATA LAYER:
1. `BibleApiService.kt` (data/remote/bible/): Retrofit interface.
   @GET("passage/text/") suspend fun getVerse(@Query("q") reference: String, @Query("include-headings") includeHeadings: Boolean = false, @Query("include-footnotes") includeFootnotes: Boolean = false, @Query("include-verse-numbers") includeVerseNumbers: Boolean = false): VerseResponse

2. `VerseDao.kt`: getByRef(ref: String): VerseEntity?, insert(verse: VerseEntity), update existing if same ref already exists (use @Insert(onConflict = REPLACE))

3. `DevotionalRepositoryImpl.kt` — implement completely:

   getTodayDevotional(): Flow<Result<Devotional>>
   - Emit Room record for today's LocalDate immediately if it exists (Result.Success)
   - Then if NetworkMonitor.isOnline(): fetch from Supabase: supabase.from("devotionals").select().eq("scheduled_date", LocalDate.now().toString()).limit(1).decodeSingle<DevotionalDto>()
   - Call fetchAndCacheVerse(dto.verseRef) to ensure verse is in Room
   - Upsert fetched devotional into Room → Room Flow re-emits → UI updates automatically
   - If Supabase fails: emit Result.Error("Could not load today's devotional. Showing cached version.") — the Room data remains visible

   fetchAndCacheVerse(ref: String): suspend fun String
   - verseDao.getByRef(ref) → if not null return entity.text immediately (offline cache hit)
   - If null and online: bibleApiService.getVerse(ref) → take response.passages.first() → clean HTML tags with Regex → insert VerseEntity(ref=ref, text=cleanText, cachedAt=System.currentTimeMillis())
   - If null and offline: return "This verse will appear once you connect to the internet."
   - Wrap entire function in try/catch

   markComplete(devoId: String, journalEntry: String): Result<Unit>
   - Encrypt journalEntry: use EncryptedSharedPreferences OR Jetpack Security AES-256-GCM cipher before Room insert. Add comment: "Journal entries are encrypted at the application layer before storage. The encryption key is tied to the Android Keystore and never leaves the device."
   - Insert UserDevoProgress into Room immediately (optimistic update)
   - Increment streak: read LAST_DEVO_DATE from DataStore. If it equals yesterday → streak + 1. If it equals today → no change (already counted). If older or null → streak resets to 1. Write new streak and today's date back to DataStore.
   - If NetworkMonitor.isOnline(): upsert to Supabase user_devo_progress with encrypted_journal_entry (store cipher text as base64)
   - If offline: insert OfflineSyncEntity(action="MARK_DEVO_COMPLETE", payload=json of devoId + encrypted entry) into Room sync queue
   - Return Result.Success

   getStreak(): Flow<Int> — reads UserPreferencesRepo.devoStreak as a Flow

4. Implement `GetTodayDevotionalUseCase.kt` and `MarkDevotionalCompleteUseCase.kt` with full logic (no NotImplementedError).

5. `DevoSyncWorker.kt` (worker/):
   - CoroutineWorker annotated @HiltWorker, injected via AssistedInject
   - doWork(): fetch devotionals for next 7 days from Supabase → upsert all into Room → for each devotional fetch and cache its verse via fetchAndCacheVerse → return Result.success()
   - On failure: return Result.retry() (WorkManager will retry with exponential backoff)
   - Enqueued in GraceApplication.onCreate() as PeriodicWorkRequest every 24 hours

6. `StreakWorker.kt` (worker/): OneTimeWorkRequest. Reads streak from DataStore → updates Supabase users.streak column. On success: return Result.success(). On failure: return Result.retry().

PRESENTATION:
7. `DevotionalViewModel.kt`:
   UiState: devotional: Devotional?, verseText: String, currentStep: Int (0–3), isLoading: Boolean, isDone: Boolean, streakCount: Int, progress: Float (currentStep/3f, clamped 0f–1f, 1f when isDone), journalText: String, isMarkingComplete: Boolean, error: String?, isOfflineCached: Boolean
   Events: NextStep, PreviousStep, JournalTextChanged(text), MarkComplete, DismissError, RetryLoad
   Effects: ShowCompletionCelebration(newStreak: Int), ShowError(message)
   In init {}: collect getTodayDevotionalUseCase() + getStreak() simultaneously using combine or separate coroutines

8. `DevotionalScreen.kt` — the most visually important screen. Build it beautifully.
   Overall layout: Scaffold with no bottom bar (full screen devotional experience). Dark background GraceDeepBlue.

   TOP SECTION (always visible, does not scroll):
   - Row: left side has Day number (LATO 10sp letterSpacing 3sp GraceGold) + devotional title (Cormorant Garamond 22sp GraceCream)
   - Right side has CompletionRing (84dp) tapping it calls NextStep event
   - Below title: Step pill row (4 pills: Scripture · Reflection · Prayer · Journal). Completed steps = GraceGold text + GraceGold border. Current step = GraceGold filled. Future steps = GraceMuted.
   - If isOfflineCached: show "📡 Offline" badge in GraceGreen below pills

   STEP 0 — Scripture:
   - Large card (GraceCardAlt background, GraceGold border): centered ✦ icon (32sp animated float) + verse text in Cormorant Garamond 22sp italic GraceCream lineHeight 1.7 + verse reference in Lato 12sp GraceGold
   - Info card below: "📡 This verse is cached — available offline" in GraceGreen
   - Full-width GraceGold button "Continue to Reflection →"

   STEP 1 — Reflection:
   - Section label "TODAY'S REFLECTION" Lato 10sp letterSpacing 3sp GraceGold
   - Reflection text in Cormorant Garamath 17sp GraceCreamDim lineHeight 1.85 inside a card
   - Full-width GraceGold button "Continue to Prayer →"

   STEP 2 — Prayer:
   - Section label "PRAYER STARTER" in GraceGold
   - Prayer text in large card with dark gradient background + 🙏 icon centered above text
   - Helper text below: italic "Close your eyes and pray in your own words. This is just a starting point." in GraceCreamDim 12sp
   - Full-width GraceGold button "Continue to Journal →"

   STEP 3 — Journal:
   - Section label "MY JOURNAL" in GraceGold + "🔒 Private" badge (GraceGreen, 10sp)
   - Journal prompt shown in Cormorant Garamath italic 16sp above the text field
   - OutlinedTextField: placeholder "Write your thoughts... this is private, just between you and God.", minHeight 180dp, Cormorant Garamath 16sp, border turns GraceGold when focused, background GraceCardBg
   - "Mark Devotional Complete ✓" Button: GraceGreen background, full width, disabled when journalText.isBlank() or isMarkingComplete = true. Shows CircularProgressIndicator when isMarkingComplete = true.

   COMPLETION STATE (isDone = true):
   - Animate transition with AnimatedContent
   - Show Lottie animation (use LottieAnimation composable from lottie-compose, asset name "completion_celebration.json" — note: developer must add this Lottie file to assets/)
   - "🎉 Devotional Complete!" in Cormorant Garamath 24sp GraceGreen
   - "Day {streakCount} streak — God sees your faithfulness." in Lato 13sp GraceCreamDim
   - "Share to Feed" TextButton + "Back to Home" TextButton

9. `CompletionRing.kt` (presentation/components/):
   @Composable fun CompletionRing(progress: Float, isDone: Boolean, onTap: () -> Unit, modifier: Modifier = Modifier, size: Dp = 84.dp)
   - animateFloatAsState targeting (if isDone) 1f else progress, tween 800ms FastOutSlowInEasing
   - Canvas drawArc: background arc in GraceMuted, progress arc in (if isDone) GraceGreen else GraceGold, StrokeCap.Round, strokeWidth 6.dp
   - When isDone: draw a faint glow circle behind ring using drawCircle with GraceGreen at 25% alpha
   - Center content (Box): if isDone show animated Check icon (scale animateFloatAsState from 0f to 1f with spring()), else show progress percentage Text in Lato Bold GraceGold 11sp
   - clickable { onTap() } only when !isDone, with ripple disabled (indication = null)

As UI/UX Designer — verify:
- Scripture step: verse text must be fully readable, never truncated. If verse is long, the step card must scroll (scrollable Column inside the card, maxHeight 240dp)
- Step pills are horizontally scrollable if needed (LazyRow fallback)
- Journal TextField expands as user types (minHeight 180dp but no maxHeight restriction)
- Completion Lottie animation plays automatically on state transition, plays once (iterations = 1)
- All buttons are at least 52dp tall with 16dp horizontal padding inside

As Debugger — verify:
- DevotionalViewModel.init cancels both coroutines in onCleared()
- MarkDevotionalCompleteUseCase does not double-count streak if called twice on same day
- fetchAndCacheVerse strips HTML tags from ESV API response (it returns HTML — use a Regex to clean it)
- StreakWorker is enqueued as OneTimeWorkRequest only after markComplete succeeds — not before
- DevoSyncWorker uses AssistedInject correctly and is registered in NetworkModule or via @HiltWorker

As QA Tester — write this checklist:
- [ ] With internet: today's devotional loads from Supabase and is shown within 2 seconds
- [ ] With airplane mode ON after first load: app shows cached devotional from Room with 📡 badge
- [ ] With airplane mode ON and no cache: app shows graceful empty state with "Connect to load today's devotional" message (not a crash)
- [ ] Tapping CompletionRing advances to next step
- [ ] "Continue" buttons advance step correctly: 0→1→2→3
- [ ] "Back" step pill taps navigate to that step (no crash on any step)
- [ ] Marking complete with empty journal text: "Mark Complete" button is disabled
- [ ] Marking complete with journal text: isDone becomes true, streak increments, completion animation plays
- [ ] Completion animation plays exactly once (not looping)
- [ ] Streak increments by 1 after completing devotional (verify in Home screen streak badge)
- [ ] Completing devotional twice in same day: streak does not double-count
- [ ] Journal text survives step navigation back-and-forth without being cleared
- [ ] DevoSyncWorker runs without crash in background (verify in Android Studio WorkManager inspector)

End your response with: files created, data flow diagram (Supabase → Room → ViewModel → Screen), and what Prompt 5 will build.
```

---

### ▶ PROMPT 5 — Prayer Wall: Anonymous Posts, Realtime Counter, Moderation

```
Act as a Senior Software Engineer specializing in Android Mobile Application Development, UI/UX Design, Debugging, and Quality Assurance Testing. Apply all four roles simultaneously.

You are building the GRACE youth ministry Android app. Read CLAUDE.md for full context. Prompts 1–4 complete. Auth works, devotional is complete with offline cache and completion ring.

YOUR TASK: Build the complete Prayer Wall feature — anonymous posting with Supabase RLS, live pray_count via Supabase Realtime, category filtering, offline post queue, and leader moderation layer. Write every file completely.

As Mobile Developer — implement:

DATA LAYER:
1. `PrayerRepositoryImpl.kt` — implement completely:

   getPrayers(category: PrayerCategory?): Flow<Result<List<Prayer>>>
   - Collect from prayerDao.getAllActive() (status != ARCHIVED) as immediate emission
   - If online: fetch from Supabase with select(), filter by status = 'active', optionally filter by category, order by created_at DESC
   - Map each PrayerDto: if isAnonymous == true → set userId = null, userName = null in domain Prayer (CRITICAL — add comment explaining this)
   - Upsert all fetched DTOs into Room
   - Emit Result.Error as a secondary emission if network fails (Room data stays visible)

   subscribeToPrayCount(prayerId: String): Flow<Int>
   - Open Supabase Realtime channel named "prayer_interceptions_$prayerId"
   - Listen for INSERT events on prayer_intercessions table filtered by prayer_id = prayerId
   - On each INSERT event: re-query prayer_intercessions count for this prayerId and emit
   - Channel must be unsubscribed when the calling coroutine scope is cancelled
   - Return as a callbackFlow with awaitClose { channel.unsubscribe() }

   postPrayer(content: String, isAnonymous: Boolean, category: PrayerCategory): Result<Unit>
   - If isAnonymous = true: insert to Supabase with is_anonymous = true. The real user_id IS included in the DB insert (needed for safeguarding) but RLS prevents other users from reading it back. Add comment explaining this.
   - Insert optimistic PrayerEntity to Room immediately with a temporary ID (UUID.randomUUID())
   - Call supabase.from("prayers").insert(PrayerDto(...)) — on success update Room entity with real Supabase-returned ID
   - If offline: add to OfflineSyncQueue with action="POST_PRAYER" and payload = JSON of content/isAnonymous/category. Return Result.Success so UI feels responsive.

   intercede(prayerId: String): Result<Unit>
   - Upsert to prayer_intercessions(prayer_id, user_id) — ON CONFLICT DO NOTHING (idempotent)
   - Increment Room PrayerEntity.prayCount optimistically + 1
   - If Supabase call fails: decrement prayCount back and return Result.Error

   markAnswered(prayerId: String): Result<Unit>
   - Update Supabase: prayers set status = 'answered' where id = prayerId
   - Update Room: prayerDao.updateStatus(prayerId, PrayerStatus.ANSWERED)

2. Implement all prayer use cases with full logic: GetPrayersUseCase, PostPrayerUseCase, IntercedeForPrayerUseCase, MarkPrayerAnsweredUseCase.

PRESENTATION:
3. `PrayerViewModel.kt`:
   UiState: prayers: List<Prayer>, isLoading, activeFilter: PrayerCategory?, showPostForm: Boolean, newPrayerText: String, newPrayerCategory: PrayerCategory (default PERSONAL), isNewPrayerAnonymous: Boolean (default true), isSubmittingPrayer: Boolean, prayCountUpdates: Map<String, Int> (prayerId → realtime count), error: String?
   Events: FilterChanged(category), TogglePostForm, PrayerTextChanged(text), CategorySelected(category), AnonymousToggled, SubmitPrayer, Intercede(prayerId), MarkAnswered(prayerId), DismissError
   Effects: PrayerSubmitted, ShowError(message)
   In init {}: collect getPrayers. For each visible prayer, launch a coroutine subscribing to subscribeToPrayCount and update prayCountUpdates map.
   All Realtime subscriptions cancelled in onCleared().

4. `PrayerWallScreen.kt`:
   Top bar:
   - "Prayer Wall 🙏" Cormorant Garamath 28sp GraceCream
   - Subtitle: "{count} people lifting up prayers right now" Lato 12sp GraceCreamDim
   - "+ Request" Button (GraceBlue, rounded 12dp) — toggles showPostForm

   Post form (AnimatedVisibility with slide+fade):
   - Card with GraceBlue border 30% alpha
   - "SHARE YOUR PRAYER REQUEST" label
   - OutlinedTextField multi-line: "What would you like the community to pray for?"
   - Category chip row (horizontally scrollable LazyRow): FAMILY · SCHOOL · FAITH · HEALTH · PERSONAL · NATIONS. Each chip: category color at 20% alpha background, category color border on selected, GraceCreamDim color unselected.
   - Anonymous toggle row: Checkbox (GraceBlue when checked) + "Post anonymously" label + ℹ️ icon with tooltip "Your name is hidden from other members. Church leaders can see your identity for your safety."
   - "Submit 🙏" Button: GraceBlue, disabled when newPrayerText.isBlank() or isSubmittingPrayer, shows CircularProgressIndicator when submitting

   Category filter pills (LazyRow, horizontally scrollable):
   - "All" pill + one per PrayerCategory. Selected = GraceBlue filled. Unselected = GraceCardBg.

   Prayer cards (LazyColumn):
   - Each item is a PrayerCard composable

5. `PrayerCard.kt` (presentation/components/):
   Parameters: prayer: Prayer, realtimePrayCount: Int, hasUserPrayed: Boolean, onPrayTap: () -> Unit, onMarkAnswered: () -> Unit, isLeader: Boolean
   Layout:
   - Card: GraceCardBg background. If answered: soft GraceGreen gradient background.
   - Header row: avatar circle (anonymous = 🕊️ emoji on GraceMuted background; named = first letter of name on category-color background) + name ("A Youth in Prayer" if anonymous, real name otherwise) + time ago (relative: "2h ago") + category badge (category color 20% alpha, category color text)
   - If answered: "ANSWERED ✨" badge in GraceGreen
   - Prayer text: Cormorant Garamath 16sp GraceCreamDim lineHeight 1.7. Long texts truncated at 4 lines with "Read more" TextButton expanding the card.
   - Bottom row: Pray button (🙏 + count). If hasUserPrayed: GraceBlue background 20% alpha, GraceBlue border, GraceBlue text, pulse animation (animateFloatAsState scale 1.0→1.1→1.0). If not prayed: GraceCardBg background. Count reflects realtimePrayCount (from ViewModel's prayCountUpdates map, falling back to prayer.prayCount).
   - If isLeader and prayer.isAnonymous: show a small 🔍 "View Identity" TextButton (only leaders can see who posted anonymously — this triggers a confirmation dialog before showing)
   - If prayer belongs to current user: show "Mark as Answered 🙏" option

As UI/UX Designer — verify:
- Category chip colors: FAMILY=#4A7CFF, SCHOOL=#9B5DE5, FAITH=#C9A84C, HEALTH=#3ECF8E, PERSONAL=#E05C7A, NATIONS=#F4A261
- Pray button has satisfying tactile feedback: use rememberRipple() + scale animation on tap
- Anonymous post: zero chance the posting user's name or ID appears in the UI — verify with a test case in the QA checklist
- Answered prayer cards visually distinct but still readable — don't overdo the green
- Post form slides in from the top with AnimatedVisibility(enter = slideInVertically + fadeIn, exit = slideOutVertically + fadeOut)

As Debugger — verify:
- Realtime subscription uses a unique channel name per prayer (prayer_intercessions_{prayerId}) — two different prayers cannot share a channel
- All Realtime channels are unsubscribed in ViewModel.onCleared() — use a MutableList<RealtimeChannel> to track them all
- postPrayer with isAnonymous=true: the Supabase DTO must include user_id (for DB storage), but the domain Prayer returned from getPrayers must have userId = null and userName = null — verify both sides
- intercede() is idempotent: calling it twice for the same prayer does not duplicate the database row (Supabase UPSERT with ON CONFLICT DO NOTHING)
- Offline queue: if postPrayer is called offline, the prayer appears in the local list immediately and syncs when back online

As QA Tester — write this checklist:
- [ ] Prayer Wall loads with all active prayers from Room on first render (no blank screen flash)
- [ ] Category filter "Family" shows only Family prayers (no other categories visible)
- [ ] Post form: submit disabled when text is empty
- [ ] Post anonymous prayer: it appears in the list as "A Youth in Prayer" — the poster's name is NOT visible to any non-leader
- [ ] Post named prayer: poster's name visible to all
- [ ] Tap 🙏 Pray: count increments immediately (optimistic), button turns GraceBlue with pulse animation
- [ ] Tap 🙏 Pray again on same prayer: count should stay the same (idempotent, no double-count)
- [ ] Realtime: open the app on two devices, tap Pray on device 1 — count updates on device 2 without refresh
- [ ] Mark prayer answered: card turns green, "ANSWERED ✨" badge appears
- [ ] No crash on very long prayer text (200+ characters)
- [ ] Offline: post a prayer → prayer appears immediately in local list → reconnect → prayer syncs to Supabase
- [ ] Leader user sees "View Identity" on anonymous posts. Regular member does NOT see this button.

End your response with: files created, Realtime data flow diagram (user taps → Supabase → Realtime channel → ViewModel → UI), and what Prompt 6 will build.
```

---

### ▶ PROMPT 6 — Life Feed + Leader Connect + Real-Time Chat

```
Act as a Senior Software Engineer specializing in Android Mobile Application Development, UI/UX Design, Debugging, and Quality Assurance Testing. Apply all four roles simultaneously.

You are building the GRACE youth ministry Android app. Read CLAUDE.md for full context. Prompts 1–5 complete. Auth, devotional, and prayer wall are all working.

YOUR TASK: Build the Life Feed (with Paging 3 and image upload) and Leader Connect (with real-time private chat and weekly check-in). Write every file completely.

As Mobile Developer — implement:

FEED DATA LAYER:
1. `PostPagingSource.kt` (data/remote/supabase/): PagingSource<Int, PostDto> that fetches from Supabase posts table with pagination (offset-based: page * pageSize). pageSize = 20.
2. `FeedRepositoryImpl.kt`:
   createPost(type, content, imageUri, verseRef): Result<Unit>
   - If imageUri != null: read bytes from uri using ContentResolver → upload to Supabase Storage bucket "posts" at path "{userId}/{UUID}.jpg" → get public URL
   - Insert PostDto into Supabase with all fields including imageUrl if present
   - Insert PostEntity into Room immediately (optimistic)
   react(postId, reactionType): Result<Unit>
   - Check if user already has this reaction (query reactions table for this postId + userId)
   - If same reaction: DELETE from reactions (toggle off) → decrement count
   - If different reaction: UPDATE existing row's reaction_type
   - If no existing reaction: INSERT new row
   - Update Room PostEntity reaction counts accordingly

LEADER DATA LAYER:
3. `LeaderRepositoryImpl.kt`:
   getMyLeader(): Flow<Result<User?>>
   - Read GROUP_ID from DataStore → fetch group from Supabase → fetch group.leader_id user → return as Flow<Result<User?>>
   getMessages(leaderId: String): Flow<Result<List<Message>>>
   - Emit from Room messageDao.getConversation(myUserId, leaderId) immediately
   - Subscribe to Supabase Realtime on messages table: filter "(sender_id = me AND receiver_id = leaderId) OR (sender_id = leaderId AND receiver_id = me)"
   - On new message: insert into Room → Room Flow auto-emits → UI updates
   - Mark received messages as read: on collecting this flow, call markMessagesRead(leaderId)
   sendMessage(receiverId, content): Result<Unit>
   - Insert MessageEntity into Room with isFailed = false, isRead = false, a temporary local ID
   - Insert to Supabase messages table → on success update Room message with real ID
   - On failure: update Room message isFailed = true (UI shows retry option)

FEED PRESENTATION:
4. `FeedViewModel.kt`:
   UiState: posts: LazyPagingItems<Post> (use Pager + PagingData.collectAsLazyPagingItems()), showCompose: Boolean, draftText: String, draftImageUri: Uri?, draftVerseRef: String?, isPosting: Boolean, error: String?
   Events: ToggleCompose, DraftTextChanged(text), ImagePicked(uri), VerseRefChanged(ref), SubmitPost, React(postId, reactionType), DismissError

5. `FeedScreen.kt`:
   Header: "Life Feed 🌿" + "+ Share" Button (GraceGreen)
   Compose form (AnimatedVisibility):
   - Row: user avatar initial circle (GraceGreen background) + multi-line OutlinedTextField "What is God teaching you today?"
   - Row below: 📷 Add Photo TextButton (launches photo picker intent) + 📖 Tag Verse TextButton (opens inline field for verse reference) + "Post 🌿" Button (GraceGreen, disabled when draftText.isBlank())
   - If draftImageUri != null: show small image preview with ✕ remove button
   LazyColumn with Paging 3:
   - items(posts) { post → PostCard(post, onReact = { viewModel.onEvent(React(post.id, it)) }) }
   - Paging load states: show CircularProgressIndicator at bottom when loading next page, show error with retry button on failure

6. `PostCard.kt` (presentation/components/):
   - User avatar initial circle + name + timestamp
   - If verseRef != null: verse chip (GraceGold background 10% alpha, GraceGold text, verse reference, Cormorant Garamath italic) — tapping expands to show full cached verse text
   - Post text: Lato 15sp GraceCreamDim, expandable if > 4 lines
   - If imageUrl != null: AsyncImage (Coil) with loading placeholder and error placeholder, maxHeight 240dp, content scale Crop
   - If isHighlighted: gold left border (4dp) + "✦ Spotlighted by Leader" label above card in GraceGold 10sp
   - Reaction row: 3 buttons (🙏 Praying · 🔥 This hit · ✝️ Amen) each showing count. User's active reaction has GraceGreen background 20% alpha + GraceGreen border. Tapping active reaction = toggle off. Tapping inactive = activate (deactivates previous).
   - Comment count TextButton (💬 {count}) — placeholder, tapping shows Snackbar "Comments coming soon"

LEADER PRESENTATION:
7. `LeaderViewModel.kt`:
   UiState: myLeader: User?, allLeaders: List<User>, checkInStep: Int, checkInAnswers: MutableList<String> (3 items), checkInDone: Boolean, isSubmittingCheckIn: Boolean, error: String?
   Events: CheckInAnswerChanged(step, text), NextCheckInStep, PreviousCheckInStep, SubmitCheckIn, NavigateToChat(leaderId), DismissError

8. `LeaderScreen.kt`:
   Header: "My Leaders 🤝" + subtitle "Real mentorship. Real people."
   Weekly Check-In card (visible if !checkInDone):
   - "📋 WEEKLY CHECK-IN" label in GracePurple
   - Progress bar: 3 segments, completed segments in GracePurple
   - Question text: Cormorant Garamath 18sp GraceCream
   - Questions: Q1 "How's your faith walk this week? (1 = struggling, 5 = thriving)" · Q2 "What's your biggest struggle right now?" · Q3 "What can your leader specifically pray for you this week?"
   - OutlinedTextField for current answer
   - Back / Next / Submit buttons
   - On submit success: show "✓ Submitted! Your leader will respond soon." card in GraceGreen
   Leader cards (LazyColumn):
   - LeaderCard composable per leader
   - "Message" taps → navController.navigate to ChatScreen(leader.id)
   - "Book Chat" taps → placeholder Snackbar "Booking feature coming soon"
   Realtime presence: subscribe to Supabase presence channel "leaders_presence" — leaders update their online status on app foreground/background.

9. `ChatScreen.kt`:
   LazyColumn of MessageBubble composables:
   - My messages: right-aligned, GraceGold background, GraceDeepBlue text, rounded corners (18,18,4,18)
   - Leader messages: left-aligned, GraceCardAlt background, GraceCream text, rounded corners (18,18,18,4)
   - Each bubble: message text + timestamp (HH:mm format) + read receipt (single check = sent, double check in GraceBlue = is_read = true)
   - Failed message bubble: GraceRose border + "⚠ Failed · Tap to retry" TextButton
   - LazyListState.animateScrollToItem(lastIndex) whenever new message arrives
   Input row (pinned to bottom via imePadding + navigationBarsPadding):
   - OutlinedTextField: "Message {leaderName}..." rounded 24dp, single line with maxLines=5 to expand
   - Send button: circular 44dp GraceGold background, arrow icon, disabled when messageText.isBlank() or isSending

As UI/UX Designer — verify:
- PostCard image uses ContentScale.Crop and clips to rounded corners (12dp)
- Reaction buttons animate with scale animateFloatAsState on tap (1.0 → 1.3 → 1.0)
- Check-in progress bar segments animate with animateFloatAsState when step advances
- Chat bubbles have 12dp vertical spacing between messages from same sender, 20dp spacing between sender switches
- ChatScreen input row has windowSoftInputMode = adjustResize equivalent via imePadding()

As Debugger — verify:
- Paging 3: PostPagingSource does not fetch same page twice (check key and nextKey logic)
- Image upload: if Supabase Storage upload fails, the post is NOT inserted into DB — fail atomically
- Realtime message subscription unsubscribed in LeaderViewModel.onCleared()
- sendMessage optimistic update: Room shows message immediately even before Supabase confirms
- markMessagesRead runs on the IO dispatcher, not on main thread
- ChatScreen auto-scrolls to bottom: use LaunchedEffect(messages.size) { listState.animateScrollToItem(messages.lastIndex) }

As QA Tester — write this checklist:
- [ ] Life Feed loads posts in pages — scrolling to bottom triggers next page load
- [ ] Empty feed state: shows "No posts yet. Be the first to share! 🌿" illustration card
- [ ] Post text post: appears in feed immediately (optimistic), persists after app restart
- [ ] Post with photo: image uploads and appears in card within 3 seconds on WiFi
- [ ] React 🙏 Praying: count increments, button turns GraceGreen — persist after re-opening app
- [ ] React same reaction twice: count decrements back (toggle off)
- [ ] React different reaction: previous reaction removed, new one applied
- [ ] Check-in: all 3 questions answered → Submit → "✓ Submitted" card appears, check-in card hidden
- [ ] Chat: send message → appears immediately as my bubble on right → leader sends reply → appears as leader bubble on left (test with two devices)
- [ ] Chat read receipt: message shows double check once leader opens the chat
- [ ] Failed message: shows ⚠ retry option → tapping retries → on success shows normally
- [ ] ChatScreen: keyboard open → input row stays above keyboard (no overlap)
- [ ] Long conversation: auto-scrolls to latest message when new message arrives

End your response with: files created, Realtime chat flow diagram, and what Prompt 7 will build.
```

---

### ▶ PROMPT 7 — Push Notifications, Offline Sync, Verse Widget, Home & Settings

```
Act as a Senior Software Engineer specializing in Android Mobile Application Development, UI/UX Design, Debugging, and Quality Assurance Testing. Apply all four roles simultaneously.

You are building the GRACE youth ministry Android app. Read CLAUDE.md for full context. Prompts 1–6 complete. All core features are working. Now finalize push notifications, background sync, the home screen, settings, and the verse widget.

YOUR TASK: Build FCM push notifications, the offline sync queue worker, the Home screen, Settings screen, the Verse of the Day widget, and apply the full design system. Write every file completely.

As Mobile Developer — implement:

PUSH NOTIFICATIONS:
1. `GraceFcmService.kt` (service/): @AndroidEntryPoint, extends FirebaseMessagingService.
   onNewToken(token): update DataStore FCM_TOKEN + update Supabase users.fcm_token if logged in.
   onMessageReceived(message): read message.data["channel"] → select correct NotificationChannel → build NotificationCompat with setAutoCancel(true) + setSmallIcon(R.drawable.ic_notification) + deep link PendingIntent using TaskStackBuilder.
   Deep link routes: "prayer" channel → opens PrayerWallScreen, "devotional" → DevotionalScreen, "message" → ChatScreen(leaderId), "community" → FeedScreen.
   Register in AndroidManifest with intent-filter MESSAGING_EVENT + exported=false.

OFFLINE SYNC:
2. `OfflineSyncWorker.kt` (worker/): @HiltWorker CoroutineWorker.
   doWork(): fetch all OfflineSyncEntity from Room with retryCount < 3.
   For each: deserialize payload JSON → switch on action: "POST_PRAYER" → call PrayerRepository.postPrayer(), "MARK_DEVO_COMPLETE" → call DevotionalRepository.markComplete().
   On Supabase success: delete entity from Room.
   On failure: increment retryCount in Room. If retryCount reaches 3: mark as permanently failed (add failed_at timestamp column) and do not retry again.
   Return Result.success() even if some items failed (don't retry the whole worker for partial failures).
   Enqueued from NetworkMonitor when isOnline transitions to true, with ExistingWorkPolicy.REPLACE.

HOME SCREEN:
3. `HomeViewModel.kt`:
   Combines: currentUser (DataStore), todayDevotional (DevotionalRepository), recentPrayers (top 3 active from PrayerRepository), spotlightPost (latest highlighted post from FeedRepository), networkState (NetworkMonitor), nextEvent (first upcoming event from Supabase events table).
   UiState: userName, greeting (computed from current hour: before 12 → "Good morning", 12–17 → "Good afternoon", after → "Good evening"), streak, todayDevotional, devoDone, recentPrayers, spotlightPost, isOnline, nextEvent

4. `HomeScreen.kt` — the first screen users see every day. Make it warm, personal, and motivating.
   Full layout (scrollable Column):
   HEADER (dark gradient card):
   - Greeting: "Good morning," Lato 12sp letterSpacing 2sp GraceGold → userName in Cormorant Garamath 30sp GraceCream below
   - Today's date: Lato 13sp GraceCreamDim
   - Streak badge top-right: 🔥 icon (animated float) + "{streak}" Lato 18sp bold GraceGold + "DAY STREAK" Lato 9sp GraceGoldDim. Background: GraceGold 20% alpha rounded card.

   DEVOTIONAL CARD (tappable → DevotionalScreen):
   - "TODAY'S DEVOTIONAL" label + done badge if devoDone
   - Devotional title Cormorant Garamath 20sp GraceCream
   - Verse preview italic truncated at 80 chars GraceCreamDim
   - Verse reference Lato 11sp GraceGold
   - CompletionRing (72dp) on the right — non-tappable here, just visual indicator
   - Bottom row: "Tap to read & complete" or "✓ Completed today" in GraceCreamDim

   QUICK ACTIONS (2×2 grid, each a tappable card navigating to its screen):
   - Prayer Wall 🙏 (GraceBlue) + "{activeCount} active prayers"
   - Life Feed 🌿 (GraceGreen) + "Share what God is doing"
   - My Leader 🤝 (GracePurple) + leader name + online dot
   - Next Event 📅 (GraceRose) + event title + date/time

   SPOTLIGHT (if spotlightPost != null):
   - "✦ COMMUNITY SPOTLIGHT" label GraceGold
   - PostCard in compact mode (no reactions, just content + name)

   OFFLINE VERSE BANNER (always present, always cached):
   - "📡 OFFLINE VERSE CACHE" Lato 9sp GraceGoldDim
   - Random verse from cached Room verses in Cormorant Garamath 16sp italic GraceCreamDim
   - Verse reference GraceGold

SETTINGS:
5. `SettingsScreen.kt`:
   Profile section: AsyncImage avatar (Coil, from Supabase Storage) → tapping launches PhotoPicker → on pick: upload to Supabase Storage avatars/{userId}/avatar.jpg → update users.avatar_url → reload.
   Name display + email (read-only) + role badge.
   Devotional section: "Reminder Time" ListItem with current time → tapping opens TimePickerDialog → on confirm save DEVO_REMINDER_HOUR to DataStore → cancel and reschedule WorkManager PeriodicWorkRequest for devotional reminder at new time.
   Notifications section: 4 SwitchListTiles for each channel (Prayer Wall, Devotional, Messages, Community) — toggled state saved to DataStore keys. GraceFcmService checks these before building notification.
   Privacy section: "Your journal entries are encrypted on your device and never readable by anyone else." explanatory Text. "Delete My Account" Button → AlertDialog confirmation → on confirm call AuthRepository.deleteAccount() → navigate to Login.
   About section: App version (BuildConfig.VERSION_NAME) + "GRACE Youth Ministry" + "Made with ❤️ for the Church in the Philippines"

VERSE WIDGET:
6. `VerseOfDayWidget.kt` (widget/): GlanceAppWidget.
   In provideGlance(): read today's cached verse from Room (via a direct DAO call using runBlocking — acceptable in widget context) → display in Glance layout.
   Layout: GlanceTheme, Box with dark background color, Column with: verse text (fontStyle italic, GraceGold color, fontSize 14.sp), verse reference (GraceGold, 10.sp bold), GRACE label (12.sp GraceCreamDim bottom-right).
   ActionCallback: tapping widget launches MainActivity with Intent extra "destination" = "devotional" → MainActivity reads this extra in onCreate() and navigates to DevotionalScreen.
   Widget update: called from DevoSyncWorker.doWork() after syncing — GlanceAppWidgetManager.getInstance(context).updateAll(VerseOfDayWidget()).
   Register in AndroidManifest: AppWidgetProvider receiver + AppWidgetProviderInfo XML in res/xml/verse_widget_info.xml (minWidth=180dp, minHeight=110dp, updatePeriodMillis=86400000).

MANIFEST COMPLETION:
7. Update `AndroidManifest.xml` completely:
   - android:name=".GraceApplication"
   - GraceFcmService with MESSAGING_EVENT intent-filter
   - WorkManager initializer (if using custom initializer)
   - VerseOfDayWidget receiver
   - Permissions: INTERNET, ACCESS_NETWORK_STATE, POST_NOTIFICATIONS, RECEIVE_BOOT_COMPLETED, USE_BIOMETRIC, USE_FINGERPRINT
   - Deep link intent-filter on MainActivity: scheme="grace", host="app"
   - android:windowSoftInputMode="adjustResize" on MainActivity

PROGUARD:
8. Update `proguard-rules.pro`:
   - Keep all classes in io.github.jan-tennert.supabase package
   - Keep all @Serializable annotated classes
   - Keep Retrofit and OkHttp classes
   - Keep Firebase classes
   - Keep Room entities (add @Keep or keep rule)
   - -assumenosideeffects class android.util.Log { public static int d(...); public static int v(...); } (strip debug logs in release)

As UI/UX Designer — verify:
- Home screen feels warm and personal — greeting uses real name, streak badge is prominent
- Quick action cards are exactly 2×2, equal height, minimum 100dp per card
- Settings screen uses Section headers (Lato 10sp letterSpacing 3sp GraceCreamDim) separating groups
- Widget background is exactly GraceDeepBlue (#08090F) — matches app background
- Home screen is fully scrollable — no content cut off on any screen size (test 360dp and 414dp width)

As Debugger — verify:
- GraceFcmService: if notification received and app is in foreground → show in-app banner (NOT a system notification) using a SharedFlow in a singleton NotificationManager object
- OfflineSyncWorker: verify the worker does NOT run while the user is offline (enqueue only on connectivity restored)
- HomeViewModel: if any data source (devotional, prayers, etc.) throws, the home screen still renders — partial data is fine, full crash is not
- VerseWidget: runBlocking in widget is acceptable but must have a timeout (5 seconds max) to avoid ANR
- Settings avatar upload: if upload fails, the existing avatar must remain — do not clear it on failure

As QA Tester — write this checklist:
- [ ] Home screen shows correct greeting (Good morning before noon, Good afternoon 12–5pm, Good evening after 5pm)
- [ ] Streak badge shows current streak number correctly
- [ ] Devotional card: shows "✓ Completed today" if devotional was completed today
- [ ] Prayer Wall quick-action shows correct active prayer count
- [ ] Leader quick-action shows leader name and online dot
- [ ] Offline banner always shows a cached verse — even with airplane mode on from first launch
- [ ] Receiving FCM push notification while app is backgrounded: system notification appears with correct text
- [ ] Tapping push notification: opens correct screen (prayer notification → PrayerWall)
- [ ] Settings reminder time: change to 8:00 AM → close app → next day devotional reminder arrives at 8 AM (manual timing test)
- [ ] Settings notification toggle OFF for "Prayer Wall" → prayer notification not received
- [ ] Account deletion: all local data cleared, navigates to Login, cannot log back in
- [ ] Verse widget appears on home screen after adding it from widget picker
- [ ] Verse widget updates after DevoSyncWorker runs (force-run in WorkManager inspector)
- [ ] App builds in RELEASE variant without ProGuard errors
- [ ] APK size is under 20MB (check with Analyze APK in Android Studio)

End your response with: files created, full AndroidManifest.xml content, launch readiness assessment (what's done, what's missing), and next steps for Play Store submission.
```

---

### ▶ PROMPT 8 — Bug Fix (use whenever a bug is found)

```
Act as a Senior Software Engineer specializing in Android Mobile Application Development, UI/UX Design, Debugging, and Quality Assurance Testing. Apply all four roles simultaneously, with emphasis on your Debugger and QA Tester roles for this task.

You are debugging an issue in the GRACE youth ministry Android app. Read CLAUDE.md for full project context, architecture, and conventions before diagnosing anything.

BUG REPORT:

Feature / Screen affected: [e.g. Prayer Wall → PrayerWallScreen.kt]

Symptom: [e.g. The 🙏 pray_count number displayed on each PrayerCard does not update in real time when another user taps the Pray button on a different device. The count only updates after the user pulls to refresh or restarts the app.]

Expected behavior: [e.g. The count should increment live within 1–2 seconds via Supabase Realtime subscription, with no manual refresh required, matching the behavior demonstrated in the prototype.]

Error / Crash message (paste in full if available):
[paste full stack trace here, or write "No crash — silent failure"]

Relevant files:
[e.g. PrayerWallScreen.kt, PrayerViewModel.kt, PrayerRepositoryImpl.kt, PrayerCard.kt]

As Debugger — do the following in order:
1. Read each relevant file top to bottom before suggesting any fix.
2. Identify the exact line(s) causing the issue. Explain in plain English why this causes the observed symptom.
3. Check all related systems: Is the Realtime channel being subscribed to correctly? Is it unsubscribed too early? Is the ViewModel collecting the Flow? Is the Composable recomposing when the state changes?
4. Propose ONE focused fix. Do not refactor unrelated code.
5. Show the exact diff — old code block → new code block — for every changed line.

As Mobile Developer — apply the fix:
- Write the corrected file(s) in full.
- Add a comment above the fix explaining WHY the bug occurred and what the fix does.
- Do not change architecture patterns. Do not introduce new dependencies.
- Do not modify any file not directly related to this bug.

As QA Tester — after the fix, write 5 specific test cases to verify the bug is resolved:
- [ ] [Specific test 1]
- [ ] [Specific test 2]
- [ ] [Specific test 3]
- [ ] [Specific test 4]
- [ ] [Specific test 5]

As UI/UX Designer — check: does the fix affect any visual behavior? If the count animation, button state, or loading indicator changed, verify the UI still feels responsive and correct.

End your response with: root cause (1 sentence), fix applied (which files, which lines), and regression risk (any side effects to watch for).
```

---

### ▶ PROMPT 9 — New Feature Addition (use for any feature from the Recommended Additions list)

```
Act as a Senior Software Engineer specializing in Android Mobile Application Development, UI/UX Design, Debugging, and Quality Assurance Testing. Apply all four roles simultaneously throughout this entire task.

You are adding a new feature to the GRACE youth ministry Android app. Read CLAUDE.md for full project context, existing architecture, database schema, design system, and coding conventions before writing anything.

NEW FEATURE TO BUILD: [e.g. Testimony Wall — a dedicated screen showing answered prayers as full-length testimony stories, separate from the Life Feed, with monthly featured testimonies highlighted by the Youth President]

FEATURE DESCRIPTION:
[Write a clear description of what the feature does, who uses it, and what problem it solves for the youth community]

SCOPE:
- New Supabase table needed: [yes/no — if yes, describe columns and RLS policy]
- New Room entity/DAO: [yes/no]
- New domain model: [yes/no]
- New repository method(s): [describe each method]
- New use case(s): [describe each]
- New screen(s): [describe what each screen shows and does]
- Navigation entry point: [where in the app does the user reach this feature]
- Design notes: [any specific visual direction — colors, layout, feel]

As Mobile Developer — follow this exact implementation sequence:
1. Write the Supabase SQL migration script (CREATE TABLE + RLS policies)
2. Write the Room entity + DAO with all required queries
3. Write the Supabase DTO + @Serializable annotations + mapper extension functions
4. Write the domain model + repository interface method(s)
5. Write the RepositoryImpl with offline-first logic (Room first, Supabase sync)
6. Write the use case(s) with full business logic
7. Write the UiState data class + Event sealed class + Effect sealed class
8. Write the ViewModel with all event handlers implemented
9. Write the Composable Screen, beautiful and consistent with the design system
10. Update NavGraph.kt with the new route
11. Add the navigation entry point (button, tab, bottom nav item, or menu)
12. Update RepositoryModule.kt if a new repository was added

As UI/UX Designer — for the new screen:
- Apply the GRACE design system: GraceDeepBlue background, GraceGold accents, Cormorant Garamath for headers/quotes, Lato for body text
- Include at least one meaningful animation (AnimatedVisibility, animateFloatAsState, or AnimatedContent)
- Ensure empty state has an illustrated message (emoji + text), not a blank screen
- All interactive elements have 48dp minimum tap targets
- Screen must be tested at 360dp width (small phone) and 414dp width (large phone)

As Debugger — after implementing:
- Confirm the new repository method follows the offline-first pattern
- Confirm the new ViewModel unsubscribes any Realtime channels in onCleared()
- Confirm the new route is properly registered in NavGraph and accessible
- Confirm no new Android imports appear in the domain layer
- Confirm the SQL migration does not break the existing schema

As QA Tester — write a QA checklist of at least 10 test cases specific to this new feature:
- [ ] Happy path: [describe]
- [ ] Empty state: [describe]
- [ ] Error state: [describe]
- [ ] Offline behavior: [describe]
- [ ] [6 more specific tests]

Write every file completely. Zero TODOs. Zero NotImplementedError after Prompt 2. End your response with: files created (with package path), SQL to run in Supabase, and what the user should test first.
```

---

*Last updated: May 2026*
*App: GRACE Youth Ministry · Stack: Kotlin + Jetpack Compose + Supabase*
*Developer: Euno · Church: Philippines*

---

## 🚀 COMPLETE CLAUDE CODE PROMPTS — COPY & PASTE DIRECTLY

> Each prompt below is self-contained and complete. Copy the entire block and paste it into Claude Code in Android Studio exactly as written. Do not modify unless instructed.

---

### ▶ PROMPT 1 — Project Bootstrap & Hilt Setup
> Use this first. Run it before anything else.

```
You are building GRACE — a youth ministry Android mobile application for a church in the Philippines. The full project specification, architecture rules, design system, database schema, and coding conventions are defined in this file (CLAUDE.md). Read all of it carefully before writing a single line of code.

YOUR TASK — STEP 1: Project Bootstrap

Set up the complete Android project foundation. Do the following in order, writing every file in full:

1. Create the root-level `gradle/libs.versions.toml` file using the exact dependency versions defined in the TECH STACK section of this file. Include every library listed — do not skip or substitute any.

2. Update `settings.gradle.kts` to enable the version catalog.

3. Update the root `build.gradle.kts` to apply all plugins from the version catalog.

4. Update `app/build.gradle.kts` with:
   - All plugins: android-application, kotlin-android, kotlin-compose, kotlin-serialization, hilt, ksp, google-services
   - All dependencies from libs.versions.toml
   - buildConfigField entries for SUPABASE_URL, SUPABASE_ANON_KEY, BIBLE_API_KEY (reading from local.properties)
   - compileSdk = 35, minSdk = 26, targetSdk = 35
   - Package name: com.grace.app

5. Create `local.properties` template (with placeholder values, clearly marked — never real keys):
   SUPABASE_URL=https://your-project.supabase.co
   SUPABASE_ANON_KEY=your-anon-key-here
   BIBLE_API_KEY=your-bible-api-key-here

6. Create `app/src/main/java/com/grace/app/GraceApplication.kt`:
   - Annotated with @HiltAndroidApp
   - Initializes nothing directly (Supabase client is provided via Hilt)

7. Create `app/src/main/java/com/grace/app/MainActivity.kt`:
   - Single Activity annotated with @AndroidEntryPoint
   - Sets up the NavHost inside a Scaffold with a BottomNavigationBar
   - Placeholder destinations for: Home, Devotional, Prayer, Feed, Leaders

8. Create all four Hilt modules in `app/src/main/java/com/grace/app/di/`:

   a. NetworkModule.kt:
      - Provides SupabaseClient singleton using createSupabaseClient() with Postgrest, Auth, Realtime, Storage, Functions plugins
      - Reads SUPABASE_URL and SUPABASE_ANON_KEY from BuildConfig
      - Provides BibleApiService (Retrofit) with OkHttpClient — base URL: https://api.esv.org/v3/
      - OkHttpClient adds Authorization header: "Token ${BuildConfig.BIBLE_API_KEY}" on every request
      - Adds HttpLoggingInterceptor (BODY level) in DEBUG builds only

   b. DatabaseModule.kt:
      - Provides GraceDatabase (Room) as a singleton
      - Provides all DAOs: DevotionalDao, PrayerDao, PostDao, MessageDao, UserDao

   c. RepositoryModule.kt:
      - @Binds all repository interfaces to their implementations (leave implementations as TODO stubs for now)

   d. DataStoreModule.kt:
      - Provides DataStore<Preferences> singleton using preferencesDataStore

9. Create `app/src/main/java/com/grace/app/data/local/GraceDatabase.kt`:
   - @Database with version = 1, exportSchema = true
   - Register all entities: DevotionalEntity, PrayerEntity, PostEntity, MessageEntity, UserEntity, VerseEntity
   - Include all DAOs

10. Create the Room entity files in `data/local/entity/`:
    Each entity must exactly mirror the Supabase table schema defined in this file.
    Create: DevotionalEntity.kt, PrayerEntity.kt, PostEntity.kt, MessageEntity.kt, UserEntity.kt, VerseEntity.kt (for offline Bible cache — fields: ref: String @PrimaryKey, text: String, cached_at: Long)

11. Create stub DAO interfaces in `data/local/dao/` for each entity. Each DAO must have at minimum:
    - getAll(): Flow<List<Entity>>
    - getById(id: String): Entity?
    - insert(entity: Entity)
    - insertAll(entities: List<Entity>)
    - delete(entity: Entity)

12. Create `data/util/NetworkMonitor.kt`:
    - Wraps Android ConnectivityManager
    - Exposes: val isOnline: Boolean (computed) and val networkState: Flow<Boolean> (reactive)
    - Injected as a singleton via Hilt

13. Create `.gitignore` at the project root that ignores: local.properties, google-services.json, *.jks, /build, /.gradle, /.idea

After writing every file, show me the complete dependency graph: which modules depend on which, and confirm the project should compile cleanly before I proceed to Step 2.

Architecture rules to follow throughout:
- Clean Architecture: Presentation → Domain → Data. No reverse dependencies.
- Every Supabase/Retrofit call is a suspend function.
- All secrets come from BuildConfig only.
- No hardcoded strings in Kotlin files.
```

---

### ▶ PROMPT 2 — Core Data Layer (Domain Models, DTOs, Mappers)

```
You are continuing to build GRACE — a youth ministry Android app. Read CLAUDE.md for the full project context, architecture rules, database schema, and conventions. Step 1 (project bootstrap) is complete.

YOUR TASK — STEP 2: Core Data Layer

Build the complete domain model, Supabase DTO, and mapper layer. Write every file in full — no stubs or TODOs.

1. Create all domain models in `domain/model/`. These are pure Kotlin data classes with zero Android or Supabase imports:
   - User.kt: id, email, name, avatarUrl, role (enum: MEMBER, CELL_LEADER, YOUTH_PRESIDENT, PASTOR, ADMIN), groupId, streak, lastDevoAt
   - Prayer.kt: id, userId, content, isAnonymous, category (enum), status (enum: ACTIVE, ANSWERED, ARCHIVED), prayCount, isFlagged, expiresAt, createdAt
   - Devotional.kt: id, scheduledDate, title, verseRef, verseText, reflection, prayerStarter, journalPrompt, planId
   - Post.kt: id, userId, type (enum: TEXT, PHOTO, SCRIPTURE, PROMPT), content, imageUrl, verseRef, isHighlighted, createdAt
   - Message.kt: id, senderId, receiverId, content, isRead, sentAt
   - Group.kt: id, name, leaderId, description

2. Create all Supabase DTOs in `data/remote/supabase/dto/`. Each DTO is annotated with @Serializable and has @SerialName annotations matching exact Supabase column names (snake_case):
   - PrayerDto.kt
   - DevotionalDto.kt
   - PostDto.kt
   - UserDto.kt
   - MessageDto.kt

3. Create all Room entity mapper extension functions. For each entity, create a companion file or an Extensions.kt in the same package:
   - DevotionalEntity.toDomain(): Devotional
   - DevotionalDto.toEntity(): DevotionalEntity
   - DevotionalDto.toDomain(): Devotional
   - Apply the same pattern for Prayer, Post, User, Message

4. Create all repository interfaces in `domain/repository/`:
   - AuthRepository.kt: signIn(email, password), signUp(email, password, name), signOut(), currentUser(): Flow<User?>
   - DevotionalRepository.kt: getTodayDevotional(): Flow<Result<Devotional>>, markComplete(devoId, journalEntry): Result<Unit>, getStreak(): Flow<Int>
   - PrayerRepository.kt: getPrayers(category): Flow<Result<List<Prayer>>>, postPrayer(content, isAnonymous, category): Result<Unit>, intercede(prayerId): Result<Unit>, markAnswered(prayerId): Result<Unit>
   - FeedRepository.kt: getPosts(): Flow<Result<List<Post>>>, createPost(type, content, imageUri, verseRef): Result<Unit>, react(postId, reactionType): Result<Unit>
   - LeaderRepository.kt: getMyLeader(): Flow<Result<User?>>, getMessages(leaderId): Flow<Result<List<Message>>>, sendMessage(receiverId, content): Result<Unit>, submitCheckIn(answers): Result<Unit>

5. Create a sealed Result<T> class in `domain/util/Result.kt`:
   sealed class Result<out T> {
     data class Success<T>(val data: T) : Result<T>()
     data class Error(val message: String, val cause: Throwable? = null) : Result<Nothing>()
     object Loading : Result<Nothing>()
   }

6. Create all use cases in `domain/usecase/`. Each use case is a class with a single operator fun invoke(). Wire to the repository interface via constructor injection. Do not implement business logic yet — throw NotImplementedError() for now, but the class structure, constructor, and invoke signature must be complete and correct for every use case listed in CLAUDE.md.

After writing every file, confirm: does the domain layer compile with zero Android imports? If any Android import appears in domain/, flag it — that is an architecture violation.
```

---

### ▶ PROMPT 3 — Authentication Flow

```
You are continuing to build GRACE — a youth ministry Android app. Read CLAUDE.md for full context. Steps 1 and 2 are complete (project bootstrap + data layer).

YOUR TASK — STEP 3: Complete Authentication Flow

Build the full auth system — backend repository, use cases, and UI screens. Write every file in full.

1. Implement `AuthRepositoryImpl.kt` in `data/repository/`:
   - signIn: calls supabase.auth.signInWithPassword(email, password) → maps to Result<User>
   - signUp: calls supabase.auth.signUp(email, password) → creates user record in Supabase users table → Result<Unit>
   - signOut: calls supabase.auth.signOut() → clears DataStore session
   - currentUser(): Flow<User?> — observes supabase.auth.sessionStatus and maps to domain User
   - All calls wrapped in try/catch → emit Result.Error on failure
   - Store session in DataStore via UserPreferencesRepo (userId, role, groupId keys)

2. Create `data/datastore/UserPreferences.kt`:
   - Define DataStore preference keys: USER_ID, USER_ROLE, USER_NAME, GROUP_ID, FCM_TOKEN, DEVO_STREAK, LAST_DEVO_DATE, DEVO_REMINDER_HOUR
   - Create `UserPreferencesRepo.kt` with typed getters and setters for each key

3. Implement all auth use cases:
   - SignInUseCase: validates email/password not blank → calls authRepo.signIn() → returns Result<User>
   - SignUpUseCase: validates fields → calls authRepo.signUp() → returns Result<Unit>
   - SignOutUseCase: calls authRepo.signOut() → clears all DataStore keys → returns Result<Unit>

4. Create the auth navigation graph in `presentation/navigation/`:
   - Screen.kt sealed class with: Auth (nested: Login, SignUp, ProfileSetup), Main (nested: Home, Devotional, Prayer, Feed, Leaders, Chat)
   - NavGraph.kt: starts on Login if no session in DataStore, starts on Home if session exists
   - Auth graph → Main graph transition happens after successful login or sign up + profile setup

5. Create `LoginScreen.kt` and `LoginViewModel.kt`:
   UiState: email, password, isLoading, emailError, passwordError, generalError
   Events: EmailChanged, PasswordChanged, LoginClicked, NavigateToSignUp
   Effects: NavigateToHome, ShowError(message)
   UI elements:
   - App logo + "GRACE" title at top (Cormorant Garamond font, GraceGold color)
   - Email TextField with validation (must be valid email format)
   - Password TextField with show/hide toggle
   - "Sign In" button (GraceGold background, disabled + shows CircularProgressIndicator when isLoading = true)
   - "Don't have an account? Sign Up" text button
   - Error snackbar on general error

6. Create `SignUpScreen.kt` and `SignUpViewModel.kt`:
   UiState: name, email, password, confirmPassword, isLoading, errors per field
   Validation: name ≥ 2 chars, valid email, password ≥ 8 chars, passwords match
   UI elements:
   - Same header as LoginScreen
   - Name, Email, Password, Confirm Password fields
   - "Create Account" button
   - "Already have an account? Sign In" text button

7. Create `ProfileSetupScreen.kt`:
   This screen appears once after sign up, before entering the main app.
   UiState: selectedRole, selectedGroupId, availableGroups, isLoading
   Events: RoleSelected, GroupSelected, CompleteSetup
   UI elements:
   - "Welcome to GRACE 🙏" heading
   - Role selection as tappable cards: Member · Cell Leader · Youth President · Pastor
   - Group selection dropdown (fetched from Supabase groups table)
   - "Let's Go" button that calls supabase.from("users").update(role, groupId)
   - Navigates to Main graph on success

8. Update MainActivity.kt to:
   - Observe AuthRepository.currentUser() 
   - Show auth graph if null, main graph if user exists
   - Request POST_NOTIFICATIONS permission on Android 13+ using accompanist-permissions

Security requirements:
- Store JWT refresh token in EncryptedSharedPreferences using security-crypto library, not plain DataStore
- On app launch, if session exists but is expired, silently refresh via supabase.auth.refreshCurrentSession()
- If refresh fails, clear session and send user to LoginScreen
```

---

### ▶ PROMPT 4 — Daily Devotional + Bible API + Completion Ring

```
You are continuing to build GRACE — a youth ministry Android app. Read CLAUDE.md for full context. Steps 1–3 are complete.

YOUR TASK — STEP 4: Daily Devotional, Bible API Offline Cache, Completion Ring

This is the most important feature of GRACE. Build it completely and correctly.

1. Implement `DevotionalRepositoryImpl.kt`:
   - getTodayDevotional(): Flow<Result<Devotional>>
     → First emit Room DevotionalEntity for today's date if it exists
     → Then if online, fetch from Supabase: supabase.from("devotionals").select().eq("scheduled_date", today).single().decodeAs<DevotionalDto>()
     → On success: upsert into Room → Room Flow emits updated data automatically
     → On network failure: stale Room data stays on screen, emit Result.Error for a soft warning banner
   - fetchAndCacheVerse(ref: String): suspend fun returning String
     → Check VerseDao.getByRef(ref) first — return cached text immediately if found
     → If not found and online: call BibleApiService.getVerse(ref) → parse response → insert into Room as VerseEntity → return text
     → If not found and offline: return "This verse will load when you're back online."
   - markComplete(devoId: String, journalEntry: String): Result<Unit>
     → Encrypt journalEntry using Jetpack Security AES-256-GCM before storing
     → Insert UserDevoProgress into Room immediately (optimistic update)
     → If online: upsert to Supabase user_devo_progress table
     → If offline: queue in OfflineSyncQueue (a simple Room table with pending_action: String and payload: String)
     → Increment streak in DataStore: UserPreferencesRepo.incrementStreak()
     → Trigger StreakWorker via WorkManager to sync streak to Supabase

2. Implement `GetTodayDevotionalUseCase.kt` and `MarkDevotionalCompleteUseCase.kt` with full logic.

3. Create `DevotionalViewModel.kt`:
   UiState: devotional: Devotional?, currentStep: Int (0–3), isLoading, error, isDone, streakCount, progress: Float
   Events: NextStep, PreviousStep, JournalTextChanged(text), MarkComplete, DismissError
   Effects: ShowCompletionCelebration, ShowError(msg)
   Logic:
   - progress = currentStep / 3f (0.0 to 1.0), 1.0 when isDone = true
   - On MarkComplete event: call MarkDevotionalCompleteUseCase → on success emit ShowCompletionCelebration effect

4. Create `DevotionalScreen.kt`:
   Full 4-step layout:
   - Step 0 — Scripture: Large Cormorant Garamond italic verse text centered on a dark card. Reference in GraceGold. Offline badge if verse was loaded from Room cache. "Continue" button advances to step 1.
   - Step 1 — Reflection: Leader-written reflection text in a readable card. "Continue" → step 2.
   - Step 2 — Prayer: Prayer starter text. Instruction: "Close your eyes and pray in your own words." "Continue" → step 3.
   - Step 3 — Journal: journalPrompt shown as italic heading. Multi-line OutlinedTextField for journal entry. "Mark Complete" button (GraceGreen when journal has text, disabled when empty).
   Header area (visible across all steps):
   - Devotional title + Day number
   - Step pill row (Scripture · Reflection · Prayer · Journal) — tappable, completed steps show gold color
   - CompletionRing composable in the top-right corner
   Completion state: show Lottie animation (confetti/stars) + "Devotional Complete! 🎉" card + streak count update

5. Create `CompletionRing.kt` composable (exact spec from CLAUDE.md):
   - Canvas-drawn SVG ring using drawArc
   - animateFloatAsState for smooth progress fill animation (800ms, FastOutSlowInEasing)
   - Ring color: GraceGold when in progress, GraceGreen when isDone = true
   - Center: shows percentage text when in progress, Check icon (animated pop) when done
   - Tapping the ring when not done advances to next step (calls onTap lambda)
   - Glow effect when isDone: drawCircle with semi-transparent GraceGreen behind the ring

6. Create `DevoSyncWorker.kt` (WorkManager):
   - PeriodicWorkRequest scheduled nightly at 2:00 AM
   - Fetches next 7 days of devotionals from Supabase and upserts to Room
   - Fetches all verse refs referenced in those devotionals and caches them in Room
   - Enqueued in GraceApplication.onCreate() with ExistingPeriodicWorkPolicy.KEEP

7. Create `StreakWorker.kt` (WorkManager):
   - OneTimeWorkRequest triggered after MarkDevotionalCompleteUseCase succeeds
   - Reads current streak from DataStore → updates Supabase users.streak column
   - Also checks: if today's date ≠ lastDevoDate + 1 day → reset streak to 1 (streak broken)
   - Writes updated streak back to DataStore

Do not leave TODOs. Every function must be implemented. Show every file you create before writing it.
```

---

### ▶ PROMPT 5 — Prayer Wall + Anonymous Posts + Supabase Realtime

```
You are continuing to build GRACE — a youth ministry Android app. Read CLAUDE.md for full context. Steps 1–4 are complete.

YOUR TASK — STEP 5: Prayer Wall with Anonymous Posting and Supabase Realtime

1. Implement `PrayerRepositoryImpl.kt`:
   - getPrayers(category: String?): Flow<Result<List<Prayer>>>
     → Emit from Room PrayerDao first
     → Fetch from Supabase: select all non-archived prayers, ordered by created_at DESC
     → IMPORTANT: when mapping anonymous prayers, set Prayer.userId = null and Prayer.userName = null in the domain model — never expose the real user_id to the UI for anonymous posts. The RLS policy at the database level already enforces this, but the client must also respect it.
     → Upsert fetched prayers into Room
   - subscribeToRealtimePrayCount(prayerId: String): Flow<Int>
     → Subscribe to Supabase Realtime channel on prayer_intercessions table
     → Filter: eq("prayer_id", prayerId)
     → On INSERT event: re-fetch count and emit via Flow
     → Unsubscribe in CoroutineScope cleanup (onCleared)
   - postPrayer(content: String, isAnonymous: Boolean, category: String): Result<Unit>
     → Insert into Supabase: supabase.from("prayers").insert(PrayerDto(...))
     → If isAnonymous = true, the user_id is still stored in Supabase (for pastoral safeguarding) but RLS prevents non-admin users from reading it back
     → Insert optimistically into Room immediately, update with Supabase-returned ID on success
   - intercede(prayerId: String): Result<Unit>
     → Upsert to prayer_intercessions (prayerId, auth.uid()) — upsert prevents duplicate
     → Increment pray_count in Room PrayerEntity immediately (optimistic)
     → Decrement and refresh from Supabase if the call fails
   - markAnswered(prayerId: String): Result<Unit>
     → Update prayers set status = 'answered' in Supabase
     → Update Room entity status to ANSWERED

2. Implement all prayer use cases with full logic: GetPrayersUseCase, PostPrayerUseCase, IntercedeForPrayerUseCase, MarkPrayerAnsweredUseCase.

3. Create `PrayerViewModel.kt`:
   UiState: prayers: List<Prayer>, isLoading, activeFilter: String?, isSubmittingPrayer, showPostForm, newPrayerText, newPrayerCategory, isNewPrayerAnonymous, error
   Events: FilterChanged, TogglePostForm, PrayerTextChanged, CategorySelected, AnonymousToggled, SubmitPrayer, Intercede(id), MarkAnswered(id), DismissError
   Effects: PrayerSubmitted, ShowError(message)
   Realtime: subscribe to pray_count updates for visible prayer cards in init {}

4. Create `PrayerWallScreen.kt`:
   Top bar: "Prayer Wall 🙏" title + "+ Request" button (opens inline form when tapped)
   Prayer post form (animated slide-in when visible):
   - Multi-line OutlinedTextField: "What would you like the community to pray for?"
   - Category chip row (horizontal scrollable): Family · School · Faith · Health · Personal · Nations — selected chip highlighted in GraceBlue
   - Anonymous toggle: Checkbox + label "Post anonymously" + info icon explaining what anonymous means
   - "Submit 🙏" button — disabled if text is empty or isSubmittingPrayer = true
   Category filter pills (horizontal scrollable): All + all categories — tapping filters the list
   Prayer cards (LazyColumn):
   - Each card is a PrayerCard composable (create this in presentation/components/)
   - Anonymous posts show dove emoji 🕊️ avatar + "A Youth in Prayer" name
   - Named posts show user initial avatar + name
   - Category badge in card top-right, colored per category
   - Answered prayers: green gradient card background + "ANSWERED ✨" badge
   - Pray button (🙏 + count): tapping calls Intercede event, button pulses (pulse animation) when user has interceded, color changes to GraceBlue
   - Count updates live via Supabase Realtime without requiring a pull-to-refresh

5. Create `PrayerCard.kt` composable in `presentation/components/`:
   Parameters: prayer: Prayer, hasUserPrayed: Boolean, onPrayTap: () -> Unit, onMarkAnswered: () -> Unit
   Implement the animated pray button with AnimatedContent for count transitions.

Security note: In PrayerRepositoryImpl, add a comment above every anonymous prayer mapping explaining: "We do not include user_id in the domain Prayer model for anonymous prayers. This mirrors the Supabase RLS policy. The userId is stored in the database for pastoral safeguarding but is never surfaced to the UI layer."
```

---

### ▶ PROMPT 6 — Life Feed, Leader Connect, Private Chat

```
You are continuing to build GRACE — a youth ministry Android app. Read CLAUDE.md for full context. Steps 1–5 are complete.

YOUR TASK — STEP 6: Life Feed with Reactions and Leader Connect with Real-Time Chat

PART A — LIFE FEED

1. Implement `FeedRepositoryImpl.kt`:
   - getPosts(): Flow<Result<List<Post>>> — offline-first pattern (same as PrayerRepo)
   - createPost(type, content, imageUri, verseRef): Result<Unit>
     → If imageUri is not null: upload to Supabase Storage bucket "posts/{userId}/{uuid}.jpg" first → get public URL → include in post DTO
     → Insert post into Supabase → upsert into Room
   - react(postId: String, reactionType: String): Result<Unit>
     → Upsert into reactions table (post_id, user_id, reaction_type) — upsert replaces previous reaction
     → If same reaction type tapped again: delete from reactions (toggle off)
     → Update local Room post reaction counts optimistically

2. Create `FeedViewModel.kt` and `FeedScreen.kt`:
   UiState: posts: LazyPagingItems<Post> (use Paging 3), isComposing, draftText, draftImageUri, draftVerseRef, isPosting, error
   Events: ToggleCompose, DraftTextChanged, ImagePicked, React(postId, type), SubmitPost, DismissError
   Screen layout:
   - Header: "Life Feed 🌿" + "+ Share" button
   - Compose form (slide in): avatar initial · text field · optional image preview · verse tag input · "Post 🌿" button
   - LazyColumn of PostCard composables with Paging 3 (posts load more on scroll)
   - Each PostCard: user avatar initial · name · time · optional verse chip (gold) · post text · optional image (Coil) · 3 reaction buttons (🙏 Praying · 🔥 This hit · ✝️ Amen) with counts · comment count button
   - Highlighted posts: gold border on card + "✦ Spotlighted" label

PART B — LEADER CONNECT

3. Implement `LeaderRepositoryImpl.kt`:
   - getMyLeader(): Flow<Result<User?>> — reads group_id from DataStore → fetches group → fetches group.leader_id user from Supabase
   - getMessages(leaderId: String): Flow<Result<List<Message>>>
     → Emit from Room MessageDao first
     → Subscribe to Supabase Realtime on messages table: filter sender_id = leaderId AND receiver_id = me, OR sender_id = me AND receiver_id = leaderId
     → On new message INSERT: insert into Room → Flow emits automatically
   - sendMessage(receiverId: String, content: String): Result<Unit>
     → Insert into Room immediately (optimistic, mark as "pending")
     → Insert into Supabase messages table
     → On success: update Room message to remove "pending" state
     → On failure: mark Room message as "failed" — show retry option in UI
   - submitCheckIn(answers: Map<String, String>): Result<Unit>
     → Insert into Supabase checkins table with answers as JSONB

4. Create `LeaderScreen.kt` and `LeaderViewModel.kt`:
   UiState: myLeader: User?, allLeaders: List<User>, checkInStep: Int (0–2), checkInAnswers: List<String>, checkInDone, isLoading, error
   Screen layout:
   - Header: "My Leaders 🤝"
   - Weekly Check-In card (shown until submitted this week):
     - Step progress bar (3 segments)
     - Current question text
     - Multi-line text field for answer
     - Back / Next buttons, "Submit Check-In ✓" on final step
     - On submit: call SubmitCheckInUseCase → show "Submitted ✓" confirmation card
   - Leader cards (LazyColumn):
     - Avatar initial circle (colored per role), name, role badge, bio text
     - Online dot (green = available, grey = offline) — driven by Supabase Realtime presence
     - "Message" and "Book Chat" buttons — Message navigates to ChatScreen(leaderId)

5. Create `ChatScreen.kt` and update `LeaderViewModel.kt` for chat state:
   UiState: messages: List<Message>, messageText, isSending, error
   Events: MessageTextChanged, SendMessage, RetryMessage(id)
   Screen layout:
   - Top bar: back arrow + leader name + availability dot
   - LazyColumn of message bubbles:
     - My messages: right-aligned, GraceGold background, dark text
     - Leader messages: left-aligned, GraceCardAlt background, cream text
     - Each bubble: message text + timestamp + read receipt (single tick = sent, double tick = read when is_read = true)
     - Failed messages: show "⚠ Failed · Retry" tap target
   - Bottom input row: text field + send button (arrow icon, GraceGold)
   - Auto-scroll to bottom on new message (LazyListState.animateScrollToItem)
   - Mark messages as read (update is_read = true in Supabase) when ChatScreen is active

Write every file completely. No TODOs or stubs.
```

---

### ▶ PROMPT 7 — Push Notifications, WorkManager Sync, Offline Queue

```
You are continuing to build GRACE — a youth ministry Android app. Read CLAUDE.md for full context. Steps 1–6 are complete.

YOUR TASK — STEP 7: FCM Push Notifications, Background Sync, and Offline Queue

1. Create `GraceFcmService.kt` in `app/src/main/java/com/grace/app/service/`:
   - Extends FirebaseMessagingService, annotated @AndroidEntryPoint
   - onNewToken(token): injects UserPreferencesRepo → saves token to DataStore → if user is logged in, updates Supabase users.fcm_token
   - onMessageReceived(message): reads message.data["channel"] to determine notification channel
   - Build and display NotificationCompat.Builder with:
     - SmallIcon: R.drawable.ic_grace_notification (a simple cross/dove vector — describe what to draw)
     - Content title and body from message.notification or message.data
     - Auto-cancel = true
     - PendingIntent that deep links into the app: prayer/{prayerId} for prayer notifications, "devotional" for devo reminders
   - Create notification channels in GraceApplication.onCreate():
     - CHANNEL_PRAYER: "Prayer Wall" — importance HIGH
     - CHANNEL_DEVOTIONAL: "Daily Devotional" — importance DEFAULT
     - CHANNEL_MESSAGES: "Leader Messages" — importance HIGH
     - CHANNEL_COMMUNITY: "Community" — importance LOW

2. Create `OfflineSyncQueue` in `data/local/`:
   - Room entity: OfflineSyncEntity(id: UUID, action: String, payload: String, createdAt: Long, retryCount: Int)
   - OfflineSyncDao with: getPending(): List<OfflineSyncEntity>, insert(), delete(), incrementRetry()
   - Register in GraceDatabase

3. Create `OfflineSyncWorker.kt`:
   - OneTimeWorkRequest triggered by NetworkMonitor when connectivity returns
   - Reads all pending OfflineSyncEntity records
   - For each record: deserialize payload → execute appropriate Supabase call → on success delete from queue → on failure increment retryCount → if retryCount > 3 mark as permanently failed and remove
   - Enqueue from NetworkMonitor: whenever isOnline transitions false → true, enqueue this worker

4. Update `NetworkMonitor.kt`:
   - Register a ConnectivityManager.NetworkCallback
   - When onAvailable(): emit true to networkState Flow + enqueue OfflineSyncWorker
   - When onLost(): emit false
   - Expose isOnline: Boolean as a synchronous computed property
   - Expose networkState: StateFlow<Boolean> for reactive UI (show offline banner in MainScreen)

5. Add offline banner to `MainActivity.kt`:
   - Collect networkState from NetworkMonitor (injected via Hilt)
   - When offline: show a persistent top banner "📡 You're offline — some features may be limited" in GraceOrange color
   - Animate banner in/out with AnimatedVisibility (slide from top)

6. Create `VerseOfDayWidget.kt` using Glance API:
   - GlanceAppWidget implementation
   - Layout: dark background · GraceGold verse text (Cormorant font approximated via Glance) · verse reference · GRACE logo
   - Data source: reads from Room VerseEntity for today's scheduled devotional date — fully offline
   - Tap action: opens DevotionalScreen via Intent
   - Update via: GlanceAppWidgetManager.update() called from DevoSyncWorker after nightly sync

7. Update `AndroidManifest.xml` with all required entries:
   - GraceFcmService with intent-filter for com.google.firebase.MESSAGING_EVENT
   - GraceApplication as android:name
   - POST_NOTIFICATIONS permission (Android 13+)
   - INTERNET, ACCESS_NETWORK_STATE, RECEIVE_BOOT_COMPLETED permissions
   - VerseOfDayWidget receiver with AppWidgetProviderInfo metadata
   - Deep link intent-filter on MainActivity: scheme="grace", host="app"

Write every file in full with all imports. Double-check that GraceFcmService is registered in the manifest or the app will never receive push notifications.
```

---

### ▶ PROMPT 8 — Home Screen, Settings, Final Polish

```
You are continuing to build GRACE — a youth ministry Android app. Read CLAUDE.md for full context. Steps 1–7 are complete. This is the final polish step.

YOUR TASK — STEP 8: Home Screen, Settings Screen, Navigation Polish, and Launch Readiness

1. Create `HomeScreen.kt` and `HomeViewModel.kt`:
   UiState: userName, greeting (Good morning/afternoon/evening based on hour), streak, todayDevotional, recentPrayers (top 3), spotlightPost, isOnline, nextEvent
   Screen layout:
   - Header: time-based greeting + user name · date · streak badge (🔥 N DAY STREAK, GraceGold)
   - Today's Devotional card: verse preview (italic, truncated at 80 chars) + title + CompletionRing (small, 72dp) + "Tap to read" or "✓ Done" label — tapping navigates to DevotionalScreen
   - Quick action 2×2 grid: Prayer Wall (prayCount badge), Life Feed (new posts badge), My Leader (online dot), Next Event (date/time)
   - Spotlight section (if any post is highlighted by a leader): "✦ Community Spotlight" heading + single PostCard
   - Offline verse banner at bottom: always shows a cached verse with 📡 badge

2. Create `SettingsScreen.kt`:
   Sections:
   - Profile: avatar (Coil, tappable to change from gallery), name, email (read-only), role badge
   - Devotional: reminder time picker (TimePickerDialog → saves to DataStore DEVO_REMINDER_HOUR) → schedules a daily AlarmManager or WorkManager notification at that time
   - Notifications: toggle switches for each channel (Prayer Wall · Devotional · Messages · Community) — saves to DataStore, respected by GraceFcmService
   - Privacy: "My data is private" explanatory text · "Export my data" button (placeholder) · "Delete my account" button → confirmation AlertDialog → calls AuthRepository.deleteAccount() → signs out
   - About: App version (from BuildConfig.VERSION_NAME) · "GRACE Youth Ministry" · "Made with ❤️ for the Church"

3. Create `NotificationsScreen.kt`:
   - Shows a list of recent notifications from DataStore or a local Room notifications table
   - Each notification: icon per type · title · body · timestamp · tappable to navigate to the relevant screen

4. Polish BottomNavBar.kt:
   - 5 tabs: ⌂ Home · 📖 Devo · 🙏 Prayer · 🌿 Feed · 🤝 Leaders
   - Active tab: icon in GraceGold, label in GraceGold, small dot indicator below
   - Inactive tabs: desaturated, 50% opacity
   - Badge on Prayer tab: unread prayer count
   - Badge on Leaders tab: unread message count
   - Animate tab transitions with CrossFade

5. Apply the full GraceTheme:
   - GraceTheme.kt: MaterialTheme with GraceDarkColorScheme, GraceTypography, GraceShapes
   - GraceDarkColorScheme: primary=GraceGold, background=GraceDeepBlue, surface=GraceCardBg, onPrimary=GraceDeepBlue, onBackground=GraceCream
   - GraceTypography: displayLarge/Medium → Cormorant Garamond · body/label → Lato
   - GraceShapes: small=8dp, medium=16dp, large=24dp rounded corners throughout

6. ProGuard setup in `proguard-rules.pro`:
   - Keep all Supabase SDK classes
   - Keep all Retrofit/OkHttp classes
   - Keep @Serializable annotated classes (Kotlinx Serialization)
   - Keep Firebase classes
   - Strip all Log.d, Log.v calls in release builds

7. Final checklist — verify and fix each item before declaring done:
   - [ ] App compiles cleanly in both debug and release build variants
   - [ ] No hardcoded strings remain in Kotlin files
   - [ ] No API keys in source code (all via BuildConfig)
   - [ ] All Supabase calls are wrapped in try/catch
   - [ ] All ViewModels cleaned up coroutines in onCleared()
   - [ ] Realtime subscriptions unsubscribed when ViewModel is cleared
   - [ ] Anonymous prayer user_id never appears in any UI layer
   - [ ] Journal entries are encrypted before Room storage
   - [ ] .gitignore covers local.properties and google-services.json
   - [ ] All notification channels created on app start
   - [ ] WorkManager tasks are registered with unique names (no duplicate workers)

Write every file completely. After finishing, give me a summary of every file created in this step and its responsibility in one sentence each.
```

---

### ▶ PROMPT 9 — Bug Fix Template (use anytime)

```
You are working on GRACE — a youth ministry Android app built with Kotlin Jetpack Compose + Supabase. Read CLAUDE.md for full project context and architecture rules.

BUG REPORT:
- Screen / Feature affected: [e.g. Prayer Wall]
- What happens: [e.g. The pray_count does not update in real time when another user taps the pray button]
- What should happen: [e.g. The count should increment live via Supabase Realtime without requiring a pull-to-refresh]
- Error message (if any): [paste full stack trace here]
- Relevant files: [e.g. PrayerViewModel.kt, PrayerRepositoryImpl.kt, PrayerWallScreen.kt]

RULES FOR THIS FIX:
- Do not change Clean Architecture layer separation
- Do not introduce new dependencies unless absolutely necessary — if you must, explain why
- Do not touch files unrelated to this bug
- Show the exact diff (old code → new code) for every change
- After the fix, explain in plain language what was wrong and why your fix resolves it
```

---

### ▶ PROMPT 10 — New Feature Addition Template (use anytime)

```
You are adding a new feature to GRACE — a youth ministry Android app. Read CLAUDE.md for full project context, architecture, and conventions.

NEW FEATURE: [Feature Name — e.g. Testimony Wall]

Description: [What this feature does — e.g. A dedicated screen showing answered prayers as full testimony stories, separate from the Life Feed]

Required changes:
- New Supabase table(s): [describe or say "none"]
- New Room entity/DAO: [yes/no — describe]
- New domain model: [yes/no — describe]
- New repository method(s): [describe]
- New use case(s): [describe]
- New screen(s): [describe what the UI shows]
- Navigation change: [where to add the route]

Follow this sequence exactly:
1. Write the SQL migration script for any new Supabase table + RLS policy
2. Write the Room entity + DAO
3. Write the Supabase DTO + mapper
4. Write the domain model + repository interface method
5. Write the repository implementation
6. Write the use case
7. Write the UiState + Event + Effect sealed classes
8. Write the ViewModel
9. Write the Composable Screen
10. Add the route to NavGraph.kt
11. Add navigation entry point (button, tab, or menu item)

Write every file completely. Zero TODOs.
```

---

*Last updated: May 2026*
*App: GRACE Youth Ministry · Stack: Kotlin + Jetpack Compose + Supabase*
*Developer: Euno · Church: Philippines*
