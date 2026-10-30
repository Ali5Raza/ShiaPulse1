package com.example.ui.components

import android.content.ComponentName
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.utils.PlaybackService
import kotlinx.coroutines.delay

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun AudioPlayerComponent(
    audioUrl: String,
    modifier: Modifier = Modifier,
    onPlaybackEnded: () -> Unit = {},
    onIsPlayingChanged: (Boolean) -> Unit = {},
    shouldPause: Boolean = false,
    onPauseHandled: () -> Unit = {},
    seekToPosition: Long? = null,
    onSeekHandled: () -> Unit = {},
    onProgressUpdate: (currentPositionMs: Long, durationMs: Long) -> Unit,
    autoPlay: Boolean = false
) {
    if (audioUrl.isBlank()) return

    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("shia_tracker_prefs", android.content.Context.MODE_PRIVATE) }
    val currentAudioUrl by rememberUpdatedState(audioUrl)
    val currentOnPlaybackEnded by rememberUpdatedState(onPlaybackEnded)
    var exoPlayer: Player? by remember { mutableStateOf(null) }

    DisposableEffect(context) {
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        
        controllerFuture.addListener({
            val controller = controllerFuture.get()
            exoPlayer = controller
        }, ContextCompat.getMainExecutor(context))
        
        onDispose {
            exoPlayer?.let { player ->
                val currentMediaUri = player.currentMediaItem?.localConfiguration?.uri?.toString()
                if (currentMediaUri == currentAudioUrl) {
                    prefs.edit().putLong("playback_pos_$currentAudioUrl", player.currentPosition).apply()
                }
            }
            MediaController.releaseFuture(controllerFuture)
            exoPlayer = null
        }
    }

    var isPlaying by remember { mutableStateOf(false) }
    var isExpanded by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableStateOf(0L) }
    var duration by remember { mutableStateOf(0L) }

    var playbackSpeed by remember { mutableFloatStateOf(1f) }
    var speedMenuExpanded by remember { mutableStateOf(false) }

    var continuousPlay by remember { mutableStateOf(true) }
    var keepScreenAwake by remember { mutableStateOf(false) }

    var sleepTimerMinutes by remember { mutableStateOf<Int?>(null) }
    var sleepTimerMenuExpanded by remember { mutableStateOf(false) }
    var sleepTimerRemainingSeconds by remember { mutableIntStateOf(0) }

    val view = LocalView.current
    DisposableEffect(keepScreenAwake) {
        view.keepScreenOn = keepScreenAwake
        onDispose {
            view.keepScreenOn = false
        }
    }

    LaunchedEffect(sleepTimerMinutes) {
        val timerMinutes = sleepTimerMinutes
        if (timerMinutes != null) {
            sleepTimerRemainingSeconds = timerMinutes * 60
            while (sleepTimerRemainingSeconds > 0) {
                delay(1000)
                sleepTimerRemainingSeconds--
            }
            if (sleepTimerRemainingSeconds <= 0) {
                exoPlayer?.pause()
                sleepTimerMinutes = null
            }
        } else {
            sleepTimerRemainingSeconds = 0
        }
    }

    LaunchedEffect(exoPlayer, audioUrl) {
        exoPlayer?.let { player ->
            val currentMediaUri = player.currentMediaItem?.localConfiguration?.uri?.toString()
            if (currentMediaUri != audioUrl) {
                // If it was playing something else, save its position before switching
                if (currentMediaUri != null) {
                    prefs.edit().putLong("playback_pos_$currentMediaUri", player.currentPosition).apply()
                }

                player.setMediaItem(MediaItem.fromUri(audioUrl))
                
                // Retrieve and restore saved playback position
                val savedPos = prefs.getLong("playback_pos_$audioUrl", 0L)
                if (savedPos > 1000L) {
                    player.seekTo(savedPos)
                }

                player.prepare()
                player.playWhenReady = autoPlay
            } else {
                // Same URL, but if the player is ready, it already has the position.
                // Just to be safe, if we are at 0 and have a saved position, seek.
                val savedPos = prefs.getLong("playback_pos_$audioUrl", 0L)
                if (player.currentPosition < 1000L && savedPos > 1000L) {
                    player.seekTo(savedPos)
                }
            }
        }
    }

    LaunchedEffect(shouldPause, exoPlayer) {
        if (shouldPause && exoPlayer != null) {
            exoPlayer?.pause()
            prefs.edit().putLong("playback_pos_$currentAudioUrl", exoPlayer?.currentPosition ?: 0L).apply()
            onPauseHandled()
        }
    }

    LaunchedEffect(seekToPosition, exoPlayer) {
        if (seekToPosition != null && exoPlayer != null) {
            exoPlayer?.seekTo(seekToPosition)
            prefs.edit().putLong("playback_pos_$audioUrl", seekToPosition).apply()
            onSeekHandled()
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlayingChanged: Boolean) {
                isPlaying = isPlayingChanged
                onIsPlayingChanged(isPlayingChanged)
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING
                if (playbackState == Player.STATE_READY) {
                    duration = exoPlayer?.duration?.coerceAtLeast(0L) ?: 0L
                } else if (playbackState == Player.STATE_ENDED) {
                    // Reset saved position to 0 since it completed
                    prefs.edit().putLong("playback_pos_$currentAudioUrl", 0L).apply()
                    if (continuousPlay) {
                        currentOnPlaybackEnded()
                    }
                }
            }
        }
        exoPlayer?.let { player ->
            player.addListener(listener)
            isPlaying = player.isPlaying
            onIsPlayingChanged(player.isPlaying)
            currentPosition = player.currentPosition
            duration = player.duration.coerceAtLeast(0L)
        }
        onDispose {
            exoPlayer?.removeListener(listener)
        }
    }

    LaunchedEffect(isPlaying, audioUrl) {
        var lastSavedPos = 0L
        while (isPlaying) {
            val pos = exoPlayer?.currentPosition ?: 0L
            currentPosition = pos
            duration = exoPlayer?.duration?.coerceAtLeast(0L) ?: 0L
            onProgressUpdate(currentPosition, duration)
            if (kotlin.math.abs(pos - lastSavedPos) >= 1000L) {
                prefs.edit().putLong("playback_pos_$audioUrl", pos).apply()
                lastSavedPos = pos
            }
            delay(200) // Update progress every 200ms
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = if (isExpanded) 12.dp else 4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Play/Pause Button
                    Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                        if (isBuffering) {
                            androidx.compose.material3.CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            IconButton(
                                onClick = {
                                    exoPlayer?.let { player ->
                                        if (player.playbackState == Player.STATE_ENDED) {
                                            player.seekTo(0)
                                        }
                                        if (isPlaying) {
                                            player.pause()
                                            prefs.edit().putLong("playback_pos_$currentAudioUrl", player.currentPosition).apply()
                                        } else {
                                            player.play()
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))

                    // Progress Slider
                    Column(modifier = Modifier.weight(1f)) {
                        val progress = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f
                        Slider(
                            value = progress,
                            onValueChange = { newProgress ->
                                val newPosition = (newProgress * duration).toLong()
                                exoPlayer?.seekTo(newPosition)
                                currentPosition = newPosition
                                onProgressUpdate(currentPosition, duration)
                                prefs.edit().putLong("playback_pos_$audioUrl", newPosition).apply()
                            },
                            modifier = Modifier.height(24.dp)
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = formatTime(currentPosition),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.Gray
                            )
                            Text(
                                text = formatTime(duration),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.Gray
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = { isExpanded = !isExpanded },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isExpanded) "Show Less" else "Show More",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                AnimatedVisibility(
                    visible = isExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = Color.LightGray.copy(alpha = 0.3f)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Speed Control
                            Box {
                                TextButton(onClick = { speedMenuExpanded = true }) {
                                    Icon(Icons.Default.Speed, contentDescription = "Speed", modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("${playbackSpeed}x", fontSize = 12.sp)
                                }
                                DropdownMenu(expanded = speedMenuExpanded, onDismissRequest = { speedMenuExpanded = false }) {
                                    listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f).forEach { speed ->
                                        DropdownMenuItem(
                                            text = { Text("${speed}x") },
                                            onClick = {
                                                playbackSpeed = speed
                                                exoPlayer?.playbackParameters = PlaybackParameters(speed)
                                                speedMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            // Continuous Play (Next)
                            val autoPlayColor = if (continuousPlay) MaterialTheme.colorScheme.primary else LocalContentColor.current.copy(alpha = 0.7f)
                            IconButton(onClick = { continuousPlay = !continuousPlay }) {
                                Icon(
                                    imageVector = Icons.Default.Repeat,
                                    contentDescription = "Continuous Play (Next)",
                                    modifier = Modifier.size(20.dp),
                                    tint = autoPlayColor
                                )
                            }

                            // Loop Current Dua
                            var isLooping by remember { mutableStateOf(exoPlayer?.repeatMode == Player.REPEAT_MODE_ONE) }
                            val loopColor = if (isLooping) MaterialTheme.colorScheme.primary else LocalContentColor.current.copy(alpha = 0.7f)
                            IconButton(onClick = { 
                                isLooping = !isLooping
                                exoPlayer?.repeatMode = if (isLooping) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
                            }) {
                                Icon(
                                    imageVector = Icons.Default.RepeatOne,
                                    contentDescription = "Loop Current",
                                    modifier = Modifier.size(20.dp),
                                    tint = loopColor
                                )
                            }

                            // Sleep Timer
                            Box {
                                val timerColor = if (sleepTimerMinutes != null) MaterialTheme.colorScheme.primary else LocalContentColor.current.copy(alpha = 0.7f)
                                TextButton(onClick = { sleepTimerMenuExpanded = true }) {
                                    Icon(Icons.Default.Timer, contentDescription = "Timer", modifier = Modifier.size(16.dp), tint = timerColor)
                                    Spacer(Modifier.width(4.dp))
                                    val timerText = if (sleepTimerMinutes != null) {
                                        String.format("%02d:%02d", sleepTimerRemainingSeconds / 60, sleepTimerRemainingSeconds % 60)
                                    } else "Timer"
                                    Text(timerText, fontSize = 12.sp, color = timerColor)
                                }
                                DropdownMenu(expanded = sleepTimerMenuExpanded, onDismissRequest = { sleepTimerMenuExpanded = false }) {
                                    DropdownMenuItem(text = { Text("Off") }, onClick = { sleepTimerMinutes = null; sleepTimerMenuExpanded = false })
                                    listOf(5, 10, 15, 20, 30).forEach { mins ->
                                        DropdownMenuItem(
                                            text = { Text("$mins minutes") },
                                            onClick = {
                                                sleepTimerMinutes = mins
                                                sleepTimerMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            // Keep Awake
                            val awakeColor = if (keepScreenAwake) MaterialTheme.colorScheme.primary else LocalContentColor.current.copy(alpha = 0.7f)
                            TextButton(onClick = { keepScreenAwake = !keepScreenAwake }) {
                                Icon(
                                    imageVector = if (keepScreenAwake) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Keep Awake",
                                    modifier = Modifier.size(16.dp),
                                    tint = awakeColor
                                )
                                Spacer(Modifier.width(4.dp))
                                Text("Awake", fontSize = 12.sp, color = awakeColor)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatTime(timeMs: Long): String {
    if (timeMs < 0) return "00:00"
    val totalSeconds = timeMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
