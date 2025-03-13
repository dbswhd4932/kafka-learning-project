package org.example.classes

// java로 치면 "static 메서드와 필드를 가진 클래스"와 비슷한 역할
object Authenticate {

    fun authenticate(userName: String, password: String) {
        println("User authenticate for userName : $userName")
    }
}

fun main() {
    Authenticate.authenticate("Dilip", "abc")

}