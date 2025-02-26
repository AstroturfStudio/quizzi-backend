package service

import model.Category

/**
 * @author guvencenanguvenal
 */
class CategoryService {
    companion object {

        private val FIRST = 0;
        private val categories: MutableSet<Category> = mutableSetOf(Category(1, "Flag Quiz"))

        fun getAllCategories(): MutableSet<Category> {
            return categories
        }

        fun getCategoryById(id : Int): Category {
            return categories.filter { c -> c.id == id }[FIRST]
        }
    }
}