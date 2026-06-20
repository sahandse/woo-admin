package com.sahand.wooadmin.core.utils

import java.text.DecimalFormat

object Helpers {

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

    fun formatPrice(amount: Long): String {
        val formatter = DecimalFormat("#,###")
        val formatted = formatter.format(amount)
        return toPersianDigits("$formatted تومان")
    }
}
