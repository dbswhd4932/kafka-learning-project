package org.example.leetcode

class ValidParentheses {
    fun isValid(s: String): Boolean {

        val length = s.length
        // 첫번째, 마지막 비교
        for (i in length - 1 downTo 0) {
            if (s[i] != s[length - i]) return false
        }
        return true
    }
}

fun main() {

    val validParentheses = ValidParentheses()
    println(validParentheses.isValid("()"))
    println(validParentheses.isValid("()[]{}"))
    println(validParentheses.isValid("(]"))
    println(validParentheses.isValid("([])"))
}