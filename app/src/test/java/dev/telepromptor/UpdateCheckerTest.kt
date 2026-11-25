package dev.telepromptor

import dev.telepromptor.update.isNewerVersion
import dev.telepromptor.update.parseVersion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateCheckerTest {
    @Test fun parsesTagsWithAndWithoutPrefix() {
        assertEquals(listOf(1, 2, 0), parseVersion("v1.2.0"))
        assertEquals(listOf(1, 2, 0), parseVersion(" 1.2.0 "))
        assertEquals(listOf(2, 0), parseVersion("v2.0-beta1"))
    }

    @Test fun rejectsTagsThatAreNotVersions() {
        assertNull(parseVersion(""))
        assertNull(parseVersion("latest"))
        assertNull(parseVersion("v1.x.0"))
    }

    @Test fun detectsOnlyStrictlyNewerVersions() {
        assertTrue(isNewerVersion("1.1.0", "v1.1.1"))
        assertTrue(isNewerVersion("1.1.0", "v1.2.0"))
        assertTrue(isNewerVersion("1.9.0", "v1.10.0"))
        assertTrue(isNewerVersion("1.1.0", "v2"))
        assertFalse(isNewerVersion("1.1.0", "v1.1.0"))
        assertFalse(isNewerVersion("1.1.0", "v1.1"))
        assertFalse(isNewerVersion("1.2.0", "v1.1.9"))
    }

    @Test fun ignoresInvalidVersionsInEitherPosition() {
        assertFalse(isNewerVersion("1.1.0", "nightly"))
        assertFalse(isNewerVersion("dev", "v9.9.9"))
        assertFalse(isNewerVersion("1.1.0", ""))
    }
}
