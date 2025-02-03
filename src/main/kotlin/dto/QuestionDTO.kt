package dto

import kotlinx.serialization.Serializable
import model.Option

/**
 * @author guvencenanguvenal
 */
@Serializable
data class QuestionDTO(
    val imageCode: String?,
    val content: String,
    val options: List<Option>
)
