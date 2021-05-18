package xyz.janboerman.scalaloader.configurationserializable.runtime.types;

import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.configuration.serialization.SerializableAs;
import xyz.janboerman.scalaloader.bytecode.Called;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * <p>
 *     A Java implementation of Scala's NumericRange. Subclasses implement {@link ConfigurationSerializable}.
 * </p>
 * <p>
 *     Because this class' constructor is protected, you can implement your own subclass specialized for a certain number type.
 *     Keep in mind that the Number type I <b>*must*</b> be integral!
 * </p>
 * @param <I> the type of number. Must be integral! (e.g. Integer, Long, Short, Byte, BigInteger)
 *
 * @see OfByte
 * @see OfShort
 * @see OfInteger
 * @see OfLong
 * @see OfBigInteger
 */
public abstract class NumericRange<I> {
    public static void registerWithConfigurationSerialization() {
        OfByte.register();
        OfShort.register();
        OfInteger.register();
        OfLong.register();
        OfBigInteger.register();
    }

    private static final String
            START = "start",
            STEP = "step",
            END = "end",
            INCLUSIVE = "inclusive";

    private final I start;
    private final I step;
    private final I end;
    private final boolean inclusive;

    /**
     * Construct the numeric range.
     * @param start the lower bound of this range
     * @param step the step size of this range
     * @param end the upper bound of this range
     * @param inclusive whether to include the upper bound in the range
     */
    protected NumericRange(I start, I step, I end, boolean inclusive) {
        this.start = start;
        this.step = step;
        this.end = end;
        this.inclusive = inclusive;
    }

    /**
     * Get the lower bound of this range.
     * @return the lower bound
     */
    public I getStart() {
        return start;
    }

    /**
     * Get the step size of this range.
     * @return the step size
     */
    public I getStep() {
        return step;
    }

    /**
     * Get the upper bound of this range.
     * @return the upper bound
     */
    public I getEnd() {
        return end;
    }

    /**
     * Get whether the upper bound is included in this range.
     * @return true if the upper bound is included, otherwise false
     */
    public boolean isInclusive() {
        return inclusive;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getStart(), getStep(), getEnd(), isInclusive());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof NumericRange)) return false;

        NumericRange that = (NumericRange) obj;
        return Objects.equals(this.getStart(), that.getStart())
                && Objects.equals(this.getStep(), that.getStep())
                && Objects.equals(this.getEnd(), that.getEnd())
                && this.isInclusive() == that.isInclusive();
    }

    @Override
    public String toString() {
        return "NumericRange " + getStart() + (isInclusive() ? " to " : " until ") + getEnd() + " by " + getStep();
    }

    @SerializableAs("NumericRange.OfByte")
    public static class OfByte extends NumericRange<Byte> implements ConfigurationSerializable {
        private static void register() {
            ConfigurationSerialization.registerClass(OfByte.class, "NumericRange.OfByte");
        }

        public OfByte(byte start, byte step, byte end, boolean inclusive) {
            super(start, step, end, inclusive);
        }

        public byte start() {
            return super.getStart();
        }

        public byte step() {
            return super.getStep();
        }

        public byte end() {
            return super.getEnd();
        }

        @Override
        public Map<String, Object> serialize() {
            Map<String, Object> map = new HashMap<>();

            map.put(START, getStart().intValue());
            map.put(STEP, getStep().intValue());
            map.put(END, getEnd().intValue());
            map.put(INCLUSIVE, isInclusive());

            return map;
        }

        public static OfByte valueOf(Map<String, Object> map) {
            Integer start = (Integer) map.get(START);
            Integer step = (Integer) map.get(STEP);
            Integer end = (Integer) map.get(END);
            Boolean inclusive = (Boolean) map.get(INCLUSIVE);

            return new OfByte(start.byteValue(), step.byteValue(), end.byteValue(), inclusive);
        }
    }

    @SerializableAs("NumericRange.OfShort")
    public static class OfShort extends NumericRange<Short> implements ConfigurationSerializable {
        private static void register() {
            ConfigurationSerialization.registerClass(OfShort.class, "NumericRange.OfShort");
        }

        public OfShort(short start, short step, short end, boolean inclusive) {
            super(start, step, end, inclusive);
        }

        public short start() {
            return super.getStart();
        }

        public short step() {
            return super.getStep();
        }

        public short end() {
            return super.getEnd();
        }

        @Override
        public Map<String, Object> serialize() {
            Map<String, Object> map = new HashMap<>();

            map.put(START, getStart().intValue());
            map.put(STEP, getStep().intValue());
            map.put(END, getEnd().intValue());
            map.put(INCLUSIVE, isInclusive());

            return map;
        }

        public static OfShort valueOf(Map<String, Object> map) {
            Integer start = (Integer) map.get(START);
            Integer step = (Integer) map.get(STEP);
            Integer end = (Integer) map.get(END);
            Boolean inclusive = (Boolean) map.get(INCLUSIVE);

            return new OfShort(start.shortValue(), step.shortValue(), end.shortValue(), inclusive);
        }
    }

    @SerializableAs("NumericRange.OfInteger")
    public static class OfInteger extends NumericRange<Integer> implements ConfigurationSerializable {
        private static void register() {
            ConfigurationSerialization.registerClass(OfInteger.class, "NumericRange.OfInteger");
        }

        @Called
        public OfInteger(int start, int step, int end, boolean inclusive) {
            super(start, step, end, inclusive);
        }

        @Called
        public int start() {
            return super.getStart();
        }

        @Called
        public int step() {
            return super.getStep();
        }

        @Called
        public int end() {
            return super.getEnd();
        }

        @Override
        public Map<String, Object> serialize() {
            Map<String, Object> map = new HashMap<>();

            map.put(START, getStart());
            map.put(STEP, getStep());
            map.put(END, getEnd());
            map.put(INCLUSIVE, isInclusive());

            return map;
        }

        public static OfInteger valueOf(Map<String, Object> map) {
            Integer start = (Integer) map.get(START);
            Integer step = (Integer) map.get(STEP);
            Integer end = (Integer) map.get(END);
            Boolean inclusive = (Boolean) map.get(INCLUSIVE);

            return new OfInteger(start, step, end, inclusive);
        }
    }

    @SerializableAs("NumericRange.OfLong")
    public static class OfLong extends NumericRange<Long> implements ConfigurationSerializable {
        private static void register() {
            ConfigurationSerialization.registerClass(OfLong.class, "NumericRange.OfLong");
        }

        public OfLong(long start, long step, long end, boolean inclusive) {
            super(start, step, end, inclusive);
        }

        public long start() {
            return super.getStart();
        }

        public long step() {
            return super.getStep();
        }

        public long end() {
            return super.getEnd();
        }

        @Override
        public Map<String, Object> serialize() {
            Map<String, Object> map = new HashMap<>();

            map.put(START, getStart().toString());
            map.put(STEP, getStep().toString());
            map.put(END, getEnd().toString());
            map.put(INCLUSIVE, isInclusive());

            return map;
        }

        public static OfLong valueOf(Map<String, Object> map) {
            String start = (String) map.get(START);
            String step = (String) map.get(STEP);
            String end = (String) map.get(END);
            Boolean inclusive = (Boolean) map.get(INCLUSIVE);

            return new OfLong(Long.parseLong(start), Long.parseLong(step), Long.parseLong(end), inclusive);
        }
    }

    @SerializableAs("NumericRange.OfBigInteger")
    public static class OfBigInteger extends NumericRange<BigInteger> implements ConfigurationSerializable {
        private static void register() {
            ConfigurationSerialization.registerClass(OfBigInteger.class, "NumericRange.OfBigInteger");
        }

        public OfBigInteger(BigInteger start, BigInteger step, BigInteger end, boolean inclusive) {
            super(start, step, end, inclusive);
        }

        public BigInteger start() {
            return super.getStart();
        }

        public BigInteger step() {
            return super.getStep();
        }

        public BigInteger end() {
            return super.getEnd();
        }

        @Override
        public Map<String, Object> serialize() {
            Map<String, Object> map = new HashMap<>();

            map.put(START, getStart().toString());
            map.put(STEP, getStep().toString());
            map.put(END, getEnd().toString());
            map.put(INCLUSIVE, isInclusive());

            return map;
        }

        public static OfBigInteger valueOf(Map<String, Object> map) {
            String start = (String) map.get(START);
            String step = (String) map.get(STEP);
            String end = (String) map.get(END);
            Boolean inclusive = (Boolean) map.get(INCLUSIVE);

            return new OfBigInteger(new BigInteger(start), new BigInteger(step), new BigInteger(end), inclusive);
        }
    }

}
