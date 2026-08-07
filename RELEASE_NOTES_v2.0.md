# WooCommerce Manager v2.0 - Release Notes

## 🎉 نسخه 2.0 - WooCommerce-like Transformation

این نسخه تبدیل کامل برنامه به سبک اپ اصلی ووکامرس است.

## ✨ ویژگی‌های جدید

### 🔄 API هوشمند ووکامرس
- اتصال مستقیم به WooCommerce REST API v3
- احراز هویت امن با کلیدهای مصرف‌کننده (Consumer Key/Secret)
- رمزگذاری کلیدهای API با EncryptedSharedPreferences
- همگام‌سازی خودکار داده‌ها با سرور

### 🎨 طراحی مینیمال ووکامرس
- پالت رنگی الهام گرفته از WooCommerce (بنفش، سبز، آبی)
- حالت تاریک و روشن با پشتیبانی کامل
- فونت‌های فارسی وزیر و ایران سنس
- کارت‌های ساده و تمیز با گوشه‌های گرد

### 📱 ناوبری بازطراحی شده
- حذف تب «هوشمند» از نوار پایین
- اضافه شدن تب «بیشتر» برای دسترسی به تنظیمات
- صفحه متمرکز «بیشتر» برای مدیریت فروشگاه

### 📦 مدیریت سفارشات
- کارت‌های سفارش ساده و خوانا
- نمایش سریع وضعیت پرداخت و ارسال
- فیلترهای وضعیت سفارش

### 🛍️ مدیریت محصولات
- کارت‌های محصول مینیمال با عکس، قیمت و موجودی
- دکمه‌های سریع برای ویرایش موجودی و قیمت
- نمایش وضعیت stocked با رنگ‌بندی

### 👥 مدیریت مشتریان
- لیست مشتریان با فیلتر دسته‌بندی
- صفحه جزئیات مشتری با آمار خرید
- امکان تماس تلفنی و واتساپ

### ⚙️ تنظیمات
- اتصال امن به فروشگاه ووکامرس
- مدیریت فروشگاه‌های چندگانه
- تنظیمات پیامک (ملی‌پیامک) در تنظیمات
- خروج ایمن از حساب

## 🗑️ ویژگی‌های حذف شده

- هوش مصنوعی (AI Analyst) - برای ساده‌سازی برنامه
- نمودار سفارشی Recharts - جایگزین با کارت‌های آمار ساده
- پنل شبیه‌ساز سفارش - ابزار توسعه‌دهنده حذف شد
- دکمه شناور ارسال پیامک از صفحه مشتریان - منتقل به تنظیمات

## 🔧 بهبودهای فنی

- اضافه شدن `WooCommerceApiService.kt` برای REST API
- اضافه شدن `WooCommerceAuthenticator.kt` برای مدیریت امن کلیدها
- اضافه شدن `RetrofitInstance.kt` برای پیکربندی شبکه
- اضافه شدن `MoreScreen.kt` به عنوان صفحه متمرکز
- به‌روزرسانی `WooRepository.kt` با قابلیت همگام‌سازی
- ساده‌سازی `DashboardScreen` - حذف نمودارهای پیچیده
- بهبود امنیت با وابستگی `androidx.security.crypto`

## 📋 تغییرات فایل‌ها

### فایل‌های جدید:
- `app/src/main/java/com/example/data/WooCommerceApiService.kt`
- `app/src/main/java/com/example/data/WooCommerceAuthenticator.kt`
- `app/src/main/java/com/example/data/RetrofitInstance.kt`
- `app/src/main/java/com/example/ui/screens/MoreScreen.kt`
- `app/src/main/res/font/vazir_font_family.xml`
- `app/src/main/res/font/iransans_font_family.xml`

### فایل‌های به‌روز شده:
- `app/build.gradle.kts` - نسخه 2.0، اضافه شدن وابستگی‌های جدید
- `app/src/main/java/com/example/ui/theme/Color.kt` - پالت رنگی ووکامرس
- `app/src/main/java/com/example/ui/theme/Theme.kt` - تم تاریک/روشن
- `app/src/main/java/com/example/ui/theme/Type.kt` - تنظیمات فونت
- `app/src/main/java/com/example/ui/screens/MainAppContainer.kt` - ناوبری جدید
- `app/src/main/java/com/example/ui/screens/AuxiliaryScreens.kt` - داشبورد ساده
- `app/src/main/java/com/example/ui/screens/LoginScreen.kt` - رابط ورود ساده
- `app/src/main/java/com/example/ui/screens/OrdersScreens.kt` - کارت سفارش ساده
- `app/src/main/java/com/example/ui/screens/ProductsScreens.kt` - کارت محصول ساده
- `app/src/main/java/com/example/ui/screens/CustomerScreens.kt` - حذف FAB پیامک
- `app/src/main/java/com/example/ui/screens/SettingsScreen.kt` - اضافه شدن تنظیمات SMS
- `app/src/main/java/com/example/ui/viewmodel/WooViewModel.kt` - حذف AI، اضافه sync
- `app/src/main/java/com/example/data/WooRepository.kt` - اضافه شدن متدهای همگام‌سازی
- `app/src/main/java/com/example/data/DataModels.kt` - تبدیل‌گرهای WC API
- `app/proguard-rules.pro` - قوانین Release

## 🚀 نصب و راه‌اندازی

1. فایل `my-upload-key.jks` را در ریشه پروژه قرار دهید
2. متغیرهای محیطی زیر را تنظیم کنید:
   - `KEYSTORE_PATH`: مسیر فایل keystore
   - `STORE_PASSWORD`: رمز keystore
   - `KEY_PASSWORD`: رمز کلید
   - `KEY_ALIAS`: نام کلید (پیش‌فرض: upload)
3. دستور زیر را اجرا کنید:
   ```bash
   ./gradlew assembleRelease
   ```

## 📱 مشخصات برنامه

- **Package**: com.aistudio.woopanel.qshvzw
- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 36
- **Version Code**: 2
- **Version Name**: 2.0
- **Compile SDK**: 36

## 🔐 امنیت

- کلیدهای API با EncryptedSharedPreferences ذخیره می‌شوند
- پروتکل HTTPS برای ارتباط با سرور الزامی است
- ProGuard/R8 برای محافظت از کد فعال است

## 📝 توجه

- برای استفاده از ویژگی‌های آنلاین، نیاز به کلیدهای API ووکامرس دارید
- حالت آزمایشی (Demo) برای تست بدون اتصال به سرور فعال است
- پیامک‌رسانی نیاز به پنل ملی‌پیامک دارد

---
**توسعه داده شده با ❤️ برای مدیریت فروشگاه‌های ووکامرس**
