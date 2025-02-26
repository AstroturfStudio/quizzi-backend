package model

import dto.CategoryDTO
import kotlinx.serialization.Serializable

/**
 * @author guvencenanguvenal
 */
@Serializable
data class Category(val id: Int, val name: String) {
    fun toDto(): CategoryDTO {
        return CategoryDTO(id, name)
    }
}
