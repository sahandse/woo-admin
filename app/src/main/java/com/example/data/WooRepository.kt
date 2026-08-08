package com.example.data

import android.content.Context
import android.util.Log
import com.example.core.utils.Helpers
import com.example.core.utils.JalaliCalendar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import okhttp3.Credentials

class WooRepository(private val db: AppDatabase, private val context: Context) {

    private val sharedPrefs = context.getSharedPreferences("woo_preferences", Context.MODE_PRIVATE)

    var isDarkThemeEnabled: Boolean
        get() = sharedPrefs.getBoolean("is_dark_theme_enabled", true)
        set(value) = sharedPrefs.edit().putBoolean("is_dark_theme_enabled", value).apply()

    var jwtToken: String
        get() = sharedPrefs.getString("jwt_token", "") ?: ""
        set(value) = sharedPrefs.edit().putString("jwt_token", value).apply()

    var activeAdminUsername: String
        get() = sharedPrefs.getString("active_admin", "admin") ?: "admin"
        set(value) = sharedPrefs.edit().putString("active_admin", value).apply()

    var backendUrl: String
        get() = sharedPrefs.getString("backend_url", "https://api.example.com") ?: "https://api.example.com"
        set(value) = sharedPrefs.edit().putString("backend_url", value).apply()

    // --- MELIPAYAMAK SMS INTEGRATION ---
    var melipayamakUsername: String
        get() = sharedPrefs.getString("melipayamak_username", "") ?: ""
        set(value) = sharedPrefs.edit().putString("melipayamak_username", value).apply()

    var melipayamakPassword: String
        get() = sharedPrefs.getString("melipayamak_password", "") ?: ""
        set(value) = sharedPrefs.edit().putString("melipayamak_password", value).apply()

    var melipayamakSender: String
        get() = sharedPrefs.getString("melipayamak_sender", "500040001015") ?: "500040001015"
        set(value) = sharedPrefs.edit().putString("melipayamak_sender", value).apply()

    var smsNewOrderEnabled: Boolean
        get() = sharedPrefs.getBoolean("sms_new_order_enabled", true)
        set(value) = sharedPrefs.edit().putBoolean("sms_new_order_enabled", value).apply()

    var smsStatusChangeEnabled: Boolean
        get() = sharedPrefs.getBoolean("sms_status_change_enabled", true)
        set(value) = sharedPrefs.edit().putBoolean("sms_status_change_enabled", value).apply()

    var smsTemplateNewOrder: String
        get() = sharedPrefs.getString("sms_template_new_order", "مشتری عزیز {name}، سفارش شما به شماره {order_id} با موفقیت ثبت شد.") ?: "مشتری عزیز {name}، سفارش شما به شماره {order_id} با موفقیت ثبت شد."
        set(value) = sharedPrefs.edit().putString("sms_template_new_order", value).apply()

    var smsTemplateStatusProcessing: String
        get() = sharedPrefs.getString("sms_template_status_processing", "مشتری عزیز {name}، وضعیت سفارش {order_id} به \"در حال آماده‌سازی\" تغییر یافت.") ?: "مشتری عزیز {name}، وضعیت سفارش {order_id} به \"در حال آماده‌سازی\" تغییر یافت."
        set(value) = sharedPrefs.edit().putString("sms_template_status_processing", value).apply()

    var smsTemplateStatusCompleted: String
        get() = sharedPrefs.getString("sms_template_status_completed", "مشتری عزیز {name}، سفارش {order_id} تکمیل و ارسال شد. با تشکر!") ?: "مشتری عزیز {name}، سفارش {order_id} تکمیل و ارسال شد. با تشکر!"
        set(value) = sharedPrefs.edit().putString("sms_template_status_completed", value).apply()

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

    // --- REMOTE SYNC ---
    val authenticator = WooCommerceAuthenticator(context)

    suspend fun syncAllData() = withContext(Dispatchers.IO) {
        if (!authenticator.isLoggedIn()) return@withContext

        try {
            val credential = Credentials.basic(authenticator.consumerKey, authenticator.consumerSecret)
            val baseUrl = authenticator.storeUrl.trimEnd('/')
            val api = RetrofitInstance.getOrCreateWooCommerceApi(baseUrl)

            // Sync orders
            val ordersResponse = api.getOrders(credential)
            val localOrders = ordersResponse.map { it.toLocal() }
            db.orderDao().clearAllOrders()
            db.orderDao().insertOrders(localOrders)

            // Sync products
            val productsResponse = api.getProducts(credential)
            val localProducts = productsResponse.map { it.toLocal() }
            db.productDao().clearAllProducts()
            db.productDao().insertProducts(localProducts)

            // Sync customers
            val customersResponse = api.getCustomers(credential)
            val localCustomers = customersResponse.map { it.toLocal() }
            db.customerDao().clearAllCustomers()
            db.customerDao().insertCustomers(localCustomers)

            // Sync coupons
            val couponsResponse = api.getCoupons(credential)
            val localCoupons = couponsResponse.map { it.toLocal() }
            db.couponDao().clearAllCoupons()
            db.couponDao().insertCoupons(localCoupons)

            logActivity("SYNC", "همگام‌سازی کامل با سرور ووکامرس انجام شد.")
        } catch (e: Exception) {
            Log.e("WooRepository", "Sync failed", e)
            logActivity("SYNC_ERROR", "خطا در همگام‌سازی: ${e.localizedMessage}")
        }
    }

    suspend fun syncOrders() = withContext(Dispatchers.IO) {
        if (!authenticator.isLoggedIn()) return@withContext
        try {
            val credential = Credentials.basic(authenticator.consumerKey, authenticator.consumerSecret)
            val baseUrl = authenticator.storeUrl.trimEnd('/')
            val api = RetrofitInstance.getOrCreateWooCommerceApi(baseUrl)
            val ordersResponse = api.getOrders(credential)
            val localOrders = ordersResponse.map { it.toLocal() }
            db.orderDao().clearAllOrders()
            db.orderDao().insertOrders(localOrders)
        } catch (e: Exception) {
            Log.e("WooRepository", "Orders sync failed", e)
        }
    }

    suspend fun syncProducts() = withContext(Dispatchers.IO) {
        if (!authenticator.isLoggedIn()) return@withContext
        try {
            val credential = Credentials.basic(authenticator.consumerKey, authenticator.consumerSecret)
            val baseUrl = authenticator.storeUrl.trimEnd('/')
            val api = RetrofitInstance.getOrCreateWooCommerceApi(baseUrl)
            val productsResponse = api.getProducts(credential)
            val localProducts = productsResponse.map { it.toLocal() }
            db.productDao().clearAllProducts()
            db.productDao().insertProducts(localProducts)
        } catch (e: Exception) {
            Log.e("WooRepository", "Products sync failed", e)
        }
    }

    // --- WRITE ACTIONS & TRANSACTIONS ---
    suspend fun changeOrderStatus(orderId: Long, status: OrderStatus) = withContext(Dispatchers.IO) {
        db.orderDao().updateOrderStatus(orderId, status.name)
        logActivity("CHANGE_STATUS", "وضعیت سفارش به شناسه $orderId را به «${status.persianLabel}» تغییر داد.")

        // Send automatic SMS status notification if enabled
        if (smsStatusChangeEnabled) {
            val order = db.orderDao().getOrderById(orderId)
            if (order != null && order.customerPhone.isNotBlank()) {
                var textTemplate = ""
                if (status == OrderStatus.PROCESSING) {
                    textTemplate = smsTemplateStatusProcessing
                } else if (status == OrderStatus.COMPLETED) {
                    textTemplate = smsTemplateStatusCompleted
                }

                if (textTemplate.isNotBlank()) {
                    val formattedMsg = textTemplate
                        .replace("{name}", order.customerName)
                        .replace("{order_id}", order.orderNumber)
                        .replace("{amount}", Helpers.formatPrice(order.totalAmount))
                    
                    sendMeliPayamakSms(order.customerPhone, formattedMsg)
                }
            }
        }
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

    suspend fun sendMeliPayamakSms(recipientPhone: String, messageText: String): com.example.core.network.SmsResult = withContext(Dispatchers.IO) {
        val username = melipayamakUsername
        val password = melipayamakPassword
        val sender = melipayamakSender

        if (username.isBlank() || password.isBlank()) {
            return@withContext com.example.core.network.SmsResult.Error("مشخصات ملی‌پیامک تنظیم نشده است.")
        }

        val result = com.example.core.network.MeliPayamakService.sendSms(username, password, recipientPhone, sender, messageText)
        when (result) {
            is com.example.core.network.SmsResult.Success -> {
                logActivity("SEND_SMS", "ملی‌پیامک - ارسال پیامک موفق به $recipientPhone. (متن: $messageText)")
            }
            is com.example.core.network.SmsResult.Error -> {
                logActivity("SEND_SMS", "ملی‌پیامک - ارسال پیامک ناموفق به $recipientPhone. علت: ${result.errorMessage}")
            }
        }
        result
    }
}
