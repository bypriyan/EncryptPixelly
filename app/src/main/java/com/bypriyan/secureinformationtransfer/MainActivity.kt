package com.bypriyan.secureinformationtransfer

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bypriyan.secureinformationtransfer.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.encryptBtn.setOnClickListener {
            startActivity(Intent(this, EncryptActivity::class.java))
        }

        binding.decryptBtn.setOnClickListener {
            startActivity(Intent(this, DecryptActivity::class.java))
        }


    }
}