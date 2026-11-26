package dev.telepromptor.update

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject

class ApkInstaller
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) {
        fun canInstall(): Boolean = context.packageManager.canRequestPackageInstalls()

        /** Opens the system screen where the user allows this app to install APKs. */
        fun openInstallPermissionSettings() {
            val intent =
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                context.startActivity(intent)
            } catch (_: ActivityNotFoundException) {
                // The user can still allow installs from the app's system settings page.
            }
        }

        /** Hands the APK to the system installer. Returns false if no installer could be opened. */
        fun install(apk: File): Boolean {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apk)
            val intent =
                Intent(Intent.ACTION_VIEW)
                    .setDataAndType(uri, "application/vnd.android.package-archive")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            return try {
                context.startActivity(intent)
                true
            } catch (_: ActivityNotFoundException) {
                false
            }
        }
    }
