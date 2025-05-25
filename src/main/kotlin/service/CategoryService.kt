package service

import model.Category

/**
 * @author guvencenanguvenal
 */
class CategoryService {
    companion object {

        private val FIRST = 0;
        private val categories: MutableSet<Category> = mutableSetOf(
            Category(1, "Country Flags"),
            Category(2, "Country Capitals"),
            Category(3, "Hollywood Stars"),
            Category(4, "Movie Posters"),
            Category(5, "Football Club Logos")
        )

        fun getAllCategories(): MutableSet<Category> {
            return categories
        }

        fun getCategoryById(id: Int): Category {
            return categories.filter { c -> c.id == id }[FIRST]
        }
    }
}