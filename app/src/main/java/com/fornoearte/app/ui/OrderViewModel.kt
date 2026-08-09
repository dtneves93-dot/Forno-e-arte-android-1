package com.fornoearte.app.ui

import androidx.lifecycle.*
import com.fornoearte.app.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

data class UiState(val orders: List<OrderEntity> = emptyList(), val query: String = "", val loading: Boolean = true, val error: String? = null) {
    val todayOrders get() = orders.filter { val start = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY,0); set(Calendar.MINUTE,0); set(Calendar.SECOND,0); set(Calendar.MILLISECOND,0) }.timeInMillis; it.createdAt >= start }
}

class OrderViewModel(private val repository: OrderRepository) : ViewModel() {
    private val query = MutableStateFlow(""); private val error = MutableStateFlow<String?>(null)
    val state = combine(query.flatMapLatest(repository::orders), query, error) { orders, q, e -> UiState(orders, q, false, e) }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState())
    fun search(value: String) { query.value = value }
    fun clearError() { error.value = null }
    fun save(order: OrderEntity, done: () -> Unit) = viewModelScope.launch { runCatching { repository.save(order) }.onSuccess { done() }.onFailure { error.value = "Não foi possível salvar: ${it.localizedMessage}" } }
    fun setStatus(order: OrderEntity, status: OrderStatus, done: () -> Unit = {}) = save(order.copy(status = status), done)
    fun delete(order: OrderEntity) = viewModelScope.launch { runCatching { repository.delete(order) }.onFailure { error.value = "Não foi possível excluir o pedido" } }
    class Factory(private val repo: OrderRepository) : ViewModelProvider.Factory { override fun <T : ViewModel> create(modelClass: Class<T>): T = OrderViewModel(repo) as T }
}
