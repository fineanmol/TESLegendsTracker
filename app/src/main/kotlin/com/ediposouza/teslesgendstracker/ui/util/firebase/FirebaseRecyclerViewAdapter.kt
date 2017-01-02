/*
 * Firebase UI Bindings Android Library
 *
 * Copyright © 2015 Firebase - All Rights Reserved
 * https://www.firebase.com
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binaryform must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY FIREBASE AS IS AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO
 * EVENT SHALL FIREBASE BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.ediposouza.teslesgendstracker.ui.util.firebase

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.Query
import java.lang.reflect.InvocationTargetException

/**
 * This class is a generic way of backing an RecyclerView with a Firebase location.
 * It handles all of the child events at the given Firebase location. It marshals received data into the given
 * class type.
 * To use this class in your app, subclass it passing in all required parameters and implement the
 * populateViewHolder method.
 * <blockquote><pre>
 * {@code
 *     private static class ChatMessageViewHolder extends RecyclerView.ViewHolder {
 *         TextView messageText;
 *         TextView nameText;
 *
 *         public ChatMessageViewHolder(View itemView) {
 *             super(itemView);
 *             nameText = (TextView)itemView.findViewById(android.R.id.text1);
 *             messageText = (TextView) itemView.findViewById(android.R.id.text2);
 *         }
 *     }
 *
 *     FirebaseRecyclerViewAdapter<ChatMessage, ChatMessageViewHolder> adapter;
 *     ref = new Firebase("https://<yourapp>.firebaseio.com");
 *
 *     RecyclerView recycler = (RecyclerView) findViewById(R.id.messages_recycler);
 *     recycler.setHasFixedSize(true);
 *     recycler.setLayoutManager(new LinearLayoutManager(this));
 *
 *     adapter = new FirebaseRecyclerViewAdapter<ChatMessage, ChatMessageViewHolder>(ChatMessage.class, android.R.layout.two_line_list_item, ChatMessageViewHolder.class, mRef) {
 *         public void populateViewHolder(ChatMessageViewHolder chatMessageViewHolder, ChatMessage chatMessage) {
 *             chatMessageViewHolder.nameText.setText(chatMessage.getName());
 *             chatMessageViewHolder.messageText.setText(chatMessage.getMessage());
 *         }
 *     };
 *     recycler.setAdapter(mAdapter);
 * }
 * </pre></blockquote>
 *
 * @param <T> The Java class that maps to the type of objects stored in the Firebase location.
 * @param <VH> The ViewHolder class that contains the Views in the layout that is shown for each object.
 * </VH></T>
 *
 * CONSTRUCTOR
 * @param modelClass Firebase will marshall the data at a location into an instance of a class that you provide
 * *
 * @param modelLayout This is the layout used to represent a single item in the list. You will be responsible for populating an
 * *                    instance of the corresponding view with the data from an instance of modelClass.
 * *
 * @param viewHolderClass The class that hold references to all sub-views in an instance modelLayout.
 * *
 * @param ref        The Firebase location to watch for data changes.
 * *
 * @param pageSize   initial page size. set 0 for no limit.
 */
abstract class FirebaseRecyclerViewAdapter<T, VH : RecyclerView.ViewHolder>(
        var mModelClass: Class<T>,
        protected var mModelLayout: Int,
        var mViewHolderClass: Class<VH>,
        ref: Query?,
        pageSize: Int = 0,
        orderASC: Boolean = false) : RecyclerView.Adapter<VH>() {

    var mSnapshots: FirebaseArray = FirebaseArray(ref, pageSize, orderASC)

    init {
        mSnapshots.mOnChangedListener = object : FirebaseArray.OnChangedListener {
            override fun onChanged(type: FirebaseArray.EventType, index: Int, oldIndex: Int) {
                when (type) {
                    FirebaseArray.EventType.Added -> notifyItemInserted(index + snapShotOffset)
                    FirebaseArray.EventType.Changed -> notifyItemChanged(index + snapShotOffset)
                    FirebaseArray.EventType.Removed -> notifyItemRemoved(index + snapShotOffset)
                    FirebaseArray.EventType.Moved -> notifyItemMoved(oldIndex + snapShotOffset, index + snapShotOffset)
                    FirebaseArray.EventType.Reset -> notifyDataSetChanged()
                    else -> throw IllegalStateException("Incomplete case statement")
                }
            }

        }
        mSnapshots.mOnSyncStatusListener = object : FirebaseArray.OnSyncStatusChangedListener {
            override fun onChanged(type: FirebaseArray.SyncType) {
                onSyncStatusChanged(type == FirebaseArray.SyncType.Synced)
            }
        }
        mSnapshots.mOnErrorListener = object : FirebaseArray.OnErrorListener {
            override fun onError(firebaseError: DatabaseError) {
                onArrayError(firebaseError)
            }
        }

    }

    /**
     * Increase the limit of the query by one page.
     */
    fun more() = mSnapshots.more()

    /**
     * Reset the limit of the query to its original size.
     */
    fun reset() = mSnapshots.reset()

    fun cleanup() = mSnapshots.cleanup()

    /**
     * Override when adding headers.
     * @return number of items inserted in front of the FirebaseArray
     */
    open val snapShotOffset: Int = 0

    /**
     * Override when adding headers or footers
     * @return number of items including headers and footers.
     */
    override fun getItemCount(): Int {
        return mSnapshots.count
    }

    fun getItem(position: Int): T {
        return mSnapshots.getItem(position - snapShotOffset).getValue(mModelClass)
    }

    fun getRef(position: Int): DatabaseReference {
        return mSnapshots.getItem(position).ref
    }

    override fun getItemId(position: Int): Long {
        if (position < snapShotOffset) return ("header" + position).hashCode().toLong()
        if (position >= snapShotOffset + mSnapshots.count) return ("footer" + (position - (snapShotOffset + mSnapshots.count))).hashCode().toLong()
        // http://stackoverflow.com/questions/5100071/whats-the-purpose-of-item-ids-in-android-listview-adapter
        return mSnapshots.getItem(position).key.hashCode().toLong()
    }

    /**
     * Override when adding headers or footers.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context).inflate(mModelLayout, parent, false) as ViewGroup
        try {
            val constructor = mViewHolderClass.getConstructor(View::class.java)
            return constructor.newInstance(view)
        } catch (e: NoSuchMethodException) {
            throw RuntimeException(e)
        } catch (e: InvocationTargetException) {
            throw RuntimeException(e)
        } catch (e: InstantiationException) {
            throw RuntimeException(e)
        } catch (e: IllegalAccessException) {
            throw RuntimeException(e)
        }

    }

    override fun onBindViewHolder(viewHolder: VH, position: Int) {
        var model: T? = null
        val arrayPosition = position - snapShotOffset
        if (arrayPosition < mSnapshots.count && arrayPosition >= 0) {
            model = getItem(position)
        }
        populateViewHolder(viewHolder, model, position)
    }

    /**
     * Each time the data at the given Firebase location changes, this method will be called for each item that needs
     * to be displayed. The first two arguments correspond to the mLayout and mModelClass given to the constructor of
     * this class. The third argument is the item's position in the list.
     * <p>
     * Your implementation should populate the view using the data contained in the model.
     * You should implement either this method or the other FirebaseRecyclerViewAdapter#populateViewHolder(VH, Object) method
     * but not both.
     *
     * @param viewHolder The view to populate
     * *
     * @param model      The object containing the data used to populate the view
     * *
     * @param position  The position in the list of the view being populated
     */
    open protected fun populateViewHolder(viewHolder: VH, model: T?, position: Int) {
        populateViewHolder(viewHolder, model)
    }

    /**
     * This is a backwards compatible version of populateViewHolder.
     *
     *
     * You should implement either this method or the other FirebaseRecyclerViewAdapter#populateViewHolder(VH, T, int) method
     * but not both.
     * see FirebaseListAdapter#populateView(View, Object, int)
     * @param viewHolder The view to populate
     * *
     * @param model      The object containing the data used to populate the view
     */
    open protected fun populateViewHolder(viewHolder: VH, model: T?) {
    }

    open protected fun onSyncStatusChanged(synced: Boolean) {}
    open protected fun onArrayError(firebaseError: DatabaseError) {}
}