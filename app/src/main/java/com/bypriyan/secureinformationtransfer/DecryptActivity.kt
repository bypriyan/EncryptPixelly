package com.bypriyan.secureinformationtransfer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bypriyan.secureinformationtransfer.databinding.ActivityDecryptBinding

class DecryptActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDecryptBinding
    private lateinit var pickImageLauncher: ActivityResultLauncher<Intent>
    private var selectedImageUri: Uri? = null
    private var selectedAlgorithm: String = "LSB"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDecryptBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up algorithm spinner
        val algorithms = listOf("LSB", "DCT")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, algorithms)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.algorithmSpinner.adapter = adapter

        binding.algorithmSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                selectedAlgorithm = algorithms[position]
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Set up image picker
        pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK && result.data != null) {
                selectedImageUri = result.data?.data
                binding.selecterIng.setImageURI(selectedImageUri)
                binding.lin.visibility = View.GONE
            }
        }

        // Button to select an image
        binding.selectImg.setOnClickListener { openGallery() }

        binding.decryptBtn.setOnClickListener{

        }

    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
        }
        pickImageLauncher.launch(intent)
    }

}