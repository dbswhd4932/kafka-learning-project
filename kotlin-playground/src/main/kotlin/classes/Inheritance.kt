package org.example.classes

import org.example.classes.Student.Companion.noOfEnrolledCourses

open class User(val name: String) {

    open var isLoggedIn : Boolean = false

    open fun login() {
        println("Inside User Login")
    }

}

class Student(name: String) : User(name) {

    override var isLoggedIn = true

    // static 이랑 비슷
    companion object {
        const val noOfEnrolledCourses = 10
        fun country() = "USA"
    }

    override fun login() {
        println("Inside Student Login") // Inside Student Login
        super.login() // Inside User Login
    }
}

class Instructor(name: String) : User(name) {

    override fun login() {
        println("Inside Instructor Login")
    }
}

fun main() {

    val student = Student("John")
    println(student.name)
    student.login()
    println("Logged in values is ${student.isLoggedIn}")

    val country = Student.country()
    println("Country is ${country}")
    println("noOfEnrolledCourses is ${Student.noOfEnrolledCourses}")

    val instructor = Instructor("Alex")
    println(instructor.name)
    instructor.login()

}