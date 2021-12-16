package com.vishnu.dosmatrixwatchface

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.support.wearable.activity.WearableActivity
import android.widget.EditText
import android.widget.Toast
import com.google.android.material.slider.Slider
import kotlinx.android.synthetic.main.activity_configuration.*


class ConfigurationActivity : WearableActivity() {

    lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_configuration)
        // Enables Always-on
        setAmbientEnabled()
        val matrixSwitch = findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.swMatrix)
        val cursorSwitch = findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.swCursor)
        val randomSwitch = findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.swRandom)
        val sliderFont = findViewById<Slider>(R.id.sliderFont)
        val sliderMatrix = findViewById<Slider>(R.id.sliderMatrix)
        val sliderDensity = findViewById<Slider>(R.id.sliderDensity)
        val sliderX = findViewById<Slider>(R.id.xPositionSlider)
        val sliderY = findViewById<Slider>(R.id.yPositionslider)
        val swTermina = findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.swTermina)
        val timeText = findViewById<EditText>(R.id.editName)
        val saveBtnTime = findViewById<com.google.android.material.button.MaterialButton>(R.id.saveBtnTime)

        sharedPreferences = getSharedPreferences(
                getString(R.string.preference_file_name),
                Context.MODE_PRIVATE
        )
        val matrixFlag = sharedPreferences.getInt("matrixFlag", 1)
        val cursorFlag = sharedPreferences.getInt("cursorFlag", 1)
        val randomFlag = sharedPreferences.getInt("randomFlag", 1)
        val terminaFlag = sharedPreferences.getInt("terminaFlag", 0)
        val mainFontSize = sharedPreferences.getFloat("mainFontSize", 60f)
        val matrixFontSize = sharedPreferences.getFloat("matrixFontSize", 25f)
        val matrixDensityValue = sharedPreferences.getFloat("sliderDensity", 100f)
        val xData = sharedPreferences.getFloat("xData", 60f)
        val yData = sharedPreferences.getFloat("yData", 60f)
        val saveBtnTimeValue = sharedPreferences.getString("saveBtnTimeValue", "time")

        sliderFont.value = mainFontSize
        sliderMatrix.value = matrixFontSize
        sliderDensity.value = matrixDensityValue
        sliderX.value = xData
        sliderY.value = yData

//        txtsample.textSize = (mainFontSize - (mainFontSize / 3))
//        txtMatix.textSize = matrixFontSize - (matrixFontSize / 3)

        if (terminaFlag == 1) {
            swTermina.toggle()
        }

        if (matrixFlag == 1) {
            matrixSwitch.toggle()
        }

        if (randomFlag == 1) {
            randomSwitch.toggle()
        }

        if (cursorFlag == 1) {
            cursorSwitch.toggle()
        }

        saveBtnTime.setOnClickListener {
            sharedPreferences.edit().putString("saveBtnTimeValue", timeText.text.toString()).apply()
            Toast.makeText(this@ConfigurationActivity, "SAVED", Toast.LENGTH_SHORT).show()
        }

        matrixSwitch.setOnClickListener {
            if (matrixSwitch.isChecked) {
                sharedPreferences.edit().putInt("matrixFlag", 1).apply()
            } else {
                sharedPreferences.edit().putInt("matrixFlag", 0).apply()
            }
        }

        randomSwitch.setOnClickListener {
            if (randomSwitch.isChecked) {
                sharedPreferences.edit().putInt("randomFlag", 1).apply()
            } else {
                sharedPreferences.edit().putInt("randomFlag", 0).apply()
            }
        }

        cursorSwitch.setOnClickListener {
            if (cursorSwitch.isChecked) {
                sharedPreferences.edit().putInt("cursorFlag", 1).apply()
            } else {
                sharedPreferences.edit().putInt("cursorFlag", 0).apply()
            }
        }

        swTermina.setOnClickListener {
            if (swTermina.isChecked) {
                sharedPreferences.edit().putInt("terminaFlag", 1).apply()
            } else {
                sharedPreferences.edit().putInt("terminaFlag", 0).apply()
            }
        }

        sliderFont.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {
                // Responds to when slider's touch event is being started
            }

            override fun onStopTrackingTouch(slider: Slider) {
                // Responds to when slider's touch event is being stopped
            }
        })

        sliderFont.addOnChangeListener { slider, value, fromUser ->

            //txtsample.textSize = (value - (value / 3))
            sharedPreferences.edit().putFloat("mainFontSize", value).apply()

            // Responds to when slider's value is changed
        }

        sliderMatrix.addOnChangeListener { slider, value, fromUser ->

            //txtMatix.textSize = (value - (value / 3))
            sharedPreferences.edit().putFloat("matrixFontSize", value).apply()

            // Responds to when slider's value is changed
        }

        sliderDensity.addOnChangeListener { slider, value, fromUser ->

            sharedPreferences.edit().putFloat("sliderDensity", value).apply()

            // Responds to when slider's value is changed
        }

        xPositionSlider.addOnChangeListener { slider, value, fromUser ->

            sharedPreferences.edit().putFloat("xData", value).apply()

            // Responds to when slider's value is changed
        }

        yPositionslider.addOnChangeListener { slider, value, fromUser ->

            sharedPreferences.edit().putFloat("yData", value).apply()

            // Responds to when slider's value is changed
        }


    }


}