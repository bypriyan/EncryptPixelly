package com.bypriyan.secureinformationtransfer

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bypriyan.secureinformationtransfer.databinding.ActivityEncryptBinding
import java.io.OutputStream

class EncryptActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEncryptBinding
    private lateinit var pickImageLauncher: ActivityResultLauncher<Intent>
    private var selectedImageUri: Uri? = null
    private var selectedAlgorithm: String = "LSB"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize View Binding
        binding = ActivityEncryptBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up algorithm spinner
        val algorithms = listOf("LSB", "DCT", "Custom Algorithm 1", "Custom Algorithm 2")
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
            }
        }

        // Button to select an image
        binding.selectImg.setOnClickListener { openGallery() }

        // Button to embed data into the image
        binding.encryptBtn.setOnClickListener { checkPermissionsAndEmbedData() }
    }

    // Open gallery to select an image
    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
        }
        pickImageLauncher.launch(intent)
    }

    // Check permissions and embed data
    private fun checkPermissionsAndEmbedData() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    100
                )
                return
            }
        }
        embedData()
    }

    // Embed data into the image
    private fun embedData() {
        if (selectedImageUri == null) {
            Toast.makeText(this, "Please select an image first", Toast.LENGTH_SHORT).show()
            return
        }

        val secretData = binding.msgEt.text.toString()
        if (secretData.isEmpty()) {
            Toast.makeText(this, "Please enter data to hide", Toast.LENGTH_SHORT).show()
            return
        }

        val inputStream = contentResolver.openInputStream(selectedImageUri!!)
        val bitmap = BitmapFactory.decodeStream(inputStream)

        if (bitmap != null) {
            val embeddedBitmap = when (selectedAlgorithm) {
                "LSB" -> embedUsingLSB(bitmap, secretData)
                "DCT" -> embedUsingDCT(bitmap, secretData)
                else -> bitmap // Placeholder for custom algorithms
            }

            saveEmbeddedImage(embeddedBitmap)
            Toast.makeText(this, "Data embedded successfully using $selectedAlgorithm", Toast.LENGTH_SHORT).show()
            isLoading(false)
        }
    }

    // Embed data using LSB algorithm
    private fun embedUsingLSB(bitmap: Bitmap, data: String): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val resultBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)

        val binaryData = data.toByteArray(Charsets.UTF_8)
            .joinToString("") { it.toString(2).padStart(8, '0') }
        var dataIndex = 0

        loop@ for (y in 0 until height) {
            for (x in 0 until width) {
                if (dataIndex >= binaryData.length) break@loop

                val pixel = bitmap.getPixel(x, y)
                val red = pixel shr 16 and 0xFF
                val newRed = (red and 0xFE) or (binaryData[dataIndex].digitToInt())
                val newPixel = (pixel and 0xFF00FFFF.toInt()) or (newRed shl 16)

                resultBitmap.setPixel(x, y, newPixel)
                dataIndex++
            }
        }
        return resultBitmap
    }

    // Placeholder for DCT algorithm
    private fun embedUsingDCT(bitmap: Bitmap, data: String): Bitmap {
        // Implement DCT embedding logic here
        return bitmap
    }

    // Save the embedded image
    private fun saveEmbeddedImage(bitmap: Bitmap) {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "embedded_image_${System.currentTimeMillis()}.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        }

        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        if (uri != null) {
            val outputStream: OutputStream? = contentResolver.openOutputStream(uri)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream!!)
            outputStream?.close()
        }
    }

    fun isLoading(isLoading: Boolean){
        if (isLoading){
            binding.progressbar.visibility = View.VISIBLE
            binding.encryptBtn.visibility = View.GONE
        }else{
            binding.progressbar.visibility = View.GONE
            binding.encryptBtn.visibility = View.VISIBLE
        }
    }

}
