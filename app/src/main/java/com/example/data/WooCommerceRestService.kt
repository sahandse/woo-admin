package com.example.data

import android.util.Log
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// --- WooCommerce REST API Response DTOs ---

@JsonClass(generateAdapter = true)
data class WcProductDto(
    @Json(name = "id") val id: Long = 0,
    @Json(name = "name") val name: String = "",
    @Json(name = "slug") val slug: String = "",
    @Json(name = "description") val description: String = "",
    @Json(name = "short_description") val shortDescription: String = "",
    @Json(name = "sku") val sku: String = "",
    @Json(name = "regular_price") val regularPrice: String = "0",
    @Json(name = "sale_price") val salePrice: String = "0",
    @Json(name = "stock_quantity") val stockQuantity: Int? = 0,
    @Json(name = "manage_stock") val manageStock: Boolean = false,
    @Json(name = "stock_status") val stockStatus: String = "instock",
    @Json(name = "status") val status: String = "publish",
    @Json(name = "featured") val featured: Boolean = false,
    @Json(name = "virtual") val virtual: Boolean = false,
    @Json(name = "downloadable") val downloadable: Boolean = false,
    @Json(name = "categories") val categories: List<WcTermDto> = emptyList(),
    @Json(name = "tags") val tags: List<WcTermDto> = emptyList(),
    @Json(name = "images") val images: List<WcImageDto> = emptyList(),
    @Json(name = "weight") val weight: String = "0",
    @Json(name = "dimensions") val dimensions: WcDimensionsDto? = null
)

@JsonClass(generateAdapter = true)
data class WcTermDto(
    @Json(name = "id") val id: Long = 0,
    @Json(name = "name") val name: String = ""
)

@JsonClass(generateAdapter = true)
data class WcImageDto(
    @Json(name = "src") val src: String = ""
)

@JsonClass(generateAdapter = true)
data class WcDimensionsDto(
    @Json(name = "length") val length: String = "0",
    @Json(name = "width") val width: String = "0",
    @Json(name = "height") val height: String = "0"
)

@JsonClass(generateAdapter = true)
data class WcOrderDto(
    @Json(name = "id") val id: Long = 0,
    @Json(name = "number") val number: String = "",
    @Json(name = "status") val status: String = "processing",
    @Json(name = "date_created") val dateCreated: String = "",
    @Json(name = "billing") val billing: WcAddressDto = WcAddressDto(),
    @Json(name = "shipping") val shipping: WcAddressDto = WcAddressDto(),
    @Json(name = "line_items") val lineItems: List<WcLineItemDto> = emptyList(),
    @Json(name = "shipping_total") val shippingTotal: String = "0",
    @Json(name = "discount_total") val discountTotal: String = "0",
    @Json(name = "total") val total: String = "0",
    @Json(name = "payment_method_title") val paymentMethodTitle: String = "",
    @Json(name = "date_paid") val datePaid: String? = null,
    @Json(name = "customer_note") val customerNote: String = ""
)

@JsonClass(generateAdapter = true)
data class WcAddressDto(
    @Json(name = "first_name") val firstName: String = "",
    @Json(name = "last_name") val lastName: String = "",
    @Json(name = "phone") val phone: String = "",
    @Json(name = "email") val email: String = "",
    @Json(name = "address_1") val address1: String = "",
    @Json(name = "address_2") val address2: String = "",
    @Json(name = "city") val city: String = "",
    @Json(name = "state") val state: String = "",
    @Json(name = "postcode") val postcode: String = ""
)

@JsonClass(generateAdapter = true)
data class WcLineItemDto(
    @Json(name = "product_id") val productId: Long = 0,
    @Json(name = "name") val name: String = "",
    @Json(name = "quantity") val quantity: Int = 1,
    @Json(name = "price") val price: String = "0",
    @Json(name = "total") val total: String = "0",
    @Json(name = "image") val image: WcImageDto? = null
)

@JsonClass(generateAdapter = true)
data class WcCustomerDto(
    @Json(name = "id") val id: Long = 0,
    @Json(name = "first_name") val firstName: String = "",
    @Json(name = "last_name") val lastName: String = "",
    @Json(name = "email") val email: String = "",
    @Json(name = "date_created") val dateCreated: String = "",
    @Json(name = "orders_count") val ordersCount: Int = 0,
    @Json(name = "total_spent") val totalSpent: String = "0",
    @Json(name = "billing") val billing: WcAddressDto = WcAddressDto()
)

@JsonClass(generateAdapter = true)
data class WcCouponDto(
    @Json(name = "id") val id: Long = 0,
    @Json(name = "code") val code: String = "",
    @Json(name = "discount_type") val discountType: String = "percent",
    @Json(name = "amount") val amount: String = "0",
    @Json(name = "minimum_amount") val minimumAmount: String = "0",
    @Json(name = "maximum_amount") val maximumAmount: String = "0",
    @Json(name = "usage_limit") val usageLimit: Int? = null,
    @Json(name = "usage_limit_per_user") val usageLimitPerUser: Int? = null,
    @Json(name = "usage_count") val usageCount: Int = 0,
    @Json(name = "date_expires") val dateExpires: String? = null,
    @Json(name = "free_shipping") val freeShipping: Boolean = false,
    @Json(name = "status") val status: String = "publish"
)

// --- Retrofit Interface ---
interface WooCommerceRestApi {
    @GET("wp-json/wc/v3/products")
    suspend fun getProducts(@Query("per_page") perPage: Int = 100, @Query("page") page: Int = 1): List<WcProductDto>

    @GET("wp-json/wc/v3/orders")
    suspend fun getOrders(@Query("per_page") perPage: Int = 100, @Query("page") page: Int = 1): List<WcOrderDto>

    @GET("wp-json/wc/v3/customers")
    suspend fun getCustomers(@Query("per_page") perPage: Int = 100, @Query("page") page: Int = 1): List<WcCustomerDto>

    @GET("wp-json/wc/v3/coupons")
    suspend fun getCoupons(@Query("per_page") perPage: Int = 100, @Query("page") page: Int = 1): List<WcCouponDto>
}

// --- Sync Engine ---
object WooCommerceSync {

    fun buildApi(baseUrl: String, consumerKey: String, consumerSecret: String): WooCommerceRestApi {
        val url = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val req = chain.request().newBuilder()
                    .header("Authorization", Credentials.basic(consumerKey, consumerSecret))
                    .build()
                chain.proceed(req)
            }
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
        return Retrofit.Builder()
            .baseUrl(url)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(WooCommerceRestApi::class.java)
    }

    private fun mapProduct(dto: WcProductDto): WooProduct = WooProduct(
        id = dto.id,
        name = dto.name,
        slug = dto.slug,
        shortDescription = dto.shortDescription,
        description = dto.description,
        regularPrice = dto.regularPrice.toDoubleOrNull()?.toLong() ?: 0L,
        salePrice = dto.salePrice.toDoubleOrNull()?.toLong() ?: 0L,
        sku = dto.sku,
        manageStock = dto.manageStock,
        stockQuantity = dto.stockQuantity ?: 0,
        inStock = dto.stockStatus == "instock",
        status = dto.status,
        isFeatured = dto.featured,
        isVirtual = dto.virtual,
        isDownloadable = dto.downloadable,
        categories = dto.categories.joinToString(", ") { it.name },
        tags = dto.tags.joinToString(", ") { it.name },
        mainImage = dto.images.firstOrNull()?.src ?: "",
        galleryImages = if (dto.images.size > 1) dto.images.drop(1).map { it.src } else emptyList(),
        weight = dto.weight.toDoubleOrNull() ?: 0.0,
        length = dto.dimensions?.length?.toDoubleOrNull() ?: 0.0,
        width = dto.dimensions?.width?.toDoubleOrNull() ?: 0.0,
        height = dto.dimensions?.height?.toDoubleOrNull() ?: 0.0
    )

    private fun mapOrder(dto: WcOrderDto): WooOrder {
        val status = when (dto.status.lowercase()) {
            "pending" -> "PENDING_PAYMENT"
            "processing" -> "PROCESSING"
            "on-hold" -> "ON_HOLD"
            "completed" -> "COMPLETED"
            "cancelled" -> "CANCELLED"
            "refunded" -> "REFUNDED"
            "failed" -> "FAILED"
            else -> "PROCESSING"
        }
        val customerName = "${dto.billing.firstName} ${dto.billing.lastName}".trim()
            .ifBlank { "مشتری ووکامرس" }
        val address = listOf(dto.billing.address1, dto.billing.address2)
            .filter { it.isNotBlank() }.joinToString("، ")
        val shippingAddr = dto.shipping.address1.ifBlank { address }
        val items = dto.lineItems.map { li ->
            OrderItem(
                productId = li.productId,
                productName = li.name,
                quantity = li.quantity,
                unitPrice = li.price.toDoubleOrNull()?.toLong() ?: 0L,
                totalPrice = li.total.toDoubleOrNull()?.toLong() ?: 0L,
                image = li.image?.src ?: ""
            )
        }
        val dateParts = dto.dateCreated.split("T")
        return WooOrder(
            id = dto.id,
            orderNumber = dto.number,
            status = status,
            createdAtJalali = dateParts.firstOrNull() ?: dto.dateCreated,
            createdAtTime = if (dateParts.size > 1) dateParts[1].take(5) else "",
            customerName = customerName,
            customerPhone = dto.billing.phone,
            customerEmail = dto.billing.email,
            billingAddress = address,
            shippingAddress = shippingAddr,
            city = dto.billing.city,
            province = dto.billing.state,
            postalCode = dto.billing.postcode,
            customerNote = dto.customerNote,
            items = items,
            subtotal = items.sumOf { it.totalPrice },
            discount = dto.discountTotal.toDoubleOrNull()?.toLong() ?: 0L,
            shippingCost = dto.shippingTotal.toDoubleOrNull()?.toLong() ?: 0L,
            totalAmount = dto.total.toDoubleOrNull()?.toLong() ?: 0L,
            paymentMethod = dto.paymentMethodTitle,
            isPaid = dto.datePaid != null
        )
    }

    private fun mapCustomer(dto: WcCustomerDto): WooCustomer {
        val totalSpent = dto.totalSpent.toDoubleOrNull()?.toLong() ?: 0L
        val avgOrder = if (dto.ordersCount > 0) totalSpent / dto.ordersCount else 0L
        val category = when {
            dto.ordersCount == 0 -> "INACTIVE"
            dto.ordersCount >= 10 -> "LOYAL"
            totalSpent >= 10_000_000L -> "HIGH_VALUE"
            dto.ordersCount <= 2 -> "NEW"
            else -> "REGULAR"
        }
        val dateParts = dto.dateCreated.split("T")
        return WooCustomer(
            id = dto.id,
            firstName = dto.firstName,
            lastName = dto.lastName,
            phone = dto.billing.phone,
            email = dto.email,
            registeredDateJalali = dateParts.firstOrNull() ?: dto.dateCreated,
            ordersCount = dto.ordersCount,
            totalSpent = totalSpent,
            lastPurchaseDateJalali = "",
            averageOrderValue = avgOrder,
            billingAddress = listOf(dto.billing.address1, dto.billing.address2)
                .filter { it.isNotBlank() }.joinToString("، "),
            shippingAddress = "",
            category = category
        )
    }

    private fun mapCoupon(dto: WcCouponDto): WooCoupon = WooCoupon(
        id = dto.id,
        code = dto.code.uppercase(),
        discountType = dto.discountType,
        amount = dto.amount.toDoubleOrNull()?.toLong() ?: 0L,
        minSpend = dto.minimumAmount.toDoubleOrNull()?.toLong() ?: 0L,
        maxDiscount = dto.maximumAmount.toDoubleOrNull()?.toLong() ?: 0L,
        usageLimit = dto.usageLimit ?: 0,
        usageLimitPerUser = dto.usageLimitPerUser ?: 1,
        usedCount = dto.usageCount,
        active = dto.status == "publish",
        freeShipping = dto.freeShipping,
        expiryJalali = dto.dateExpires ?: ""
    )

    suspend fun syncOrdersOnly(
        db: AppDatabase,
        baseUrl: String,
        consumerKey: String,
        consumerSecret: String
    ): List<WooOrder> = withContext(Dispatchers.IO) {
        val api = buildApi(baseUrl, consumerKey, consumerSecret)
        val existingIds = db.orderDao().getAllOrderIds().toSet()
        val orders = api.getOrders()
        val mapped = orders.map { mapOrder(it) }
        db.orderDao().insertOrders(mapped)
        mapped.filter { it.id !in existingIds }
    }

    suspend fun syncAll(
        db: AppDatabase,
        baseUrl: String,
        consumerKey: String,
        consumerSecret: String,
        onProgress: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        val api = buildApi(baseUrl, consumerKey, consumerSecret)

        try {
            onProgress("دریافت محصولات از ووکامرس...")
            val products = api.getProducts()
            db.productDao().insertProducts(products.map { mapProduct(it) })
            onProgress("${products.size} محصول دریافت و ذخیره شد")
        } catch (e: Exception) {
            Log.w("WooSync", "Products sync failed: ${e.message}")
            onProgress("دریافت محصولات با مشکل مواجه شد")
        }

        try {
            onProgress("دریافت سفارش‌ها از ووکامرس...")
            val orders = api.getOrders()
            db.orderDao().insertOrders(orders.map { mapOrder(it) })
            onProgress("${orders.size} سفارش دریافت و ذخیره شد")
        } catch (e: Exception) {
            Log.w("WooSync", "Orders sync failed: ${e.message}")
            onProgress("دریافت سفارش‌ها با مشکل مواجه شد")
        }

        try {
            onProgress("دریافت مشتریان از ووکامرس...")
            val customers = api.getCustomers()
            db.customerDao().insertCustomers(customers.map { mapCustomer(it) })
            onProgress("${customers.size} مشتری دریافت و ذخیره شد")
        } catch (e: Exception) {
            Log.w("WooSync", "Customers sync failed: ${e.message}")
            onProgress("دریافت مشتریان با مشکل مواجه شد")
        }

        try {
            onProgress("دریافت کوپن‌ها از ووکامرس...")
            val coupons = api.getCoupons()
            db.couponDao().insertCoupons(coupons.map { mapCoupon(it) })
            onProgress("${coupons.size} کوپن دریافت و ذخیره شد")
        } catch (e: Exception) {
            Log.w("WooSync", "Coupons sync failed: ${e.message}")
            onProgress("دریافت کوپن‌ها با مشکل مواجه شد")
        }
    }
}
