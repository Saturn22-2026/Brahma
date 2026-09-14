package com.btq.wallet

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.btq.wallet.viewmodel.WalletViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class ViewModelStressTest {

    private lateinit var viewModel: WalletViewModel

    @Before
    fun setup() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        viewModel = WalletViewModel(app)
    }

    @Test
    fun stressTestWalletCreation() = runTest {
        val iterations = 20
        for (i in 1..iterations) {
            viewModel.createWallet(is24Words = i % 2 == 0, isHardware = false)
            val address = viewModel.address.value
            assertNotNull("Address should not be null at iteration $i", address)
            assertTrue("Address should not be empty at iteration $i", address.isNotEmpty())
        }
    }

    @Test
    fun stressTestStateFlowUpdates() = runTest {
        val iterations = 50
        for (i in 1..iterations) {
            val mockReferrer = "btq_referrer_$i"
            viewModel.setReferrer(mockReferrer)
            assertEquals("Referrer address mismatch at iteration $i", mockReferrer, viewModel.referrerAddress.value)
        }
    }

    @Test
    fun stressTestWipeWallet() = runTest {
        val iterations = 10
        for (i in 1..iterations) {
            viewModel.createWallet(true, false)
            viewModel.wipeWallet()
            assertTrue("Address should be empty after wipe", viewModel.address.value.isEmpty())
            assertEquals("Status should be Wallet Wiped", "Wallet Wiped", viewModel.status.value)
        }
    }
}
