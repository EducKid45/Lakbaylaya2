package com.example.lakbaylaya.ui.screens.map.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.lakbaylaya.ui.screens.map.models.ManeuverType

/**
 * Utility object for mapping maneuver types to appropriate Material icons
 * Provides consistent visual representation of turn-by-turn directions
 *
 * Note: Uses basic Material icons. For production, consider adding material-icons-extended
 * dependency for more specific navigation icons.
 */
object DirectionIconMapper {

    /**
     * Get icon for a maneuver type
     * Falls back to basic arrows when specific icons aren't available
     */
    fun getIconForManeuver(maneuver: ManeuverType): ImageVector {
        return when (maneuver) {
            ManeuverType.START -> Icons.Default.MyLocation
            ManeuverType.TURN_LEFT -> Icons.AutoMirrored.Filled.KeyboardArrowLeft
            ManeuverType.TURN_RIGHT -> Icons.AutoMirrored.Filled.KeyboardArrowRight
            ManeuverType.TURN_SLIGHT_LEFT -> Icons.AutoMirrored.Filled.TrendingFlat
            ManeuverType.TURN_SLIGHT_RIGHT -> Icons.AutoMirrored.Filled.TrendingFlat
            ManeuverType.TURN_SHARP_LEFT -> Icons.AutoMirrored.Filled.TrendingDown
            ManeuverType.TURN_SHARP_RIGHT -> Icons.AutoMirrored.Filled.TrendingUp
            ManeuverType.CONTINUE -> Icons.Default.ArrowUpward
            ManeuverType.MERGE -> Icons.AutoMirrored.Filled.CallMerge
            ManeuverType.ROUNDABOUT -> Icons.Default.Sync
            ManeuverType.ARRIVE -> Icons.Default.LocationOn
            ManeuverType.UTURN_LEFT -> Icons.Default.UTurnLeft
            ManeuverType.UTURN_RIGHT -> Icons.Default.UTurnRight
            ManeuverType.KEEP_LEFT -> Icons.AutoMirrored.Filled.KeyboardArrowLeft
            ManeuverType.KEEP_RIGHT -> Icons.AutoMirrored.Filled.KeyboardArrowRight
        }
    }

    /**
     * Get content description for accessibility
     */
    fun getContentDescription(maneuver: ManeuverType): String {
        return when (maneuver) {
            ManeuverType.START -> "Start navigation"
            ManeuverType.TURN_LEFT -> "Turn left"
            ManeuverType.TURN_RIGHT -> "Turn right"
            ManeuverType.TURN_SLIGHT_LEFT -> "Turn slight left"
            ManeuverType.TURN_SLIGHT_RIGHT -> "Turn slight right"
            ManeuverType.TURN_SHARP_LEFT -> "Turn sharp left"
            ManeuverType.TURN_SHARP_RIGHT -> "Turn sharp right"
            ManeuverType.CONTINUE -> "Continue straight"
            ManeuverType.MERGE -> "Merge"
            ManeuverType.ROUNDABOUT -> "Enter roundabout"
            ManeuverType.ARRIVE -> "Arrive at destination"
            ManeuverType.UTURN_LEFT -> "Make U-turn left"
            ManeuverType.UTURN_RIGHT -> "Make U-turn right"
            ManeuverType.KEEP_LEFT -> "Keep left"
            ManeuverType.KEEP_RIGHT -> "Keep right"
        }
    }
}




