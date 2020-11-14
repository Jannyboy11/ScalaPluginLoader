package xyz.janboerman.scalaloader.bytecode;

public interface MyInterface {

    public static void foo() {
        System.out.println("foo!");
    }

}

class User {
    public static void main(String[] args) {
        MyInterface.foo();
    }
}
