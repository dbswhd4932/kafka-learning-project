package org.example.leetcode

class LongestCommonPrefix {
    fun longestCommonPrefix(strs: Array<String>): String {

        var result = ""

        // 가장 짧은 길이
        var shortIndex = 0
        for (str in strs) {
            shortIndex = str.length
            if (shortIndex > str.length) {
                shortIndex = str.length
            }
        }

        for (str in strs) {
            for (index in shortIndex downTo 0) {
                str[index]
            }
        }

        return ""
    }
}