package com.ediposouza.teslesgendstracker.interactor

import com.ediposouza.teslesgendstracker.data.Attribute
import com.ediposouza.teslesgendstracker.data.Card
import com.ediposouza.teslesgendstracker.data.FirebaseCard
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import timber.log.Timber

/**
 * Created by ediposouza on 01/11/16.
 */
class CardInteractor() {

    val NODE_CARDS = "cards"
    val NODE_CORE = "core"
    val CHILD_COST = "cost"

    val mDatabase by lazy { FirebaseDatabase.getInstance().reference }

    fun getCards(cls: Attribute, onSuccess: (List<Card>) -> Unit) {
        val node_cls = cls.name.toLowerCase()
        mDatabase.child(NODE_CARDS).child(NODE_CORE).child(node_cls).orderByChild(CHILD_COST)
                .addValueEventListener(object : ValueEventListener {

                    override fun onDataChange(ds: DataSnapshot) {
                        val cards = ds.children.mapTo(arrayListOf<Card>()) {
                            it.getValue(FirebaseCard::class.java).toCard(cls)
                        }
                        Timber.d(cards.toString())
                        onSuccess.invoke(cards)
                    }

                    override fun onCancelled(de: DatabaseError) {
                        Timber.d("Fail: " + de.message)
                    }

                })
    }

}