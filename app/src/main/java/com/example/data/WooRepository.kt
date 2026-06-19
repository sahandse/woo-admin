package com.example.data

import android.content.Context
import android.util.Log
import com.example.core.utils.Helpers
import com.example.core.utils.JalaliCalendar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

class WooRepository(private val db: AppDatabase, private val context: Context) {

    private val sharedPrefs = context.getSharedPreferences("woo_preferences", Context.MODE_PRIVATE)

    // Demo Mode toggle: True by default so that they can see a fully populated professional app first.
    var isDemoMode: Boolean
        get() = sharedPrefs.getBoolean("is_demo_mode", true)
        set(value) = sharedPrefs.edit().putBoolean("is_demo_mode", value).apply()

    var jwtToken: String
        get() = sharedPrefs.getString("jwt_token", "demo_jwt_token") ?: "demo_jwt_token"
        set(value) = sharedPrefs.edit().putString("jwt_token", value).apply()

    var activeAdminUsername: String
        get() = sharedPrefs.getString("active_admin", "admin") ?: "admin"
        set(value) = sharedPrefs.edit().putString("active_admin", value).apply()

    var backendUrl: String
        get() = sharedPrefs.getString("backend_url", "https://api.example.com") ?: "https://api.example.com"
        set(value) = sharedPrefs.edit().putString("backend_url", value).apply()

    // Seeds the database with rich Persian details if they don't already exist
    suspend fun seedDatabase() = withContext(Dispatchers.IO) {
        try {
            // Seed stores
            val stores = db.storeDao().getAllStores().firstOrNull()
            if (stores.isNullOrEmpty()) {
                for (store in Helpers.mockStores) {
                    db.storeDao().insertStore(store)
                }
            }

            // Seed products
            val products = db.productDao().getAllProducts().firstOrNull()
            if (products.isNullOrEmpty()) {
                db.productDao().insertProducts(Helpers.mockProducts)
            }

            // Seed orders
            val orders = db.orderDao().getAllOrders().firstOrNull()
            if (orders.isNullOrEmpty()) {
                db.orderDao().insertOrders(Helpers.mockOrders)
            }

            // Seed customers
            val customers = db.customerDao().getAllCustomers().firstOrNull()
            if (customers.isNullOrEmpty()) {
                db.customerDao().insertCustomers(Helpers.mockCustomers)
            }

            // Seed coupons
            val coupons = db.couponDao().getAllCoupons().firstOrNull()
            if (coupons.isNullOrEmpty()) {
                db.couponDao().insertCoupons(Helpers.mockCoupons)
            }

            // Seed notifications
            val notifications = db.notificationDao().getAllNotifications().firstOrNull()
            if (notifications.isNullOrEmpty()) {
                for (notif in Helpers.mockNotifications) {
                    db.notificationDao().insertNotification(notif)
                }
            }

            // Seed admin users
            val admins = db.adminUserDao().getAllAdminUsers().firstOrNull()
            if (admins.isNullOrEmpty()) {
                for (adm in Helpers.mockAdminUsers) {
                    db.adminUserDao().insertAdminUser(adm)
                }
            }

            // Seed activity logs
            val activities = db.adminActivityDao().getAllActivities().firstOrNull()
            if (activities.isNullOrEmpty()) {
                for (act in Helpers.mockActivities) {
                    db.adminActivityDao().insertActivity(act)
                }
            }
        } catch (e: Exception) {
            Log.e("WooRepository", "Failed to seed DB", e)
        }
    }

    // --- REPLAY CHANNELS AND STREAMS ---
    fun getAllOrders(): Flow<List<WooOrder>> = db.orderDao().getAllOrders()
    fun searchOrders(query: String): Flow<List<WooOrder>> = db.orderDao().searchOrders(query)
    fun getOrdersByStatus(status: String): Flow<List<WooOrder>> = db.orderDao().getOrdersByStatus(status)

    fun getAllProducts(): Flow<List<WooProduct>> = db.productDao().getAllProducts()
    fun searchProducts(query: String): Flow<List<WooProduct>> = db.productDao().searchProducts(query)

    fun getAllCustomers(): Flow<List<WooCustomer>> = db.customerDao().getAllCustomers()
    fun searchCustomers(query: String): Flow<List<WooCustomer>> = db.customerDao().searchCustomers(query)

    fun getAllCoupons(): Flow<List<WooCoupon>> = db.couponDao().getAllCoupons()

    fun getAllNotifications(): Flow<List<WooNotification>> = db.notificationDao().getAllNotifications()

    fun getAllAdminUsers(): Flow<List<AdminUser>> = db.adminUserDao().getAllAdminUsers()

    fun getAllActivities(): Flow<List<AdminActivity>> = db.adminActivityDao().getAllActivities()

    fun getAllStores(): Flow<List<WooStore>> = db.storeDao().getAllStores()

    // --- WRITE ACTIONS & TRANSACTIONS ---
    suspend fun changeOrderStatus(orderId: Long, status: OrderStatus) = withContext(Dispatchers.IO) {
        db.orderDao().updateOrderStatus(orderId, status.name)
        logActivity("CHANGE_STATUS", "وضعیت سفارش به شناسه $orderId را به «${status.persianLabel}» تغییر داد.")
    }

    suspend fun changeOrderShippingStatus(orderId: Long, shippingStatus: String) = withContext(Dispatchers.IO) {
        db.orderDao().updateOrderShippingStatus(orderId, shippingStatus)
        val farsiStatus = when (shippingStatus) {
            "READY_TO_PACK" -> "آماده بسته‌بندی"
            "PACKED" -> "بسته‌بندی شده"
            "READY_TO_SHIP" -> "آماده ارسال"
            "HANDED_TO_POST" -> "تحویل به پست"
            "HANDED_TO_COURIER" -> "تحویل به پیک"
            "SHIPPED" -> "ارسال شده"
            "DELIVERED" -> "تحویل شده"
            "RETURNED" -> "مرجوع شده"
            else -> shippingStatus
        }
        logActivity("CHANGE_SHIPPING", "وضعیت ارسال سفارش $orderId را به «$farsiStatus» تغییر داد.")
    }

    suspend fun changeOrderPaymentStatus(orderId: Long, isPaid: Boolean) = withContext(Dispatchers.IO) {
        db.orderDao().updateOrderPaymentStatus(orderId, isPaid)
        val farsiPaid = if (isPaid) "پرداخت شده" else "پرداخت نشده"
        logActivity("CHANGE_PAYMENT", "وضعیت پرداخت سفارش $orderId را به «$farsiPaid» تغییر داد.")
    }

    suspend fun createSimulatedOrder(order: WooOrder) = withContext(Dispatchers.IO) {
        db.orderDao().insertOrder(order)
        val today = JalaliCalendar.getTodayJalali().toString()
        val notif = WooNotification(
            title = "سفارش جدید #${order.orderNumber}",
            body = "سفارشی به مبلغ ${Helpers.formatPrice(order.totalAmount)} از طرف ${order.customerName} ثبتی شد.",
            type = "NEW_ORDER",
            dateJalali = today,
            isRead = false,
            linkedId = order.id
        )
        db.notificationDao().insertNotification(notif)
        logActivity("NEW_ORDER", "سفارش جدید دریافتی با موفقیت در سیستم ثبت شد.")
    }

    suspend fun saveOrderTracking(orderId: Long, trackingCode: String, company: String) = withContext(Dispatchers.IO) {
        db.orderDao().updateOrderTracking(orderId, trackingCode, company)
        logActivity("CHANGE_STATUS", "کد رهگیری $trackingCode از شرکت $company را برای سفارش $orderId ثبت کرد.")
    }

    suspend fun saveOrderAdminNotes(orderId: Long, notes: String) = withContext(Dispatchers.IO) {
        db.orderDao().updateOrderAdminNotes(orderId, notes)
    }

    suspend fun updateProductStock(productId: Long, quantity: Int, inStock: Boolean) = withContext(Dispatchers.IO) {
        db.productDao().updateStock(productId, quantity, inStock)
        logActivity("EDIT_STOCK", "موجودی محصول به شناسه $productId را به $quantity عدد تغییر داد.")
    }

    suspend fun updateProductPrice(productId: Long, regularPrice: Long, salePrice: Long) = withContext(Dispatchers.IO) {
        db.productDao().updatePrice(productId, regularPrice, salePrice)
        logActivity("EDIT_PRICE", "قیمت محصول $productId را به ${Helpers.formatPrice(regularPrice)} تغییر داد.")
    }

    suspend fun updateProductStatus(productId: Long, status: String) = withContext(Dispatchers.IO) {
        db.productDao().updateProductStatus(productId, status)
    }

    suspend fun createProduct(product: WooProduct) = withContext(Dispatchers.IO) {
        db.productDao().insertProduct(product)
        logActivity("CREATE_PRODUCT", "محصول جدیدی با نام «${product.name}» ایجاد کرد.")
    }

    suspend fun updateProduct(product: WooProduct) = withContext(Dispatchers.IO) {
        db.productDao().insertProduct(product)
        logActivity("CREATE_PRODUCT", "محصول «${product.name}» را ویرایش کرد.")
    }

    suspend fun deleteProduct(productId: Long, name: String) = withContext(Dispatchers.IO) {
        db.productDao().deleteProduct(productId)
        logActivity("REMOVE_PRODUCT", "محصول به شناسه $productId با نام «$name» را حذف کرد.")
    }

    suspend fun insertCustomerInternalNotes(customerId: Long, notes: String) = withContext(Dispatchers.IO) {
        db.customerDao().updateCustomerNotes(customerId, notes)
    }

    suspend fun addCoupon(coupon: WooCoupon) = withContext(Dispatchers.IO) {
        db.couponDao().insertCoupon(coupon)
        logActivity("CREATE_COUPON", "کوپن جدید تخفیف با رمز «${coupon.code}» ایجاد کرد.")
    }

    suspend fun deleteCoupon(couponId: Long, code: String) = withContext(Dispatchers.IO) {
        db.couponDao().deleteCoupon(couponId)
        logActivity("REMOVE_COUPON", "کوپن تخفیف با رمز «$code» را حذف کرد.")
    }

    suspend fun updateCouponActive(couponId: Long, active: Boolean) = withContext(Dispatchers.IO) {
        db.couponDao().updateCouponActive(couponId, active)
    }

    suspend fun markNotificationAsRead(id: Long) = withContext(Dispatchers.IO) {
        db.notificationDao().markAsRead(id)
    }

    suspend fun deleteNotification(id: Long) = withContext(Dispatchers.IO) {
        db.notificationDao().deleteNotification(id)
    }

    suspend fun addAdminUser(user: AdminUser) = withContext(Dispatchers.IO) {
        db.adminUserDao().insertAdminUser(user)
    }

    suspend fun deleteAdminUser(id: Long) = withContext(Dispatchers.IO) {
        db.adminUserDao().deleteAdminUser(id)
    }

    suspend fun switchActiveStore(storeId: Long) = withContext(Dispatchers.IO) {
        db.storeDao().deactivateAllStores()
        db.storeDao().activateStore(storeId)
    }

    suspend fun addNewStore(store: WooStore) = withContext(Dispatchers.IO) {
        db.storeDao().insertStore(store)
    }

    suspend fun deleteStore(storeId: Long) = withContext(Dispatchers.IO) {
        db.storeDao().deleteStoreById(storeId)
    }

    suspend fun getActiveAdmin(): AdminUser? = withContext(Dispatchers.IO) {
        db.adminUserDao().getAdminUserByUsername(activeAdminUsername)
    }

    private suspend fun logActivity(actionType: String, details: String) {
        val today = JalaliCalendar.getTodayJalali().toString()
        val time = JalaliCalendar.getCurrentTime()
        val admin = getActiveAdmin()
        val adminName = admin?.fullName ?: "مدیر دمو"
        val adminRole = admin?.role ?: "SUPER_ADMIN"
        db.adminActivityDao().insertActivity(
            AdminActivity(
                adminName = adminName,
                adminRole = adminRole,
                actionType = actionType,
                details = details,
                timestampJalali = "$today - $time"
            )
        )
    }
}
