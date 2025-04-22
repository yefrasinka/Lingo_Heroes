package com.example.lingoheroesapp.activities

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
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
    private var currentQuestionIndex = 0
    private var playerHealth = 100
    private var opponentHealth = 100
    private var playerScore = 0
    private var opponentScore = 0
    private var correctAnswerIndex = 0
    private var isAnswerSelected = false
    private var timer: CountDownTimer? = null
    private val random = Random()
    
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
    
    // Dodaję flagę do śledzenia wyświetlenia raportu
    private var reportShown = false
    
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
        
        // Inicjalizacja menedżera animacji
        animationManager = DuelAnimationManager()
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
        
        // Usunięto odwołania do playerAvatar i opponentAvatar
        
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
        
        // Initialize health texts
        playerHealthText.text = "$playerHealth/${playerCharacter.hp}"
        opponentHealthText.text = "$opponentHealth/${enemyCharacter.hp}"
        
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
            specialAbilityName = when (elementType) {
                ElementType.FIRE -> "Ściana Ognia"
                ElementType.ICE -> "Lodowa Zbroja"
                ElementType.LIGHTNING -> "Porażenie"
                else -> "Specjalna Zdolność"
            },
            specialAbilityDescription = when (elementType) {
                ElementType.FIRE -> "Zadaje większe obrażenia przeciwnikowi"
                ElementType.ICE -> "Zmniejsza otrzymywane obrażenia"
                ElementType.LIGHTNING -> "Zwiększa precyzję odpowiedzi"
                else -> "Specjalna zdolność postaci"
            },
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
        
        // Zaktualizuj obrazek postaci gracza (poprawiona ścieżka do zasobu)
        playerCharacterView.setImageResource(R.drawable.duels_characters_wizard)
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
        if (specialAbilityCooldown > 0 || isAnimationInProgress) {
            return
        }
        
        isAnimationInProgress = true
        
        // Wywołaj animację specjalnej zdolności
        animationManager.animateSpecialAbility(
            elementEffectContainer,
            elementEffectImage,
            playerCharacter.element.toWandType()
        ) {
            // Zastosuj efekt specjalnej zdolności
            applySpecialAbilityEffect()
            
            // Ustaw cooldown
            specialAbilityCooldown = playerCharacter.specialAbilityCooldown
            updateSpecialAbilityButtonText()
            
            isAnimationInProgress = false
        }
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
        // Zastosuj efekt specjalnej zdolności w zależności od elementu
        when (playerCharacter.element) {
            ElementType.FIRE -> {
                // Ściana Ognia - zadaje dodatkowe obrażenia
                val damage = calculateDamage(true, 0) * 1.5f
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
            else -> {
                // Generyczna zdolność dla nieznanych typów
                playerScore += 10
                playerHealth += 10
                if (playerHealth > 100) playerHealth = 100
                
                // Pokaż informację zwrotną
                feedbackText.text = "Specjalna Zdolność!"
                feedbackText.setTextColor(resources.getColor(R.color.colorAccent, theme))
                feedbackText.visibility = View.VISIBLE
            }
        }
        
        // Aktualizuj UI
        updateScoreAndHealth()
        
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
        
        if (!isCorrect) {
            val question = questions[currentQuestionIndex]
            mistakes.add("Pytanie: ${question.question}\n" +
                        "Twoja odpowiedź: ${answerButtons[selectedIndex].text}\n" +
                        "Poprawna odpowiedź: ${question.correctAnswer}")
            
            // Podświetl niepoprawną odpowiedź
            animationManager.animateIncorrectAnswer(answerButtons[selectedIndex])
            
            // Podświetl poprawną odpowiedź
            animationManager.animateCorrectAnswer(answerButtons[correctAnswerIndex])
        } else {
            // Podświetl poprawną odpowiedź
            animationManager.animateCorrectAnswer(answerButtons[selectedIndex])
        }
        
        // Calculate damage based on correctness and time
        val damage = calculateDamage(isCorrect, timeSpent)
        
        if (isCorrect) {
            playerScore += damage
            opponentHealth -= damage
            correctAnswers++
        } else {
            opponentScore += damage
            playerHealth -= damage
        }
        
        totalAnswers++
        
        // Update UI
        updateScoreAndHealth()
        showFeedback(isCorrect, damage)
        
        // Ukryj pytanie z animacją
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
                // Animacja ataku przeciwnika
                animationManager.animateMonsterAttack(
                    playerCharacterView,
                    monsterCharacterView,
                    attackAnimationContainer,
                    monsterAttackAnimation,
                    playerDamageText,
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
            }
        }
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
    
    private fun displayQuestion() {
        if (currentQuestionIndex >= questions.size) {
            // Gdy skończą się pytania, pokaż wyniki
            showResults()
            return
        }

        // Resetuj stan
        isAnswerSelected = false
        
        // Pokaż pytanie z animacją
        animationManager.animateQuestionAppear(questionContainer) {
            // Pokaż przyciski odpowiedzi
            answerButtons.forEach { it.isEnabled = true }
            
            // Ustaw pytanie
            val question = questions[currentQuestionIndex]
            questionText.text = question.question
            
            // Ustaw odpowiedzi - używamy właściwości answers z klasy Question zamiast własnej metody
            val answers = ArrayList(question.answers)
            // Losowo mieszamy odpowiedzi
            answers.shuffle()
            
            answerButtons.forEachIndexed { index, button ->
                button.text = answers[index]
                button.background = ContextCompat.getDrawable(this, R.drawable.button_answer_normal)
            }
            
            // Zapisz poprawną odpowiedź
            correctAnswerIndex = answers.indexOf(question.correctAnswer)
            
            // Rozpocznij licznik czasu
            startQuestionTimer()
            
            // Zapamiętaj czas rozpoczęcia pytania
            currentQuestionStartTime = System.currentTimeMillis()
        }
    }
    
    private fun startQuestionTimer() {
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

    private fun saveAdditionalQuestionsToFirebase(additionalQuestions: List<com.example.lingoheroesapp.models.Question>) {
        val questionsRef = database.reference.child("duelStages").child(stageNumber.toString()).child("additionalQuestions")
        
        for (i in additionalQuestions.indices) {
            val question = additionalQuestions[i]
            val questionMap = mapOf(
                "question" to question.question,
                "correctAnswer" to question.correctAnswer,
                "incorrectAnswers" to question.incorrectAnswers
            )
            questionsRef.child("additionalQuestion_$i").setValue(questionMap)
        }
    }

    private fun handleCorrectAnswer() {
        // Increase player score
        playerScore += 10
        playerScoreText.text = playerScore.toString()
        
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
            showResults()
            return
        }
        
        // Play correct answer animation and feedback
        feedbackText.text = "Poprawna odpowiedź!"
        feedbackText.setTextColor(resources.getColor(R.color.correct_green, theme))
        feedbackText.visibility = View.VISIBLE
        
        // Load next question after delay
        Handler(Looper.getMainLooper()).postDelayed({
            feedbackText.visibility = View.INVISIBLE
            playerDamageText.visibility = View.INVISIBLE
            
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

    private fun calculateDamage(attack: Int, defense: Int, isSpecialEffectTriggered: Boolean): Int {
        val baseDamage = (attack - defense * 0.5).coerceAtLeast(1.0).toInt()
        
        // Apply element effect multiplier if triggered
        if (isSpecialEffectTriggered && playerCharacter.element == ElementType.FIRE) {
            val multiplier = playerCharacter.element.getEffectiveness(ElementType.FIRE)
            return (baseDamage * multiplier).toInt()
        }
        
        return baseDamage
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
                opponentHealth -= additionalDamage
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
        // Check if ice shield effect is active
        var defenseMod = 1.0
        if (playerDefenseIncreased) {
            defenseMod = 1.2 // 20% bonus to defense from ice element
            playerDefenseIncreased = false
        }
        
        // Check if opponent defense is reduced
        var attackMod = 1.0
        if (enemyDefenseReduced) {
            attackMod = 1.3 // 30% bonus to attack against reduced defense
            enemyDefenseReduced = false
        }
        
        // Calculate damage based on enemy character stats
        val damage = calculateEnemyDamage(
            enemyCharacter.baseAttack,
            playerCharacter.defense,
            defenseMod,
            attackMod
        )

        // Apply damage to player
        playerHealth = (playerHealth - damage).coerceAtLeast(0)
        
        // Show damage text
        opponentDamageText.text = "+" + damage.toString()
        opponentDamageText.visibility = View.VISIBLE
        animateDamageText(opponentDamageText)
        
        // Update health bar
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
        val baseDamage = (modifiedAttack - modifiedDefense * 0.5).coerceAtLeast(1.0).toInt()
        
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
} 