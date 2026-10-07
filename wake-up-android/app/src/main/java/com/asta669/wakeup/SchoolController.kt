package com.asta669.wakeup

/** Android-independent state machine. The journal is persisted before each system change. */
internal enum class SchoolSetting { MEDIA, RING, NOTIFICATIONS, RINGER, DND }

internal data class SchoolSnapshot(
    val values: Map<SchoolSetting, Int>,
    val pending: Set<SchoolSetting> = emptySet(),
    val restoring: Boolean = false
)

internal interface SchoolStateStore {
    fun read(): SchoolSnapshot?
    fun write(snapshot: SchoolSnapshot): Boolean
    fun clear(): Boolean
}

internal interface SchoolSystem {
    fun read(setting: SchoolSetting): Int
    fun set(setting: SchoolSetting, value: Int)
    fun canChangeDnd(): Boolean
}

internal data class SchoolChangeResult(val problems: List<String>)

internal class SchoolController(private val store: SchoolStateStore, private val system: SchoolSystem) {
    companion object {
        // Android AudioManager.RINGER_MODE_SILENT and NotificationManager.INTERRUPTION_FILTER_ALARMS.
        const val SILENT = 0
        const val ALARMS_ONLY = 4
    }

    fun enable(): SchoolChangeResult {
        var snapshot = store.read()
        if (snapshot?.restoring == true) {
            return SchoolChangeResult(listOf("Terminez d’abord la restauration avec School OFF."))
        }
        if (snapshot == null) {
            snapshot = SchoolSnapshot(SchoolSetting.entries.associateWith(system::read))
            if (!store.write(snapshot)) return storageFailure()
        }
        val problems = mutableListOf<String>()
        // DND must be applied last: some devices also change it when the ringer changes.
        for (setting in SchoolSetting.entries) {
            if (setting == SchoolSetting.DND && !system.canChangeDnd()) {
                problems += "Accès « Ne pas déranger » manquant : filtrage des appels non appliqué."
                continue
            }
            val target = if (setting == SchoolSetting.DND) ALARMS_ONLY else SILENT
            if (system.read(setting) == target) continue
            // Ring/ringer changes can alter DND as a side effect. Include it in the restore journal.
            val affected = if (setting in setOf(SchoolSetting.RING, SchoolSetting.NOTIFICATIONS, SchoolSetting.RINGER)) {
                setOf(SchoolSetting.RING, SchoolSetting.NOTIFICATIONS, SchoolSetting.RINGER, SchoolSetting.DND)
            } else setOf(setting)
            snapshot = snapshot!!.copy(pending = snapshot.pending + affected)
            if (!store.write(snapshot)) {
                problems += "Enregistrement impossible : aucune autre modification appliquée."
                break
            }
            try {
                system.set(setting, target)
                if (system.read(setting) != target) problems += "${label(setting)} : réglage refusé par le téléphone."
            } catch (_: SecurityException) {
                problems += "${label(setting)} : autorisation système requise."
            } catch (_: IllegalArgumentException) {
                problems += "${label(setting)} : réglage indisponible sur ce téléphone."
            }
        }
        return SchoolChangeResult(problems)
    }

    fun disable(): SchoolChangeResult {
        var snapshot = store.read() ?: return SchoolChangeResult(emptyList())
        snapshot = snapshot.copy(restoring = true)
        if (!store.write(snapshot)) return storageFailure()
        val problems = mutableListOf<String>()
        // Unmute first so stream readings reflect their real level. Reapply the saved ringer
        // after restoring linked streams. DND last avoids ringer side effects overriding it.
        val order = listOf(SchoolSetting.RINGER, SchoolSetting.MEDIA, SchoolSetting.RING,
            SchoolSetting.NOTIFICATIONS, SchoolSetting.RINGER, SchoolSetting.DND)
        val failures = mutableMapOf<SchoolSetting, String>()
        for (setting in order) {
            if (setting !in snapshot.pending) continue
            val original = snapshot.values.getValue(setting)
            try {
                if (system.read(setting) != original) {
                    if (setting == SchoolSetting.DND && !system.canChangeDnd()) throw SecurityException()
                    system.set(setting, original)
                }
                if (system.read(setting) != original) {
                    failures[setting] = "${label(setting)} : restauration à terminer."
                    continue
                }
            } catch (_: SecurityException) {
                failures[setting] = "${label(setting)} : autorisez « Ne pas déranger », puis réessayez School OFF."
            } catch (_: IllegalArgumentException) {
                failures[setting] = "${label(setting)} : restauration indisponible."
            }
        }
        val remaining = snapshot.pending.filter { system.read(it) != snapshot.values.getValue(it) }.toSet()
        problems += remaining.map { failures[it] ?: "${label(it)} : restauration à terminer." }
        snapshot = snapshot.copy(pending = remaining)
        if (!store.write(snapshot)) return storageFailure()
        if (snapshot.pending.isEmpty() && !store.clear()) return storageFailure()
        return SchoolChangeResult(problems)
    }

    private fun storageFailure() = SchoolChangeResult(listOf("Impossible de sauvegarder les réglages. Réessayez."))

    private fun label(setting: SchoolSetting) = when (setting) {
        SchoolSetting.MEDIA -> "Médias"
        SchoolSetting.RING -> "Sonnerie"
        SchoolSetting.NOTIFICATIONS -> "Notifications"
        SchoolSetting.RINGER -> "Mode silencieux"
        SchoolSetting.DND -> "Ne pas déranger"
    }
}
