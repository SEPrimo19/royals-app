package com.grace.app.domain.model

data class LifeGroupDetail(
    val group: Group,
    val leader: User?,
    val members: List<User>
) {
    val memberCount: Int get() = members.size
}
