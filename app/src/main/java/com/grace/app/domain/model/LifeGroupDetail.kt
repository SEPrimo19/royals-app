package com.grace.app.domain.model

/**
 * Aggregate view of a Life Group: the group itself, the leader (if their
 * user record could be resolved), and the full member roster.
 *
 * `Group` is kept narrow (id/name/leaderId/description) because it's also
 * used by ProfileSetup's group-picker. The richer aggregate lives here so
 * the Life Group screen doesn't have to do three separate fetches in UI.
 */
data class LifeGroupDetail(
    val group: Group,
    val leader: User?,
    val members: List<User>
) {
    val memberCount: Int get() = members.size
}
