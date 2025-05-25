package data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import model.Question
import java.io.BufferedReader
import java.io.InputStreamReader

object QuestionDatabase {
    private val questions: List<Question> = loadQuestions()

    private fun loadQuestions(): List<Question> {
        val questions = mutableListOf<Question>()
        // Names of all your question files in src/main/resources/questions
        val questionFiles = listOf(
            "countryCapitalsQuestions.json",
            "flagQuestions.json",
            "footballClubsLogoQuestions.json",
            "hollywoodStarsQuestions.json",
            "moviePostersQuestions.json"
        )

        questionFiles.forEach { fileName ->
            try {
                val inputStream = javaClass.getResourceAsStream("/questions/$fileName")
                if (inputStream == null) {
                    println("Failed to find resource: /questions/$fileName")
                    return@forEach // continue to next file
                }
                val json = BufferedReader(InputStreamReader(inputStream)).use { it.readText() }
                val response = Json.decodeFromString<QuestionResponse>(json)
                questions.addAll(response.questions)
            } catch (e: Exception) {
                println("Failed to read or parse $fileName: ${e.message}")
                e.printStackTrace()
            }
        }
        return questions
    }

    fun getRandomQuestion(categoryId: Int): Question {
        val question = questions.filter { q -> q.categoryId == categoryId }.shuffled().first()

        return Question(
            id = question.id,
            categoryId = question.categoryId,
            imageCode = question.imageCode,
            content = question.content,
            options = question.options.shuffled(),
            answer = question.answer
        )
    }

    @Serializable
    private data class QuestionResponse(
        val questions: List<Question>
    )
}