package com.grace.app.presentation.navigation

sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object SignUp : Screen("signup")
    data object ProfileSetup : Screen("profile_setup")

    data object Home : Screen("home")
    data object Devotional : Screen("devotional")
    data object Bible : Screen("bible")
    data object StudyNotes : Screen("study_notes")
    data class BibleNoteEditor(val noteId: String) : Screen("bible_note/{noteId}") {
        fun createRoute() = "bible_note/$noteId"
    }
    data object Prayer : Screen("prayer")
    data object Feed : Screen("feed")
    data object Leaders : Screen("leaders")
    data object Settings : Screen("settings")
    data object Privacy : Screen("privacy")
    data object Events : Screen("events")
    data object Admin : Screen("admin")
    data object AdminComplianceReport : Screen("admin_compliance_report")
    data class AdminUserDetail(val userId: String) :
        Screen("admin_user_detail/{userId}") {
        fun createRoute() = "admin_user_detail/$userId"
    }
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
    data object FindCell : Screen("find_cell")
    data object DiscipleshipLibrary : Screen("discipleship_library")
    data class DiscipleshipAuthor(val activityId: String) :
        Screen("discipleship_author/{activityId}") {
        fun createRoute() = "discipleship_author/$activityId"
        companion object { const val NEW = "new" }
    }
    data object EditProfile : Screen("edit_profile")
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

    data class EditQuestion(val questionId: String) : Screen("edit_question/{questionId}") {
        fun createRoute() = "edit_question/$questionId"
        companion object { const val NEW = "new" }
    }

    data class EditPassage(val passageId: String) : Screen("edit_passage/{passageId}") {
        fun createRoute() = "edit_passage/$passageId"
        companion object { const val NEW = "new" }
    }

    data class Trivia(val mode: String) : Screen("trivia/{mode}") {
        fun createRoute() = "trivia/$mode"
    }

    data class MemberDetail(val memberId: String) : Screen("member_detail/{memberId}") {
        fun createRoute() = "member_detail/$memberId"
    }

    data class AddProxyMember(val eventId: String? = null) :
        Screen("add_proxy_member?eventId={eventId}") {
        fun createRoute(): String =
            if (eventId.isNullOrBlank()) "add_proxy_member"
            else "add_proxy_member?eventId=$eventId"
    }

    data class PostPrayerOnBehalf(val memberId: String) :
        Screen("post_prayer_on_behalf/{memberId}") {
        fun createRoute() = "post_prayer_on_behalf/$memberId"
    }

    data class LogReflectionOnBehalf(val memberId: String) :
        Screen("log_reflection_on_behalf/{memberId}") {
        fun createRoute() = "log_reflection_on_behalf/$memberId"
    }

    data class MemberReport(val memberId: String) :
        Screen("member_report/{memberId}") {
        fun createRoute() = "member_report/$memberId"
    }

    data object ClaimRecord : Screen("claim_record")

    data class MemberReflections(val memberId: String) :
        Screen("member_reflections/{memberId}") {
        fun createRoute() = "member_reflections/$memberId"
    }

    data class EventQr(val eventId: String) : Screen("event_qr/{eventId}") {
        fun createRoute() = "event_qr/$eventId"
    }

    data class EventCheckIn(val eventId: String) : Screen("event_checkin/{eventId}") {
        fun createRoute() = "event_checkin/$eventId"
    }

    data class EventRoster(val eventId: String) : Screen("event_roster/{eventId}") {
        fun createRoute() = "event_roster/$eventId"
    }

    data class EventForm(val eventId: String) : Screen("event_form/{eventId}") {
        fun createRoute() = "event_form/$eventId"
        companion object { const val NEW = "new" }
    }
    companion object {
        const val AUTH_GRAPH = "auth_graph"
        const val MAIN_GRAPH = "main_graph"
    }
}
