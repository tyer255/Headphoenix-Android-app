package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Shimmer effect modifier that creates a subtle, smooth linear gradient animation
 * matching native Android design guidelines without heavy CPU overhead.
 */
fun Modifier.shimmerEffect(
    shape: Shape = RoundedCornerShape(8.dp),
    baseColor: Color = Color(0xFF222225),
    highlightColor: Color = Color(0xFF333338)
): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmerTransition")
    val translateAnim by transition.animateFloat(
        initialValue = -300f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerOffset"
    )

    val brush = Brush.linearGradient(
        colors = listOf(
            baseColor,
            highlightColor,
            baseColor
        ),
        start = Offset(translateAnim, translateAnim),
        end = Offset(translateAnim + 250f, translateAnim + 250f)
    )

    this
        .clip(shape)
        .background(brush)
}

@Composable
fun SkeletonBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(8.dp)
) {
    Box(
        modifier = modifier.shimmerEffect(shape = shape)
    )
}

@Composable
fun SkeletonLine(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(4.dp)
) {
    Box(
        modifier = modifier.shimmerEffect(shape = shape)
    )
}

@Composable
fun SkeletonCircle(
    modifier: Modifier = Modifier,
    size: Int = 48
) {
    Box(
        modifier = modifier
            .size(size.dp)
            .shimmerEffect(shape = CircleShape)
    )
}

@Composable
fun SkeletonTrackRow(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SkeletonBox(
            modifier = Modifier.size(48.dp),
            shape = RoundedCornerShape(6.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            SkeletonLine(
                modifier = Modifier
                    .fillMaxWidth(0.65f)
                    .height(14.dp),
                shape = RoundedCornerShape(4.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            SkeletonLine(
                modifier = Modifier
                    .fillMaxWidth(0.4f)
                    .height(11.dp),
                shape = RoundedCornerShape(4.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        SkeletonBox(
            modifier = Modifier.size(20.dp),
            shape = CircleShape
        )
    }
}

@Composable
fun SkeletonAddTracksRow(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 52dp Artwork Skeleton with play button placeholder
        SkeletonBox(
            modifier = Modifier.size(52.dp),
            shape = RoundedCornerShape(4.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        // Title & Artist Skeletons
        Column(modifier = Modifier.weight(1f)) {
            SkeletonLine(
                modifier = Modifier
                    .fillMaxWidth(0.65f)
                    .height(15.dp),
                shape = RoundedCornerShape(4.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            SkeletonLine(
                modifier = Modifier
                    .fillMaxWidth(0.42f)
                    .height(12.dp),
                shape = RoundedCornerShape(4.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        // Trailing (+) icon skeleton
        SkeletonBox(
            modifier = Modifier.size(28.dp),
            shape = CircleShape
        )
    }
}

@Composable
fun SkeletonAddTracksList(
    count: Int = 8,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeat(count) {
            SkeletonAddTracksRow()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkeletonAlbumScreen(
    onBack: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
    ) {
        TopAppBar(
            title = {},
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                navigationIconContentColor = Color.White
            )
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            // Header Skeleton
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Big Album Artwork Skeleton
                    SkeletonBox(
                        modifier = Modifier
                            .size(200.dp),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    // Album Title Skeleton
                    SkeletonLine(
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .height(22.dp),
                        shape = RoundedCornerShape(6.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    // Artist & Metadata Skeleton
                    SkeletonLine(
                        modifier = Modifier
                            .fillMaxWidth(0.45f)
                            .height(14.dp),
                        shape = RoundedCornerShape(4.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SkeletonLine(
                        modifier = Modifier
                            .fillMaxWidth(0.3f)
                            .height(12.dp),
                        shape = RoundedCornerShape(4.dp)
                    )
                }
            }

            // Action Buttons Skeleton Row
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SkeletonCircle(size = 32)
                        SkeletonCircle(size = 32)
                    }
                    SkeletonCircle(size = 56)
                }
            }

            // Track list skeletons
            items(6) {
                SkeletonTrackRow()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkeletonPlaylistScreen(
    onBack: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
    ) {
        TopAppBar(
            title = {},
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                navigationIconContentColor = Color.White
            )
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 140.dp)
        ) {
            // Header Skeleton
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SkeletonBox(
                            modifier = Modifier.size(130.dp),
                            shape = RoundedCornerShape(10.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            SkeletonLine(
                                modifier = Modifier
                                    .fillMaxWidth(0.9f)
                                    .height(20.dp),
                                shape = RoundedCornerShape(5.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            SkeletonLine(
                                modifier = Modifier
                                    .fillMaxWidth(0.7f)
                                    .height(13.dp),
                                shape = RoundedCornerShape(4.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            SkeletonLine(
                                modifier = Modifier
                                    .fillMaxWidth(0.45f)
                                    .height(12.dp),
                                shape = RoundedCornerShape(4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SkeletonCircle(size = 32)
                            SkeletonCircle(size = 32)
                        }
                        SkeletonCircle(size = 54)
                    }
                }
            }

            // Tracks Skeleton List
            items(7) {
                SkeletonTrackRow()
            }
        }
    }
}

@Composable
fun SkeletonArtistScreen(
    onBack: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 140.dp)
        ) {
            // Hero Banner Skeleton
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                ) {
                    SkeletonBox(
                        modifier = Modifier.fillMaxSize(),
                        shape = RoundedCornerShape(0.dp)
                    )

                    // Top Bar Back Button
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .statusBarsPadding()
                            .padding(8.dp)
                            .align(Alignment.TopStart)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    // Bottom info overlay skeleton
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(horizontal = 20.dp, vertical = 20.dp)
                    ) {
                        SkeletonLine(
                            modifier = Modifier
                                .width(220.dp)
                                .height(32.dp),
                            shape = RoundedCornerShape(6.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        SkeletonLine(
                            modifier = Modifier
                                .width(130.dp)
                                .height(14.dp),
                            shape = RoundedCornerShape(4.dp)
                        )
                    }
                }
            }

            // Action Buttons Row Skeleton
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Following pill button skeleton
                        SkeletonBox(
                            modifier = Modifier
                                .width(90.dp)
                                .height(34.dp),
                            shape = RoundedCornerShape(50)
                        )
                        SkeletonCircle(size = 32)
                    }
                    SkeletonCircle(size = 54)
                }
            }

            // Popular section title skeleton
            item {
                SkeletonLine(
                    modifier = Modifier
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                        .width(100.dp)
                        .height(18.dp),
                    shape = RoundedCornerShape(4.dp)
                )
            }

            // Popular track items skeleton
            items(5) {
                SkeletonTrackRow()
            }

            // Albums section title skeleton
            item {
                SkeletonLine(
                    modifier = Modifier
                        .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 12.dp)
                        .width(120.dp)
                        .height(18.dp),
                    shape = RoundedCornerShape(4.dp)
                )
            }

            // Horizontal Albums Row Skeleton
            item {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(4) {
                        Column(modifier = Modifier.width(130.dp)) {
                            SkeletonBox(
                                modifier = Modifier.size(130.dp),
                                shape = RoundedCornerShape(8.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            SkeletonLine(
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .height(14.dp),
                                shape = RoundedCornerShape(4.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            SkeletonLine(
                                modifier = Modifier
                                    .fillMaxWidth(0.5f)
                                    .height(11.dp),
                                shape = RoundedCornerShape(4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SkeletonSearchScreen() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 140.dp)
    ) {
        // Top Result Card Skeleton
        item {
            SkeletonLine(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 10.dp)
                    .width(110.dp)
                    .height(18.dp),
                shape = RoundedCornerShape(4.dp)
            )
            SkeletonBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp),
                shape = RoundedCornerShape(16.dp)
            )
        }

        // Song Results Title Skeleton
        item {
            SkeletonLine(
                modifier = Modifier
                    .padding(top = 22.dp, bottom = 10.dp)
                    .width(80.dp)
                    .height(18.dp),
                shape = RoundedCornerShape(4.dp)
            )
        }

        // Song Rows Skeleton
        items(4) {
            SkeletonTrackRow(modifier = Modifier.padding(horizontal = 0.dp))
        }

        // Artists Section Skeleton
        item {
            SkeletonLine(
                modifier = Modifier
                    .padding(top = 22.dp, bottom = 12.dp)
                    .width(90.dp)
                    .height(18.dp),
                shape = RoundedCornerShape(4.dp)
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(3) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(100.dp)
                    ) {
                        SkeletonCircle(size = 90)
                        Spacer(modifier = Modifier.height(8.dp))
                        SkeletonLine(
                            modifier = Modifier
                                .width(70.dp)
                                .height(13.dp),
                            shape = RoundedCornerShape(4.dp)
                        )
                    }
                }
            }
        }

        // Albums Section Skeleton
        item {
            SkeletonLine(
                modifier = Modifier
                    .padding(top = 22.dp, bottom = 12.dp)
                    .width(90.dp)
                    .height(18.dp),
                shape = RoundedCornerShape(4.dp)
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(3) {
                    Column(modifier = Modifier.width(130.dp)) {
                        SkeletonBox(
                            modifier = Modifier.size(130.dp),
                            shape = RoundedCornerShape(8.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        SkeletonLine(
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .height(14.dp),
                            shape = RoundedCornerShape(4.dp)
                        )
                    }
                }
            }
        }
    }
}
