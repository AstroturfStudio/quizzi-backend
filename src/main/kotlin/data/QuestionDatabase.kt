package data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import model.Question
import java.nio.file.Files
import java.nio.file.Paths

object QuestionDatabase {
    private val questions: List<Question> = loadQuestions()

    private fun loadQuestions(): List<Question> {
        val questions = mutableListOf<Question>()

        val resourceDirUri = javaClass.getResource("/questions")?.toURI() ?: return emptyList()

        val resourceDir = Paths.get(resourceDirUri)

        Files.newDirectoryStream(resourceDir, "*.json").use { stream ->
            for (path in stream) {
                try {
                    val json = Files.newBufferedReader(path).use { it.readText() }
                    val response = Json.decodeFromString<QuestionResponse>(json)
                    questions.addAll(response.questions)
                } catch (e: Exception) {
                    println("Failed to read ${path.fileName}: ${e.message}")
                }
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