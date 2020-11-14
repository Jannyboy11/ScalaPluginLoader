package xyz.janboerman.scalaloader.example.java;

import org.bukkit.configuration.file.YamlConfiguration;
import scala.Option;
import scala.Some;
import scala.Tuple2;
import xyz.janboerman.scalaloader.configurationserializable.ConfigurationSerializable;
import xyz.janboerman.scalaloader.configurationserializable.DeserializationMethod;
import xyz.janboerman.scalaloader.configurationserializable.Scan;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.logging.Logger;

class Money {

    private final File saveFile;
    private final Logger logger;

    Money(ExamplePlugin examplePlugin) {
        File dataFolder = examplePlugin.getDataFolder();
        dataFolder.mkdirs();
        saveFile = new File(dataFolder, "money-serialization-test.yml");
        if (!saveFile.exists()) {
            try {
                saveFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        this.logger = examplePlugin.getLogger();
    }

    public void test() {

        Currency currency = Currency.DOLLARS;
        FieldMoney fieldMoney = new FieldMoney();       fieldMoney.amount = 9001;   fieldMoney.currency = Currency.EUROS;
        MethodMoney methodMoney = new MethodMoney();    methodMoney.setAmount(1L);  methodMoney.setCurrency(Currency.EUROS);
        CaseMoney caseMoney = CaseMoney.apply(Currency.DOLLARS, "500");
        RecordMoney recordMoney = new RecordMoney(Currency.YEN, new BigDecimal("0.5"));
        ConstructorRecordMoney ctorRecordMoney = new ConstructorRecordMoney(Currency.EUROS, new BigInteger("1337"));

        YamlConfiguration yamlConfiguration = new YamlConfiguration();
        yamlConfiguration.set("currency", currency);
        yamlConfiguration.set("fieldMoney", fieldMoney);
        yamlConfiguration.set("methodMoney", methodMoney);
        yamlConfiguration.set("caseMoney", caseMoney);
        yamlConfiguration.set("recordMoney", recordMoney);
        yamlConfiguration.set("constructorRecordMoney", ctorRecordMoney);
        try {
            yamlConfiguration.save(saveFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        yamlConfiguration = YamlConfiguration.loadConfiguration(saveFile);
        // TODO the Currency enum was not serialized correctly!
        /*  It got serialized as:
         *      currency: !!xyz.janboerman.scalaloader.example.java.Currency 'DOLLARS'
         *  Instead of
         *      currency:
         *          ==: Currency
         *          name: 'DOLLARS'
         *  :(
         *  I reproduced it in a minimal test case, and reported an issue on SpigotMC's jira:
         *  https://hub.spigotmc.org/jira/browse/SPIGOT-6234
         *  So, in the meantime, I have un-enum'ed the Currency class.
         */
        logger.info("deserialized currency = " + yamlConfiguration.get("currency"));
        logger.info("deserialized fieldMoney = " + yamlConfiguration.get("fieldMoney"));
        logger.info("deserialized methodMoney = " + yamlConfiguration.get("methodMoney"));
        logger.info("deserialized caseMoney = " + yamlConfiguration.get("caseMoney"));
        logger.info("deserialized recordMoney = " + yamlConfiguration.get("recordMoney"));
        logger.info("deserialized constructorRecordMoney = " + yamlConfiguration.get("constructorRecordMoney"));
    }

}

@ConfigurationSerializable(as = "RECORD MONKEY", scan = @Scan(Scan.Type.RECORD))
record RecordMoney(Currency currency, BigDecimal amount) {
}

@ConfigurationSerializable(as = "CONSTRUCTOR RECORD MONKEY", scan = @Scan(Scan.Type.RECORD), constructUsing = DeserializationMethod.MAP_CONSTRUCTOR)
record ConstructorRecordMoney(Currency currency, BigInteger amount) {}

@ConfigurationSerializable(as = "FIELD MONKEY", scan = @Scan(Scan.Type.FIELDS))
class FieldMoney /*implements org.bukkit.configuration.serialization.ConfigurationSerializable*/ {

    Currency currency;
    float amount = 0;

    @Scan.ExcludeProperty
    private String doesNotCount = "Invisible";

    public String toString() {
        return "FieldMoney(" + currency + ',' + amount + ')';
    }
}

@ConfigurationSerializable(as = "METHOD MONKEY", scan = @Scan(Scan.Type.GETTER_SETTER_METHODS))
class MethodMoney /*implements org.bukkit.configuration.serialization.ConfigurationSerializable*/ {

    private Currency currency;
    private long amount;

    MethodMoney() {
    }

    //adapts to 'currency'
    public Currency getCurrency() {
        return currency;
    }

    //adapts to 'currency'
    public void setCurrency(Currency currency) {
        this.currency = currency;
    }

    @Scan.IncludeProperty("amount")
    private long getAmount() {
        return amount;
    }

    @Scan.IncludeProperty("amount")
    public void setAmount(long amount) {
        this.amount = amount;
    }

    public String toString() {
        return "MethodMoney(" + getCurrency() + ',' + getAmount() + ')';
    }
}

@ConfigurationSerializable(as = "CASE MONKEY", scan = @Scan(Scan.Type.CASE_CLASS))
class CaseMoney {
    //this is not a true case class, but a public static apply and unapply are all that's needed for the bytecode transformation to work!

    private final Currency currency;
    private final String amount;

    private CaseMoney(Currency currency, String amount) {
        this.currency = currency;
        this.amount = amount;
    }

    public static CaseMoney apply(Currency currency, String amount) {
        return new CaseMoney(currency, amount);
    }

    public static Option<Tuple2<Object, Object>> unapply(CaseMoney caseMoney) {
        return Some.apply(new Tuple2<>(caseMoney.currency, caseMoney.amount));
    }

    public String toString() {
        return "CaseMoney(" + currency + ',' + amount + ')';
    }

}
