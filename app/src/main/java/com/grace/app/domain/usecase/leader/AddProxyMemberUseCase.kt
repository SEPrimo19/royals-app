package com.grace.app.domain.usecase.leader

import com.grace.app.domain.repository.LeaderRepository
import com.grace.app.domain.util.Result
import java.time.LocalDate
import javax.inject.Inject

/**
 * Registers a no-smartphone member to the calling leader's cell. Validates
 * the inputs the form can't easily reject (Compassion number format, age
 * sanity) and delegates the insert to LeaderRepository.
 *
 * Phase P.1 of Leader Proxy Mode — see [[leader-proxy-mode-spec]] memory.
 * Returns the new member's id so the caller can pop back to MemberDetail
 * for them immediately if desired.
 */
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
        // Name validation — minimal but defensive.
        val cleanName = name.trim()
        if (cleanName.length < 2) {
            return Result.Error("Please enter the member's full name.")
        }

        // Sex must be one of the two values our DB CHECK constraint allows.
        if (sex != "M" && sex != "F") {
            return Result.Error("Please pick a sex (M or F).")
        }

        // Birthdate sanity: not in the future, not more than 100 years ago.
        // Royals serves youth, so a 100yr cap is generous — guards against
        // a user holding the picker on 1925 by accident.
        val today = LocalDate.now()
        if (birthdate.isAfter(today)) {
            return Result.Error("Birthdate can't be in the future.")
        }
        if (birthdate.isBefore(today.minusYears(100))) {
            return Result.Error("Please check the birthdate — that's over 100 years ago.")
        }

        // If they marked the member as Compassion, the ID must match the
        // PH867-#### format the DB CHECK constraint also enforces. Catch
        // it here so the user sees a readable message instead of a 23514.
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
