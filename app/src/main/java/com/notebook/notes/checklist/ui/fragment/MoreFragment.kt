package com.notebook.notes.checklist.ui.fragment

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.google.android.material.card.MaterialCardView
import com.notebook.notes.checklist.R
import com.notebook.notes.checklist.databinding.FragmentMoreBinding
import com.notebook.notes.checklist.helper.PreferenceHelper
import com.notebook.notes.checklist.ui.activity.EditLabelActivity
import com.notebook.notes.checklist.ui.activity.FeedbackActivity
import com.notebook.notes.checklist.ui.activity.HomeActivity
import com.notebook.notes.checklist.ui.activity.NoteBackgroundPickerActivity
import com.notebook.notes.checklist.ui.activity.RecycleBinActivity
import com.notebook.notes.checklist.utils.Constant
import com.notebook.notes.checklist.viewmodel.utils.launchActivity
import com.notebook.notes.checklist.viewmodel.utils.openWithHomeInterAd
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MoreFragment : Fragment() {

    @Inject
    lateinit var preferenceHelper: PreferenceHelper

    private lateinit var binding: FragmentMoreBinding
    private var selectedStrokePx = 0

    private val backgroundPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // Default is persisted inside NoteBackgroundPickerActivity when saveAsDefault=true.
        refreshThemeSelection()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMoreBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        selectedStrokePx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            SELECTED_STROKE_DP,
            resources.displayMetrics
        ).toInt()
        setupClickListeners()
        refreshThemeSelection()
    }

    override fun onResume() {
        super.onResume()
        if (::binding.isInitialized) {
            refreshThemeSelection()
        }
    }

    private fun setupClickListeners() {
        binding.btnMoreMenu.setOnClickListener {
            (activity as? HomeActivity)?.openDrawer()
        }
        binding.cardArchive.setOnClickListener {
            (activity as? HomeActivity)?.openArchiveTab()
        }
        binding.cardEditLabels.setOnClickListener {
            openWithHomeInterAd {
                requireActivity().launchActivity<EditLabelActivity>()
            }
        }
        binding.cardTrashBin.setOnClickListener {
            openWithHomeInterAd {
                requireActivity().launchActivity<RecycleBinActivity>()
            }
        }
        binding.cardSyncBackup.setOnClickListener {

        }
        binding.cardRateUs.setOnClickListener {
            com.notebook.notes.checklist.ui.dialog.RatingBottomSheet().apply {
                setOnRatingSubmittedListener { rating ->
                    if (rating >= 4) {
                        openPlayStore()
                    } else {
                        openWithHomeInterAd {
                            requireActivity().launchActivity<FeedbackActivity>()
                        }
                    }
                }
            }.show(requireActivity().supportFragmentManager, com.notebook.notes.checklist.ui.dialog.RatingBottomSheet.TAG)
        }
        binding.cardShareApp.setOnClickListener {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name))
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Check out this app:\nhttps://play.google.com/store/apps/details?id=$`package`"
                )
            }
            startActivity(Intent.createChooser(shareIntent, "Share App"))
        }

        binding.tvseeMore.setOnClickListener {
            val selectedKey = currentDefaultBackgroundKey()
            openWithHomeInterAd {
                backgroundPickerLauncher.launch(
                    NoteBackgroundPickerActivity.createIntent(
                        context = requireContext(),
                        selectedKey = selectedKey,
                        saveAsDefault = true
                    )
                )
            }
        }

        binding.themePurple.setOnClickListener { selectTheme(THEME_PURPLE) }
        binding.themePeach.setOnClickListener { selectTheme(THEME_PEACH) }
        binding.themeTeal.setOnClickListener { selectTheme(THEME_TEAL) }
    }

    private fun selectTheme(colorKey: String) {
        preferenceHelper.putString(Constant.PREF_DEFAULT_NOTE_BACKGROUND, colorKey)
        refreshThemeSelection()
    }
    private fun openPlayStore() {
        val packageName = requireActivity().packageName

        try {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
                )
            )
        } catch (e: Exception) {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=$packageName")
                )
            )
        }
    }

    private fun refreshThemeSelection() {
        val selectedKey = currentDefaultBackgroundKey()
        bindThemeCard(
            card = binding.themePurple,
            check = binding.ivThemePurpleCheck,
            selected = selectedKey.equals(THEME_PURPLE, ignoreCase = true)
        )
        bindThemeCard(
            card = binding.themePeach,
            check = binding.ivThemePeachCheck,
            selected = selectedKey.equals(THEME_PEACH, ignoreCase = true)
        )
        bindThemeCard(
            card = binding.themeTeal,
            check = binding.ivThemeTealCheck,
            selected = selectedKey.equals(THEME_TEAL, ignoreCase = true)
        )
    }

    private fun bindThemeCard(
        card: MaterialCardView,
        check: ImageView,
        selected: Boolean
    ) {
        check.isVisible = selected
        card.strokeWidth = if (selected) selectedStrokePx else 0
    }

    private fun currentDefaultBackgroundKey(): String? {
        return preferenceHelper
            .getString(Constant.PREF_DEFAULT_NOTE_BACKGROUND)
            ?.takeIf { it.isNotBlank() }
    }

    companion object {
        private const val SELECTED_STROKE_DP = 1.5f

        /** Preview colors in fragment_more — stored as default note background keys. */
        private const val THEME_PURPLE = "#E8E4F8"
        private const val THEME_PEACH = "#FFE5D9"
        private const val THEME_TEAL = "#D9F5E5"
    }
}
