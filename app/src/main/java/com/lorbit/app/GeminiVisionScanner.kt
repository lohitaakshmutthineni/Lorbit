package com.lorbit.app

import android.graphics.Bitmap
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

object GeminiVisionScanner {

    /**
     * AI Parser for Timetable Screenshot -> Structured Class Schedule
     */
    suspend fun scanTimetableImage(bitmap: Bitmap, apiKey: String): List<TimetableSlotEntity> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext emptyList()
        try {
            val generativeModel = GenerativeModel(
                modelName = "gemini-1.5-flash",
                apiKey = apiKey
            )

            val prompt = """
                Analyze this timetable screenshot. Extract all class schedules into a clean JSON array with this schema:
                [
                  {
                    "subjectName": "Data Structures",
                    "dayOfWeek": 1,
                    "startTime": "09:00 AM",
                    "endTime": "10:30 AM",
                    "room": "Room 304"
                  }
                ]
                Day of week must be an integer (1 for Monday, 2 for Tuesday, ..., 7 for Sunday).
                Return ONLY valid raw JSON array. Do not include markdown codeblocks, backticks, or extra text.
            """.trimIndent()

            val inputContent = content {
                image(bitmap)
                text(prompt)
            }

            val response = generativeModel.generateContent(inputContent)
            val jsonText = response.text?.trim()?.removePrefix("```json")?.removePrefix("```")?.removeSuffix("```")?.trim() ?: "[]"

            val jsonArray = JSONArray(jsonText)
            val list = mutableListOf<TimetableSlotEntity>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    TimetableSlotEntity(
                        subjectName = obj.optString("subjectName", "Class"),
                        dayOfWeek = obj.optInt("dayOfWeek", 1),
                        startTime = obj.optString("startTime", "09:00 AM"),
                        endTime = obj.optString("endTime", "10:00 AM"),
                        room = obj.optString("room", "Room 101")
                    )
                )
            }
            list
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * AI Parser for Attendance Screenshot -> Subjects with Attended & Total classes
     */
    suspend fun scanAttendanceImage(bitmap: Bitmap, apiKey: String): List<SubjectEntity> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext emptyList()
        try {
            val generativeModel = GenerativeModel(
                modelName = "gemini-1.5-flash",
                apiKey = apiKey
            )

            val prompt = """
                Analyze this college portal attendance screenshot. Extract all subjects, attended classes, total classes, and subject code into a JSON array:
                [
                  {
                    "name": "Operating Systems",
                    "code": "CS202",
                    "attendedClasses": 14,
                    "totalClasses": 18,
                    "targetAttendance": 75.0
                  }
                ]
                Return ONLY valid raw JSON array. Do not include markdown codeblocks, backticks, or extra text.
            """.trimIndent()

            val inputContent = content {
                image(bitmap)
                text(prompt)
            }

            val response = generativeModel.generateContent(inputContent)
            val jsonText = response.text?.trim()?.removePrefix("```json")?.removePrefix("```")?.removeSuffix("```")?.trim() ?: "[]"

            val jsonArray = JSONArray(jsonText)
            val list = mutableListOf<SubjectEntity>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    SubjectEntity(
                        name = obj.optString("name", "Subject"),
                        code = obj.optString("code", "SUB101"),
                        attendedClasses = obj.optInt("attendedClasses", 0),
                        totalClasses = obj.optInt("totalClasses", 0),
                        targetAttendance = obj.optDouble("targetAttendance", 75.0).toFloat()
                    )
                )
            }
            list
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}