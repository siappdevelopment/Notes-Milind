package com.notebook.notes.checklist.ui.card

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import android.content.Context
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import coil.load
import com.google.android.material.card.MaterialCardView
import com.notebook.notes.checklist.R
import com.notebook.notes.checklist.databinding.IncludeNoteCardFooterBinding
import com.notebook.notes.checklist.databinding.IncludeNoteCardHeaderBinding
import com.notebook.notes.checklist.databinding.ItemNoteCardBulletListBinding
import com.notebook.notes.checklist.databinding.ItemNoteCardBulletListRowBinding
import com.notebook.notes.checklist.databinding.ItemNoteCardImageBinding
import com.notebook.notes.checklist.databinding.ItemNoteCardLockedBinding
import com.notebook.notes.checklist.databinding.ItemNoteCardMixedBinding
import com.notebook.notes.checklist.databinding.ItemNoteCardNumberListBinding
import com.notebook.notes.checklist.databinding.ItemNoteCardNumberListRowBinding
import com.notebook.notes.checklist.databinding.ItemNoteCardTextBinding
import com.notebook.notes.checklist.databinding.ItemNoteCardTodoBinding
import com.notebook.notes.checklist.databinding.ItemNoteCardTodoRowBinding
import com.notebook.notes.checklist.databinding.ItemNoteSectionHeaderBinding
import com.notebook.notes.checklist.databinding.PopupRecycleBinCardOptionsBinding
import com.notebook.notes.checklist.databinding.PopupNoteCardOptionsBinding
import com.notebook.notes.checklist.model.Note
import com.notebook.notes.checklist.model.NumberListHelper
import com.notebook.notes.checklist.ui.background.NoteBackgroundApplier
import com.notebook.notes.checklist.util.keepNavigationBarHidden
import com.notebook.notes.checklist.ui.model.BulletListPreviewContent
import com.notebook.notes.checklist.ui.model.BulletListPreviewItem
import com.notebook.notes.checklist.ui.model.ImagePreviewContent
import com.notebook.notes.checklist.ui.model.MixedPreviewContent
import com.notebook.notes.checklist.ui.model.NoteCardUiModel
import com.notebook.notes.checklist.ui.model.NumberListPreviewContent
import com.notebook.notes.checklist.ui.model.NumberListPreviewItem
import com.notebook.notes.checklist.ui.model.PreviewType
import com.notebook.notes.checklist.ui.model.TextPreviewContent
import com.notebook.notes.checklist.ui.model.TodoPreviewContent
import com.notebook.notes.checklist.ui.model.TodoPreviewItem
import com.notebook.notes.checklist.util.DateTimeUtils
import com.notebook.notes.checklist.util.NoteTitleHelper
import com.notebook.notes.checklist.util.SearchHighlightHelper
import java.io.File

sealed class HomeListItem {
    data class Section(val titleRes: Int) : HomeListItem()
    data class Card(val model: NoteCardUiModel) : HomeListItem()
}

class NoteCardAdapter(
    private val onNoteClick: (Note) -> Unit,
    private val onPinClick: (Note) -> Unit,
    private val onFavoriteClick: (Note) -> Unit,
    private val onDeleteClick: (Note) -> Unit,
    private val onShareClick: (Note) -> Unit = {},
    private val onArchiveClick: (Note) -> Unit = {},
    private val onLockClick: (Note) -> Unit = {},
    private val onReminderClick: (Note) -> Unit = {},
    private val onCopyClick: (Note) -> Unit = {},
    private val cardMenuMode: CardMenuMode = CardMenuMode.DEFAULT,
    private val onRestoreClick: (Note) -> Unit = {},
    private val onPermanentDeleteClick: (Note) -> Unit = {}
) : ListAdapter<HomeListItem, RecyclerView.ViewHolder>(HomeListDiffCallback()) {

    enum class CardMenuMode {
        DEFAULT,
        RECYCLE_BIN
    }

    private var searchQuery: String = ""
    private var boundTimeFormat: String = DateTimeUtils.currentFormat()
    private var boundDateFormat: String = DateTimeUtils.currentDateLayout()
    private var itemTouchHelper: ItemTouchHelper? = null
    private var boundRecyclerView: RecyclerView? = null
    private var dragInProgress = false
    private var dragReorderEnabled = false
    private var pendingNotes: List<NoteCardUiModel>? = null
    private var selectionMode = false
    private val selectedNoteIds = mutableSetOf<Long>()

    var onSelectionModeChanged: (Boolean, Int) -> Unit = { _, _ -> }

    var onDragReorderFinished: (List<Pair<Long, Boolean>>) -> Unit = {}

    init {
        setHasStableIds(true)
    }

    /**
     * Rebinds cards when Settings → Time Format or Date Format changes
     * while this list is visible (e.g. returning from SettingActivity).
     */
    fun refreshDateTimeFormatIfNeeded() {
        val currentTime = DateTimeUtils.currentFormat()
        val currentDate = DateTimeUtils.currentDateLayout()
        if (boundTimeFormat == currentTime && boundDateFormat == currentDate) return
        boundTimeFormat = currentTime
        boundDateFormat = currentDate
        if (itemCount > 0) {
            notifyItemRangeChanged(0, itemCount, PAYLOAD_DATETIME_FORMAT)
        }
    }

    /** @deprecated Use [refreshDateTimeFormatIfNeeded]; kept for existing fragment callers. */
    fun refreshTimeFormatIfNeeded() = refreshDateTimeFormatIfNeeded()

    fun setSearchQuery(query: String) {
        val normalized = query.trim()
        if (searchQuery == normalized) return
        searchQuery = normalized
        if (itemCount > 0) {
            notifyItemRangeChanged(0, itemCount, PAYLOAD_SEARCH_HIGHLIGHT)
        }
    }

    fun setDragReorderEnabled(enabled: Boolean) {
        if (dragReorderEnabled == enabled) return
        dragReorderEnabled = enabled
        if (!enabled && pendingNotes != null) {
            val notes = pendingNotes!!
            pendingNotes = null
            submitNotesInternal(notes)
        }
    }

    /**
     * Drag is allowed when reorder is enabled and either:
     * - not in selection mode, or
     * - exactly one note is selected (that note can be dragged).
     */
    fun isDragReorderEnabled(): Boolean {
        if (!dragReorderEnabled) return false
        if (!selectionMode) return true
        return selectedNoteIds.size == 1
    }

    fun isDragInProgress(): Boolean = dragInProgress

    fun isInSelectionMode(): Boolean = selectionMode

    fun getSelectedNoteIds(): Set<Long> = selectedNoteIds.toSet()

    fun getSelectedCount(): Int = selectedNoteIds.size

    fun isNoteSelected(noteId: Long): Boolean = selectedNoteIds.contains(noteId)

    fun isAllSelected(noteIds: Collection<Long>): Boolean =
        noteIds.isNotEmpty() && selectedNoteIds.containsAll(noteIds)

    fun enterSelectionMode(noteId: Long? = null) {
        if (!selectionMode) {
            selectionMode = true
        }
        noteId?.let { selectedNoteIds.add(it) }
        notifySelectionChanged()
    }

    fun exitSelectionMode() {
        if (!selectionMode) return
        selectionMode = false
        selectedNoteIds.clear()
        notifySelectionChanged()
    }

    fun toggleNoteSelection(noteId: Long) {
        if (!selectionMode) {
            enterSelectionMode(noteId)
            return
        }
        if (noteId in selectedNoteIds) {
            selectedNoteIds.remove(noteId)
            if (selectedNoteIds.isEmpty()) {
                exitSelectionMode()
            } else {
                notifySelectionChanged(noteId)
            }
        } else {
            selectedNoteIds.add(noteId)
            notifySelectionChanged(noteId)
        }
    }

    fun selectAll(noteIds: Collection<Long>) {
        if (noteIds.isEmpty()) return
        if (!selectionMode) {
            selectionMode = true
        }
        selectedNoteIds.clear()
        selectedNoteIds.addAll(noteIds)
        notifySelectionChanged()
    }

    fun deselectAll() {
        exitSelectionMode()
    }

    private fun notifySelectionChanged(changedNoteId: Long? = null) {
        onSelectionModeChanged(selectionMode, selectedNoteIds.size)
        if (changedNoteId != null) {
            findCardPosition(changedNoteId)?.let { notifyItemChanged(it, PAYLOAD_SELECTION) }
        } else {
            notifyItemRangeChanged(0, itemCount, PAYLOAD_SELECTION)
        }
    }

    private fun findCardPosition(noteId: Long): Int? {
        val index = currentList.indexOfFirst { item ->
            item is HomeListItem.Card && item.model.noteId == noteId
        }
        return index.takeIf { it >= 0 }
    }

    fun attachDragHelper(recyclerView: RecyclerView) {
        val callback = NoteCardDragCallback(
            adapter = this,
            onDragFinished = {
                onDragReorderFinished(getOrderedCardsForPersistence())
            }
        )
        itemTouchHelper = ItemTouchHelper(callback).also { it.attachToRecyclerView(recyclerView) }
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        boundRecyclerView = recyclerView
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        if (boundRecyclerView === recyclerView) boundRecyclerView = null
    }

    /**
     * Staggered grid caches each item's column. After a pin/unpin the list layout
     * changes (e.g. a single pinned card would otherwise stay in the right column),
     * so span assignments are invalidated to reflow items from column 0. Posted so it
     * runs after the current layout/animation pass rather than mid-diff.
     */
    private fun reflowStaggeredColumns() {
        val recyclerView = boundRecyclerView ?: return
        val lm = recyclerView.layoutManager as? StaggeredGridLayoutManager ?: return
        recyclerView.post {
            if (recyclerView.isComputingLayout) {
                recyclerView.post { lm.invalidateSpanAssignments() }
            } else {
                lm.invalidateSpanAssignments()
            }
        }
    }

    fun getListItem(position: Int): HomeListItem? = currentList.getOrNull(position)

    fun moveItem(fromPosition: Int, toPosition: Int): Boolean {
        val fromItem = currentList.getOrNull(fromPosition) as? HomeListItem.Card ?: return false
        val toItem = currentList.getOrNull(toPosition) as? HomeListItem.Card ?: return false
        if (fromItem.model.isPinned != toItem.model.isPinned) return false

        val updated = currentList.toMutableList()
        val moved = updated.removeAt(fromPosition)
        updated.add(toPosition, moved)
        dragInProgress = true
        submitList(updated)
        return true
    }

    fun finishDragIfNeeded(): Boolean {
        if (!dragInProgress) return false
        dragInProgress = false
        pendingNotes = null
        return true
    }

    fun getOrderedCardsForPersistence(): List<Pair<Long, Boolean>> =
        currentList.filterIsInstance<HomeListItem.Card>().map { it.model.noteId to it.model.isPinned }

    fun startDrag(viewHolder: RecyclerView.ViewHolder) {
        if (!isDragReorderEnabled() || viewHolder.itemViewType == VIEW_TYPE_SECTION) return
        if (selectionMode) {
            val item = getListItem(viewHolder.bindingAdapterPosition) as? HomeListItem.Card ?: return
            if (item.model.noteId !in selectedNoteIds) return
        }
        itemTouchHelper?.startDrag(viewHolder)
    }

    fun submitNotes(notes: List<NoteCardUiModel>) {
        if (dragInProgress) {
            pendingNotes = notes
            return
        }
        submitNotesInternal(notes)
    }

    private fun submitNotesInternal(notes: List<NoteCardUiModel>) {
        val pinned = notes.filter { it.isPinned }
        val others = notes.filter { !it.isPinned }
        val items = buildList {
            if (pinned.isNotEmpty()) {
                add(HomeListItem.Section(R.string.section_pinned))
                pinned.forEach { add(HomeListItem.Card(it)) }
            }
            if (others.isNotEmpty()) {
                add(HomeListItem.Section(R.string.section_others))
                others.forEach { add(HomeListItem.Card(it)) }
            }
        }
        submitList(items) { reflowStaggeredColumns() }
    }

    fun submitDeletedNotes(notes: List<NoteCardUiModel>) {
        submitList(notes.map { HomeListItem.Card(it) }) { reflowStaggeredColumns() }
    }

    override fun getItemId(position: Int): Long = when (val item = getItem(position)) {
        is HomeListItem.Section -> item.titleRes.toLong().inv()
        is HomeListItem.Card -> item.model.noteId
    }

    override fun getItemViewType(position: Int): Int = when (val item = getItem(position)) {
        is HomeListItem.Section -> VIEW_TYPE_SECTION
        is HomeListItem.Card -> item.model.previewType.ordinal
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        if (viewType == VIEW_TYPE_SECTION) {
            return SectionViewHolder(ItemNoteSectionHeaderBinding.inflate(inflater, parent, false))
        }
        return when (PreviewType.entries[viewType]) {
            PreviewType.TEXT -> TextViewHolder(ItemNoteCardTextBinding.inflate(inflater, parent, false))
            PreviewType.TODO -> TodoViewHolder(ItemNoteCardTodoBinding.inflate(inflater, parent, false))
            PreviewType.NUMBER_LIST -> NumberListViewHolder(ItemNoteCardNumberListBinding.inflate(inflater, parent, false))
            PreviewType.BULLET_LIST -> BulletListViewHolder(ItemNoteCardBulletListBinding.inflate(inflater, parent, false))
            PreviewType.IMAGE -> ImageViewHolder(ItemNoteCardImageBinding.inflate(inflater, parent, false))
            PreviewType.LOCKED -> LockedViewHolder(ItemNoteCardLockedBinding.inflate(inflater, parent, false))
            PreviewType.MIXED -> MixedViewHolder(ItemNoteCardMixedBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is HomeListItem.Section -> (holder as SectionViewHolder).bind(item)
            is HomeListItem.Card -> when (holder) {
                is TextViewHolder -> holder.bind(item.model)
                is TodoViewHolder -> holder.bind(item.model)
                is NumberListViewHolder -> holder.bind(item.model)
                is BulletListViewHolder -> holder.bind(item.model)
                is ImageViewHolder -> holder.bind(item.model)
                is LockedViewHolder -> holder.bind(item.model)
                is MixedViewHolder -> holder.bind(item.model)
            }
        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.contains(PAYLOAD_SEARCH_HIGHLIGHT) ||
            payloads.contains(PAYLOAD_DATETIME_FORMAT) ||
            payloads.contains(PAYLOAD_TIME_FORMAT)
        ) {
            onBindViewHolder(holder, position)
            return
        }
        if (payloads.contains(PAYLOAD_SELECTION)) {
            val item = getItem(position) as? HomeListItem.Card ?: return
            if (holder is BaseCardViewHolder) {
                holder.bindSelectionOnly(item.model)
            }
            return
        }
        super.onBindViewHolder(holder, position, payloads)
    }

    private class SectionViewHolder(
        private val binding: ItemNoteSectionHeaderBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: HomeListItem.Section) {
            binding.tvSectionTitle.setText(item.titleRes)
            // A section header must occupy the whole row in the staggered grid so
            // the section's cards begin on a fresh row below it.
            (binding.root.layoutParams as? StaggeredGridLayoutManager.LayoutParams)
                ?.isFullSpan = true
        }
    }

    private abstract inner class BaseCardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        protected fun applyTheme(container: View, model: NoteCardUiModel): NoteBackgroundApplier.Appearance {
            return NoteCardBackgroundHelper.apply(container, model)
        }

        protected fun bindHeader(
            header: IncludeNoteCardHeaderBinding,
            model: NoteCardUiModel,
            appearance: NoteBackgroundApplier.Appearance
        ) {
            header.tvTitle.text = highlightText(
                NoteTitleHelper.displayTitle(header.root.context, model.note.title),
                appearance
            )
            // Hide pin/bookmark status icons while selection chrome is visible.
            val showStatusIcons = !this@NoteCardAdapter.selectionMode
            header.ivPin.isVisible = showStatusIcons && model.isPinned
            header.ivBookmark.isVisible = showStatusIcons && model.isFavorite

            val showCategory = model.categoryName.isNotBlank()
            header.tvCategoryPill.isVisible = showCategory
            if (showCategory) {
                header.tvCategoryPill.text = model.categoryName
            }

            NoteCardBackgroundHelper.applyTextColors(
                appearance = appearance,
                title = header.tvTitle,
                categoryPill = header.tvCategoryPill.takeIf { showCategory },
                icons = listOfNotNull(
                    header.ivPin.takeIf { showStatusIcons && model.isPinned },
                    header.ivBookmark.takeIf { showStatusIcons && model.isFavorite }
                )
            )
        }

        protected fun bindFooter(
            footer: IncludeNoteCardFooterBinding,
            model: NoteCardUiModel,
            appearance: NoteBackgroundApplier.Appearance
        ) {
            val context = footer.root.context
            footer.tvDate.text = if (cardMenuMode == CardMenuMode.RECYCLE_BIN && model.note.deletedAt > 0L) {
                context.getString(
                    R.string.recycle_bin_card_date,
                    DateTimeUtils.formatCardDate(model.note.createdAt),
                    DateTimeUtils.formatCardDate(model.note.deletedAt)
                )
            } else {
                DateTimeUtils.formatCardDate(model.updatedAt)
            }
            NoteCardBackgroundHelper.applyTextColors(
                appearance = appearance,
                muted = footer.tvDate,
                icons = listOf(footer.btnMore),
                divider = footer.footerDivider
            )
            footer.btnMore.setOnClickListener { showContextMenu(it, model) }
        }

        /** Match colors adapt to each card's own surface so text stays readable per theme. */
        protected fun highlightText(
            text: CharSequence?,
            appearance: NoteBackgroundApplier.Appearance
        ): CharSequence {
            val palette = SearchHighlightHelper.paletteFor(appearance.backgroundColor)
            return SearchHighlightHelper.highlight(text, this@NoteCardAdapter.searchQuery, palette)
        }

        protected fun bindSelectionState(cardRoot: MaterialCardView, noteId: Long) {
            val context = cardRoot.context
            val isSelected = isNoteSelected(noteId)
            cardRoot.strokeWidth = if (isSelected) {
                context.resources.getDimensionPixelSize(R.dimen.note_card_selection_stroke_width)
            } else {
                0
            }
            if (isSelected) {
                cardRoot.strokeColor = ContextCompat.getColor(context, R.color.app_primary_color)
            }

            var badge = cardRoot.findViewWithTag<View>(SELECTION_BADGE_TAG)
            if (isSelected) {
                if (badge == null) {
                    badge = LayoutInflater.from(context)
                        .inflate(R.layout.layout_note_card_selection_badge, cardRoot, false)
                    badge.tag = SELECTION_BADGE_TAG
                    cardRoot.addView(badge)
                }
                badge.isVisible = true
                badge.bringToFront()
            } else {
                badge?.isVisible = false
            }
        }

        fun bindSelectionOnly(model: NoteCardUiModel) {
            val cardRoot = itemView.findViewById<MaterialCardView>(R.id.cardRoot) ?: return
            bindSelectionState(cardRoot, model.noteId)
            val showStatusIcons = !this@NoteCardAdapter.selectionMode
            itemView.findViewById<View>(R.id.ivPin)?.isVisible =
                showStatusIcons && model.isPinned
            itemView.findViewById<View>(R.id.ivBookmark)?.isVisible =
                showStatusIcons && model.isFavorite
        }

        protected fun bindInteractions(root: View, model: NoteCardUiModel) {
            root.setOnClickListener {
                if (this@NoteCardAdapter.selectionMode) {
                    toggleNoteSelection(model.noteId)
                } else {
                    onNoteClick(model.note)
                }
            }
            root.setOnLongClickListener {
                if (this@NoteCardAdapter.selectionMode) {
                    // With exactly one selected note, long-press starts drag reorder.
                    if (selectedNoteIds.size == 1 &&
                        model.noteId in selectedNoteIds &&
                        isDragReorderEnabled()
                    ) {
                        startDrag(this)
                    } else {
                        toggleNoteSelection(model.noteId)
                    }
                } else {
                    enterSelectionMode(model.noteId)
                }
                true
            }
            (root as? MaterialCardView)?.let { bindSelectionState(it, model.noteId) }
        }

        protected fun showContextMenu(anchor: View, model: NoteCardUiModel) {
            if (cardMenuMode == CardMenuMode.RECYCLE_BIN) {
                showRecycleBinContextMenu(anchor, model)
                return
            }
            val context = anchor.context
            val binding = PopupNoteCardOptionsBinding.inflate(LayoutInflater.from(context))
            val density = context.resources.displayMetrics.density
            val minWidth = (200 * density).toInt()

            val popup = PopupWindow(
                binding.root,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                false
            ).apply {
                elevation = 16f
                isOutsideTouchable = true
                isFocusable = false
                setBackgroundDrawable(
                    ContextCompat.getDrawable(context, android.R.color.transparent)
                )
                keepNavigationBarHidden(context)
            }

            if (model.isLocked) {
                binding.tvLock.setText(R.string.card_menu_unlock)
                binding.ivLock.setImageResource(R.drawable.ic_unlock)
            } else {
                binding.tvLock.setText(R.string.card_menu_lock)
                binding.ivLock.setImageResource(R.drawable.ic_lock)
            }

            if (model.isPinned) {
                binding.tvPin.setText(R.string.card_menu_unpin)
                binding.ivPin.setImageResource(R.drawable.ic_unpin)
            } else {
                binding.tvPin.setText(R.string.card_menu_pin)
                binding.ivPin.setImageResource(R.drawable.ic_op_pin)
            }

            binding.optionLock.setOnClickListener {
                popup.dismiss()
                onLockClick(model.note)
            }
            binding.optionReminder.setOnClickListener {
                popup.dismiss()
                onReminderClick(model.note)
            }
            binding.optionPin.setOnClickListener {
                popup.dismiss()
                onPinClick(model.note)
            }
            binding.optionCopy.setOnClickListener {
                popup.dismiss()
                onCopyClick(model.note)
            }
            binding.optionDelete.setOnClickListener {
                popup.dismiss()
                onDeleteClick(model.note)
            }

            // Prefer aligning under the card, slightly offset from the press/more anchor.
            // Flip above the anchor when there is not enough room below (e.g. last visible cards).
            showAnchoredPopup(
                popup = popup,
                content = binding.root,
                anchor = anchor,
                minWidth = minWidth,
                horizontalAlign = PopupHorizontalAlign.CENTER_UNDER_ANCHOR
            )
        }

        private fun showRecycleBinContextMenu(anchor: View, model: NoteCardUiModel) {
            val context = anchor.context
            val binding = PopupRecycleBinCardOptionsBinding.inflate(LayoutInflater.from(context))
            val density = context.resources.displayMetrics.density
            val minWidth = (200 * density).toInt()

            val popup = PopupWindow(
                binding.root,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                false
            ).apply {
                elevation = 16f
                isOutsideTouchable = true
                isFocusable = false
                setBackgroundDrawable(
                    ContextCompat.getDrawable(context, android.R.color.transparent)
                )
                keepNavigationBarHidden(context)
            }

            binding.optionRestore.setOnClickListener {
                popup.dismiss()
                onRestoreClick(model.note)
            }
            binding.optionDeletePermanently.setOnClickListener {
                popup.dismiss()
                onPermanentDeleteClick(model.note)
            }

            showAnchoredPopup(
                popup = popup,
                content = binding.root,
                anchor = anchor,
                minWidth = minWidth,
                horizontalAlign = PopupHorizontalAlign.END_OF_ANCHOR
            )
        }

        private fun showAnchoredPopup(
            popup: PopupWindow,
            content: View,
            anchor: View,
            minWidth: Int,
            horizontalAlign: PopupHorizontalAlign
        ) {
            val context = anchor.context
            val density = context.resources.displayMetrics.density
            val margin = (16 * density).toInt()
            val gap = (4 * density).toInt()

            // Defer until the anchor has a valid screen location (important after fast scroll).
            anchor.post {
                if (!anchor.isAttachedToWindow) return@post

                content.measure(
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                )
                val popupWidth = content.measuredWidth.coerceAtLeast(minWidth)
                val popupHeight = content.measuredHeight
                popup.width = popupWidth

                val location = IntArray(2)
                anchor.getLocationOnScreen(location)
                // Use the visible window frame so system bars / nav don't cause clipping.
                val visibleFrame = android.graphics.Rect()
                anchor.rootView.getWindowVisibleDisplayFrame(visibleFrame)

                var xOffset = when (horizontalAlign) {
                    PopupHorizontalAlign.CENTER_UNDER_ANCHOR ->
                        ((anchor.width - popupWidth) / 2).coerceAtMost(0)
                    PopupHorizontalAlign.END_OF_ANCHOR ->
                        anchor.width - popupWidth - (8 * density).toInt()
                }
                if (location[0] + xOffset + popupWidth > visibleFrame.right - margin) {
                    xOffset = visibleFrame.right - margin - location[0] - popupWidth
                }
                if (location[0] + xOffset < visibleFrame.left + margin) {
                    xOffset = visibleFrame.left + margin - location[0]
                }

                val spaceBelow =
                    visibleFrame.bottom - (location[1] + anchor.height) - margin
                val spaceAbove = location[1] - visibleFrame.top - margin
                val openAbove = when {
                    spaceBelow >= popupHeight -> false
                    spaceAbove >= popupHeight -> true
                    // Prefer the side with more room so as many items as possible stay visible.
                    else -> spaceAbove > spaceBelow
                }
                val yOffset = if (openAbove) {
                    // showAsDropDown is relative to the bottom of the anchor.
                    -(anchor.height + popupHeight + gap)
                } else {
                    gap
                }

                popup.showAsDropDown(anchor, xOffset, yOffset)
            }
        }
    }

    private enum class PopupHorizontalAlign {
        CENTER_UNDER_ANCHOR,
        END_OF_ANCHOR
    }

    private fun bindTodoProgress(
        progressBinding: com.notebook.notes.checklist.databinding.IncludeNoteCardTodoProgressBinding,
        completed: Int,
        total: Int,
        ink: Int,
        context: android.content.Context
    ) {
        val hasTodos = total > 0
        progressBinding.root.isVisible = hasTodos
        if (!hasTodos) return
        progressBinding.tvCompleted.text = context.getString(
            R.string.todos_completed,
            completed,
            total
        )
        progressBinding.tvCompleted.setTextColor(ink)
        NoteCardBackgroundHelper.applyTodoProgress(
            container = progressBinding.progressTodos,
            fill = progressBinding.progressFill,
            ink = ink,
            completed = completed,
            total = total
        )
    }

    private inner class TodoViewHolder(
        private val binding: ItemNoteCardTodoBinding
    ) : BaseCardViewHolder(binding.root) {

        fun bind(model: NoteCardUiModel) {
            val appearance = applyTheme(binding.cardContainer, model)
            bindHeader(binding.header, model, appearance)
            bindFooter(binding.footer, model, appearance)
            bindInteractions(binding.cardRoot, model)
            val content = model.previewContent as? TodoPreviewContent ?: return
            populateTodoRows(binding.todoContainer, content.items, appearance.textColor, appearance.backgroundColor)
            binding.tvMoreTasks.isVisible = content.remainingCount > 0
            if (content.remainingCount > 0) {
                binding.tvMoreTasks.text =
                    binding.root.context.getString(R.string.more_tasks, content.remainingCount)
            }
            bindTodoProgress(
                progressBinding = binding.todoProgress,
                completed = content.completedCount,
                total = content.totalCount,
                ink = appearance.textColor,
                context = binding.root.context
            )
            NoteCardBackgroundHelper.applyTextColors(
                appearance = appearance,
                muted = binding.tvMoreTasks
            )
        }
    }

    private inner class NumberListViewHolder(
        private val binding: ItemNoteCardNumberListBinding
    ) : BaseCardViewHolder(binding.root) {

        fun bind(model: NoteCardUiModel) {
            val appearance = applyTheme(binding.cardContainer, model)
            bindHeader(binding.header, model, appearance)
            bindFooter(binding.footer, model, appearance)
            bindInteractions(binding.cardRoot, model)
            val content = model.previewContent as? NumberListPreviewContent ?: return
            populateNumberListRows(binding.numberListContainer, content.items, appearance.mutedColor, appearance.backgroundColor)
            binding.tvMoreItems.isVisible = content.remainingCount > 0
            if (content.remainingCount > 0) {
                binding.tvMoreItems.text =
                    binding.root.context.getString(R.string.more_items, content.remainingCount)
            }
            NoteCardBackgroundHelper.applyTextColors(
                appearance = appearance,
                muted = binding.tvMoreItems
            )
        }
    }

    private inner class BulletListViewHolder(
        private val binding: ItemNoteCardBulletListBinding
    ) : BaseCardViewHolder(binding.root) {

        fun bind(model: NoteCardUiModel) {
            val appearance = applyTheme(binding.cardContainer, model)
            bindHeader(binding.header, model, appearance)
            bindFooter(binding.footer, model, appearance)
            bindInteractions(binding.cardRoot, model)
            val content = model.previewContent as? BulletListPreviewContent ?: return
            populateBulletListRows(binding.bulletListContainer, content.items, appearance.mutedColor, appearance.backgroundColor)
            binding.tvMoreItems.isVisible = content.remainingCount > 0
            if (content.remainingCount > 0) {
                binding.tvMoreItems.text =
                    binding.root.context.getString(R.string.more_items, content.remainingCount)
            }
            NoteCardBackgroundHelper.applyTextColors(
                appearance = appearance,
                muted = binding.tvMoreItems
            )
        }
    }

    private inner class ImageViewHolder(
        private val binding: ItemNoteCardImageBinding
    ) : BaseCardViewHolder(binding.root) {

        fun bind(model: NoteCardUiModel) {
            val appearance = applyTheme(binding.cardContainer, model)
            bindHeader(binding.header, model, appearance)
            bindFooter(binding.footer, model, appearance)
            bindInteractions(binding.cardRoot, model)
            val content = model.previewContent as? ImagePreviewContent ?: return
            binding.ivPreview.loadImage(content)
        }
    }

    private inner class LockedViewHolder(
        private val binding: ItemNoteCardLockedBinding
    ) : BaseCardViewHolder(binding.root) {

        fun bind(model: NoteCardUiModel) {
            val context = binding.root.context
            val cornerRadius = context.resources.getDimension(R.dimen.note_card_corner_radius)

            // Preserve the note's original background color / gradient / theme.
            applyTheme(binding.cardContainer, model)
            bindInteractions(binding.cardRoot, model)

            binding.cardContainer.clipToOutline = true
            binding.cardContainer.outlineProvider = object : android.view.ViewOutlineProvider() {
                override fun getOutline(view: View, outline: android.graphics.Outline) {
                    outline.setRoundRect(0, 0, view.width, view.height, cornerRadius)
                }
            }

            // Blur only the non-sensitive silhouette layer (API 31+). Lock UI stays sharp.
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                binding.blurredPreviewSilhouette.setRenderEffect(
                    android.graphics.RenderEffect.createBlurEffect(
                        28f,
                        28f,
                        android.graphics.Shader.TileMode.CLAMP
                    )
                )
            }

            binding.tvTitle.setText(R.string.locked_note)
            binding.tvTitle.setTextColor(ContextCompat.getColor(context, R.color.locked_note_title))
            binding.tvSubtitle.setText(R.string.locked_note_subtitle)
            binding.tvSubtitle.setTextColor(ContextCompat.getColor(context, R.color.locked_note_subtitle))
            binding.ivLock.setImageResource(R.drawable.ic_ls_lock)
            binding.ivLock.clearColorFilter()
            binding.btnMore.setOnClickListener { showContextMenu(it, model) }
        }
    }

    private inner class TextViewHolder(
        private val binding: ItemNoteCardTextBinding
    ) : BaseCardViewHolder(binding.root) {

        fun bind(model: NoteCardUiModel) {
            val appearance = applyTheme(binding.cardContainer, model)
            bindHeader(binding.header, model, appearance)
            bindFooter(binding.footer, model, appearance)
            bindInteractions(binding.cardRoot, model)
            val content = model.previewContent as? TextPreviewContent ?: return
            val previewText = content.text.trim().let { text ->
                if (text.isEmpty() || text.equals("No content", ignoreCase = true)) {
                    binding.root.context.getString(R.string.no_content)
                } else {
                    text
                }
            }
            binding.tvPreview.text = highlightText(previewText, appearance)
            NoteCardBackgroundHelper.applyTextColors(
                appearance = appearance,
                body = binding.tvPreview
            )
        }
    }

    private inner class MixedViewHolder(
        private val binding: ItemNoteCardMixedBinding
    ) : BaseCardViewHolder(binding.root) {

        fun bind(model: NoteCardUiModel) {
            val appearance = applyTheme(binding.cardContainer, model)
            bindHeader(binding.header, model, appearance)
            bindFooter(binding.footer, model, appearance)
            bindInteractions(binding.cardRoot, model)
            val content = model.previewContent as? MixedPreviewContent ?: return

            binding.todoContainer.isVisible = content.todoItems.isNotEmpty()
            if (content.todoItems.isNotEmpty()) {
                populateTodoRows(binding.todoContainer, content.todoItems, appearance.textColor, appearance.backgroundColor)
            }
            binding.tvMoreTasks.isVisible = content.remainingTodoCount > 0
            if (content.remainingTodoCount > 0) {
                binding.tvMoreTasks.text =
                    binding.root.context.getString(R.string.more_tasks, content.remainingTodoCount)
            }
            bindTodoProgress(
                progressBinding = binding.todoProgress,
                completed = content.todoCompletedCount,
                total = content.todoTotalCount,
                ink = appearance.textColor,
                context = binding.root.context
            )

            binding.badgeContainer.removeAllViews()
            if (content.numberListItems.isNotEmpty()) {
                val numberContainer = LinearLayout(binding.root.context).apply {
                    orientation = LinearLayout.VERTICAL
                }
                populateNumberListRows(numberContainer, content.numberListItems, appearance.mutedColor, appearance.backgroundColor)
                binding.badgeContainer.addView(numberContainer)
            }
            if (content.bulletListItems.isNotEmpty()) {
                val bulletContainer = LinearLayout(binding.root.context).apply {
                    orientation = LinearLayout.VERTICAL
                }
                populateBulletListRows(bulletContainer, content.bulletListItems, appearance.mutedColor, appearance.backgroundColor)
                binding.badgeContainer.addView(bulletContainer)
            }
            if (content.hasImage) {
                binding.badgeContainer.addView(
                    createBadge(
                        binding.root.context,
                        "📷 ${binding.root.context.getString(R.string.image_attached)}",
                        appearance.mutedColor
                    )
                )
            }
            if (content.hasVoice) {
                binding.badgeContainer.addView(
                    createBadge(
                        binding.root.context,
                        "🎤 ${binding.root.context.getString(R.string.voice_attached)}",
                        appearance.mutedColor
                    )
                )
            }
            if (content.hasDocument) {
                binding.badgeContainer.addView(
                    createBadge(
                        binding.root.context,
                        "📄 ${binding.root.context.getString(R.string.document_attached)}",
                        appearance.mutedColor
                    )
                )
            }

            binding.tvTextSnippet.isVisible = !content.textSnippet.isNullOrBlank()
            binding.tvTextSnippet.text = highlightText(content.textSnippet, appearance)
            NoteCardBackgroundHelper.applyTextColors(
                appearance = appearance,
                body = binding.tvTextSnippet,
                muted = binding.tvMoreTasks
            )
        }
    }

    private fun populateTodoRows(
        container: LinearLayout,
        items: List<TodoPreviewItem>,
        ink: Int,
        surfaceColor: Int
    ) {
        container.removeAllViews()
        val inflater = LayoutInflater.from(container.context)
        val palette = SearchHighlightHelper.paletteFor(surfaceColor)
        items.forEach { item ->
            val row = ItemNoteCardTodoRowBinding.inflate(inflater, container, false)
            NoteCardBackgroundHelper.tintTodoCheckbox(row.ivCheckbox, item.isCompleted, ink)
            row.tvTask.text = SearchHighlightHelper.highlight(item.text, searchQuery, palette)
            row.tvTask.setTextColor(ink)
            row.tvTask.paintFlags = if (item.isCompleted) {
                row.tvTask.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                row.tvTask.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }
            container.addView(row.root)
        }
    }

    private fun populateNumberListRows(
        container: LinearLayout,
        items: List<NumberListPreviewItem>,
        ink: Int,
        surfaceColor: Int
    ) {
        container.removeAllViews()
        val inflater = LayoutInflater.from(container.context)
        val palette = SearchHighlightHelper.paletteFor(surfaceColor)
        items.forEach { item ->
            val row = ItemNoteCardNumberListRowBinding.inflate(inflater, container, false)
            row.tvNumber.text = NumberListHelper.formatPrefix(item.number)
            row.tvNumber.setTextColor(ink)
            row.tvItem.text = SearchHighlightHelper.highlight(item.text, searchQuery, palette)
            row.tvItem.setTextColor(ink)
            container.addView(row.root)
        }
    }

    private fun populateBulletListRows(
        container: LinearLayout,
        items: List<BulletListPreviewItem>,
        ink: Int,
        surfaceColor: Int
    ) {
        container.removeAllViews()
        val inflater = LayoutInflater.from(container.context)
        val palette = SearchHighlightHelper.paletteFor(surfaceColor)
        items.forEach { item ->
            val row = ItemNoteCardBulletListRowBinding.inflate(inflater, container, false)
            row.tvBullet.setTextColor(ink)
            row.tvItem.text = SearchHighlightHelper.highlight(item.text, searchQuery, palette)
            row.tvItem.setTextColor(ink)
            container.addView(row.root)
        }
    }

    private fun createBadge(context: android.content.Context, text: String, color: Int): TextView {
        return TextView(context).apply {
            this.text = text
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodySmall)
            setTextColor(color)
            setPadding(0, 4, 0, 4)
        }
    }

    private fun ImageView.loadImage(content: ImagePreviewContent) {
        val imageUri = content.imageUri
        val placeholderRes = content.placeholderRes
        if (imageUri.isNotBlank()) {
            val file = File(imageUri)
            if (file.exists()) {
                load(file) {
                    crossfade(true)
                    placeholderRes?.let { res ->
                        placeholder(res)
                        error(res)
                    }
                }
                return
            }
        }
        if (placeholderRes != null) {
            setImageResource(placeholderRes)
        } else {
            setImageDrawable(null)
        }
    }

    private class HomeListDiffCallback : DiffUtil.ItemCallback<HomeListItem>() {
        override fun areItemsTheSame(oldItem: HomeListItem, newItem: HomeListItem): Boolean {
            return when {
                oldItem is HomeListItem.Section && newItem is HomeListItem.Section ->
                    oldItem.titleRes == newItem.titleRes
                oldItem is HomeListItem.Card && newItem is HomeListItem.Card ->
                    oldItem.model.noteId == newItem.model.noteId &&
                        oldItem.model.previewType == newItem.model.previewType
                else -> false
            }
        }

        override fun areContentsTheSame(oldItem: HomeListItem, newItem: HomeListItem): Boolean =
            oldItem == newItem
    }

    companion object {
        const val VIEW_TYPE_SECTION = 1000
        const val GRID_SPAN_COUNT = 2
        private const val PAYLOAD_SEARCH_HIGHLIGHT = "search_highlight"
        private const val PAYLOAD_TIME_FORMAT = "time_format"
        private const val PAYLOAD_DATETIME_FORMAT = "datetime_format"
        private const val PAYLOAD_SELECTION = "selection"
        private const val SELECTION_BADGE_TAG = "selection_badge"

        /**
         * Grid view uses a staggered (masonry) layout so a short card doesn't force
         * the next card in its column to start below a taller neighbour — each column
         * packs independently, matching the compact look. Section headers span the
         * full width so each section starts on a fresh row.
         *
         * Default gap handling (MOVE_ITEMS_BETWEEN_SPANS) is kept on purpose so a
         * single card (e.g. one pinned note) reflows into the left column instead of
         * being stranded in the right column with an empty slot beside it.
         */
        fun createLayoutManager(
            context: Context,
            isGridView: Boolean,
            adapter: NoteCardAdapter
        ): RecyclerView.LayoutManager {
            if (!isGridView) return LinearLayoutManager(context)
            return StaggeredGridLayoutManager(
                GRID_SPAN_COUNT,
                StaggeredGridLayoutManager.VERTICAL
            )
        }
    }
}
