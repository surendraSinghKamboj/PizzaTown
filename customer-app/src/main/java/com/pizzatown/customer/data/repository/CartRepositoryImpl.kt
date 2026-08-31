package com.pizzatown.customer.data.repository

import com.pizzatown.customer.data.local.CartDao
import com.pizzatown.customer.data.local.toDomain
import com.pizzatown.customer.data.local.toEntity
import com.pizzatown.customer.domain.model.CartItem
import com.pizzatown.customer.domain.repository.CartRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Local-only cart persistence via Room. Works with no network at all, and
 * survives app restarts / process death (requirement: cart must not be
 * lost unexpectedly, must work even with temporary network loss).
 */
class CartRepositoryImpl @Inject constructor(
    private val cartDao: CartDao
) : CartRepository {

    override fun observeCart(): Flow<List<CartItem>> =
        cartDao.observeCartItems().map { list -> list.map { it.toDomain() } }

    override suspend fun addOrIncrement(item: CartItem) {
        val configKey = CartItem.configKey(
            menuItemId = item.menuItemId,
            variantId = item.selectedVariantId,
            selectedOptionIds = item.selectedOptions.map { it.optionId }
        )
        val existing = cartDao.getByCartItemId(configKey)
        if (existing != null) {
            cartDao.update(existing.copy(quantity = existing.quantity + item.quantity))
        } else {
            cartDao.upsert(item.copy(cartItemId = configKey).toEntity())
        }
    }

    override suspend fun updateQuantity(cartItemId: String, quantity: Int) {
        if (quantity <= 0) {
            cartDao.deleteByCartItemId(cartItemId)
            return
        }
        val existing = cartDao.getByCartItemId(cartItemId) ?: return
        cartDao.update(existing.copy(quantity = quantity))
    }

    override suspend fun removeItem(cartItemId: String) {
        cartDao.deleteByCartItemId(cartItemId)
    }

    override suspend fun clearCart() {
        cartDao.clearCart()
    }
}
