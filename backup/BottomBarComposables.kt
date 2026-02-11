package com.example.lakbaylaya.ui.navigation.bottom

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lakbaylaya.ui.navigation.model.NavigationBarItem
import com.example.lakbaylaya.ui.navigation.system.SystemBarDefaults
import com.example.lakbaylaya.ui.navigation.system.systemBottomBarPadding

@Composable
fun EvenlyDistributedBottomBar(
    modifier: Modifier = Modifier,
    items: List<NavigationBarItem>,
    selectedIndex: Int,
    onItemSelected: (index: Int) -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .systemBottomBarPadding(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(SystemBarDefaults.BottomBarMinHeight),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val stableItems = remember(items) { items.toList() }
            for ((index, item) in stableItems.withIndex()) {
                BottomBarItemContent(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = SystemBarDefaults.HorizontalPadding),
                    item = item,
                    selected = index == selectedIndex,
                    onClick = { onItemSelected(index) }
                )
            }
        }
    }
}

@Composable
private fun BottomBarItemContent(
    modifier: Modifier = Modifier,
    item: NavigationBarItem,
    selected: Boolean,
    onClick: () -> Unit
) {
    // Provide semantics for TalkBack: include selection state
    val semanticsModifier = modifier.semantics {
        // contentDescription will be supplied by callers via resource ids; leave empty here
    }

    Box(
        modifier = semanticsModifier
            .clickable(onClick = onClick)
            .padding(vertical = SystemBarDefaults.VerticalPadding)
            .then(Modifier),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(vertical = 2.dp)
                .height(SystemBarDefaults.BottomBarMinHeight)
        ) {
            val icon = if (selected && item.selectedIcon != null) item.selectedIcon!! else item.icon
            Icon(
                imageVector = icon,
                contentDescription = null, // Caller should provide semantics via res ids
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(SystemBarDefaults.IconSize)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = stringResource(item.labelRes),
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}
