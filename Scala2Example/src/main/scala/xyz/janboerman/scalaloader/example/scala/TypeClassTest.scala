package xyz.janboerman.scalaloader.example.scala

trait Show[T] {
    def show(value: T): String



}

object Show {

    // if the scala scala compiler emits a *signature* in the bytecode, and not just a 'descriptor', then we could get this instance from the bytecode.
    implicit val showInt: Show[Int] = new Show[Int] {
        override def show(value: Int): String = java.lang.Integer.toString(value)
    }

    // idem (but then we need to check the return type signature).
    implicit def showByte: Show[Byte] = new Show[Byte] {
        override def show(value: Byte): String = java.lang.Byte.toString(value);
    }

    // for this one we need some extra indirection because we need to check the interfaces which the object implements.
    // I *really* hope we can use the pickles from Scala 2, or otherwise use the TASTy file.
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

