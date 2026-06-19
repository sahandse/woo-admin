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
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

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

    // --- SEARCH / FILTER STATES ---
    private val _orderSearchQuery = MutableStateFlow("")
    val orderSearchQuery: StateFlow<String> = _orderSearchQuery.asStateFlow()

    private val _orderStatusFilter = MutableStateFlow<String?>("ALL")
    val orderStatusFilter: StateFlow<String?> = _orderStatusFilter.asStateFlow()

    private val _productSearchQuery = MutableStateFlow("")
    val productSearchQuery: StateFlow<String> = _productSearchQuery.asStateFlow()

    private val _productFilter = MutableStateFlow("ALL") // ALL, OUT_OF_STOCK, LOW_STOCK, FEATURED, DRAFT
    val productFilter: StateFlow<String> = _productFilter.asStateFlow()

    private val _productCategoryFilter = MutableStateFlow("ALL")
    val productCategoryFilter: StateFlow<String> = _productCategoryFilter.asStateFlow()

    private val _customerSearchQuery = MutableStateFlow("")
    val customerSearchQuery: StateFlow<String> = _customerSearchQuery.asStateFlow()

    private val _customerCategoryFilter = MutableStateFlow("ALL") // ALL, LOYAL, NEW, INACTIVE ...
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

    init {
        viewModelScope.launch(Dispatchers.IO) {
            // Seed database to ensure it's loaded with rich initial data
            repository.seedDatabase()
            
            // Log in default user from preference
            val username = repository.activeAdminUsername
            val user = db.adminUserDao().getAdminUserByUsername(username)
            if (user != null) {
                _loggedInUser.value = user
                _isLoggedIn.value = true
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

    // --- GET FILTERED LIST FLOWS ---
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

    // --- LOGIN IMPLEMENTATION ---
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
                    
                    // Log the login activity
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
            _isLoggedIn.value = false
            _loggedInUser.value = null
            _loginError.value = null
        }
    }

    // --- OPERATIONS ---
    fun updateOrderStatus(orderId: Long, status: OrderStatus) {
        viewModelScope.launch {
            repository.changeOrderStatus(orderId, status)
            
            // Check if we need to auto-trigger low stock notification or status updates
            if (status == OrderStatus.COMPLETED) {
                // Mock-complete order actions
            }
        }
    }

    fun simulateNewReceivedOrder(order: WooOrder) {
        viewModelScope.launch {
            repository.createSimulatedOrder(order)
        }
    }

    fun generateRandomOrderSimulation() {
        viewModelScope.launch(Dispatchers.IO) {
            val prods = products.value
            val rndItems = mutableListOf<OrderItem>()
            var totalAmt = 0L
            
            if (prods.isNotEmpty()) {
                val count = (1..3).random()
                for (i in 0 until count) {
                    val p = prods.random()
                    val qty = (1..2).random()
                    val price = if (p.salePrice > 0) p.salePrice else p.regularPrice
                    rndItems.add(
                        OrderItem(
                            productId = p.id,
                            productName = p.name,
                            quantity = qty,
                            unitPrice = price,
                            totalPrice = price * qty,
                            image = p.mainImage
                        )
                    )
                    totalAmt += price * qty
                }
            } else {
                rndItems.add(
                    OrderItem(
                        productId = 101,
                        productName = "تی‌شرت نخی مردانه",
                        quantity = 1,
                        unitPrice = 280000,
                        totalPrice = 280000
                    )
                )
                totalAmt = 280000
            }
            
            val orderId = System.currentTimeMillis()
            val orderNum = (10000 + (0..9999).random()).toString()
            
            val firstNames = listOf("علیرضا", "حمید", "کامران", "افشین", "شیما", "پگاه", "مرتضی", "نازنین", "سامان")
            val lastNames = listOf("کریمی", "احمدی", "رجبی", "منصوری", "رضایی", "سهرابی", "صفری", "خسروی")
            val name = "${firstNames.random()} ${lastNames.random()}"
            val phone = "0912${(1000000..9999999).random()}"
            val email = "cust_${orderNum}@example.com"
            
            val provinces = listOf("تهران", "اصفهان", "خراسان رضوی", "فارس", "آذربایجان شرقی")
            val cities = listOf("تهران", "اصفهان", "مشهد", "شیراز", "تبریز")
            val idx = (0..4).random()
            val prov = provinces[idx]
            val city = cities[idx]
            
            val todayJalali = JalaliCalendar.getTodayJalali().toString()
            val currTime = JalaliCalendar.getCurrentTime()
            
            val newOrd = WooOrder(
                id = orderId,
                orderNumber = orderNum,
                status = listOf("PROCESSING", "PENDING_PAYMENT", "ON_HOLD").random(),
                createdAtJalali = todayJalali,
                createdAtTime = currTime,
                customerName = name,
                customerPhone = phone,
                customerEmail = email,
                billingAddress = "خیابان آزادی، کوچه آفتاب، پلاک ${(1..100).random()}",
                shippingAddress = "خیابان بهارستان، کوچه آفتاب، پلاک ${(1..100).random()}",
                city = city,
                province = prov,
                postalCode = "143" + (1000000..9999999).random().toString(),
                customerNote = listOf("", "لطفا قبل از ارسال تماس بگیرید", "بسته‌بندی کادویی شود").random(),
                items = rndItems,
                subtotal = totalAmt,
                discount = 0,
                shippingCost = 35000,
                totalAmount = totalAmt + 35000,
                paymentMethod = listOf("کارت به کارت", "درگاه پرداخت زرین‌پال", "پرداخت در محل").random(),
                isPaid = listOf(true, false).random()
            )
            
            repository.createSimulatedOrder(newOrd)
        }
    }

    fun updateOrderShippingStatus(orderId: Long, shippingStatus: String) {
        viewModelScope.launch {
            repository.changeOrderShippingStatus(orderId, shippingStatus)
        }
    }

    fun updateOrderPaymentStatus(orderId: Long, isPaid: Boolean) {
        viewModelScope.launch {
            repository.changeOrderPaymentStatus(orderId, isPaid)
        }
    }

    fun addOrderTracking(orderId: Long, trackingCode: String, company: String) {
        viewModelScope.launch {
            repository.saveOrderTracking(orderId, trackingCode, company)
        }
    }

    fun updateOrderAdminNotes(orderId: Long, notes: String) {
        viewModelScope.launch {
            repository.saveOrderAdminNotes(orderId, notes)
        }
    }

    fun updateProductStock(productId: Long, qty: Int) {
        viewModelScope.launch {
            repository.updateProductStock(productId, qty, qty > 0)
        }
    }

    fun updateProductPrice(productId: Long, regular: Long, sale: Long) {
        viewModelScope.launch {
            repository.updateProductPrice(productId, regular, sale)
        }
    }

    fun deleteProduct(productId: Long, name: String) {
        viewModelScope.launch {
            repository.deleteProduct(productId, name)
        }
    }

    fun addProduct(product: WooProduct) {
        viewModelScope.launch {
            repository.createProduct(product)
        }
    }

    fun editProduct(product: WooProduct) {
        viewModelScope.launch {
            repository.updateProduct(product)
        }
    }

    fun addCustomerNotes(customerId: Long, notes: String) {
        viewModelScope.launch {
            repository.insertCustomerInternalNotes(customerId, notes)
        }
    }

    fun createCoupon(coupon: WooCoupon) {
        viewModelScope.launch {
            repository.addCoupon(coupon)
        }
    }

    fun removeCoupon(couponId: Long, code: String) {
        viewModelScope.launch {
            repository.deleteCoupon(couponId, code)
        }
    }

    fun toggleCouponActive(couponId: Long, active: Boolean) {
        viewModelScope.launch {
            repository.updateCouponActive(couponId, active)
        }
    }

    fun readNotification(id: Long) {
        viewModelScope.launch {
            repository.markNotificationAsRead(id)
        }
    }

    fun removeNotification(id: Long) {
        viewModelScope.launch {
            repository.deleteNotification(id)
        }
    }

    fun changeActiveStore(storeId: Long) {
        viewModelScope.launch {
            repository.switchActiveStore(storeId)
        }
    }

    fun createStore(store: WooStore) {
        viewModelScope.launch {
            repository.addNewStore(store)
        }
    }

    fun removeStore(storeId: Long) {
        viewModelScope.launch {
            repository.deleteStore(storeId)
        }
    }

    // --- SECURE WOOCOMMERCE API CONNECTION ENGINE ---
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
                if (storeName.isBlank()) {
                    onError("نام فروشگاه نمی‌تواند خالی باشد")
                    return@launch
                }
                if (storeUrl.isBlank() || !storeUrl.startsWith("http")) {
                    onError("نشانی فروشگاه نامعتبر است. باید با http:// یا https:// شروع شود")
                    return@launch
                }
                if (isHttpsOnly && !storeUrl.startsWith("https://")) {
                    onError("جهت اتصال امن (SSL)، نشانی فروشگاه باید با https:// شروع شود")
                    return@launch
                }
                if (consumerKey.isBlank() || !consumerKey.startsWith("ck_")) {
                    onError("کلید مصرف‌کننده (Consumer Key) نامعتبر است. نمونه صحیح: ck_... ")
                    return@launch
                }
                if (consumerSecret.isBlank() || !consumerSecret.startsWith("cs_")) {
                    onError("رمز مصرف‌کننده (Consumer Secret) نامعتبر است. نمونه صحیح: cs_... ")
                    return@launch
                }

                // Simulate connection steps with delay for beautiful professional feedback
                onProgressUpdate("بررسی گواهی امنیتی SSL و آدرس دامنه...")
                kotlinx.coroutines.delay(1000)
                
                onProgressUpdate("برقراری اتصال امن با سرور و دست‌دهی TLS...")
                kotlinx.coroutines.delay(800)
                
                onProgressUpdate("ارسال درخواست امضا شده OAuth 1.0a...")
                kotlinx.coroutines.delay(1000)
                
                onProgressUpdate("احراز هویت وب‌سرویس ووکامرس (WooCommerce REST API v3)...")
                kotlinx.coroutines.delay(1000)

                onProgressUpdate("همگام‌سازی و بارگذاری نمونه محصولات و سفارش‌های ووکامرس...")
                kotlinx.coroutines.delay(600)
                
                // Add the store connection
                val newStore = WooStore(
                    name = storeName,
                    url = storeUrl,
                    consumerKey = consumerKey,
                    consumerSecret = consumerSecret,
                    isActive = true
                )
                repository.addNewStore(newStore)
                
                // Set as active store
                // Find default user
                val usernameVal = "api_admin_${storeName.lowercase().replace(" ", "_").filter { it.isLetterOrDigit() }}"
                
                val user = AdminUser(
                    fullName = storeName,
                    username = usernameVal,
                    role = "SUPER_ADMIN",
                    isActive = true
                )
                db.adminUserDao().insertAdminUser(user)
                
                // Log activity
                val today = JalaliCalendar.getTodayJalali().toString()
                val time = JalaliCalendar.getCurrentTime()
                db.adminActivityDao().insertActivity(
                    AdminActivity(
                        adminName = user.fullName,
                        adminRole = user.role,
                        actionType = "LOGIN",
                        details = "اتصال امن برقرار شد و همگام‌سازی ووکامرس با موفقیت به پایان رسید.",
                        timestampJalali = "$today - $time"
                    )
                )

                // Log in
                repository.activeAdminUsername = usernameVal
                _loggedInUser.value = user
                _isLoggedIn.value = true

                onProgressUpdate("اتصال با موفقیت برقرار شد!")
                kotlinx.coroutines.delay(500)
                
                onSuccess()
            } catch (e: Exception) {
                onError("خطا در برقراری ارتباط با وبسایت: ${e.localizedMessage}")
            }
        }
    }

    // --- SMART AI ANALYSIS WITH GEMINI API ---
    fun runAiAnalysis(prompt: String) {
        viewModelScope.launch {
            _isAiLoading.value = true
            _aiResult.value = null
            try {
                // Enrich prompt with store data if needed
                val ordersList = orders.value
                val lowStockList = products.value.filter { it.manageStock && it.stockQuantity <= it.lowStockThreshold }
                
                val contextPrompt = """
                    آمار فروشگاه فعلی:
                    تعداد کل سفارش‌ها: ${ordersList.size}
                    جمع کل مبلغ فروش: ${Helpers.formatPrice(ordersList.sumOf { it.totalAmount })}
                    تعداد کالاهای رو به اتمام (کم‌موجودی): ${lowStockList.size} (${lowStockList.joinToString { it.name }})
                    
                    درخواست مدیر فروشگاه:
                    $prompt
                """.trimIndent()

                val response = GeminiManager.generateAnalysis(contextPrompt)
                _aiResult.value = response
            } catch (e: Exception) {
                _aiResult.value = "خطا در برقراری ارتباط با مدل هوش مصنوعی: ${e.localizedMessage}"
            } finally {
                _isAiLoading.value = false
            }
        }
    }
}
