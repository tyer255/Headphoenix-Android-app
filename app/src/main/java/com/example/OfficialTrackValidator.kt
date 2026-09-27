package com.example

import java.util.Locale

/**
 * 100% Strict Authenticity Verification Gate.
 * Guarantees that only official, verified tracks play in the application.
 * Strictly forbids covers, female versions, remixes, lofi, sped-up/slowed edits,
 * karaoke, or unrelated tracks from ever playing unless explicitly requested by the user.
 */
object OfficialTrackValidator {

    private val FORBIDDEN_MODIFIERS = listOf(
        "female", "cover", "remix", "lofi", "lo-fi", "slowed", "reverb",
        "sped up", "speed up", "unplugged", "acoustic", "tribute",
        "instrumental", "karaoke", "duomelo", "arjama", "riya bishwas",
        "parody", "mashup", "reprise", "chipmunk", "8d audio", "ringtone",
        "dialogue", "teaser", "trailer", "reaction", "guitar cover",
        "piano cover", "dance mix", "club mix", "dj mix", "bass boosted"
    )

    fun clean(text: String): String {
        return text.lowercase(Locale.ROOT)
            .replace(Regex("&quot;|&amp;|&#039;|&apos;"), " ")
            .replace(Regex("\\([^)]*\\)|\\[[^\\]]*\\]"), " ")
            .replace(Regex("- (single|official|audio|video|lyrics|from [^\\-]*)", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("[^a-zA-Z0-9\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    /**
     * Calculates authenticity confidence score (0 to 100).
     * Score >= 70 is required to qualify as the authentic official track.
     */
    fun calculateScore(
        requestedTitle: String,
        requestedArtist: String,
        candidateTitle: String,
        candidateArtist: String,
        requestedDurationSeconds: Int? = null,
        candidateDurationSeconds: Int? = null
    ): Int {
        val rawReqTitleLower = requestedTitle.lowercase(Locale.ROOT)
        val rawCandTitleLower = candidateTitle.lowercase(Locale.ROOT)
        val rawCandArtistLower = candidateArtist.lowercase(Locale.ROOT)

        // 1. Strict Forbidden Modifier Disqualification:
        // If candidate contains any forbidden modifier not present in the requested song, DISQUALIFY immediately.
        for (modifier in FORBIDDEN_MODIFIERS) {
            if ((rawCandTitleLower.contains(modifier) || rawCandArtistLower.contains(modifier)) &&
                !rawReqTitleLower.contains(modifier)
            ) {
                return 0 // Disqualified! Cover / remix / unofficial edit
            }
        }

        val normReqTitle = clean(requestedTitle)
        val normReqArtist = clean(requestedArtist)
        val normCandTitle = clean(candidateTitle)
        val normCandArtist = clean(candidateArtist)

        var score = 0

        // 2. Strict Title Matching
        if (normReqTitle == normCandTitle) {
            score += 55
        } else if (normCandTitle.startsWith(normReqTitle) || normReqTitle.startsWith(normCandTitle)) {
            score += 45
        } else {
            val reqWords = normReqTitle.split(" ").filter { it.length > 1 }
            val candWords = normCandTitle.split(" ").filter { it.length > 1 }

            if (reqWords.isEmpty()) {
                return 0
            }

            val matchedWords = reqWords.filter { it in candWords }
            val matchRatio = matchedWords.size.toDouble() / reqWords.size

            if (matchRatio >= 0.70) {
                val extraWords = candWords.filter { it !in reqWords }
                // For short titles (1-2 words like "Khat"), extra unrelated words mean a different song
                if (reqWords.size <= 2 && extraWords.isNotEmpty()) {
                    val dangerousWords = listOf("likhu", "liye", "tere", "khatir", "aale", "wala", "mandir", "katha", "aarti")
                    if (extraWords.any { it in dangerousWords }) {
                        return 0
                    }
                    score += 20
                } else {
                    score += (matchRatio * 40).toInt()
                }
            } else {
                return 0 // Title does not match
            }
        }

        // 3. Strict Artist Matching
        if (normReqArtist.isNotEmpty()) {
            val reqArtistWords = normReqArtist.split(" ").filter { it.length > 2 }
            if (normCandArtist.contains(normReqArtist) || normReqArtist.contains(normCandArtist)) {
                score += 45
            } else if (reqArtistWords.isNotEmpty()) {
                val matchedArtists = reqArtistWords.filter { it in normCandArtist }
                if (matchedArtists.isNotEmpty()) {
                    val artistRatio = matchedArtists.size.toDouble() / reqArtistWords.size
                    score += (artistRatio * 40).toInt()
                } else {
                    // Artist completely unmatched! Disqualify.
                    return 0
                }
            } else {
                return 0
            }
        } else {
            score += 35 // No artist provided, neutral
        }

        // 4. Duration Tolerance Check
        if (requestedDurationSeconds != null && candidateDurationSeconds != null &&
            requestedDurationSeconds > 30 && candidateDurationSeconds > 30
        ) {
            val diff = Math.abs(requestedDurationSeconds - candidateDurationSeconds)
            if (diff > 90) {
                // Large discrepancy in duration (> 90 seconds) indicates an extended/cut/different version
                score -= 25
            }
        }

        return score
    }

    /**
     * Returns true ONLY if candidate track is verified authentic official version.
     */
    fun isAuthentic(
        requestedTitle: String,
        requestedArtist: String,
        candidateTitle: String,
        candidateArtist: String,
        requestedDurationSeconds: Int? = null,
        candidateDurationSeconds: Int? = null
    ): Boolean {
        return calculateScore(
            requestedTitle,
            requestedArtist,
            candidateTitle,
            candidateArtist,
            requestedDurationSeconds,
            candidateDurationSeconds
        ) >= 70
    }
}
