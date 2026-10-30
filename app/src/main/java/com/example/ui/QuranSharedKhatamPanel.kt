package com.example.ui

import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import com.example.data.QuranSharedJuz
import com.example.data.QuranSharedJuzAction
import com.example.data.QuranSharedKhatamRepository
import com.example.data.QuranSharedKhatamSnapshot
import com.example.data.QuranSharedMember
import com.example.data.SHARED_KHATAM_JUZ_COUNT
import com.example.data.SHARED_KHATAM_TARGET_OPTIONS
import com.example.data.normalizeSharedKhatamCode
import com.example.data.sharedKhatamDailyJuzGoal
import com.example.data.sharedKhatamDaysRemaining
import com.example.receiver.QuranSharedKhatamReminderScheduler
import com.google.firebase.auth.FirebaseAuth
import java.util.Locale

private const val SHARED_KHATAM_PREFS = "quran_shared_khatam"

@Composable
fun QuranSharedKhatamPanel(languageCode: String) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val prefs = remember { context.getSharedPreferences(SHARED_KHATAM_PREFS, android.content.Context.MODE_PRIVATE) }
    var activeCode by remember { mutableStateOf(prefs.getString("active_code", null)) }
    var displayName by rememberSaveable {
        mutableStateOf(
            prefs.getString("display_name", null)
                ?: runCatching { FirebaseAuth.getInstance().currentUser?.displayName }.getOrNull().orEmpty()
        )
    }
    var titleInput by rememberSaveable { mutableStateOf("") }
    var joinInput by rememberSaveable { mutableStateOf("") }
    var snapshot by remember { mutableStateOf<QuranSharedKhatamSnapshot?>(null) }
    var currentUserId by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var isBusy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    var memberPendingRemoval by remember { mutableStateOf<Pair<String, QuranSharedMember>?>(null) }
    var showLeaveConfirm by remember { mutableStateOf(false) }

    fun saveDisplayName() {
        displayName = displayName.trim().replace(Regex("\\s+"), " ").take(40)
        prefs.edit().putString("display_name", displayName).apply()
    }

    fun activateKhatam(code: String) {
        prefs.edit().putString("active_code", code).apply()
        snapshot = null
        error = null
        activeCode = code
    }

    fun switchKhatam() {
        prefs.edit().remove("active_code").apply()
        activeCode = null
        snapshot = null
        currentUserId = null
        error = null
        notice = null
        joinInput = ""
    }

    fun updateJuz(juzNumber: Int, action: QuranSharedJuzAction) {
        val code = activeCode ?: return
        if (isBusy) return
        isBusy = true
        error = null
        QuranSharedKhatamRepository.updateJuz(
            rawCode = code,
            juzNumber = juzNumber,
            displayName = displayName,
            action = action,
            onSuccess = { isBusy = false },
            onError = {
                isBusy = false
                error = it
            }
        )
    }

    fun performGroupMutation(
        successNotice: String?,
        onSuccess: () -> Unit = {},
        operation: ((String?) -> Unit) -> Unit
    ) {
        if (isBusy) return
        isBusy = true
        error = null
        notice = null
        operation { mutationError ->
            isBusy = false
            if (mutationError == null) {
                notice = successNotice
                onSuccess()
            } else {
                error = mutationError
            }
        }
    }

    DisposableEffect(activeCode) {
        val code = activeCode
        var stop: (() -> Unit)? = null
        if (!code.isNullOrBlank()) {
            isLoading = true
            snapshot = null
            error = null
            stop = QuranSharedKhatamRepository.observeKhatam(
                rawCode = code,
                onUser = { currentUserId = it },
                onSnapshot = { result ->
                    isLoading = false
                    if (result == null) {
                        snapshot = null
                        error = "not_found"
                    } else {
                        snapshot = result
                        error = null
                    }
                },
                onError = {
                    isLoading = false
                    error = it
                }
            )
        }
        onDispose { stop?.invoke() }
    }

    val code = activeCode?.takeIf { it.isNotBlank() }
    if (code == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Icon(
                        Icons.Default.Group,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        quranSharedText("title", languageCode),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        quranSharedText("intro", languageCode),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it.take(40) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(quranSharedText("display_name", languageCode)) },
                singleLine = true
            )
            OutlinedTextField(
                value = titleInput,
                onValueChange = { titleInput = it.take(60) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(quranSharedText("khatam_title_optional", languageCode)) },
                singleLine = true
            )
            Button(
                onClick = {
                    saveDisplayName()
                    if (displayName.isBlank()) {
                        error = "name_required"
                    } else {
                        isBusy = true
                        error = null
                        notice = null
                        QuranSharedKhatamRepository.createKhatam(
                            title = titleInput.ifBlank { quranSharedText("default_title", languageCode) },
                            displayName = displayName,
                            onSuccess = {
                                isBusy = false
                                activateKhatam(it)
                                notice = "created"
                            },
                            onError = {
                                isBusy = false
                                error = it
                            }
                        )
                    }
                },
                enabled = !isBusy,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(quranSharedText("create", languageCode))
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                HorizontalDivider(modifier = Modifier.weight(1f))
                Text(
                    quranSharedText("or", languageCode),
                    modifier = Modifier.padding(horizontal = 12.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                HorizontalDivider(modifier = Modifier.weight(1f))
            }

            OutlinedTextField(
                value = joinInput,
                onValueChange = { joinInput = normalizeSharedKhatamCode(it).take(6) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(quranSharedText("invite_code", languageCode)) },
                placeholder = { Text("ABC234") },
                singleLine = true
            )
            OutlinedButton(
                onClick = {
                    saveDisplayName()
                    when {
                        displayName.isBlank() -> error = "name_required"
                        joinInput.length != 6 -> error = "invalid_code"
                        else -> {
                            isBusy = true
                            error = null
                            notice = null
                            QuranSharedKhatamRepository.joinKhatam(
                                rawCode = joinInput,
                                displayName = displayName,
                                onSuccess = {
                                    isBusy = false
                                    activateKhatam(it)
                                    notice = "joined"
                                },
                                onError = {
                                    isBusy = false
                                    error = it
                                }
                            )
                        }
                    }
                },
                enabled = !isBusy,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(quranSharedText("join", languageCode))
            }
            if (isBusy) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }
            SharedKhatamMessage(error, notice, languageCode)
            Text(
                quranSharedText("privacy_note", languageCode),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    quranSharedText("title", languageCode),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = { switchKhatam() }) {
                    Text(quranSharedText("switch", languageCode))
                }
            }
        }
        item { SharedKhatamMessage(error, notice, languageCode) }
        if (isLoading) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(quranSharedText("connecting", languageCode))
                }
            }
        } else {
            snapshot?.let { group ->
                val isOwner = currentUserId != null && group.ownerId == currentUserId
                item {
                    Card(shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                group.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(5.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    quranSharedText("invite_code", languageCode) + ": " + group.code,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = {
                                    clipboard.setText(AnnotatedString(group.code))
                                    notice = "copied"
                                }) {
                                    Icon(
                                        Icons.Default.Share,
                                        contentDescription = quranSharedText("copy_code", languageCode)
                                    )
                                }
                            }
                            Text(
                                "${group.completedCount}/$SHARED_KHATAM_JUZ_COUNT " +
                                    quranSharedText("completed_juz", languageCode) +
                                    " · ${group.assignedCount}/$SHARED_KHATAM_JUZ_COUNT " +
                                    quranSharedText("assigned", languageCode) +
                                    " · ${group.members.size} " + quranSharedText("members", languageCode),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            LinearProgressIndicator(
                                progress = { group.completedCount.toFloat() / SHARED_KHATAM_JUZ_COUNT },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
                item {
                    SharedKhatamTargetCard(
                        group = group,
                        isOwner = isOwner,
                        isBusy = isBusy,
                        languageCode = languageCode,
                        onTargetSelected = { days ->
                            performGroupMutation("target_updated") { callback ->
                                QuranSharedKhatamRepository.updateTargetDays(group.code, days, callback)
                            }
                        }
                    )
                }
                item {
                    SharedKhatamReminderCard(
                        group = group,
                        languageCode = languageCode,
                        onError = { error = it },
                        onNotice = {
                            error = null
                            notice = it
                        }
                    )
                }
                item {
                    SharedKhatamMembersCard(
                        group = group,
                        isOwner = isOwner,
                        isBusy = isBusy,
                        languageCode = languageCode,
                        onRemove = { memberId, member ->
                            memberPendingRemoval = memberId to member
                        }
                    )
                }
                items(group.juz, key = { it.number }) { juz ->
                    SharedJuzRow(
                        juz = juz,
                        currentUserId = currentUserId,
                        isOwner = isOwner,
                        isBusy = isBusy,
                        languageCode = languageCode,
                        onClaim = { updateJuz(juz.number, QuranSharedJuzAction.CLAIM) },
                        onToggle = { updateJuz(juz.number, QuranSharedJuzAction.TOGGLE_COMPLETED) },
                        onRelease = { updateJuz(juz.number, QuranSharedJuzAction.RELEASE) },
                        onOwnerRelease = {
                            performGroupMutation("juz_released") { callback ->
                                QuranSharedKhatamRepository.releaseJuzAsOwner(group.code, juz.number, callback)
                            }
                        }
                    )
                }
                if (currentUserId != null && !isOwner) {
                    item {
                        OutlinedButton(
                            onClick = { showLeaveConfirm = true },
                            enabled = !isBusy,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(quranSharedText("leave", languageCode))
                        }
                    }
                }
            }
        }
    }

    memberPendingRemoval?.let { (memberId, member) ->
        AlertDialog(
            onDismissRequest = { if (!isBusy) memberPendingRemoval = null },
            title = { Text(quranSharedText("remove_member", languageCode)) },
            text = {
                Text(quranSharedText("remove_member_confirm", languageCode).replace("%s", member.displayName))
            },
            confirmButton = {
                TextButton(onClick = {
                    memberPendingRemoval = null
                    performGroupMutation("member_removed") { callback ->
                        QuranSharedKhatamRepository.removeMember(code, memberId, callback)
                    }
                }) { Text(quranSharedText("remove", languageCode)) }
            },
            dismissButton = {
                TextButton(onClick = { memberPendingRemoval = null }) {
                    Text(quranSharedText("cancel", languageCode))
                }
            }
        )
    }

    if (showLeaveConfirm) {
        AlertDialog(
            onDismissRequest = { if (!isBusy) showLeaveConfirm = false },
            title = { Text(quranSharedText("leave", languageCode)) },
            text = { Text(quranSharedText("leave_confirm", languageCode)) },
            confirmButton = {
                TextButton(onClick = {
                    showLeaveConfirm = false
                    performGroupMutation(
                        successNotice = null,
                        onSuccess = {
                            QuranSharedKhatamReminderScheduler.setReminder(
                                context, code, snapshot?.title.orEmpty(), 20, 0, enabled = false
                            )
                            switchKhatam()
                        }
                    ) { callback ->
                        QuranSharedKhatamRepository.leaveKhatam(code, callback)
                    }
                }) { Text(quranSharedText("leave", languageCode)) }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveConfirm = false }) {
                    Text(quranSharedText("cancel", languageCode))
                }
            }
        )
    }
}

@Composable
private fun SharedKhatamTargetCard(
    group: QuranSharedKhatamSnapshot,
    isOwner: Boolean,
    isBusy: Boolean,
    languageCode: String,
    onTargetSelected: (Int) -> Unit
) {
    val daysRemaining = sharedKhatamDaysRemaining(group.targetStartedAt, group.targetDays)
    val dailyGoal = sharedKhatamDailyJuzGoal(group.completedCount, daysRemaining)
    Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(
                quranSharedText("target", languageCode),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                quranSharedText("target_length", languageCode).replace("%d", group.targetDays.toString()) +
                    " · " + quranSharedText("days_remaining", languageCode).replace("%d", daysRemaining.toString()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                when {
                    group.completedCount == SHARED_KHATAM_JUZ_COUNT -> quranSharedText("target_completed", languageCode)
                    daysRemaining == 0 -> quranSharedText("target_date_reached", languageCode)
                    else -> quranSharedText("daily_goal", languageCode).replace("%d", dailyGoal.toString())
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            if (isOwner) {
                Text(
                    quranSharedText("choose_target", languageCode),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                SHARED_KHATAM_TARGET_OPTIONS.chunked(2).forEach { options ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        options.forEach { days ->
                            FilterChip(
                                selected = group.targetDays == days,
                                onClick = { if (group.targetDays != days) onTargetSelected(days) },
                                enabled = !isBusy,
                                label = { Text(quranSharedText("days_option", languageCode).replace("%d", days.toString())) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SharedKhatamReminderCard(
    group: QuranSharedKhatamSnapshot,
    languageCode: String,
    onError: (String) -> Unit,
    onNotice: (String) -> Unit
) {
    val context = LocalContext.current
    var enabled by rememberSaveable(group.code) {
        mutableStateOf(QuranSharedKhatamReminderScheduler.isEnabled(context, group.code))
    }
    var hour by rememberSaveable(group.code) {
        mutableStateOf(QuranSharedKhatamReminderScheduler.reminderHour(context, group.code))
    }
    var minute by rememberSaveable(group.code) {
        mutableStateOf(QuranSharedKhatamReminderScheduler.reminderMinute(context, group.code))
    }
    val time = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)

    fun saveTime(selectedHour: Int, selectedMinute: Int) {
        hour = selectedHour
        minute = selectedMinute
        QuranSharedKhatamReminderScheduler.setReminder(
            context, group.code, group.title, selectedHour, selectedMinute, enabled
        )
        onNotice("reminder_time_updated")
    }

    Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        quranSharedText("daily_reminder", languageCode),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        quranSharedText("reminder_note", languageCode),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = { shouldEnable ->
                        if (shouldEnable && !NotificationManagerCompat.from(context).areNotificationsEnabled()) {
                            onError("notifications_disabled")
                        } else {
                            enabled = shouldEnable
                            QuranSharedKhatamReminderScheduler.setReminder(
                                context, group.code, group.title, hour, minute, shouldEnable
                            )
                            onNotice(if (shouldEnable) "reminder_enabled" else "reminder_disabled")
                        }
                    }
                )
            }
            OutlinedButton(
                onClick = {
                    TimePickerDialog(
                        context,
                        { _, selectedHour, selectedMinute -> saveTime(selectedHour, selectedMinute) },
                        hour,
                        minute,
                        android.text.format.DateFormat.is24HourFormat(context)
                    ).show()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(quranSharedText("reminder_time", languageCode) + ": " + time)
            }
        }
    }
}

@Composable
private fun SharedKhatamMembersCard(
    group: QuranSharedKhatamSnapshot,
    isOwner: Boolean,
    isBusy: Boolean,
    languageCode: String,
    onRemove: (String, QuranSharedMember) -> Unit
) {
    Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                quranSharedText("members_title", languageCode),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            group.members.entries.sortedWith(compareBy({ it.key != group.ownerId }, { it.value.displayName }))
                .forEach { (memberId, member) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                member.displayName.ifBlank { quranSharedText("member", languageCode) },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                if (memberId == group.ownerId) quranSharedText("owner", languageCode)
                                else quranSharedText("member_assigned", languageCode).replace(
                                    "%d", group.juz.count { it.userId == memberId }.toString()
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (isOwner && memberId != group.ownerId) {
                            TextButton(onClick = { onRemove(memberId, member) }, enabled = !isBusy) {
                                Text(quranSharedText("remove", languageCode))
                            }
                        }
                    }
                }
            if (isOwner) {
                Text(
                    quranSharedText("owner_controls_note", languageCode),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SharedJuzRow(
    juz: QuranSharedJuz,
    currentUserId: String?,
    isOwner: Boolean,
    isBusy: Boolean,
    languageCode: String,
    onClaim: () -> Unit,
    onToggle: () -> Unit,
    onRelease: () -> Unit,
    onOwnerRelease: () -> Unit
) {
    val isMine = juz.isAssigned && juz.userId == currentUserId
    Surface(
        shape = RoundedCornerShape(17.dp),
        color = if (juz.completed) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isMine) {
                Checkbox(
                    checked = juz.completed,
                    onCheckedChange = { onToggle() },
                    enabled = !isBusy
                )
            } else {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = if (juz.completed) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    quranSharedText("juz", languageCode).replace("%d", juz.number.toString()),
                    fontWeight = FontWeight.Bold,
                    color = if (juz.completed) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = when {
                        !juz.isAssigned -> quranSharedText("available", languageCode)
                        isMine && juz.completed -> quranSharedText("you_completed", languageCode)
                        isMine -> quranSharedText("assigned_to_you", languageCode)
                        juz.completed -> juz.displayName + " · " + quranSharedText("completed", languageCode)
                        else -> juz.displayName
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            when {
                !juz.isAssigned -> OutlinedButton(
                    onClick = onClaim,
                    enabled = !isBusy && currentUserId != null
                ) {
                    Text(quranSharedText("assign_me", languageCode))
                }
                isMine -> TextButton(onClick = onRelease, enabled = !isBusy) {
                    Text(quranSharedText("release", languageCode))
                }
                isOwner -> TextButton(onClick = onOwnerRelease, enabled = !isBusy) {
                    Text(quranSharedText("release", languageCode))
                }
            }
        }
    }
}

@Composable
private fun SharedKhatamMessage(error: String?, notice: String?, languageCode: String) {
    val message = when {
        error != null -> quranSharedError(error, languageCode)
        notice != null -> quranSharedText(notice, languageCode)
        else -> return
    }
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (error != null) MaterialTheme.colorScheme.errorContainer
            else MaterialTheme.colorScheme.secondaryContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            message,
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodySmall,
            color = if (error != null) MaterialTheme.colorScheme.onErrorContainer
            else MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

private fun quranSharedError(error: String, languageCode: String): String {
    val key = when (error) {
        "name_required", "invalid_code", "not_found", "assigned_to_other", "claim_required",
        "invalid_juz", "unavailable", "not_owner", "owner_cannot_leave", "member_not_found",
        "invalid_target", "mutation_rejected", "notifications_disabled" -> error
        else -> "online_error"
    }
    val localized = quranSharedText(key, languageCode)
    return if (key == "online_error" && error.isNotBlank()) "$localized\n$error" else localized
}

internal fun quranSharedText(key: String, languageCode: String): String = when (languageCode) {
    "ur" -> when (key) {
        "title" -> "مشترکہ آن لائن ختم قرآن"
        "target" -> "ختم قرآن کا ہدف"
        "target_length" -> "%d دن کا ہدف"
        "days_remaining" -> "%d دن باقی"
        "target_completed" -> "تمام 30 پارے مکمل ہو گئے۔"
        "target_date_reached" -> "ہدف کی تاریخ آ گئی ہے؛ باقی پارے اپنے وقت میں مکمل کریں۔"
        "daily_goal" -> "تجویز کردہ رفتار: روزانہ %d پارہ"
        "choose_target" -> "مالک کے لیے ہدف کی مدت منتخب کریں"
        "days_option" -> "%d دن"
        "daily_reminder" -> "روزانہ یاد دہانی"
        "reminder_note" -> "اس ڈیوائس پر مقامی یاد دہانی؛ کھولنے پر یہی مشترکہ ختم قرآن ملے گا۔"
        "reminder_time" -> "یاد دہانی کا وقت"
        "reminder_time_updated" -> "یاد دہانی کا وقت محفوظ ہو گیا۔"
        "reminder_enabled" -> "روزانہ یاد دہانی فعال ہو گئی۔"
        "reminder_disabled" -> "روزانہ یاد دہانی بند ہو گئی۔"
        "notifications_disabled" -> "نوٹیفیکیشن کی اجازت دیں تاکہ یاد دہانی موصول ہو۔"
        "members_title" -> "ارکان اور کنٹرول"
        "member" -> "رکن"
        "owner" -> "مالک"
        "member_assigned" -> "%d پارے تفویض شدہ"
        "remove" -> "ہٹائیں"
        "remove_member" -> "رکن کو ہٹائیں"
        "remove_member_confirm" -> "%s کو ہٹائیں؟ اس رکن کے تفویض شدہ پارے بھی دستیاب ہو جائیں گے۔"
        "cancel" -> "منسوخ کریں"
        "leave" -> "ختم قرآن چھوڑیں"
        "leave_confirm" -> "کیا آپ یہ مشترکہ ختم قرآن چھوڑنا چاہتے ہیں؟ آپ کے پارے دوبارہ تقسیم کے لیے دستیاب ہوں گے۔"
        "member_removed" -> "رکن ہٹا دیا گیا اور اس کے پارے دستیاب کر دیے گئے۔"
        "juz_released" -> "پارہ دوبارہ تقسیم کے لیے دستیاب ہے۔"
        "target_updated" -> "ختم قرآن کا ہدف اپڈیٹ ہو گیا۔"
        "owner_controls_note" -> "مالک کسی بھی رکن کو ہٹا سکتا ہے اور نیچے کسی بھی پارے کا تفویض ختم کر سکتا ہے۔"
        "not_owner" -> "یہ کنٹرول صرف ختم قرآن کے مالک کے لیے ہے۔"
        "owner_cannot_leave" -> "مالک ختم قرآن نہیں چھوڑ سکتا؛ ارکان کو ہٹائیں یا گروپ بدلیں۔"
        "member_not_found" -> "یہ رکن اب گروپ میں موجود نہیں۔"
        "invalid_target" -> "ہدف کی مدت 7، 30، 40 یا 60 دن منتخب کریں۔"
        "mutation_rejected" -> "تبدیلی محفوظ نہیں ہو سکی؛ دوبارہ کوشش کریں۔"
        "live_shared" -> "لائیو مشترکہ پیش رفت"
        "default_title" -> "مشترکہ ختم قرآن"
        "intro" -> "دعوتی کوڈ سے ختم قرآن بنائیں یا شامل ہوں۔ 30 پارے تقسیم کریں اور سب کی پیش رفت لائیو دیکھیں۔"
        "display_name" -> "آپ کا نام"
        "khatam_title_optional" -> "ختم قرآن کا عنوان (اختیاری)"
        "create" -> "مشترکہ ختم قرآن بنائیں"
        "join" -> "کوڈ سے شامل ہوں"
        "or" -> "یا"
        "invite_code" -> "دعوتی کوڈ"
        "privacy_note" -> "ذاتی offline Khatam الگ اور محفوظ رہتا ہے۔ آن لائن حصے کے لیے Firebase session اور انٹرنیٹ درکار ہے؛ صرف کوڈ والے ساتھی شامل ہوں۔"
        "switch" -> "ختم قرآن بدلیں"
        "connecting" -> "مشترکہ پیش رفت سے جڑا جا رہا ہے…"
        "completed_juz" -> "پارے مکمل"
        "assigned" -> "تقسیم شدہ"
        "members" -> "ارکان"
        "juz" -> "پارہ %d"
        "available" -> "ابھی کسی کو تفویض نہیں"
        "assigned_to_you" -> "آپ کے ذمے"
        "you_completed" -> "آپ نے مکمل کیا"
        "completed" -> "مکمل"
        "assign_me" -> "میرے ذمے کریں"
        "release" -> "چھوڑیں"
        "copy_code" -> "دعوتی کوڈ نقل کریں"
        "created" -> "مشترکہ ختم قرآن بن گیا۔ کوڈ ساتھیوں کو دیں۔"
        "joined" -> "آپ مشترکہ ختم قرآن میں شامل ہو گئے۔"
        "copied" -> "دعوتی کوڈ نقل ہو گیا۔"
        "name_required" -> "پہلے اپنا نام لکھیں۔"
        "invalid_code" -> "6 حروف کا درست دعوتی کوڈ لکھیں۔"
        "not_found" -> "اس کوڈ کا مشترکہ ختم قرآن نہیں ملا۔"
        "assigned_to_other" -> "یہ پارہ کسی اور ساتھی کے ذمے ہے۔"
        "claim_required" -> "پہلے یہ پارہ اپنے ذمے لیں۔"
        "invalid_juz" -> "پارہ نمبر 1 تا 30 ہونا چاہیے۔"
        "unavailable" -> "آن لائن ختم قرآن فی الحال دستیاب نہیں۔ دوبارہ کوشش کریں۔"
        "online_error" -> "آن لائن کنکشن یا اجازت کا مسئلہ ہے۔"
        else -> key
    }
    "ar" -> when (key) {
        "title" -> "ختمة القرآن المشتركة"
        "target" -> "هدف الختمة"
        "target_length" -> "هدف مدته %d يوماً"
        "days_remaining" -> "تبقى %d يوماً"
        "target_completed" -> "اكتملت الأجزاء الثلاثون كلها."
        "target_date_reached" -> "حان موعد الهدف؛ أتمم الأجزاء المتبقية وفق وقتك."
        "daily_goal" -> "الوتيرة المقترحة: %d جزء يومياً"
        "choose_target" -> "للمالك: اختر مدة الهدف"
        "days_option" -> "%d أيام"
        "daily_reminder" -> "تذكير يومي"
        "reminder_note" -> "تذكير محلي على هذا الجهاز؛ ويفتح هذه الختمة المشتركة مباشرة."
        "reminder_time" -> "وقت التذكير"
        "reminder_time_updated" -> "تم حفظ وقت التذكير."
        "reminder_enabled" -> "تم تفعيل التذكير اليومي."
        "reminder_disabled" -> "تم إيقاف التذكير اليومي."
        "notifications_disabled" -> "اسمح بالإشعارات لتصلك التذكيرات."
        "members_title" -> "الأعضاء وعناصر التحكم"
        "member" -> "عضو"
        "owner" -> "المالك"
        "member_assigned" -> "%d أجزاء مخصصة"
        "remove" -> "إزالة"
        "remove_member" -> "إزالة العضو"
        "remove_member_confirm" -> "هل تريد إزالة %s؟ ستصبح أجزاؤه المخصصة متاحة أيضاً."
        "cancel" -> "إلغاء"
        "leave" -> "مغادرة الختمة"
        "leave_confirm" -> "هل تريد مغادرة هذه الختمة المشتركة؟ ستصبح أجزاؤك متاحة لإعادة التوزيع."
        "member_removed" -> "تمت إزالة العضو وإتاحة أجزائه."
        "juz_released" -> "الجزء متاح الآن لإعادة التوزيع."
        "target_updated" -> "تم تحديث هدف الختمة."
        "owner_controls_note" -> "يمكن للمالك إزالة أي عضو وإلغاء إسناد أي جزء أدناه."
        "not_owner" -> "هذا التحكم متاح لمالك الختمة فقط."
        "owner_cannot_leave" -> "لا يمكن للمالك مغادرة الختمة؛ أزل الأعضاء أو غيّر المجموعة."
        "member_not_found" -> "لم يعد هذا العضو موجوداً في المجموعة."
        "invalid_target" -> "اختر مدة 7 أو 30 أو 40 أو 60 يوماً."
        "mutation_rejected" -> "تعذر حفظ التغيير؛ حاول مجدداً."
        "live_shared" -> "تقدم مشترك مباشر"
        "default_title" -> "ختمة قرآن مشتركة"
        "intro" -> "أنشئ ختمة أو انضم برمز دعوة. وزّع أجزاء القرآن الثلاثين وشاهد تقدم الجميع مباشرة."
        "display_name" -> "اسمك"
        "khatam_title_optional" -> "عنوان الختمة (اختياري)"
        "create" -> "إنشاء ختمة مشتركة"
        "join" -> "الانضمام بالرمز"
        "or" -> "أو"
        "invite_code" -> "رمز الدعوة"
        "privacy_note" -> "تبقى ختمتك الشخصية دون اتصال منفصلة. تتطلب المشاركة جلسة Firebase واتصالاً بالإنترنت، ولا ينضم إلا من يملك الرمز."
        "switch" -> "تغيير الختمة"
        "connecting" -> "جارٍ الاتصال بالتقدم المشترك…"
        "completed_juz" -> "جزءاً مكتملاً"
        "assigned" -> "موزعاً"
        "members" -> "أعضاء"
        "juz" -> "الجزء %d"
        "available" -> "غير مخصص بعد"
        "assigned_to_you" -> "مخصص لك"
        "you_completed" -> "أتممته"
        "completed" -> "مكتمل"
        "assign_me" -> "إسناده إليّ"
        "release" -> "إلغاء الإسناد"
        "copy_code" -> "نسخ رمز الدعوة"
        "created" -> "تم إنشاء الختمة. شارك الرمز مع رفاقك."
        "joined" -> "انضممت إلى الختمة المشتركة."
        "copied" -> "تم نسخ رمز الدعوة."
        "name_required" -> "اكتب اسمك أولاً."
        "invalid_code" -> "أدخل رمز دعوة صحيحاً من 6 أحرف."
        "not_found" -> "لم يتم العثور على ختمة بهذا الرمز."
        "assigned_to_other" -> "هذا الجزء مخصص لمشارك آخر."
        "claim_required" -> "أسند الجزء إليك أولاً."
        "invalid_juz" -> "يجب أن يكون رقم الجزء بين 1 و30."
        "unavailable" -> "الختمة المشتركة غير متاحة حالياً. حاول مجدداً."
        "online_error" -> "تعذر الاتصال أو لم تسمح الصلاحيات بالعملية."
        else -> key
    }
    else -> when (key) {
        "title" -> "Shared Online Khatam"
        "target" -> "Khatam target"
        "target_length" -> "%d-day target"
        "days_remaining" -> "%d days remaining"
        "target_completed" -> "All 30 Juz are complete."
        "target_date_reached" -> "The target date has arrived; finish the remaining Juz at your own pace."
        "daily_goal" -> "Suggested pace: %d Juz per day"
        "choose_target" -> "Owner: choose a target duration"
        "days_option" -> "%d days"
        "daily_reminder" -> "Daily reminder"
        "reminder_note" -> "A local reminder on this device; tapping it opens this shared Khatam."
        "reminder_time" -> "Reminder time"
        "reminder_time_updated" -> "Reminder time saved."
        "reminder_enabled" -> "Daily reminder enabled."
        "reminder_disabled" -> "Daily reminder disabled."
        "notifications_disabled" -> "Allow notifications so the reminder can be delivered."
        "members_title" -> "Members and controls"
        "member" -> "Member"
        "owner" -> "Owner"
        "member_assigned" -> "%d Juz assigned"
        "remove" -> "Remove"
        "remove_member" -> "Remove member"
        "remove_member_confirm" -> "Remove %s? Their assigned Juz will also become available."
        "cancel" -> "Cancel"
        "leave" -> "Leave Khatam"
        "leave_confirm" -> "Leave this shared Khatam? Your Juz will become available for reassignment."
        "member_removed" -> "Member removed and their Juz released."
        "juz_released" -> "Juz is available for reassignment."
        "target_updated" -> "Khatam target updated."
        "owner_controls_note" -> "The owner can remove any member and release any assigned Juz below."
        "not_owner" -> "Only the Khatam owner can use this control."
        "owner_cannot_leave" -> "The owner cannot leave this Khatam; remove members or switch groups instead."
        "member_not_found" -> "This member is no longer in the group."
        "invalid_target" -> "Choose a 7, 30, 40, or 60-day target."
        "mutation_rejected" -> "The change could not be saved. Please try again."
        "live_shared" -> "Live shared progress"
        "default_title" -> "Shared Quran Khatam"
        "intro" -> "Create or join with an invite code. Assign all 30 Juz and watch everyone's progress update live."
        "display_name" -> "Your display name"
        "khatam_title_optional" -> "Khatam title (optional)"
        "create" -> "Create shared Khatam"
        "join" -> "Join with code"
        "or" -> "OR"
        "invite_code" -> "Invite code"
        "privacy_note" -> "Your personal offline Khatam stays separate. Online sharing needs internet and a Firebase session; only people with the invite code can join."
        "switch" -> "Switch Khatam"
        "connecting" -> "Connecting to shared progress…"
        "completed_juz" -> "Juz completed"
        "assigned" -> "assigned"
        "members" -> "members"
        "juz" -> "Juz %d"
        "available" -> "Available to assign"
        "assigned_to_you" -> "Assigned to you"
        "you_completed" -> "Completed by you"
        "completed" -> "completed"
        "assign_me" -> "Assign to me"
        "release" -> "Release"
        "copy_code" -> "Copy invite code"
        "created" -> "Shared Khatam created. Share the invite code with your group."
        "joined" -> "You joined the shared Khatam."
        "copied" -> "Invite code copied."
        "name_required" -> "Enter your display name first."
        "invalid_code" -> "Enter a valid 6-character invite code."
        "not_found" -> "No shared Khatam was found for this code."
        "assigned_to_other" -> "This Juz is assigned to another participant."
        "claim_required" -> "Assign this Juz to yourself first."
        "invalid_juz" -> "Juz number must be between 1 and 30."
        "unavailable" -> "Shared Khatam is unavailable right now. Please try again."
        "online_error" -> "The online connection or permission check failed."
        else -> key
    }
}
