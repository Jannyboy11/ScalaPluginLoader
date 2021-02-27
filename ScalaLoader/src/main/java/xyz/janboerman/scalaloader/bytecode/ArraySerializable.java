package xyz.janboerman.scalaloader.bytecode;

import org.bukkit.configuration.serialization.*;

import java.util.*;

//@ConfigurationSerializable(as = "ArraySerializationTest", scan = @Scan(Scan.Type.FIELDS))
@SerializableAs("ArraySerializable")
@Deprecated
class ArraySerializable implements ConfigurationSerializable {

    //just needs container type conversion: String[]<->List<String>
    private String[] strings = new String[] { "hello", "world" };
    //needs container type conversion as well as component type conversion: int[]<->List<Integer>
    private int[] ints = new int[] {0, 1, 2};
    //idem, but the component type conversion is more than just (un)boxing: long[]<->List<String>
    private long[] longs = new long[] {4L, 5L, 6L};
    //idem, but the component itself is a container, and therefore needs nested conversion: boolean[][]<->List<List<Boolean>> (need to utilise recursion!)
    private boolean[][] booleanss = new boolean[][] {
            {true, false, false},
            {false, true, true}
    };

    private ArraySerializable() {
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();

        {
            List<String> list0;
            list0 = Arrays.asList(strings);
            map.put("strings", list0);
        }
        {
            List<Integer> list0;
            list0 = new ArrayList<>(ints.length);
            for (int x : ints) {
                list0.add(Integer.valueOf(x));
            }
            map.put("ints", list0);
        }
        {
            List<String> list0;
            list0 = new ArrayList<>(longs.length);
            for (long x : longs) {
                list0.add(Long.toString(x));
            }
            map.put("longs", list0);
        }
        {
            List<List<Boolean>> list0;
            list0 = new ArrayList<>(booleanss.length);
            for (boolean[] xs : booleanss) {
                List<Boolean> list1;
                list1 = new ArrayList<>(xs.length);
                for (boolean x : xs) {
                    list1.add((Boolean.valueOf(x)));
                }
                list0.add(list1);
            }
            map.put("booleanss", list0);
        }

        return map;
    }

    public static ArraySerializable deserialize(Map<String, Object> map) {
        ArraySerializable res = new ArraySerializable();

        {
            List<String> list0 = (List<String>) map.get("strings");
            String[] array0 = list0.toArray(new String[list0.size()]);
            res.strings = array0;
        }
        {
            List<Integer> list0 = (List<Integer>) map.get("ints");
            int[] array0 = new int[list0.size()];
            for (int idx0 = 0; idx0 < array0.length; idx0++) {
                array0[idx0] = list0.get(idx0).intValue();
            }
            res.ints = array0;
        }
        {
            List<String> list0 = (List<String>) map.get("longs");
            long[] array0 = new long[list0.size()];
            for (int idx0 = 0; idx0 < array0.length; idx0++) {
                array0[idx0] = Long.parseLong(list0.get(idx0));
            }
            res.longs = array0;
        }
        {
            List<List<Boolean>> list0 = (List<List<Boolean>>) map.get("booleanss");
            boolean[][] array0 = new boolean[list0.size()][];
            for (int idx0 = 0; idx0 < array0.length; idx0++) {
                List<Boolean> list1 = list0.get(idx0);
                boolean[] array1 = new boolean[list1.size()];
                for (int idx1 = 0; idx1 < array1.length; idx1++) {
                    array1[idx1] = list1.get(idx1).booleanValue();
                }
                array0[idx0] = array1;
            }

            res.booleanss = array0;
        }


        return res;
    }

    @Override
    public int hashCode() {
        int result = 1;

        result = 31 * result + Arrays.hashCode(strings);
        result = 31 * result + Arrays.hashCode(ints);
        result = 31 * result + Arrays.hashCode(longs);
        result = 31 * result + Arrays.deepHashCode(booleanss);

        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof ArraySerializable)) return false;

        ArraySerializable that = (ArraySerializable) obj;
        if (!Arrays.equals(this.strings, that.strings)) return false;
        if (!Arrays.equals(this.ints, that.ints)) return false;
        if (!Arrays.equals(this.longs, that.longs)) return false;
        if (!Arrays.deepEquals(this.booleanss, that.booleanss)) return false;

        return true;
    }

    @Override
    public String toString() {
        return "ArraySerializable"
                + "{strings = " + Arrays.toString(strings)
                + ",ints = " + Arrays.toString(ints)
                + ",longs = " + Arrays.toString(longs)
                + ",booleanss = " + Arrays.deepToString(booleanss)
                + "}";

    }

    private static final int[] deserializeInts(List<Integer> list0) {
        int[] array0 = new int[list0.size()];
        for (int idx0 = 0; idx0 < array0.length; idx0++) {
            array0[idx0] = list0.get(idx0).intValue();
        }
        return array0;
    }

    /*
    {
            methodVisitor = classWriter.visitMethod(ACC_PRIVATE | ACC_FINAL | ACC_STATIC, "deserializeInts", "(Ljava/util/List;)[I", "(Ljava/util/List<Ljava/lang/Integer;>;)[I", null);
            methodVisitor.visitParameter("list0", 0);
            methodVisitor.visitCode();

            Label label0 = new Label();                                                                                 // int[] array0 = new int[list0.size()]
            methodVisitor.visitLabel(label0);
            methodVisitor.visitVarInsn(ALOAD, 0);
            methodVisitor.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "size", "()I", true);
            methodVisitor.visitIntInsn(NEWARRAY, T_INT);
            methodVisitor.visitVarInsn(ASTORE, 1);

            Label label1 = new Label();                                                                                 //int idx0 = 0;
            methodVisitor.visitLabel(label1);
            methodVisitor.visitInsn(ICONST_0);
            methodVisitor.visitVarInsn(ISTORE, 2);

            Label label2 = new Label();                                                                                 //jump target (this is the first time we got here, or idx < array0.length is true)
            methodVisitor.visitLabel(label2);
            methodVisitor.visitFrame(Opcodes.F_APPEND, 2, new Object[]{"[I", Opcodes.INTEGER}, 0, null);                //we have two local variables and nothing on the stack
            methodVisitor.visitVarInsn(ILOAD, 2);                                                                       //[..., idx]
            methodVisitor.visitVarInsn(ALOAD, 1);                                                                       //[..., idx, array0]
            methodVisitor.visitInsn(ARRAYLENGTH);                                                                       //[..., idx, array0.length]

            Label label3 = new Label();                                                                                 //if (not (idx < array0.length)) goto label3 (= end of loop)
            methodVisitor.visitJumpInsn(IF_ICMPGE, label3);

            Label label4 = new Label();                                                                                 //loop body
            methodVisitor.visitLabel(label4);
                                                                                                                        //load stuff onto the stack
            methodVisitor.visitVarInsn(ALOAD, 1);                                                                       //[..., array0]
            methodVisitor.visitVarInsn(ILOAD, 2);                                                                       //[..., array0, idx0]
            methodVisitor.visitVarInsn(ALOAD, 0);                                                                       //[..., array0, idx0, list0]
            methodVisitor.visitVarInsn(ILOAD, 2);                                                                       //[..., array0, idx0, list0, idx0]
            methodVisitor.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "get", "(I)Ljava/lang/Object;", true);     //[..., array0, idx0, element: Object]

                                                                                                                        //apply element conversion
            methodVisitor.visitTypeInsn(CHECKCAST, "java/lang/Integer");                                                //[..., array0, idx0, serializedElement: Integer]
            methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false);                //[..., array0, idx0, liveElement: int]
                                                                                                                        //array0[idx0] = liveElement
            methodVisitor.visitInsn(IASTORE);                                                                           //[...]

            Label label5 = new Label();                                                                                 //end of for-loop body. apply UpdateStatement and ConditionCheck in (for (Initialization; ConditionCheck; UpdateStatement))
            methodVisitor.visitLabel(label5);
            methodVisitor.visitIincInsn(2, 1);                                                                          //idx0++;
            methodVisitor.visitJumpInsn(GOTO, label2);                                                                  //goto loop condition check

            methodVisitor.visitLabel(label3);
            methodVisitor.visitFrame(Opcodes.F_CHOP, 1, null, 0, null);                                                 //num locals is one less than before we got here (idx0 is no longer in scope), and the stack is empty.
            methodVisitor.visitVarInsn(ALOAD, 1);                                                                       //[..., array0]
            methodVisitor.visitInsn(ARETURN);                                                                           //return array0;

            Label label6 = new Label();
            methodVisitor.visitLabel(label6);
            methodVisitor.visitLocalVariable("idx0", "I", null, label2, label3, 2);
            methodVisitor.visitLocalVariable("list0", "Ljava/util/List;", "Ljava/util/List<Ljava/lang/Integer;>;", label0, label6, 0);
            methodVisitor.visitLocalVariable("array0", "[I", null, label1, label6, 1);
            methodVisitor.visitMaxs(4, 3);
            methodVisitor.visitEnd();
        }
     */

    private static final List<List<Boolean>> serializeBooleanss(boolean[][] array0) {
        List<List<Boolean>> list0;
        list0 = new ArrayList<>(array0.length);
        for (boolean[] array1 : array0) {
            List<Boolean> list1;
            list1 = new ArrayList<>(array1.length);
            for (boolean x : array1) {
                list1.add((Boolean.valueOf(x)));
            }
            list0.add(list1);
        }
        return list0;
    }

    /*
    {
            methodVisitor = classWriter.visitMethod(ACC_PRIVATE | ACC_FINAL | ACC_STATIC, "serializeBooleanss", "([[Z)Ljava/util/List;", "([[Z)Ljava/util/List<Ljava/util/List<Ljava/lang/Boolean;>;>;", null);
            methodVisitor.visitParameter("array0", 0);
            methodVisitor.visitCode();

            Label label0 = new Label();                                                                                 //List<List<Boolean> list0 = new ArrayList<>(0.length);
            methodVisitor.visitLabel(label0);
            methodVisitor.visitTypeInsn(NEW, "java/util/ArrayList");
            methodVisitor.visitInsn(DUP);
            methodVisitor.visitVarInsn(ALOAD, 0);
            methodVisitor.visitInsn(ARRAYLENGTH);
            methodVisitor.visitMethodInsn(INVOKESPECIAL, "java/util/ArrayList", "<init>", "(I)V", false);
            methodVisitor.visitVarInsn(ASTORE, 1);

            Label label1 = new Label();
            methodVisitor.visitLabel(label1);
            methodVisitor.visitVarInsn(ALOAD, 0);                                                                       //[..., array0]                     (array0, list0)
            methodVisitor.visitVarInsn(ASTORE, 2);                                                                      //[...]                             (array0, list0, array0)
            methodVisitor.visitVarInsn(ALOAD, 2);                                                                       //[..., array0]
            methodVisitor.visitInsn(ARRAYLENGTH);                                                                       //[..., array0.length]
            methodVisitor.visitVarInsn(ISTORE, 3);                                                                      //[...]                             (array0, list0, array0, array0.length)
            methodVisitor.visitInsn(ICONST_0);                                                                          //[..., 0]
            methodVisitor.visitVarInsn(ISTORE, 4);                                                                      //[...]                             (array0, list0, array0, array0.length, idx0=0)

            Label label2 = new Label();
            methodVisitor.visitLabel(label2);
            methodVisitor.visitFrame(Opcodes.F_FULL, 5, new Object[]{"[[Z", "java/util/List", "[[Z", Opcodes.INTEGER, Opcodes.INTEGER}, 0, new Object[]{});
            methodVisitor.visitVarInsn(ILOAD, 4);                                                                       //[..., idx0]
            methodVisitor.visitVarInsn(ILOAD, 3);                                                                       //[..., idx0, array0.length]

            Label label3 = new Label();
            methodVisitor.visitJumpInsn(IF_ICMPGE, label3);                                                             //if (not (idx0 < array0.length)) goto label3 (= loop end)
            methodVisitor.visitVarInsn(ALOAD, 2);                                                                       //[..., array0]
            methodVisitor.visitVarInsn(ILOAD, 4);                                                                       //[..., array0, idx0]
            methodVisitor.visitInsn(AALOAD);                                                                            //[..., array0[idx0]]
            methodVisitor.visitVarInsn(ASTORE, 5);                                                                      //boolean[] array1 = array0[idx0];  (array0, list0, array0, array0.length, idx0, array1);

            Label label4 = new Label();
            methodVisitor.visitLabel(label4);
            methodVisitor.visitTypeInsn(NEW, "java/util/ArrayList");
            methodVisitor.visitInsn(DUP);
            methodVisitor.visitVarInsn(ALOAD, 5);
            methodVisitor.visitInsn(ARRAYLENGTH);
            methodVisitor.visitMethodInsn(INVOKESPECIAL, "java/util/ArrayList", "<init>", "(I)V", false);
            methodVisitor.visitVarInsn(ASTORE, 6);                                                                      //List list1 = new ArrayList(array1.length);    (array0, list0, array0, array0.length, idx0, array1, list1)

            Label label5 = new Label();
            methodVisitor.visitLabel(label5);
            methodVisitor.visitVarInsn(ALOAD, 5);                                                                       //[..., array1]
            methodVisitor.visitVarInsn(ASTORE, 7);                                                                      //[...]                             (array0, list0, array0, array0.length, idx0, array1, list1, array1)
            methodVisitor.visitVarInsn(ALOAD, 7);                                                                       //[..., array1]
            methodVisitor.visitInsn(ARRAYLENGTH);                                                                       //[..., array1.length]
            methodVisitor.visitVarInsn(ISTORE, 8);                                                                      //[...]                             (array0, list0, array0, array0.length, idx0, array1, list1, array1, array1.length)
            methodVisitor.visitInsn(ICONST_0);                                                                          //[..., 0]
            methodVisitor.visitVarInsn(ISTORE, 9);                                                                      //[...]                             (array0, list0, array0, array0.length. idx0, array1, list1, array1, array1.length, idx1=0)

            Label label6 = new Label();
            methodVisitor.visitLabel(label6);
            methodVisitor.visitFrame(Opcodes.F_FULL, 10, new Object[]{"[[Z", "java/util/List", "[[Z", Opcodes.INTEGER, Opcodes.INTEGER, "[Z", "java/util/List", "[Z", Opcodes.INTEGER, Opcodes.INTEGER}, 0, new Object[]{});
            methodVisitor.visitVarInsn(ILOAD, 9);                                                                       //[..., idx1]
            methodVisitor.visitVarInsn(ILOAD, 8);                                                                       //[..., idx1, array1.length]

            Label label7 = new Label();
            methodVisitor.visitJumpInsn(IF_ICMPGE, label7);                                                             //if (not (idx1 < array1.length)) goto label7 (= inner loop end)
            methodVisitor.visitVarInsn(ALOAD, 7);                                                                       //[..., array1]
            methodVisitor.visitVarInsn(ILOAD, 9);                                                                       //[..., array1, idx1]
            methodVisitor.visitInsn(BALOAD);                                                                            //[..., array1[idx1]]
            methodVisitor.visitVarInsn(ISTORE, 10);                                                                     //boolean x = array1[idx1]          (array0, list0, array0, array0.length, idx0, array1, list1, array1, array1.length, idx1, x: boolean)

            Label label8 = new Label();
            methodVisitor.visitLabel(label8);
            methodVisitor.visitVarInsn(ALOAD, 6);                                                                           //[..., list1]
            methodVisitor.visitVarInsn(ILOAD, 10);                                                                          //[..., list1, x: boolean]
            methodVisitor.visitMethodInsn(INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false);   //[..., list1, serialize(x)]
            methodVisitor.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "add", "(Ljava/lang/Object;)Z", true);         //[..., list1#add(Object) boolean result]
            methodVisitor.visitInsn(POP);                                                                                   //[...] //get rid of da boolean

            Label label9 = new Label();
            methodVisitor.visitLabel(label9);
            methodVisitor.visitIincInsn(9, 1);                                                                          //idx1++
            methodVisitor.visitJumpInsn(GOTO, label6);                                                                  //Jump back to just before the loop condition
            methodVisitor.visitLabel(label7);                                                                           //inner loop end
            methodVisitor.visitFrame(Opcodes.F_CHOP, 3, null, 0, null);                                                 //[...] chop last 3 locals:     (array0, list0, array0, array0.length, idx0, array1, list1, array1)
            methodVisitor.visitVarInsn(ALOAD, 1);                                                                       //[..., list0]
            methodVisitor.visitVarInsn(ALOAD, 6);                                                                       //[..., list0, list1]
            methodVisitor.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "add", "(Ljava/lang/Object;)Z", true);     //[..., list0#add(list1) boolean result]
            methodVisitor.visitInsn(POP);                                                                               //[...]

            Label label10 = new Label();
            methodVisitor.visitLabel(label10);
            methodVisitor.visitIincInsn(4, 1);                                                                          //idx0++;
            methodVisitor.visitJumpInsn(GOTO, label2);                                                                  //Jump back to just before the loop condition
            methodVisitor.visitLabel(label3);                                                                           //outer loop end
            methodVisitor.visitFrame(Opcodes.F_FULL, 2, new Object[]{"[[Z", "java/util/List"}, 0, new Object[]{});      //[...]                         (array0, list0)
            methodVisitor.visitVarInsn(ALOAD, 1);                                                                       //[..., list0]
            methodVisitor.visitInsn(ARETURN);                                                                           /return list0

            Label label11 = new Label();
            methodVisitor.visitLabel(label11);
            methodVisitor.visitLocalVariable("x", "Z", null, label8, label9, 10);
            methodVisitor.visitLocalVariable("list1", "Ljava/util/List;", "Ljava/util/List<Ljava/lang/Boolean;>;", label5, label10, 6);
            methodVisitor.visitLocalVariable("array1", "[Z", null, label4, label10, 5);
            methodVisitor.visitLocalVariable("array0", "[[Z", null, label0, label11, 0);
            methodVisitor.visitLocalVariable("list0", "Ljava/util/List;", "Ljava/util/List<Ljava/util/List<Ljava/lang/Boolean;>;>;", label1, label11, 1);
            methodVisitor.visitMaxs(3, 11);
            methodVisitor.visitEnd();
        }
     */
}
