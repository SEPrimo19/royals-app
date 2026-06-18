package com.grace.app.domain.usecase.profile

import com.grace.app.domain.repository.AuthRepository
import com.grace.app.domain.util.Result
import javax.inject.Inject

class UpdateMyProfileUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(
        name: String,
        bio: String?,
        messengerUrl: String?,
        messengerPublic: Boolean,
        isCompassion: Boolean = false,
        compassionDigits: String = "",
        emergencyContact: String? = null,
        birthdate: java.time.LocalDate? = null,
        sex: String? = null
    ): Result<Unit> {
        val trimmedName = name.trim()
        if (trimmedName.length < 2) {
            return Result.Error("Name must be at least 2 characters.")
        }

        val cleanedBio = bio?.trim()?.takeIf { it.isNotBlank() }
        if (cleanedBio != null && cleanedBio.length > 280) {
            return Result.Error("Bio must be 280 characters or fewer.")
        }

        val cleanedMessenger = messengerUrl?.trim()?.takeIf { it.isNotBlank() }
        if (cleanedMessenger != null && !isMessengerUrl(cleanedMessenger)) {
            return Result.Error(
                "Messenger link must be on m.me, messenger.com, facebook.com, " +
                    "or fb.me."
            )
        }

        val cleanedDigits = compassionDigits.filter { it.isDigit() }
        if (isCompassion && cleanedDigits.length != 4) {
            return Result.Error(
                "Compassion number must be 4 digits (e.g. PH867-0142)."
            )
        }
        val composedCompassionNumber = if (isCompassion)
            "PH867-$cleanedDigits" else null

        if (isCompassion) {
            if (birthdate == null) {
                return Result.Error("Birthdate is required for Compassion participants.")
            }
            if (sex != "M" && sex != "F") {
                return Result.Error("Please pick a sex (M or F) for Compassion participants.")
            }
        }
        if (birthdate != null) {
            val today = java.time.LocalDate.now()
            if (birthdate.isAfter(today)) {
                return Result.Error("Birthdate can't be in the future.")
            }
            if (birthdate.isBefore(today.minusYears(100))) {
                return Result.Error("Please check the birthdate — that's over 100 years ago.")
            }
        }

        val cleanedEmergency = emergencyContact?.trim()?.takeIf { it.isNotEmpty() }

        return authRepository.updateMyProfile(
            name = trimmedName,
            bio = cleanedBio,
            messengerUrl = cleanedMessenger,
            messengerPublic = if (cleanedMessenger == null) false else messengerPublic,
            isCompassion = isCompassion,
            compassionNumber = composedCompassionNumber,
            emergencyContact = cleanedEmergency,
            birthdate = birthdate,
            sex = sex?.takeIf { it == "M" || it == "F" }
        )
    }

    private fun isMessengerUrl(raw: String): Boolean {
        val pattern = Regex(
            "^https?://(www\\.)?(m\\.me|messenger\\.com|facebook\\.com|fb\\.me)/.+",
            RegexOption.IGNORE_CASE
        )
        return pattern.matches(raw)
    }
}
