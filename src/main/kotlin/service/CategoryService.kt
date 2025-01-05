package service

import model.Category

/**
 * @author guvencenanguvenal
 */
class CategoryService {
    companion object {

        private val categories: MutableSet<Category> = mutableSetOf(Category(1, "Bayrak Bilmece"))

        fun getAllCategories(): MutableSet<Category> {
            return categories
        }
    }
}