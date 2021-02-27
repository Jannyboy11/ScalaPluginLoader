package xyz.janboerman.scalaloader.configurationserializable.transform;

import org.objectweb.asm.signature.SignatureVisitor;

import static xyz.janboerman.scalaloader.configurationserializable.transform.ConfigurationSerializableTransformations.ASM_API;

class UnapplyParamCounter extends SignatureVisitor {

    private int count = 0;

    //only call this using the signature of a return type!
    UnapplyParamCounter() {
        super(ASM_API);
    }


    //visitBaseType | visitTypeVariable | visitArrayType | ( visitClassType visitTypeArgument* ( visitInnerClassType visitTypeArgument* )* visitEnd ) )

    @Override
    public void visitBaseType(char primitiveType) {
        if ('Z' == primitiveType) {
            count = 0;
        }
    }

    @Override
    public void visitClassType(String name) {
        switch (name) {
            case "scala/Option":    count += 1;     break;
            case "scala/Tuple2":    count += 1;     break;
            case "scala/Tuple3":    count += 2;     break;
            case "scala/Tuple4":    count += 3;     break;
            case "scala/Tuple5":    count += 4;     break;
            case "scala/Tuple6":    count += 5;     break;
            case "scala/Tuple7":    count += 6;     break;
            case "scala/Tuple8":    count += 7;     break;
            case "scala/Tuple9":    count += 8;     break;
            case "scala/Tuple10":   count += 9;     break;
            case "scala/Tuple11":   count += 10;    break;
            case "scala/Tuple12":   count += 11;    break;
            case "scala/Tuple13":   count += 12;    break;
            case "scala/Tuple14":   count += 13;    break;
            case "scala/Tuple15":   count += 14;    break;
            case "scala/Tuple16":   count += 15;    break;
            case "scala/Tuple17":   count += 16;    break;
            case "scala/Tuple18":   count += 17;    break;
            case "scala/Tuple19":   count += 18;    break;
            case "scala/Tuple20":   count += 19;    break;
            case "scala/Tuple21":   count += 20;    break;
            case "scala/Tuple22":   count += 21;    break;
        }
    }

    public int getParamCount() {
        return count;
    }

}
