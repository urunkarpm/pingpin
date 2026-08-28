package com.urunkarpm.pingpin.ui.update

import android.app.Application
import android.content.Context
import com.urunkarpm.pingpin.service.UpdateInfo
import com.urunkarpm.pingpin.service.UpdateState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class AppUpdateViewModelTest {

    private lateinit var mockApplication: Application
    private lateinit var mockContext: Context
    private lateinit var viewModel: AppUpdateViewModel

    @Before
    fun setUp() {
        mockApplication = mock(Application::class.java)
        mockContext = mock(Context::class.java)
        `when`(mockApplication.applicationContext).thenReturn(mockContext)
        `when`(mockContext.packageName).thenReturn("com.urunkarpm.pingpin")

        viewModel = AppUpdateViewModel(mockApplication)
    }

    @Test
    fun testInitialStateIsIdleAndDialogHidden() {
        assertEquals(UpdateState.Idle, viewModel.updateState.value)
        assertFalse(viewModel.showDialog.value)
    }

    @Test
    fun testOpenAndDismissDialog() {
        viewModel.openDialog()
        assertTrue(viewModel.showDialog.value)

        viewModel.dismissDialog()
        assertFalse(viewModel.showDialog.value)
    }

    @Test
    fun testVersionNameFallback() {
        val version = viewModel.getCurrentVersionName()
        assertEquals("2.1.0", version)
    }
}
