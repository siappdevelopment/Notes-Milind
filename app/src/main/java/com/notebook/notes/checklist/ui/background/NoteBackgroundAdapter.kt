package com.notebook.notes.checklist.ui.background

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.graphics.ColorUtils
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.notebook.notes.checklist.R
import com.notebook.notes.checklist.databinding.ItemNoteBackgroundBinding
import com.notebook.notes.checklist.iap.FeatureUnlockDialog
import com.notebook.notes.checklist.iap.FeatureUnlockListener
import com.notebook.notes.checklist.iap.PremiumFeature
import com.notebook.notes.checklist.iap.PremiumFeatureGate
import com.notebook.notes.checklist.model.NoteBackgroundOption

private fun Context.findActivity(): Activity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return ctx as? Activity
}

class NoteBackgroundAdapter(
    private val repository: NoteBackgroundRepository,
    private val onOptionSelected: (NoteBackgroundOption) -> Unit
) : ListAdapter<NoteBackgroundOption, NoteBackgroundAdapter.ViewHolder>(DiffCallback) {

    private var selectedKey: String? = null

    fun setSelectedKey(key: String?) {
        if (selectedKey.equals(key, ignoreCase = true)) return
        val previous = selectedKey
        selectedKey = key
        currentList.forEachIndexed { index, option ->
            val wasSelected = option.key.equals(previous, ignoreCase = true)
            val isSelected = option.key.equals(key, ignoreCase = true)
            if (wasSelected || isSelected) notifyItemChanged(index, PAYLOAD_SELECTION)
        }
    }

    fun indexOfKey(key: String?): Int {
        if (key.isNullOrBlank()) return -1
        return currentList.indexOfFirst { it.key.equals(key, ignoreCase = true) }
    }

    /** Refresh lock overlays after premium purchase or session unlock (no full rebind). */
    fun refreshAccessState() {
        val count = itemCount
        if (count > 0) {
            notifyItemRangeChanged(0, count, PAYLOAD_LOCK)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemNoteBackgroundBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
        when {
            payloads.isEmpty() -> super.onBindViewHolder(holder, position, payloads)
            else -> {
                val option = getItem(position)
                if (payloads.contains(PAYLOAD_SELECTION)) {
                    holder.bindSelection(option)
                }
                if (payloads.contains(PAYLOAD_LOCK)) {
                    holder.bindLockState(option, position)
                }
            }
        }
    }

    inner class ViewHolder(
        private val binding: ItemNoteBackgroundBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val selectedStrokePx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            2.5f,
            binding.root.resources.displayMetrics
        ).toInt()

        fun bind(option: NoteBackgroundOption, position: Int) {
            when (option) {
                is NoteBackgroundOption.SolidColor -> bindSolid(option.hex)
                is NoteBackgroundOption.Asset -> bindAsset(option)
            }
            bindSelection(option)
            bindLockState(option, position)
            binding.root.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos == RecyclerView.NO_POSITION) return@setOnClickListener
                selectOption(getItem(pos), pos)
            }
        }

        fun bindSelection(option: NoteBackgroundOption) {
            val selected = option.key.equals(selectedKey, ignoreCase = true)
            binding.cardBackground.isChecked = selected
            binding.cardBackground.strokeWidth = if (selected) selectedStrokePx else 0
            binding.vSelectedScrim.isVisible = selected
            binding.ivSelected.isVisible = selected
            if (selected) {
                val sample = sampleColorFor(option)
                val contrast = ColorUtils.calculateLuminance(sample)

                binding.vSelectedScrim.setBackgroundColor(
                    if (contrast > 0.55) 0x22000000 else 0x33000000
                )
            }
        }

        fun bindLockState(option: NoteBackgroundOption, position: Int) {
            val locked = BackgroundRewardSessionManager.isLocked(option.key, position)
            binding.vLockOverlay.isVisible = locked
            binding.ivLock.isVisible = locked
            binding.root.contentDescription = if (locked) {
                binding.root.context.getString(R.string.premium_feature_title)
            } else {
                null
            }
        }

        private fun selectOption(option: NoteBackgroundOption, position: Int) {
            if (BackgroundRewardSessionManager.isAccessible(option.key, position)) {
                applyOption(option)
                return
            }

            val activity = binding.root.context.findActivity() ?: return

            if (BackgroundRewardSessionManager.canWatchReward()) {
                showFeatureUnlock(activity, option, position)
            } else {
                showPurchaseOnly(activity, option)
            }
        }

        private fun showFeatureUnlock(
            activity: Activity,
            option: NoteBackgroundOption,
            position: Int,
        ) {
            val fragmentActivity = activity as? FragmentActivity ?: run {
                showPurchaseOnly(activity, option)
                return
            }

            FeatureUnlockDialog.show(
                activity = fragmentActivity,
                feature = PremiumFeature.CUSTOM_BACKGROUND,
                showRewardOption = true,
                listener = object : FeatureUnlockListener {
                    override fun onRewardUnlock() {
                        if (!BackgroundRewardSessionManager.unlockForSession(option.key)) {
                            return
                        }
                        val index = indexOfKey(option.key).takeIf { it >= 0 } ?: position
                        notifyItemChanged(index, PAYLOAD_LOCK)
                        applyOption(option)
                    }

                    override fun onUpgrade() {

                        // Paywall opened by FeatureUnlockDialog; locks refresh on resume/premium.
                    }

                    override fun onCancel() = Unit
                },
            )
        }

        private fun showPurchaseOnly(activity: Activity, option: NoteBackgroundOption) {
            PremiumFeatureGate.requirePremium(
                activity = activity,
                feature = PremiumFeature.CUSTOM_BACKGROUND,
            ) {
                applyOption(option)
                refreshAccessState()
            }
        }

        private fun applyOption(option: NoteBackgroundOption) {
            if (!option.key.equals(selectedKey, ignoreCase = true)) {
                setSelectedKey(option.key)
            }
            onOptionSelected(option)
        }

        private fun bindSolid(hex: String) {
            binding.ivThumbnail.isVisible = false
            binding.ivThumbnail.setImageDrawable(null)
            binding.vColor.isVisible = true
            val color = runCatching { Color.parseColor(hex) }.getOrDefault(Color.LTGRAY)
            binding.vColor.background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(color)
            }
        }

        private fun bindAsset(option: NoteBackgroundOption.Asset) {
            val item = option.item
            val thumbPath = repository.thumbnailAssetPath(item)
                ?: repository.overlayAssetPath(item)
            val colors = item.bg?.gradientColors.orEmpty()
                .mapNotNull { runCatching { Color.parseColor(it) }.getOrNull() }

            if (thumbPath != null) {
                binding.vColor.isVisible = colors.isNotEmpty()
                if (colors.isNotEmpty()) {
                    binding.vColor.background = gradientDrawable(colors)
                }
                binding.ivThumbnail.isVisible = true
                binding.ivThumbnail.load(repository.assetUri(thumbPath)) {
                    crossfade(true)
                }
            } else {
                binding.ivThumbnail.isVisible = false
                binding.ivThumbnail.setImageDrawable(null)
                binding.vColor.isVisible = true
                binding.vColor.background = gradientDrawable(colors)
            }
        }

        private fun sampleColorFor(option: NoteBackgroundOption): Int {
            return when (option) {
                is NoteBackgroundOption.SolidColor ->
                    runCatching { Color.parseColor(option.hex) }.getOrDefault(Color.LTGRAY)
                is NoteBackgroundOption.Asset ->
                    option.item.bg?.gradientColors.orEmpty()
                        .firstOrNull()
                        ?.let { runCatching { Color.parseColor(it) }.getOrNull() }
                        ?: Color.GRAY
            }
        }

        private fun gradientDrawable(colors: List<Int>): GradientDrawable {
            return GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                when {
                    colors.isEmpty() -> setColor(Color.parseColor("#F5F5F5"))
                    colors.size == 1 -> setColor(colors.first())
                    else -> {
                        orientation = GradientDrawable.Orientation.TOP_BOTTOM
                        this.colors = colors.toIntArray()
                    }
                }
            }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<NoteBackgroundOption>() {
        override fun areItemsTheSame(
            oldItem: NoteBackgroundOption,
            newItem: NoteBackgroundOption
        ): Boolean = oldItem.key == newItem.key

        override fun areContentsTheSame(
            oldItem: NoteBackgroundOption,
            newItem: NoteBackgroundOption
        ): Boolean = oldItem == newItem
    }

    companion object {
        private const val PAYLOAD_SELECTION = "selection"
        private const val PAYLOAD_LOCK = "lock"
    }
}
