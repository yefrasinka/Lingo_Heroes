package com.example.lingoheroesapp

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.lingoheroesapp.activities.LanguageLevelActivity
import com.example.lingoheroesapp.activities.RegisterActivity



class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //val intent = Intent(this, RegisterActivity::class.java)
        //startActivity(intent)



        //add  task to Firebase
        //TestActivity().loadQuestionsToFirebase()




    }
}