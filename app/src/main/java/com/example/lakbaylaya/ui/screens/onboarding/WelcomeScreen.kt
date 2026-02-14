package com.example.lakbaylaya.ui.screens.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lakbaylaya.R
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.get
import androidx.core.graphics.set
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor

/*
 WelcomeOnboardingScreen composable (preview-safe):
 - Displays the app adaptive mipmap logo when available.
 - Larger logo, nudged upward, transparent near-white pixels.
 - Light-grey rounded container holds headline, subhead, and trimmed voice prompt.
 - Large primary and outlined buttons for "Set up now" and "Skip".
 - This file contains only preview-safe UI (no navigation/state logic).
*/

private val BackgroundWelcome = Color(0xFFDFF7E1) // light greenish background for overall screen
private val PrimaryBlue = Color(0xFF1565C0)
private val TextDark = Color(0xFF212121)

private const val WELCOME_PROMPT =
    "Welcome to LakbayLaya, your voice navigation assistant. Would you like to set up your emergency contact and home address now for safer navigation? You can skip this and start using navigation immediately."

@Composable
fun WelcomeOnboardingScreen(
    showLogoRes: Int = R.mipmap.lakbaylaya,
    headline: String = "Welcome to LakbayLaya",
    subhead: String = "Your voice navigation assistant.",
    voicePrompt: String = WELCOME_PROMPT,
    onSetUp: () -> Unit = {},
    onSkip: () -> Unit = {}
) {
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundWelcome)
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val context = LocalContext.current
            val logoPainter = remember(showLogoRes) {
                runCatching {
                    ContextCompat.getDrawable(context, showLogoRes)
                        ?.toBitmap()
                        ?.let { bmp ->
                            val mutable =
                                if (bmp.config == Bitmap.Config.ARGB_8888 && bmp.isMutable) bmp else bmp.copy(
                                    Bitmap.Config.ARGB_8888,
                                    true
                                )
                            val width = mutable.width
                            val height = mutable.height
                            for (y in 0 until height) {
                                for (x in 0 until width) {
                                    val px = mutable.get(x, y)
                                    val r = AndroidColor.red(px)
                                    val g = AndroidColor.green(px)
                                    val b = AndroidColor.blue(px)
                                    if (r >= 240 && g >= 240 && b >= 240) {
                                        val rgb = px and 0x00FFFFFF
                                        mutable.set(x, y, rgb)
                                    }
                                }
                            }
                            mutable.asImageBitmap()
                        }
                        ?.let { BitmapPainter(it) }
                }.getOrNull()
            }

            if (logoPainter != null) {
                Image(
                    painter = logoPainter,
                    contentDescription = "LakbayLaya app logo (adaptive mipmap)",
                    modifier = Modifier
                        .size(220.dp)
                        .offset(y = (-40).dp)
                        .semantics { contentDescription = "App logo" }
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Navigation,
                    contentDescription = "LakbayLaya logo fallback",
                    tint = PrimaryBlue,
                    modifier = Modifier
                        .size(180.dp)
                        .offset(y = (-40).dp)
                        .semantics { contentDescription = "App logo fallback" }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Light-grey rounded container for all text sections (headline, subhead, prompt)
            val displayedVoicePrompt = if (voicePrompt.contains('.')) {
                val after = voicePrompt.substringAfter('.').trim()
                if (after.isNotEmpty()) after else voicePrompt
            } else {
                voicePrompt.replace(Regex("(?i)Welcome to LakbayLaya,?\\s*"), "").trim()
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
            ) {
                Column(
                    modifier = Modifier
                        .padding(18.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(modifier = Modifier.widthIn(max = 640.dp)) {
                            Text(
                                text = headline,
                                color = TextDark,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = subhead,
                                color = TextDark,
                                fontSize = 18.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            HorizontalDivider(color = Color(0xFFE0E0E0), thickness = 1.dp)

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = displayedVoicePrompt.trim(),
                                color = TextDark,
                                fontSize = 16.sp,
                                textAlign = TextAlign.Start,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Column(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = onSetUp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .semantics { contentDescription = "Set up now" },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Icon(imageVector = Icons.Default.PersonAdd, contentDescription = "Set up icon")
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = "Set up now", fontSize = 18.sp, color = Color.White)
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = onSkip,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .semantics { contentDescription = "Skip setup" }
                ) {
                    Icon(imageVector = Icons.Default.SkipNext, contentDescription = "Skip icon")
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = "Skip", fontSize = 18.sp, color = TextDark)
                }
            }
        }
    }
}
