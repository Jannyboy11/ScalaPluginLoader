package xyz.janboerman.scalaloader.example.scala3

enum RoseTree[+A]:
    case RoseNode(elem: A)
    case RoseLeaf

//TODO configuration serializable tree structure (defined as an enum!)
