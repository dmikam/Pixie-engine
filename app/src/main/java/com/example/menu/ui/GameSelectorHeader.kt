package com.example.menu.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Top marquee banner for the Pixie retro arcade proof-of-concept launcher.
 */
@Composable
fun ArcadeHeader(
    onOpenRoadmap: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF141924)),
        border = BorderStroke(
            1.5.dp,
            Brush.horizontalGradient(
                listOf(Color(0xFF00E5FF), Color(0xFFFF9100), Color(0xFFD500F9))
            )
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ArcadeHeaderTitle()
            ArcadeHeaderSubtitle()
            Spacer(modifier = Modifier.height(10.dp))
            ArcadeHeaderDescription()
            Spacer(modifier = Modifier.height(12.dp))
            ArcadeRoadmapButton(onOpenRoadmap = onOpenRoadmap)
        }
    }
}

@Composable
private fun ArcadeHeaderTitle() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(text = "◆", color = Color(0xFF00E5FF), fontSize = 14.sp)
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "PIXIE ENGINE",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 2.sp
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = "◆", color = Color(0xFFFF9100), fontSize = 14.sp)
    }
}

@Composable
private fun ArcadeHeaderSubtitle() {
    Text(
        text = "RETRO ARCADE PROOF OF CONCEPT HUB",
        color = Color(0xFFFFD54F),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        letterSpacing = 1.sp
    )
}

@Composable
private fun ArcadeHeaderDescription() {
    Text(
        text = "A multi-genre testbed validating fixed-timestep 2D physics, 5-layer parallax, and real-time ColorMatrix grading across 4 classic paradigms.",
        color = Color(0xFFB0BEC5),
        fontSize = 12.sp,
        lineHeight = 16.sp,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun ArcadeRoadmapButton(onOpenRoadmap: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        OutlinedButton(
            onClick = onOpenRoadmap,
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, Color(0xFF00E5FF)),
            modifier = Modifier.testTag("header_view_roadmap_button")
        ) {
            Icon(
                imageVector = Icons.Default.Description,
                contentDescription = null,
                tint = Color(0xFF00E5FF),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "VIEW DOCS / ROADMAP",
                color = Color(0xFF00E5FF),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

/**
 * Highlights horizontal bar displaying Pixie engine architectural capabilities.
 */
@Composable
fun ArchitectureFeaturePillsBar(modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF161A22),
        border = BorderStroke(1.dp, Color(0xFF2C3545)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FeaturePill(icon = "60", label = "FPS Loop")
            Text("•", color = Color(0xFF546E7A))
            FeaturePill(icon = "5L", label = "Parallax")
            Text("•", color = Color(0xFF546E7A))
            FeaturePill(icon = "RGB", label = "ColorMatrix")
            Text("•", color = Color(0xFF546E7A))
            FeaturePill(icon = "4", label = "PoC Genres")
        }
    }
}

@Composable
fun FeaturePill(
    icon: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Text(
            text = icon,
            color = Color(0xFF00E5FF),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text(
            text = label,
            color = Color(0xFF90A4AE),
            fontSize = 10.sp
        )
    }
}

/**
 * Section title and quick action row preceding the game catalog.
 */
@Composable
fun SelectorSectionHeader(
    onOpenRoadmap: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "SELECT PROOF OF CONCEPT",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "Gradual multi-genre engine testbed",
                color = Color(0xFF90A4AE),
                fontSize = 11.sp
            )
        }

        QuickRoadmapBadge(onOpenRoadmap = onOpenRoadmap)
    }
}

@Composable
private fun QuickRoadmapBadge(onOpenRoadmap: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = Color(0xFF1E2838),
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable { onOpenRoadmap() }
            .testTag("menu_roadmap_quick_button")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Description,
                contentDescription = null,
                tint = Color(0xFF00E5FF),
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "ROADMAP",
                color = Color(0xFF00E5FF),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

/**
 * Bottom platform and architectural summary notice.
 */
@Composable
fun SelectorFooterNotice(modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF131720),
        border = BorderStroke(1.dp, Color(0xFF263238)),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "PIXIE ENGINE KOTLIN MULTIPLATFORM CORE",
                color = Color(0xFF78909C),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Pure Kotlin core (:pixie-engine) + Android Compose/Canvas adapter (:pixie-android). Developing Raptor, Arkanoid, and Chiki step by step.",
                color = Color(0xFF546E7A),
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
