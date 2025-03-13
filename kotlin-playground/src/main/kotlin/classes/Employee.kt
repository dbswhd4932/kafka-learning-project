package org.example.classes

data class Employee(
    val id: Int,
    val name: String
)

fun main() {
    val employee = Employee(1, "John")
    println(employee)

    val employee1 = Employee(1, "John")
    println("${employee == employee1}")

    val employee2 = employee.copy(2, "Yoon")
    println("${employee == employee2}")
}
