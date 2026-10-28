package com.notebook.notes.checklist.ui.password

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.notebook.notes.checklist.R
import com.notebook.notes.checklist.databinding.BottomSheetSecurityQuestionBinding

class SecurityQuestionBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetSecurityQuestionBinding? = null
    private val binding get() = _binding!!

    private var onQuestionSelected: ((String) -> Unit)? = null
    private var questions: List<String> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetSecurityQuestionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        questions = arguments?.getStringArrayList(ARG_QUESTIONS) ?: emptyList()

        binding.rvQuestions.layoutManager = LinearLayoutManager(requireContext())
        binding.rvQuestions.adapter = QuestionsAdapter(questions) { question ->
            onQuestionSelected?.invoke(question)
            dismiss()
        }

        binding.btnClose.setOnClickListener { dismiss() }
    }

    override fun onStart() {
        super.onStart()
        dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            ?.setBackgroundResource(android.R.color.transparent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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

    companion object {
        private const val TAG = "SecurityQuestionBottomSheet"
        private const val ARG_QUESTIONS = "arg_questions"

        fun show(
            fragmentManager: FragmentManager,
            questions: List<String>,
            onSelected: (String) -> Unit
        ) {
            val fragment = SecurityQuestionBottomSheet().apply {
                arguments = Bundle().apply {
                    putStringArrayList(ARG_QUESTIONS, ArrayList(questions))
                }
                onQuestionSelected = onSelected
            }
            fragment.show(fragmentManager, TAG)
        }
    }
}
