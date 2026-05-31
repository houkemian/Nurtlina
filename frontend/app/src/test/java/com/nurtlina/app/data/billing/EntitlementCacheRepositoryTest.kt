package com.nurtlina.app.data.billing

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class EntitlementCacheRepositoryTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `backend failure can keep cached pro grace period`() = runTest {
        val repository = EntitlementCacheRepository(
            PreferenceDataStoreFactory.create(
                scope = TestScope(UnconfinedTestDispatcher(testScheduler) + Job()),
                produceFile = { File(temporaryFolder.root, "entitlement.preferences_pb") },
            ),
        )
        val graceUntil = Instant.parse("2024-01-04T00:00:00Z")

        repository.save(
            EntitlementCache(
                isPro = true,
                plan = "YEARLY",
                status = "TEMPORARY",
                expiresAt = null,
                lastVerifiedAt = null,
                gracePeriodUntil = graceUntil,
                source = "GOOGLE_PLAY_LOCAL",
            ),
        )

        val cached = repository.get()
        assertTrue(cached.isPro)
        assertEquals("YEARLY", cached.plan)
        assertEquals(graceUntil, cached.gracePeriodUntil)
    }
}
