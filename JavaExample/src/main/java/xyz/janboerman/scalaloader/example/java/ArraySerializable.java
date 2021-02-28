package xyz.janboerman.scalaloader.example.java;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;

import xyz.janboerman.scalaloader.configurationserializable.ConfigurationSerializable;
import xyz.janboerman.scalaloader.configurationserializable.Scan;

import static xyz.janboerman.scalaloader.example.java.ExamplePlugin.assertionsEnabled;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

class ArraySerializationTest {

    private final File saveFile;
    private final Logger logger;
    private final ExamplePlugin plugin;

    ArraySerializationTest(ExamplePlugin plugin) {
        this.plugin = plugin;
        File dataFolder = plugin.getDataFolder();
        dataFolder.mkdirs();
        this.saveFile = new File(dataFolder, "array-serialization-test.yml");
        if (!saveFile.exists()) {
            try {
                saveFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        this.logger = plugin.getLogger();
    }

    void test() {
        plugin.getServer().getConsoleSender().sendMessage(ChatColor.YELLOW + "Test " + ChatColor.RESET + "test deserialize(serialize(arrayserializable)).equals(arrayserializable)");

        //org.bukkit.configuration.serialization.ConfigurationSerialization.registerClass(ArraySerializable.class, "ArraySerializable");

        ArraySerializable as = new ArraySerializable();
        YamlConfiguration yamlConfiguration = new YamlConfiguration();
        yamlConfiguration.set("as", as);
        try {
            yamlConfiguration.save(saveFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        yamlConfiguration = YamlConfiguration.loadConfiguration(saveFile);
        ArraySerializable bs = (ArraySerializable) yamlConfiguration.get("as");
        assert as.equals(bs) : "deserialized ArraySerializable does not equal the original ArraySerializable";
        if (assertionsEnabled()) {
            plugin.getServer().getConsoleSender().sendMessage(ChatColor.GREEN + "Test passed!");
        }
    }
}

@ConfigurationSerializable(as = "ArraySerializable", scan = @Scan(Scan.Type.FIELDS))
//@org.bukkit.configuration.serialization.SerializableAs("ArraySerializable")
public class ArraySerializable /*implements org.bukkit.configuration.serialization.ConfigurationSerializable*/ {

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

    ArraySerializable() {
    }

    /* GENERATED!
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
                List<Boolean> list1 = new ArrayList<>(xs.length);
                for (boolean x : xs) {
                    list1.add((Boolean.valueOf(x)));
                }
                list0.add(list1);
            }
            map.put("booleanss", list0);
        }

        return map;
    }
     */

    /* Generated!
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
     */

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
}
