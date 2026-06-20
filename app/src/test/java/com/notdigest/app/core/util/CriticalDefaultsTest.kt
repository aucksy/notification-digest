package com.notdigest.app.core.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CriticalDefaultsTest {

    @Test
    fun `messaging apps stay real-time by default`() {
        assertThat(CriticalDefaults.isCritical("com.google.android.apps.messaging", "Messages")).isTrue()
    }

    @Test
    fun `authenticators stay real-time by default`() {
        assertThat(CriticalDefaults.isCritical("com.azure.authenticator", "Authenticator")).isTrue()
    }

    @Test
    fun `otp keyword in package is treated as critical`() {
        assertThat(CriticalDefaults.isCritical("com.example.otp", "Example OTP")).isTrue()
    }

    @Test
    fun `social apps are NOT assumed critical`() {
        assertThat(CriticalDefaults.isCritical("com.instagram.android", "Instagram")).isFalse()
    }

    @Test
    fun `banking apps are NOT assumed critical - the user decides`() {
        assertThat(CriticalDefaults.isCritical("com.mybank.app", "My Bank")).isFalse()
    }
}
