package de.tfc.texttospeech

import android.app.Activity
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.OnInitListener
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import java.util.Locale

class MainActivity : Activity(), OnInitListener {
    private var tts: TextToSpeech? = null
    private var editText: EditText? = null
    private var speakButton: Button? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        editText = findViewById(R.id.editText)
        speakButton = findViewById(R.id.speakButton)

        // Initialize TextToSpeech
        tts = TextToSpeech(this, this)

        // Use safe call operator to set the click listener
        speakButton?.setOnClickListener { speakOut() }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            // Set language
            val result = tts!!.setLanguage(Locale.getDefault())

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS", "The Language not supported!")
                Toast.makeText(this, "The Language not supported!", Toast.LENGTH_SHORT).show()
            } else {
                speakButton?.isEnabled = true
            }
        } else {
            Log.e("TTS", "Initialization Failed!")
            Toast.makeText(this, "Initialization Failed!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun speakOut() {
        val text = editText!!.text.toString()
        if (text.isNotEmpty()) {
            tts!!.speak(text, TextToSpeech.QUEUE_FLUSH, null, "")
        } else {
            Toast.makeText(this, "Please enter text to speak", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        // Shutdown TTS when activity is destroyed
        if (tts != null) {
            tts!!.stop()
            tts!!.shutdown()
        }
        super.onDestroy()
    }
}