package com.notebook.notes.checklist.util

import android.content.Context
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.notebook.notes.checklist.R
import kotlin.math.max

class FlowLayoutManager(
    context: Context
) : RecyclerView.LayoutManager() {

    private val horizontalSpacing =
        context.resources.getDimensionPixelSize(R.dimen.label_suggestion_chip_spacing_horizontal)
    private val verticalSpacing =
        context.resources.getDimensionPixelSize(R.dimen.label_suggestion_chip_spacing_vertical)

    override fun generateDefaultLayoutParams(): RecyclerView.LayoutParams =
        RecyclerView.LayoutParams(
            RecyclerView.LayoutParams.WRAP_CONTENT,
            RecyclerView.LayoutParams.WRAP_CONTENT
        )

    override fun onLayoutChildren(recycler: RecyclerView.Recycler, state: RecyclerView.State) {
        if (itemCount == 0) {
            detachAndScrapAttachedViews(recycler)
            return
        }

        detachAndScrapAttachedViews(recycler)

        val availableWidth = width - paddingLeft - paddingRight
        var currentX = paddingLeft
        var currentY = paddingTop
        var rowMaxHeight = 0

        for (index in 0 until itemCount) {
            val view = recycler.getViewForPosition(index)
            addView(view)
            measureChildWithMargins(view, 0, 0)

            val childWidth = getDecoratedMeasuredWidth(view)
            val childHeight = getDecoratedMeasuredHeight(view)

            if (currentX > paddingLeft && currentX + childWidth > paddingLeft + availableWidth) {
                currentX = paddingLeft
                currentY += rowMaxHeight + verticalSpacing
                rowMaxHeight = 0
            }

            layoutDecorated(view, currentX, currentY, currentX + childWidth, currentY + childHeight)
            currentX += childWidth + horizontalSpacing
            rowMaxHeight = max(rowMaxHeight, childHeight)
        }
    }

    override fun canScrollHorizontally(): Boolean = false

    override fun canScrollVertically(): Boolean = false

    override fun onMeasure(
        recycler: RecyclerView.Recycler,
        state: RecyclerView.State,
        widthSpec: Int,
        heightSpec: Int
    ) {
        val width = View.getDefaultSize(width, widthSpec)
        if (state.itemCount == 0) {
            setMeasuredDimension(width, paddingTop + paddingBottom)
            return
        }

        val availableWidth = width - paddingLeft - paddingRight
        var currentX = paddingLeft
        var currentY = paddingTop
        var rowMaxHeight = 0

        for (index in 0 until state.itemCount) {
            val view = recycler.getViewForPosition(index)
            measureChildWithMargins(view, 0, 0)

            val childWidth = getDecoratedMeasuredWidth(view)
            val childHeight = getDecoratedMeasuredHeight(view)

            if (currentX > paddingLeft && currentX + childWidth > paddingLeft + availableWidth) {
                currentX = paddingLeft
                currentY += rowMaxHeight + verticalSpacing
                rowMaxHeight = 0
            }

            currentX += childWidth + horizontalSpacing
            rowMaxHeight = max(rowMaxHeight, childHeight)
            recycler.recycleView(view)
        }

        val height = currentY + rowMaxHeight + paddingBottom
        setMeasuredDimension(width, height)
    }
}
