package xyz.janboerman.scalaloader.configurationserializable.transform;

//with the introduction of LocalVariableTable, we no longer need to track local variables here.
//since we don't need to do that anymore, this class will effectively become just a wrapper around int increasedMaxStack
//so we could get rid of this class completely if we wanted. I think that's what I want.
//TODO weigh pros and cons of removing this class.
//TODO would I want to keep it with revised functionality?
@Deprecated
class StackLocal {

    //TODO use OperandStack instead!!

    int increasedMaxStack = 0;
    @Deprecated
    int usedLocals = 0;

}
