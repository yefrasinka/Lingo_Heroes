package com.example.lingoheroesapp

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.lingoheroesapp.activities.LanguageLevelActivity
import com.example.lingoheroesapp.activities.LoginActivity
import com.example.lingoheroesapp.activities.RegisterActivity



class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //zamieniamy Activity
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)


        //add topic,subtopic(dodaje 1 raz nowe dane)
        //TestActivity().saveSampleData()

        //add  task to Firebase robi update poprzednich danych po id
        //TestActivity().loadQuestionsToFirebase()




    }
}