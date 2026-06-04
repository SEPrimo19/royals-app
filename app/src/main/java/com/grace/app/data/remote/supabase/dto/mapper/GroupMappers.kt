package com.grace.app.data.remote.supabase.dto.mapper

import com.grace.app.data.remote.supabase.dto.GroupDto
import com.grace.app.domain.model.Group

fun GroupDto.toDomain(): Group = Group(
    id = id,
    name = name,
    leaderId = leaderId,
    description = description
)
