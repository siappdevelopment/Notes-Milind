package com.notebook.notes.checklist.ui.password

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.notebook.notes.checklist.R
import com.notebook.notes.checklist.databinding.PopupSecurityQuestionsBinding

object SecurityQuestionPopup {

    fun show(
        context: Context,
        anchor: View,
        questions: List<String>,
        onQuestionSelected: (String) -> Unit
    ) {
        val binding = PopupSecurityQuestionsBinding.inflate(LayoutInflater.from(context))
        val density = context.resources.displayMetrics.density
        val minWidth = (220 * density).toInt()
        val popupWidth = maxOf(anchor.width, minWidth)
        val maxHeight = (280 * density).toInt()

        lateinit var popup: PopupWindow
        popup = PopupWindow(
            binding.root,
            popupWidth,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            elevation = 12f
            isOutsideTouchable = true
            setBackgroundDrawable(
                ContextCompat.getDrawable(context, android.R.color.transparent)
            )
        }

        binding.rvQuestions.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = QuestionsAdapter(questions) { question ->
                onQuestionSelected(question)
                popup.dismiss()
            }
        }

        val yOffset = (4 * density).toInt()

        anchor.post {
            binding.root.measure(
                View.MeasureSpec.makeMeasureSpec(popupWidth, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )

            if (binding.rvQuestions.height > maxHeight) {
                binding.rvQuestions.layoutParams = binding.rvQuestions.layoutParams.apply {
                    height = maxHeight
                }
            }

            val measuredPopupWidth = binding.root.measuredWidth
            val location = IntArray(2)
            anchor.getLocationOnScreen(location)
            val screenWidth = context.resources.displayMetrics.widthPixels

            var xOff = 0
            if (location[0] + measuredPopupWidth > screenWidth) {
                xOff = screenWidth - location[0] - measuredPopupWidth - (16 * density).toInt()
            }

            popup.showAsDropDown(anchor, xOff, yOffset)
        }
    }

    private class QuestionsAdapter(
        private val items: List<String>,
        private val onItemClick: (String) -> Unit
    ) : RecyclerView.Adapter<QuestionsAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvQuestion: TextView = view.findViewById(R.id.tvQuestion)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_security_question, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.tvQuestion.text = item
            holder.itemView.setOnClickListener { onItemClick(item) }
        }

        override fun getItemCount() = items.size
    }
}
