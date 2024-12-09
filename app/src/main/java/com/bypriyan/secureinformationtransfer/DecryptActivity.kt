package com.bypriyan.secureinformationtransfer

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bypriyan.secureinformationtransfer.databinding.ActivityDecryptBinding
import java.io.InputStream

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

        binding.algorithmSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    selectedAlgorithm = algorithms[position]
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

        // Set up image picker
        pickImageLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK && result.data != null) {
                    selectedImageUri = result.data?.data
                    binding.selecterIng.setImageURI(selectedImageUri)
                    binding.lin.visibility = View.GONE
                }
            }

        // Button to select an image
        binding.selectImg.setOnClickListener { openGallery() }

        binding.decryptBtn.setOnClickListener {
            decryptImage()
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
        }
        pickImageLauncher.launch(intent)
    }

    private fun decryptImage() {
        if (selectedImageUri == null) {
            Toast.makeText(this, "Please select an image to decrypt", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val inputStream: InputStream? = contentResolver.openInputStream(selectedImageUri!!)
            val decryptedMessage = when (selectedAlgorithm) {
                "LSB" -> decryptUsingLSB(inputStream)
                "DCT" -> decryptUsingDCT(inputStream)
                else -> throw IllegalArgumentException("Unknown algorithm selected")
            }

            Log.d("Decryption", "Decrypted Message: $decryptedMessage")
            binding.msgEt.setText(decryptedMessage.split("æ")[0])
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to decrypt the image: ${e.message}", Toast.LENGTH_SHORT)
                .show()
        }
    }

    private fun decryptUsingLSB(inputStream: InputStream?): String {
        if (inputStream == null) throw IllegalArgumentException("Input stream cannot be null")

        val bitmap = BitmapFactory.decodeStream(inputStream)
        val width = bitmap.width
        val height = bitmap.height

        val binaryData = StringBuilder()

        loop@ for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = bitmap.getPixel(x, y)
                val red = pixel shr 16 and 0xFF
                val lsb = red and 1
                binaryData.append(lsb)

                // Check if the last 8 bits form the termination marker
                if (binaryData.length % 8 == 0) {
                    val lastByte = binaryData.substring(binaryData.length - 8, binaryData.length)
                    if (lastByte == "00000000") break@loop
                }
            }
        }

        // Convert binary data to string
        val message = StringBuilder()
        for (i in 0 until binaryData.length step 8) {
            val byteStr = binaryData.substring(i, i + 8)
            if (byteStr == "00000000") break // Stop at termination marker
            val char = byteStr.toInt(2).toChar()
            message.append(char)
        }

        return message.toString()
    }

    private fun decryptUsingDCT(inputStream: InputStream?): String {
        if (inputStream == null) throw IllegalArgumentException("Input stream cannot be null")

        val bitmap = BitmapFactory.decodeStream(inputStream)
        val width = bitmap.width
        val height = bitmap.height

        val binaryData = StringBuilder()

        val blockSize = 8
        loop@ for (y in 0 until height step blockSize) {
            for (x in 0 until width step blockSize) {
                val block = Array(blockSize) { DoubleArray(blockSize) }
                for (i in 0 until blockSize) {
                    for (j in 0 until blockSize) {
                        if (x + j < width && y + i < height) {
                            val pixel = bitmap.getPixel(x + j, y + i)
                            val gray = (pixel shr 16 and 0xFF) * 0.299 +
                                    (pixel shr 8 and 0xFF) * 0.587 +
                                    (pixel and 0xFF) * 0.114
                            block[i][j] = gray
                        }
                    }
                }

                val dctCoefficients = performDCT(block)
                val bit = (dctCoefficients[1][1] % 2).toInt()
                binaryData.append(bit)

                // Check for termination marker
                if (binaryData.length % 8 == 0) {
                    val lastByte = binaryData.substring(binaryData.length - 8, binaryData.length)
                    if (lastByte == "00000000") break@loop
                }
            }
        }

        val message = StringBuilder()
        for (i in 0 until binaryData.length step 8) {
            val byteStr = binaryData.substring(i, i + 8)
            if (byteStr == "00000000") break
            val char = byteStr.toInt(2).toChar()
            message.append(char)
        }

        return message.toString()
    }

    private fun performDCT(block: Array<DoubleArray>): Array<DoubleArray> {
        val N = block.size
        val dctBlock = Array(N) { DoubleArray(N) }
        for (u in 0 until N) {
            for (v in 0 until N) {
                var sum = 0.0
                for (x in 0 until N) {
                    for (y in 0 until N) {
                        val coefficient = block[x][y] *
                                Math.cos((2 * x + 1) * u * Math.PI / (2 * N)) *
                                Math.cos((2 * y + 1) * v * Math.PI / (2 * N))
                        sum += coefficient
                    }
                }
                val cu = if (u == 0) 1.0 / Math.sqrt(2.0) else 1.0
                val cv = if (v == 0) 1.0 / Math.sqrt(2.0) else 1.0
                dctBlock[u][v] = 0.25 * cu * cv * sum
            }
        }
        return dctBlock
    }
}
