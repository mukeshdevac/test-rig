package com.loftitan.eoltestfixture.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loftitan.eoltestfixture.ui.theme.BambuAccentGreen
import com.loftitan.eoltestfixture.ui.theme.BambuCardBorder
import com.loftitan.eoltestfixture.ui.theme.BambuCardSurface
import com.loftitan.eoltestfixture.ui.theme.BambuSlateBackground
import com.loftitan.eoltestfixture.ui.theme.BambuTextPrimary
import com.loftitan.eoltestfixture.ui.theme.BambuTextSecondary
import com.loftitan.eoltestfixture.ui.theme.BambuWarningYellow

enum class AppDestination(val title: String, val icon: ImageVector) {
    RUN_TEST("Run Test", Icons.Default.PlayCircle),
    HARDWARE("Rig Link", Icons.Default.Usb),
    DIAGNOSTICS("Diagnostics", Icons.Default.Memory),
    HISTORY_QUEUE("Queue", Icons.Default.CloudSync),
    SETTINGS("Settings", Icons.Default.Settings)
}

/**
 * Sidebar Navigation Rail for Tablets / Landscape / Desktop screens.
 */
@Composable
fun NavigationRailView(
    currentDestination: AppDestination,
    onDestinationSelected: (AppDestination) -> Unit,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    pendingQueueCount: Int,
    modifier: Modifier = Modifier
) {
    val railWidth = if (isExpanded) 190.dp else 68.dp

    Column(
        modifier = modifier
            .width(railWidth)
            .fillMaxHeight()
            .background(BambuCardSurface)
            .border(width = 1.dp, color = BambuCardBorder)
            .padding(vertical = 12.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Brand Logo / Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (isExpanded) Arrangement.Start else Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(BambuAccentGreen, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "LT",
                        color = BambuSlateBackground,
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp
                    )
                }
                AnimatedVisibility(visible = isExpanded) {
                    Row {
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "TITAN S3",
                                color = BambuTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "EOL FIXTURE",
                                color = BambuAccentGreen,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Navigation Items
            AppDestination.values().forEach { destination ->
                val isSelected = currentDestination == destination

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(
                            color = if (isSelected) BambuSlateBackground else BambuCardSurface,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .border(
                            width = if (isSelected) 1.5.dp else 0.dp,
                            color = if (isSelected) BambuAccentGreen else androidx.compose.ui.graphics.Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { onDestinationSelected(destination) }
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = if (isExpanded) Arrangement.Start else Arrangement.Center
                ) {
                    if (destination == AppDestination.HISTORY_QUEUE && pendingQueueCount > 0) {
                        BadgedBox(badge = {
                            Badge(
                                containerColor = BambuWarningYellow,
                                contentColor = BambuSlateBackground
                            ) {
                                Text(
                                    text = pendingQueueCount.toString(),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }) {
                            Icon(
                                imageVector = destination.icon,
                                contentDescription = destination.title,
                                tint = if (isSelected) BambuAccentGreen else BambuTextSecondary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    } else {
                        Icon(
                            imageVector = destination.icon,
                            contentDescription = destination.title,
                            tint = if (isSelected) BambuAccentGreen else BambuTextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    AnimatedVisibility(visible = isExpanded) {
                        Row {
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = destination.title,
                                color = if (isSelected) BambuAccentGreen else BambuTextPrimary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
            }
        }

        // Bottom Expand/Collapse Toggle Button
        IconButton(
            onClick = onToggleExpand,
            modifier = Modifier.size(44.dp)
        ) {
            Icon(
                imageVector = if (isExpanded) Icons.Default.ChevronLeft else Icons.Default.ChevronRight,
                contentDescription = "Toggle Sidebar",
                tint = BambuTextSecondary
            )
        }
    }
}

/**
 * Bottom Navigation Bar for Compact Phones / Portrait screens.
 */
@Composable
fun BottomNavigationBarView(
    currentDestination: AppDestination,
    onDestinationSelected: (AppDestination) -> Unit,
    pendingQueueCount: Int,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .border(1.dp, BambuCardBorder),
        containerColor = BambuCardSurface,
        tonalElevation = 0.dp
    ) {
        AppDestination.values().forEach { destination ->
            val isSelected = currentDestination == destination

            NavigationBarItem(
                selected = isSelected,
                onClick = { onDestinationSelected(destination) },
                icon = {
                    if (destination == AppDestination.HISTORY_QUEUE && pendingQueueCount > 0) {
                        BadgedBox(badge = {
                            Badge(
                                containerColor = BambuWarningYellow,
                                contentColor = BambuSlateBackground
                            ) {
                                Text(pendingQueueCount.toString(), fontWeight = FontWeight.Bold)
                            }
                        }) {
                            Icon(destination.icon, contentDescription = destination.title)
                        }
                    } else {
                        Icon(destination.icon, contentDescription = destination.title)
                    }
                },
                label = {
                    Text(
                        text = destination.title,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = BambuSlateBackground,
                    selectedTextColor = BambuAccentGreen,
                    indicatorColor = BambuAccentGreen,
                    unselectedIconColor = BambuTextSecondary,
                    unselectedTextColor = BambuTextSecondary
                )
            )
        }
    }
}
