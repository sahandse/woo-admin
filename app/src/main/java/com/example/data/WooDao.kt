package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface StoreDao {
    @Query("SELECT * FROM woo_stores")
    fun getAllStores(): Flow<List<WooStore>>

    @Query("SELECT * FROM woo_stores WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveStore(): WooStore?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStore(store: WooStore)

    @Update
    suspend fun updateStore(store: WooStore)

    @Query("UPDATE woo_stores SET isActive = 0")
    suspend fun deactivateAllStores()

    @Query("UPDATE woo_stores SET isActive = 1 WHERE id = :storeId")
    suspend fun activateStore(storeId: Long)

    @Query("DELETE FROM woo_stores WHERE id = :storeId")
    suspend fun deleteStoreById(storeId: Long)
}

@Dao
interface OrderDao {
    @Query("SELECT * FROM woo_orders ORDER BY id DESC")
    fun getAllOrders(): Flow<List<WooOrder>>

    @Query("SELECT * FROM woo_orders WHERE id = :id")
    suspend fun getOrderById(id: Long): WooOrder?

    @Query("SELECT * FROM woo_orders WHERE orderNumber LIKE '%' || :query || '%' OR customerName LIKE '%' || :query || '%' OR customerPhone LIKE '%' || :query || '%' OR customerEmail LIKE '%' || :query || '%'")
    fun searchOrders(query: String): Flow<List<WooOrder>>

    @Query("SELECT * FROM woo_orders WHERE status = :statusName ORDER BY id DESC")
    fun getOrdersByStatus(statusName: String): Flow<List<WooOrder>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrders(orders: List<WooOrder>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: WooOrder)

    @Query("UPDATE woo_orders SET status = :statusName WHERE id = :orderId")
    suspend fun updateOrderStatus(orderId: Long, statusName: String)

    @Query("UPDATE woo_orders SET shippingStatus = :shippingStatus WHERE id = :orderId")
    suspend fun updateOrderShippingStatus(orderId: Long, shippingStatus: String)

    @Query("UPDATE woo_orders SET isPaid = :isPaid WHERE id = :orderId")
    suspend fun updateOrderPaymentStatus(orderId: Long, isPaid: Boolean)

    @Query("UPDATE woo_orders SET trackingCode = :trackingCode, shippingCompany = :company WHERE id = :orderId")
    suspend fun updateOrderTracking(orderId: Long, trackingCode: String, company: String)

    @Query("UPDATE woo_orders SET adminNotes = :adminNotes WHERE id = :orderId")
    suspend fun updateOrderAdminNotes(orderId: Long, adminNotes: String)

    @Query("SELECT id FROM woo_orders")
    suspend fun getAllOrderIds(): List<Long>

    @Query("DELETE FROM woo_orders")
    suspend fun clearAllOrders()
}

@Dao
interface ProductDao {
    @Query("SELECT * FROM woo_products ORDER BY id DESC")
    fun getAllProducts(): Flow<List<WooProduct>>

    @Query("SELECT * FROM woo_products WHERE id = :id")
    suspend fun getProductById(id: Long): WooProduct?

    @Query("SELECT * FROM woo_products WHERE name LIKE '%' || :query || '%' OR sku LIKE '%' || :query || '%'")
    fun searchProducts(query: String): Flow<List<WooProduct>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<WooProduct>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: WooProduct)

    @Query("UPDATE woo_products SET stockQuantity = :quantity, inStock = :inStock WHERE id = :productId")
    suspend fun updateStock(productId: Long, quantity: Int, inStock: Boolean)

    @Query("UPDATE woo_products SET regularPrice = :regularPrice, salePrice = :salePrice WHERE id = :productId")
    suspend fun updatePrice(productId: Long, regularPrice: Long, salePrice: Long)

    @Query("UPDATE woo_products SET status = :status WHERE id = :productId")
    suspend fun updateProductStatus(productId: Long, status: String)

    @Query("DELETE FROM woo_products WHERE id = :productId")
    suspend fun deleteProduct(productId: Long)

    @Query("DELETE FROM woo_products")
    suspend fun clearAllProducts()
}

@Dao
interface CustomerDao {
    @Query("SELECT * FROM woo_customers ORDER BY id DESC")
    fun getAllCustomers(): Flow<List<WooCustomer>>

    @Query("SELECT * FROM woo_customers WHERE id = :id")
    suspend fun getCustomerById(id: Long): WooCustomer?

    @Query("SELECT * FROM woo_customers WHERE firstName LIKE '%' || :query || '%' OR lastName LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%' OR email LIKE '%' || :query || '%'")
    fun searchCustomers(query: String): Flow<List<WooCustomer>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomers(customers: List<WooCustomer>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: WooCustomer)

    @Query("UPDATE woo_customers SET internalNotes = :notes WHERE id = :customerId")
    suspend fun updateCustomerNotes(customerId: Long, notes: String)

    @Query("DELETE FROM woo_customers")
    suspend fun clearAllCustomers()
}

@Dao
interface CouponDao {
    @Query("SELECT * FROM woo_coupons ORDER BY id DESC")
    fun getAllCoupons(): Flow<List<WooCoupon>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCoupons(coupons: List<WooCoupon>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCoupon(coupon: WooCoupon)

    @Query("UPDATE woo_coupons SET active = :active WHERE id = :couponId")
    suspend fun updateCouponActive(couponId: Long, active: Boolean)

    @Query("DELETE FROM woo_coupons WHERE id = :couponId")
    suspend fun deleteCoupon(couponId: Long)

    @Query("DELETE FROM woo_coupons")
    suspend fun clearAllCoupons()
}

@Dao
interface NotificationDao {
    @Query("SELECT * FROM woo_notifications ORDER BY id DESC")
    fun getAllNotifications(): Flow<List<WooNotification>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: WooNotification)

    @Query("UPDATE woo_notifications SET isRead = 1 WHERE id = :notificationId")
    suspend fun markAsRead(notificationId: Long)

    @Query("DELETE FROM woo_notifications WHERE id = :notificationId")
    suspend fun deleteNotification(notificationId: Long)

    @Query("DELETE FROM woo_notifications")
    suspend fun clearAllNotifications()
}

@Dao
interface AdminUserDao {
    @Query("SELECT * FROM admin_users ORDER BY id DESC")
    fun getAllAdminUsers(): Flow<List<AdminUser>>

    @Query("SELECT * FROM admin_users WHERE username = :username LIMIT 1")
    suspend fun getAdminUserByUsername(username: String): AdminUser?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAdminUser(user: AdminUser)

    @Query("UPDATE admin_users SET isActive = :isActive WHERE id = :id")
    suspend fun updateAdminUserStatus(id: Long, isActive: Boolean)

    @Query("UPDATE admin_users SET role = :role WHERE id = :id")
    suspend fun updateAdminUserRole(id: Long, role: String)

    @Query("DELETE FROM admin_users WHERE id = :id")
    suspend fun deleteAdminUser(id: Long)
}

@Dao
interface AdminActivityDao {
    @Query("SELECT * FROM admin_activities ORDER BY id DESC LIMIT 500")
    fun getAllActivities(): Flow<List<AdminActivity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivity(activity: AdminActivity)
}
