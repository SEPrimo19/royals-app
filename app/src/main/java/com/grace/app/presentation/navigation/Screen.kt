package com.grace.app.presentation.navigation

// Type-safe route definitions. Expanded with real destinations in Prompt 3+.
sealed class Screen(val route: String) {
    // Auth graph
    data object Login : Screen("login")
    data object SignUp : Screen("signup")
    data object ProfileSetup : Screen("profile_setup")

    // Main graph
    data object Home : Screen("home")
    data object Devotional : Screen("devotional")
    data object Prayer : Screen("prayer")
    data object Feed : Screen("feed")
    data object Leaders : Screen("leaders")
    data object Settings : Screen("settings")
    data object Events : Screen("events")
    data object Admin : Screen("admin")
    /** Admin → bulk compliance report generator. */
    data object AdminComplianceReport : Screen("admin_compliance_report")
    /** Admin → user detail. Path arg lets the VM look up the row by id. */
    data class AdminUserDetail(val userId: String) :
        Screen("admin_user_detail/{userId}") {
        fun createRoute() = "admin_user_detail/$userId"
    }
    /**
     * Bulk-email composer. Optional querystring params (subject, message)
     * let callers pre-fill the form — used by the Events screen to seed
     * an event-announcement email.
     */
    data class BulkEmail(
        val subject: String = "",
        val message: String = ""
    ) : Screen("bulk_email?subject={subject}&message={message}") {
        fun createRoute(): String {
            val s = java.net.URLEncoder.encode(subject, Charsets.UTF_8.name())
            val m = java.net.URLEncoder.encode(message, Charsets.UTF_8.name())
            return "bulk_email?subject=$s&message=$m"
        }
    }
    data object LifeGroup : Screen("life_group")
    data object EditProfile : Screen("edit_profile")
    /** Aggregated landing surface for personal/community content — listed
     *  as a card on Home so the user doesn't have to dig through the drawer
     *  to find their attendance / progress / journal / life group. */
    data object CommunityHub : Screen("community_hub")
    data object MyContent : Screen("my_content")
    data object MyAttendance : Screen("my_attendance")
    data object MyProgress : Screen("my_progress")
    data object MyMembers : Screen("my_members")
    data object MyJournal : Screen("my_journal")
    data object GamesHome : Screen("games_home")
    data object DailyVerse : Screen("daily_verse")
    data object WhoAmI : Screen("who_am_i")
    data object MemoryMatch : Screen("memory_match")
    data object VerseScramble : Screen("verse_scramble")
    data object TimelineSort : Screen("timeline_sort")
    data object Leaderboard : Screen("leaderboard")
    data object ManageContent : Screen("manage_content")

    /** Question editor. Pass "new" to create, or a question id to edit. */
    data class EditQuestion(val questionId: String) : Screen("edit_question/{questionId}") {
        fun createRoute() = "edit_question/$questionId"
        companion object { const val NEW = "new" }
    }

    /** Passage editor for FITB. Pass "new" or a passage id. */
    data class EditPassage(val passageId: String) : Screen("edit_passage/{passageId}") {
        fun createRoute() = "edit_passage/$passageId"
        companion object { const val NEW = "new" }
    }

    /** Trivia round. mode = "daily-easy"/"daily-medium"/"daily-hard"/"practice". */
    data class Trivia(val mode: String) : Screen("trivia/{mode}") {
        fun createRoute() = "trivia/$mode"
    }

    /** Leader's view of one mentee — chat + check-ins. */
    data class MemberDetail(val memberId: String) : Screen("member_detail/{memberId}") {
        fun createRoute() = "member_detail/$memberId"
    }

    /** Leader's form to register a no-smartphone cell member. Phase P.1 of
     *  Leader Proxy Mode. Phase P.2.6 adds an optional eventId query arg:
     *  when present, the screen shows a "Save & Mark Attended" button that
     *  registers the member AND marks them present at that event in one
     *  shot — for the "new member walked into the cell meeting" case. */
    data class AddProxyMember(val eventId: String? = null) :
        Screen("add_proxy_member?eventId={eventId}") {
        fun createRoute(): String =
            if (eventId.isNullOrBlank()) "add_proxy_member"
            else "add_proxy_member?eventId=$eventId"
    }

    /** Phase P.3 — leader posts a prayer to the wall on behalf of a member. */
    data class PostPrayerOnBehalf(val memberId: String) :
        Screen("post_prayer_on_behalf/{memberId}") {
        fun createRoute() = "post_prayer_on_behalf/$memberId"
    }

    /** Phase P.3 — leader logs a weekly meditation reflection on behalf of a member. */
    data class LogReflectionOnBehalf(val memberId: String) :
        Screen("log_reflection_on_behalf/{memberId}") {
        fun createRoute() = "log_reflection_on_behalf/$memberId"
    }

    /** Phase P.4 — leader-generated compliance report for a single member.
     *  PDF cover shows the MEMBER's identity; "Generated by {leader}" line
     *  appears in the cover extras for audit. */
    data class MemberReport(val memberId: String) :
        Screen("member_report/{memberId}") {
        fun createRoute() = "member_report/$memberId"
    }

    /** Phase P.5 — one-shot screen shown after a fresh signin if a proxy-
     *  only users row matches the user's email. Either redirects to Home
     *  immediately (no match) or shows the "is this you?" prompt. */
    data object ClaimRecord : Screen("claim_record")

    /** Leader's dedicated reflections-history view for one mentee. Split off
     *  from MemberDetail so the list can grow without crowding the summary. */
    data class MemberReflections(val memberId: String) :
        Screen("member_reflections/{memberId}") {
        fun createRoute() = "member_reflections/$memberId"
    }

    /** Creator-only attendance QR for a specific event. */
    data class EventQr(val eventId: String) : Screen("event_qr/{eventId}") {
        fun createRoute() = "event_qr/$eventId"
    }

    /** Confirmation page after a member scans a QR. */
    data class EventCheckIn(val eventId: String) : Screen("event_checkin/{eventId}") {
        fun createRoute() = "event_checkin/$eventId"
    }

    /** Leader's roster view of an event — Phase P.2 of Leader Proxy Mode.
     *  Shows their cell members + per-event attendance status; supports
     *  marking proxy attendance for members without smartphones. */
    data class EventRoster(val eventId: String) : Screen("event_roster/{eventId}") {
        fun createRoute() = "event_roster/$eventId"
    }

    /**
     * Event create/edit form. Pass the literal "new" for create, or a real
     * event id to edit. The form VM keys on this arg to decide which.
     */
    data class EventForm(val eventId: String) : Screen("event_form/{eventId}") {
        fun createRoute() = "event_form/$eventId"
        companion object { const val NEW = "new" }
    }
    companion object {
        const val AUTH_GRAPH = "auth_graph"
        const val MAIN_GRAPH = "main_graph"
    }
}
