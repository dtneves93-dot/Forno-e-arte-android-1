package com.fornoearte.app

import android.app.Application
import com.fornoearte.app.data.AppDatabase
import com.fornoearte.app.data.OrderRepository

class FornoApplication : Application() {
    val database by lazy { AppDatabase.create(this) }
    val repository by lazy { OrderRepository(database.orderDao()) }
}
