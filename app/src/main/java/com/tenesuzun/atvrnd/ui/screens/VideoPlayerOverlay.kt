package com.tenesuzun.atvrnd.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tenesuzun.atvrnd.R

@Composable
fun ReelsVideoPlayerOverlay(
    modifier: Modifier = Modifier,
    username: String = "username",
    profileImageId: Int = R.drawable.ic_launcher_foreground, // Replace with your placeholder
    caption: String = "This is a sample video caption with some hashtags #video #reels #trending",
    likeCount: String = "12.5K",
    commentCount: String = "2,450",
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent) // Transparent background to show video underneath
    ) {
        // Right side actions column
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Profile picture (larger)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.Gray)
                        .clickable { /* onClick */ }
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Profile",
                        tint = Color.White,
                        modifier = Modifier
                            .size(24.dp)
                            .align(Alignment.Center)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Follow",
                    tint = Color.White,
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0095F6))
                        .padding(2.dp)
                )
            }

            // Like button
            ActionButton(
                icon = Icons.Outlined.FavoriteBorder,
                count = likeCount
            )

            // Comment button
            ActionButton(
                icon = Icons.Default.Create,
                count = commentCount
            )

            // Share button
            ActionButton(
                icon = Icons.AutoMirrored.Default.Send,
                count = ""
            )

            // Save button
            ActionButton(
                icon = Icons.Outlined.AddCircle,
                count = ""
            )

            // More options
            ActionButton(
                icon = Icons.Outlined.MoreVert,
                count = ""
            )
        }

        // Bottom section with user info and caption
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
                .fillMaxWidth(0.85f)
        ) {
            // Username and music info
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.Gray)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Profile",
                        tint = Color.White,
                        modifier = Modifier
                            .size(20.dp)
                            .align(Alignment.Center)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = username,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "• Follow",
                    color = Color.White,
                    fontSize = 14.sp
                )
            }

            // Caption
            Text(
                text = caption,
                color = Color.White,
                fontSize = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Music info
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "Music",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Original Sound • Artist Name",
                    color = Color.White,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun ActionButton(
    icon: ImageVector,
    count: String,
    onClick: () -> Unit = {}
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier
                .size(28.dp)
                .clickable(onClick = onClick)
        )
        if (count.isNotEmpty()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = count,
                color = Color.White,
                fontSize = 12.sp
            )
        }
    }
}