package com.example.alarm

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private var alarmList = mutableListOf<String>()
    private lateinit var adapter: ArrayAdapter<String>
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        listView = findViewById(R.id.listview)
        sharedPreferences = getSharedPreferences("AlarmPrefs", MODE_PRIVATE)

        adapter = ArrayAdapter(this, R.layout.list_item, alarmList)
        listView.adapter = adapter

        loadAlarms()

        // Обработчик нажатия на элемент списка
        listView.setOnItemClickListener { _, _, position, _ ->
            val selectedAlarm = alarmList[position]
            openEditAlarmActivity(selectedAlarm, position)
        }

        val button3: Button = findViewById(R.id.button3)
        button3.setOnClickListener {
            val intent = Intent(this, Create_Alarm::class.java)
            startActivity(intent)
        }

        val button2: Button = findViewById(R.id.button2)
        button2.setOnClickListener {
            showDeleteAlarmDialog()
        }
    }

    private fun openEditAlarmActivity(alarmDetails: String, position: Int) {
        val intent = Intent(this, Create_Alarm::class.java).apply {
            putExtra("alarmDetails", alarmDetails) // Передаем данные будильника
            putExtra("position", position) // Передаем позицию будильника в списке
        }
        startActivity(intent) // Запускаем Create_Alarm для редактирования
    }

    override fun onResume() {
        super.onResume()
        loadAlarms() // Обновляем список будильников
    }

    private fun loadAlarms() {
        alarmList.clear()
        val alarmCount = sharedPreferences.getInt("alarmCount", 0)
        for (i in 0 until alarmCount) {
            val alarm = sharedPreferences.getString("alarm_$i", null)
            alarm?.let { alarmList.add(it) }
        }
        adapter.notifyDataSetChanged()
    }

    private fun addAlarm(alarmDetails: String) {
        if (!alarmList.contains(alarmDetails)) {
            alarmList.add(alarmDetails)
            adapter.notifyDataSetChanged()
            saveAlarmToPreferences(alarmDetails)
        }
    }

    private fun saveAlarmToPreferences(alarmDetails: String) {
        val alarmCount = sharedPreferences.getInt("alarmCount", 0)
        val editor = sharedPreferences.edit()
        editor.putString("alarm_$alarmCount", alarmDetails)
        editor.putInt("alarmCount", alarmCount + 1)
        editor.apply()
    }

    private fun showDeleteAlarmDialog() {
        if (alarmList.isEmpty()) {
            Toast.makeText(this, "Нет будильников для удаления", Toast.LENGTH_SHORT).show()
            return
        }

        val alarmToDelete = alarmList.toTypedArray()
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Выберите будильник для удаления")
        builder.setItems(alarmToDelete) { dialog, which ->
            deleteAlarm(which)
        }
        builder.setNegativeButton("ОТМЕНА", null)
        builder.show()
    }

    private fun deleteAlarm(position: Int) {
        val alarmToRemove = alarmList[position]
        alarmList.removeAt(position)
        adapter.notifyDataSetChanged()
        updateSharedPreferencesAfterDeletion(alarmToRemove)
        Toast.makeText(this, "Будильник \"$alarmToRemove\" удалён", Toast.LENGTH_SHORT).show()
    }

    private fun updateSharedPreferencesAfterDeletion(alarmToRemove: String) {
        val alarmCount = sharedPreferences.getInt("alarmCount", 0)
        val editor = sharedPreferences.edit()

        for (i in 0 until alarmCount) {
            val alarm = sharedPreferences.getString("alarm_$i", null)
            if (alarm == alarmToRemove) {
                editor.remove("alarm_$i")
            } else if (alarm != null) {
                editor.putString("alarm_${i - 1}", alarm)
            }
        }
        editor.putInt("alarmCount", alarmCount - 1)
        editor.apply()
    }
}




