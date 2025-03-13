package org.example.leetcode

class RomanToInteger {
    fun romanToInt(s: String): Int {

        var result = 0

        for (i in s.indices) {
            if (s[i] == 'I') {
                result += 1
            } else if (s[i] == 'V') {
                result += 5
            } else if (s[i] == 'X') {
                result += 10
            } else if (s[i] == 'L') {
                result += 50
            } else if (s[i] == 'C') {
                result += 100
            } else if (s[i] == 'D') {
                result += 500
            } else if (s[i] == 'M') {
                result += 1000
            }
        }
        return result;
    }
}

fun main() {

    val result = RomanToInteger().romanToInt("MCMXCIV")
    println(result);

}