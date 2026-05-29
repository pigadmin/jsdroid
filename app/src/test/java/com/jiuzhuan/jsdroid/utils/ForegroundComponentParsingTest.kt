package com.jiuzhuan.jsdroid.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ForegroundComponentParsingTest {

    @Test
    fun extractPackageAndClassCandidates_supportsJoinedShellLines() {
        val raw = "mFocusedApp=ActivityRecord{1a2b3c u0 com.miui.home/.launcher.Launcher t12}@@topResumedActivity=ActivityRecord{4d5e6f u0 com.target.app/.MainActivity t12}"

        val candidates = extractPackageAndClassCandidates(raw)

        assertEquals(2, candidates.size)
        assertEquals("com.miui.home", candidates[0].packageName)
        assertEquals(".MainActivity", candidates[1].className)
    }

    @Test
    fun selectBestPackageAndClassCandidate_prefersResumedAppOverLauncher() {
        val raw = "mFocusedApp=ActivityRecord{1a2b3c u0 com.miui.home/.launcher.Launcher t12}\n" +
            "topResumedActivity=ActivityRecord{4d5e6f u0 com.target.app/.MainActivity t12}"

        val candidate = selectBestPackageAndClassCandidate(
            extractPackageAndClassCandidates(raw)
        )

        assertNotNull(candidate)
        assertEquals("com.target.app", candidate?.packageName)
        assertEquals(".MainActivity", candidate?.className)
    }

    @Test
    fun extractPackageAndClass_keepsCurrentFocusActivityName() {
        val raw = "mCurrentFocus=Window{8e3957 u0 com.target.app/com.target.app.feature.DeepLinkActivity}"

        val component = extractPackageAndClass(raw)

        assertNotNull(component)
        assertEquals("com.target.app", component?.first)
        assertEquals("com.target.app.feature.DeepLinkActivity", component?.second)
    }
}