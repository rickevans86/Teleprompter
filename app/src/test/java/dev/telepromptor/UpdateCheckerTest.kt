package dev.telepromptor

import dev.telepromptor.update.UpdateResult
import dev.telepromptor.update.parseRelease
import dev.telepromptor.update.releaseVersionCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateCheckerTest {
    private val apkUrl = "https://github.com/rickevans86/Teleprompter/releases/download/v1.1.12/Telepromptor.apk"

    private fun release(
        tag: String,
        vararg assets: Pair<String, String>,
    ): String {
        val list = assets.joinToString(",") { """{"name":"${it.first}","browser_download_url":"${it.second}","size":1}""" }
        return """{"tag_name":"$tag","name":"x","draft":false,"assets":[$list]}"""
    }

    @Test fun readsTheVersionCodeFromTheTagSuffix() {
        assertEquals(12, releaseVersionCode("v1.1.12"))
        assertEquals(7, releaseVersionCode(" v1.0.7 "))
        assertEquals(0, releaseVersionCode("v1.1.0"))
    }

    @Test fun rejectsTagsWithoutANumberSuffix() {
        assertNull(releaseVersionCode("latest"))
        assertNull(releaseVersionCode("v1.1."))
        assertNull(releaseVersionCode(""))
    }

    @Test fun offersAReleaseWithAHigherVersionCode() {
        val result = parseRelease(release("v1.1.12", "Telepromptor.apk" to apkUrl), installedCode = 11)
        assertEquals(UpdateResult.Available("v1.1.12", apkUrl), result)
    }

    @Test fun sameOrOlderReleaseIsUpToDate() {
        assertEquals(UpdateResult.UpToDate, parseRelease(release("v1.1.12", "a.apk" to apkUrl), installedCode = 12))
        assertEquals(UpdateResult.UpToDate, parseRelease(release("v1.1.12", "a.apk" to apkUrl), installedCode = 40))
        assertEquals(UpdateResult.UpToDate, parseRelease(release("v1.1.0", "a.apk" to apkUrl), installedCode = 1))
    }

    @Test fun usesTheFirstApkAssetAndIgnoresOthers() {
        val result = parseRelease(release("v1.1.5", "notes.txt" to "https://example.com/n", "Telepromptor.apk" to apkUrl), installedCode = 1)
        assertEquals(UpdateResult.Available("v1.1.5", apkUrl), result)
    }

    @Test fun failsWhenThereIsNoUsableApk() {
        assertTrue(parseRelease(release("v1.1.5"), installedCode = 1) is UpdateResult.Failed)
        assertTrue(parseRelease(release("v1.1.5", "Telepromptor.apk" to "http://example.com/a.apk"), installedCode = 1) is UpdateResult.Failed)
    }

    @Test fun failsOnBadTagOrBadJson() {
        assertTrue(parseRelease(release("nightly", "a.apk" to apkUrl), installedCode = 1) is UpdateResult.Failed)
        assertTrue(parseRelease("not json", installedCode = 1) is UpdateResult.Failed)
        assertTrue(parseRelease("", installedCode = 1) is UpdateResult.Failed)
    }
}
