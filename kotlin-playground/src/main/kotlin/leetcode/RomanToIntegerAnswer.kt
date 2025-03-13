package org.example.leetcode

class RomanToIntegerAnswer {
    fun romanToInt(s: String): Int {

        // 미리 맵핑
        val romanValues = mapOf(
            'I' to 1,
            'V' to 5,
            'X' to 10,
            'L' to 50,
            'C' to 100,
            'D' to 500,
            'M' to 1000,
        )

        var result = 0 // return 값
        var i = 0   // index

        while (i < s.length) {
            // 현재 문자의 값
            val current = romanValues[s[i]]!!

            // 다음 문자가 있으면서, 현재 값이 다음 문자보다 작으면 빼기
            if (i + 1 < s.length && current < romanValues[s[i + 1]]!!) {
                result += romanValues[s[i + 1]]!! - current
                // 인덱스 2개 증가
                i += 2
            } else {
                // 이외의 경우는 그냥 더하고
                result += current
                // 인덱스 1개 증가
                i++
            }
        }

        return result
    }
}

fun main() {

    val romanToIntegerAnswer = RomanToIntegerAnswer()
    println(romanToIntegerAnswer.romanToInt("III"))     // 출력: 3
    println(romanToIntegerAnswer.romanToInt("LVIII"))   // 출력: 58
    println(romanToIntegerAnswer.romanToInt("MCMXCIV")) // 출력: 1994

}