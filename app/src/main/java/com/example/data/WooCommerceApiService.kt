package com.example.data

import okhttp3.Credentials
import okhttp3.Interceptor
import okhttp3.Response
import retrofit2.http.*

// WooCommerce REST API v3 data models aligned with official API
data class WcOrder(
    val id: Long,
    val order_number: String? = null,
    val status: String,
    val date_created: String? = null,
    val date_created_jalali: String? = null,
    val billing: WcBilling,
    val shipping: WcShipping,
    val line_items: List<WcLineItem>,
    val total: String,
    val subtotal: String? = null,
    val discount_total: String? = null,
    val shipping_total: String? = null,
    val payment_method: String? = null,
    val customer_id: Long? = null,
    val customer_note: String? = null,
    val meta_data: List<WcMeta>? = null
)

data class WcBilling(
    val first_name: String,
    val last_name: String,
    val email: String,
    val phone: String,
    val address_1: String,
    val city: String,
    val state: String
)

data class WcShipping(
    val first_name: String,
    val last_name: String,
    val address_1: String,
    val city: String,
    val state: String
)

data class WcLineItem(
    val product_id: Long,
    val name: String,
    val quantity: Int,
    val total: String,
    val image: WcImage? = null
)

data class WcImage(
    val src: String
)

data class WcMeta(
    val key: String,
    val value: String
)

data class WcProduct(
    val id: Long,
    val name: String,
    val slug: String,
    val short_description: String,
    val description: String,
    val regular_price: String,
    val sale_price: String,
    val sku: String,
    val manage_stock: Boolean,
    val stock_quantity: Int,
    val in_stock: Boolean,
    val categories: List<WcCategory>,
    val images: List<WcImage>,
    val status: String,
    val weight: String? = null,
    val dimensions: WcDimensions? = null,
    val featured: Boolean = false
)

data class WcCategory(
    val id: Long,
    val name: String
)

data class WcDimensions(
    val length: String,
    val width: String,
    val height: String
)

data class WcCustomer(
    val id: Long,
    val first_name: String,
    val last_name: String,
    val email: String,
    val username: String,
    val billing: WcBilling,
    val shipping: WcShipping,
    val meta_data: List<WcMeta>? = null
)

data class WcCoupon(
    val id: Long,
    val code: String,
    val discount_type: String,
    val amount: String,
    val date_expires: String? = null,
    val usage_count: Int,
    val status: String
)

interface WooCommerceApiService {

    @GET("wp-json/wc/v3/orders")
    suspend fun getOrders(
        @Header("Authorization") auth: String,
        @Query("per_page") perPage: Int = 20,
        @Query("page") page: Int = 1,
        @Query("status") status: String? = null,
        @Query("after") after: String? = null,
        @Query("before") before: String? = null
    ): List<WcOrder>

    @GET("wp-json/wc/v3/orders/{id}")
    suspend fun getOrder(
        @Header("Authorization") auth: String,
        @Path("id") id: Long
    ): WcOrder

    @PUT("wp-json/wc/v3/orders/{id}")
    suspend fun updateOrder(
        @Header("Authorization") auth: String,
        @Path("id") id: Long,
        @Body order: Map<String, Any>
    ): WcOrder

    @POST("wp-json/wc/v3/orders")
    suspend fun createOrder(
        @Header("Authorization") auth: String,
        @Body order: Map<String, Any>
    ): WcOrder

    @GET("wp-json/wc/v3/products")
    suspend fun getProducts(
        @Header("Authorization") auth: String,
        @Query("per_page") perPage: Int = 20,
        @Query("page") page: Int = 1,
        @Query("status") status: String? = null,
        @Query("category") category: String? = null,
        @Query("search") search: String? = null
    ): List<WcProduct>

    @GET("wp-json/wc/v3/products/{id}")
    suspend fun getProduct(
        @Header("Authorization") auth: String,
        @Path("id") id: Long
    ): WcProduct

    @POST("wp-json/wc/v3/products")
    suspend fun createProduct(
        @Header("Authorization") auth: String,
        @Body product: Map<String, Any>
    ): WcProduct

    @PUT("wp-json/wc/v3/products/{id}")
    suspend fun updateProduct(
        @Header("Authorization") auth: String,
        @Path("id") id: Long,
        @Body product: Map<String, Any>
    ): WcProduct

    @DELETE("wp-json/wc/v3/products/{id}")
    suspend fun deleteProduct(
        @Header("Authorization") auth: String,
        @Path("id") id: Long,
        @Query("force") force: Boolean = true
    ): WcProduct

    @GET("wp-json/wc/v3/customers")
    suspend fun getCustomers(
        @Header("Authorization") auth: String,
        @Query("per_page") perPage: Int = 20,
        @Query("page") page: Int = 1,
        @Query("email") email: String? = null
    ): List<WcCustomer>

    @GET("wp-json/wc/v3/customers/{id}")
    suspend fun getCustomer(
        @Header("Authorization") auth: String,
        @Path("id") id: Long
    ): WcCustomer

    @GET("wp-json/wc/v3/coupons")
    suspend fun getCoupons(
        @Header("Authorization") auth: String,
        @Query("per_page") perPage: Int = 20,
        @Query("page") page: Int = 1
    ): List<WcCoupon>

    @POST("wp-json/wc/v3/coupons")
    suspend fun createCoupon(
        @Header("Authorization") auth: String,
        @Body coupon: Map<String, Any>
    ): WcCoupon

    @DELETE("wp-json/wc/v3/coupons/{id}")
    suspend fun deleteCoupon(
        @Header("Authorization") auth: String,
        @Path("id") id: Long,
        @Query("force") force: Boolean = true
    ): WcCoupon

    @GET("wp-json/wc/v3/reports/sales")
    suspend fun getSalesReport(
        @Header("Authorization") auth: String,
        @Query("period") period: String = "week"
    ): WcSalesReport
}

data class WcSalesReport(
    val totals: WcSalesTotals
)

data class WcSalesTotals(
    val total_sales: String,
    val net_sales: String,
    val average_sales: String,
    val total_orders: Int,
    val total_items: Int,
    val total_tax: String,
    val total_shipping: String,
    val total_refunds: String,
    val total_discount: String
)

// Basic Auth interceptor for WooCommerce REST API
class BasicAuthInterceptor(
    private val consumerKey: String,
    private val consumerSecret: String
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val credential = Credentials.basic(consumerKey, consumerSecret)
        val request = chain.request().newBuilder()
            .header("Authorization", credential)
            .build()
        return chain.proceed(request)
    }
}
