package com.example.lingoheroesapp.activities

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.lingoheroesapp.R
import com.example.lingoheroesapp.adapters.MistakesAdapter
import com.example.lingoheroesapp.models.*
import com.example.lingoheroesapp.models.DuelBattleCharacter
import com.example.lingoheroesapp.models.DuelBattleEnemy
import com.example.lingoheroesapp.models.WandType
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

// Dodatkowe importy
import com.example.lingoheroesapp.utils.DuelAnimationManager

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
    private lateinit var playerElementBadge: ImageView
    private lateinit var opponentElementBadge: ImageView
    private lateinit var playerName: TextView
    private lateinit var opponentName: TextView
    private lateinit var resultCard: CardView
    private lateinit var resultText: TextView
    private lateinit var continueButton: Button
    private lateinit var specialAbilityButton: Button
    
    // Dodatkowe elementy UI
    private lateinit var playerHealthText: TextView
    private lateinit var opponentHealthText: TextView
    private lateinit var playerNameText: TextView
    private lateinit var opponentNameText: TextView
    
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
    private val questions = ArrayList<com.example.lingoheroesapp.models.Question>()
    private val additionalQuestions = ArrayList<com.example.lingoheroesapp.models.Question>()
    private var currentQuestionIndex = 0
    private var playerHealth = 100
    private var opponentHealth = 100
    private var playerScore = 0
    private var opponentScore = 0
    private var correctAnswerIndex = 0
    private var isAnswerSelected = false
    private var timer: CountDownTimer? = null
    private val random = Random()
    
    // Dodaję zmienną przechowującą czas rozpoczęcia pytania
    private var questionStartTime: Long = 0
    // Licznik poprawnych odpowiedzi
    private var correctAnswersDisplayed = 0
    
    // Character and enemy instances
    private lateinit var playerCharacter: DuelBattleCharacter
    private lateinit var enemyCharacter: DuelBattleEnemy
    
    // Special ability variables
    private var specialAbilityCooldown = 0
    private var isSpecialAbilityAvailable = false
    
    // Wand effect state variables
    private var enemyFrozen = false
    private var enemyDefenseReduced = false
    private var playerDefenseIncreased = false
    
    // Stage variables
    private var stageNumber: Int = 1
    private var stageCompleted = false
    private var correctAnswers = 0
    private var totalAnswers = 0
    private var totalDamageDealt = 0
    private var totalDamageTaken = 0
    private var battleStartTime: Long = 0
    private val mistakes = mutableListOf<String>()
    private var currentQuestionStartTime: Long = 0
    
    // Dialogi
    private var activeDialog: Dialog? = null
    
    private val battleTime: Long
        get() = System.currentTimeMillis() - battleStartTime
    
    // Dodajemy nowe referencje do elementów UI
    private lateinit var playerCharacterView: ImageView
    private lateinit var monsterCharacterView: ImageView
    private lateinit var attackAnimationContainer: FrameLayout
    private lateinit var playerAttackAnimation: ImageView
    private lateinit var monsterAttackAnimation: ImageView
    private lateinit var questionContainer: CardView
    
    // Menedżer animacji
    private lateinit var animationManager: DuelAnimationManager
    
    // Flagi stanu animacji
    private var isAnimationInProgress = false
    
    // Track correct answers count for superpowers
    private var correctAnswersCount = 0
    private var nextSuperPowerUnlockAt = 3
    private val activeSuperPowers = mutableListOf<ActiveSuperPower>()
    
    // Dodaję flagę do śledzenia wyświetlenia raportu
    private var reportShown = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_duel_battle)
        
        // Get stage number from intent
        stageNumber = intent.getIntExtra("STAGE_NUMBER", 1)
        
        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        currentUser = auth.currentUser
        
        // Najpierw inicjalizujemy domyślne wartości dla postaci, aby uniknąć UninitializedPropertyAccessException
        createDefaultPlayerCharacter()
        createInitialEnemyForStage()
        
        // Initialize UI components
        initViews()
        
        // Start battle timer
        battleStartTime = System.currentTimeMillis()
        
        // Teraz ładujemy prawdziwe dane postaci z Firebase
        loadPlayerCharacter()
        loadEnemyForStage()
        
        // Resetujemy licznik poprawnych odpowiedzi dla nowego pojedynku
        correctAnswersCount = 0
        correctAnswers = 0
        nextSuperPowerUnlockAt = 3
        activeSuperPowers.clear()
        
        // Wczytujemy ewentualne aktywne supermoce, ale tylko dla istniejącego użytkownika
        if (currentUser != null) {
            loadSuperPowersFromFirebase()
        }
        
        // Inicjalizacja menedżera animacji
        animationManager = DuelAnimationManager()
        
        // Aktualizacja stanu przycisku supermocy
        updateSuperPowerButtonState()
        
        // Log dla debugowania
        Log.d("DuelBattleActivity", "Inicjalizacja aktywności zakończona. Stan przycisku supermocy zaktualizowany.")
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
        
        // Dodatkowe elementy UI
        playerHealthText = findViewById(R.id.playerHealthText)
        opponentHealthText = findViewById(R.id.opponentHealthText)
        
        // Referencje do elementów potrzebnych przy aktualizowaniu UI
        playerNameText = playerName
        opponentNameText = opponentName
        
        // Inicjalizacja nowych elementów UI
        playerCharacterView = findViewById(R.id.playerCharacter)
        monsterCharacterView = findViewById(R.id.monsterCharacter)
        attackAnimationContainer = findViewById(R.id.attackAnimationContainer)
        playerAttackAnimation = findViewById(R.id.playerAttackAnimation)
        monsterAttackAnimation = findViewById(R.id.monsterAttackAnimation)
        questionContainer = findViewById(R.id.questionContainer)
        
        // Ustawienie widoczności kontenerów animacji
        attackAnimationContainer.visibility = View.GONE
        playerAttackAnimation.visibility = View.GONE
        monsterAttackAnimation.visibility = View.GONE
        
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
            if (correctAnswersCount >= 3) {
                showSuperPowerSelectionDialog()
            } else {
                Toast.makeText(this, "Odpowiedz poprawnie na więcej pytań, aby odblokować super moc!", Toast.LENGTH_SHORT).show()
            }
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
        
        // Initialize health texts
        playerHealthText.text = "$playerHealth/${playerCharacter.hp}"
        opponentHealthText.text = "$opponentHealth/${enemyCharacter.hp}"
        
        // Initialize score texts
        playerScoreText.text = "0"
        opponentScoreText.text = "0"
        
        // Initialize superpower button
        updateSuperPowerButtonState()
    }
    
    private fun loadPlayerCharacter() {
        if (currentUser == null) {
            // Użyj domyślnej postaci, jeśli użytkownik nie jest zalogowany
            createDefaultPlayerCharacter()
            return
        }
        
        // Pobierz nazwę użytkownika i ekwipunek z bazy danych
        val userRef = database.reference
            .child("users")
            .child(currentUser!!.uid)
        
        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Pobierz nazwę użytkownika
                val username = snapshot.child("username").getValue(String::class.java)
                    ?: currentUser?.displayName
                    ?: "Gracz"
                
                // Pobierz ekwipunek
                val equipmentSnapshot = snapshot.child("equipment")
                var baseHp = 100
                var baseDamage = 10
                var wandType = WandType.FIRE
                
                if (equipmentSnapshot.exists()) {
                    try {
                        // Spróbuj odczytać ekwipunek
                        val equipment = equipmentSnapshot.getValue(Equipment::class.java)
                        if (equipment != null) {
                            baseHp = equipment.getCurrentHp()
                            baseDamage = equipment.getCurrentDamage()
                            wandType = equipment.wandType
                        } else {
                            // Ręczne odczytanie wartości ekwipunku
                            val armorLevel = equipmentSnapshot.child("armorLevel").getValue(Long::class.java)?.toInt() ?: 1
                            val wandLevel = equipmentSnapshot.child("wandLevel").getValue(Long::class.java)?.toInt() ?: 1
                            val baseHpRaw = equipmentSnapshot.child("baseHp").getValue(Long::class.java)?.toInt() ?: 100
                            val baseDamageRaw = equipmentSnapshot.child("baseDamage").getValue(Long::class.java)?.toInt() ?: 10
                            
                            // Odczytaj armorTier
                            val armorTierRaw = equipmentSnapshot.child("armorTier").getValue()
                            val armorTier = ArmorTier.fromAny(armorTierRaw)
                            
                            // Odczytaj wandType
                            val wandTypeStr = equipmentSnapshot.child("wandType").getValue(String::class.java) ?: "FIRE"
                            wandType = try {
                                WandType.valueOf(wandTypeStr)
                            } catch (e: Exception) {
                                WandType.FIRE
                            }
                            
                            // Oblicz HP na podstawie poziomu zbroi i tier
                            val tierMultiplier = when (armorTier) {
                                ArmorTier.BRONZE -> 1.0
                                ArmorTier.SILVER -> 1.2
                                ArmorTier.GOLD -> 1.5
                            }
                            baseHp = (baseHpRaw * (1 + (armorLevel - 1) * 0.1) * tierMultiplier).toInt()
                            
                            // Oblicz obrażenia na podstawie poziomu różdżki
                            baseDamage = (baseDamageRaw * (1 + (wandLevel - 1) * 0.1)).toInt()
                        }
                    } catch (e: Exception) {
                        Log.e("DuelBattleActivity", "Błąd podczas wczytywania ekwipunku: ${e.message}")
                    }
                }
                
                // Sprawdź, czy użytkownik ma już postać
                val characterSnapshot = snapshot.child("character")
                if (characterSnapshot.exists()) {
                    try {
                        // Pobierz poszczególne pola zamiast całego obiektu
                        val elementStr = characterSnapshot.child("elementValue").getValue(String::class.java) 
                            ?: characterSnapshot.child("element").getValue(String::class.java)
                            ?: wandType.name // Użyj typu różdżki jako wartości domyślnej
                        val element = ElementType.fromString(elementStr)
                        
                        val imageResId = characterSnapshot.child("imageResId").getValue(Int::class.java) ?: R.drawable.ic_player_avatar
                        
                        // Użyj wartości z ekwipunku jako wartości bazowych
                        val baseAttack = characterSnapshot.child("baseAttack").getValue(Int::class.java) ?: baseDamage
                        val baseDefense = characterSnapshot.child("baseDefense").getValue(Int::class.java) ?: (baseHp / 10)
                        
                        val specialAbilityName = when (element) {
                            ElementType.FIRE -> "Ściana Ognia"
                            ElementType.ICE -> "Lodowa Zbroja"
                            ElementType.LIGHTNING -> "Porażenie"
                            else -> "Specjalna Zdolność"
                        }
                        
                        val specialAbilityDescription = when (element) {
                            ElementType.FIRE -> "Zadaje większe obrażenia przeciwnikowi"
                            ElementType.ICE -> "Zmniejsza otrzymywane obrażenia"
                            ElementType.LIGHTNING -> "Zwiększa precyzję odpowiedzi"
                            else -> "Specjalna zdolność postaci"
                        }
                        
                        playerCharacter = DuelBattleCharacter(
                            id = "player_" + currentUser!!.uid,
                            name = username,
                            element = element,
                            imageResId = imageResId,
                            baseAttack = baseAttack,
                            baseDefense = baseDefense,
                            specialAbilityName = specialAbilityName,
                            specialAbilityDescription = specialAbilityDescription,
                            specialAbilityCooldown = 3,
                            hp = baseHp
                        )
                        
                        // Ustaw wartość początkową zdrowia gracza
                        playerHealth = baseHp
                    } catch (e: Exception) {
                        Log.e("DuelBattleActivity", "Błąd deserializacji postaci: ${e.message}")
                        createDefaultPlayerCharacter(username, baseHp, baseDamage, wandType)
                    }
                } else {
                    // Utwórz domyślną postać, jeśli nie ma jej w bazie, ale z wartościami z ekwipunku
                    createDefaultPlayerCharacter(username, baseHp, baseDamage, wandType)
                    // Zapisz domyślną postać do bazy
                    userRef.child("character").setValue(playerCharacter)
                }
                
                // Aktualizuj max zdrowie w pasku zdrowia
                playerHealthBar.max = playerCharacter.hp
                playerHealthBar.progress = playerHealth
                playerHealthText.text = "$playerHealth/${playerCharacter.hp}"
                
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
    
    private fun createDefaultPlayerCharacter(username: String? = null, baseHp: Int = 100, baseDamage: Int = 10, wandType: WandType = WandType.FIRE): DuelBattleCharacter {
        // Użyj typu różdżki jako domyślnego elementu
        val elementType = when (wandType) {
            WandType.FIRE -> ElementType.FIRE
            WandType.ICE -> ElementType.ICE
            WandType.LIGHTNING -> ElementType.LIGHTNING
        }
        
        playerCharacter = DuelBattleCharacter(
            id = "player_" + (currentUser?.uid ?: "default"),
            name = username ?: currentUser?.displayName ?: "Gracz",
            element = elementType,
            imageResId = R.drawable.ic_player_avatar,
            baseAttack = baseDamage,
            baseDefense = baseHp / 10,
            specialAbilityName = "Super Moc", // Zmieniono z elementowo-specyficznych na ogólną nazwę
            specialAbilityDescription = "Aktywuje specjalną zdolność w zależności od wybranej mocy",
            specialAbilityCooldown = 3,
            hp = baseHp
        )
        
        // Ustaw wartość początkową zdrowia gracza
        playerHealth = baseHp
        
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
                    enemyCharacter = snapshot.getValue(DuelBattleEnemy::class.java)
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
    
    private fun createDefaultEnemyForStage(): DuelBattleEnemy {
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
        
        return DuelBattleEnemy(
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
            else -> R.drawable.ic_fire // Domyślny element
        }
        playerElementBadge.setImageResource(elementDrawableId)
        
        // Aktualizuj tekst HP
        playerHealthText.text = "$playerHealth/${playerCharacter.hp}"
        
        // Aktualizuj tekst specjalnej zdolności
        updateSpecialAbilityButtonText()

        val playerDrawableId = when (playerCharacter.element) {
            ElementType.FIRE -> R.drawable.ic_warrior_fire
            ElementType.ICE -> R.drawable.ic_warrior_ice
            ElementType.LIGHTNING -> R.drawable.ic_warrior_lightning
        }
        
        // Zaktualizuj obrazek postaci gracza (poprawiona ścieżka do zasobu)
        playerCharacterView.setImageResource(playerDrawableId)
    }
    
    private fun updateEnemyUI() {
        // Aktualizuj UI przeciwnika
        opponentName.text = enemyCharacter.name
        
        // Ustaw ikonę elementu
        val elementDrawableId = when (enemyCharacter.element) {
            ElementType.FIRE -> R.drawable.ic_fire
            ElementType.ICE -> R.drawable.ic_ice
            ElementType.LIGHTNING -> R.drawable.ic_lightning
            else -> R.drawable.ic_fire // Domyślny element
        }
        opponentElementBadge.setImageResource(elementDrawableId)
        
        // Aktualizuj tekst HP
        opponentHealthText.text = "$opponentHealth/${enemyCharacter.hp}"
        
        // Zaktualizuj obrazek potwora na podstawie etapu (poprawione ścieżki do zasobów)
        val monsterDrawable = when (stageNumber % 3) {
            0 -> R.drawable.duels_monsters_ogr_removebg_preview
            1 -> R.drawable.duels_monsters_goblin_removebg_preview
            else -> R.drawable.duels_monsters_bat_removebg_preview
        }
        monsterCharacterView.setImageResource(monsterDrawable)
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
        // Ta funkcja nie jest już potrzebna, ponieważ używamy updateSuperPowerButtonState()
        // Pozostawiamy pustą implementację dla zachowania wywołań w innych miejscach
    }
    
    // Rozszerzenie dla ElementType, które konwertuje go na WandType
    private fun ElementType.toWandType(): WandType {
        return when (this) {
            ElementType.FIRE -> WandType.FIRE
            ElementType.ICE -> WandType.ICE
            ElementType.LIGHTNING -> WandType.LIGHTNING
        }
    }
    
    private fun applySpecialAbilityEffect() {
        // Usunięto implementację - zastąpiono przez applySuperPower() z różnymi typami supermocy
    }
    
    private fun showElementEffect(elementType: ElementType) {
        // Ustaw odpowiedni obrazek efektu
        val effectDrawableId = when (elementType) {
            ElementType.FIRE -> R.drawable.effect_fire
            ElementType.ICE -> R.drawable.effect_ice
            ElementType.LIGHTNING -> R.drawable.effect_lightning
            else -> R.drawable.effect_fire // Domyślny efekt
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
        if (isAnswerSelected || isAnimationInProgress) return

        isAnswerSelected = true
        isAnimationInProgress = true
        timer?.cancel()
        
        val isCorrect = selectedIndex == correctAnswerIndex
        val timeSpent = System.currentTimeMillis() - currentQuestionStartTime
        
        // Debugowanie
        Log.d("DuelBattleActivity", "Wybrano odpowiedź: $selectedIndex, poprawna: $correctAnswerIndex, isCorrect: $isCorrect")
        
        if (!isCorrect) {
            val question = questions[currentQuestionIndex]
            mistakes.add("Pytanie: ${question.question}\n" +
                        "Twoja odpowiedź: ${if (selectedIndex >= 0 && selectedIndex < answerButtons.size) answerButtons[selectedIndex].text else "Brak odpowiedzi"}\n" +
                        "Poprawna odpowiedź: ${question.correctAnswer}")
            
            // Podświetl niepoprawną odpowiedź
            if (selectedIndex >= 0 && selectedIndex < answerButtons.size) {
                animationManager.animateIncorrectAnswer(answerButtons[selectedIndex])
            }
            
            // Podświetl poprawną odpowiedź
            animationManager.animateCorrectAnswer(answerButtons[correctAnswerIndex])
        } else {
            // Podświetl poprawną odpowiedź
            animationManager.animateCorrectAnswer(answerButtons[selectedIndex])
            
            // Zwiększ licznik poprawnych odpowiedzi tylko przy poprawnej odpowiedzi
            increaseCorrectAnswers()
        }
        
        // Calculate damage based on correctness and time
        var damage = calculateDamage(isCorrect, timeSpent)
        
        if (isCorrect) {
            // Sprawdź czy są aktywne wzmocnienia obrażeń (damage_boost)
            val damageBoostSuperPowers = activeSuperPowers.filter { it.superPowerData?.effectType == "damage_boost" }
            
            if (damageBoostSuperPowers.isNotEmpty()) {
                // Wzmocnij obrażenia i pokaż informację
                val originalDamage = damage
                
                for (boost in damageBoostSuperPowers) {
                    val superPower = boost.superPowerData!!
                    val boostValue = if (superPower.isPercentage) {
                        originalDamage * superPower.effectValue / 100
                    } else {
                        superPower.effectValue
                    }
                    
                    damage += boostValue.toInt()
                    
                    // Pokaż informację o wzmocnieniu
                    feedbackText.text = "${superPower.name}: +${boostValue.toInt()} obrażeń!"
                    feedbackText.visibility = View.VISIBLE
                    
                    // Usuń tę supermoc jeśli trwa tylko 1 turę
                    if (boost.remainingDuration <= 1) {
                        activeSuperPowers.remove(boost)
                    } else {
                        boost.remainingDuration--
                    }
                }
            }
            
            // Sprawdź, czy aktywna jest supermoc egzekucji (execute)
            val executeSuperPowers = activeSuperPowers.filter { it.superPowerData?.effectType == "execute" }
            
            if (executeSuperPowers.isNotEmpty() && opponentHealth > 0) {
                val maxHealth = enemyCharacter.hp
                val currentHealthPercent = (opponentHealth * 100) / maxHealth
                
                for (execute in executeSuperPowers) {
                    val superPower = execute.superPowerData!!
                    
                    // Sprawdź warunek egzekucji
                    val executionThreshold = if (superPower.id == "armageddon") 25 else 30
                    
                    if (currentHealthPercent < executionThreshold) {
                        val originalDamage = damage
                        val executeBonus = if (superPower.isPercentage) {
                            originalDamage * superPower.effectValue / 100
                        } else {
                            superPower.effectValue
                        }
                        
                        damage += executeBonus.toInt()
                        
                        // Pokaż informację o egzekucji
                        feedbackText.text = "${superPower.name}: +${executeBonus.toInt()} obrażeń!"
                        feedbackText.visibility = View.VISIBLE
                    }
                    
                    // Usuń tę supermoc jeśli trwa tylko 1 turę
                    if (execute.remainingDuration <= 1) {
                        activeSuperPowers.remove(execute)
                    } else {
                        execute.remainingDuration--
                    }
                }
            }
            
            playerScore += damage
            opponentHealth -= damage
            
            // Sprawdź czy aktywna jest supermoc kradzieży życia (life_steal)
            val lifeStealSuperPowers = activeSuperPowers.filter { it.superPowerData?.effectType == "life_steal" }
            
            if (lifeStealSuperPowers.isNotEmpty()) {
                for (lifesteal in lifeStealSuperPowers) {
                    val superPower = lifesteal.superPowerData!!
                    val healAmount = if (superPower.isPercentage) {
                        damage * superPower.effectValue / 100
                    } else {
                        superPower.effectValue
                    }
                    
                    // Dodaj życie graczowi
                    val maxHealth = playerCharacter.hp
                    val oldHealth = playerHealth
                    playerHealth = (playerHealth + healAmount.toInt()).coerceAtMost(maxHealth)
                    
                    // Pokaż tekst leczenia tylko jeśli faktycznie uleczono
                    if (playerHealth > oldHealth) {
                        playerDamageText.text = "+${playerHealth - oldHealth}"
                        playerDamageText.setTextColor(resources.getColor(R.color.health_good, theme))
                        playerDamageText.visibility = View.VISIBLE
                        animateDamageText(playerDamageText)
                        
                        // Resetuj kolor po animacji
                        Handler(Looper.getMainLooper()).postDelayed({
                            playerDamageText.setTextColor(resources.getColor(R.color.damage_red, theme))
                        }, 1500)
                        
                        // Pokaż informację o wampirze
                        feedbackText.text = "${superPower.name}: odzyskano ${playerHealth - oldHealth} HP!"
                        feedbackText.visibility = View.VISIBLE
                    }
                    
                    // Usuń tę supermoc jeśli trwa tylko 1 turę
                    if (lifesteal.remainingDuration <= 1) {
                        activeSuperPowers.remove(lifesteal)
                    } else {
                        lifesteal.remainingDuration--
                    }
                }
            }
            
            // Dodaj do całkowitych obrażeń zadanych
            totalDamageDealt += damage
        } else {
            // Sprawdź czy przeciwnik jest zamrożony (freeze) lub ogłuszony (stun)
            val isEnemyDisabled = isEnemyStunnedOrFrozen()
            
            // Obrażenia zadawane tylko jeśli przeciwnik nie jest zamrożony/ogłuszony
            if (!isEnemyDisabled) {
                // Sprawdź, czy trzeba zastosować efekty obronne
                val reducedDamage = applyDefensiveSuperPowers(damage)
                
                // Log dla debugowania
                Log.d("DuelBattleActivity", "Początkowe obrażenia: $damage, po zastosowaniu obron: $reducedDamage")
                
                // Zastosuj obrażenia tylko jeśli faktycznie zostały zadane (mogą być 0 przez unik)
                if (reducedDamage > 0) {
                    opponentScore += reducedDamage
                    playerHealth -= reducedDamage
                    
                    // Dodaj do całkowitych obrażeń otrzymanych
                    totalDamageTaken += reducedDamage
                }
            } else {
                // Przeciwnik jest zamrożony/ogłuszony - nie atakuje
                feedbackText.text = "Przeciwnik nie może atakować!"
                feedbackText.setTextColor(resources.getColor(R.color.ice_blue, theme))
                feedbackText.visibility = View.VISIBLE
                
                // Log dla debugowania
                Log.d("DuelBattleActivity", "Przeciwnik zamrożony/ogłuszony - omija turę ataku")
            }
        }
        
        totalAnswers++
        
        // Update UI
        updateScoreAndHealth()
        showFeedback(isCorrect, damage)
        
        // Opóźnienie przed rozpoczęciem animacji znikania pytania
        Handler(Looper.getMainLooper()).postDelayed({
            // Ukryj pytanie z animacją
            if (questionContainer.visibility == View.VISIBLE) {
                animationManager.animateQuestionDisappear(questionContainer) {
                    // Wykonaj animację ataku
                    if (isCorrect) {
                        // Animacja ataku gracza
                        animationManager.animatePlayerAttack(
                            playerCharacterView,
                            monsterCharacterView,
                            attackAnimationContainer,
                            playerAttackAnimation,
                            playerCharacter.element.toWandType(),
                            opponentDamageText,
                            damage
                        ) {
                            // Sprawdź, czy gra się skończyła
                            if (playerHealth <= 0 || opponentHealth <= 0) {
                                stageCompleted = opponentHealth <= 0
                                showResults()
                            } else {
                                // Przejdź do następnego pytania
                                currentQuestionIndex++
                                isAnimationInProgress = false
                                displayQuestion()
                            }
                        }
                    } else {
                        // Sprawdź czy przeciwnik jest zamrożony/ogłuszony
                        val isEnemyDisabled = isEnemyStunnedOrFrozen()
                        
                        if (isEnemyDisabled) {
                            // Przeciwnik nie atakuje - pomiń animację ataku i przechodź do następnego pytania
                            Handler(Looper.getMainLooper()).postDelayed({
                                if (playerHealth <= 0 || opponentHealth <= 0) {
                                    stageCompleted = opponentHealth <= 0
                                    showResults()
                                } else {
                                    // Przejdź do następnego pytania
                                    currentQuestionIndex++
                                    isAnimationInProgress = false
                                    displayQuestion()
                                }
                            }, 1000)
                        } else {
                            // Zwykła animacja ataku przeciwnika
                            animationManager.animateMonsterAttack(
                                playerCharacterView,
                                monsterCharacterView,
                                attackAnimationContainer,
                                monsterAttackAnimation,
                                playerDamageText,
                                if (playerHealth < playerHealth + damage) damage else 0 // Pokaż 0 jeśli był unik
                            ) {
                                // Sprawdź, czy gra się skończyła
                                if (playerHealth <= 0 || opponentHealth <= 0) {
                                    stageCompleted = opponentHealth <= 0
                                    showResults()
                                } else {
                                    // Przejdź do następnego pytania
                                    currentQuestionIndex++
                                    isAnimationInProgress = false
                                    displayQuestion()
                                }
                            }
                        }
                    }
                }
            } else {
                Log.e("DuelBattleActivity", "Kontener pytania jest już niewidoczny")
                
                // Awaryjna obsługa - wykonaj animację ataku bezpośrednio
                if (isCorrect) {
                    animationManager.animatePlayerAttack(
                        playerCharacterView,
                        monsterCharacterView,
                        attackAnimationContainer,
                        playerAttackAnimation,
                        playerCharacter.element.toWandType(),
                        opponentDamageText,
                        damage
                    ) {
                        if (playerHealth <= 0 || opponentHealth <= 0) {
                            stageCompleted = opponentHealth <= 0
                            showResults()
                        } else {
                            currentQuestionIndex++
                            isAnimationInProgress = false
                            displayQuestion()
                        }
                    }
                } else {
                    animationManager.animateMonsterAttack(
                        playerCharacterView,
                        monsterCharacterView,
                        attackAnimationContainer,
                        monsterAttackAnimation,
                        playerDamageText,
                        if (playerHealth < playerHealth + damage) damage else 0 // Pokaż 0 jeśli był unik
                    ) {
                        if (playerHealth <= 0 || opponentHealth <= 0) {
                            stageCompleted = opponentHealth <= 0
                            showResults()
                        } else {
                            currentQuestionIndex++
                            isAnimationInProgress = false
                            displayQuestion()
                        }
                    }
                }
            }
        }, 1000) // 1 sekunda opóźnienia, aby pokazać feedback
    }
    
    private fun calculateDamage(isCorrect: Boolean, timeSpent: Long): Int {
        val baseDamage = if (isCorrect) 15 else 10
        val timeBonus = when {
            timeSpent < 3000 -> 1.5f  // Super fast
            timeSpent < 5000 -> 1.2f  // Fast
            timeSpent < 8000 -> 1.0f  // Normal
            else -> 0.8f              // Slow
        }
        
        // Uwzględnij efektywność elementów
        val effectiveness = if (isCorrect) {
            playerCharacter.element.getEffectiveness(enemyCharacter.element)
        } else {
            enemyCharacter.element.getEffectiveness(playerCharacter.element)
        }
        
        return (baseDamage * timeBonus * effectiveness).toInt()
    }
    
    private fun loadQuestions() {
        val questionsRef = database.reference.child("duelStages").child(stageNumber.toString()).child("questions")
        
        questionsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                questions.clear()
                
                if (snapshot.exists() && snapshot.childrenCount > 0) {
                    // Wczytaj pytania z Firebase
                    for (questionSnapshot in snapshot.children) {
                        try {
                            // Pobieramy wartości pól zamiast całego obiektu, aby uniknąć problemów z typami
                            val questionText = questionSnapshot.child("question").getValue(String::class.java) ?: ""
                            val correctAnswer = questionSnapshot.child("correctAnswer").getValue(String::class.java) ?: ""
                            
                            // Pobieramy listę niepoprawnych odpowiedzi
                            val incorrectAnswers = mutableListOf<String>()
                            val incorrectAnswersSnapshot = questionSnapshot.child("incorrectAnswers")
                            if (incorrectAnswersSnapshot.exists()) {
                                for (answerSnapshot in incorrectAnswersSnapshot.children) {
                                    val answer = answerSnapshot.getValue(String::class.java)
                                    if (answer != null) {
                                        incorrectAnswers.add(answer)
                                    }
                                }
                            }
                            
                            // Tworzymy nowy obiekt Question z pakietu models
                            val question = com.example.lingoheroesapp.models.Question(
                                question = questionText,
                                correctAnswer = correctAnswer,
                                incorrectAnswers = incorrectAnswers
                            )
                            
                            questions.add(question)
                        } catch (e: Exception) {
                            Log.e("DuelBattleActivity", "Błąd podczas parsowania pytania: ${e.message}")
                        }
                    }
                } else {
                    // Utwórz domyślne pytania, jeśli nie ma ich w bazie danych
                    createDefaultQuestions()
                    
                    // Zapisz domyślne pytania do Firebase dla przyszłych użyć
                    saveDemoQuestionsToFirebase()
                }
                
                // Ładuj dodatkowe pytania
                loadAdditionalQuestions()
                
                // Dodaj debugowanie
                Log.d("DuelBattleActivity", "Załadowano ${questions.size} pytań głównych")
                
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
                currentQuestionIndex = 0
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
    
    // Ładowanie dodatkowych pytań
    private fun loadAdditionalQuestions() {
        val additionalQuestionsRef = database.reference.child("duelStages").child(stageNumber.toString()).child("additionalQuestions")
        
        additionalQuestionsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                additionalQuestions.clear()
                
                if (snapshot.exists() && snapshot.childrenCount > 0) {
                    // Wczytaj dodatkowe pytania z Firebase
                    for (questionSnapshot in snapshot.children) {
                        try {
                            val questionText = questionSnapshot.child("question").getValue(String::class.java) ?: ""
                            val correctAnswer = questionSnapshot.child("correctAnswer").getValue(String::class.java) ?: ""
                            
                            val incorrectAnswers = mutableListOf<String>()
                            val incorrectAnswersSnapshot = questionSnapshot.child("incorrectAnswers")
                            if (incorrectAnswersSnapshot.exists()) {
                                for (answerSnapshot in incorrectAnswersSnapshot.children) {
                                    val answer = answerSnapshot.getValue(String::class.java)
                                    if (answer != null) {
                                        incorrectAnswers.add(answer)
                                    }
                                }
                            }
                            
                            val question = com.example.lingoheroesapp.models.Question(
                                question = questionText,
                                correctAnswer = correctAnswer,
                                incorrectAnswers = incorrectAnswers
                            )
                            
                            additionalQuestions.add(question)
                        } catch (e: Exception) {
                            Log.e("DuelBattleActivity", "Błąd podczas parsowania dodatkowego pytania: ${e.message}")
                        }
                    }
                } else {
                    // Jeśli nie ma dodatkowych pytań, generujemy je
                    createAdditionalQuestions()
                    
                    // Zapisujemy dodatkowe pytania do Firebase
                    saveAdditionalQuestionsToFirebase(additionalQuestions)
                }
                
                Log.d("DuelBattleActivity", "Załadowano ${additionalQuestions.size} dodatkowych pytań")
            }
            
            override fun onCancelled(error: DatabaseError) {
                Log.e("DuelBattleActivity", "Błąd ładowania dodatkowych pytań: ${error.message}")
            }
        })
    }
    
    // Tworzenie dodatkowych pytań
    private fun createAdditionalQuestions() {
        // W zależności od poziomu generujemy różne dodatkowe pytania
        when (stageNumber) {
            1 -> {
                additionalQuestions.add(com.example.lingoheroesapp.models.Question(
                    question = "Co oznacza 'Goodbye' po polsku?",
                    correctAnswer = "Do widzenia",
                    incorrectAnswers = listOf("Witaj", "Przepraszam", "Dzień dobry")
                ))
                additionalQuestions.add(com.example.lingoheroesapp.models.Question(
                    question = "Jak powiedzieć 'Please' po polsku?",
                    correctAnswer = "Proszę",
                    incorrectAnswers = listOf("Przepraszam", "Dziękuję", "Nie ma za co")
                ))
            }
            2 -> {
                additionalQuestions.add(com.example.lingoheroesapp.models.Question(
                    question = "Jak powiedzieć 'Bird' po polsku?",
                    correctAnswer = "Ptak",
                    incorrectAnswers = listOf("Ryba", "Żaba", "Wąż")
                ))
                additionalQuestions.add(com.example.lingoheroesapp.models.Question(
                    question = "Co oznacza 'Fish' po polsku?",
                    correctAnswer = "Ryba",
                    incorrectAnswers = listOf("Ptak", "Delfin", "Żółw")
                ))
            }
            3 -> {
                additionalQuestions.add(com.example.lingoheroesapp.models.Question(
                    question = "Co oznacza 'Carrot' po polsku?",
                    correctAnswer = "Marchewka",
                    incorrectAnswers = listOf("Ziemniak", "Cebula", "Burak")
                ))
                additionalQuestions.add(com.example.lingoheroesapp.models.Question(
                    question = "Jak powiedzieć 'Milk' po polsku?",
                    correctAnswer = "Mleko",
                    incorrectAnswers = listOf("Woda", "Sok", "Kawa")
                ))
            }
            else -> {
                additionalQuestions.add(com.example.lingoheroesapp.models.Question(
                    question = "Dodatkowe pytanie 1 dla poziomu $stageNumber",
                    correctAnswer = "Poprawna odpowiedź",
                    incorrectAnswers = listOf("Błędna 1", "Błędna 2", "Błędna 3")
                ))
                additionalQuestions.add(com.example.lingoheroesapp.models.Question(
                    question = "Dodatkowe pytanie 2 dla poziomu $stageNumber",
                    correctAnswer = "Poprawna odpowiedź",
                    incorrectAnswers = listOf("Niepoprawna A", "Niepoprawna B", "Niepoprawna C")
                ))
            }
        }
    }
    
    // Zapisywanie dodatkowych pytań do Firebase
    private fun saveAdditionalQuestionsToFirebase(additionalQuestions: List<com.example.lingoheroesapp.models.Question>) {
        val questionsRef = database.reference.child("duelStages").child(stageNumber.toString()).child("additionalQuestions")
        
        for (i in additionalQuestions.indices) {
            val question = additionalQuestions[i]
            val questionMap = mapOf(
                "question" to question.question,
                "correctAnswer" to question.correctAnswer,
                "incorrectAnswers" to question.incorrectAnswers
            )
            questionsRef.child("question_$i").setValue(questionMap)
        }
    }
    
    private fun createDefaultQuestions() {
        // Przykładowe pytania dla etapu (zależnie od stageNumber)
        when (stageNumber) {
            1 -> {
                // Poziom 1 - podstawowe słówka
                questions.add(Question(
                    question = "Co oznacza 'Hello' po polsku?",
                    correctAnswer = "Cześć",
                    incorrectAnswers = listOf("Pa", "Dziękuję", "Proszę")
                ))
                questions.add(Question(
                    question = "Jak powiedzieć 'Thank you' po polsku?",
                    correctAnswer = "Dziękuję",
                    incorrectAnswers = listOf("Proszę", "Przepraszam", "Do widzenia")
                ))
                questions.add(Question(
                    question = "Co oznacza 'Dog' po polsku?",
                    correctAnswer = "Pies",
                    incorrectAnswers = listOf("Kot", "Mysz", "Ryba")
                ))
            }
            2 -> {
                // Poziom 2 - zwierzęta
                questions.add(Question(
                    question = "Jak powiedzieć 'Cat' po polsku?",
                    correctAnswer = "Kot",
                    incorrectAnswers = listOf("Pies", "Krowa", "Owca")
                ))
                questions.add(Question(
                    question = "Co oznacza 'Cow' po polsku?",
                    correctAnswer = "Krowa",
                    incorrectAnswers = listOf("Kura", "Koń", "Świnia")
                ))
                questions.add(Question(
                    question = "Jak powiedzieć 'Horse' po polsku?",
                    correctAnswer = "Koń",
                    incorrectAnswers = listOf("Osioł", "Krowa", "Pies")
                ))
            }
            3 -> {
                // Poziom 3 - jedzenie
                questions.add(Question(
                    question = "Co oznacza 'Apple' po polsku?",
                    correctAnswer = "Jabłko",
                    incorrectAnswers = listOf("Gruszka", "Banan", "Pomarańcza")
                ))
                questions.add(Question(
                    question = "Jak powiedzieć 'Potato' po polsku?",
                    correctAnswer = "Ziemniak",
                    incorrectAnswers = listOf("Pomidor", "Marchewka", "Burak")
                ))
                questions.add(Question(
                    question = "Co oznacza 'Bread' po polsku?",
                    correctAnswer = "Chleb",
                    incorrectAnswers = listOf("Bułka", "Ciasto", "Masło")
                ))
            }
            else -> {
                // Domyślne pytania dla pozostałych poziomów
                questions.add(Question(
                    question = "Pytanie testowe 1 dla poziomu $stageNumber",
                    correctAnswer = "Poprawna odpowiedź",
                    incorrectAnswers = listOf("Zła odpowiedź 1", "Zła odpowiedź 2", "Zła odpowiedź 3")
                ))
                questions.add(Question(
                    question = "Pytanie testowe 2 dla poziomu $stageNumber",
                    correctAnswer = "Poprawna odpowiedź",
                    incorrectAnswers = listOf("Zła odpowiedź A", "Zła odpowiedź B", "Zła odpowiedź C")
                ))
                questions.add(Question(
                    question = "Pytanie testowe 3 dla poziomu $stageNumber",
                    correctAnswer = "Poprawna odpowiedź",
                    incorrectAnswers = listOf("Niepoprawna", "Błędna", "Niewłaściwa")
                ))
            }
        }
    }
    
    private fun saveDemoQuestionsToFirebase() {
        val questionsRef = database.reference.child("duelStages").child(stageNumber.toString()).child("questions")
        
        for (i in questions.indices) {
            val question = questions[i]
            // Zapisujemy pytanie jako mapę wartości, zamiast obiektu Question, aby uniknąć problemów z typami
            val questionMap = mapOf(
                "question" to question.question,
                "correctAnswer" to question.correctAnswer,
                "incorrectAnswers" to question.incorrectAnswers
            )
            questionsRef.child("question_$i").setValue(questionMap)
        }
    }
    
    // Display current question
    private fun displayQuestion() {
        isAnswerSelected = false
        
        // Zapisujemy czas rozpoczęcia pytania
        questionStartTime = System.currentTimeMillis()
        currentQuestionStartTime = System.currentTimeMillis()
        
        // Sprawdź czy mamy jeszcze pytania
        if (currentQuestionIndex >= questions.size) {
            // Sprawdź czy mamy dodatkowe pytania
            if (additionalQuestions.isNotEmpty()) {
                // Dodaj dodatkowe pytania do głównej puli
                questions.addAll(additionalQuestions)
                additionalQuestions.clear()
            } else {
                // Jeśli brakuje pytań, stwórz dodatkowe
                val moreQuestions = createMoreDefaultQuestions()
                questions.addAll(moreQuestions)
            }
        }
        
        // Get the current question
        val currentQuestion = 
            if (currentQuestionIndex < questions.size) questions[currentQuestionIndex]
            else {
                // Awaryjnie stworzone pytanie
                Log.e("DuelBattleActivity", "Brak pytania na indeksie $currentQuestionIndex, generowanie awaryjne")
                createMoreDefaultQuestions().first()
            }
        
        // Process superpower effects at the start of the turn
        processSuperPowerEffects()
        
        // If player/opponent is defeated, show results
        if (playerHealth <= 0 || opponentHealth <= 0) {
            showResults()
            return
        }
        
        // Update question text
        questionText.text = currentQuestion.question
        
        // Shuffle answers
        val allAnswers = mutableListOf<String>()
        allAnswers.add(currentQuestion.correctAnswer)
        allAnswers.addAll(currentQuestion.incorrectAnswers)
        allAnswers.shuffle()
        
        // Store the correct answer index
        correctAnswerIndex = allAnswers.indexOf(currentQuestion.correctAnswer)
        
        // Set answer button texts
        answerButtons.forEachIndexed { index, button ->
            if (index < allAnswers.size) {
                button.text = allAnswers[index]
                button.visibility = View.VISIBLE
            } else {
                button.visibility = View.GONE
            }
        }
        
        // Ukryj feedback
        feedbackText.visibility = View.INVISIBLE
        playerDamageText.visibility = View.INVISIBLE
        opponentDamageText.visibility = View.INVISIBLE
        
        // Inicjalnie ustaw kontener pytania jako niewidoczny, aby go zanimować
        questionContainer.visibility = View.INVISIBLE
        
        // Animuj pojawienie się pytania
        animationManager.animateQuestionAppear(questionContainer) {
            // Start timer for this question po zakończeniu animacji pojawienia się
            startQuestionTimer()
        }
    }
    
    private fun startQuestionTimer() {
        // Cancel existing timer if any
        timer?.cancel()
        
        // Reset progress bar
        timerProgressBar.progress = 100
        
        // Zapisz aktualny czas na rozpoczęciu pytania
        currentQuestionStartTime = System.currentTimeMillis()
        
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
    
    private fun updateScoreAndHealth() {
        // Update score text view
        playerScoreText.text = playerScore.toString()
        opponentScoreText.text = opponentScore.toString()
        
        // Update player health bar
        animateHealthBar(playerHealthBar, playerHealthBar.progress, playerHealth)
        updateHealthBarColor(playerHealthBar, playerHealth)
        playerHealthText.text = "$playerHealth/${playerCharacter.hp}"
        
        // Update enemy health bar
        animateHealthBar(opponentHealthBar, opponentHealthBar.progress, opponentHealth)
        updateHealthBarColor(opponentHealthBar, opponentHealth)
        opponentHealthText.text = "$opponentHealth/${enemyCharacter.hp}"
    }
    
    private fun animateHealthBar(healthBar: ProgressBar, from: Int, to: Int, onAnimationEnd: () -> Unit = {}) {
        val valueAnimator = ValueAnimator.ofInt(from, to)
        valueAnimator.duration = 500 // Krótka animacja dla lepszej responsywności
        valueAnimator.addUpdateListener { animation ->
            val animatedValue = animation.animatedValue as Int
            // Sprawdź czy aktywność nie jest niszczona
            if (!isFinishing && !isDestroyed) {
                healthBar.progress = animatedValue
            } else {
                // Jeśli aktywność jest niszczona, zatrzymaj animację
                animation.cancel()
            }
        }
        valueAnimator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                // Sprawdź czy aktywność nie jest niszczona przed wywołaniem callback'a
                if (!isFinishing && !isDestroyed) {
                    onAnimationEnd()
                }
            }
        })
        valueAnimator.start()
    }
    
    private fun updateHealthBarColor(healthBar: ProgressBar, health: Int) {
        val maxHealth = if (healthBar == playerHealthBar) 
            playerCharacter.hp else enemyCharacter.hp
        
        val healthPercentage = (health * 100) / maxHealth
        
        val colorRes = when {
            healthPercentage > 60 -> R.color.health_good
            healthPercentage > 30 -> R.color.health_medium
            else -> R.color.health_low
        }
        
        healthBar.progressTintList = ColorStateList.valueOf(resources.getColor(colorRes, theme))
    }
    
    private fun animateDamageText(textView: TextView) {
        val animation = AnimationUtils.loadAnimation(this, R.anim.damage_text_animation)
        textView.startAnimation(animation)
    }
    
    private fun showResults() {
        // Jeśli raport już został wyświetlony, nie pokazuj go ponownie
        if (reportShown) {
            return
        }
        
        // Ustawiam flagę na true, aby zapobiec ponownemu wyświetleniu
        reportShown = true
        
        // Zablokuj interakcję użytkownika, aby zapobiec kolejnym akcjom
        window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
        
        val timeSpent = System.currentTimeMillis() - battleStartTime
        val xpGained = calculateXpReward()
        val coinsGained = calculateCoinsReward()
        val stars = calculateStars()
        
        // Check who won
        val playerWon = opponentHealth <= 0 || playerScore > opponentScore
        stageCompleted = playerWon
        
        // Resetujemy licznik poprawnych odpowiedzi przy zakończeniu pojedynku
        correctAnswersCount = 0
        
        // Create a DuelReport object
        val duelReport = DuelReport(
            stageNumber = stageNumber,
            isCompleted = stageCompleted,
            correctAnswers = correctAnswers,
            totalAnswers = totalAnswers,
            timeSpent = timeSpent,
            xpGained = xpGained,
            coinsGained = coinsGained,
            stars = stars,
            mistakes = mistakes.toList()
        )
        
        // Save battle statistics
        saveBattleStatistics()
        
        // Ukryj zbędny kartę wyników
        resultCard.visibility = View.GONE
        
        // Pokazuj raport bezpośrednio, bez żadnych opóźnień i callbacków
        if (!isFinishing && !isDestroyed) {
            try {
                showDuelReport(duelReport)
            } catch (e: Exception) {
                Log.e("DuelBattleActivity", "Error showing duel report: ${e.message}")
                e.printStackTrace()
                // Odblokuj interakcję użytkownika w przypadku błędu
                window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                // Wróć do ekranu pojedynków
                returnToDuelsActivity()
            }
        } else {
            // Aktywność już jest zamykana, po prostu wracamy
            returnToDuelsActivity()
        }
    }
    
    private fun showDuelReport(report: DuelReport) {
        // Check if the activity is still valid
        if (isFinishing || isDestroyed) {
            returnToDuelsActivity()
            return
        }
        
        try {
            // Zamknij aktywny dialog, jeśli istnieje
            activeDialog?.dismiss()
            
            // Utwórz dialog z zachowaniem weak reference (soft reference) do aktywności
            val dialogReference = Dialog(this)
            activeDialog = dialogReference
            dialogReference.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialogReference.setContentView(R.layout.duel_report_dialog)
            dialogReference.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialogReference.setCancelable(false) // Uniemożliw zamknięcie przez back button
            
            // Set report data
            dialogReference.findViewById<TextView>(R.id.correctAnswersText).text = 
                "${report.correctAnswers}/${report.totalAnswers}"
            dialogReference.findViewById<TextView>(R.id.timeSpentText).text = 
                formatTime(report.timeSpent)
            dialogReference.findViewById<TextView>(R.id.coinsGainedText).text = 
                "+${report.coinsGained}"
            
            // Set stars
            val starsContainer = dialogReference.findViewById<LinearLayout>(R.id.starsContainer)
            for (i in 0 until 3) {
                val star = ImageView(this)
                star.layoutParams = LinearLayout.LayoutParams(48, 48)
                star.setImageResource(
                    if (i < report.stars) R.drawable.ic_star_filled
                    else R.drawable.ic_star_empty
                )
                starsContainer.addView(star)
            }
            
            // Set mistakes
            val mistakesRecyclerView = dialogReference.findViewById<RecyclerView>(R.id.mistakesRecyclerView)
            mistakesRecyclerView.layoutManager = LinearLayoutManager(this)
            mistakesRecyclerView.adapter = MistakesAdapter(report.mistakes)
            
            // Set continue button - finishes the activity to prevent window leaks
            val continueButton = dialogReference.findViewById<Button>(R.id.continueButton)
            continueButton.setOnClickListener {
                // Bezpośrednio zamknij dialog i przejdź do aktywności
                dialogReference.dismiss()
                
                // Bezpiecznie wróć do poprzedniej aktywności
                if (!isFinishing && !isDestroyed) {
                    returnToDuelsActivity()
                }
            }
            
            // Dodaj obsługę przycisku back, aby zapobiec wyciekowi okna
            dialogReference.setOnKeyListener { dialog, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                    dialog.dismiss()
                    if (!isFinishing && !isDestroyed) {
                        returnToDuelsActivity()
                    }
                    true
                } else {
                    false
                }
            }
            
            // Make dialog take most of the screen
            val metrics = resources.displayMetrics
            val width = metrics.widthPixels
            dialogReference.window?.setLayout((width * 0.9).toInt(), WindowManager.LayoutParams.WRAP_CONTENT)
            
            // Bezpiecznie pokazuj dialog
            if (!isFinishing && !isDestroyed) {
                // Użyj Handler, aby pokazać dialog w wątku UI
                Handler(Looper.getMainLooper()).post {
                    try {
                        if (!isFinishing && !isDestroyed) {
                            // Dialog nie powinien automatycznie zamykać się i przekierowywać do innej aktywności
                            // Usuwamy automatyczne przekierowanie po zamknięciu dialogu
                            dialogReference.show()
                        } else {
                            // Jeśli aktywność jest kończona, zamknij dialog
                            dialogReference.dismiss()
                        }
                    } catch (e: Exception) {
                        Log.e("DuelBattleActivity", "Błąd podczas pokazywania dialogu: ${e.message}")
                        e.printStackTrace()
                        returnToDuelsActivity()
                    }
                }
            } else {
                returnToDuelsActivity()
            }
        } catch (e: Exception) {
            Log.e("DuelBattleActivity", "Error showing dialog: ${e.message}")
            e.printStackTrace()
            // Make sure activity returns if dialog fails
            returnToDuelsActivity() 
        }
    }
    
    private fun saveBattleStatistics() {
        // Zapisz statystyki pojedynku do Firebase, jeśli użytkownik jest zalogowany
        if (currentUser != null) {
            // Określ ID statystyk dla większej czytelności
            val statsId = System.currentTimeMillis().toString()
            
            val statsRef = database.reference
                .child("users")
                .child(currentUser!!.uid)
                .child("duelStats")
                .child(stageNumber.toString())
                .child(statsId)
            
            // Oblicz nagrody
            val timeSpent = System.currentTimeMillis() - battleStartTime
            val xpGained = 0 // Ustawiam XP na 0, aby nie przyznawać XP za pojedynki
            val coinsGained = calculateCoinsReward()
            val stars = calculateStars()
            
            // Utwórz mapę statystyk
            val stats = mapOf(
                "stageNumber" to stageNumber,
                "isVictory" to stageCompleted,
                "stars" to stars,
                "playerScore" to playerScore,
                "opponentScore" to opponentScore,
                "playerHealth" to playerHealth,
                "opponentHealth" to opponentHealth,
                "correctAnswers" to correctAnswers,
                "totalAnswers" to totalAnswers,
                "timeSpent" to timeSpent,
                "xpGained" to xpGained,
                "coinsGained" to coinsGained,
                "mistakes" to mistakes.toList()
            )
            
            // Aktualizuj statystyki w bazie
            statsRef.setValue(stats)
                .addOnSuccessListener {
                    Log.d("DuelBattleActivity", "Statystyki zapisane pomyślnie")
                    
                    // Zaktualizuj monety użytkownika (bez XP)
                    if (stageCompleted) {
                        val userRef = database.reference.child("users").child(currentUser!!.uid)
                        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                // Pobierz obecne wartości
                                val currentCoins = snapshot.child("coins").getValue(Int::class.java) ?: 0
                                
                                // Dodaj nagrody (tylko monety)
                                val updatedValues = HashMap<String, Any>()
                                updatedValues["coins"] = currentCoins + coinsGained
                                
                                // Aktualizuj wartości w bazie
                                userRef.updateChildren(updatedValues)
                                    .addOnSuccessListener {
                                        Log.d("DuelBattleActivity", "Monety zaktualizowane")
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e("DuelBattleActivity", "Błąd podczas aktualizacji monet: ${e.message}")
                                    }
                            }
                            
                            override fun onCancelled(error: DatabaseError) {
                                Log.e("DuelBattleActivity", "Błąd podczas odczytu danych użytkownika: ${error.message}")
                            }
                        })
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("DuelBattleActivity", "Błąd podczas zapisu statystyk: ${e.message}")
                }
        }
    }
    
    private fun addBronzeArmorToUserEquipment(statsId: String = "") {
        // Sprawdź najpierw, czy gracz zdobył 3 gwiazdki - tylko wtedy daj zbroję
        val stars = calculateStars()
        if (stars < 3) {
            Log.d("DuelBattleActivity", "Nie dodano zbroi - zdobyto tylko $stars gwiazdki (wymagane 3)")
            return
        }
        
        // Zapewnij, że użytkownik jest zalogowany
        if (currentUser == null) {
            Log.e("DuelBattleActivity", "Nie można dodać zbroi - użytkownik niezalogowany")
            return
        }

        // Utwórz jednoznaczne odniesienie do userId
        val userId = currentUser!!.uid
        val userRef = database.reference.child("users").child(userId)
        
        // Wykonaj operację synchronicznie na głównym wątku, żeby uniknąć problemów z wątkami
        userRef.child("equipment").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    // Odczytaj aktualne dane ekwipunku - obsługa różnych typów
                    val armorLevel = when {
                        snapshot.child("armorLevel").getValue(Long::class.java) != null ->
                            snapshot.child("armorLevel").getValue(Long::class.java)!!.toInt()
                        snapshot.child("armorLevel").getValue(Int::class.java) != null ->
                            snapshot.child("armorLevel").getValue(Int::class.java)!!
                        snapshot.child("armorLevel").getValue(String::class.java) != null ->
                            snapshot.child("armorLevel").getValue(String::class.java)!!.toInt()
                        else -> 1
                    }
                    
                    val wandLevel = when {
                        snapshot.child("wandLevel").getValue(Long::class.java) != null ->
                            snapshot.child("wandLevel").getValue(Long::class.java)!!.toInt()
                        snapshot.child("wandLevel").getValue(Int::class.java) != null ->
                            snapshot.child("wandLevel").getValue(Int::class.java)!!
                        snapshot.child("wandLevel").getValue(String::class.java) != null ->
                            snapshot.child("wandLevel").getValue(String::class.java)!!.toInt()
                        else -> 1
                    }
                    
                    val baseHp = when {
                        snapshot.child("baseHp").getValue(Long::class.java) != null ->
                            snapshot.child("baseHp").getValue(Long::class.java)!!.toInt()
                        snapshot.child("baseHp").getValue(Int::class.java) != null ->
                            snapshot.child("baseHp").getValue(Int::class.java)!!
                        snapshot.child("baseHp").getValue(String::class.java) != null ->
                            snapshot.child("baseHp").getValue(String::class.java)!!.toInt()
                        else -> 100
                    }
                    
                    val baseDamage = when {
                        snapshot.child("baseDamage").getValue(Long::class.java) != null ->
                            snapshot.child("baseDamage").getValue(Long::class.java)!!.toInt()
                        snapshot.child("baseDamage").getValue(Int::class.java) != null ->
                            snapshot.child("baseDamage").getValue(Int::class.java)!!
                        snapshot.child("baseDamage").getValue(String::class.java) != null ->
                            snapshot.child("baseDamage").getValue(String::class.java)!!.toInt()
                        else -> 10
                    }
                    
                    // Pobierz armorTier używając metody getValue bez określonego typu
                    val armorTierRaw = snapshot.child("armorTier").getValue()
                    
                    // Określ armorTier na podstawie różnych możliwych typów
                    val armorTier = when (armorTierRaw) {
                        is Long -> ArmorTier.fromInt((armorTierRaw.toInt() + 1))
                        is Int -> ArmorTier.fromInt((armorTierRaw + 1))
                        is String -> {
                            try {
                                // Próba odczytu jako enum lub konwersja na int
                                val tierInt = armorTierRaw.toIntOrNull()
                                if (tierInt != null) {
                                    ArmorTier.fromInt(tierInt + 1)
                                } else {
                                    try {
                                        ArmorTier.valueOf(armorTierRaw)
                                    } catch (e: Exception) {
                                        ArmorTier.BRONZE // domyślnie
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e("DuelBattleActivity", "Błąd konwersji armorTier: ${e.message}")
                                ArmorTier.BRONZE
                            }
                        }
                        else -> ArmorTier.BRONZE // domyślnie
                    }
                    
                    val bronzeArmorCount = when {
                        snapshot.child("bronzeArmorCount").getValue(Long::class.java) != null ->
                            snapshot.child("bronzeArmorCount").getValue(Long::class.java)!!.toInt()
                        snapshot.child("bronzeArmorCount").getValue(Int::class.java) != null ->
                            snapshot.child("bronzeArmorCount").getValue(Int::class.java)!!
                        snapshot.child("bronzeArmorCount").getValue(String::class.java) != null ->
                            snapshot.child("bronzeArmorCount").getValue(String::class.java)!!.toInt()
                        else -> 0
                    }
                    
                    val silverArmorCount = when {
                        snapshot.child("silverArmorCount").getValue(Long::class.java) != null ->
                            snapshot.child("silverArmorCount").getValue(Long::class.java)!!.toInt()
                        snapshot.child("silverArmorCount").getValue(Int::class.java) != null ->
                            snapshot.child("silverArmorCount").getValue(Int::class.java)!!
                        snapshot.child("silverArmorCount").getValue(String::class.java) != null ->
                            snapshot.child("silverArmorCount").getValue(String::class.java)!!.toInt()
                        else -> 0
                    }
                    
                    val goldArmorCount = when {
                        snapshot.child("goldArmorCount").getValue(Long::class.java) != null ->
                            snapshot.child("goldArmorCount").getValue(Long::class.java)!!.toInt()
                        snapshot.child("goldArmorCount").getValue(Int::class.java) != null ->
                            snapshot.child("goldArmorCount").getValue(Int::class.java)!!
                        snapshot.child("goldArmorCount").getValue(String::class.java) != null ->
                            snapshot.child("goldArmorCount").getValue(String::class.java)!!.toInt()
                        else -> 0
                    }
                    
                    // Odczytaj aktualny typ różdżki
                    val wandTypeStr = snapshot.child("wandType").getValue(String::class.java) ?: "FIRE"
                    val wandType = try {
                        WandType.valueOf(wandTypeStr)
                    } catch (e: Exception) {
                        WandType.FIRE // Domyślnie ogień
                    }
                    
                    // Utwórz obiekt Equipment z aktualnymi danymi
                    val currentEquipment = Equipment(
                        armorLevel = armorLevel,
                        wandLevel = wandLevel,
                        baseHp = baseHp,
                        baseDamage = baseDamage,
                        armorTier = armorTier,
                        bronzeArmorCount = bronzeArmorCount,
                        silverArmorCount = silverArmorCount,
                        goldArmorCount = goldArmorCount,
                        wandType = wandType
                    )
                    
                    Log.d("DuelBattleActivity", "Odczytane wartości: armorTier=${armorTier}, bronzeArmorCount=${bronzeArmorCount}")
                    
                    // Dodaj brązową zbroję
                    val updatedEquipment = currentEquipment.addBronzeArmor()
                    Log.d("DuelBattleActivity", "Przed aktualizacją: bronze=${currentEquipment.bronzeArmorCount}, Po: bronze=${updatedEquipment.bronzeArmorCount}")
                    
                    // Sprawdź, czy nastąpił upgrade zbroi
                    val upgradeMessage = if (updatedEquipment.armorTier != currentEquipment.armorTier) {
                        "Gratulacje! Twoja zbroja została ulepszona do ${updatedEquipment.armorTier.name}!\n" +
                        "Odblokowano nowy wygląd postaci z lepszą ochroną!"
                    } else {
                        "Zdobyłeś brązową zbroję! (${updatedEquipment.bronzeArmorCount}/10)\n" +
                        "Zbierz jeszcze ${10 - updatedEquipment.bronzeArmorCount} sztuk, aby awansować na wyższy poziom."
                    }
                    
                    // Tworzę mapę z danymi do aktualizacji
                    val equipmentMap = HashMap<String, Any>()
                    equipmentMap["armorLevel"] = updatedEquipment.armorLevel
                    equipmentMap["wandLevel"] = updatedEquipment.wandLevel
                    equipmentMap["baseHp"] = updatedEquipment.baseHp
                    equipmentMap["baseDamage"] = updatedEquipment.baseDamage
                    equipmentMap["armorTier"] = updatedEquipment.armorTier.ordinal  // zapisz jako int
                    equipmentMap["bronzeArmorCount"] = updatedEquipment.bronzeArmorCount
                    equipmentMap["silverArmorCount"] = updatedEquipment.silverArmorCount
                    equipmentMap["goldArmorCount"] = updatedEquipment.goldArmorCount
                    equipmentMap["wandType"] = updatedEquipment.wandType.name
                    
                    // Aktualizuj pojedyncze pola zamiast całego obiektu
                    userRef.child("equipment").updateChildren(equipmentMap)
                        .addOnSuccessListener {
                            // Pokaż komunikat o zdobyciu zbroi tylko po zakończeniu zapisu
                            // używając głównego wątku do aktualizacji UI
                            Handler(Looper.getMainLooper()).post {
                                Log.d("DuelBattleActivity", "Zbroja dodana pomyślnie: $upgradeMessage")
                                Toast.makeText(
                                    this@DuelBattleActivity,
                                    upgradeMessage,
                                    Toast.LENGTH_LONG
                                ).show()
                                
                                // Dodaj informację o aktualizacji do statystyk
                                if (statsId.isNotEmpty()) {
                                    val armorUpdate = mapOf("bronzeArmorAdded" to true)
                                    database.reference
                                        .child("users")
                                        .child(userId)
                                        .child("duelStats")
                                        .child(stageNumber.toString())
                                        .child(statsId)
                                        .updateChildren(armorUpdate)
                                }
                            }
                        }
                        .addOnFailureListener { e ->
                            // Pokaż błąd na głównym wątku
                            Handler(Looper.getMainLooper()).post {
                                Log.e("DuelBattleActivity", "Błąd podczas zapisu ekwipunku: ${e.message}")
                                e.printStackTrace()
                                Toast.makeText(
                                    this@DuelBattleActivity,
                                    "Wystąpił błąd podczas aktualizacji ekwipunku",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                } catch (e: Exception) {
                    Log.e("DuelBattleActivity", "Błąd podczas aktualizacji ekwipunku: ${e.message}")
                    e.printStackTrace() // Dodatkowe logowanie stosu błędów
                }
            }
            
            override fun onCancelled(error: DatabaseError) {
                Log.e("DuelBattleActivity", "Błąd odczytu danych ekwipunku: ${error.message}")
            }
        })
    }
    
    private fun calculateXpReward(): Int {
        // Zawsze zwracamy 0, ponieważ nie przyznajemy XP za pojedynki
        return 0
        
        // Stara implementacja (wykomentowana):
        /*
        val baseXp = 100
        val timeBonus = when {
            battleTime < 60000 -> 1.5f  // Under 1 minute
            battleTime < 120000 -> 1.2f // Under 2 minutes
            else -> 1.0f                // Over 2 minutes
        }
        val accuracyBonus = (correctAnswers.toFloat() / totalAnswers) * 1.5f
        return (baseXp * timeBonus * accuracyBonus).toInt()
        */
    }
    
    private fun calculateCoinsReward(): Int {
        val baseCoins = 50
        val timeBonus = when {
            battleTime < 60000 -> 1.5f
            battleTime < 120000 -> 1.2f
            else -> 1.0f
        }
        val accuracyBonus = (correctAnswers.toFloat() / totalAnswers) * 1.5f
        return (baseCoins * timeBonus * accuracyBonus).toInt()
    }
    
    private fun calculateStars(): Int {
        return when {
            correctAnswers == totalAnswers && battleTime < 60000 -> 3  // Perfect
            correctAnswers >= totalAnswers * 0.8 && battleTime < 120000 -> 2  // Good
            correctAnswers >= totalAnswers * 0.6 -> 1  // Pass
            else -> 0  // Fail
        }
    }
    
    private fun formatTime(millis: Long): String {
        val seconds = millis / 1000
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format("%02d:%02d", minutes, remainingSeconds)
    }
    
    private fun returnToDuelsActivity() {
        // Zapisz statystyki pojedynku przed powrotem
        if (stageCompleted) {
            saveBattleStatistics()
        }
        
        // Przygotuj wynik do zwrócenia do DuelsActivity
        val resultIntent = Intent()
        
        // Przekaż informacje o ukończonym etapie i nagrodach
        if (stageCompleted) {
            val stars = calculateStars()
            val xpGained = calculateXpReward()
            val coinsGained = calculateCoinsReward()
            
            resultIntent.putExtra("COMPLETED_STAGE", stageNumber)
            resultIntent.putExtra("STAGE_STARS", stars)
            resultIntent.putExtra("XP_GAINED", xpGained)
            resultIntent.putExtra("COINS_GAINED", coinsGained)
            resultIntent.putExtra("CORRECT_ANSWERS", correctAnswers)
            resultIntent.putExtra("TOTAL_ANSWERS", totalAnswers)
        }
        
        // Ustaw wynik i zamknij aktywność
        setResult(RESULT_OK, resultIntent)
        finish()
    }
    
    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        // Zamknij aktualnie otwarty dialog, jeśli istnieje
        if (activeDialog?.isShowing == true) {
            activeDialog?.dismiss()
            activeDialog = null
            return
        }
        
        // Pokaż komunikat o potwierdzeniu wyjścia
        showExitConfirmationDialog()
    }
    
    private fun showExitConfirmationDialog() {
        // Zamknij aktywny dialog, jeśli istnieje
        activeDialog?.dismiss()
        
        val dialog = Dialog(this)
        activeDialog = dialog
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_confirmation)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCancelable(true)
        
        val titleText = dialog.findViewById<TextView>(R.id.dialogTitle)
        val messageText = dialog.findViewById<TextView>(R.id.dialogMessage)
        val yesButton = dialog.findViewById<Button>(R.id.yesButton)
        val noButton = dialog.findViewById<Button>(R.id.noButton)
        
        titleText.text = "Opuścić pojedynek?"
        messageText.text = "Jeśli opuścisz pojedynek, stracisz cały postęp. Czy na pewno chcesz wyjść?"
        
        yesButton.setOnClickListener {
            dialog.dismiss()
            returnToDuelsActivity()
        }
        
        noButton.setOnClickListener {
            dialog.dismiss()
        }
        
        // Ustaw onDismissListener przed pokazaniem dialogu
        dialog.setOnDismissListener {
            activeDialog = null
        }
        
        // Make dialog take most of the screen
        val metrics = resources.displayMetrics
        val width = metrics.widthPixels
        dialog.window?.setLayout((width * 0.9).toInt(), WindowManager.LayoutParams.WRAP_CONTENT)
        
        dialog.show()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        
        // Zamknij wszystkie aktywne dialogi
        activeDialog?.let {
            if (it.isShowing) {
                it.dismiss()
            }
        }
        activeDialog = null
        
        // Zwolnij referencje do Firebase
        currentUser = null
        
        // Zamknij ewentualne timery, animacje itp.
        timer?.cancel()
        timer = null
        
        Log.d("DuelBattleActivity", "onDestroy - zasoby zwolnione")
    }

    private fun showFeedback(isCorrect: Boolean, damage: Int) {
        // Pokaż tekst informacji zwrotnej
        feedbackText.text = if (isCorrect) "Poprawna odpowiedź!" else "Błędna odpowiedź!"
        feedbackText.setTextColor(resources.getColor(
            if (isCorrect) R.color.correct_green else R.color.incorrect_red,
            theme
        ))
        feedbackText.visibility = View.VISIBLE

        // Pokaż tekst obrażeń
        val damageText = if (isCorrect) opponentDamageText else playerDamageText
        damageText.text = "-$damage"
        damageText.visibility = View.VISIBLE
        animateDamageText(damageText)

        // Pokaż efekt elementu (z 30% szansą)
        if (random.nextFloat() < 0.3f) {
            showElementEffect(if (isCorrect) playerCharacter.element else enemyCharacter.element)
        }

        // Sprawdź czy gra się skończyła
        if (playerHealth <= 0 || opponentHealth <= 0) {
            stageCompleted = opponentHealth <= 0
            
            // Ukryj feedback natychmiast po zakończeniu gry
            Handler(Looper.getMainLooper()).postDelayed({
                if (!isFinishing && !isDestroyed) {
                    // Ukryj feedbackText przed pokazaniem wyników
                    feedbackText.visibility = View.INVISIBLE
                    damageText.visibility = View.INVISIBLE
                    
                    // Pokaż wyniki
                    try {
                        showResults()
                    } catch (e: Exception) {
                        Log.e("DuelBattleActivity", "Błąd podczas pokazywania wyników: ${e.message}")
                        e.printStackTrace()
                        returnToDuelsActivity()
                    }
                }
            }, 250) // Jeszcze krótsze opóźnienie dla lepszej responsywności
        }
    }

    private fun loadMoreQuestionsIfNeeded() {
        // Jeśli liczba pytań jest mała, dodaj więcej pytań domyślnych
        if (questions.size < 15) {
            val moreQuestions = createMoreDefaultQuestions()
            questions.addAll(moreQuestions)
            
            // Można też zapisać te pytania w Firebase
            saveAdditionalQuestionsToFirebase(moreQuestions)
        }
    }

    private fun createMoreDefaultQuestions(): List<com.example.lingoheroesapp.models.Question> {
        val additionalQuestions = mutableListOf<com.example.lingoheroesapp.models.Question>()
        
        // Dodaj dodatkowe pytania z różnych kategorii
        when (stageNumber) {
            1 -> {
                // Poziom 1 - więcej podstawowych słówek
                additionalQuestions.add(com.example.lingoheroesapp.models.Question(
                    question = "Co oznacza 'Car' po polsku?",
                    correctAnswer = "Samochód",
                    incorrectAnswers = listOf("Pociąg", "Autobus", "Rower")
                ))
                additionalQuestions.add(com.example.lingoheroesapp.models.Question(
                    question = "Jak powiedzieć 'School' po polsku?",
                    correctAnswer = "Szkoła",
                    incorrectAnswers = listOf("Dom", "Praca", "Sklep")
                ))
                additionalQuestions.add(com.example.lingoheroesapp.models.Question(
                    question = "Co oznacza 'Water' po polsku?",
                    correctAnswer = "Woda",
                    incorrectAnswers = listOf("Powietrze", "Ogień", "Ziemia")
                ))
                additionalQuestions.add(com.example.lingoheroesapp.models.Question(
                    question = "Jak powiedzieć 'Book' po polsku?",
                    correctAnswer = "Książka",
                    incorrectAnswers = listOf("Zeszyt", "Długopis", "Ołówek")
                ))
                additionalQuestions.add(com.example.lingoheroesapp.models.Question(
                    question = "Co oznacza 'Friend' po polsku?",
                    correctAnswer = "Przyjaciel",
                    incorrectAnswers = listOf("Wróg", "Rodzina", "Kolega")
                ))
            }
            2 -> {
                // Poziom 2 - więcej zwierząt
                additionalQuestions.add(com.example.lingoheroesapp.models.Question(
                    question = "Jak powiedzieć 'Bird' po polsku?",
                    correctAnswer = "Ptak",
                    incorrectAnswers = listOf("Ryba", "Gad", "Owad")
                ))
                additionalQuestions.add(com.example.lingoheroesapp.models.Question(
                    question = "Co oznacza 'Fish' po polsku?",
                    correctAnswer = "Ryba",
                    incorrectAnswers = listOf("Ptak", "Żaba", "Wąż")
                ))
                additionalQuestions.add(com.example.lingoheroesapp.models.Question(
                    question = "Jak powiedzieć 'Elephant' po polsku?",
                    correctAnswer = "Słoń",
                    incorrectAnswers = listOf("Żyrafa", "Lew", "Tygrys")
                ))
                additionalQuestions.add(com.example.lingoheroesapp.models.Question(
                    question = "Co oznacza 'Bear' po polsku?",
                    correctAnswer = "Niedźwiedź",
                    incorrectAnswers = listOf("Wilk", "Lis", "Borsuk")
                ))
                additionalQuestions.add(com.example.lingoheroesapp.models.Question(
                    question = "Jak powiedzieć 'Snake' po polsku?",
                    correctAnswer = "Wąż",
                    incorrectAnswers = listOf("Jaszczurka", "Żółw", "Krokodyl")
                ))
            }
            3 -> {
                // Poziom 3 - więcej jedzenia
                additionalQuestions.add(com.example.lingoheroesapp.models.Question(
                    question = "Co oznacza 'Meat' po polsku?",
                    correctAnswer = "Mięso",
                    incorrectAnswers = listOf("Warzywo", "Owoc", "Nabiał")
                ))
                additionalQuestions.add(com.example.lingoheroesapp.models.Question(
                    question = "Jak powiedzieć 'Cheese' po polsku?",
                    correctAnswer = "Ser",
                    incorrectAnswers = listOf("Mleko", "Masło", "Jogurt")
                ))
                additionalQuestions.add(com.example.lingoheroesapp.models.Question(
                    question = "Co oznacza 'Vegetable' po polsku?",
                    correctAnswer = "Warzywo",
                    incorrectAnswers = listOf("Owoc", "Mięso", "Zboże")
                ))
                additionalQuestions.add(com.example.lingoheroesapp.models.Question(
                    question = "Jak powiedzieć 'Soup' po polsku?",
                    correctAnswer = "Zupa",
                    incorrectAnswers = listOf("Kanapka", "Sałatka", "Deser")
                ))
                additionalQuestions.add(com.example.lingoheroesapp.models.Question(
                    question = "Co oznacza 'Dinner' po polsku?",
                    correctAnswer = "Obiad",
                    incorrectAnswers = listOf("Śniadanie", "Kolacja", "Deser")
                ))
            }
            else -> {
                // Poziom 4 i wyżej - różne tematy
                additionalQuestions.add(com.example.lingoheroesapp.models.Question(
                    question = "Co oznacza 'Computer' po polsku?",
                    correctAnswer = "Komputer",
                    incorrectAnswers = listOf("Telefon", "Telewizor", "Radio")
                ))
                additionalQuestions.add(com.example.lingoheroesapp.models.Question(
                    question = "Jak powiedzieć 'Family' po polsku?",
                    correctAnswer = "Rodzina",
                    incorrectAnswers = listOf("Przyjaciele", "Znajomi", "Sąsiedzi")
                ))
                additionalQuestions.add(com.example.lingoheroesapp.models.Question(
                    question = "Co oznacza 'Hospital' po polsku?",
                    correctAnswer = "Szpital",
                    incorrectAnswers = listOf("Szkoła", "Biblioteka", "Kino")
                ))
                additionalQuestions.add(com.example.lingoheroesapp.models.Question(
                    question = "Jak powiedzieć 'Weather' po polsku?",
                    correctAnswer = "Pogoda",
                    incorrectAnswers = listOf("Klimat", "Środowisko", "Temperatura")
                ))
                additionalQuestions.add(com.example.lingoheroesapp.models.Question(
                    question = "Co oznacza 'Mountain' po polsku?",
                    correctAnswer = "Góra",
                    incorrectAnswers = listOf("Dolina", "Rzeka", "Morze")
                ))
            }
        }
        
        return additionalQuestions
    }

    private fun increaseCorrectAnswers() {
        correctAnswers++
        correctAnswersCount++
        
        // Zaktualizuj stan przycisku supermocy
        updateSuperPowerButtonState()
        
        // Log dla debugowania
        Log.d("DuelBattleActivity", "Poprawne odpowiedzi: $correctAnswersCount / próg: 3")
        
        // Wyświetl Toast z informacją, gdy odblokuje się możliwość użycia supermocy
        if (correctAnswersCount == 3) {
            Toast.makeText(this, "Odblokowano Super Moc! Kliknij przycisk, aby użyć.", Toast.LENGTH_SHORT).show()
        } else if (correctAnswersCount == 6) {
            Toast.makeText(this, "Odblokowano średnie Super Moce!", Toast.LENGTH_SHORT).show()
        } else if (correctAnswersCount == 9) {
            Toast.makeText(this, "Odblokowano potężne Super Moce!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleCorrectAnswer() {
        // Increase player score and correct answers count
        playerScore += 10
        playerScoreText.text = playerScore.toString()
        correctAnswersCount++
        
        // Check if player unlocked a superpower
        if (correctAnswersCount == nextSuperPowerUnlockAt) {
            updateSuperPowerButtonState()
            showSuperPowerSelectionDialog()
            // Set next milestone (every 3 correct answers)
            nextSuperPowerUnlockAt = correctAnswersCount + 3
        } else {
            updateSuperPowerButtonState()
        }
        
        // Check if special ability effect is triggered (zastępuje sprawdzanie efektu różdżki)
        val isSpecialEffectTriggered = random.nextFloat() < 0.25f // 25% szans na aktywację
        
        // Calculate damage based on character stats
        val damage = calculateDamage(
            playerCharacter.baseAttack, 
            enemyCharacter.defense,
            isSpecialEffectTriggered
        )
        
        // Apply element effect if triggered
        if (isSpecialEffectTriggered) {
            applyElementEffect(playerCharacter.element)
        }
        
        // Apply damage to opponent
        opponentHealth = (opponentHealth - damage).coerceAtLeast(0)
        
        // Show damage text
        playerDamageText.text = "+" + damage.toString()
        playerDamageText.visibility = View.VISIBLE
        animateDamageText(playerDamageText)
        
        // Update opponent's health bar
        animateHealthBar(opponentHealthBar, opponentHealthBar.progress, opponentHealth)
        updateHealthBarColor(opponentHealthBar, opponentHealth)
        opponentHealthText.text = "$opponentHealth/${enemyCharacter.hp}"
        
        // Check if opponent is defeated
        if (opponentHealth <= 0) {
            handleEnemyDefeated()
        } else {
            // Increment correct answers count and continue
            correctAnswersDisplayed++
            continueAfterAnswer()
        }
    }

    private fun applyElementEffect(elementType: ElementType) {
        // Show effect animation
        elementEffectContainer.visibility = View.VISIBLE
        elementEffectImage.setImageResource(when (elementType) {
            ElementType.FIRE -> R.drawable.effect_fire
            ElementType.ICE -> R.drawable.effect_ice
            ElementType.LIGHTNING -> R.drawable.effect_lightning
        })
        
        // Play animation
        val animation = AnimationUtils.loadAnimation(this, R.anim.effect_animation)
        elementEffectImage.startAnimation(animation)
        
        // Apply specific effect based on element type
        when (elementType) {
            ElementType.FIRE -> {
                // Fire effect: opponent takes additional damage
                val additionalDamage = calculateDamage(playerCharacter.baseAttack, enemyCharacter.defense, false)
                opponentHealth = (opponentHealth - additionalDamage).coerceAtLeast(0)
                opponentDamageText.text = "-${additionalDamage}"
                opponentDamageText.visibility = View.VISIBLE
                animateDamageText(opponentDamageText)
            }
            ElementType.ICE -> {
                // Ice effect: opponent can't attack next turn
                enemyFrozen = true
                opponentDamageText.text = "Odp. zablokowana!"
                opponentDamageText.visibility = View.VISIBLE
                animateDamageText(opponentDamageText)
            }
            ElementType.LIGHTNING -> {
                // Lightning effect: reduces opponent defense temporarily
                enemyDefenseReduced = true
                opponentDamageText.text = "Obrona przeciwnika obniżona!"
                opponentDamageText.visibility = View.VISIBLE
                animateDamageText(opponentDamageText)
            }
        }
        
        // Hide effect after delay
        Handler(Looper.getMainLooper()).postDelayed({
            elementEffectContainer.visibility = View.GONE
        }, 2000)
    }

    private fun showEffect(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun handleIncorrectAnswer() {
        // Opponent attacks player
        val opponentDamage = calculateEnemyDamage(
            enemyCharacter.attack,
            playerCharacter.baseDefense
        )
        
        // Apply active superpowers that affect damage taken
        val modifiedDamage = applyDefensiveSuperPowers(opponentDamage)
        
        // Apply damage to player
        playerHealth = (playerHealth - modifiedDamage).coerceAtLeast(0)
        
        // Show damage text
        opponentDamageText.text = "-" + modifiedDamage.toString()
        opponentDamageText.visibility = View.VISIBLE
        animateDamageText(opponentDamageText)
        
        // Update player's health bar
        animateHealthBar(playerHealthBar, playerHealthBar.progress, playerHealth)
        updateHealthBarColor(playerHealthBar, playerHealth)
        playerHealthText.text = "$playerHealth/${playerCharacter.hp}"
        
        // Check if player is defeated
        if (playerHealth <= 0) {
            showResults()
            return
        }
        
        // Play incorrect answer animation and feedback
        feedbackText.text = "Błędna odpowiedź!"
        feedbackText.setTextColor(resources.getColor(R.color.incorrect_red, theme))
        feedbackText.visibility = View.VISIBLE
        
        // Load next question after delay
        Handler(Looper.getMainLooper()).postDelayed({
            feedbackText.visibility = View.INVISIBLE
            opponentDamageText.visibility = View.INVISIBLE
            
            // Proceed to next question
            if (currentQuestionIndex < questions.size - 1) {
                currentQuestionIndex++
                displayQuestion()
            } else {
                // If we've gone through all questions but neither player nor opponent is defeated,
                // determine winner by score
                showResults()
            }
        }, 1500)
    }
    
    private fun calculateEnemyDamage(attack: Int, defense: Int, defenseMod: Double = 1.0, attackMod: Double = 1.0): Int {
        val modifiedAttack = (attack * attackMod).toInt()
        val modifiedDefense = (defense * defenseMod).toInt()
        val baseDamage = (modifiedAttack - (modifiedDefense * 0.5).toInt()).coerceAtLeast(1)
        
        // Add some randomness
        val minDamage = (baseDamage * 0.8).toInt()
        val maxDamage = (baseDamage * 1.2).toInt()
        return random.nextInt(maxDamage - minDamage + 1) + minDamage
    }

    private fun createInitialEnemyForStage() {
        // Tymczasowy przeciwnik do inicjalizacji, bez zależności od playerCharacter
        val elementTypes = ElementType.values()
        val randomElement = elementTypes[random.nextInt(elementTypes.size)]
        
        val enemyNames = arrayOf(
            "Ognisty Golem", "Lodowy Gigant", "Burza Błyskawic",
            "Piekielny Strażnik", "Zamrożony Demon", "Elektryczny Duszek",
            "Magma", "Mróz", "Iskra"
        )
        
        val nameIndex = when (randomElement) {
            ElementType.FIRE -> 0 + (stageNumber - 1) % 3
            ElementType.ICE -> 1 + (stageNumber - 1) % 3
            ElementType.LIGHTNING -> 2 + (stageNumber - 1) % 3
        }
        
        val name = enemyNames[nameIndex]
        
        // Skalowanie statystyk przeciwnika w zależności od poziomu etapu
        val baseHp = 80 + (stageNumber * 10)
        val baseAttack = 6 + (stageNumber)
        val baseDefense = 2 + (stageNumber / 2)
        
        enemyCharacter = DuelBattleEnemy(
            id = "enemy_stage_$stageNumber",
            name = name,
            element = randomElement,
            imageResId = when (randomElement) {
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

    // Handle player defeated
    private fun handlePlayerDefeated() {
        // Show defeat message
        feedbackText.text = "Zostałeś pokonany!"
        feedbackText.setTextColor(resources.getColor(R.color.incorrect_red, theme))
        feedbackText.visibility = View.VISIBLE
        
        // Show results
        Handler(Looper.getMainLooper()).postDelayed({
            showResults()
        }, 1500)
    }
    
    // Handle enemy defeated
    private fun handleEnemyDefeated() {
        // Show victory message
        feedbackText.text = "Pokonałeś przeciwnika!"
        feedbackText.setTextColor(resources.getColor(R.color.correct_green, theme))
        feedbackText.visibility = View.VISIBLE
        
        // Show results
        Handler(Looper.getMainLooper()).postDelayed({
            showResults()
        }, 1500)
    }
    
    // Continue to next question after answering
    private fun continueAfterAnswer() {
        // Display feedback
        if (feedbackText.text.isEmpty()) {
            feedbackText.text = "Następne pytanie..."
            feedbackText.setTextColor(resources.getColor(R.color.purple_500, theme))
        }
        
        feedbackText.visibility = View.VISIBLE
        
        // Load next question after delay
        Handler(Looper.getMainLooper()).postDelayed({
            feedbackText.visibility = View.INVISIBLE
            playerDamageText.visibility = View.INVISIBLE
            opponentDamageText.visibility = View.INVISIBLE
            
            // Proceed to next question
            if (currentQuestionIndex < questions.size - 1) {
                currentQuestionIndex++
                displayQuestion()
            } else {
                // If we've gone through all questions but neither player nor opponent is defeated,
                // determine winner by score
                showResults()
            }
        }, 1500)
    }
    
    // Show superpower selection dialog
    private fun showSuperPowerSelectionDialog() {
        // Zatrzymaj timer przed pokazaniem dialogu supermocy
        timer?.cancel()
        
        val dialog = com.example.lingoheroesapp.dialogs.SuperPowerSelectionDialog(
            this,
            correctAnswersCount
        ) { selectedSuperPower ->
            // Apply the selected superpower
            applySuperPower(selectedSuperPower)
            
            // Resetuj licznik poprawnych odpowiedzi po użyciu supermocy
            correctAnswersCount = 0
            
            // Log potwierdzający reset
            Log.d("DuelBattleActivity", "Reset licznika po użyciu supermocy. Nowa wartość: $correctAnswersCount")
            
            // Aktualizuj stan przycisku
            updateSuperPowerButtonState()
        }
        
        // Ustaw anulowanie dialogu
        dialog.setOnCancelListener {
            // Zrestartuj timer po anulowaniu
            startQuestionTimer()
        }
        
        // Wyświetl dialog
        dialog.show()
    }
    
    // Apply selected superpower
    private fun applySuperPower(superPower: SuperPower) {
        // Dodaj log dla debugowania
        Log.d("DuelBattleActivity", "Używam supermocy: ${superPower.name}, typ: ${superPower.effectType}")
        
        // Create active superpower instance
        val activeSuperPower = ActiveSuperPower(
            superPowerId = superPower.id,
            remainingDuration = superPower.duration,
            appliedAt = System.currentTimeMillis(),
            superPowerData = superPower
        )
        
        // Dodaj do aktywnych supermocy (z usunięciem duplikatów tego samego typu)
        val existingSuperPowerIndex = activeSuperPowers.indexOfFirst { 
            it.superPowerData?.effectType == superPower.effectType 
        }
        
        if (existingSuperPowerIndex >= 0) {
            // Zastąp istniejącą supermoc nową
            activeSuperPowers[existingSuperPowerIndex] = activeSuperPower
        } else {
            // Dodaj nową supermoc
            activeSuperPowers.add(activeSuperPower)
        }
        
        // Apply immediate effects
        when (superPower.effectType) {
            "damage" -> {
                // Immediate damage (e.g. Iskra)
                val baseDamage = playerCharacter.baseAttack
                val additionalDamage = if (superPower.isPercentage) {
                    (baseDamage * superPower.effectValue / 100).toInt()
                } else {
                    superPower.effectValue.toInt()
                }
                
                opponentHealth = (opponentHealth - additionalDamage).coerceAtLeast(0)
                
                // Show damage text
                opponentDamageText.text = "-$additionalDamage"
                opponentDamageText.visibility = View.VISIBLE
                animateDamageText(opponentDamageText)
                
                // Update opponent's health bar
                animateHealthBar(opponentHealthBar, opponentHealthBar.progress, opponentHealth)
                updateHealthBarColor(opponentHealthBar, opponentHealth)
                opponentHealthText.text = "$opponentHealth/${enemyCharacter.hp}"
                
                // Show feedback text
                feedbackText.text = superPower.name
                feedbackText.visibility = View.VISIBLE
                
                // Check if opponent is defeated
                if (opponentHealth <= 0) {
                    handleEnemyDefeated()
                } else {
                    // Continue game after delay
                    continueSuperPowerEffect()
                }
            }
            "healing" -> {
                // Immediate healing (e.g. Małe leczenie)
                val maxHealth = playerCharacter.hp
                val healAmount = if (superPower.isPercentage) {
                    (maxHealth * superPower.effectValue / 100).toInt()
                } else {
                    superPower.effectValue.toInt()
                }
                
                playerHealth = (playerHealth + healAmount).coerceAtMost(maxHealth)
                
                // Show healing text
                playerDamageText.text = "+$healAmount"
                playerDamageText.setTextColor(resources.getColor(R.color.health_good, theme))
                playerDamageText.visibility = View.VISIBLE
                animateDamageText(playerDamageText)
                
                // Reset text color after animation
                Handler(Looper.getMainLooper()).postDelayed({
                    playerDamageText.setTextColor(resources.getColor(R.color.damage_red, theme))
                }, 1500)
                
                // Update player's health bar
                animateHealthBar(playerHealthBar, playerHealthBar.progress, playerHealth)
                updateHealthBarColor(playerHealthBar, playerHealth)
                playerHealthText.text = "$playerHealth/$maxHealth"
                
                // Show feedback text
                feedbackText.text = superPower.name
                feedbackText.visibility = View.VISIBLE
                
                // Continue game after delay
                continueSuperPowerEffect()
            }
            "dodge" -> {
                // Unik - następny atak potwora nie trafi (e.g. Unik)
                feedbackText.text = "Przygotowano ${superPower.name}!"
                feedbackText.setTextColor(resources.getColor(R.color.health_good, theme))
                feedbackText.visibility = View.VISIBLE
                
                // Log dla debugowania
                Log.d("DuelBattleActivity", "Aktywowano supermoc ${superPower.name}. Następny atak potwora chybi.")
                
                // Continue game after delay
                continueSuperPowerEffect()
            }
            "double_attack" -> {
                if (superPower.duration == 1) {
                    // Immediate double attack (e.g. Podwójne uderzenie)
                    val damage = calculateDamage(
                        playerCharacter.baseAttack,
                        enemyCharacter.defense,
                        false
                    )
                    
                    opponentHealth = (opponentHealth - damage).coerceAtLeast(0)
                    
                    // Show damage text
                    opponentDamageText.text = "-$damage"
                    opponentDamageText.visibility = View.VISIBLE
                    animateDamageText(opponentDamageText)
                    
                    // Update opponent's health bar
                    animateHealthBar(opponentHealthBar, opponentHealthBar.progress, opponentHealth)
                    updateHealthBarColor(opponentHealthBar, opponentHealth)
                    opponentHealthText.text = "$opponentHealth/${enemyCharacter.hp}"
                    
                    // Show feedback text
                    feedbackText.text = "${superPower.name}!"
                    feedbackText.visibility = View.VISIBLE
                    
                    // Check if opponent is defeated
                    if (opponentHealth <= 0) {
                        handleEnemyDefeated()
                    } else {
                        // Continue game after delay
                        continueSuperPowerEffect()
                    }
                } else {
                    // Passive effect for multiple turns (e.g. Czas chaosu)
                    feedbackText.text = "${superPower.name} aktywowano na ${superPower.duration} tury!"
                    feedbackText.visibility = View.VISIBLE
                    
                    // Continue game after delay
                    continueSuperPowerEffect()
                }
            }
            "damage_boost" -> {
                // Passive effect - increased damage on next attack (e.g. Mini-kryt, Cios krytyczny)
                feedbackText.text = "${superPower.name} - następny atak zada ${superPower.effectValue.toInt()}% więcej obrażeń!"
                feedbackText.visibility = View.VISIBLE
                
                // Continue game after delay
                continueSuperPowerEffect()
            }
            "damage_reduction" -> {
                // Passive effect - reduced damage (e.g. Tarcza energetyczna, Odporność)
                val durationText = if (superPower.duration > 1) " przez ${superPower.duration} tury" else ""
                feedbackText.text = "${superPower.name} - obrażenia zmniejszone o ${superPower.effectValue.toInt()}%$durationText!"
                feedbackText.visibility = View.VISIBLE
                
                // Continue game after delay
                continueSuperPowerEffect()
            }
            "damage_over_time" -> {
                // Passive effect - damage over time (e.g. Ognista burza)
                feedbackText.text = "${superPower.name} - zadaje ${superPower.effectValue.toInt()}% obrażeń przez ${superPower.duration} tury!"
                feedbackText.visibility = View.VISIBLE
                
                // Continue game after delay
                continueSuperPowerEffect()
            }
            "stun" -> {
                // Passive effect - enemy can't attack (e.g. Paraliż furii)
                feedbackText.text = "${superPower.name} - przeciwnik nie może atakować przez ${superPower.duration} tury!"
                feedbackText.visibility = View.VISIBLE
                
                // Continue game after delay
                continueSuperPowerEffect()
            }
            "execute" -> {
                // Execute effect for low HP targets (e.g. Pieczęć zagłady, Armagedon)
                val maxHealth = enemyCharacter.hp
                val currentHealthPercent = (opponentHealth * 100) / maxHealth
                
                // Sprawdź warunek dla egzekucji
                val executionThreshold = if (superPower.id == "armageddon") 25 else 30
                
                if (currentHealthPercent < executionThreshold) {
                    // Oblicz dodatkowe obrażenia
                    val baseDamage = playerCharacter.baseAttack
                    val additionalDamage = if (superPower.isPercentage) {
                        (baseDamage * superPower.effectValue / 100).toInt()
                    } else {
                        superPower.effectValue.toInt()
                    }
                    
                    opponentHealth = (opponentHealth - additionalDamage).coerceAtLeast(0)
                    
                    // Show damage text
                    opponentDamageText.text = "-$additionalDamage"
                    opponentDamageText.visibility = View.VISIBLE
                    animateDamageText(opponentDamageText)
                    
                    // Update opponent's health bar
                    animateHealthBar(opponentHealthBar, opponentHealthBar.progress, opponentHealth)
                    updateHealthBarColor(opponentHealthBar, opponentHealth)
                    opponentHealthText.text = "$opponentHealth/${enemyCharacter.hp}"
                    
                    // Show feedback text
                    feedbackText.text = "${superPower.name} - egzekucja!"
                    feedbackText.visibility = View.VISIBLE
                    
                    // Check if opponent is defeated
                    if (opponentHealth <= 0) {
                        handleEnemyDefeated()
                    } else {
                        // Continue game after delay
                        continueSuperPowerEffect()
                    }
                } else {
                    // Nie spełniono warunku egzekucji
                    feedbackText.text = "${superPower.name} - przeciwnik ma za dużo HP (${currentHealthPercent}%)!"
                    feedbackText.visibility = View.VISIBLE
                    
                    // Continue game after delay
                    continueSuperPowerEffect()
                }
            }
            "reflect" -> {
                // Passive effect - reflect damage (e.g. Odbicie mocy)
                feedbackText.text = "${superPower.name} - następny atak zostanie odbity!"
                feedbackText.visibility = View.VISIBLE
                
                // Continue game after delay
                continueSuperPowerEffect()
            }
            "life_steal" -> {
                // Passive effect - steal health (e.g. Wampiryzm)
                feedbackText.text = "${superPower.name} - odzyskasz ${superPower.effectValue.toInt()}% zadanych obrażeń jako HP!"
                feedbackText.visibility = View.VISIBLE
                
                // Continue game after delay
                continueSuperPowerEffect()
            }
            "stun_damage" -> {
                // Immediate stun + damage (e.g. Grom z nieba)
                val baseDamage = playerCharacter.baseAttack
                val additionalDamage = if (superPower.isPercentage) {
                    (baseDamage * superPower.effectValue / 100).toInt()
                } else {
                    superPower.effectValue.toInt()
                }
                
                opponentHealth = (opponentHealth - additionalDamage).coerceAtLeast(0)
                
                // Show damage text
                opponentDamageText.text = "-$additionalDamage"
                opponentDamageText.visibility = View.VISIBLE
                animateDamageText(opponentDamageText)
                
                // Update opponent's health bar
                animateHealthBar(opponentHealthBar, opponentHealthBar.progress, opponentHealth)
                updateHealthBarColor(opponentHealthBar, opponentHealth)
                opponentHealthText.text = "$opponentHealth/${enemyCharacter.hp}"
                
                // Show feedback text
                feedbackText.text = "${superPower.name} - paraliż + obrażenia!"
                feedbackText.visibility = View.VISIBLE
                
                // Check if opponent is defeated
                if (opponentHealth <= 0) {
                    handleEnemyDefeated()
                } else {
                    // Continue game after delay
                    continueSuperPowerEffect()
                }
            }
            "freeze" -> {
                // Passive effect - freeze enemy (e.g. Lodowa burza)
                feedbackText.text = "${superPower.name} - przeciwnik zamrożony na ${superPower.duration} tury!"
                feedbackText.visibility = View.VISIBLE
                
                // Continue game after delay
                continueSuperPowerEffect()
            }
            "enemy_attack_reduction" -> {
                // Passive effect - reduce enemy attack (e.g. Rozproszenie)
                feedbackText.text = "${superPower.name} - siła ataku przeciwnika zmniejszona o ${superPower.effectValue.toInt()}%!"
                feedbackText.visibility = View.VISIBLE
                
                // Continue game after delay
                continueSuperPowerEffect()
            }
            else -> {
                // Show feedback text for other effects
                feedbackText.text = "${superPower.name} aktywowano!"
                feedbackText.visibility = View.VISIBLE
                
                // Continue game after delay
                continueSuperPowerEffect()
            }
        }
        
        // Save active superpowers to Firebase
        saveSuperPowersToFirebase()
    }
    
    // Calculate damage with superpower modifiers
    private fun calculateDamage(attack: Int, defense: Int, isSpecialEffectTriggered: Boolean = false): Int {
        var damageMultiplier = 1.0
        
        // Apply element effect multiplier if triggered
        if (isSpecialEffectTriggered && playerCharacter.element == ElementType.FIRE) {
            val multiplier = playerCharacter.element.getEffectiveness(ElementType.FIRE)
            damageMultiplier *= multiplier
        }
        
        // Apply damage boost superpowers
        activeSuperPowers.forEach { activeSuperPower ->
            val superPower = activeSuperPower.superPowerData
            if (superPower != null) {
                when (superPower.effectType) {
                    "damage_boost" -> {
                        damageMultiplier += superPower.effectValue / 100.0
                    }
                }
            }
        }
        
        // Special effect multiplier (from wand)
        val specialEffectMultiplier = if (isSpecialEffectTriggered) 1.5 else 1.0
        
        // Calculate base damage
        val baseDamage = (attack - (defense * 0.5).toInt()).coerceAtLeast(1)
        
        // Apply randomness and multipliers
        val minDamage = (baseDamage * 0.8).toInt()
        val maxDamage = (baseDamage * 1.2).toInt()
        val randomDamage = random.nextInt(maxDamage - minDamage + 1) + minDamage
        
        // Final damage with multipliers
        return (randomDamage * damageMultiplier * specialEffectMultiplier).toInt()
    }
    
    // Apply defensive superpowers to reduce incoming damage
    private fun applyDefensiveSuperPowers(incomingDamage: Int): Int {
        var damageReduction = 0.0
        var dodgeChance = 0.0
        var reflectDamage = false
        
        // Sprawdź czy są aktywne jakiekolwiek supermoce
        Log.d("DuelBattleActivity", "Sprawdzam ${activeSuperPowers.size} aktywnych supermocy")
        
        // Apply defensive superpowers
        activeSuperPowers.forEach { activeSuperPower ->
            val superPower = activeSuperPower.superPowerData
            if (superPower != null) {
                Log.d("DuelBattleActivity", "Sprawdzam supermoc: ${superPower.name}, typ: ${superPower.effectType}")
                
                when (superPower.effectType) {
                    "damage_reduction" -> {
                        damageReduction += superPower.effectValue / 100.0
                        Log.d("DuelBattleActivity", "Redukcja obrażeń o ${superPower.effectValue}%")
                    }
                    "dodge" -> {
                        // Zagwarantowany unik (100% szans) dla supermocy Unik
                        dodgeChance = 1.0 // 100% szans na unik
                        Log.d("DuelBattleActivity", "Ustawiono 100% szans na unik dla supermocy ${superPower.name}")
                    }
                    "reflect" -> {
                        reflectDamage = true
                        Log.d("DuelBattleActivity", "Aktywowano odbicie obrażeń")
                    }
                }
            }
        }
        
        // Process dodge - jeśli mamy supermoc uniku, na pewno unikamy obrażeń
        if (dodgeChance > 0) {
            // Show dodge message
            feedbackText.text = "Unik! Potwór nie trafia!"
            feedbackText.setTextColor(resources.getColor(R.color.health_good, theme))
            feedbackText.visibility = View.VISIBLE
            
            // Dodaj animację uniku
            Handler(Looper.getMainLooper()).post {
                val originalY = playerCharacterView.translationY
                val dodgeAnimation = ObjectAnimator.ofFloat(playerCharacterView, "translationY", originalY, originalY - 50f, originalY)
                dodgeAnimation.duration = 500
                dodgeAnimation.start()
            }
            
            // Log dla debugowania
            Log.d("DuelBattleActivity", "Wykonano unik! Obrażenia zredukowane do 0")
            
            // Remove the dodge superpower after use (jednorazowa)
            activeSuperPowers.removeIf { it.superPowerData?.effectType == "dodge" }
            
            // Zapisz zmiany w aktywnych supermocach
            saveSuperPowersToFirebase()
            
            // Zawsze zwracaj 0 obrażeń dla uniku
            return 0 // No damage taken
        }
        
        // Process reflection
        if (reflectDamage) {
            // Apply reflected damage to opponent
            opponentHealth = (opponentHealth - incomingDamage).coerceAtLeast(0)
            
            // Show reflection message
            feedbackText.text = "Odbicie mocy! Przeciwnik otrzymuje własne obrażenia!"
            feedbackText.setTextColor(resources.getColor(R.color.fire_red, theme))
            feedbackText.visibility = View.VISIBLE
            
            // Update opponent's health bar
            animateHealthBar(opponentHealthBar, opponentHealthBar.progress, opponentHealth)
            updateHealthBarColor(opponentHealthBar, opponentHealth)
            opponentHealthText.text = "$opponentHealth/${enemyCharacter.hp}"
            
            // Check if opponent is defeated
            if (opponentHealth <= 0) {
                handleEnemyDefeated()
            }
            
            // Remove the reflect superpower after use (jednorazowa)
            activeSuperPowers.removeIf { it.superPowerData?.effectType == "reflect" }
            
            // Zapisz zmiany w aktywnych supermocach
            saveSuperPowersToFirebase()
            
            return 0 // No damage taken
        }
        
        // Apply damage reduction (capped at 90%)
        val damageMultiplier = 1.0 - damageReduction.coerceAtMost(0.9)
        val reducedDamage = (incomingDamage * damageMultiplier).toInt()
        
        // Log dla debugowania
        if (damageReduction > 0) {
            Log.d("DuelBattleActivity", "Obrażenia zredukowane z $incomingDamage do $reducedDamage")
        }
        
        return reducedDamage
    }
    
    // Process superpower effects at the start of a new turn
    private fun processSuperPowerEffects() {
        // Process damage over time effects
        val dotEffects = activeSuperPowers.filter { it.superPowerData?.effectType == "damage_over_time" }
        
        // Pokaż efekty DoT jeśli są aktywne
        if (dotEffects.isNotEmpty()) {
            var dotDamage = 0
            
            for (effect in dotEffects) {
                val superPower = effect.superPowerData!!
                val baseDamage = playerCharacter.baseAttack
                val damage = if (superPower.isPercentage) {
                    (baseDamage * superPower.effectValue / 100).toInt()
                } else {
                    superPower.effectValue.toInt()
                }
                
                dotDamage += damage
                
                // Pokaż efekt w UI
                feedbackText.text = "${superPower.name} (${effect.remainingDuration}): -$damage HP!"
                feedbackText.visibility = View.VISIBLE
            }
            
            if (dotDamage > 0) {
                opponentHealth = (opponentHealth - dotDamage).coerceAtLeast(0)
                
                // Show damage text
                opponentDamageText.text = "-$dotDamage"
                opponentDamageText.visibility = View.VISIBLE
                animateDamageText(opponentDamageText)
                
                // Update opponent's health bar
                animateHealthBar(opponentHealthBar, opponentHealthBar.progress, opponentHealth)
                updateHealthBarColor(opponentHealthBar, opponentHealth)
                opponentHealthText.text = "$opponentHealth/${enemyCharacter.hp}"
                
                // Check if opponent is defeated
                if (opponentHealth <= 0) {
                    handleEnemyDefeated()
                    return
                }
            }
        }
        
        // Efekty mrozące i paraliżujące (freeze, stun)
        val stunEffects = activeSuperPowers.filter { 
            it.superPowerData?.effectType == "stun" || it.superPowerData?.effectType == "stun_damage"
        }
        
        val freezeEffects = activeSuperPowers.filter { 
            it.superPowerData?.effectType == "freeze" 
        }
        
        if (stunEffects.isNotEmpty()) {
            // Pokaż informację o ogłuszeniu
            val effect = stunEffects.first()
            feedbackText.text = "Przeciwnik jest sparaliżowany - pozostało tur: ${effect.remainingDuration}!"
            feedbackText.setTextColor(resources.getColor(R.color.lightning_yellow, theme))
            feedbackText.visibility = View.VISIBLE
        } else if (freezeEffects.isNotEmpty()) {
            // Pokaż informację o zamrożeniu
            val effect = freezeEffects.first()
            feedbackText.text = "Przeciwnik jest zamrożony - pozostało tur: ${effect.remainingDuration}!"
            feedbackText.setTextColor(resources.getColor(R.color.ice_blue, theme))
            feedbackText.visibility = View.VISIBLE
        }
        
        // Decrease remaining duration for all active superpowers
        for (i in activeSuperPowers.size - 1 downTo 0) {
            val activeSuperPower = activeSuperPowers[i]
            activeSuperPower.remainingDuration--
            
            // Remove expired superpowers
            if (activeSuperPower.remainingDuration <= 0) {
                // Pokaż informację o zakończeniu efektu
                val superPower = activeSuperPower.superPowerData
                if (superPower != null) {
                    Log.d("DuelBattleActivity", "Supermoc ${superPower.name} wygasła")
                }
                
                activeSuperPowers.removeAt(i)
            }
        }
        
        // Save active superpowers to Firebase
        saveSuperPowersToFirebase()
    }
    
    // Save active superpowers to Firebase
    private fun saveSuperPowersToFirebase() {
        val userId = currentUser?.uid ?: return
        val activeSuperPowersRef = database.reference.child("users").child(userId).child("activeSuperPowers")
        
        // Clear existing superpowers
        activeSuperPowersRef.removeValue()
        
        // Add each active superpower
        activeSuperPowers.forEachIndexed { index, activeSuperPower ->
            activeSuperPowersRef.child(index.toString()).setValue(activeSuperPower)
        }
    }
    
    // Load active superpowers from Firebase
    private fun loadSuperPowersFromFirebase() {
        val userId = currentUser?.uid ?: return
        val activeSuperPowersRef = database.reference.child("users").child(userId).child("activeSuperPowers")
        
        activeSuperPowersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    activeSuperPowers.clear()
                    
                    for (superPowerSnapshot in snapshot.children) {
                        val activeSuperPower = superPowerSnapshot.getValue(ActiveSuperPower::class.java)
                        if (activeSuperPower != null) {
                            activeSuperPowers.add(activeSuperPower)
                        }
                    }
                }
            }
            
            override fun onCancelled(error: DatabaseError) {
                Log.e("DuelBattleActivity", "Error loading active superpowers: ${error.message}")
            }
        })
    }
    
    // Load game progress including correct answers count
    private fun loadGameProgress() {
        val userId = currentUser?.uid ?: return
        val progressRef = database.reference.child("users").child(userId).child("duelProgress")
        
        progressRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    correctAnswersCount = snapshot.child("correctAnswersCount").getValue(Int::class.java) ?: 0
                    
                    // Calculate next superpower unlock milestone
                    nextSuperPowerUnlockAt = (correctAnswersCount / 3 + 1) * 3
                    
                    // Aktualizuj stan przycisku supermocy
                    updateSuperPowerButtonState()
                }
            }
            
            override fun onCancelled(error: DatabaseError) {
                Log.e("DuelBattleActivity", "Error loading game progress: ${error.message}")
            }
        })
    }
    
    // Save game progress including correct answers count
    private fun saveGameProgress() {
        val userId = currentUser?.uid ?: return
        val progressRef = database.reference.child("users").child(userId).child("duelProgress")
        
        val progressMap = mapOf(
            "correctAnswersCount" to correctAnswersCount,
            "lastPlayedStage" to stageNumber,
            "lastUpdated" to System.currentTimeMillis()
        )
        
        progressRef.updateChildren(progressMap)
    }

    // Function that continues the game after using a superpower
    private fun continueSuperPowerEffect() {
        // Restart the timer
        startQuestionTimer()
        
        // Restart the game flow
        Handler(Looper.getMainLooper()).postDelayed({
            // Hide feedback text
            feedbackText.visibility = View.INVISIBLE
            playerDamageText.visibility = View.INVISIBLE
            opponentDamageText.visibility = View.INVISIBLE
            
            // Continue with the current question
            displayQuestion()
        }, 1500) // Daj czas na zobaczenie efektu supermocy
    }

    // Function that updates the state of the superpower button
    private fun updateSuperPowerButtonState() {
        // The superpower button is active when we have at least 3 correct answers
        val hasUnlockedSuperPower = correctAnswersCount >= 3
        
        // Update button appearance based on state
        specialAbilityButton.isEnabled = hasUnlockedSuperPower
        
        // Update button text to show progress
        val remainingAnswers = (3 - correctAnswersCount).coerceAtLeast(0)
        if (hasUnlockedSuperPower) {
            specialAbilityButton.text = "Super Moc" 
            specialAbilityButton.alpha = 1.0f
            // Set background color for the active button
            specialAbilityButton.setBackgroundResource(R.drawable.button_special_ability_active)
        } else {
            specialAbilityButton.text = "Super Moc za $remainingAnswers odpowiedzi"
            specialAbilityButton.alpha = 0.7f
            // Set default background color for the inactive button
            specialAbilityButton.setBackgroundResource(R.drawable.button_special_ability)
        }
    }

    // Sprawdza czy przeciwnik jest ogłuszony lub zamrożony i nie może atakować
    private fun isEnemyStunnedOrFrozen(): Boolean {
        // Sprawdzenie czy którakolwiek z supermocy stun/freeze jest aktywna
        return activeSuperPowers.any { 
            val powerType = it.superPowerData?.effectType
            powerType == "stun" || powerType == "stun_damage" || powerType == "freeze"
        }
    }
} 