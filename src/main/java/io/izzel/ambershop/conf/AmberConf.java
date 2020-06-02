package io.izzel.ambershop.conf;

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

@ConfigSerializable
public class AmberConf {

    @Setting(comment = "Check for update.")
    public boolean updater = true;

    @Setting(comment = "Default is a H2 database.\n" +
        "If you would like to use a MySQL database, use 'jdbc:mysql://user:pass@host:port/database' instead.")
    public String jdbcUrl = "jdbc:h2:./config/ambershop/ambershop";

    @Setting(comment = "Settings on chest-based shops.")
    public ShopConfig shopSettings = new ShopConfig();

    @ConfigSerializable
    public static class ShopConfig {

        @Setting(comment = "Do enable the shop system.")
        public boolean enable = true;

        @Setting(comment = "Allow players create shop by left-clicking a chest.")
        public boolean createByInteract = true;

        @Setting(comment = "Create shops only when player is sneaking.")
        public boolean createOnlySneaking = false;

        @Setting(comment = "Do protect shop.")
        public boolean protectShops = true;

        @Setting(comment = "How long does the input(amount) expires. In seconds.")
        public int inputExpireTime = 15;

        @Setting(comment = "Do show sign on the chest shops.")
        public boolean displaySign = true;

        @Setting(comment = "Do show items beyond the chest shops.")
        public boolean displayItem = true;

        @Setting(comment = "Set default shop limit player can create.\n" +
            "You can use \"lp user|group xxx meta set ambershop.max-shop <amount>\" to modify the limit of shops per player/group.\n" +
            "Set to -1 to disable limit.")
        public int maxShops = -1;

        @Setting(comment = "Tax related settings.")
        public Tax taxSettings = new Tax();

        @Setting(comment = "Item blacklist related settings.")
        public Ebi blacklistSettings = new Ebi();

        @ConfigSerializable
        public static class Tax {

            @Setting(comment = "Enable taxes.")
            public boolean enable = false;

            @Setting(comment = "Tax rate.")
            public double tax = 0.02;

        }

        @ConfigSerializable
        public static class Ebi {

            @Setting(comment = "Enable item blacklist using EpicBanItem, requires version >= 0.4.0.")
            public boolean enable = false;

            @Setting(comment = "Check EBI when creating a shop.")
            public boolean checkCreate = true;

            @Setting(comment = "Check EBI when player is trading.")
            public boolean checkTrade = true;

        }

    }

}
