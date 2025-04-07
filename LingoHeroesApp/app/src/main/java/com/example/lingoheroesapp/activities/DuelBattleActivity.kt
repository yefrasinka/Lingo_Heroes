package com.example.lingoheroesapp.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.example.lingoheroesapp.R
import com.example.lingoheroesapp.models.Character
import com.example.lingoheroesapp.models.ElementType
import com.example.lingoheroesapp.models.Enemy
import com.example.lingoheroesapp.models.Question
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.google.firebase.database.ServerValue
import java.util.*
import kotlin.collections.ArrayList

class DuelBattleActivity : AppCompatActivity() {

    // UI Components
    private lateinit var questionText: TextView
    private lateinit var answerButtons: List<Button>
    private lateinit var playerHealthBar: ProgressBar
    private lateinit var opponentHealthBar: ProgressBar
    private lateinit var timerProgressBar: ProgressBar
    private lateinit var playerScoreText: TextView
    private lateinit var opponentScoreText: TextView
    private lateinit var playerDamageText: TextView
    private lateinit var opponentDamageText: TextView
    private lateinit var feedbackText: TextView
    private lateinit var playerAvatar: ImageView
    private lateinit var opponentAvatar: ImageView
    private lateinit var playerElementBadge: ImageView
    private lateinit var opponentElementBadge: ImageView
    private lateinit var playerName: TextView
    private lateinit var opponentName: TextView
    private lateinit var resultCard: CardView
    private lateinit var resultText: TextView
    private lateinit var continueButton: Button
    private lateinit var specialAbilityButton: Button
    
    // Element effectiveness UI
    private lateinit var elementEffectivenessContainer: LinearLayout
    private lateinit var attackerElementIcon: ImageView
    private lateinit var defenderElementIcon: ImageView
    private lateinit var effectivenessText: TextView
    
    // Element effect animation
    private lateinit var elementEffectContainer: FrameLayout
    private lateinit var elementEffectImage: ImageView
    
    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private var currentUser: FirebaseUser? = null
    
    // Game variables
    private val questions = ArrayList<Question>()
    private var currentQuestionIndex = 0
    private var playerHealth = 100
    private var opponentHealth = 100
    private var playerScore = 0
    private var opponentScore = 0
    private var correctAnswerIndex = 0
    private var isAnswerSelected = false
    private var timer: CountDownTimer? = null
    private val random = Random()
    
    // Character and enemy data
    private lateinit var playerCharacter: Character
    private lateinit var enemyCharacter: Enemy
    
    // Special ability variables
    private var specialAbilityCooldown = 0
    private var isSpecialAbilityAvailable = false
    
    // Stage variables
    private var stageNumber: Int = 1
    private var stageCompleted = false
    private var correctAnswers = 0
    private var totalAnswers = 0
    private var totalDamageDealt = 0
    private var totalDamageTaken = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_duel_battle)
        
        // Get stage number from intent
        stageNumber = intent.getIntExtra("STAGE_NUMBER", 1)
        
        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        currentUser = auth.currentUser
        
        // Initialize UI components
        initViews()
        
        // Load player character
        loadPlayerCharacter()
        
        // Load enemy character for the stage
        loadEnemyForStage()
    }
    
    private fun initViews() {
        questionText = findViewById(R.id.questionText)
        answerButtons = listOf(
            findViewById(R.id.answerButton1),
            findViewById(R.id.answerButton2),
            findViewById(R.id.answerButton3),
            findViewById(R.id.answerButton4)
        )
        playerHealthBar = findViewById(R.id.playerHealthBar)
        opponentHealthBar = findViewById(R.id.opponentHealthBar)
        timerProgressBar = findViewById(R.id.timerProgressBar)
        playerScoreText = findViewById(R.id.playerScoreText)
        opponentScoreText = findViewById(R.id.opponentScoreText)
        playerDamageText = findViewById(R.id.playerDamageText)
        opponentDamageText = findViewById(R.id.opponentDamageText)
        feedbackText = findViewById(R.id.feedbackText)
        playerAvatar = findViewById(R.id.playerAvatar)
        opponentAvatar = findViewById(R.id.opponentAvatar)
        playerElementBadge = findViewById(R.id.playerElementBadge)
        opponentElementBadge = findViewById(R.id.opponentElementBadge)
        playerName = findViewById(R.id.playerName)
        opponentName = findViewById(R.id.opponentName)
        resultCard = findViewById(R.id.resultCard)
        resultText = findViewById(R.id.resultText)
        continueButton = findViewById(R.id.continueButton)
        specialAbilityButton = findViewById(R.id.specialAbilityButton)
        
        // Element effectiveness UI
        elementEffectivenessContainer = findViewById(R.id.elementEffectivenessContainer)
        attackerElementIcon = findViewById(R.id.attackerElementIcon)
        defenderElementIcon = findViewById(R.id.defenderElementIcon)
        effectivenessText = findViewById(R.id.effectivenessText)
        
        // Element effect animation
        elementEffectContainer = findViewById(R.id.elementEffectContainer)
        elementEffectImage = findViewById(R.id.elementEffectImage)
        
        // Set up answer button click listeners
        answerButtons.forEachIndexed { index, button ->
            button.setOnClickListener {
                handleAnswerSelection(index)
            }
        }
        
        // Set up continue button
        continueButton.setOnClickListener {
            returnToDuelsActivity()
        }
        
        // Set up special ability button
        specialAbilityButton.setOnClickListener {
            useSpecialAbility()
        }
        
        // Initialize UI state
        resultCard.visibility = View.GONE
        feedbackText.visibility = View.INVISIBLE
        playerDamageText.visibility = View.INVISIBLE
        opponentDamageText.visibility = View.INVISIBLE
        elementEffectivenessContainer.visibility = View.GONE
        elementEffectContainer.visibility = View.GONE
        
        // Initialize health bars
        playerHealthBar.max = 100
        opponentHealthBar.max = 100
        playerHealthBar.progress = playerHealth
        opponentHealthBar.progress = opponentHealth
        
        // Initialize score texts
        playerScoreText.text = "0"
        opponentScoreText.text = "0"
    }
    
    private fun loadPlayerCharacter() {
        if (currentUser == null) {
            // Użyj domyślnej postaci, jeśli użytkownik nie jest zalogowany
            createDefaultPlayerCharacter()
            return
        }
        
        val characterRef = database.reference
            .child("users")
            .child(currentUser!!.uid)
            .child("character")
        
        characterRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    // Pobierz postać gracza z Firebase
                    playerCharacter = snapshot.getValue(Character::class.java)
                        ?: createDefaultPlayerCharacter()
                } else {
                    // Utwórz domyślną postać, jeśli nie ma jej w bazie
                    createDefaultPlayerCharacter()
                    // Zapisz domyślną postać do bazy
                    characterRef.setValue(playerCharacter)
                }
                
                // Aktualizuj UI postaci gracza
                updatePlayerCharacterUI()
                
                // Załaduj pytania po załadowaniu postaci
                loadQuestions()
            }
            
            override fun onCancelled(error: DatabaseError) {
                Log.e("DuelBattleActivity", "Błąd ładowania postaci: ${error.message}")
                createDefaultPlayerCharacter()
                updatePlayerCharacterUI()
                loadQuestions()
            }
        })
    }
    
    private fun createDefaultPlayerCharacter(): Character {
        // Losowy typ elementu dla nowego gracza
        val elementTypes = ElementType.values()
        val randomElement = elementTypes[random.nextInt(elementTypes.size)]
        
        playerCharacter = Character(
            id = "player_default",
            name = currentUser?.displayName ?: "Gracz",
            element = randomElement,
            imageResId = R.drawable.ic_player_avatar,
            baseAttack = 12,
            baseDefense = 8,
            specialAbilityName = when (randomElement) {
                ElementType.FIRE -> "Ściana Ognia"
                ElementType.ICE -> "Lodowa Zbroja"
                ElementType.LIGHTNING -> "Porażenie"
            },
            specialAbilityDescription = when (randomElement) {
                ElementType.FIRE -> "Zadaje większe obrażenia przeciwnikowi"
                ElementType.ICE -> "Zmniejsza otrzymywane obrażenia"
                ElementType.LIGHTNING -> "Zwiększa precyzję odpowiedzi"
            },
            specialAbilityCooldown = 3
        )
        
        return playerCharacter
    }
    
    private fun loadEnemyForStage() {
        val enemyRef = database.reference
            .child("duelStages")
            .child(stageNumber.toString())
            .child("enemy")
        
        enemyRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    // Pobierz przeciwnika z Firebase
                    enemyCharacter = snapshot.getValue(Enemy::class.java)
                        ?: createDefaultEnemyForStage()
                } else {
                    // Utwórz domyślnego przeciwnika, jeśli nie ma go w bazie
                    enemyCharacter = createDefaultEnemyForStage()
                    // Zapisz domyślnego przeciwnika do bazy
                    enemyRef.setValue(enemyCharacter)
                }
                
                // Aktualizuj UI przeciwnika
                updateEnemyUI()
                
                // Aktualizuj wskaźnik efektywności elementów
                updateElementEffectivenessUI()
            }
            
            override fun onCancelled(error: DatabaseError) {
                Log.e("DuelBattleActivity", "Błąd ładowania przeciwnika: ${error.message}")
                enemyCharacter = createDefaultEnemyForStage()
                updateEnemyUI()
                updateElementEffectivenessUI()
            }
        })
    }
    
    private fun createDefaultEnemyForStage(): Enemy {
        // Losowy typ elementu dla przeciwnika, ale z preferencją dla typów, które są słabsze 
        // przeciwko typowi gracza (aby dać przewagę graczowi)
        val elementTypes = ElementType.values()
        val playerElement = playerCharacter.element
        
        // Znajdź typ, który jest słabszy przeciwko typowi gracza
        val weakerElement = when (playerElement) {
            ElementType.FIRE -> ElementType.ICE
            ElementType.ICE -> ElementType.LIGHTNING
            ElementType.LIGHTNING -> ElementType.FIRE
        }
        
        // 70% szans na wybranie słabszego typu, 30% na losowy
        val enemyElement = if (random.nextFloat() < 0.7f) {
            weakerElement
        } else {
            elementTypes[random.nextInt(elementTypes.size)]
        }
        
        val enemyNames = arrayOf(
            "Ognisty Golem", "Lodowy Gigant", "Burza Błyskawic",
            "Piekielny Strażnik", "Zamrożony Demon", "Elektryczny Duszek",
            "Magma", "Mróz", "Iskra"
        )
        
        val nameIndex = when (enemyElement) {
            ElementType.FIRE -> 0 + (stageNumber - 1) % 3
            ElementType.ICE -> 1 + (stageNumber - 1) % 3
            ElementType.LIGHTNING -> 2 + (stageNumber - 1) % 3
        }
        
        val name = enemyNames[nameIndex]
        
        // Skalowanie statystyk przeciwnika w zależności od poziomu etapu
        val baseHp = 80 + (stageNumber * 10)
        val baseAttack = 6 + (stageNumber)
        val baseDefense = 2 + (stageNumber / 2)
        
        return Enemy(
            id = "enemy_stage_$stageNumber",
            name = name,
            element = enemyElement,
            imageResId = when (enemyElement) {
                ElementType.FIRE -> R.drawable.ic_fire_enemy
                ElementType.ICE -> R.drawable.ic_ice_enemy
                ElementType.LIGHTNING -> R.drawable.ic_lightning_enemy
            },
            hp = baseHp,
            attack = baseAttack,
            defense = baseDefense,
            description = "Przeciwnik poziomu $stageNumber",
            stageId = stageNumber
        )
    }
    
    private fun updatePlayerCharacterUI() {
        // Aktualizuj UI postaci gracza
        playerName.text = playerCharacter.name
        
        // Ustaw ikonę elementu
        val elementDrawableId = when (playerCharacter.element) {
            ElementType.FIRE -> R.drawable.ic_fire
            ElementType.ICE -> R.drawable.ic_ice
            ElementType.LIGHTNING -> R.drawable.ic_lightning
        }
        playerElementBadge.setImageResource(elementDrawableId)
        
        // Aktualizuj tekst specjalnej zdolności
        updateSpecialAbilityButtonText()
    }
    
    private fun updateEnemyUI() {
        // Aktualizuj UI przeciwnika
        opponentName.text = enemyCharacter.name
        
        // Ustaw ikonę elementu
        val elementDrawableId = when (enemyCharacter.element) {
            ElementType.FIRE -> R.drawable.ic_fire
            ElementType.ICE -> R.drawable.ic_ice
            ElementType.LIGHTNING -> R.drawable.ic_lightning
        }
        opponentElementBadge.setImageResource(elementDrawableId)
        
        // Możemy dostosować awatar przeciwnika, jeśli mamy odpowiednie zasoby
        if (enemyCharacter.imageResId != 0) {
            opponentAvatar.setImageResource(enemyCharacter.imageResId)
        }
    }
    
    private fun updateElementEffectivenessUI() {
        // Pokaż wskaźnik efektywności elementów tylko jeśli oba charaktery są załadowane
        if (::playerCharacter.isInitialized && ::enemyCharacter.isInitialized) {
            // Oblicz efektywność typu gracza przeciwko typowi przeciwnika
            val effectiveness = playerCharacter.element.getEffectiveness(enemyCharacter.element)
            
            // Ustaw ikony i tekst
            attackerElementIcon.setImageResource(when (playerCharacter.element) {
                ElementType.FIRE -> R.drawable.ic_fire
                ElementType.ICE -> R.drawable.ic_ice
                ElementType.LIGHTNING -> R.drawable.ic_lightning
            })
            
            defenderElementIcon.setImageResource(when (enemyCharacter.element) {
                ElementType.FIRE -> R.drawable.ic_fire
                ElementType.ICE -> R.drawable.ic_ice
                ElementType.LIGHTNING -> R.drawable.ic_lightning
            })
            
            // Ustaw tekst mnożnika
            effectivenessText.text = "x$effectiveness"
            
            // Pokaż wskaźnik tylko jeśli jest jakaś przewaga (nie neutralna)
            if (effectiveness != 1.0f) {
                elementEffectivenessContainer.visibility = View.VISIBLE
            } else {
                elementEffectivenessContainer.visibility = View.GONE
            }
        } else {
            elementEffectivenessContainer.visibility = View.GONE
        }
    }
    
    private fun updateSpecialAbilityButtonText() {
        if (specialAbilityCooldown > 0) {
            specialAbilityButton.text = "${playerCharacter.specialAbilityName} (cooldown: $specialAbilityCooldown)"
            specialAbilityButton.isEnabled = false
        } else {
            specialAbilityButton.text = playerCharacter.specialAbilityName
            specialAbilityButton.isEnabled = true
            isSpecialAbilityAvailable = true
        }
    }
    
    private fun useSpecialAbility() {
        if (!isSpecialAbilityAvailable) return
        
        // Zastosuj efekt specjalnej zdolności w zależności od elementu
        when (playerCharacter.element) {
            ElementType.FIRE -> {
                // Ściana Ognia - zadaje dodatkowe obrażenia
                val damage = calculateDamage(true) * 1.5f
                opponentHealth -= damage.toInt()
                
                // Dodaj do całkowitych obrażeń
                totalDamageDealt += damage.toInt()
                
                // Pokaż efekt ognia
                showElementEffect(ElementType.FIRE)
                
                // Pokaż obrażenia
                opponentDamageText.text = "-${damage.toInt()}"
                opponentDamageText.visibility = View.VISIBLE
                animateDamageText(opponentDamageText)
                
                // Pokaż informację zwrotną
                feedbackText.text = "Ściana Ognia!"
                feedbackText.setTextColor(resources.getColor(R.color.fire_red, theme))
                feedbackText.visibility = View.VISIBLE
            }
            ElementType.ICE -> {
                // Lodowa Zbroja - zwiększa obronę na jedną turę
                playerHealth += 20
                if (playerHealth > 100) playerHealth = 100
                
                // Pokaż efekt lodu
                showElementEffect(ElementType.ICE)
                
                // Pokaż informację zwrotną
                feedbackText.text = "Lodowa Zbroja!"
                feedbackText.setTextColor(resources.getColor(R.color.ice_blue, theme))
                feedbackText.visibility = View.VISIBLE
            }
            ElementType.LIGHTNING -> {
                // Porażenie - blokuje przeciwnika na jedną turę
                // Zwiększamy swój wynik
                playerScore += 15
                
                // Pokaż efekt błyskawicy
                showElementEffect(ElementType.LIGHTNING)
                
                // Pokaż informację zwrotną
                feedbackText.text = "Porażenie!"
                feedbackText.setTextColor(resources.getColor(R.color.lightning_yellow, theme))
                feedbackText.visibility = View.VISIBLE
            }
        }
        
        // Aktualizuj UI
        updateScoreAndHealth()
        
        // Resetuj dostępność specjalnej zdolności i ustaw cooldown
        isSpecialAbilityAvailable = false
        specialAbilityCooldown = playerCharacter.specialAbilityCooldown
        updateSpecialAbilityButtonText()
        
        // Sprawdź, czy gra się skończyła
        if (opponentHealth <= 0) {
            Handler(Looper.getMainLooper()).postDelayed({
                showResults()
            }, 1500)
        }
    }
    
    private fun showElementEffect(elementType: ElementType) {
        // Ustaw odpowiedni obrazek efektu
        val effectDrawableId = when (elementType) {
            ElementType.FIRE -> R.drawable.effect_fire
            ElementType.ICE -> R.drawable.effect_ice
            ElementType.LIGHTNING -> R.drawable.effect_lightning
        }
        elementEffectImage.setImageResource(effectDrawableId)
        
        // Pokaż kontener efektu
        elementEffectContainer.visibility = View.VISIBLE
        
        // Animacja efektu
        val animation = AnimationUtils.loadAnimation(this, R.anim.element_effect_animation)
        animation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {}
            
            override fun onAnimationEnd(animation: Animation?) {
                // Ukryj efekt po zakończeniu animacji
                elementEffectContainer.visibility = View.GONE
            }
            
            override fun onAnimationRepeat(animation: Animation?) {}
        })
        elementEffectImage.startAnimation(animation)
    }
    
    private fun handleAnswerSelection(selectedIndex: Int) {
        // Stop timer
        timer?.cancel()
        
        // Set flag to prevent multiple selections
        if (isAnswerSelected) return
        isAnswerSelected = true
        
        // Increment total answers counter
        totalAnswers++
        
        // Disable all buttons
        for (button in answerButtons) {
            button.isEnabled = false
        }
        
        // Check if answer is correct
        val isCorrect = selectedIndex == correctAnswerIndex
        
        // Show visual feedback
        if (selectedIndex >= 0) {
            // Highlight selected answer
            answerButtons[selectedIndex].setBackgroundResource(
                if (isCorrect) R.drawable.button_correct else R.drawable.button_incorrect
            )
        }
        
        // Always show correct answer
        answerButtons[correctAnswerIndex].setBackgroundResource(R.drawable.button_correct)
        answerButtons[correctAnswerIndex].setTypeface(null, Typeface.BOLD)
        
        // Update scores and health
        if (isCorrect) {
            // Increment correct answers counter
            correctAnswers++
            
            // Player answered correctly
            playerScore += 10
            
            // Oblicz obrażenia z uwzględnieniem typu elementu
            val effectiveness = playerCharacter.element.getEffectiveness(enemyCharacter.element)
            val baseDamage = calculateDamage(true)
            val damage = (baseDamage * effectiveness).toInt()
            
            opponentHealth -= damage
            
            // Add to total damage dealt
            totalDamageDealt += damage
            
            // Show feedback
            feedbackText.text = "Poprawna odpowiedź!"
            feedbackText.setTextColor(resources.getColor(R.color.correct_green, theme))
            
            // Show damage dealt
            opponentDamageText.text = "-$damage"
            opponentDamageText.visibility = View.VISIBLE
            animateDamageText(opponentDamageText)
            
            // Pokaż efekt elementu przy trafieniu (z mniejszym prawdopodobieństwem)
            if (random.nextFloat() < 0.3f) {
                showElementEffect(playerCharacter.element)
            }
        } else {
            // Player answered incorrectly or time ran out
            opponentScore += 5
            
            // Oblicz obrażenia z uwzględnieniem typu elementu
            val effectiveness = enemyCharacter.element.getEffectiveness(playerCharacter.element)
            val baseDamage = calculateDamage(false)
            val damage = (baseDamage * effectiveness).toInt()
            
            playerHealth -= damage
            
            // Add to total damage taken
            totalDamageTaken += damage
            
            // Show feedback
            feedbackText.text = if (selectedIndex >= 0) "Błędna odpowiedź!" else "Czas minął!"
            feedbackText.setTextColor(resources.getColor(R.color.incorrect_red, theme))
            
            // Show damage received
            playerDamageText.text = "-$damage"
            playerDamageText.visibility = View.VISIBLE
            animateDamageText(playerDamageText)
            
            // Pokaż efekt elementu przeciwnika przy otrzymaniu obrażeń (z mniejszym prawdopodobieństwem)
            if (random.nextFloat() < 0.3f) {
                showElementEffect(enemyCharacter.element)
            }
        }
        
        // Ensure health doesn't go below 0
        playerHealth = maxOf(0, playerHealth)
        opponentHealth = maxOf(0, opponentHealth)
        
        // Update UI
        updateScoreAndHealth()
        feedbackText.visibility = View.VISIBLE
        
        // Decrease special ability cooldown
        if (specialAbilityCooldown > 0) {
            specialAbilityCooldown--
            updateSpecialAbilityButtonText()
        }
        
        // Check if game is over
        if (playerHealth <= 0 || opponentHealth <= 0) {
            // Game over, show results after a delay
            Handler(Looper.getMainLooper()).postDelayed({
                showResults()
            }, 1500)
        } else {
            // Move to next question after a delay
            Handler(Looper.getMainLooper()).postDelayed({
                currentQuestionIndex++
                displayQuestion()
            }, 1500)
        }
    }
    
    private fun loadQuestions() {
        val questionsRef = database.reference.child("duelStages").child(stageNumber.toString()).child("questions")
        
        questionsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                questions.clear()
                
                if (snapshot.exists() && snapshot.childrenCount > 0) {
                    // Wczytaj pytania z Firebase
                    for (questionSnapshot in snapshot.children) {
                        val question = questionSnapshot.getValue(Question::class.java)
                        if (question != null) {
                            questions.add(question)
                        }
                    }
                } else {
                    // Utwórz domyślne pytania, jeśli nie ma ich w bazie danych
                    createDefaultQuestions()
                    
                    // Zapisz domyślne pytania do Firebase dla przyszłych użyć
                    saveDemoQuestionsToFirebase()
                }
                
                // Jeśli nadal nie ma pytań (mimo prób utworzenia domyślnych),
                // należy obsłużyć taki przypadek
                if (questions.isEmpty()) {
                    Toast.makeText(this@DuelBattleActivity, 
                        "Nie udało się załadować pytań dla tego etapu.", 
                        Toast.LENGTH_SHORT).show()
                    finish()
                    return
                }
                
                // Rozpocznij pojedynek - wyświetl pierwsze pytanie
                displayQuestion()
            }
            
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@DuelBattleActivity, 
                    "Błąd ładowania pytań: ${error.message}", 
                    Toast.LENGTH_SHORT).show()
                finish()
            }
        })
    }
    
    private fun createDefaultQuestions() {
        // Przykładowe pytania dla etapu (zależnie od stageNumber)
        when (stageNumber) {
            1 -> {
                // Poziom 1 - podstawowe słówka
                questions.add(Question(
                    "Co oznacza 'Hello' po polsku?",
                    "Cześć",
                    listOf("Pa", "Dziękuję", "Proszę")
                ))
                questions.add(Question(
                    "Jak powiedzieć 'Thank you' po polsku?",
                    "Dziękuję",
                    listOf("Proszę", "Przepraszam", "Do widzenia")
                ))
                questions.add(Question(
                    "Co oznacza 'Dog' po polsku?",
                    "Pies",
                    listOf("Kot", "Mysz", "Ryba")
                ))
            }
            2 -> {
                // Poziom 2 - zwierzęta
                questions.add(Question(
                    "Jak powiedzieć 'Cat' po polsku?",
                    "Kot",
                    listOf("Pies", "Krowa", "Owca")
                ))
                questions.add(Question(
                    "Co oznacza 'Cow' po polsku?",
                    "Krowa",
                    listOf("Kura", "Koń", "Świnia")
                ))
                questions.add(Question(
                    "Jak powiedzieć 'Horse' po polsku?",
                    "Koń",
                    listOf("Osioł", "Krowa", "Pies")
                ))
            }
            3 -> {
                // Poziom 3 - jedzenie
                questions.add(Question(
                    "Co oznacza 'Apple' po polsku?",
                    "Jabłko",
                    listOf("Gruszka", "Banan", "Pomarańcza")
                ))
                questions.add(Question(
                    "Jak powiedzieć 'Potato' po polsku?",
                    "Ziemniak",
                    listOf("Pomidor", "Marchewka", "Burak")
                ))
                questions.add(Question(
                    "Co oznacza 'Bread' po polsku?",
                    "Chleb",
                    listOf("Bułka", "Ciasto", "Masło")
                ))
            }
            else -> {
                // Domyślne pytania dla pozostałych poziomów
                questions.add(Question(
                    "Pytanie testowe 1 dla poziomu $stageNumber",
                    "Poprawna odpowiedź",
                    listOf("Zła odpowiedź 1", "Zła odpowiedź 2", "Zła odpowiedź 3")
                ))
                questions.add(Question(
                    "Pytanie testowe 2 dla poziomu $stageNumber",
                    "Poprawna odpowiedź",
                    listOf("Zła odpowiedź A", "Zła odpowiedź B", "Zła odpowiedź C")
                ))
                questions.add(Question(
                    "Pytanie testowe 3 dla poziomu $stageNumber",
                    "Poprawna odpowiedź",
                    listOf("Niepoprawna", "Błędna", "Niewłaściwa")
                ))
            }
        }
    }
    
    private fun saveDemoQuestionsToFirebase() {
        val questionsRef = database.reference.child("duelStages").child(stageNumber.toString()).child("questions")
        
        for (i in questions.indices) {
            val question = questions[i]
            questionsRef.child("question_$i").setValue(question)
        }
    }
    
    private fun displayQuestion() {
        if (currentQuestionIndex < questions.size) {
            val question = questions[currentQuestionIndex]
            
            // Display question text
            questionText.text = question.text
            
            // Get all possible answers including the correct one
            val answers = question.incorrectAnswers.toMutableList()
            answers.add(question.correctAnswer)
            
            // Shuffle answers
            answers.shuffle()
            
            // Find index of correct answer
            correctAnswerIndex = answers.indexOf(question.correctAnswer)
            
            // Display answers on buttons
            for (i in answerButtons.indices) {
                if (i < answers.size) {
                    answerButtons[i].text = answers[i]
                    answerButtons[i].visibility = View.VISIBLE
                } else {
                    answerButtons[i].visibility = View.GONE
                }
            }
            
            // Reset UI state for new question
            isAnswerSelected = false
            for (button in answerButtons) {
                button.isEnabled = true
                button.setBackgroundResource(R.drawable.button_normal)
                button.setTypeface(null, Typeface.NORMAL)
            }
            
            // Reset feedback and damage texts
            feedbackText.visibility = View.INVISIBLE
            playerDamageText.visibility = View.INVISIBLE
            opponentDamageText.visibility = View.INVISIBLE
            
            // Start timer
            startTimer()
        } else {
            // End of questions, show results
            showResults()
        }
    }
    
    private fun startTimer() {
        // Cancel existing timer if any
        timer?.cancel()
        
        // Reset progress bar
        timerProgressBar.progress = 100
        
        // Create and start new timer
        timer = object : CountDownTimer(10000, 100) {
            override fun onTick(millisUntilFinished: Long) {
                // Update progress bar
                val progress = (millisUntilFinished / 10000.0 * 100).toInt()
                timerProgressBar.progress = progress
            }
            
            override fun onFinish() {
                // Time's up - this counts as incorrect answer
                if (!isAnswerSelected) {
                    handleAnswerSelection(-1)
                }
            }
        }.start()
    }
    
    private fun calculateDamage(isPlayerAttack: Boolean): Int {
        // Base damage
        val baseDamage = if (isPlayerAttack) 15 else 10
        
        // Random factor (80% to 120% of base damage)
        val randomFactor = 0.8f + random.nextFloat() * 0.4f
        
        // Calculate final damage
        return (baseDamage * randomFactor).toInt()
    }
    
    private fun updateScoreAndHealth() {
        // Update score texts
        playerScoreText.text = playerScore.toString()
        opponentScoreText.text = opponentScore.toString()
        
        // Update health bars
        playerHealthBar.progress = playerHealth
        opponentHealthBar.progress = opponentHealth
    }
    
    private fun animateDamageText(textView: TextView) {
        val animation = AnimationUtils.loadAnimation(this, R.anim.damage_text_animation)
        textView.startAnimation(animation)
    }
    
    private fun showResults() {
        // Check who won
        val playerWon = opponentHealth <= 0 || playerScore > opponentScore
        
        // Set result message
        resultText.text = if (playerWon) "Zwycięstwo!" else "Porażka!"
        resultText.setTextColor(resources.getColor(
            if (playerWon) R.color.correct_green else R.color.incorrect_red, 
            theme
        ))
        
        // Mark stage as completed if player won
        stageCompleted = playerWon
        
        // Show result card
        resultCard.visibility = View.VISIBLE
        
        // Animation for result card
        val animation = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
        resultCard.startAnimation(animation)
    }
    
    private fun calculateStars(): Int {
        if (!stageCompleted) return 0
        
        // Obliczanie gwiazdek na podstawie wyniku gracza
        // - 1 gwiazdka za samo ukończenie etapu
        // - 2 gwiazdki jeśli gracz ma co najmniej 60% zdrowia
        // - 3 gwiazdki jeśli gracz ma co najmniej 80% zdrowia i dwa razy większy wynik niż przeciwnik
        
        val healthPercent = playerHealth
        
        if (healthPercent >= 80 && playerScore >= opponentScore * 2) {
            return 3
        } else if (healthPercent >= 60) {
            return 2
        } else {
            return 1
        }
    }
    
    private fun calculateXpReward(): Int {
        // Podstawowa nagroda XP zależna od poziomu etapu i uzyskanych gwiazdek
        val baseXp = stageNumber * 10
        val starMultiplier = calculateStars()
        
        // Bonus za procent poprawnych odpowiedzi (maksymalnie 50% bonusu)
        val accuracyPercent = if (totalAnswers > 0) (correctAnswers.toFloat() / totalAnswers) else 0f
        val accuracyBonus = (baseXp * accuracyPercent * 0.5).toInt()
        
        return baseXp * starMultiplier + accuracyBonus
    }
    
    private fun calculateCoinsReward(): Int {
        // Podstawowa nagroda w monetach zależna od poziomu etapu
        val baseCoins = stageNumber * 5
        
        // Bonus za gwiazdki
        val starBonus = calculateStars() * 10
        
        // Bonus za przewagę punktową (maksymalnie 20 monet)
        val scoreDifference = playerScore - opponentScore
        val scoreBonus = minOf(20, maxOf(0, scoreDifference / 5))
        
        return baseCoins + starBonus + scoreBonus
    }
    
    private fun saveBattleStatistics() {
        // Zapisz statystyki pojedynku do Firebase, jeśli użytkownik jest zalogowany
        if (currentUser != null) {
            val statsRef = database.reference
                .child("users")
                .child(currentUser!!.uid)
                .child("duelStats")
                .child(stageNumber.toString())
                .child(System.currentTimeMillis().toString())
            
            val stats = mapOf(
                "stageNumber" to stageNumber,
                "isVictory" to stageCompleted,
                "stars" to calculateStars(),
                "playerScore" to playerScore,
                "opponentScore" to opponentScore,
                "playerHealth" to playerHealth,
                "opponentHealth" to opponentHealth,
                "correctAnswers" to correctAnswers,
                "totalAnswers" to totalAnswers,
                "totalDamageDealt" to totalDamageDealt,
                "totalDamageTaken" to totalDamageTaken,
                "timestamp" to ServerValue.TIMESTAMP
            )
            
            statsRef.setValue(stats)
                .addOnSuccessListener {
                    Log.d("DuelBattleActivity", "Statystyki pojedynku zapisane pomyślnie")
                }
                .addOnFailureListener { e ->
                    Log.e("DuelBattleActivity", "Błąd podczas zapisywania statystyk pojedynku", e)
                }
        }
    }
    
    private fun returnToDuelsActivity() {
        // Zapisz statystyki pojedynku przed powrotem
        if (stageCompleted) {
            saveBattleStatistics()
        }
        
        // Oblicz nagrody
        val xpReward = if (stageCompleted) calculateXpReward() else 0
        val coinsReward = if (stageCompleted) calculateCoinsReward() else 0
        
        // Create intent with result data
        val resultIntent = Intent()
        resultIntent.putExtra("COMPLETED_STAGE", if (stageCompleted) stageNumber else -1)
        resultIntent.putExtra("STAGE_STARS", calculateStars())
        resultIntent.putExtra("XP_GAINED", xpReward)
        resultIntent.putExtra("COINS_GAINED", coinsReward)
        resultIntent.putExtra("CORRECT_ANSWERS", correctAnswers)
        resultIntent.putExtra("TOTAL_ANSWERS", totalAnswers)
        
        // Set result and finish
        setResult(RESULT_OK, resultIntent)
        finish()
    }
    
    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        // Show confirmation dialog if in the middle of a battle
        // For now, just return to duels activity
        returnToDuelsActivity()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Make sure to cancel the timer to prevent memory leaks
        timer?.cancel()
    }
} 