package org.example.basic

fun main() {
    // if-else
    // when

    var name = "Alex"
    name = "Chloe"

    val result = if (name.length == 4) {
        println("Name is Four Characters")
        name // 반환값
    } else {
        println("Name is not Four Characters")
        name // 반환값
    }

    println("result : $result")

    // 1 -> GOLD 2 -> SLIVER 3 -> BRONZE

/*    val position = 1
    val medal = if (position == 1) {
        "GOLD"
    } else if (position == 2) {
        "SLIVER"
    } else if (position == 3) {
        "BRONZE"
    } else {
        "NO MEDAL"
    }*/

    val position = 3
    val medal = when (position) {
        1 -> "GOLD"
        2 -> "SILVER"
        3 -> "BRONZE"
        else -> "NO MEDAL"
    }

    println("medal: $medal")
}