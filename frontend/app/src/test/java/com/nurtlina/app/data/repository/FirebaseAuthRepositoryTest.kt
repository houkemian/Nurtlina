package com.nurtlina.app.data.repository

import android.app.Activity
import com.nurtlina.app.data.remote.FirebaseAuthSource
import com.nurtlina.app.data.remote.FirestoreSource
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class FirebaseAuthRepositoryTest {

    @Test
    fun `microsoft failure does not launch a fallback web flow`() = runTest {
        val authSource = mockk<FirebaseAuthSource>()
        val firestoreSource = mockk<FirestoreSource>()
        val activity = mockk<Activity>()
        val expected = IllegalStateException("Microsoft web flow failed")
        every { authSource.observeCurrentUser() } returns flowOf(null)
        coEvery { authSource.signInWithMicrosoft(activity) } throws expected
        val repository = FirebaseAuthRepository(authSource, firestoreSource)

        val result = repository.signInWithMicrosoft(activity)

        assertTrue(result.exceptionOrNull() === expected)
        coVerify(exactly = 1) { authSource.signInWithMicrosoft(activity) }
        verify(exactly = 0) { authSource.isSignedIn() }
    }
}
