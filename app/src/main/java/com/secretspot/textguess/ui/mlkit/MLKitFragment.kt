package com.secretspot.textguess.ui.mlkit

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.secretspot.textguess.databinding.FragmentMlkitBinding
import com.secretspot.textguess.ui.textrecognition.TextRecognitionActivity

class MLKitFragment : Fragment() {
    private var _binding: FragmentMlkitBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val homeViewModel = ViewModelProvider(this).get(MLKitViewModel::class.java)

        _binding = FragmentMlkitBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.textMlkit
        homeViewModel.text.observe(viewLifecycleOwner) {
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