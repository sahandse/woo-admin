package com.sahand.wooadmin.data

import retrofit2.http.*

// Request & Response bodies for API connection
data class LoginRequest(val username: String, val password: String)
data class LoginResponse(val token: String, val user: ApiAdminUser?)

data class ApiAdminUser(
    val id: Long,
    val fullName: String,
    val username: String,
    val role: String,
    val isActive: Boolean
)

data class DashboardSummaryResponse(
    val salesToday: Long,
    val salesYesterday: Long,
    val salesThisWeek: Long,
    val salesThisMonth: Long,
    val newOrdersCount: Int,
    val processingOrdersCount: Int,
    val completedOrdersCount: Int,
    val cancelledOrdersCount: Int,
    val refundedOrdersCount: Int,
    val lowStockCount: Int,
    val outOfStockCount: Int,
    val newCustomersCount: Int,
    val averageOrderValue: Long,
    val totalDiscounts: Long,
    val shippingFeesToday: Long,
    val salesNet: Long,
    val salesGross: Long
)

data class StatusUpdateRequest(val status: String)
data class TrackingUpdateRequest(val trackingCode: String, val shippingCompany: String)
data class StockUpdateRequest(val quantity: Int, val inStock: Boolean)
data class PriceUpdateRequest(val regularPrice: Long, val salePrice: Long)
data class NoteRequest(val note: String, val isCustomerNote: Boolean)

// Response for Gemini service running on NodeJS Backend (optional backend pass-through)
data class AiAnalysisResponse(val result: String)

interface WooApiService {

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @GET("api/dashboard/summary")
    suspend fun getDashboardSummary(@Header("Authorization") token: String): DashboardSummaryResponse

    @GET("api/orders")
    suspend fun getOrders(@Header("Authorization") token: String): List<WooOrder>

    @PUT("api/orders/{id}/status")
    suspend fun updateOrderStatus(
        @Header("Authorization") token: String,
        @Path("id") id: Long,
        @Body request: StatusUpdateRequest
    ): WooOrder

    @POST("api/orders/{id}/tracking")
    suspend fun updateOrderTracking(
        @Header("Authorization") token: String,
        @Path("id") id: Long,
        @Body request: TrackingUpdateRequest
    ): WooOrder

    @POST("api/orders/{id}/notes")
    suspend fun addOrderNote(
        @Header("Authorization") token: String,
        @Path("id") id: Long,
        @Body request: NoteRequest
    ): WooOrder

    @GET("api/products")
    suspend fun getProducts(@Header("Authorization") token: String): List<WooProduct>

    @POST("api/products")
    suspend fun createProduct(@Header("Authorization") token: String, @Body product: WooProduct): WooProduct

    @PUT("api/products/{id}")
    suspend fun updateProduct(@Header("Authorization") token: String, @Path("id") id: Long, @Body product: WooProduct): WooProduct

    @DELETE("api/products/{id}")
    suspend fun deleteProduct(@Header("Authorization") token: String, @Path("id") id: Long): Map<String, Boolean>

    @PUT("api/products/{id}/stock")
    suspend fun updateStock(
        @Header("Authorization") token: String,
        @Path("id") id: Long,
        @Body request: StockUpdateRequest
    ): WooProduct

    @PUT("api/products/{id}/price")
    suspend fun updatePrice(
        @Header("Authorization") token: String,
        @Path("id") id: Long,
        @Body request: PriceUpdateRequest
    ): WooProduct

    @GET("api/customers")
    suspend fun getCustomers(@Header("Authorization") token: String): List<WooCustomer>

    @GET("api/coupons")
    suspend fun getCoupons(@Header("Authorization") token: String): List<WooCoupon>

    @POST("api/ai/{task}")
    suspend fun getAiSummary(
        @Header("Authorization") token: String,
        @Path("task") task: String,
        @Body context: Map<String, String>
    ): AiAnalysisResponse
}
