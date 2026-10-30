package com.lifepath.app

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class ServerConfigActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(Intent(this, AuthActivity::class.java))
        finish()
    }
}
