package com.example.lingoheroesapp.views

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.AnimationUtils
import com.example.lingoheroesapp.R
import com.example.lingoheroesapp.models.DuelLevel
import com.example.lingoheroesapp.models.Position

class DuelMapView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val levels = mutableListOf<DuelLevel>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val pathPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 10f
        color = Color.WHITE
    }
    private val path = Path()
    
    private var selectedLevel: DuelLevel? = null
    private var onLevelSelectedListener: ((DuelLevel) -> Unit)? = null

    // Animacja pulsowania dla aktywnego poziomu
    private var pulseScale = 1f
    private var isPulsing = false

    init {
        // Włącz obsługę dotknięć
        isClickable = true
        isFocusable = true
    }

    fun setLevels(newLevels: List<DuelLevel>) {
        levels.clear()
        levels.addAll(newLevels)
        invalidate()
    }

    fun setOnLevelSelectedListener(listener: (DuelLevel) -> Unit) {
        onLevelSelectedListener = listener
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Rysuj ścieżkę między poziomami
        drawPath(canvas)

        // Rysuj poziomy
        levels.forEach { level ->
            drawLevel(canvas, level)
        }
    }

    private fun drawPath(canvas: Canvas) {
        path.reset()
        
        levels.forEachIndexed { index, level ->
            val x = level.position.x.toFloat()
            val y = level.position.y.toFloat()
            
            if (index == 0) {
                path.moveTo(x, y)
            } else {
                // Dodaj krzywą Beziera między poziomami
                val prevLevel = levels[index - 1]
                val controlX = (prevLevel.position.x + level.position.x) / 2f
                val controlY = prevLevel.position.y.toFloat()
                
                path.quadTo(controlX, controlY, x, y)
            }
        }
        
        canvas.drawPath(path, pathPaint)
    }

    private fun drawLevel(canvas: Canvas, level: DuelLevel) {
        val x = level.position.x.toFloat()
        val y = level.position.y.toFloat()
        
        // Rysuj okrąg reprezentujący poziom
        paint.apply {
            style = Paint.Style.FILL
            color = if (level.isLocked) Color.GRAY else Color.GREEN
        }
        
        val radius = if (level == selectedLevel) 40f * pulseScale else 40f
        canvas.drawCircle(x, y, radius, paint)
        
        // Rysuj obramowanie
        paint.apply {
            style = Paint.Style.STROKE
            strokeWidth = 5f
            color = Color.WHITE
        }
        canvas.drawCircle(x, y, radius, paint)
        
        // Rysuj tekst poziomu
        paint.apply {
            style = Paint.Style.FILL
            color = Color.WHITE
            textSize = 30f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(level.name, x, y + 10f, paint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val touchX = event.x
                val touchY = event.y
                
                // Sprawdź, czy dotknięto jakiegoś poziomu
                levels.forEach { level ->
                    val dx = touchX - level.position.x
                    val dy = touchY - level.position.y
                    val distance = Math.sqrt((dx * dx + dy * dy).toDouble())
                    
                    if (distance < 40 && !level.isLocked) {
                        selectedLevel = level
                        startPulseAnimation()
                        onLevelSelectedListener?.invoke(level)
                        invalidate()
                        return true
                    }
                }
            }
        }
        return super.onTouchEvent(event)
    }

    private fun startPulseAnimation() {
        isPulsing = true
        val animation = AnimationUtils.loadAnimation(context, R.anim.pulse)
        animation.setAnimationListener(object : android.view.animation.Animation.AnimationListener {
            override fun onAnimationStart(animation: android.view.animation.Animation?) {}
            override fun onAnimationEnd(animation: android.view.animation.Animation?) {
                isPulsing = false
                invalidate()
            }
            override fun onAnimationRepeat(animation: android.view.animation.Animation?) {}
        })
        startAnimation(animation)
    }
} 