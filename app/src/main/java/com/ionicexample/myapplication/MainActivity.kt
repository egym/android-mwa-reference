package com.ionicexample.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val intent = Intent(this, IonicSampleActivity::class.java);

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.main_activity)

        findViewById<Button>(R.id.btnOpenBioage).setOnClickListener {
            intent.putExtra("app", "bioage")
            startActivity(intent)
        }

        findViewById<Button>(R.id.btnOpenWorkouts).setOnClickListener {
            intent.putExtra("app", "workouts")
            startActivity(intent)
        }
    }
}
