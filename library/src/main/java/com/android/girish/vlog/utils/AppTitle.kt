/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 Girish Budhwani
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.android.girish.vlog.utils

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.pm.PackageInfoCompat

/**
 * The name of the app using Vlog, as the launcher shows it.
 */
internal fun Context.getAppName(): String = applicationInfo.loadLabel(packageManager).toString()

/**
 * The title for the app using Vlog, see [formatAppTitle]. Falls back to the name alone when the package
 * info is not available, so the title is never empty.
 *
 * @param showAppName false leaves the name out, for apps with a name too long for the title
 */
internal fun Context.getAppTitle(showAppName: Boolean = true): String {
    val name = getAppName()
    val info = getPackageInfo() ?: return name

    return formatAppTitle(
        name = if (showAppName) name else null,
        versionName = info.versionName,
        versionCode = PackageInfoCompat.getLongVersionCode(info),
    )
}

/**
 * Joins the parts of the title, for example `My App v1.2.0 (42)`. A `null` name is left out, as is an empty
 * version name, the version code is always there.
 */
internal fun formatAppTitle(
    name: String?,
    versionName: String?,
    versionCode: Long,
): String {
    val parts = mutableListOf<String>()
    if (!name.isNullOrEmpty()) parts.add(name)
    if (!versionName.isNullOrEmpty()) parts.add("v$versionName")
    parts.add("($versionCode)")
    return parts.joinToString(" ")
}

private fun Context.getPackageInfo(): PackageInfo? =
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(packageName, 0)
        }
    } catch (e: PackageManager.NameNotFoundException) {
        null
    }
