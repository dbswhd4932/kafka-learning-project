package org.example.classes

data class Course(
    val id: Int,
    val name: String,
    val author: String
)

fun main() {

    val course = Course(1, "Reactive Programming in Modern Java using Project Reactor", "Dilip")
    println(course)

    val course1 = Course(2, "Reactive Programming in Modern Java using Project Reactor", "Dilip")
    println("Checking object Equality : ${course == course1}")

    val course3 = course1.copy(
        id = 3, author = "Dilip1"
    )
    println(course3)
}