package com.example.alarm

import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.view.inputmethod.InputMethodManager
import android.app.AlertDialog
import android.content.Intent
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.media.MediaPlayer
import android.text.InputType
import android.widget.EditText
import android.widget.TextView
import android.widget.TimePicker
import android.widget.Toast
import android.app.AlarmManager
import android.app.PendingIntent

import java.util.Calendar



class Create_Alarm : AppCompatActivity() {

    private lateinit var textView2: TextView
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var textView7: TextView

    private var isEditMode = false
    private var editPosition = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.alarm_create)

        // Инициализация SharedPreferences
        sharedPreferences = getSharedPreferences("AlarmPrefs", MODE_PRIVATE)

        textView2 = findViewById(R.id.textView2)
        textView7 = findViewById(R.id.textView7)

        // Проверяем, открыта ли активность для редактирования
        val alarmDetails = intent.getStringExtra("alarmDetails")
        editPosition = intent.getIntExtra("position", -1)

        if (alarmDetails != null && editPosition != -1) {
            isEditMode = true
            loadAlarmDetails(alarmDetails) // Загружаем данные будильника
        } else {
            updateAlarmNameTextView()
        }

        val backButton: Button = findViewById(R.id.button_back)
        backButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        val saveButton: Button = findViewById(R.id.save)
        saveButton.setOnClickListener {
            saveAlarmSettings()
        }

        val repeatButton: Button = findViewById(R.id.repeat_button)
        repeatButton.setOnClickListener {
            showDaySelectionDialog()
        }

        val chooseMusicButton: Button = findViewById(R.id.choose_music_button)
        chooseMusicButton.setOnClickListener {
            showMelodySelectionDialog()
        }

        val nazvanie: Button = findViewById(R.id.nazvanie)
        nazvanie.setOnClickListener {
            showAlarmNameInputDialog()
        }

        // Установка слушателя окна для правильного отображения
        ViewCompat.setOnApplyWindowInsetsListener(repeatButton) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private val daysOfWeek = arrayOf(
        "Понедельник", "Вторник", "Среда",
        "Четверг", "Пятница", "Суббота", "Воскресенье"
    )

    private val dayAbbreviations = mapOf(
        "Понедельник" to "Пн",
        "Вторник" to "Вт",
        "Среда" to "Ср",
        "Четверг" to "Чт",
        "Пятница" to "Пт",
        "Суббота" to "Сб",
        "Воскресенье" to "Вс"
    )

    private val selectedDays = mutableSetOf<String>()

    private fun showDaySelectionDialog() {
        selectedDays.clear()
        textView2.text = "Выберите дни"

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Выберите дни недели")
        builder.setMultiChoiceItems(daysOfWeek, null) { _, which, isChecked ->
            val selectedDay = daysOfWeek[which]
            if (isChecked) {
                selectedDays.add(selectedDay)
            } else {
                selectedDays.remove(selectedDay)
            }
        }

        builder.setPositiveButton("Готово") { _, _ ->
            updateSelectedDays()
        }

        builder.setNegativeButton("Отмена") { dialog, _ ->
            dialog.dismiss()
            setNoRepeat()
        }

        builder.show()
    }

    private fun updateSelectedDays() {
        if (selectedDays.isEmpty()) {
            setNoRepeat()
        } else {
            // Превращаем выбранные дни в строку
            val dayList = selectedDays.toList()
            val dayString = when (dayList.size) {
                7 -> "Каждый день"
                else -> dayList.joinToString(", ") { day -> dayAbbreviations[day] ?: day }
            }

            textView2.text = dayString

            // Сохраняем выбранные дни в SharedPreferences как множество
            val editor = sharedPreferences.edit()
            editor.putStringSet("selectedDays", selectedDays)
            editor.apply()

            // Уведомляем пользователя
            Toast.makeText(this, "Вы выбрали: $dayString", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setNoRepeat() {
        textView2.text = "Никогда >"

        // Сохраняем "Никогда" в SharedPreferences
        val editor = sharedPreferences.edit()
        editor.putString("selectedDays", "Никогда >")
        editor.apply()
    }

    private var mediaPlayer: MediaPlayer? = null
    private var currentMelodyIndex = -1

    private fun showMelodySelectionDialog() {
        val melodies = arrayOf("Мелодия 1", "Мелодия 2", "Мелодия 3")
        val melodyIds = arrayOf(R.raw.music1, R.raw.music2, R.raw.music3)

        // Устанавливаем текст "Нет мелодии" по умолчанию
        findViewById<TextView>(R.id.textView5).text = "Нет мелодии"

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Выберите мелодию")

        // Устанавливаем список выбора мелодий
        builder.setSingleChoiceItems(melodies, currentMelodyIndex) { dialog, which ->
            if (currentMelodyIndex != which) {
                currentMelodyIndex = which
                val selectedMelody = melodies[which]
                saveSelectedMelody(selectedMelody)
                playMelody(melodyIds[which])
                findViewById<TextView>(R.id.textView5).text = selectedMelody
            }
        }

        // Кнопка "Готово"
        builder.setPositiveButton("Готово") { dialog, _ ->
            // Сохраняем выбранную мелодию
            if (currentMelodyIndex != -1) {
                val selectedMelody = melodies[currentMelodyIndex]
                saveSelectedMelody(selectedMelody)
                findViewById<TextView>(R.id.textView5).text = selectedMelody
            }
            dialog.dismiss()
        }

        // Кнопка "Отмена"
        builder.setNegativeButton("Отмена") { dialog, _ ->
            // Останавливаем воспроизведение мелодии
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null

            // Сбрасываем выбор мелодии
            currentMelodyIndex = -1
            saveSelectedMelody(null) // Сохраняем null как выбранную мелодию
            findViewById<TextView>(R.id.textView5).text = "Нет мелодии"
            dialog.dismiss()
        }

        // Обработчик закрытия диалога
        builder.setOnDismissListener {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
            currentMelodyIndex = -1
        }

        builder.show()
    }

    private fun saveSelectedMelody(melody: String?) {
        val editor = sharedPreferences.edit()
        editor.putString("selectedMelody", melody)
        editor.apply() // Сохраняем изменения
    }

    private fun playMelody(melodyResId: Int) {
        mediaPlayer?.stop()
        mediaPlayer?.release()

        mediaPlayer = MediaPlayer.create(this, melodyResId)
        mediaPlayer?.start()

        mediaPlayer?.setOnCompletionListener {
            it.release()
            mediaPlayer = null
        }
    }

    private var alarmName = "Будильник"
    private val origname = alarmName

    private fun showAlarmNameInputDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Введите название будильника")

        val input = EditText(this)

        // Установите пустой текст в EditText
        input.setText("")
        input.requestFocus()

        // Устанавливаем тип ввода для EditText, чтобы разрешить ввод букв и цифр
        input.inputType = InputType.TYPE_CLASS_TEXT

        // Открываем клавиатуру
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT)

        builder.setView(input)

        builder.setPositiveButton("Готово") { dialog, _ ->
            alarmName = input.text.toString()
            saveAlarmName(alarmName)
            updateAlarmNameTextView()

            // Скрываем клавиатуру
            imm.hideSoftInputFromWindow(input.windowToken, 0)
            dialog.dismiss()
        }

        builder.setNegativeButton("Отмена") { dialog, _ ->
            alarmName = origname
            updateAlarmNameTextView()
            imm.hideSoftInputFromWindow(input.windowToken, 0)
            dialog.cancel()
        }

        val dialog = builder.create()
        dialog.show()

        // Автоматическое открытие клавиатуры
        input.post {
            input.requestFocus()
            imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    private fun saveAlarmName(name: String) {
        val editor = sharedPreferences.edit()
        editor.putString("alarmName", name)
        editor.apply()
    }

    private fun updateAlarmNameTextView() {
        textView7.text = alarmName
    }

    private fun loadAlarmDetails(alarmDetails: String) {
        // Разбираем строку с данными будильника
        val lines = alarmDetails.split("\n")
        if (lines.size >= 3) {
            // Название будильника
            val name = lines[0].trim()
            textView7.text = name
            alarmName = name

            // Дни повторения
            val days = lines[1].substringAfter("Дни: ").trim()
            if (days != "Никогда") {
                // Преобразуем сокращённые дни в полные названия
                val selectedDaysList = days.split(", ").map { abbreviation ->
                    dayAbbreviations.entries.find { it.value == abbreviation }?.key ?: abbreviation
                }.toSet()
                selectedDays.clear()
                selectedDays.addAll(selectedDaysList)
                // Отображаем дни в сокращённом формате
                textView2.text = selectedDaysList.joinToString(", ") { dayAbbreviations[it] ?: it }
            } else {
                textView2.text = "Никогда"
            }

            // Время
            val time = lines[2].substringAfter("Время: ").trim()
            val timePicker: TimePicker = findViewById(R.id.timePicker)
            val (hour, minute) = time.split(":").map { it.toInt() }
            timePicker.hour = hour
            timePicker.minute = minute

            // Мелодия
            val melody = lines[3].substringAfter("Мелодия: ").trim()
            findViewById<TextView>(R.id.textView5).text = melody
        }
    }

    private fun saveAlarmSettings() {
        val timePicker: TimePicker = findViewById(R.id.timePicker)

        val hour = timePicker.hour
        val minute = timePicker.minute

        // Форматируем время в формат 00:00
        val formattedTime = String.format("%02d:%02d", hour, minute)

        // Получаем выбранные дни
        val selectedDays = sharedPreferences.getStringSet("selectedDays", emptySet()) ?: emptySet()

        // Проверяем, выбраны ли дни повтора
        if (selectedDays.isEmpty() || textView2.text == "Никогда >") {
            // Подсветка поля выбора дней
            textView2.setTextColor(Color.RED)
            // Показываем сообщение об ошибке
            Toast.makeText(this, "Пожалуйста, выберите дни недели для повтора", Toast.LENGTH_SHORT).show()
            return // Прерываем выполнение метода
        } else {
            // Сброс подсветки, если дни выбраны
            textView2.setTextColor(Color.BLACK) // или ваш стандартный цвет текста
        }

        // Сортируем дни по порядку
        val sortedDays = daysOfWeek.filter { it in selectedDays }
        // Преобразуем дни в сокращённый формат
        val joinedDays = if (sortedDays.isEmpty()) {
            "Никогда >"
        } else {
            sortedDays.joinToString(", ") { dayAbbreviations[it] ?: it }
        }

        // Получаем выбранную мелодию, если она не выбрана, устанавливаем "нет мелодии"
        val currentMelody = sharedPreferences.getString("selectedMelody", null) ?: "нет мелодии"

        val alarmDetails = """
        ${alarmName.uppercase()}
        Дни: $joinedDays
        Время: $formattedTime
        Мелодия: $currentMelody
    """.trimIndent()

        // Сохранение будильника в SharedPreferences
        saveAlarmToPreferences(alarmDetails)

        // Переход к MainActivity
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("alarmDetails", alarmDetails)
        }
        startActivity(intent)
    }

    private fun saveAlarmToPreferences(alarmDetails: String) {
        if (isEditMode && editPosition != -1) {
            val editor = sharedPreferences.edit()
            editor.putString("alarm_$editPosition", alarmDetails)
            editor.apply()
        } else {
            val alarmCount = sharedPreferences.getInt("alarmCount", 0)
            val editor = sharedPreferences.edit()
            editor.putString("alarm_$alarmCount", alarmDetails)
            editor.putInt("alarmCount", alarmCount + 1)
            editor.apply()
        }
    }
}
