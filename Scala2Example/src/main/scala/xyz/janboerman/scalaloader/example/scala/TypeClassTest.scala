package xyz.janboerman.scalaloader.example.scala

trait Show[T] {
    def show(value: T): String



}

object Show {

    implicit val showInt: Show[Int] = new Show[Int] {
        override def show(value: Int): String = java.lang.Integer.toString(value)
    }

    implicit object ShowLong extends Show[Long] {
        override def show(value: Long): String = java.lang.Long.toString(value)
    }
}

case class Cat(name: String)

object Cat {
    implicit val showCat: Show[Cat] = new Show[Cat] {
        override def show(cat: Cat): String = s"Cat(${cat.name})"
    }
}

case class Dog(name: String, enemy: Cat)

object Dog {
    implicit def showDog(implicit showCat: Show[Cat]): Show[Dog] = new Show[Dog] {
        override def show(dog: Dog): String = s"Dog(${dog.name},enemy=${showCat.show(dog.enemy)}"
    }
}

