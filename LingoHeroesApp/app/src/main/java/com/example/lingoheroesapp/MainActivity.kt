package com.example.lingoheroesapp

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.lingoheroesapp.activities.LanguageLevelActivity
import com.example.lingoheroesapp.activities.LoginActivity
import com.example.lingoheroesapp.activities.RegisterActivity
import com.example.lingoheroesapp.activities.MainMenuActivity
import com.example.lingoheroesapp.utils.AchievementManager
import com.example.lingoheroesapp.utils.ChallengeManager
import com.example.lingoheroesapp.utils.UserDataMigration
import com.google.firebase.auth.FirebaseAuth
import com.example.lingoheroesapp.utils.NotificationManager
import com.example.lingoheroesapp.utils.AutomatedNotificationManager
import com.example.lingoheroesapp.utils.NotificationScheduler


class MainActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicjalizacja powiadomień
        NotificationManager.requestNotificationPermission(this)
        
        // Inicjalizacja menedżera automatycznych powiadomień
        AutomatedNotificationManager.init(this)
        
        // Planowanie powiadomień
        NotificationScheduler.scheduleNotifications(this)
        
        // Dodaj testowe powiadomienie
        AutomatedNotificationManager.sendTestNotification(this, "Powiadomienie testowe z MainActivity")
        
        auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser

        if (currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        } else {
            // Check and migrate user data if needed
            currentUser.uid.let { userId ->
                // Migracja wyposażenia
                UserDataMigration.migrateEquipmentData(userId)
                
                // Inicjalizacja osiągnięć
                AchievementManager.initializeAchievementsForUser(userId)
                
                // Inicjalizacja wyzwań
                ChallengeManager.createDefaultChallengesForUser(userId)
                
                // Synchronizacja osiągnięć
                AchievementManager.syncAchievements(userId)
                
                // Sprawdzenie i zresetowanie wygasłych wyzwań
                ChallengeManager.checkAndResetExpiredChallenges()

                // Subskrybuj użytkownika na powiadomienia
                AutomatedNotificationManager.subscribeUserToNotifications(userId)
            }
            
            startActivity(Intent(this, MainMenuActivity::class.java))
            finish()
        }
    }
}