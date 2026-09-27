package com.example

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Robolectric
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class MainActivityTest {
    @Test
    fun testActivityLaunches() {
        Robolectric.buildActivity(MainActivity::class.java).create().start().resume().visible()
    }
}
