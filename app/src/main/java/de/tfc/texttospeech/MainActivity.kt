package de.tfc.texttospeech

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import de.tfc.texttospeech.ui.theme.TextToSpeechTheme
import java.util.Locale

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var isTtsInitialized by mutableStateOf(value = false)

    // State exposed to Compose
    var textState = mutableStateOf("")
    var speechRateState = mutableFloatStateOf(1.0f)
    var pitchState = mutableFloatStateOf(1.0f)
    var selectedLocaleState = mutableStateOf(Locale.getDefault())
    var isSpeakingState = mutableStateOf(value = false)
    var historyState = mutableStateOf(listOf<String>())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)

        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                isSpeakingState.value = true
            }

            override fun onDone(utteranceId: String?) {
                isSpeakingState.value = false
            }

            @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
            override fun onError(utteranceId: String?) {
                isSpeakingState.value = false
            }
        })

        setContent {
            TextToSpeechTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        isInitialized = isTtsInitialized,
                        textState = textState,
                        speechRate = speechRateState,
                        pitch = pitchState,
                        selectedLocale = selectedLocaleState,
                        isSpeaking = isSpeakingState,
                        history = historyState,
                        onSpeak = { speakOut(it) },
                        onStop = { stopSpeaking() },
                    )
                }
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(selectedLocaleState.value)
            if ((result == TextToSpeech.LANG_MISSING_DATA) || (result == TextToSpeech.LANG_NOT_SUPPORTED)) {
                Log.e("TTS", getString(R.string.lang_not_supported))
                Toast.makeText(this, getString(R.string.lang_not_supported), Toast.LENGTH_SHORT).show()
            } else {
                isTtsInitialized = true
            }
        } else {
            Log.e("TTS", getString(R.string.tts_init_failed))
            Toast.makeText(this, getString(R.string.tts_init_failed), Toast.LENGTH_SHORT).show()
        }
    }

    private fun speakOut(text: String) {
        if (text.isBlank()) {
            Toast.makeText(this, getString(R.string.text_empty), Toast.LENGTH_SHORT).show()
            return
        }
        tts?.setSpeechRate(speechRateState.floatValue)
        tts?.setPitch(pitchState.floatValue)
        tts?.language = selectedLocaleState.value

        isSpeakingState.value = true
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "TTS_UTTERANCE_ID")

        // Add to history if not already top
        val currentHistory = historyState.value.toMutableList()
        if (!currentHistory.contains(text)) {
            currentHistory.add(0, text)
            if (currentHistory.size > 10) currentHistory.removeAt(currentHistory.size - 1)
            historyState.value = currentHistory
        }
    }

    private fun stopSpeaking() {
        tts?.stop()
        isSpeakingState.value = false
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    isInitialized: Boolean,
    textState: MutableState<String>,
    speechRate: MutableFloatState,
    pitch: MutableFloatState,
    selectedLocale: MutableState<Locale>,
    isSpeaking: MutableState<Boolean>,
    history: MutableState<List<String>>,
    onSpeak: (String) -> Unit,
    onStop: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }

    val locales: List<Pair<Locale, String>> = listOf(
        Pair(Locale.getDefault(), "System Default"),
        Pair(Locale.ENGLISH, "English"),
        Pair(Locale.GERMAN, "Deutsch"),
        Pair(Locale.FRENCH, "Français"),
        Pair(Locale.forLanguageTag("es"), "Español"),
        Pair(Locale.ITALIAN, "Italiano")
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Text Input Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        OutlinedTextField(
                            value = textState.value,
                            onValueChange = { textState.value = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            placeholder = { Text(stringResource(R.string.enter_text_to_speak)) },
                            shape = RoundedCornerShape(12.dp),
                            trailingIcon = {
                                if (textState.value.isNotEmpty()) {
                                    IconButton(onClick = { textState.value = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.clear))
                                    }
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${textState.value.length} chars",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        clipboardManager.getText()?.text?.let {
                                            textState.value = it
                                        }
                                    },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(stringResource(R.string.paste))
                                }

                                OutlinedButton(
                                    onClick = {
                                        if (textState.value.isNotEmpty()) {
                                            clipboardManager.setText(AnnotatedString(textState.value))
                                        }
                                    },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(stringResource(R.string.copy))
                                }
                            }
                        }
                    }
                }
            }

            // Speech Controls Card (Rate, Pitch, Language)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Settings",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        // Speech Rate Slider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${stringResource(R.string.speech_rate)}: ${String.format(Locale.US, "%.1f", speechRate.floatValue)}x",
                                modifier = Modifier.width(140.dp),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Slider(
                                value = speechRate.floatValue,
                                onValueChange = { speechRate.floatValue = it },
                                valueRange = 0.5f..2.0f,
                                steps = 14,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Pitch Slider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${stringResource(R.string.pitch)}: ${String.format(Locale.US, "%.1f", pitch.floatValue)}x",
                                modifier = Modifier.width(140.dp),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Slider(
                                value = pitch.floatValue,
                                onValueChange = { pitch.floatValue = it },
                                valueRange = 0.5f..2.0f,
                                steps = 14,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Language Selection
                        Text(
                            text = stringResource(R.string.language),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(locales) { (loc, label) ->
                                val selected = selectedLocale.value == loc
                                FilterChip(
                                    selected = selected,
                                    onClick = { selectedLocale.value = loc },
                                    label = { Text(label) },
                                    leadingIcon = if (selected) {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null
                                )
                            }
                        }
                    }
                }
            }

            // Speak & Action Buttons
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            if (isSpeaking.value) {
                                onStop()
                            } else {
                                onSpeak(textState.value)
                            }
                        },
                        enabled = isInitialized,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSpeaking.value) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = if (isSpeaking.value) Icons.Default.Stop else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isSpeaking.value) stringResource(R.string.stop) else stringResource(R.string.speak),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    OutlinedButton(
                        onClick = { textState.value = "" },
                        modifier = Modifier
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = null)
                    }
                }
            }

            // History Section
            if (history.value.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.padding(top = 8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.history),
                                style = MaterialTheme.typography.titleMedium
                            )
                            TextButton(onClick = { history.value = emptyList() }) {
                                Text("Clear")
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        history.value.forEach { item ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable { textState.value = item },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(12.dp)
                                        .fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = item,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 2,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    IconButton(onClick = { onSpeak(item) }) {
                                        Icon(
                                            Icons.Default.PlayArrow,
                                            contentDescription = "Speak",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
