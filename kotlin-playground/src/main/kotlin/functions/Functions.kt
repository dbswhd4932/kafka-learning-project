package org.example.functions

import java.time.LocalDate

fun printName(name: String): Unit {

    println("Name is $name")
}

fun addition(x: Int, y: Int): Int {
    return x + y
}

fun addition_approach1(x: Int, y: Int) = x + y

fun printPersonDetails(
    name: String,
    email: String = "",
    dob: LocalDate = LocalDate.now()
) {
    println("Name: $name, email: $email, dob: $dob")
}

fun main() {

    printName("Dilip")
    val result = addition(1, 2)
    println("result is $result")

    val result1 = addition_approach1(1, 2)
    println("result2 is $result1")

    printPersonDetails("yoonjong", "abc@gmail.com", LocalDate.parse("2000-01-01"));
    printPersonDetails("yoonjong");
    printPersonDetails(
        dob = LocalDate.parse("2000-01-01"),
        name = "yoonjong",
        email = "abc@gmail.com"
    );
}




