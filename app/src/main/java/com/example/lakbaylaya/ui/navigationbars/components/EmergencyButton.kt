package com.example.lakbaylaya.ui.navigationbars.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lakbaylaya.R
import com.example.lakbaylaya.ui.theme.DarkEmergency
import com.example.lakbaylaya.ui.theme.Emergency

/**
 * Reusable Emergency Button Component
 *
 * Can be used in screens or dialogs for quick emergency access.
 * Follows the same design principles as the TopBar emergency icon.
 *
 * @param onClick Callback when button is clicked
 * @param modifier Modifier for customization (should be the first optional parameter)
 * @param isDarkTheme Whether dark theme is active
 * @param enabled Whether button is enabled (prevents multiple triggers)
 */
@Composable
fun EmergencyButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = false,
    enabled: Boolean = true
) {
    // Pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "emergency_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "emergency_scale"
    )

    val emergencyColor = if (isDarkTheme) DarkEmergency else Emergency

    // Resolve composable resources outside of non-composable lambdas
    val contentDesc = stringResource(R.string.content_desc_emergency)

    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .scale(scale)
            .semantics {
                contentDescription = contentDesc
            },
        colors = ButtonDefaults.buttonColors(
            containerColor = emergencyColor,
            contentColor = MaterialTheme.colorScheme.onError,
            disabledContainerColor = emergencyColor.copy(alpha = 0.5f),
            disabledContentColor = MaterialTheme.colorScheme.onError.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = stringResource(R.string.action_emergency),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}
