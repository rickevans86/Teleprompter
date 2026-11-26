package dev.telepromptor

import dev.telepromptor.update.UpdateActions
import dev.telepromptor.update.UpdateFlow
import dev.telepromptor.update.UpdateResult
import dev.telepromptor.update.UpdateState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

private class FakeActions(
    var result: UpdateResult = UpdateResult.UpToDate,
    var canInstall: Boolean = true,
    var apk: File? = File("update.apk"),
    var installOpens: Boolean = true,
) : UpdateActions {
    var checks = 0
    var downloads = 0
    var installs = 0
    var permissionScreens = 0

    override suspend fun check(): UpdateResult {
        checks++
        return result
    }

    override fun canInstall() = canInstall

    override fun openInstallPermissionSettings() {
        permissionScreens++
    }

    override suspend fun download(
        tag: String,
        url: String,
    ): File? {
        downloads++
        return apk
    }

    override fun install(apk: File): Boolean {
        installs++
        return installOpens
    }
}

class UpdateFlowTest {
    private val found = UpdateResult.Available("v1.1.9", "https://example.com/a.apk")
    private val available = UpdateState.Available("v1.1.9", "https://example.com/a.apk")

    private fun run(
        actions: FakeActions,
        state: UpdateState,
        progress: MutableList<UpdateState> = mutableListOf(),
    ) = runBlocking { UpdateFlow(actions).tap(state) { progress += it } }

    @Test fun startupCheckOnlyReportsANewRelease() =
        runBlocking {
            assertEquals(available, UpdateFlow(FakeActions(found)).startupCheck())
            assertEquals(UpdateState.Idle, UpdateFlow(FakeActions(UpdateResult.UpToDate)).startupCheck())
            assertEquals(UpdateState.Idle, UpdateFlow(FakeActions(UpdateResult.Failed("offline"))).startupCheck())
        }

    @Test fun firstTapChecksAndReportsTheResult() {
        val progress = mutableListOf<UpdateState>()
        assertEquals(available, run(FakeActions(found), UpdateState.Idle, progress))
        assertEquals(listOf<UpdateState>(UpdateState.Checking), progress)
        assertEquals(UpdateState.UpToDate, run(FakeActions(UpdateResult.UpToDate), UpdateState.Idle))
        assertEquals(UpdateState.Failed("offline"), run(FakeActions(UpdateResult.Failed("offline")), UpdateState.UpToDate))
        assertEquals(UpdateState.Available("v1.1.9", "https://example.com/a.apk"), run(FakeActions(found), UpdateState.Failed("x")))
    }

    @Test fun missingInstallPermissionOpensSettingsAndWaits() {
        val actions = FakeActions(canInstall = false)
        val result = run(actions, available)
        assertEquals(UpdateState.NeedPermission("v1.1.9", "https://example.com/a.apk"), result)
        assertEquals(1, actions.permissionScreens)
        assertEquals(0, actions.downloads)
    }

    @Test fun downloadsAndInstallsOncePermissionIsGranted() {
        val actions = FakeActions()
        val progress = mutableListOf<UpdateState>()
        val result = run(actions, UpdateState.NeedPermission("v1.1.9", "https://example.com/a.apk"), progress)
        assertEquals(UpdateState.Installing("v1.1.9", "https://example.com/a.apk"), result)
        assertEquals(listOf<UpdateState>(UpdateState.Downloading), progress)
        assertEquals(1, actions.downloads)
        assertEquals(1, actions.installs)
        assertEquals(0, actions.permissionScreens)
    }

    @Test fun reportsDownloadAndInstallerFailures() {
        assertEquals(UpdateState.Failed("Download failed"), run(FakeActions(apk = null), available))
        val noInstaller = run(FakeActions(installOpens = false), available)
        assertEquals(UpdateState.Failed("Could not open the Android installer"), noInstaller)
    }

    @Test fun tappingInstallingRetriesTheInstall() {
        val actions = FakeActions()
        val result = run(actions, UpdateState.Installing("v1.1.9", "https://example.com/a.apk"))
        assertEquals(UpdateState.Installing("v1.1.9", "https://example.com/a.apk"), result)
        assertEquals(1, actions.installs)
    }

    @Test fun tapsWhileBusyDoNothing() {
        val actions = FakeActions(found)
        assertEquals(UpdateState.Checking, run(actions, UpdateState.Checking))
        assertEquals(UpdateState.Downloading, run(actions, UpdateState.Downloading))
        assertEquals(0, actions.checks + actions.downloads + actions.installs)
    }
}
