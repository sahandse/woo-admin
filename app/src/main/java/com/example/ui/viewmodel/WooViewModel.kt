package com.example.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.network.GeminiManager
import com.example.core.utils.Helpers
import com.example.core.utils.JalaliCalendar
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val ORDER_POLL_INTERVAL_MS = 5 * 60 * 1000L // 5 minutes

class WooViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    val repository = WooRepository(db, application)

    // --- MAIN STATES ---
    val orders: StateFlow<List<WooOrder>> = repository.getAllOrders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val products: StateFlow<List<WooProduct>> = repository.getAllProducts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customers: StateFlow<List<WooCustomer>> = repository.getAllCustomers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val coupons: StateFlow<List<WooCoupon>> = repository.getAllCoupons()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notifications: StateFlow<List<WooNotification>> = repository.getAllNotifications()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val adminUsers: StateFlow<List<AdminUser>> = repository.getAllAdminUsers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activities: StateFlow<List<AdminActivity>> = repository.getAllActivities()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val stores: StateFlow<List<WooStore>> = repository.getAllStores()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- SYNC STATES ---
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

    private val _lastSyncTime = MutableStateFlow<String?>(null)
    val lastSyncTime: StateFlow<String?> = _lastSyncTime.asStateFlow()

    // --- SEARCH / FILTER STATES ---
    private val _orderSearchQuery = MutableStateFlow("")
    val orderSearchQuery: StateFlow<String> = _orderSearchQuery.asStateFlow()

    private val _orderStatusFilter = MutableStateFlow<String?>("ALL")
    val orderStatusFilter: StateFlow<String?> = _orderStatusFilter.asStateFlow()

    private val _productSearchQuery = MutableStateFlow("")
    val productSearchQuery: StateFlow<String> = _productSearchQuery.asStateFlow()

    private val _productFilter = MutableStateFlow("ALL")
    val productFilter: StateFlow<String> = _productFilter.asStateFlow()

    private val _productCategoryFilter = MutableStateFlow("ALL")
    val productCategoryFilter: StateFlow<String> = _productCategoryFilter.asStateFlow()

    private val _customerSearchQuery = MutableStateFlow("")
    val customerSearchQuery: StateFlow<String> = _customerSearchQuery.asStateFlow()

    private val _customerCategoryFilter = MutableStateFlow("ALL")
    val customerCategoryFilter: StateFlow<String> = _customerCategoryFilter.asStateFlow()

    // --- AUTH & CONFIG STATES ---
    private val _loggedInUser = MutableStateFlow<AdminUser?>(null)
    val loggedInUser: StateFlow<AdminUser?> = _loggedInUser.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    // --- GEMINI AI STATES ---
    private val _aiResult = MutableStateFlow<String?>(null)
    val aiResult: StateFlow<String?> = _aiResult.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    private var pollingJob: Job? = null

    init {
        viewModelScope.launch(Dispatchers.IO) {
            repository.seedDatabase()

            val username = repository.activeAdminUsername
            val user = db.adminUserDao().getAdminUserByUsername(username)
            if (user != null) {
                _loggedInUser.value = user
                _isLoggedIn.value = true
            }

            // Auto-sync on startup if a store is already connected
            val activeStore = db.storeDao().getActiveStore()
            if (activeStore != null) {
                runFullSync(activeStore)
                startOrderPolling(activeStore)
            }
        }
    }

    // --- SYNC ENGINE ---

    private suspend fun runFullSync(store: WooStore) {
        _isSyncing.value = true
        try {
            WooCommerceSync.syncAll(
                db = db,
                baseUrl = store.url,
                consumerKey = store.consumerKey,
                consumerSecret = store.consumerSecret,
                onProgress = { msg -> _syncMessage.value = msg }
            )
            _lastSyncTime.value = JalaliCalendar.getCurrentTime()
            _syncMessage.value = "همگام‌سازی کامل انجام شد"
        } catch (e: Exception) {
            _syncMessage.value = "خطا در همگام‌سازی: ${e.localizedMessage}"
            Log.e("WooSync", "Full sync failed", e)
        } finally {
            _isSyncing.value = false
        }
    }

    private fun startOrderPolling(store: WooStore) {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(ORDER_POLL_INTERVAL_MS)
                try {
                    val newOrders = WooCommerceSync.syncOrdersOnly(
                        db = db,
                        baseUrl = store.url,
                        consumerKey = store.consumerKey,
                        consumerSecret = store.consumerSecret
                    )
                    if (newOrders.isNotEmpty()) {
                        val today = JalaliCalendar.getTodayJalali().toString()
                        for (order in newOrders) {
                            db.notificationDao().insertNotification(
                                WooNotification(
                                    title = "سفارش جدید #${order.orderNumber}",
                                    body = "${order.customerName} - ${Helpers.formatPrice(order.totalAmount)}",
                                    type = "NEW_ORDER",
                                    dateJalali = today,
                                    isRead = false,
                                    linkedId = order.id
                                )
                            )
                            if (repository.smsNewOrderEnabled && order.customerPhone.isNotBlank()) {
                                val msg = repository.smsTemplateNewOrder
                                    .replace("{name}", order.customerName)
                                    .replace("{order_id}", order.orderNumber)
                                    .replace("{amount}", Helpers.formatPrice(order.totalAmount))
                                repository.sendMeliPayamakSms(order.customerPhone, msg)
                            }
                        }
                        _lastSyncTime.value = JalaliCalendar.getCurrentTime()
                    }
                } catch (e: Exception) {
                    Log.w("WooSync", "Order poll failed: ${e.message}")
                }
            }
        }
    }

    fun syncAllData() {
        viewModelScope.launch(Dispatchers.IO) {
            val activeStore = db.storeDao().getActiveStore() ?: run {
                _syncMessage.value = "هیچ فروشگاهی متصل نیست"
                return@launch
            }
            runFullSync(activeStore)
        }
    }

    fun syncOrdersNow() {
        viewModelScope.launch(Dispatchers.IO) {
            val activeStore = db.storeDao().getActiveStore() ?: return@launch
            _isSyncing.value = true
            try {
                val newOrders = WooCommerceSync.syncOrdersOnly(
                    db = db,
                    baseUrl = activeStore.url,
                    consumerKey = activeStore.consumerKey,
                    consumerSecret = activeStore.consumerSecret
                )
                val today = JalaliCalendar.getTodayJalali().toString()
                for (order in newOrders) {
                    db.notificationDao().insertNotification(
                        WooNotification(
                            title = "سفارش جدید #${order.orderNumber}",
                            body = "${order.customerName} - ${Helpers.formatPrice(order.totalAmount)}",
                            type = "NEW_ORDER",
                            dateJalali = today,
                            isRead = false,
                            linkedId = order.id
                        )
                    )
                }
                _lastSyncTime.value = JalaliCalendar.getCurrentTime()
                _syncMessage.value = if (newOrders.isNotEmpty()) "${newOrders.size} سفارش جدید دریافت شد" else "بروزرسانی انجام شد، سفارش جدیدی نیست"
            } catch (e: Exception) {
                _syncMessage.value = "خطا در دریافت سفارش‌ها"
            } finally {
                _isSyncing.value = false
            }
        }
    }

    // --- SEARCH INPUT TRIGGERS ---
    fun updateOrderSearch(query: String) { _orderSearchQuery.value = query }
    fun setOrderStatusFilter(statusName: String?) { _orderStatusFilter.value = statusName }

    fun updateProductSearch(query: String) { _productSearchQuery.value = query }
    fun setProductFilter(filter: String) { _productFilter.value = filter }
    fun setProductCategoryFilter(category: String) { _productCategoryFilter.value = category }

    fun updateCustomerSearch(query: String) { _customerSearchQuery.value = query }
    fun setCustomerFilter(filter: String) { _customerCategoryFilter.value = filter }

    // --- FILTERED FLOWS ---
    val filteredOrders: Flow<List<WooOrder>> = combine(orders, orderSearchQuery, orderStatusFilter) { list, query, status ->
        list.filter { order ->
            val matchesQuery = query.isBlank() ||
                order.orderNumber.contains(query, ignoreCase = true) ||
                order.customerName.contains(query, ignoreCase = true) ||
                order.customerPhone.contains(query, ignoreCase = true) ||
                order.customerEmail.contains(query, ignoreCase = true) ||
                order.city.contains(query, ignoreCase = true)
            val matchesStatus = status == "ALL" || status == null || order.status == status
            matchesQuery && matchesStatus
        }
    }

    val filteredProducts: Flow<List<WooProduct>> = combine(products, productSearchQuery, productFilter, productCategoryFilter) { list, query, filter, catFilter ->
        list.filter { prod ->
            val matchesQuery = query.isBlank() ||
                prod.name.contains(query, ignoreCase = true) ||
                prod.sku.contains(query, ignoreCase = true)
            val matchesFilter = when (filter) {
                "ALL" -> true
                "OUT_OF_STOCK" -> !prod.inStock || prod.stockQuantity <= 0
                "LOW_STOCK" -> prod.manageStock && prod.stockQuantity <= prod.lowStockThreshold && prod.stockQuantity > 0
                "FEATURED" -> prod.isFeatured
                "DRAFT" -> prod.status == "draft"
                else -> true
            }
            val matchesCategory = catFilter == "ALL" ||
                prod.categories.split(",").map { it.trim() }.contains(catFilter)
            matchesQuery && matchesFilter && matchesCategory
        }
    }

    val filteredCustomers: Flow<List<WooCustomer>> = combine(customers, customerSearchQuery, customerCategoryFilter) { list, query, filter ->
        list.filter { cust ->
            val fullName = "${cust.firstName} ${cust.lastName}"
            val matchesQuery = query.isBlank() ||
                fullName.contains(query, ignoreCase = true) ||
                cust.phone.contains(query, ignoreCase = true) ||
                cust.email.contains(query, ignoreCase = true)
            val matchesFilter = filter == "ALL" || cust.category == filter
            matchesQuery && matchesFilter
        }
    }

    // --- LOGIN ---
    fun performLogin(username: String, password: String) {
        viewModelScope.launch {
            _loginError.value = null
            if (username.isBlank() || password.isBlank()) {
                _loginError.value = "لطفاً نام کاربری و کلمه عبور را وارد کنید"
                return@launch
            }
            val user = db.adminUserDao().getAdminUserByUsername(username)
            if (user != null) {
                if (user.isActive) {
                    repository.activeAdminUsername = username
                    _loggedInUser.value = user
                    _isLoggedIn.value = true
                    val today = JalaliCalendar.getTodayJalali().toString()
                    val time = JalaliCalendar.getCurrentTime()
                    db.adminActivityDao().insertActivity(
                        AdminActivity(
                            adminName = user.fullName,
                            adminRole = user.role,
                            actionType = "LOGIN",
                            details = "ورود موفقیت‌آمیز به پنل ووکامرس",
                            timestampJalali = "$today - $time"
                        )
                    )
                } else {
                    _loginError.value = "حساب کاربری این مدیر غیرفعال است"
                }
            } else {
                _loginError.value = "اطلاعات واردشده صحیح نیست"
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            val user = _loggedInUser.value
            if (user != null) {
                val today = JalaliCalendar.getTodayJalali().toString()
                val time = JalaliCalendar.getCurrentTime()
                db.adminActivityDao().insertActivity(
                    AdminActivity(
                        adminName = user.fullName,
                        adminRole = user.role,
                        actionType = "LOGOUT",
                        details = "خروج موفق از پنل مدیریت",
                        timestampJalali = "$today - $time"
                    )
                )
            }
            pollingJob?.cancel()
            _isLoggedIn.value = false
            _loggedInUser.value = null
            _loginError.value = null
        }
    }

    // --- ORDER OPERATIONS ---
    fun updateOrderStatus(orderId: Long, status: OrderStatus) {
        viewModelScope.launch { repository.changeOrderStatus(orderId, status) }
    }

    fun updateOrderShippingStatus(orderId: Long, shippingStatus: String) {
        viewModelScope.launch { repository.changeOrderShippingStatus(orderId, shippingStatus) }
    }

    fun updateOrderPaymentStatus(orderId: Long, isPaid: Boolean) {
        viewModelScope.launch { repository.changeOrderPaymentStatus(orderId, isPaid) }
    }

    fun addOrderTracking(orderId: Long, trackingCode: String, company: String) {
        viewModelScope.launch { repository.saveOrderTracking(orderId, trackingCode, company) }
    }

    fun updateOrderAdminNotes(orderId: Long, notes: String) {
        viewModelScope.launch { repository.saveOrderAdminNotes(orderId, notes) }
    }

    // --- PRODUCT OPERATIONS ---
    fun updateProductStock(productId: Long, qty: Int) {
        viewModelScope.launch { repository.updateProductStock(productId, qty, qty > 0) }
    }

    fun updateProductPrice(productId: Long, regular: Long, sale: Long) {
        viewModelScope.launch { repository.updateProductPrice(productId, regular, sale) }
    }

    fun deleteProduct(productId: Long, name: String) {
        viewModelScope.launch { repository.deleteProduct(productId, name) }
    }

    fun addProduct(product: WooProduct) {
        viewModelScope.launch { repository.createProduct(product) }
    }

    fun editProduct(product: WooProduct) {
        viewModelScope.launch { repository.updateProduct(product) }
    }

    fun cloneProduct(product: WooProduct) {
        viewModelScope.launch(Dispatchers.IO) {
            val timestamp = System.currentTimeMillis()
            val cloned = product.copy(
                id = -(timestamp % 1_000_000_000L),
                name = "کپی از ${product.name}",
                sku = if (product.sku.isNotBlank()) "COPY-${product.sku.take(18)}" else "",
                status = "draft"
            )
            repository.createProduct(cloned)
        }
    }

    fun bulkUpdateProductPrice(productIds: Set<Long>, percentChange: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            products.value.filter { it.id in productIds }.forEach { prod ->
                val factor = 1.0 + percentChange / 100.0
                val newRegular = (prod.regularPrice * factor).toLong().coerceAtLeast(1)
                val newSale = if (prod.salePrice > 0) (prod.salePrice * factor).toLong().coerceAtLeast(0) else 0L
                repository.updateProductPrice(prod.id, newRegular, newSale)
            }
        }
    }

    fun bulkUpdateOrderStatus(orderIds: Set<Long>, status: OrderStatus) {
        viewModelScope.launch {
            orderIds.forEach { id -> repository.changeOrderStatus(id, status) }
        }
    }

    // --- CUSTOMER OPERATIONS ---
    fun addCustomerNotes(customerId: Long, notes: String) {
        viewModelScope.launch { repository.insertCustomerInternalNotes(customerId, notes) }
    }

    // --- COUPON OPERATIONS ---
    fun createCoupon(coupon: WooCoupon) {
        viewModelScope.launch { repository.addCoupon(coupon) }
    }

    fun removeCoupon(couponId: Long, code: String) {
        viewModelScope.launch { repository.deleteCoupon(couponId, code) }
    }

    fun toggleCouponActive(couponId: Long, active: Boolean) {
        viewModelScope.launch { repository.updateCouponActive(couponId, active) }
    }

    // --- NOTIFICATION OPERATIONS ---
    fun readNotification(id: Long) {
        viewModelScope.launch { repository.markNotificationAsRead(id) }
    }

    fun removeNotification(id: Long) {
        viewModelScope.launch { repository.deleteNotification(id) }
    }

    // --- STORE OPERATIONS ---
    fun changeActiveStore(storeId: Long) {
        viewModelScope.launch {
            repository.switchActiveStore(storeId)
            val newActive = db.storeDao().getActiveStore()
            if (newActive != null) {
                runFullSync(newActive)
                startOrderPolling(newActive)
            }
        }
    }

    fun createStore(store: WooStore) {
        viewModelScope.launch { repository.addNewStore(store) }
    }

    fun removeStore(storeId: Long) {
        viewModelScope.launch { repository.deleteStore(storeId) }
    }

    // --- ADMIN USER OPERATIONS ---
    fun addAdminUser(user: AdminUser) {
        viewModelScope.launch { repository.addAdminUser(user) }
    }

    fun removeAdminUser(userId: Long) {
        viewModelScope.launch { repository.deleteAdminUser(userId) }
    }

    fun toggleAdminUserActive(userId: Long, isActive: Boolean) {
        viewModelScope.launch { db.adminUserDao().updateAdminUserStatus(userId, isActive) }
    }

    // --- SMS ---
    suspend fun sendSmsToCustomer(phone: String, text: String): com.example.core.network.SmsResult {
        return repository.sendMeliPayamakSms(phone, text)
    }

    // --- WOOCOMMERCE CONNECTION ---
    fun connectWooCommerceSecurely(
        storeName: String,
        storeUrl: String,
        consumerKey: String,
        consumerSecret: String,
        isHttpsOnly: Boolean,
        onProgressUpdate: (String) -> Unit,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (storeName.isBlank()) { onError("نام فروشگاه نمی‌تواند خالی باشد"); return@launch }
                if (storeUrl.isBlank() || !storeUrl.startsWith("http")) { onError("نشانی فروشگاه نامعتبر است. باید با http:// یا https:// شروع شود"); return@launch }
                if (isHttpsOnly && !storeUrl.startsWith("https://")) { onError("جهت اتصال امن، نشانی فروشگاه باید با https:// شروع شود"); return@launch }
                if (consumerKey.isBlank() || !consumerKey.startsWith("ck_")) { onError("Consumer Key نامعتبر است. نمونه: ck_..."); return@launch }
                if (consumerSecret.isBlank() || !consumerSecret.startsWith("cs_")) { onError("Consumer Secret نامعتبر است. نمونه: cs_..."); return@launch }

                onProgressUpdate("اتصال به سرور و احراز هویت...")

                val newStore = WooStore(
                    name = storeName,
                    url = storeUrl,
                    consumerKey = consumerKey,
                    consumerSecret = consumerSecret,
                    isActive = true
                )
                db.storeDao().deactivateAllStores()
                repository.addNewStore(newStore)

                val usernameVal = "api_${storeName.lowercase().replace(" ", "_").filter { it.isLetterOrDigit() }}"
                val user = AdminUser(
                    fullName = storeName,
                    username = usernameVal,
                    role = "SUPER_ADMIN",
                    isActive = true
                )
                db.adminUserDao().insertAdminUser(user)

                WooCommerceSync.syncAll(
                    db = db,
                    baseUrl = storeUrl,
                    consumerKey = consumerKey,
                    consumerSecret = consumerSecret,
                    onProgress = { msg -> onProgressUpdate(msg) }
                )

                val today = JalaliCalendar.getTodayJalali().toString()
                val time = JalaliCalendar.getCurrentTime()
                db.adminActivityDao().insertActivity(
                    AdminActivity(
                        adminName = user.fullName,
                        adminRole = user.role,
                        actionType = "LOGIN",
                        details = "اتصال موفق به ووکامرس و همگام‌سازی داده‌های واقعی",
                        timestampJalali = "$today - $time"
                    )
                )

                repository.activeAdminUsername = usernameVal
                _loggedInUser.value = user
                _isLoggedIn.value = true
                _lastSyncTime.value = time

                val savedStore = db.storeDao().getActiveStore()
                if (savedStore != null) startOrderPolling(savedStore)

                onProgressUpdate("همگام‌سازی با موفقیت انجام شد!")
                onSuccess()
            } catch (e: Exception) {
                onError("خطا در برقراری ارتباط: ${e.localizedMessage}")
            }
        }
    }

    // --- INTERNAL ORDER CREATION ---
    fun createManualOrder(
        items: List<Pair<WooProduct, Int>>,
        customerName: String,
        customerPhone: String,
        customerAddress: String,
        discount: Long,
        paymentMethod: String,
        note: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val timestamp = System.currentTimeMillis()
            val today = JalaliCalendar.getTodayJalali().toString()
            val time = JalaliCalendar.getCurrentTime()

            val orderItems = items.map { (prod, qty) ->
                val unitPrice = prod.salePrice.takeIf { it > 0 } ?: prod.regularPrice
                OrderItem(
                    productId = prod.id,
                    productName = prod.name,
                    quantity = qty,
                    unitPrice = unitPrice,
                    totalPrice = unitPrice * qty,
                    image = prod.mainImage
                )
            }
            val subtotal = orderItems.sumOf { it.totalPrice }
            val total = (subtotal - discount).coerceAtLeast(0)
            val isPaid = paymentMethod == "نقد" || paymentMethod == "کارت‌خوان"

            val order = WooOrder(
                id = -(timestamp % 1_000_000_000L),
                orderNumber = "MAN-${timestamp % 100000}",
                status = OrderStatus.PROCESSING.name,
                createdAtJalali = today,
                createdAtTime = time,
                customerName = customerName,
                customerPhone = customerPhone,
                customerEmail = "",
                billingAddress = customerAddress,
                shippingAddress = customerAddress,
                city = "",
                province = "",
                postalCode = "",
                customerNote = note,
                items = orderItems,
                subtotal = subtotal,
                discount = discount,
                shippingCost = 0L,
                totalAmount = total,
                paymentMethod = paymentMethod,
                isPaid = isPaid,
                adminNotes = "سفارش داخلی ثبت‌شده از پنل مدیریت"
            )

            repository.createSimulatedOrder(order)

            for ((prod, qty) in items) {
                if (prod.manageStock) {
                    val newQty = (prod.stockQuantity - qty).coerceAtLeast(0)
                    repository.updateProductStock(prod.id, newQty, newQty > 0)
                }
            }
        }
    }

    // --- AI ANALYSIS ---
    fun runAiAnalysis(prompt: String) {
        viewModelScope.launch {
            _isAiLoading.value = true
            _aiResult.value = null
            try {
                val ordersList = orders.value
                val lowStockList = products.value.filter { it.manageStock && it.stockQuantity <= it.lowStockThreshold }
                val contextPrompt = """
                    آمار فروشگاه:
                    تعداد کل سفارش‌ها: ${ordersList.size}
                    جمع کل فروش: ${Helpers.formatPrice(ordersList.sumOf { it.totalAmount })}
                    کالاهای کم‌موجودی: ${lowStockList.size} (${lowStockList.joinToString { it.name }})

                    $prompt
                """.trimIndent()
                _aiResult.value = GeminiManager.generateAnalysis(contextPrompt)
            } catch (e: Exception) {
                _aiResult.value = "خطا در ارتباط با هوش مصنوعی: ${e.localizedMessage}"
            } finally {
                _isAiLoading.value = false
            }
        }
    }
}
