package com.secretspot.textguess.ui.cloudvision

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.secretspot.textguess.databinding.FragmentCloudvisionBinding
import com.secretspot.textguess.ui.textrecognition.TextRecognitionActivity

class CloudVisionFragment : Fragment() {
    private var _binding: FragmentCloudvisionBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val dashboardViewModel = ViewModelProvider(this).get(CloudVisionViewModel::class.java)

        _binding = FragmentCloudvisionBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.textCloudvision
        dashboardViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }

        binding.btnTextRecognition.setOnClickListener {
            startActivity(Intent(requireContext(), TextRecognitionActivity::class.java))
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}