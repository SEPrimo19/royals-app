package com.grace.app.widget

import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.Color
import androidx.glance.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontStyle
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.grace.app.MainActivity
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import java.time.LocalDate

private val Deep = Color(0xFF08090F)
private val Gold = Color(0xFFC9A84C)
private val CreamDim = Color(0xFFA09080)

class VerseOfDayWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: androidx.glance.GlanceId) {
        val db = EntryPointAccessors
            .fromApplication(context.applicationContext, WidgetEntryPoint::class.java)
            .graceDatabase()

        val today = LocalDate.now().toString()
        val todayDevo = db.devotionalDao().getByDate(today).first()
        val verseText = todayDevo?.verseText
            ?: db.verseDao().getAll().first().randomOrNull()?.text
            ?: "Connect to load today's verse."
        val verseRef = todayDevo?.verseRef
            ?: db.verseDao().getAll().first().randomOrNull()?.ref
            ?: ""

        provideContent {
            val ctx = LocalContext.current
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(Deep)
                    .padding(14.dp)
                    .clickable(
                        actionStartActivity(
                            Intent(ctx, MainActivity::class.java)
                                .putExtra("destination", "devotional")
                        )
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = verseText,
                    style = TextStyle(
                        color = ColorProvider(Gold),
                        fontSize = 14.sp,
                        fontStyle = FontStyle.Italic
                    )
                )
                Text(
                    text = verseRef,
                    style = TextStyle(
                        color = ColorProvider(Gold),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = "Royals",
                    style = TextStyle(color = ColorProvider(CreamDim), fontSize = 10.sp)
                )
            }
        }
    }
}

class VerseWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = VerseOfDayWidget()
}
