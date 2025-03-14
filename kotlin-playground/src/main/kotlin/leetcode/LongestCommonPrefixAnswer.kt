package org.example.leetcode

class LongestCommonPrefixAnswer {
    fun longestCommonPrefix(strs: Array<String>): String {
        if (strs.isEmpty()) return ""

        // 첫 번째 문자열 기준
        val first = strs[0]

        // 모든 문자열을 순회하며 접두사 비교
        for (i in first.indices) {
            val char = first[i]
            // 나머지 문자열들과 비교
            for (str in strs) {
                // 현재 문자열이 i보다 짧거나, 문자가 다르면 종료
                if (i >= str.length || str[i] != char) {
                    return first.substring(0, i)
                }
            }
        }

        // 모든 문자가 일치하면 첫 번째 문자열 전체 리턴
        return first;
    }
}

fun main() {
    val lcp = LongestCommonPrefixAnswer()
    println(lcp.longestCommonPrefix(arrayOf("flower", "flow", "flight"))) // "fl"
    println(lcp.longestCommonPrefix(arrayOf("dog", "racecar", "car")))    // ""
    println(lcp.longestCommonPrefix(arrayOf("interspecies", "interstellar", "interstate"))) // "inters"
    println(lcp.longestCommonPrefix(arrayOf(""))) // ""
    println(lcp.longestCommonPrefix(arrayOf("a"))) // "a"
}