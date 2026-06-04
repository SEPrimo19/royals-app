package com.grace.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.grace.app.presentation.theme.GraceCardAlt
import com.grace.app.presentation.theme.GraceCream
import com.grace.app.presentation.theme.GraceCreamDim
import com.grace.app.presentation.theme.GraceDeepBlue
import com.grace.app.presentation.theme.GraceGold

/**
 * Compassion sponsor block — shared by ProfileSetupScreen (initial signup)
 * and EditProfileScreen (post-signup edits). The PH867- prefix is locked
 * because every Royals Compassion member shares this prefix; the user only
 * enters the 4-digit suffix and we compose the full ID server-side.
 *
 * Inputs are pure state; the parent owns the truth and validates on submit.
 *
 * @param digits the 4-digit suffix only (NOT the composed PH867-XXXX)
 */
@Composable
fun CompassionSection(
    isCompassion: Boolean,
    digits: String,
    onToggle: (Boolean) -> Unit,
    onDigitsChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = GraceCardAlt),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "I'm a Compassion Participant",
                        color = GraceCream, fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Compassion-sponsored youth get monthly compliance reports.",
                        color = GraceCreamDim, fontSize = 11.sp
                    )
                }
                Switch(
                    checked = isCompassion,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = GraceDeepBlue,
                        checkedTrackColor = GraceGold,
                        uncheckedThumbColor = GraceCreamDim,
                        uncheckedTrackColor = GraceCardAlt
                    )
                )
            }
            if (isCompassion) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "Your Compassion Number",
                    color = GraceCreamDim, fontSize = 11.sp
                )
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(
                                GraceGold.copy(alpha = 0.15f),
                                RoundedCornerShape(10.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 12.dp)
                    ) {
                        Text(
                            "PH867-", color = GraceGold,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(
                        value = digits,
                        onValueChange = onDigitsChange,
                        modifier = Modifier.weight(1f),
                        placeholder = {
                            Text("0142", color = GraceCreamDim, fontSize = 14.sp)
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.NumberPassword
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = GraceCream,
                            unfocusedTextColor = GraceCream,
                            focusedContainerColor = GraceDeepBlue,
                            unfocusedContainerColor = GraceDeepBlue,
                            focusedBorderColor = GraceGold,
                            unfocusedBorderColor = GraceCardAlt,
                            cursorColor = GraceGold
                        )
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "Enter your 4-digit Compassion member number.",
                    color = GraceCreamDim, fontSize = 11.sp
                )
            }
        }
    }
}
