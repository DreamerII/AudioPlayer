package com.dreamfire.bookdemo.presentation

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.dreamfire.bookdemo.R

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, PlayerFragment())
                .commit()
        }
    }
}