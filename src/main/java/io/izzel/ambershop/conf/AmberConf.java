package io.izzel.ambershop.conf;

import com.google.common.collect.ImmutableList;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

import java.util.List;
import java.util.Locale;

@ConfigSerializable
public class AmberConf {

    @Setting(comment = "Default language is en_us.")
    public String language = Locale.getDefault().toString().toLowerCase();

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

        @Setting(comment = "Do protect shop.")
        public boolean protectShops = true;

        @Setting(comment = "How long does the input(amount) expires. In seconds.")
        public int inputExpireTime = 15;

        @Setting(comment = "Do show sign on the chest shops.")
        public boolean displaySign = true;

        @Setting(comment = "Do show items beyond the chest shops.")
        public boolean displayItem = true;

        @Setting(comment = "Set what the sign shows.\n" +
                "Placeholders are:\n" +
                "  %shop_owner% for owner's name\n" +
                "  %shop_price% for the price\n" +
                "  %shop_stock% for the stock, or unlimited(editable)\n" +
                "  %shop_item% for the display name(if present) of the itemName, or the default name of the itemName\n" +
                "  &shop_type& for buy or sell(editable)")
        public List<String> signInfo = ImmutableList.of("[AmberShop]", "%shop_type% %shop_item%", "Price: %shop_price%", "Stock: %shop_stock%");

    }

}
