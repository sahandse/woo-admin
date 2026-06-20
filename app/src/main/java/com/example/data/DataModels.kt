package com.sahand.wooadmin.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

// --- STORE ENTITY ---
@Entity(tableName = "woo_stores")
data class WooStore(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val url: String,
    val consumerKey: String,
    val consumerSecret: String,
    val isActive: Boolean = false
)

// --- ORDER ENTITY & STATUSES ---
enum class OrderStatus(val persianLabel: String, val colorHex: String) {
    PENDING_PAYMENT("در انتظار پرداخت", "#FF9800"),
    PROCESSING("در حال انجام", "#2196F3"),
    COMPLETED("تکمیل‌شده", "#4CAF50"),
    CANCELLED("لغوشده", "#F44336"),
    REFUNDED("مرجوع‌شده", "#9C27B0"),
    FAILED("ناموفق", "#E91E63"),
    ON_HOLD("در انتظار بررسی", "#795548"),
    READY_TO_SHIP("آماده ارسال", "#00BCD4"),
    SHIPPED("ارسال‌شده", "#3F51B5"),
    NEEDS_FOLLOWUP("نیازمند پیگیری", "#FF5722")
}

@JsonClass(generateAdapter = true)
data class OrderItem(
    val productId: Long,
    val productName: String,
    val quantity: Int,
    val unitPrice: Long,
    val totalPrice: Long,
    val image: String = ""
)

@Entity(tableName = "woo_orders")
data class WooOrder(
    @PrimaryKey val id: Long,
    val orderNumber: String,
    val status: String, // mapped to OrderStatus enum name
    val createdAtJalali: String,
    val createdAtTime: String,
    val customerName: String,
    val customerPhone: String,
    val customerEmail: String,
    val billingAddress: String,
    val shippingAddress: String,
    val city: String,
    val province: String,
    val postalCode: String,
    val customerNote: String,
    val items: List<OrderItem>,
    val subtotal: Long,
    val discount: Long,
    val shippingCost: Long,
    val totalAmount: Long,
    val paymentMethod: String,
    val isPaid: Boolean,
    val trackingCode: String = "",
    val shippingCompany: String = "",
    val shippingStatus: String = "READY_TO_PACK", // READY_TO_PACK, PACKED, READY_TO_SHIP, HANDED_TO_POST, HANDED_TO_COURIER, SHIPPED, DELIVERED, RETURNED
    val adminNotes: String = ""
)

// --- PRODUCT ENTITY ---
@Entity(tableName = "woo_products")
data class WooProduct(
    @PrimaryKey val id: Long,
    val name: String,
    val slug: String,
    val shortDescription: String,
    val description: String,
    val regularPrice: Long,
    val salePrice: Long = 0,
    val isPromoActive: Boolean = false,
    val promoStartJalali: String = "",
    val promoEndJalali: String = "",
    val sku: String,
    val manageStock: Boolean,
    val stockQuantity: Int,
    val inStock: Boolean,
    val lowStockThreshold: Int = 5,
    val weight: Double = 0.0,
    val length: Double = 0.0,
    val width: Double = 0.0,
    val height: Double = 0.0,
    val categories: String, // Comma-separated list like "پوشاک, کفش"
    val tags: String, // Comma-separated list
    val mainImage: String,
    val galleryImages: List<String> = emptyList(), // Comma-separated URLs
    val isFeatured: Boolean = false,
    val isVirtual: Boolean = false,
    val isDownloadable: Boolean = false,
    val status: String = "publish", // publish, draft
    val colors: List<String> = emptyList(),
    val sizes: List<String> = emptyList(),
    val warehouseNote: String = "",
    // Advanced fields
    val purchaseNote: String = "",
    val backorders: String = "no", // no, notify, yes
    val soldIndividually: Boolean = false,
    val externalUrl: String = "",
    val buttonText: String = "",
    val seoTitle: String = "",
    val seoDescription: String = "",
    val minQuantity: Int = 1,
    val maxQuantity: Int = 0, // 0 = unlimited
    val linkedUpsells: String = "", // comma-separated product IDs
    val linkedCrossSells: String = "" // comma-separated product IDs
)

// --- CUSTOMER ENTITY ---
enum class CustomerCategory(val persianLabel: String, val colorHex: String) {
    NEW("مشتری جدید", "#4CAF50"),
    REGULAR("مشتری عادی", "#2196F3"),
    LOYAL("مشتری وفادار", "#E91E63"),
    HIGH_VALUE("مشتری پرخرید", "#9C27B0"),
    INACTIVE("مشتری غیرفعال", "#9E9E9E"),
    NEEDS_FOLLOW_UP("نیازمند پیگیری", "#FF9800"),
    PROBLEMATIC("مشتری مشکل‌دار", "#F44336")
}

@Entity(tableName = "woo_customers")
data class WooCustomer(
    @PrimaryKey val id: Long,
    val firstName: String,
    val lastName: String,
    val phone: String,
    val email: String,
    val registeredDateJalali: String,
    val ordersCount: Int,
    val totalSpent: Long,
    val lastPurchaseDateJalali: String,
    val averageOrderValue: Long,
    val billingAddress: String,
    val shippingAddress: String,
    val category: String, // mapped to CustomerCategory
    val internalNotes: String = "",
    val tags: String = ""
)

// --- COUPON ENTITY ---
@Entity(tableName = "woo_coupons")
data class WooCoupon(
    @PrimaryKey val id: Long,
    val code: String,
    val discountType: String, // percent, fixed_cart, fixed_product
    val amount: Long,
    val minSpend: Long = 0,
    val maxDiscount: Long = 0,
    val startJalali: String = "",
    val expiryJalali: String = "",
    val usageLimit: Int = 0,
    val usageLimitPerUser: Int = 0,
    val usedCount: Int = 0,
    val active: Boolean = true,
    val freeShipping: Boolean = false,
    val excludedProducts: String = "" // product IDs
)

// --- NOTIFICATION ENTITY ---
@Entity(tableName = "woo_notifications")
data class WooNotification(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val body: String,
    val type: String, // NEW_ORDER, LOW_STOCK, OUT_OF_STOCK, NEW_CUSTOMER, COMPLETED_ORDER
    val dateJalali: String,
    val isRead: Boolean = false,
    val linkedId: Long = 0 // points to order or product ID
)

// --- ADMIN USERS (Internal Admin profiles) ---
enum class AdminRole(val label: String, val description: String) {
    SUPER_ADMIN("مدیر اصلی", "دسترسی کامل به همه بخش‌ها"),
    STORE_ADMIN("مدیر فروشگاه", "دسترسی به سفارش‌ها، محصولات، مشتریان و گزارش‌ها"),
    ORDER_ADMIN("مدیر سفارش‌ها", "فقط ثبت کد رهگیری، تغییر وضعیت و یادداشت سفارش"),
    PRODUCT_ADMIN("مدیر محصولات", "فقط دسترسی به محصولات، قیمت، موجودی، دسته‌بندی"),
    STOCK_CLERK("انباردار", "مشاهده سفارش‌های آماده ارسال، ثبت وضعیت ارسال و کنترل موجودی"),
    ACCOUNTANT("حسابدار", "مشاهده گزارش‌های مالی، سفارش‌ها، پرداخت‌ها و خروجی فاکتور"),
    SUPPORT("پشتیبان مشتری", "تماس، پیامک، مشاهده مشتریان و سفارش‌های مرتبط")
}

@Entity(tableName = "admin_users")
data class AdminUser(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fullName: String,
    val username: String,
    val role: String, // mapped to AdminRole name
    val isActive: Boolean = true,
    val lastLoginJalali: String = "",
    val permissions: List<String> = emptyList() // sub-attributes
)

// --- ACTIVITY LOGS ---
@Entity(tableName = "admin_activities")
data class AdminActivity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val adminName: String,
    val adminRole: String,
    val actionType: String, // LOGIN, LOGOUT, CHANGE_STATUS, EDIT_PRICE, EDIT_STOCK, REMOVE_PRODUCT, CREATE_COUPON
    val details: String,
    val timestampJalali: String
)

// --- TYPE CONVERTERS FOR COMPLEX PROPERTIES ---
class RoomTypeConverters {
    private val moshi = Moshi.Builder().build()

    @TypeConverter
    fun fromOrderItemList(value: List<OrderItem>?): String {
        if (value == null) return "[]"
        val type = Types.newParameterizedType(List::class.java, OrderItem::class.java)
        val adapter = moshi.adapter<List<OrderItem>>(type)
        return adapter.toJson(value)
    }

    @TypeConverter
    fun toOrderItemList(value: String): List<OrderItem> {
        val type = Types.newParameterizedType(List::class.java, OrderItem::class.java)
        val adapter = moshi.adapter<List<OrderItem>>(type)
        return adapter.fromJson(value) ?: emptyList()
    }

    @TypeConverter
    fun fromStringList(value: List<String>?): String {
        if (value == null) return "[]"
        val type = Types.newParameterizedType(List::class.java, String::class.java)
        val adapter = moshi.adapter<List<String>>(type)
        return adapter.toJson(value)
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        val type = Types.newParameterizedType(List::class.java, String::class.java)
        val adapter = moshi.adapter<List<String>>(type)
        return adapter.fromJson(value) ?: emptyList()
    }
}
