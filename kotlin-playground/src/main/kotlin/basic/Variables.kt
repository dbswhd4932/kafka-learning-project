package org.example.basic

import org.example.functions.courseName
import org.example.functions.topLevelFunction

fun main() {

    val name : String = "Dilip"

    println(name)

    var age : Int = 34
    println(age)
    age = 20
    println(age)

    val salary = 30000L
    println(salary)

    val course = "Kotlin Spring"
    println("course : $course and the course length is ${course.length}")

    val multiLine = "ABC \n DEF"
    println(multiLine)

    val multiLine1 = """
        ABC
        DEF
    """.trimIndent()
    println(multiLine1)

    val topLevelFunction = topLevelFunction()
    println("topLevelFunction = $topLevelFunction")

    println("------------------------------")

    println("courseName = $courseName")
}