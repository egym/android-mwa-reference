package com.ionicexample.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.main_activity)

        findViewById<Button>(R.id.btnOpenBioage).setOnClickListener {
            startActivity(Intent(this, IonicSampleActivity::class.java))
        }
    }
}
