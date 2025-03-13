package org.example.classes

class Item() {

    var name: String = ""
    var price: Double = 0.0
        get() {
            println("Inside Getter")
            return field
        }

        set(value) {
            println("Inside Setter")
            if(value >= 0.0) {
                field = value
            } else {
                throw IllegalArgumentException("Value can't be negative")
            }
        }

    constructor(_name: String) : this() {
        name = _name
    }

}

fun main() {

    val item = Item("Iphone")
    println("Item name is ${item.name}")

    item.name = "Iphone 13"
    println("Item name is ${item.name}")

    println(item)

    item.price = 2.0
    println(item.price)


}