package org.example.`interface`

import org.example.classes.Course

interface CourseRepository {

    val isCoursePersisted : Boolean
    fun getById(id: Int): Course
    fun save(course: Course) : Int {
        println("course : $course")
        return course.id
    }

}

interface Repository {

    fun getAll() : Any

}

class SqlCourseRepository : CourseRepository, Repository {
    override var isCoursePersisted: Boolean = false

    override fun getById(id: Int): Course {

        return Course(id, "Reactive Programming in Modern Java using Project Reactor", "Dilip")

    }

    override fun save(course: Course): Int {
        isCoursePersisted = true
        return super.save(course)
    }

    override fun getAll(): Any {

        return 1
    }
}

interface A {
    fun doSomething() {
       println("doSomething in A")
    }
}

interface B {
    fun doSomething() {
        println("doSomething in B")
    }
}

class AB : A , B {
    override fun doSomething() {
        super<A>.doSomething()
        super<B>.doSomething()
        println("doSomething in AB")
    }
}

class NoSqlCourseRepository : CourseRepository {
    override val isCoursePersisted: Boolean = false

    override fun getById(id: Int): Course {
        return Course(id, "Reactive Programming in Modern Java using Project Reactor", "Dilip")
    }

    override fun save(course: Course): Int {
        println("save course in NoSqlCourseRepository : $course")
        return course.id
    }


}


fun main() {

    val sqlCourseRepository = SqlCourseRepository()
    val course = sqlCourseRepository.getById(1)
    val courseId = sqlCourseRepository.save(course)

    println("Course is ${course}")
    println("saved Course Id is ${courseId}")
    println("Course isCoursePersisted is ${sqlCourseRepository.isCoursePersisted}")


    val nosqlCourseRepository = NoSqlCourseRepository()
    val course1 = nosqlCourseRepository.getById(2)
    val noCourseId = nosqlCourseRepository.save(course1)

    println("NoCourse is ${course1}")
    println("saved Course Id is ${noCourseId}")

    val ab = AB()
    ab.doSomething()

}