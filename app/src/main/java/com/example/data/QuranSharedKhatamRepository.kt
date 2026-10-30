package com.example.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener
import java.security.SecureRandom

internal const val SHARED_KHATAM_JUZ_COUNT = 30
internal val SHARED_KHATAM_TARGET_OPTIONS = listOf(7, 30, 40, 60)

internal data class QuranSharedMember(
    val userId: String,
    val displayName: String,
    val joinedAt: Long = 0L
)

internal data class QuranSharedJuz(
    val number: Int,
    val userId: String = "",
    val displayName: String = "",
    val completed: Boolean = false,
    val updatedAt: Long = 0L
) {
    val isAssigned: Boolean get() = userId.isNotBlank()
}

internal data class QuranSharedKhatamSnapshot(
    val code: String,
    val title: String,
    val ownerId: String,
    val createdAt: Long,
    val targetDays: Int,
    val targetStartedAt: Long,
    val members: Map<String, QuranSharedMember>,
    val juz: List<QuranSharedJuz>
) {
    val completedCount: Int get() = juz.count { it.completed }
    val assignedCount: Int get() = juz.count { it.isAssigned }
}

internal fun sharedKhatamDaysRemaining(
    targetStartedAt: Long,
    targetDays: Int,
    nowMillis: Long = System.currentTimeMillis()
): Int {
    val elapsedDays = ((nowMillis - targetStartedAt).coerceAtLeast(0L)) / (24L * 60L * 60L * 1000L)
    return (targetDays.toLong() - elapsedDays).coerceIn(0L, targetDays.toLong()).toInt()
}

internal fun sharedKhatamDailyJuzGoal(completedCount: Int, daysRemaining: Int): Int {
    val remaining = (SHARED_KHATAM_JUZ_COUNT - completedCount).coerceAtLeast(0)
    if (remaining == 0) return 0
    return ((remaining + daysRemaining.coerceAtLeast(1) - 1) / daysRemaining.coerceAtLeast(1))
}

internal enum class QuranSharedJuzAction {
    CLAIM,
    TOGGLE_COMPLETED,
    RELEASE
}

/** Online collaborative Khatam only. Personal Quran progress remains in [QuranProgressStore]. */
internal object QuranSharedKhatamRepository {
    private const val DATABASE_URL =
        "https://shiapulse-pk-default-rtdb.asia-southeast1.firebasedatabase.app"
    private const val ROOT = "sharedKhatams"
    private const val CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    private const val CODE_LENGTH = 6
    private val random = SecureRandom()

    private val auth: FirebaseAuth?
        get() = runCatching { FirebaseAuth.getInstance() }.getOrNull()

    private val database: FirebaseDatabase?
        get() = runCatching { FirebaseDatabase.getInstance(DATABASE_URL) }.getOrNull()

    fun createKhatam(
        title: String,
        displayName: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val cleanName = cleanDisplayName(displayName)
        if (cleanName.isEmpty()) {
            onError("name_required")
            return
        }
        val cleanTitle = title.trim().take(60).ifBlank { "Quran Khatam" }
        ensureUser(
            onReady = { userId -> createWithUniqueCode(cleanTitle, cleanName, userId, 0, onSuccess, onError) },
            onError = onError
        )
    }

    private fun createWithUniqueCode(
        title: String,
        displayName: String,
        userId: String,
        attempt: Int,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        if (attempt >= 5) {
            onError("unavailable")
            return
        }
        val reference = groupReference(newInviteCode()) ?: run {
            onError("unavailable")
            return
        }
        reference.child("meta").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    createWithUniqueCode(title, displayName, userId, attempt + 1, onSuccess, onError)
                    return
                }
                val now = System.currentTimeMillis()
                val updates = mapOf<String, Any>(
                    "meta" to mapOf(
                        "title" to title,
                        "ownerId" to userId,
                        "createdAt" to now,
                        "targetDays" to 30,
                        "targetStartedAt" to now
                    ),
                    "members/$userId" to mapOf(
                        "displayName" to displayName,
                        "joinedAt" to now
                    )
                )
                reference.updateChildren(updates).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        onSuccess(reference.key.orEmpty())
                    } else {
                        onError(task.exception?.localizedMessage ?: "unavailable")
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) = onError(error.message)
        })
    }

    fun joinKhatam(
        rawCode: String,
        displayName: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val code = normalizeSharedKhatamCode(rawCode)
        val cleanName = cleanDisplayName(displayName)
        when {
            code.length != CODE_LENGTH -> onError("invalid_code")
            cleanName.isEmpty() -> onError("name_required")
            else -> ensureUser(
                onReady = { userId ->
                    val reference = groupReference(code) ?: run {
                        onError("unavailable")
                        return@ensureUser
                    }
                    reference.child("meta").addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if (!snapshot.exists()) {
                                onError("not_found")
                                return
                            }
                            val member = mapOf(
                                "displayName" to cleanName,
                                "joinedAt" to System.currentTimeMillis()
                            )
                            reference.child("members").child(userId).setValue(member)
                                .addOnCompleteListener { task ->
                                    if (task.isSuccessful) onSuccess(code)
                                    else onError(task.exception?.localizedMessage ?: "unavailable")
                                }
                        }

                        override fun onCancelled(error: DatabaseError) = onError(error.message)
                    })
                },
                onError = onError
            )
        }
    }

    fun observeKhatam(
        rawCode: String,
        onUser: (String) -> Unit,
        onSnapshot: (QuranSharedKhatamSnapshot?) -> Unit,
        onError: (String) -> Unit
    ): () -> Unit {
        val code = normalizeSharedKhatamCode(rawCode)
        val reference = groupReference(code)
        if (reference == null) {
            onError("unavailable")
            return {}
        }
        var stopped = false
        var listener: ValueEventListener? = null
        ensureUser(
            onReady = { userId ->
                if (stopped) return@ensureUser
                onUser(userId)
                val valueListener = object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (!stopped) onSnapshot(snapshot.toSharedKhatam(code))
                    }

                    override fun onCancelled(error: DatabaseError) {
                        if (!stopped) onError(error.message)
                    }
                }
                listener = valueListener
                reference.addValueEventListener(valueListener)
            },
            onError = { if (!stopped) onError(it) }
        )
        return {
            stopped = true
            listener?.let { reference.removeEventListener(it) }
            listener = null
        }
    }

    fun updateTargetDays(rawCode: String, targetDays: Int, onComplete: (String?) -> Unit) {
        mutateGroup(rawCode, ownerOnly = true, onComplete = onComplete) { group, _ ->
            when {
                targetDays !in SHARED_KHATAM_TARGET_OPTIONS -> "invalid_target"
                else -> {
                    group.child("meta/targetDays").value = targetDays
                    null
                }
            }
        }
    }

    fun releaseJuzAsOwner(rawCode: String, juzNumber: Int, onComplete: (String?) -> Unit) {
        mutateGroup(rawCode, ownerOnly = true, onComplete = onComplete) { group, _ ->
            when {
                juzNumber !in 1..SHARED_KHATAM_JUZ_COUNT -> "invalid_juz"
                else -> {
                    group.child("juz/$juzNumber").value = null
                    null
                }
            }
        }
    }

    fun removeMember(rawCode: String, memberId: String, onComplete: (String?) -> Unit) {
        mutateGroup(rawCode, ownerOnly = true, onComplete = onComplete) { group, userId ->
            when {
                memberId == userId -> "owner_cannot_leave"
                !group.hasChild("members/$memberId") -> "member_not_found"
                else -> {
                    group.child("members/$memberId").value = null
                    (1..SHARED_KHATAM_JUZ_COUNT)
                        .filter { group.child("juz/$it/userId").value == memberId }
                        .forEach { group.child("juz/$it").value = null }
                    null
                }
            }
        }
    }

    fun leaveKhatam(rawCode: String, onComplete: (String?) -> Unit) {
        mutateGroup(rawCode, ownerOnly = false, onComplete = onComplete) { group, userId ->
            when {
                !group.hasChild("members/$userId") -> "member_not_found"
                group.child("meta/ownerId").value == userId -> "owner_cannot_leave"
                else -> {
                    group.child("members/$userId").value = null
                    (1..SHARED_KHATAM_JUZ_COUNT)
                        .filter { group.child("juz/$it/userId").value == userId }
                        .forEach { group.child("juz/$it").value = null }
                    null
                }
            }
        }
    }

    private fun mutateGroup(
        rawCode: String,
        ownerOnly: Boolean,
        onComplete: (String?) -> Unit,
        mutation: (MutableData, String) -> String?
    ) {
        ensureUser(
            onReady = { userId ->
                val reference = groupReference(rawCode) ?: run {
                    onComplete("unavailable")
                    return@ensureUser
                }
                var rejection = "mutation_rejected"
                val completion = onComplete
                reference.runTransaction(object : Transaction.Handler {
                    override fun doTransaction(currentData: MutableData): Transaction.Result {
                        if (currentData.value == null) {
                            rejection = "not_found"
                            return Transaction.abort()
                        }
                        if (ownerOnly && currentData.child("meta/ownerId").value != userId) {
                            rejection = "not_owner"
                            return Transaction.abort()
                        }
                        val mutationError = mutation(currentData, userId)
                        if (mutationError != null) rejection = mutationError
                        return if (mutationError == null) Transaction.success(currentData) else Transaction.abort()
                    }

                    override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {
                        completion(if (error != null) error.message ?: "network_error" else if (committed) null else rejection)
                    }
                })
            },
            onError = { error ->
                onComplete(error.ifBlank { "auth_failed" })
            }
        )
    }

    fun updateJuz(
        rawCode: String,
        juzNumber: Int,
        displayName: String,
        action: QuranSharedJuzAction,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (juzNumber !in 1..SHARED_KHATAM_JUZ_COUNT) {
            onError("invalid_juz")
            return
        }
        mutateGroup(rawCode, ownerOnly = false, onComplete = { error ->
            if (error == null) onSuccess() else onError(error)
        }) { group, userId ->
            if (!group.hasChild("members/$userId")) return@mutateGroup "member_not_found"
            val juz = group.child("juz/$juzNumber")
            val assignedUser = juz.child("userId").value as? String ?: ""
            if (assignedUser.isNotBlank() && assignedUser != userId) {
                return@mutateGroup "assigned_to_other"
            }
            when (action) {
                QuranSharedJuzAction.CLAIM -> {
                    juz.value = juzValue(
                        number = juzNumber,
                        userId = userId,
                        displayName = cleanDisplayName(displayName),
                        completed = juz.child("completed").value as? Boolean ?: false
                    )
                }
                QuranSharedJuzAction.TOGGLE_COMPLETED -> {
                    if (assignedUser != userId) return@mutateGroup "claim_required"
                    juz.value = juzValue(
                        number = juzNumber,
                        userId = userId,
                        displayName = cleanDisplayName(displayName),
                        completed = !(juz.child("completed").value as? Boolean ?: false)
                    )
                }
                QuranSharedJuzAction.RELEASE -> {
                    if (assignedUser != userId) return@mutateGroup "claim_required"
                    juz.value = null
                }
            }
            null
        }
    }

    private fun ensureUser(onReady: (String) -> Unit, onError: (String) -> Unit) {
        val firebaseAuth = auth
        if (firebaseAuth == null) {
            onError("unavailable")
            return
        }
        firebaseAuth.currentUser?.uid?.let { userId ->
            onReady(userId)
            return
        }
        firebaseAuth.signInAnonymously().addOnCompleteListener { task ->
            val userId = firebaseAuth.currentUser?.uid
            if (task.isSuccessful && userId != null) onReady(userId)
            else onError(task.exception?.localizedMessage ?: "unavailable")
        }
    }

    private fun groupReference(code: String): DatabaseReference? {
        val normalizedCode = normalizeSharedKhatamCode(code)
        if (normalizedCode.length != CODE_LENGTH) return null
        return database?.reference?.child(ROOT)?.child(normalizedCode)
    }

    private fun newInviteCode(): String = buildString(CODE_LENGTH) {
        repeat(CODE_LENGTH) { append(CODE_ALPHABET[random.nextInt(CODE_ALPHABET.length)]) }
    }

    private fun juzValue(
        number: Int,
        userId: String,
        displayName: String,
        completed: Boolean
    ): Map<String, Any> = mapOf(
        "number" to number,
        "userId" to userId,
        "displayName" to displayName,
        "completed" to completed,
        "updatedAt" to System.currentTimeMillis()
    )
}

internal fun normalizeSharedKhatamCode(value: String): String =
    value.uppercase().filter { it.isLetterOrDigit() }.take(8)

private fun cleanDisplayName(value: String): String = value.trim().replace(Regex("\\s+"), " ").take(40)

private fun DataSnapshot.toSharedKhatam(code: String): QuranSharedKhatamSnapshot? {
    val meta = child("meta")
    if (!meta.exists()) return null
    val members = child("members").children.mapNotNull { member ->
        val userId = member.key ?: return@mapNotNull null
        QuranSharedMember(
            userId = userId,
            displayName = member.child("displayName").getValue(String::class.java) ?: "Member",
            joinedAt = member.child("joinedAt").longValue()
        )
    }.associateBy { it.userId }
    val juz = (1..SHARED_KHATAM_JUZ_COUNT).map { number ->
        val item = child("juz").child(number.toString())
        QuranSharedJuz(
            number = number,
            userId = item.child("userId").getValue(String::class.java).orEmpty(),
            displayName = item.child("displayName").getValue(String::class.java).orEmpty(),
            completed = item.child("completed").getValue(Boolean::class.java) ?: false,
            updatedAt = item.child("updatedAt").longValue()
        )
    }
    val createdAt = meta.child("createdAt").longValue()
    return QuranSharedKhatamSnapshot(
        code = code,
        title = meta.child("title").getValue(String::class.java)?.ifBlank { "Quran Khatam" } ?: "Quran Khatam",
        ownerId = meta.child("ownerId").getValue(String::class.java).orEmpty(),
        createdAt = createdAt,
        targetDays = meta.child("targetDays").intValue()?.coerceIn(1, 365) ?: 30,
        targetStartedAt = meta.child("targetStartedAt").longValue().takeIf { it > 0L } ?: createdAt,
        members = members,
        juz = juz
    )
}

private fun DataSnapshot.longValue(): Long = when (val value = value) {
    is Number -> value.toLong()
    is String -> value.toLongOrNull() ?: 0L
    else -> 0L
}

private fun DataSnapshot.intValue(): Int? = when (val value = value) {
    is Number -> value.toInt()
    is String -> value.toIntOrNull()
    else -> null
}
