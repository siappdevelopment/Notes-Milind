package com.notebook.notes.checklist.iap

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import com.notebook.notes.checklist.Advertisement.ADSRewardedDisplay
import com.notebook.notes.checklist.R
import com.notebook.notes.checklist.databinding.DialogFeatureUnlockBinding
import com.notebook.notes.checklist.util.SystemBars

/**
 * Reusable dialog for unlocking premium features via Rewarded Ads or Upgrade.
 */
class FeatureUnlockDialog : DialogFragment() {

    private var _binding: DialogFeatureUnlockBinding? = null
    private val binding get() = _binding!!

    private var listener: FeatureUnlockListener? = null
    private var feature: PremiumFeature? = null
    private var showRewardOption: Boolean = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogFeatureUnlockBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let {
            it.setBackgroundDrawableResource(android.R.color.transparent)
            it.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            SystemBars.hideNavigationBar(it)
        }
    }

    override fun onDismiss(dialog: android.content.DialogInterface) {
        super.onDismiss(dialog)
        activity?.window?.let(SystemBars::hideNavigationBar)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        feature?.let {
            binding.tvTitle.setText(it.titleRes)
            // Use custom message if provided by enum, else default
            binding.tvMessage.setText(it.messageRes)
        }

        val rewardVisibility = if (showRewardOption) View.VISIBLE else View.GONE
        binding.llOptions.visibility = rewardVisibility

        binding.btnCancel.setOnClickListener {
            listener?.onCancel()
            dismiss()
        }

        binding.btnWatchAd.setOnClickListener {
            handleWatchAd()
        }

        binding.btnUpgrade.setOnClickListener {
            handleUpgrade()
        }
    }

    private fun handleWatchAd() {
        val activity = activity ?: return
        
        if (ADSRewardedDisplay.isRewardedReady()) {
            dismiss()
            ADSRewardedDisplay.show(activity, object : ADSRewardedDisplay.OnRewardedAdsCallback {
                override fun onRewardEarned() {
                    listener?.onRewardUnlock()
                }

                override fun onFailed() {
                    showAdError()
                }

                override fun onNotAvailable() {
                    showAdError()
                }
            })
        } else {
            showAdError()
            // Try to preload for next time if not already loading
            if (!ADSRewardedDisplay.isRewardedLoading()) {
                ADSRewardedDisplay.preload(activity)
            }
        }
    }

    private fun showAdError() {
        context?.let {
            Toast.makeText(it, R.string.reward_ad_not_available, Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleUpgrade() {
        dismiss()
        listener?.onUpgrade()
        // Paywall is handled by the caller or we can start it here
        // The instructions say: "Upgrade button: PaywallActivity"
        val intent = android.content.Intent(requireContext(), PaywallActivity::class.java)
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "FeatureUnlockDialog"

        /**
         * Shows the FeatureUnlockDialog.
         *
         * @param showRewardOption when false, hides the Watch Ad option (purchase-only).
         */
        fun show(
            activity: FragmentActivity,
            feature: PremiumFeature,
            listener: FeatureUnlockListener,
            showRewardOption: Boolean = true,
        ) {
            val dialog = FeatureUnlockDialog().apply {
                this.feature = feature
                this.listener = listener
                this.showRewardOption = showRewardOption
            }
            dialog.show(activity.supportFragmentManager, TAG)
        }
    }
}
