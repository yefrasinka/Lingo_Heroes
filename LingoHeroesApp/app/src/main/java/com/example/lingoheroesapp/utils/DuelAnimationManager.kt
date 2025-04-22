package com.example.lingoheroesapp.utils

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.Animation
import android.view.animation.ScaleAnimation
import android.view.animation.TranslateAnimation
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.example.lingoheroesapp.R
import com.example.lingoheroesapp.models.WandType

/**
 * Klasa do zarządzania animacjami podczas pojedynków
 */
class DuelAnimationManager {

    /**
     * Animacja ataku gracza na potwora
     */
    fun animatePlayerAttack(
        playerCharacter: ImageView,
        monsterCharacter: ImageView,
        attackContainer: View,
        attackAnimation: ImageView,
        wandType: WandType,
        damageText: View,
        damage: Int,
        onAnimationEnd: () -> Unit
    ) {
        // Ustawienie obrazka ataku w zależności od typu różdżki
        val attackDrawable = when (wandType) {
            WandType.FIRE -> R.drawable.fireball
            WandType.ICE -> R.drawable.ice_shard
            WandType.LIGHTNING -> R.drawable.lightning_bolt
        }
        
        // Zastosuj obrazek do animacji
        attackAnimation.setImageResource(attackDrawable)
        
        // Pokaż kontener animacji
        attackContainer.visibility = View.VISIBLE
        attackAnimation.visibility = View.VISIBLE
        
        // Ustaw początkową pozycję (przy graczu)
        attackAnimation.translationX = -300f
        attackAnimation.translationY = 0f
        attackAnimation.alpha = 0f
        
        // Animuj pojawienie się
        val fadeIn = ObjectAnimator.ofFloat(attackAnimation, "alpha", 0f, 1f)
        fadeIn.duration = 200
        
        // Animuj ruch od gracza do potwora
        val moveRight = ObjectAnimator.ofFloat(attackAnimation, "translationX", -300f, 300f)
        moveRight.duration = 800
        moveRight.interpolator = AccelerateDecelerateInterpolator()
        
        // Dodaj efekt obracania dla ognia lub błyskawicy
        if (wandType == WandType.FIRE || wandType == WandType.LIGHTNING) {
            val rotation = ObjectAnimator.ofFloat(attackAnimation, "rotation", 0f, 360f)
            rotation.duration = 800
            rotation.start()
        }
        
        // Dodaj efekt pulsowania dla lodu
        if (wandType == WandType.ICE) {
            val scaleX = ObjectAnimator.ofFloat(attackAnimation, "scaleX", 0.8f, 1.2f, 0.8f)
            scaleX.repeatCount = ValueAnimator.INFINITE
            scaleX.duration = 500
            
            val scaleY = ObjectAnimator.ofFloat(attackAnimation, "scaleY", 0.8f, 1.2f, 0.8f)
            scaleY.repeatCount = ValueAnimator.INFINITE
            scaleY.duration = 500
            
            scaleX.start()
            scaleY.start()
        }
        
        // Zacznij animacje
        fadeIn.start()
        moveRight.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                // Potrząśnij potworem (efekt uderzenia)
                shakeView(monsterCharacter)
                
                // Pokaż tekst obrażeń
                damageText.alpha = 1f
                damageText.visibility = View.VISIBLE
                damageText.translationY = 0f
                
                // Animuj tekst obrażeń
                val textMoveUp = ObjectAnimator.ofFloat(damageText, "translationY", 0f, -100f)
                val textFadeOut = ObjectAnimator.ofFloat(damageText, "alpha", 1f, 0f)
                textMoveUp.duration = 1000
                textFadeOut.duration = 1000
                textMoveUp.start()
                textFadeOut.start()
                
                // Ukryj animację ataku
                attackAnimation.visibility = View.GONE
                attackContainer.visibility = View.GONE
                
                // Wywołaj callback po zakończeniu animacji
                Handler(Looper.getMainLooper()).postDelayed({
                    onAnimationEnd()
                }, 1000)
            }
        })
        moveRight.start()
    }
    
    /**
     * Animacja ataku potwora na gracza
     */
    fun animateMonsterAttack(
        playerCharacter: ImageView,
        monsterCharacter: ImageView,
        attackContainer: View,
        attackAnimation: ImageView,
        damageText: View,
        damage: Int,
        onAnimationEnd: () -> Unit
    ) {
        // Potwór przesuwa się do gracza
        val originalX = monsterCharacter.translationX
        val moveToPlayer = ObjectAnimator.ofFloat(monsterCharacter, "translationX", originalX, -200f)
        moveToPlayer.duration = 400
        
        // Po dotarciu do gracza, następuje uderzenie
        moveToPlayer.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                // Potrząśnij graczem (efekt uderzenia)
                shakeView(playerCharacter)
                
                // Pokaż tekst obrażeń
                damageText.alpha = 1f
                damageText.visibility = View.VISIBLE
                damageText.translationY = 0f
                
                // Animuj tekst obrażeń
                val textMoveUp = ObjectAnimator.ofFloat(damageText, "translationY", 0f, -100f)
                val textFadeOut = ObjectAnimator.ofFloat(damageText, "alpha", 1f, 0f)
                textMoveUp.duration = 1000
                textFadeOut.duration = 1000
                textMoveUp.start()
                textFadeOut.start()
                
                // Potwór wraca na swoją pozycję
                val moveBack = ObjectAnimator.ofFloat(monsterCharacter, "translationX", -200f, originalX)
                moveBack.duration = 400
                moveBack.start()
                
                // Wywołaj callback po zakończeniu animacji
                Handler(Looper.getMainLooper()).postDelayed({
                    onAnimationEnd()
                }, 1000)
            }
        })
        
        moveToPlayer.start()
    }
    
    /**
     * Animacja trzęsienia widoku (efekt uderzenia)
     */
    private fun shakeView(view: View) {
        val shake = ObjectAnimator.ofFloat(view, "translationX", 0f, 25f, -25f, 25f, -25f, 15f, -15f, 6f, -6f, 0f)
        shake.duration = 500
        shake.start()
    }
    
    /**
     * Animacja efektu specjalnych zdolności
     */
    fun animateSpecialAbility(
        effectContainer: View,
        effectImage: ImageView,
        wandType: WandType,
        onAnimationEnd: () -> Unit
    ) {
        // Ustaw obrazek efektu w zależności od typu różdżki
        val effectDrawable = when (wandType) {
            WandType.FIRE -> R.drawable.effect_fire_wall
            WandType.ICE -> R.drawable.effect_ice_armor
            WandType.LIGHTNING -> R.drawable.effect_lightning_storm
        }
        
        // Zastosuj obrazek do animacji
        effectImage.setImageResource(effectDrawable)
        
        // Pokaż kontener animacji
        effectContainer.visibility = View.VISIBLE
        effectImage.visibility = View.VISIBLE
        effectImage.alpha = 0f
        
        // Animacja pojawienia się efektu
        val fadeIn = ObjectAnimator.ofFloat(effectImage, "alpha", 0f, 1f)
        fadeIn.duration = 500
        fadeIn.start()
        
        // Animacja skalowania
        val scaleAnimation = ScaleAnimation(
            0.5f, 1.2f, 0.5f, 1.2f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        )
        scaleAnimation.duration = 1500
        effectImage.startAnimation(scaleAnimation)
        
        // Ukryj efekt po animacji
        Handler(Looper.getMainLooper()).postDelayed({
            val fadeOut = ObjectAnimator.ofFloat(effectImage, "alpha", 1f, 0f)
            fadeOut.duration = 500
            fadeOut.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    effectContainer.visibility = View.GONE
                    onAnimationEnd()
                }
            })
            fadeOut.start()
        }, 2000)
    }
    
    /**
     * Animacja wyświetlenia pytania
     */
    fun animateQuestionAppear(questionContainer: View, onAnimationEnd: () -> Unit) {
        questionContainer.alpha = 0f
        questionContainer.translationY = 100f
        questionContainer.visibility = View.VISIBLE
        
        val fadeIn = ObjectAnimator.ofFloat(questionContainer, "alpha", 0f, 1f)
        val slideUp = ObjectAnimator.ofFloat(questionContainer, "translationY", 100f, 0f)
        
        fadeIn.duration = 500
        slideUp.duration = 500
        
        fadeIn.start()
        slideUp.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                onAnimationEnd()
            }
        })
        slideUp.start()
    }
    
    /**
     * Animacja ukrycia pytania
     */
    fun animateQuestionDisappear(questionContainer: View, onAnimationEnd: () -> Unit) {
        val fadeOut = ObjectAnimator.ofFloat(questionContainer, "alpha", 1f, 0f)
        val slideDown = ObjectAnimator.ofFloat(questionContainer, "translationY", 0f, 100f)
        
        fadeOut.duration = 300
        slideDown.duration = 300
        
        fadeOut.start()
        slideDown.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                questionContainer.visibility = View.GONE
                onAnimationEnd()
            }
        })
        slideDown.start()
    }
    
    /**
     * Animacja podświetlenia poprawnej odpowiedzi
     */
    fun animateCorrectAnswer(button: View) {
        val originalBackground = button.background
        button.setBackgroundResource(R.drawable.button_correct_answer) // Potrzebujesz utworzyć/dodać ten zasób
        
        val scaleX = ObjectAnimator.ofFloat(button, "scaleX", 1f, 1.1f, 1f)
        val scaleY = ObjectAnimator.ofFloat(button, "scaleY", 1f, 1.1f, 1f)
        
        scaleX.duration = 500
        scaleY.duration = 500
        
        scaleX.start()
        scaleY.start()
        
        // Przywróć oryginalny background po animacji
        Handler(Looper.getMainLooper()).postDelayed({
            button.background = originalBackground
        }, 1500)
    }
    
    /**
     * Animacja podświetlenia niepoprawnej odpowiedzi
     */
    fun animateIncorrectAnswer(button: View) {
        val originalBackground = button.background
        button.setBackgroundResource(R.drawable.button_incorrect_answer) // Potrzebujesz utworzyć/dodać ten zasób
        
        val shakeAnimation = ObjectAnimator.ofFloat(button, "translationX", 0f, 10f, -10f, 10f, -10f, 5f, -5f, 0f)
        shakeAnimation.duration = 500
        shakeAnimation.start()
        
        // Przywróć oryginalny background po animacji
        Handler(Looper.getMainLooper()).postDelayed({
            button.background = originalBackground
        }, 1500)
    }
} 