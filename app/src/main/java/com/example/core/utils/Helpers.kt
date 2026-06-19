package com.example.core.utils

import com.example.data.*
import java.text.DecimalFormat

object Helpers {

    // Converts English numerals to Persian digits
    fun toPersianDigits(input: String): String {
        return input
            .replace('0', '۰')
            .replace('1', '۱')
            .replace('2', '۲')
            .replace('3', '۳')
            .replace('4', '۴')
            .replace('5', '۵')
            .replace('6', '۶')
            .replace('7', '۷')
            .replace('8', '۸')
            .replace('9', '۹')
    }

    fun toPersianDigits(input: Number): String {
        return toPersianDigits(input.toString())
    }

    // Formats price cleanly to Toman with Persian digits (e.g. "۱۲,۵۰۰,۰۰۰ تومان")
    fun formatPrice(amount: Long): String {
        val formatter = DecimalFormat("#,###")
        val formatted = formatter.format(amount)
        return toPersianDigits("$formatted تومان")
    }

    // List of initial mock entities to populate DB if empty
    val mockStores = listOf(
        WooStore(
            id = 1,
            name = "فروشگاه مرکزی مد و پوشاک",
            url = "https://example-fashion.ir",
            consumerKey = "ck_fashion_custom_key_placeholder",
            consumerSecret = "cs_fashion_custom_secret_placeholder",
            isActive = true
        ),
        WooStore(
            id = 2,
            name = "شعبه دیجیتال و موبایل",
            url = "https://example-digital.ir",
            consumerKey = "ck_digital_custom_key_placeholder",
            consumerSecret = "cs_digital_custom_secret_placeholder",
            isActive = false
        )
    )

    val mockProducts = listOf(
        WooProduct(
            id = 101,
            name = "گوشی موبایل سامسونگ Galaxy S26 Ultra",
            slug = "samsung-galaxy-s26-ultra",
            shortDescription = "پرچمدار جدید سامسونگ با دوربین ۲۰۰ مگاپیکسلی و تراشه قدرتمند اگزینوس ۲۶۰۰",
            description = "نسل جدید گوشی‌های هوشمند سری اولترا سامسونگ با طراحی ارگونومیک، قلم S-Pen اختصاصی، صفحه نمایش LTPO AMOLED 2X و حافظه ۵۱۲ گیگابایت به همراه ۱۲ گیگابایت رم. این دستگاه مجهز به باتری ۵۵۰۰ میلی‌آمپر ساعتی با شارژ سریع ۴۵ واتی است.",
            regularPrice = 75000000,
            salePrice = 72900000,
            isPromoActive = true,
            promoStartJalali = "۱۴۰۵/۰۳/۰۱",
            promoEndJalali = "۱۴۰۵/۰۴/۰۱",
            sku = "SAM-S26U-512G",
            manageStock = true,
            stockQuantity = 8,
            inStock = true,
            lowStockThreshold = 3,
            weight = 233.0,
            length = 16.3,
            width = 7.9,
            height = 0.8,
            categories = "گوشی هوشمند, سامسونگ, لوازم دیجیتال",
            tags = "موبایل, پرچمدار, سامسونگ, اس ۲۶ اولترا",
            mainImage = "https://images.unsplash.com/photo-1610945265064-0e34e5519bbf?auto=format&fit=crop&w=600&q=80",
            galleryImages = listOf(
                "https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?auto=format&fit=crop&w=400&q=80",
                "https://images.unsplash.com/photo-1598327105666-5b89351aff97?auto=format&fit=crop&w=400&q=80"
            ),
            isFeatured = true,
            colors = listOf("مشکی فانتوم", "خاکستری تیتانیوم", "سبز جنگلی"),
            sizes = emptyList(),
            warehouseNote = "قفسه دیجیتال - طبقه دوم - ردیف ۳"
        ),
        WooProduct(
            id = 102,
            name = "کفش ورزشی مردانه نایک پگاسوس ۴۱",
            slug = "nike-pegasus-41-shoes",
            shortDescription = "کفش دویدن بسیار راحت و مقاوم با تکنولوژی لایه‌گذاری Zoom Air نایک",
            description = "کفش ورزشی مخصوص پیاده‌روی و دویدن سرعتی کارآمد برای تمامی ورزشکاران. طراحی تنفس‌پذیر توری رویه و کفی نرم که فشارهای وارده به ستون فقرات را کاهش می‌دهد.",
            regularPrice = 6800000,
            salePrice = 5900000,
            isPromoActive = true,
            promoStartJalali = "۱۴۰۵/۰۳/۱۵",
            promoEndJalali = "۱۴۰۵/۰۳/۳۰",
            sku = "NIKE-PEG41-M",
            manageStock = true,
            stockQuantity = 2,
            inStock = true,
            lowStockThreshold = 4,
            weight = 280.0,
            length = 31.0,
            width = 12.0,
            height = 11.0,
            categories = "کفش ورزشی, ملزومات دویدن, نایک",
            tags = "کتانی, نایک, ورزشی, دویدن",
            mainImage = "https://images.unsplash.com/photo-1542291026-7eec264c27ff?auto=format&fit=crop&w=600&q=80",
            galleryImages = emptyList(),
            isFeatured = false,
            colors = listOf("قرمز پررنگ", "سیاه کلاسیک"),
            sizes = listOf("۴۱", "۴۲", "۴۳", "۴۴"),
            warehouseNote = "بخش کفش - قفسه نایک - طبقه ۱"
        ),
        WooProduct(
            id = 103,
            name = "ساعت هوشمند اپل واچ سری ۱۰",
            slug = "apple-watch-series-10",
            shortDescription = "جدیدترین ساعت هوشمند اپل با سنسور تشخیص دمای بدن و اکسیژن خون",
            description = "مقاومت کامل در برابر آب تا عمق ۵۰ متر، پایش دقیق سلامتی و خواب، صفحه نمایش همیشه روشن اولد و باتری بهینه‌تر با قابلیت شارژ سریع بی‌سیم.",
            regularPrice = 24500000,
            salePrice = 0,
            isPromoActive = false,
            sku = "APL-W10-45M",
            manageStock = true,
            stockQuantity = 15,
            inStock = true,
            lowStockThreshold = 5,
            weight = 39.0,
            length = 4.5,
            width = 3.8,
            height = 1.0,
            categories = "ساعت هوشمند, لوازم دیجیتال, اپل",
            tags = "ساعت, هوشمند, اپل, گجت",
            mainImage = "https://images.unsplash.com/photo-1508685096489-7aacd43bd3b1?auto=format&fit=crop&w=600&q=80",
            galleryImages = emptyList(),
            isFeatured = true,
            colors = listOf("نقره‌ای تیتانیومی", "مشکی جت"),
            sizes = listOf("۴۵ میلی‌متری"),
            warehouseNote = "کمد شیشه‌ای امنیتی - کشو B"
        ),
        WooProduct(
            id = 104,
            name = "کتاب صوتی قهرمان درون اثر فرخزاد",
            slug = "hero-within-audio-book",
            shortDescription = "راهنمای خودشناسی عمیق و بیداری نیروهای نهفته روانشناسی",
            description = "نسخه دانلودی صوتی با کیفیت بالا و صدای گوینده مطرح کشوری. شامل ۱۰ تست تحلیل که در فهم الگوهای شخصیت به شما کمک فراوانی خواهد کرد.",
            regularPrice = 120000,
            salePrice = 90000,
            isPromoActive = true,
            sku = "BOOK-HEROW-AUDIO",
            manageStock = false,
            stockQuantity = 0,
            inStock = true,
            lowStockThreshold = 0,
            categories = "کتاب صوتی, دیجیتال, رشد فردی",
            tags = "کتاب, صوتی, رشد شخصی",
            mainImage = "https://images.unsplash.com/photo-1543002588-bfa74002ed7e?auto=format&fit=crop&w=600&q=80",
            isVirtual = true,
            isDownloadable = true,
            warehouseNote = "مجازی - نیاز به انبار ندارد"
        ),
        WooProduct(
            id = 105,
            name = "هدفون بی‌سیم سونی WH-1000XM5",
            slug = "sony-wh1000xm5-headset",
            shortDescription = "بهترین هدفون حذف نویز فعال جهان به همراه پایداری بی نظیر باتری",
            description = "مجهز به سیستم هوشمند نویز کنسلینگ پیشرفته مایل، عمر باتری طولانی ۳۰ ساعته، پشتیبانی از تماس‌های فوق‌العاده شفاف و قابلیت کنترل صوتی بی نقص الکسا.",
            regularPrice = 18900000,
            salePrice = 0,
            isPromoActive = false,
            sku = "SON-WH1000XM5",
            manageStock = true,
            stockQuantity = 0, // Out of stock to test filter
            inStock = false,
            lowStockThreshold = 2,
            weight = 250.0,
            length = 22.0,
            width = 18.0,
            height = 8.0,
            categories = "لوازم صوتی, لوازم دیجیتال, سونی",
            tags = "هدفون, سونی, موسیقی, بی‌سیم",
            mainImage = "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?auto=format&fit=crop&w=600&q=80",
            warehouseNote = "قفسه صوتی - طبقه ۳"
        )
    )

    val mockOrders = listOf(
        WooOrder(
            id = 2001,
            orderNumber = "W-14820",
            status = "PROCESSING",
            createdAtJalali = "۱۴۰۵/۰۳/۱۸",
            createdAtTime = "۱۰:۱۵",
            customerName = "علیرضا احمدی",
            customerPhone = "۰۹۱۲۳۴۵۶۷۸۹",
            customerEmail = "alireza.ae@gmail.com",
            billingAddress = "تهران، خیابان ولیعصر، کوچه سعید، پلاک ۱۲، واحد ۳",
            shippingAddress = "تهران، خیابان ولیعصر، کوچه سعید، پلاک ۱۲، واحد ۳",
            city = "تهران",
            province = "تهران",
            postalCode = "۱۴۳۵۶۷۸۹۰۱",
            customerNote = "لطفاً قبل از ارسال تماس گرفته شود و ترجیحاً در ساعات بعد از ظهر ارسال کنید.",
            items = listOf(
                OrderItem(
                    productId = 101,
                    productName = "گوشی موبایل سامسونگ Galaxy S26 Ultra",
                    quantity = 1,
                    unitPrice = 72900000,
                    totalPrice = 72900000,
                    image = "https://images.unsplash.com/photo-1610945265064-0e34e5519bbf?auto=format&fit=crop&w=100&q=80"
                )
            ),
            subtotal = 75000000,
            discount = 2100000,
            shippingCost = 45000,
            totalAmount = 72945000,
            paymentMethod = "درگاه مستقیم پاسارگاد",
            isPaid = true,
            shippingStatus = "READY_TO_PACK"
        ),
        WooOrder(
            id = 2002,
            orderNumber = "W-14819",
            status = "PENDING_PAYMENT",
            createdAtJalali = "۱۴۰۵/۰۳/۱۸",
            createdAtTime = "۰۸:۴۰",
            customerName = "مریم رجبی",
            customerPhone = "۰۹۱۷۶۵۴۳۲۱۰",
            customerEmail = "maryam.rajabi@yahoo.com",
            billingAddress = "شیراز، بلوار معالی آباد، مجتمع آرین، بلوک ب، واحد ۴",
            shippingAddress = "شیراز، بلوار معالی آباد، مجتمع آرین، بلوک ب، واحد ۴",
            city = "شیراز",
            province = "فارس",
            postalCode = "۷۱۸۹۵۴۳۲۱۱",
            customerNote = "کوپن ارسال رایگان اعمال نشده است، بررسی بفرمایید.",
            items = listOf(
                OrderItem(
                    productId = 102,
                    productName = "کفش ورزشی مردانه نایک پگاسوس ۴۱",
                    quantity = 2,
                    unitPrice = 5900000,
                    totalPrice = 11800000,
                    image = "https://images.unsplash.com/photo-1542291026-7eec264c27ff?auto=format&fit=crop&w=100&q=80"
                )
            ),
            subtotal = 13600000,
            discount = 1800000,
            shippingCost = 50000,
            totalAmount = 11850000,
            paymentMethod = "پورت کارت به کارت",
            isPaid = false,
            shippingStatus = "READY_TO_PACK"
        ),
        WooOrder(
            id = 2003,
            orderNumber = "W-14815",
            status = "COMPLETED",
            createdAtJalali = "۱۴۰۵/۰۳/۱۵",
            createdAtTime = "۱۶:۳۰",
            customerName = "محمد رضایی",
            customerPhone = "۰۹۳۵۴۴۴۵۵۶۶",
            customerEmail = "m.rezaei@outlook.com",
            billingAddress = "اصفهان، خیابان خاقانی، مجتمع پارسیان، واحد ۲",
            shippingAddress = "اصفهان، خیابان خاقانی، مجتمع پارسیان، واحد ۲",
            city = "اصفهان",
            province = "اصفهان",
            postalCode = "۸۱۳۹۶۵۴۷۸۹",
            customerNote = "بسته‌بندی هدیه باشد.",
            items = listOf(
                OrderItem(
                    productId = 103,
                    productName = "ساعت هوشمند اپل واچ سری ۱۰",
                    quantity = 1,
                    unitPrice = 24500000,
                    totalPrice = 24500000,
                    image = "https://images.unsplash.com/photo-1508685096489-7aacd43bd3b1?auto=format&fit=crop&w=100&q=80"
                )
            ),
            subtotal = 24500000,
            discount = 0,
            shippingCost = 0, // Free shipping
            totalAmount = 24500000,
            paymentMethod = "درگاه مستقیم سامان",
            isPaid = true,
            trackingCode = "1098402581023",
            shippingCompany = "تیپاکس",
            shippingStatus = "DELIVERED"
        ),
        WooOrder(
            id = 2004,
            orderNumber = "W-14812",
            status = "CANCELLED",
            createdAtJalali = "۱۴۰۵/۰۳/۱۴",
            createdAtTime = "۱۱:۰۲",
            customerName = "سارا کریمی",
            customerPhone = "۰۹۰۲۹۹۹۸۸۷۷",
            customerEmail = "sara.karimi@gmail.com",
            billingAddress = "تبریز، خیابان امام، روبروی ارک، بن‌بست پگاه، پلاک ۵",
            shippingAddress = "تبریز، خیابان امام، روبروی ارک، بن‌بست پگاه، پلاک ۵",
            city = "تبریز",
            province = "آذربایجان شرقی",
            postalCode = "۵۱۳۵۶۲۴۷۸۹",
            customerNote = "",
            items = listOf(
                OrderItem(
                    productId = 104,
                    productName = "کتاب صوتی قهرمان درون اثر فرخزاد",
                    quantity = 1,
                    unitPrice = 90000,
                    totalPrice = 90000,
                    image = "https://images.unsplash.com/photo-1543002588-bfa74002ed7e?auto=format&fit=crop&w=100&q=80"
                )
            ),
            subtotal = 120000,
            discount = 30000,
            shippingCost = 0,
            totalAmount = 90000,
            paymentMethod = "کیف پول اعتباری",
            isPaid = false,
            shippingStatus = "READY_TO_PACK"
        ),
        WooOrder(
            id = 2005,
            orderNumber = "W-14800",
            status = "NEEDS_FOLLOWUP",
            createdAtJalali = "۱۴۰۵/۰۳/۱۰",
            createdAtTime = "۱۴:۲۰",
            customerName = "پوریا قاسمی",
            customerPhone = "۰۹۱۵۱۱۱۴۴۵۵",
            customerEmail = "pooriya.ghasemi@it.com",
            billingAddress = "مشهد، بلوار سجاد، خیابان بزرگمهر، پلاک ۲",
            shippingAddress = "مشهد، بلوار سجاد، خیابان بزرگمهر、 پلاک ۲",
            city = "مشهد",
            province = "خراسان رضوی",
            postalCode = "۹۱۸۳۶۵۴۷۲۹",
            customerNote = "هزینه ارسال خیلی زیاد ثبت گردیده است. تماس حاصل فرمایید.",
            items = listOf(
                OrderItem(
                    productId = 102,
                    productName = "کفش ورزشی مردانه نایک پگاسوس ۴۱",
                    quantity = 1,
                    unitPrice = 5900000,
                    totalPrice = 5900000,
                    image = "https://images.unsplash.com/photo-1542291026-7eec264c27ff?auto=format&fit=crop&w=100&q=80"
                )
            ),
            subtotal = 6800000,
            discount = 900000,
            shippingCost = 120000,
            totalAmount = 6120000,
            paymentMethod = "درگاه مستقیم بانک سپه",
            isPaid = true,
            shippingStatus = "READY_TO_PACK",
            adminNotes = "مشتری درخواست تخفیف هزینه ارسال دارد."
        )
    )

    val mockCustomers = listOf(
        WooCustomer(
            id = 301,
            firstName = "علیرضا",
            lastName = "احمدی",
            phone = "۰۹۱۲۳۴۵۶۷۸۹",
            email = "alireza.ae@gmail.com",
            registeredDateJalali = "۱۴۰۲/۰۵/۲۴",
            ordersCount = 14,
            totalSpent = 124500000,
            lastPurchaseDateJalali = "۱۴۰۵/۰۳/۱۸",
            averageOrderValue = 8892000,
            billingAddress = "تهران، خیابان ولیعصر، کوچه سعید، پلاک ۱۲، واحد ۳",
            shippingAddress = "تهران، خیابان ولیعصر، کوچه سعید، پلاک ۱۲، واحد ۳",
            category = "LOYAL"
        ),
        WooCustomer(
            id = 302,
            firstName = "مریم",
            lastName = "رجبی",
            phone = "۰۹۱۷۶۵۴۳۲۱۰",
            email = "maryam.rajabi@yahoo.com",
            registeredDateJalali = "۱۴۰۴/۱۲/۰۱",
            ordersCount = 1,
            totalSpent = 11850000,
            lastPurchaseDateJalali = "۱۴۰۵/۰۳/۱۸",
            averageOrderValue = 11850000,
            billingAddress = "شیراز، بلوار معالی آباد، مجتمع آرین، بلوک ب، واحد ۴",
            shippingAddress = "شیراز، بلوار معالی آباد، مجتمع آرین، بلوک ب، واحد ۴",
            category = "NEW"
        ),
        WooCustomer(
            id = 303,
            firstName = "محمد",
            lastName = "رضایی",
            phone = "۰۹۳۵۴۴۴۵۵۶۶",
            email = "m.rezaei@outlook.com",
            registeredDateJalali = "۱۴۰۱/۰۱/۱۰",
            ordersCount = 38,
            totalSpent = 485100000,
            lastPurchaseDateJalali = "۱۴۰۵/۰۳/۱۵",
            averageOrderValue = 12765000,
            billingAddress = "اصفهان، خیابان خاقانی، مجتمع پارسیان، واحد ۲",
            shippingAddress = "اصفهان، خیابان خاقانی، مجتمع پارسیان، واحد ۲",
            category = "HIGH_VALUE",
            internalNotes = "خرید فوق‌العاده بالایی دارد. حتماً در کدهای تخفیف دوره‌ای ایمیل و پیامک شود."
        ),
        WooCustomer(
            id = 304,
            firstName = "سارا",
            lastName = "کریمی",
            phone = "۰۹029998877",
            email = "sara.karimi@gmail.com",
            registeredDateJalali = "۱۴۰۳/۰۴/۱۷",
            ordersCount = 5,
            totalSpent = 4800000,
            lastPurchaseDateJalali = "۱۴۰۵/۰۳/۱۴",
            averageOrderValue = 960000,
            billingAddress = "تبریز، خیابان امام، روبروی ارک، بن‌بست پگاه، پلاک ۵",
            shippingAddress = "تبریز، خیابان امام، روبروی ارک، بن‌بست پگاه، پلاک ۵",
            category = "REGULAR"
        ),
        WooCustomer(
            id = 305,
            firstName = "حسین",
            lastName = "رستمی",
            phone = "۰۹۱۹۸۸۸۷۷۶۶",
            email = "h.rostami@company.com",
            registeredDateJalali = "۱۴۰۰/۱۰/۰۵",
            ordersCount = 0,
            totalSpent = 0,
            lastPurchaseDateJalali = "هیچ",
            averageOrderValue = 0,
            billingAddress = "اهواز، زیتون کارمندی، خیابان فاطمی، پلاک ۴",
            shippingAddress = "اهواز، زیتون کارمندی، خیابان فاطمی، پلاک ۴",
            category = "INACTIVE",
            internalNotes = "بیش از ۲ سال حضور در وبسایت، هنوز خریدی نداشته است. کوپن تشویقی ۱۰۰ هزار تومانی پیامک گردد."
        )
    )

    val mockCoupons = listOf(
        WooCoupon(
            id = 401,
            code = "SPRING1405",
            discountType = "percent",
            amount = 15,
            minSpend = 500000,
            maxDiscount = 300000,
            startJalali = "۱۴۰۵/۰۱/۰۱",
            expiryJalali = "۱۴۰۵/۰۳/۳۱",
            usageLimit = 100,
            usageLimitPerUser = 1,
            usedCount = 42,
            active = true
        ),
        WooCoupon(
            id = 402,
            code = "FREE_DELIVERY",
            discountType = "fixed_cart",
            amount = 0,
            minSpend = 2000000,
            startJalali = "۱۴۰۵/۰۲/۱۵",
            expiryJalali = "۱۴۰۵/۰۵/۱۵",
            usageLimit = 1000,
            usedCount = 288,
            active = true,
            freeShipping = true
        ),
        WooCoupon(
            id = 403,
            code = "YALDA_OFFER",
            discountType = "fixed_cart",
            amount = 150000,
            minSpend = 1000000,
            startJalali = "۱۴۰۴/۰۹/۲۵",
            expiryJalali = "۱۴۰۴/۱۰/۰۵",
            usageLimit = 150,
            usedCount = 150,
            active = false // Expired
        )
    )

    val mockNotifications = listOf(
        WooNotification(
            id = 1,
            title = "سفارش جدید ثبت شد!",
            body = "سفارش شماره W-14820 متعلق به علیرضا احمدی با مبلغ ۷۲,۹۴۵,۰۰۰ تومان نیاز به بررسی دارد.",
            type = "NEW_ORDER",
            dateJalali = "۱۴۰۵/۰۳/۱۸",
            isRead = false,
            linkedId = 2001
        ),
        WooNotification(
            id = 2,
            title = "هشدار موجودی بسیار کم!",
            body = "محصول «کفش ورزشی مردانه نایک پگاسوس ۴۱» به موجودی خطرساز ۲ عدد رسید. لطفاً شارژ کنید.",
            type = "LOW_STOCK",
            dateJalali = "۱۴۰۵/۰۳/۱۸",
            isRead = false,
            linkedId = 102
        ),
        WooNotification(
            id = 3,
            title = "مشتری جدید ثبت‌نام کرد",
            body = "مشتری «پوریا قاسمی» با شماره تماس ۰۹۱۵۱۱۱۴۴۵۵ به خانواده فروشگاه پیوست.",
            type = "NEW_CUSTOMER",
            dateJalali = "۱۴۰۵/۰۳/۱۰",
            isRead = true,
            linkedId = 302
        )
    )

    val mockAdminUsers = listOf(
        AdminUser(
            id = 1,
            fullName = "مدیر سیستم (توسعه)",
            username = "admin",
            role = "SUPER_ADMIN",
            isActive = true,
            lastLoginJalali = "۱۴۰۵/۰۳/۱۸",
            permissions = listOf("ALL")
        ),
        AdminUser(
            id = 2,
            fullName = "آرش بهرامی",
            username = "arash_store",
            role = "STORE_ADMIN",
            isActive = true,
            lastLoginJalali = "۱۴۰۵/۰۳/۱۷",
            permissions = listOf("MANAGE_ORDERS", "MANAGE_PRODUCTS", "MANAGE_CUSTOMERS")
        ),
        AdminUser(
            id = 3,
            fullName = "سجاد انباردار",
            username = "sajad_pack",
            role = "STOCK_CLERK",
            isActive = true,
            lastLoginJalali = "۱۴۰۵/۰۳/۱۸",
            permissions = listOf("VIEW_PRODUCTS", "UPDATE_STOCK", "SHIPMENT_STATUS")
        )
    )

    val mockActivities = listOf(
        AdminActivity(
            id = 1,
            adminName = "مدیر سیستم (توسعه)",
            adminRole = "SUPER_ADMIN",
            actionType = "LOGIN",
            details = "ورود موفق به سیستم مدیریت ووکامرس",
            timestampJalali = "۱۴۰۵/۰۳/۱۸ - ۱۰:۰۰"
        ),
        AdminActivity(
            id = 2,
            adminName = "آرش بهرامی",
            adminRole = "STORE_ADMIN",
            actionType = "EDIT_PRICE",
            details = "قیمت حراجی محصول «کفش ورزشی مردانه نایک پگاسوس ۴۱» را به ۵,۹۰۰,۰۰۰ تومان تغییر داد.",
            timestampJalali = "۱۴۰۵/۰۳/۱۷ - ۱۷:۴۵"
        ),
        AdminActivity(
            id = 3,
            adminName = "سجاد انباردار",
            adminRole = "STOCK_CLERK",
            actionType = "EDIT_STOCK",
            details = "موجودی انبار «ساعت هوشمند اپل واچ سری ۱۰» به ۱۵ عدد تغییر یافت.",
            timestampJalali = "۱۴۰۵/۰3/۱۸ - ۰۹:۱۵"
        )
    )
}
