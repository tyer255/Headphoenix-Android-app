package com.example

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OfficialTrackValidatorTest {

    @Test
    fun testAuthenticSongsPassValidation() {
        // Exact official match
        assertTrue(
            OfficialTrackValidator.isAuthentic(
                requestedTitle = "Khat",
                requestedArtist = "Navjot Ahuja",
                candidateTitle = "Khat",
                candidateArtist = "Navjot Ahuja"
            )
        )

        // Official song with soundtrack metadata
        assertTrue(
            OfficialTrackValidator.isAuthentic(
                requestedTitle = "Tum Hi Ho",
                requestedArtist = "Arijit Singh",
                candidateTitle = "Tum Hi Ho (From \"Aashiqui 2\")",
                candidateArtist = "Arijit Singh, Mithoon"
            )
        )

        // Official multi-artist song
        assertTrue(
            OfficialTrackValidator.isAuthentic(
                requestedTitle = "Kesariya",
                requestedArtist = "Arijit Singh",
                candidateTitle = "Kesariya",
                candidateArtist = "Arijit Singh, Pritam, Amitabh Bhattacharya"
            )
        )
    }

    @Test
    fun testFemaleVersionsAndCoversAreStrictlyBlocked() {
        // Female cover rejected
        assertFalse(
            OfficialTrackValidator.isAuthentic(
                requestedTitle = "Khat",
                requestedArtist = "Navjot Ahuja",
                candidateTitle = "Khat (Female Cover)",
                candidateArtist = "Navjot Ahuja"
            )
        )

        // Unrelated song with extra words and different singer rejected
        assertFalse(
            OfficialTrackValidator.isAuthentic(
                requestedTitle = "Khat",
                requestedArtist = "Navjot Ahuja",
                candidateTitle = "Khat Likhu",
                candidateArtist = "Riya Bishwas, Suyash Yadav"
            )
        )

        // Duomelo / female duo rejected
        assertFalse(
            OfficialTrackValidator.isAuthentic(
                requestedTitle = "Khat",
                requestedArtist = "Navjot Ahuja",
                candidateTitle = "Tere Liye - Khat",
                candidateArtist = "Duomelo, Avneesh"
            )
        )

        // Female version of popular song rejected
        assertFalse(
            OfficialTrackValidator.isAuthentic(
                requestedTitle = "Tum Hi Ho",
                requestedArtist = "Arijit Singh",
                candidateTitle = "Tum Hi Ho (Female Version)",
                candidateArtist = "Palak Muchhal"
            )
        )

        // Slowed & reverb rejected
        assertFalse(
            OfficialTrackValidator.isAuthentic(
                requestedTitle = "Pehle Bhi Main",
                requestedArtist = "Vishal Mishra",
                candidateTitle = "Pehle Bhi Main - Slowed & Reverb",
                candidateArtist = "Vishal Mishra"
            )
        )

        // Lofi rejected
        assertFalse(
            OfficialTrackValidator.isAuthentic(
                requestedTitle = "Channa Mereya",
                requestedArtist = "Arijit Singh",
                candidateTitle = "Channa Mereya (Lofi Flip)",
                candidateArtist = "Chill Vibes"
            )
        )

        // Remix rejected
        assertFalse(
            OfficialTrackValidator.isAuthentic(
                requestedTitle = "Shape of You",
                requestedArtist = "Ed Sheeran",
                candidateTitle = "Shape of You (Club Remix)",
                candidateArtist = "DJ Snake"
            )
        )
    }
}
