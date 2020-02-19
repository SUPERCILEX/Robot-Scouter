package com.supercilex.robotscouter.feature.trash

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FirebaseFirestoreException.Code
import com.google.firebase.firestore.Source
import com.supercilex.robotscouter.common.DeletionType
import com.supercilex.robotscouter.core.data.logFailures
import com.supercilex.robotscouter.core.data.model.scoutParser
import com.supercilex.robotscouter.core.data.model.teamParser
import com.supercilex.robotscouter.core.data.teamsRef
import com.supercilex.robotscouter.core.data.templatesRef
import com.supercilex.robotscouter.core.unsafeLazy
import com.supercilex.robotscouter.feature.trash.databinding.TrashItemBinding
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicBoolean
import com.supercilex.robotscouter.R as RC

internal class TrashViewHolder(
        itemView: View
) : RecyclerView.ViewHolder(itemView) {
    private val unknownName: String by unsafeLazy {
        itemView.context.getString(R.string.trash_unnamed_item)
    }
    private val loadingName: String by unsafeLazy {
        itemView.context.getString(R.string.trash_loading_item)
    }
    private val deletingName: String by unsafeLazy {
        itemView.context.getString(R.string.trash_deleting_item)
    }

    private val binding = TrashItemBinding.bind(itemView)

    lateinit var trash: Trash

    fun bind(trash: Trash, selected: Boolean) {
        itemView.isActivated = selected

        if (this::trash.isInitialized && this.trash == trash) return
        this.trash = trash

        binding.name.text = loadingName
        binding.type.setImageResource(when (trash.type) {
            DeletionType.TEAM -> RC.drawable.ic_person_grey_96dp
            DeletionType.TEMPLATE -> RC.drawable.ic_content_paste_grey_96dp
            else -> error("Unsupported type: ${trash.type}")
        })

        when (val type = trash.type) {
            DeletionType.TEAM -> teamsRef.document(trash.id)
            DeletionType.TEMPLATE -> templatesRef.document(trash.id)
            else -> error("Unsupported type: $type")
        }.let {
            // Getting it from the cache is faster and we don't really care about up-to-date-ness
            it.get(Source.CACHE).addOnCompleteListener(Listener(this, it))
        }
    }

    private class Listener(
            holder: TrashViewHolder,
            private val ref: DocumentReference
    ) : OnCompleteListener<DocumentSnapshot> {
        private val holder = WeakReference(holder)
        private val hasRetried = AtomicBoolean()

        override fun onComplete(task: Task<DocumentSnapshot>) {
            val holder = holder.get() ?: return
            val trash = holder.trash

            if (!task.isSuccessful) {
                val e = task.exception
                if (
                    e is FirebaseFirestoreException && e.code == Code.UNAVAILABLE &&
                    hasRetried.compareAndSet(false, true)
                ) ref.get().logFailures("getTrashedTeam", ref, trash.id).addOnCompleteListener(this)

                return
            }

            val snapshot = checkNotNull(task.result)
            if (snapshot.id != trash.id) return // Holder might have changed
            if (!snapshot.exists()) {
                holder.binding.name.text = holder.deletingName
                return
            }

            holder.binding.name.text = when (val type = trash.type) {
                DeletionType.TEAM -> teamParser.parseSnapshot(snapshot).toString()
                DeletionType.TEMPLATE ->
                    scoutParser.parseSnapshot(snapshot).name ?: holder.unknownName
                else -> error("Unsupported type: $type")
            }
        }
    }
}
