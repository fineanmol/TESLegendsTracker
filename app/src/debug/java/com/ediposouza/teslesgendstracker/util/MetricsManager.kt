package com.ediposouza.teslesgendstracker.util

import android.content.Context
import com.ediposouza.teslesgendstracker.data.Card
import com.ediposouza.teslesgendstracker.data.Deck
import com.ediposouza.teslesgendstracker.data.Patch
import com.google.firebase.auth.FirebaseUser
import timber.log.Timber

/**
 * Created by ediposouza on 08/12/16.
 */
@Suppress("UNUSED_PARAMETER")
object MetricsManager : MetricsConstants() {

    fun initialize(context: Context) {
    }

    fun flush() {
    }

    fun trackAction(action: MetricAction) {
        Timber.d(action.name)
    }

    fun trackScreen(screen: MetricScreen) {
        Timber.d(screen.name)
    }

    fun trackSignUp() {
        Timber.d("SignUp")
    }

    fun trackSignIn(user: FirebaseUser?, success: Boolean) {
        Timber.d("SignIn success: $success")
    }

    fun trackSearch(searchTerm: String) {
        Timber.d("Search: $searchTerm")
    }

    fun trackCardView(card: Card) {
        Timber.d("Card view: $card")
    }

    fun trackDeckView(deck: Deck) {
        Timber.d("Deck view: $deck")
    }

    fun trackPatchView(patch: Patch) {
        Timber.d("Patch view: $patch")
    }

}