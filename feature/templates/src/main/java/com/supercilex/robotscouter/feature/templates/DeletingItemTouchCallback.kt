package com.supercilex.robotscouter.feature.templates

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.support.v4.content.ContextCompat
import android.support.v7.content.res.AppCompatResources
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import kotlin.math.roundToInt
import com.supercilex.robotscouter.R as RC

internal abstract class DeletingItemTouchCallback(
        dragDirs: Int,
        private val context: Context
) : ItemTouchHelper.SimpleCallback(
        dragDirs,
        ItemTouchHelper.START
) {
    private val deleteIcon = checkNotNull(AppCompatResources.getDrawable(
            context, R.drawable.ic_delete_black_24dp))
    private val deletePaint = Paint().apply {
        color = ContextCompat.getColor(context, RC.color.delete_background)
    }
    private val deleteIconPadding = context.resources.getDimension(RC.dimen.spacing_large).toInt()

    override fun onChildDraw(
            c: Canvas,
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            dX: Float,
            dY: Float,
            actionState: Int,
            isCurrentlyActive: Boolean
    ) {
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
        if (actionState != ItemTouchHelper.ACTION_STATE_SWIPE) return

        val v = viewHolder.itemView

        c.drawRect(
                v.right.toFloat() + dX,
                v.top.toFloat(),
                v.right.toFloat(),
                v.bottom.toFloat(),
                deletePaint
        )
        deleteIcon.apply {
            val right = v.right - deleteIconPadding
            val center = (v.height / 2.0).roundToInt()
            val half = intrinsicHeight / 2
            setBounds(
                    right - intrinsicWidth,
                    v.top + center - half,
                    right,
                    v.bottom - center + half
            )
            draw(c)
        }
    }
}
