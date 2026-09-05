package com.urunkarpm.pingpin.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

enum class ChangeCategory(
    val label: String,
    val icon: ImageVector,
    val lightColor: Color,
    val darkColor: Color
) {
    FIXED("Fixed & Hardened", Icons.Outlined.Build, Color(0xFFDC2626), Color(0xFFF87171)),
    ADDED("Added & Enhanced", Icons.Outlined.AutoAwesome, Color(0xFF059669), Color(0xFF34D399)),
    CHANGED("Changed & Refined", Icons.Outlined.Tune, Color(0xFF2563EB), Color(0xFF60A5FA)),
    PERFORMANCE("Performance", Icons.Outlined.Speed, Color(0xFFD97706), Color(0xFFFBBF24)),
    UI_UX("UI & UX Design", Icons.Outlined.Palette, Color(0xFF7C3AED), Color(0xFFA78BFA)),
    GENERAL("General Updates", Icons.Outlined.CheckCircleOutline, Color(0xFF475569), Color(0xFF94A3B8));

    companion object {
        fun fromHeader(header: String): ChangeCategory {
            val lower = header.lowercase()
            return when {
                lower.contains("fix") || lower.contains("harden") || lower.contains("bug") || lower.contains("resolved") || lower.contains("crash") -> FIXED
                lower.contains("add") || lower.contains("feature") || lower.contains("new") || lower.contains("support") -> ADDED
                lower.contains("change") || lower.contains("refine") || lower.contains("update") -> CHANGED
                lower.contains("perf") || lower.contains("speed") || lower.contains("optimi") || lower.contains("doze") || lower.contains("battery") -> PERFORMANCE
                lower.contains("ui") || lower.contains("ux") || lower.contains("style") || lower.contains("color") || lower.contains("theme") || lower.contains("calendar") || lower.contains("visual") -> UI_UX
                else -> GENERAL
            }
        }
    }
}

data class ChangelogEntry(
    val title: String,
    val description: String,
    val category: ChangeCategory
)

data class ReleaseVersion(
    val versionName: String,
    val isCurrent: Boolean = false,
    val releaseTag: String? = null,
    val entries: List<ChangelogEntry> = emptyList()
)

object ChangelogParser {
    fun parse(rawMarkdown: String, currentAppVersion: String = "2.4.3"): List<ReleaseVersion> {
        val lines = rawMarkdown.lines()
        val versions = mutableListOf<ReleaseVersion>()

        var currentVersionName = ""
        var currentReleaseTag: String? = null
        var isCurrentRelease = false
        var currentCategory = ChangeCategory.GENERAL
        val currentEntries = mutableListOf<ChangelogEntry>()

        fun finalizeCurrentVersion() {
            if (currentVersionName.isNotBlank() || currentEntries.isNotEmpty()) {
                val vName = currentVersionName.ifBlank { currentAppVersion }
                val isCurrent = isCurrentRelease || vName.contains(currentAppVersion) || (versions.isEmpty() && !rawMarkdown.contains("## ["))
                versions.add(
                    ReleaseVersion(
                        versionName = vName,
                        isCurrent = isCurrent,
                        releaseTag = currentReleaseTag ?: if (isCurrent) "Current Release" else null,
                        entries = currentEntries.toList()
                    )
                )
                currentEntries.clear()
            }
        }

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isBlank() || trimmed == "---") continue

            when {
                // Version Header: ## [2.4.3] - Current Release or ## [2.4.2]
                trimmed.startsWith("## ") -> {
                    finalizeCurrentVersion()
                    val headerBody = trimmed.removePrefix("## ").trim()
                    val versionMatch = Regex("""\[?([0-9]+\.[0-9]+\.[0-9]+)\]?""").find(headerBody)
                    currentVersionName = versionMatch?.groupValues?.get(1) ?: headerBody
                    isCurrentRelease = headerBody.lowercase().contains("current")
                    val tagMatch = headerBody.split("-").getOrNull(1)?.trim()
                    currentReleaseTag = if (!tagMatch.isNullOrBlank()) tagMatch else if (isCurrentRelease) "Current Release" else null
                    currentCategory = ChangeCategory.GENERAL
                }
                // Category Header: ### Fixed & Hardened
                trimmed.startsWith("### ") -> {
                    val categoryText = trimmed.removePrefix("### ").trim()
                    currentCategory = ChangeCategory.fromHeader(categoryText)
                }
                // Item bullet: - **Title**: Description or - Title: Description or - Description
                trimmed.startsWith("-") || trimmed.startsWith("*") -> {
                    val bulletContent = trimmed.substring(1).trim()
                    val boldMatch = Regex("""^\*\*(.*?)\*\*:\s*(.*)$""").find(bulletContent)
                    if (boldMatch != null) {
                        val title = boldMatch.groupValues[1].trim()
                        val desc = boldMatch.groupValues[2].trim()
                        val cat = if (currentCategory == ChangeCategory.GENERAL) ChangeCategory.fromHeader(title) else currentCategory
                        currentEntries.add(ChangelogEntry(title = title, description = desc, category = cat))
                    } else {
                        val colonIndex = bulletContent.indexOf(":")
                        if (colonIndex > 0 && colonIndex < 40 && !bulletContent.startsWith("http")) {
                            val title = bulletContent.substring(0, colonIndex).replace("**", "").trim()
                            val desc = bulletContent.substring(colonIndex + 1).replace("**", "").trim()
                            val cat = if (currentCategory == ChangeCategory.GENERAL) ChangeCategory.fromHeader(title) else currentCategory
                            currentEntries.add(ChangelogEntry(title = title, description = desc, category = cat))
                        } else {
                            val clean = bulletContent.replace("**", "").trim()
                            currentEntries.add(ChangelogEntry(title = "Improvement", description = clean, category = currentCategory))
                        }
                    }
                }
                else -> {
                    if (trimmed.isNotBlank() && !trimmed.startsWith("#")) {
                        val clean = trimmed.replace("**", "").trim()
                        currentEntries.add(ChangelogEntry(title = "Note", description = clean, category = currentCategory))
                    }
                }
            }
        }
        finalizeCurrentVersion()

        return if (versions.isEmpty()) {
            listOf(
                ReleaseVersion(
                    versionName = currentAppVersion,
                    isCurrent = true,
                    releaseTag = "Current Release",
                    entries = listOf(ChangelogEntry("Update Note", rawMarkdown.replace("**", "").trim(), ChangeCategory.GENERAL))
                )
            )
        } else {
            versions
        }
    }
}
