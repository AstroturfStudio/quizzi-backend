package dto

import kotlinx.serialization.Serializable

/**
 * @author guvencenanguvenal
 */
@Serializable
data class CategoryDTO(val id: Int, val  name: String) {
}