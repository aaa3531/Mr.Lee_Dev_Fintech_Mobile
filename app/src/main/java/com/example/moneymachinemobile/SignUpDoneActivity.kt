package com.example.moneymachinemobile

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class SignUpDoneActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up_done)

        val ab = supportActionBar!!
        ab.title = "회원가입 완료"
        ab.setDisplayShowTitleEnabled(true)

        val button = findViewById<Button>(R.id.ButtonBack)
        button.setOnClickListener{
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}