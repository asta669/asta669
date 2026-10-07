package com.asta669.wakeup

import org.junit.Assert.*
import org.junit.Test

class SchoolControllerTest {
    private class MemoryStore : SchoolStateStore {
        var snapshot: SchoolSnapshot? = null
        var writable = true
        override fun read() = snapshot
        override fun write(snapshot: SchoolSnapshot): Boolean {
            if (!writable) return false
            this.snapshot = snapshot.copy(values = snapshot.values.toMap(), pending = snapshot.pending.toSet())
            return true
        }
        override fun clear(): Boolean {
            if (!writable) return false
            snapshot = null
            return true
        }
    }

    private class FakeSystem : SchoolSystem {
        val values = mutableMapOf(
            SchoolSetting.MEDIA to 8, SchoolSetting.RING to 5, SchoolSetting.NOTIFICATIONS to 5,
            SchoolSetting.RINGER to 2, SchoolSetting.DND to 1
        )
        var access = true
        val denied = mutableSetOf<SchoolSetting>()
        val ignored = mutableSetOf<SchoolSetting>()
        var linkedRing = false
        val writes = mutableListOf<Pair<SchoolSetting, Int>>()
        override fun read(setting: SchoolSetting) = values.getValue(setting)
        override fun canChangeDnd() = access
        override fun set(setting: SchoolSetting, value: Int) {
            if (setting in denied || (setting == SchoolSetting.DND && !access)) throw SecurityException()
            writes += setting to value
            if (setting in ignored) return
            values[setting] = value
            if (linkedRing && setting == SchoolSetting.RING) {
                values[SchoolSetting.NOTIFICATIONS] = value
                values[SchoolSetting.RINGER] = if (value == 0) 0 else 2
                values[SchoolSetting.DND] = if (value == 0) 3 else 1
            }
        }
    }

    @Test fun offRestoresOriginalValuesAfterControllerRecreation() {
        val store = MemoryStore()
        val system = FakeSystem()
        val before = system.values.toMap()
        assertTrue(SchoolController(store, system).enable().problems.isEmpty())
        assertEquals(0, system.values[SchoolSetting.MEDIA])
        assertEquals(4, system.values[SchoolSetting.DND])
        assertTrue(SchoolController(store, system).disable().problems.isEmpty())
        assertEquals(before, system.values)
        assertNull(store.snapshot)
    }

    @Test fun repeatedOnDoesNotOverwriteOriginalSnapshot() {
        val store = MemoryStore()
        val system = FakeSystem()
        val before = system.values.toMap()
        val controller = SchoolController(store, system)
        controller.enable()
        system.values[SchoolSetting.MEDIA] = 3
        controller.enable()
        assertEquals(before, store.snapshot!!.values)
        assertTrue(controller.disable().problems.isEmpty())
        assertEquals(before, system.values)
    }

    @Test fun missingDndAccessProducesPartialResultAndCanRestoreOtherVolumes() {
        val store = MemoryStore()
        val system = FakeSystem().apply {
            access = false
            denied += setOf(SchoolSetting.RING, SchoolSetting.RINGER)
        }
        val original = system.values.toMap()
        val controller = SchoolController(store, system)
        assertFalse(controller.enable().problems.isEmpty())
        assertEquals(0, system.values[SchoolSetting.MEDIA])
        assertEquals(1, system.values[SchoolSetting.DND])
        assertTrue(controller.disable().problems.isEmpty())
        assertEquals(original, system.values)
    }

    @Test fun revokedPermissionKeepsSnapshotUntilRestorationCanFinish() {
        val store = MemoryStore()
        val system = FakeSystem()
        val original = system.values.toMap()
        val controller = SchoolController(store, system)
        controller.enable()
        system.access = false
        assertFalse(controller.disable().problems.isEmpty())
        assertTrue(store.snapshot!!.restoring)
        assertEquals(setOf(SchoolSetting.DND), store.snapshot!!.pending)
        assertFalse(controller.enable().problems.isEmpty())
        system.access = true
        assertTrue(SchoolController(store, system).disable().problems.isEmpty())
        assertEquals(original, system.values)
        assertNull(store.snapshot)
    }

    @Test fun journalTracksRingerAndDndSideEffectsOfLinkedVolumeStreams() {
        val store = MemoryStore()
        val system = FakeSystem().apply { linkedRing = true }
        val original = system.values.toMap()
        val controller = SchoolController(store, system)
        assertTrue(controller.enable().problems.isEmpty())
        assertEquals(4, system.values[SchoolSetting.DND])
        assertTrue(controller.disable().problems.isEmpty())
        assertEquals(original, system.values)
    }

    @Test fun refusedSystemChangeIsNotReportedAsSuccess() {
        val store = MemoryStore()
        val system = FakeSystem().apply { ignored += SchoolSetting.MEDIA }
        val result = SchoolController(store, system).enable()
        assertTrue(result.problems.any { it.startsWith("Médias") })
        assertEquals(8, system.values[SchoolSetting.MEDIA])
    }

    @Test fun failureToPersistPreventsAllSystemChanges() {
        val store = MemoryStore().apply { writable = false }
        val system = FakeSystem()
        assertFalse(SchoolController(store, system).enable().problems.isEmpty())
        assertTrue(system.writes.isEmpty())
    }

    @Test fun repeatedOffDoesNothingAfterSuccessfulRestoration() {
        val store = MemoryStore()
        val system = FakeSystem()
        val controller = SchoolController(store, system)
        controller.enable()
        controller.disable()
        val count = system.writes.size
        assertTrue(controller.disable().problems.isEmpty())
        assertEquals(count, system.writes.size)
    }

    @Test fun accessGrantedLaterPreservesInitialDndSettingForRestoration() {
        val store = MemoryStore()
        val system = FakeSystem().apply { access = false; values[SchoolSetting.DND] = 2 }
        val controller = SchoolController(store, system)
        controller.enable()
        system.access = true
        controller.enable()
        assertEquals(4, system.values[SchoolSetting.DND])
        assertTrue(controller.disable().problems.isEmpty())
        assertEquals(2, system.values[SchoolSetting.DND])
    }
}
