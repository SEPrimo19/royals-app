package com.grace.app.domain.usecase.leader

import com.grace.app.domain.repository.LeaderRepository
import com.grace.app.domain.util.Result
import java.time.LocalDate
import javax.inject.Inject

class AddProxyMemberUseCase @Inject constructor(
    private val leaderRepository: LeaderRepository
) {
    suspend operator fun invoke(
        name: String,
        birthdate: LocalDate,
        sex: String,
        isCompassion: Boolean,
        compassionNumber: String?,
        emergencyContact: String?,
        email: String?
    ): Result<String> {
        val cleanName = name.trim()
        if (cleanName.length < 2) {
            return Result.Error("Please enter the member's full name.")
        }

        if (sex != "M" && sex != "F") {
            return Result.Error("Please pick a sex (M or F).")
        }

        val today = LocalDate.now()
        if (birthdate.isAfter(today)) {
            return Result.Error("Birthdate can't be in the future.")
        }
        if (birthdate.isBefore(today.minusYears(100))) {
            return Result.Error("Please check the birthdate — that's over 100 years ago.")
        }

        if (isCompassion) {
            val clean = compassionNumber?.trim().orEmpty()
            if (!clean.matches(Regex("^PH867-\\d{4}$"))) {
                return Result.Error(
                    "Compassion number must be in the format PH867-#### " +
                        "(four digits after the hyphen)."
                )
            }
        }

        return leaderRepository.addProxyMember(
            name = cleanName,
            birthdate = birthdate,
            sex = sex,
            isCompassion = isCompassion,
            compassionNumber = if (isCompassion) compassionNumber?.trim() else null,
            emergencyContact = emergencyContact?.trim()?.takeIf { it.isNotEmpty() },
            email = email?.trim()?.takeIf { it.isNotEmpty() }
        )
    }
}
