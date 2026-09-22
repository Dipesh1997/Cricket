package cricket.player.auction.data.remote

object GoogleDriveHelper {

    /**
     * Converts a Google Drive sharing URL or File ID into a direct image loading URL
     * compatible with image loaders like Coil.
     */
    fun toDirectImageUrl(driveUrlOrId: String): String {
        if (driveUrlOrId.isBlank()) return ""

        val fileId = extractFileId(driveUrlOrId)
        return if (fileId != null) {
            "https://lh3.googleusercontent.com/d/$fileId"
        } else {
            // If it's already a direct HTTP image link or unsaved placeholder URL
            driveUrlOrId
        }
    }

    /**
     * Extracts Google Drive file ID from standard Google Drive URLs.
     */
    fun extractFileId(url: String): String? {
        val trimmed = url.trim()
        if (trimmed.isEmpty()) return null

        // Format 1: https://drive.google.com/file/d/FILE_ID/view...
        val fileDRegex = Regex("""/file/d/([a-zA-Z0-9_-]+)""")
        val match1 = fileDRegex.find(trimmed)
        if (match1 != null && match1.groupValues.size > 1) {
            return match1.groupValues[1]
        }

        // Format 2: https://drive.google.com/open?id=FILE_ID or uc?id=FILE_ID
        val idParamRegex = Regex("""[?&]id=([a-zA-Z0-9_-]+)""")
        val match2 = idParamRegex.find(trimmed)
        if (match2 != null && match2.groupValues.size > 1) {
            return match2.groupValues[1]
        }

        // Format 3: Raw ID string (typically 28 to 50 alphanumerical characters)
        if (trimmed.length in 25..55 && !trimmed.contains("/") && !trimmed.contains(".")) {
            return trimmed
        }

        return null
    }
}
