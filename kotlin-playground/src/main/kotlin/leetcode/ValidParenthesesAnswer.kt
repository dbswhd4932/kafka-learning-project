package org.example.leetcode

class ValidParenthesesAnswer {
    fun isValid(s: String): Boolean {
        // 스택으로 사용할 CharArray 기반의 리스트
        val stack = mutableListOf<Char>()

        // 괄호 짝을 매핑
        val pairs = mapOf(
            ')' to '(',
            ']' to '[',
            '}' to '{'
        )

        for (char in s) {
            when (char) {
                '(', '[', '{' -> {
                    stack.add(char)
                }

                ')', ']', '}' -> {
                    if (stack.isEmpty()) {
                        return false
                    }

                    val lastOpen = stack.removeLast()
                    if (pairs[char] != lastOpen) {
                        return false
                    }
                }
            }
        }
        return true
    }
}

fun main() {

    val validParentheses = ValidParenthesesAnswer()
//    println(validParentheses.isValid("()"))
//    println(validParentheses.isValid("()[]{}"))
    println(validParentheses.isValid("(]"))
//    println(validParentheses.isValid("([])"))
}