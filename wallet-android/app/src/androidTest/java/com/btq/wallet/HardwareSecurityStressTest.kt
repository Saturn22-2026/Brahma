package com.btq.wallet

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class HardwareSecurityStressTest {

    private lateinit var hardwareManager: HardwareSecurityManager

    @Before
    fun setup() {
        hardwareManager = HardwareSecurityManager()
    }

    @Test
    fun stressTestHardwareSigning() {
        val iterations = 20
        val alias = "stress_test_key_${UUID.randomUUID()}"
        val data = "Quantum Sovereign Stress Test".toByteArray()
        
        try {
            for (i in 1..iterations) {
                val signature = hardwareManager.signWithHardware(alias, data)
                assertNotNull("Hardware signature should not be null at iteration $i", signature)
                assertTrue("Hardware signature should not be empty at iteration $i", signature.isNotEmpty())
            }
        } finally {
            hardwareManager.wipeHardwareKeys(alias)
        }
    }

    @Test
    fun stressTestKeyLifecycle() {
        val iterations = 10
        for (i in 1..iterations) {
            val alias = "lifecycle_key_$i"
            hardwareManager.ensureHardwareKey(alias)
            val data = "Test Data".toByteArray()
            val sig = hardwareManager.signWithHardware(alias, data)
            assertNotNull(sig)
            hardwareManager.wipeHardwareKeys(alias)
        }
    }
}
