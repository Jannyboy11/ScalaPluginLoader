package xyz.janboerman.scalaloader.configurationserializable.runtime.types;

import java.text.SimpleDateFormat;
import java.time.chrono.HijrahEra;
import java.time.chrono.MinguoEra;
import java.time.chrono.ThaiBuddhistEra;
import java.time.temporal.ChronoField;
import java.util.Map;
import java.util.Objects;

import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.configuration.serialization.SerializableAs;

import xyz.janboerman.scalaloader.compat.Compat;
import xyz.janboerman.scalaloader.configurationserializable.runtime.Adapter;

public class DateTime {

    private DateTime() {
    }

    public static void registerWithConfigurationSerialization() {
        Instant.register();
        ZonedDateTime.register();
        LocalDateTime.register();
        LocalTime.register();
        LocalDate.register();
        Year.register();
        YearMonth.register();
        OffsetDateTime.register();
        OffsetTime.register();
        MinguoDate.register();
        JapaneseDate.register();
        HijrahDate.register();
        ThaiBuddhistDate.register();
        Duration.register();
        Period.register();
        Date.register();
    }

    @SerializableAs("Instant")
    public static class Instant implements Adapter<java.time.Instant> {
        private static void register() {
            ConfigurationSerialization.registerClass(Instant.class, "Instant");
        }

        public final java.time.Instant value;

        public Instant(java.time.Instant value) {
            this.value = value;
        }

        @Override
        public Map<String, Object> serialize() {
            return Compat.singletonMap("value", value == null ? null : value.toString());
        }

        public static Instant deserialize(Map<String, Object> map) {
            String value = (String) map.get("value");
            return new Instant(value == null ? null : java.time.Instant.parse(value));
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(value);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (!(obj instanceof Instant)) return false;

            Instant that = (Instant) obj;
            return Objects.equals(this.value, that.value);
        }

        @Override
        public String toString() {
            return Objects.toString(value);
        }

        @Override
        public java.time.Instant getValue() {
            return value;
        }
    }

    @SerializableAs("ZonedDateTime")
    public static class ZonedDateTime implements Adapter<java.time.ZonedDateTime> {
        private static void register() {
            ConfigurationSerialization.registerClass(ZonedDateTime.class, "ZonedDateTime");
        }

        public final java.time.ZonedDateTime value;

        public ZonedDateTime(java.time.ZonedDateTime value) {
            this.value = value;
        }

        @Override
        public Map<String, Object> serialize() {
            return Compat.singletonMap("value", value == null ? null : value.toString());
        }

        public static ZonedDateTime deserialize(Map<String, Object> map) {
            String value = (String) map.get("value");
            return new ZonedDateTime(value == null ? null : java.time.ZonedDateTime.parse(value));
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(value);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (!(obj instanceof ZonedDateTime)) return false;

            ZonedDateTime that = (ZonedDateTime) obj;
            return Objects.equals(this.value, that.value);
        }

        @Override
        public String toString() {
            return Objects.toString(value);
        }

        @Override
        public java.time.ZonedDateTime getValue() {
            return value;
        }
    }

    @SerializableAs("LocalDateTime")
    public static class LocalDateTime implements Adapter<java.time.LocalDateTime> {
        private static void register() {
            ConfigurationSerialization.registerClass(LocalDateTime.class, "LocalDateTime");
        }

        public final java.time.LocalDateTime value;

        public LocalDateTime(java.time.LocalDateTime value) {
            this.value = value;
        }

        @Override
        public Map<String, Object> serialize() {
            return Compat.singletonMap("value", value == null ? null : value.toString());
        }

        public static LocalDateTime deserialize(Map<String, Object> map) {
            String serialised = (String) map.get("value");
            return new LocalDateTime(serialised == null ? null : java.time.LocalDateTime.parse(serialised));
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(value);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (!(obj instanceof LocalDateTime)) return false;

            LocalDateTime that = (LocalDateTime) obj;
            return Objects.equals(this.value, that.value);
        }

        @Override
        public String toString() {
            return Objects.toString(value);
        }

        @Override
        public java.time.LocalDateTime getValue() {
            return value;
        }
    }

    @SerializableAs("LocalTime")
    public static class LocalTime implements Adapter<java.time.LocalTime> {
        private static void register() {
            ConfigurationSerialization.registerClass(LocalTime.class, "LocalTime");
        }

        public final java.time.LocalTime value;

        public LocalTime(java.time.LocalTime value) {
            this.value = value;
        }

        @Override
        public Map<String, Object> serialize() {
            return Compat.singletonMap("value", value == null ? null : value.toString());
        }

        public static LocalTime deserialize(Map<String, Object> map) {
            String value = (String) map.get("value");
            return new LocalTime(value == null ? null : java.time.LocalTime.parse(value));
        }


        @Override
        public int hashCode() {
            return Objects.hashCode(value);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (!(obj instanceof LocalTime)) return false;

            LocalTime that = (LocalTime) obj;
            return Objects.equals(this.value, that.value);
        }

        @Override
        public String toString() {
            return Objects.toString(value);
        }

        @Override
        public java.time.LocalTime getValue() {
            return value;
        }
    }

    @SerializableAs("LocalDate")
    public static class LocalDate implements Adapter<java.time.LocalDate> {
        private static void register() {
            ConfigurationSerialization.registerClass(LocalDate.class, "LocalDate");
        }

        public final java.time.LocalDate value;

        public LocalDate(java.time.LocalDate value) {
            this.value = value;
        }

        @Override
        public Map<String, Object> serialize() {
            return Compat.singletonMap("value", value == null ? null : value.toString());
        }

        public static LocalDate deserialize(Map<String, Object> map) {
            String value = (String) map.get("value");
            return new LocalDate(value == null ? null : java.time.LocalDate.parse(value));
        }


        @Override
        public int hashCode() {
            return Objects.hashCode(value);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (!(obj instanceof LocalDate)) return false;

            LocalDate that = (LocalDate) obj;
            return Objects.equals(this.value, that.value);
        }

        @Override
        public String toString() {
            return Objects.toString(value);
        }

        @Override
        public java.time.LocalDate getValue() {
            return value;
        }
    }

    @SerializableAs("Year")
    public static class Year implements Adapter<java.time.Year> {
        private static void register() {
            ConfigurationSerialization.registerClass(Year.class, "Year");
        }

        public final java.time.Year value;

        public Year(java.time.Year value) {
            this.value = value;
        }

        public Map<String, Object> serialize() {
            return Compat.singletonMap("value", value == null ? null : value.toString());
        }

        public static Year deserialize(Map<String, Object> map) {
            Object value = map.get("value");
            return new Year(value == null ? null : java.time.Year.parse(value.toString()));
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(value);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (!(obj instanceof Year)) return false;

            Year that = (Year) obj;
            return Objects.equals(this.value, that.value);
        }

        @Override
        public String toString() {
            return Objects.toString(value);
        }

        @Override
        public java.time.Year getValue() {
            return value;
        }
    }

    @SerializableAs("YearMonth")
    public static class YearMonth implements Adapter<java.time.YearMonth> {
        private static void register() {
            ConfigurationSerialization.registerClass(YearMonth.class, "YearMonth");
        }

        public final java.time.YearMonth value;

        public YearMonth(java.time.YearMonth value) {
            this.value = value;
        }

        public Map<String, Object> serialize() {
            return Compat.singletonMap("value", value == null ? null : value.toString());
        }

        public static YearMonth deserialize(Map<String, Object> map) {
            Object value = map.get("value");
            return new YearMonth(value == null ? null : java.time.YearMonth.parse(value.toString()));
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(value);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (!(obj instanceof YearMonth)) return false;

            YearMonth that = (YearMonth) obj;
            return Objects.equals(this.value, that.value);
        }

        @Override
        public String toString() {
            return Objects.toString(value);
        }

        @Override
        public java.time.YearMonth getValue() {
            return value;
        }
    }

    @SerializableAs("OffsetDateTime")
    public static class OffsetDateTime implements Adapter<java.time.OffsetDateTime> {
        private static void register() {
            ConfigurationSerialization.registerClass(OffsetDateTime.class, "OffsetDateTime");
        }

        public final java.time.OffsetDateTime value;

        public OffsetDateTime(java.time.OffsetDateTime value) {
            this.value = value;
        }

        @Override
        public Map<String, Object> serialize() {
            return Compat.singletonMap("value", value == null ? null : value.toString());
        }

        public static OffsetDateTime deserialize(Map<String, Object> map) {
            String value = (String) map.get("value");
            return new OffsetDateTime(value == null ? null : java.time.OffsetDateTime.parse(value));
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(value);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (!(obj instanceof OffsetDateTime)) return false;

            OffsetDateTime that = (OffsetDateTime) obj;
            return Objects.equals(this.value, that.value);
        }

        @Override
        public String toString() {
            return Objects.toString(value);
        }

        @Override
        public java.time.OffsetDateTime getValue() {
            return value;
        }
    }

    @SerializableAs("OffsetTime")
    public static class OffsetTime implements Adapter<java.time.OffsetTime> {
        private static void register() {
            ConfigurationSerialization.registerClass(OffsetTime.class, "OffsetTime");
        }

        public final java.time.OffsetTime value;

        public OffsetTime(java.time.OffsetTime value) {
            this.value = value;
        }

        @Override
        public Map<String, Object> serialize() {
            return Compat.singletonMap("value", value == null ? null : value.toString());
        }

        public static OffsetTime deserialize(Map<String, Object> map) {
            String value = (String) map.get("value");
            return new OffsetTime(value == null ? null : java.time.OffsetTime.parse(value));
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(value);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (!(obj instanceof OffsetTime)) return false;

            OffsetTime that = (OffsetTime) obj;
            return Objects.equals(this.value, that.value);
        }

        @Override
        public String toString() {
            return Objects.toString(value);
        }

        @Override
        public java.time.OffsetTime getValue() {
            return value;
        }
    }

    @SerializableAs("MinguoDate")
    public static class MinguoDate implements Adapter<java.time.chrono.MinguoDate> {
        private static void register() {
            ConfigurationSerialization.registerClass(MinguoDate.class, "MinguoDate");
        }

        public final java.time.chrono.MinguoDate value;

        public MinguoDate(java.time.chrono.MinguoDate value) {
            this.value = value;
        }

        @Override
        public Map<String, Object> serialize() {
            if (value != null) {
                return Compat.mapOf(
                        Compat.mapEntry("era", MinguoEra.of(value.get(ChronoField.ERA)).name()),
                        Compat.mapEntry("year", value.get(ChronoField.YEAR_OF_ERA)),
                        Compat.mapEntry("month", value.get(ChronoField.MONTH_OF_YEAR)),
                        Compat.mapEntry("day", value.get(ChronoField.DAY_OF_MONTH))
                );
            } else {
                return Compat.emptyMap();
            }
        }

        public static MinguoDate deserialize(Map<String, Object> map) {
            String era = (String) map.get("era");
            Integer year = (Integer) map.get("year");
            Integer month = (Integer) map.get("month");
            Integer day = (Integer) map.get("day");
            if (era != null && year != null && month != null && day != null) {
                java.time.chrono.MinguoDate value = java.time.chrono.MinguoDate.of(0, 0, 0);
                value = value.with(ChronoField.ERA, MinguoEra.valueOf(era).getValue());
                value = value.with(ChronoField.YEAR_OF_ERA, year.longValue());
                value = value.with(ChronoField.MONTH_OF_YEAR, month.longValue());
                value = value.with(ChronoField.DAY_OF_MONTH, day.longValue());
                return new MinguoDate(value);
            } else {
                return new MinguoDate(null);
            }
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(value);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (!(obj instanceof MinguoDate)) return false;

            MinguoDate that = (MinguoDate) obj;
            return Objects.equals(this.value, that.value);
        }

        @Override
        public String toString() {
            return Objects.toString(value);
        }

        @Override
        public java.time.chrono.MinguoDate getValue() {
            return value;
        }
    }

    @SerializableAs("JapaneseDate")
    public static class JapaneseDate implements Adapter<java.time.chrono.JapaneseDate> {
        private static void register() {
            ConfigurationSerialization.registerClass(JapaneseDate.class, "JapaneseDate");
        }

        public final java.time.chrono.JapaneseDate value;

        public JapaneseDate(java.time.chrono.JapaneseDate value) {
            this.value = value;
        }

        @Override
        public Map<String, Object> serialize() {
            if (value != null) {
                return Compat.mapOf(
                        Compat.mapEntry("era", value.get(ChronoField.ERA)),
                        Compat.mapEntry("year", value.get(ChronoField.YEAR_OF_ERA)),
                        Compat.mapEntry("month", value.get(ChronoField.MONTH_OF_YEAR)),
                        Compat.mapEntry("day", value.get(ChronoField.DAY_OF_MONTH))
                );
            } else {
                return Compat.emptyMap();
            }
        }

        public static JapaneseDate deserialize(Map<String, Object> map) {
            Integer era = (Integer) map.get("era");
            Integer year = (Integer) map.get("year");
            Integer month = (Integer) map.get("month");
            Integer day = (Integer) map.get("day");
            if (era != null && year != null && month != null && day != null) {
                java.time.chrono.JapaneseDate value = java.time.chrono.JapaneseDate.of(0, 0, 0);
                value = value.with(ChronoField.ERA, era.longValue());
                value = value.with(ChronoField.YEAR_OF_ERA, year.longValue());
                value = value.with(ChronoField.MONTH_OF_YEAR, month.longValue());
                value = value.with(ChronoField.DAY_OF_MONTH, day.longValue());
                return new JapaneseDate(value);
            } else {
                return new JapaneseDate(null);
            }
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(value);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (!(obj instanceof JapaneseDate)) return false;

            JapaneseDate that = (JapaneseDate) obj;
            return Objects.equals(this.value, that.value);
        }

        @Override
        public String toString() {
            return Objects.toString(value);
        }

        @Override
        public java.time.chrono.JapaneseDate getValue() {
            return value;
        }
    }

    @SerializableAs("HijrahDate")
    public static class HijrahDate implements Adapter<java.time.chrono.HijrahDate> {
        private static void register() {
            ConfigurationSerialization.registerClass(HijrahDate.class, "HijrahDate");
        }

        public final java.time.chrono.HijrahDate value;

        public HijrahDate(java.time.chrono.HijrahDate value) {
            this.value = value;
        }

        @Override
        public Map<String, Object> serialize() {
            if (value != null) {
                return Compat.mapOf(
                        Compat.mapEntry("era", HijrahEra.of(value.get(ChronoField.ERA)).name()),
                        Compat.mapEntry("year", value.get(ChronoField.YEAR_OF_ERA)),
                        Compat.mapEntry("month", value.get(ChronoField.MONTH_OF_YEAR)),
                        Compat.mapEntry("day", value.get(ChronoField.DAY_OF_MONTH))
                );
            } else {
                return Compat.emptyMap();
            }
        }

        public static HijrahDate deserialize(Map<String, Object> map) {
            String era = (String) map.get("era");
            Integer year = (Integer) map.get("year");
            Integer month = (Integer) map.get("month");
            Integer day = (Integer) map.get("day");
            if (era != null && year != null && month != null && day != null) {
                java.time.chrono.HijrahDate value = java.time.chrono.HijrahDate.of(0, 0, 0);
                value = value.with(ChronoField.ERA, HijrahEra.valueOf(era).getValue());
                value = value.with(ChronoField.YEAR_OF_ERA, year.longValue());
                value = value.with(ChronoField.MONTH_OF_YEAR, month.longValue());
                value = value.with(ChronoField.DAY_OF_MONTH, day.longValue());
                return new HijrahDate(value);
            } else {
                return new HijrahDate(null);
            }
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(value);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (!(obj instanceof HijrahDate)) return false;

            HijrahDate that = (HijrahDate) obj;
            return Objects.equals(this.value, that.value);
        }

        @Override
        public String toString() {
            return Objects.toString(value);
        }

        @Override
        public java.time.chrono.HijrahDate getValue() {
            return value;
        }
    }

    @SerializableAs("ThaiBuddhistDate")
    public static class ThaiBuddhistDate implements Adapter<java.time.chrono.ThaiBuddhistDate> {
        private static void register() {
            ConfigurationSerialization.registerClass(ThaiBuddhistDate.class, "ThaiBuddhistDate");
        }

        public final java.time.chrono.ThaiBuddhistDate value;

        public ThaiBuddhistDate(java.time.chrono.ThaiBuddhistDate value) {
            this.value = value;
        }

        @Override
        public Map<String, Object> serialize() {
            if (value != null) {
                return Compat.mapOf(
                        Compat.mapEntry("era", ThaiBuddhistEra.of(value.get(ChronoField.ERA)).name()),
                        Compat.mapEntry("year", value.get(ChronoField.YEAR_OF_ERA)),
                        Compat.mapEntry("month", value.get(ChronoField.MONTH_OF_YEAR)),
                        Compat.mapEntry("day", value.get(ChronoField.DAY_OF_MONTH))
                );
            } else {
                return Compat.emptyMap();
            }
        }

        public static ThaiBuddhistDate deserialize(Map<String, Object> map) {
            String era = (String) map.get("era");
            Integer year = (Integer) map.get("year");
            Integer month = (Integer) map.get("month");
            Integer day = (Integer) map.get("day");
            if (era != null && year != null && month != null && day != null) {
                java.time.chrono.ThaiBuddhistDate value = java.time.chrono.ThaiBuddhistDate.of(0, 0, 0);
                value = value.with(ChronoField.ERA, ThaiBuddhistEra.valueOf(era).getValue());
                value = value.with(ChronoField.YEAR_OF_ERA, year.longValue());
                value = value.with(ChronoField.MONTH_OF_YEAR, month.longValue());
                value = value.with(ChronoField.DAY_OF_MONTH, day.longValue());
                return new ThaiBuddhistDate(value);
            } else {
                return new ThaiBuddhistDate(null);
            }
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(value);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (!(obj instanceof ThaiBuddhistDate)) return false;

            ThaiBuddhistDate that = (ThaiBuddhistDate) obj;
            return Objects.equals(this.value, that.value);
        }

        @Override
        public String toString() {
            return Objects.toString(value);
        }

        @Override
        public java.time.chrono.ThaiBuddhistDate getValue() {
            return value;
        }
    }

    @SerializableAs("Duration")
    public static class Duration implements Adapter<java.time.Duration> {
        private static void register() {
            ConfigurationSerialization.registerClass(Duration.class, "Duration");
        }

        public final java.time.Duration value;

        public Duration(java.time.Duration value) {
            this.value = value;
        }

        @Override
        public Map<String, Object> serialize() {
            return Compat.singletonMap("value", value == null ? null : value.toString());
        }

        public static Duration deserialize(Map<String, Object> map) {
            String value = (String) map.get("value");
            return new Duration(value == null ? null : java.time.Duration.parse(value));
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(value);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (!(obj instanceof Duration)) return false;

            Duration that = (Duration) obj;
            return Objects.equals(this.value, that.value);
        }

        @Override
        public String toString() {
            return Objects.toString(value);
        }

        @Override
        public java.time.Duration getValue() {
            return value;
        }
    }

    @SerializableAs("Period")
    public static class Period implements Adapter<java.time.Period> {
        private static void register() {
            ConfigurationSerialization.registerClass(Period.class, "Period");
        }

        public final java.time.Period value;

        public Period(java.time.Period value) {
            this.value = value;
        }

        @Override
        public Map<String, Object> serialize() {
            return Compat.singletonMap("value", value == null ? null : value.toString());
        }

        public static Period deserialize(Map<String, Object> map) {
            String value = (String) map.get("value");
            return new Period(value == null ? null : java.time.Period.parse(value));
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(value);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (!(obj instanceof Period)) return false;

            Period that = (Period) obj;
            return Objects.equals(this.value, that.value);
        }

        @Override
        public String toString() {
            return Objects.toString(value);
        }

        @Override
        public java.time.Period getValue() {
            return value;
        }
    }

    @SerializableAs("Date")
    public static class Date implements Adapter<java.util.Date> {
        private static final String FORMAT = "yyyy-MM-dd HH:mm:ss.SSSZZ";
        private static void register() {
            ConfigurationSerialization.registerClass(Date.class, "Date");
        }

        public final java.util.Date value;

        public Date(java.util.Date value) {
            this.value = value;
        }

        @Override
        public Map<String, Object> serialize() {
            String serialised = value == null ? null : new SimpleDateFormat(FORMAT).format(value);
            return Compat.singletonMap("value", serialised);
        }

        public static Date deserialize(Map<String, Object> map) throws Exception {
            String serialised = (String) map.get("value");
            return new Date(new SimpleDateFormat(FORMAT).parse(serialised));
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(value);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (!(obj instanceof Date)) return false;

            Date that = (Date) obj;
            return Objects.equals(this.value, that.value);
        }

        @Override
        public String toString() {
            return Objects.toString(value);
        }

        @Override
        public java.util.Date getValue() {
            return value;
        }
    }
}
