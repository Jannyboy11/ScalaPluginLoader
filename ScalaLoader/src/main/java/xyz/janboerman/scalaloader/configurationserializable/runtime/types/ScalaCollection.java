package xyz.janboerman.scalaloader.configurationserializable.runtime.types;

import xyz.janboerman.scalaloader.configurationserializable.runtime.Adapter;

public abstract class ScalaCollection implements Adapter/*<scala.collection.Iterable>*/ {

    //TODO specialcases needed for a few collectoins: Range, NumericRange, WrappedString, ArraySeq (because it needs ClassTag)
    //TODO also need to special-case the 'sorted' collections: TreeSet etc

}
