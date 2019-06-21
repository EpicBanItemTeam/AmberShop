package io.izzel.ambershop.conf;

import com.google.common.reflect.TypeToken;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.izzel.ambershop.AmberShop;
import io.izzel.ambershop.data.ShopRecord;
import lombok.SneakyThrows;
import lombok.val;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.service.user.UserStorageService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.TranslatableText;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.serializer.TextSerializers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Singleton
public class AmberLocale {

    private CommentedConfigurationNode root;

    @Inject private Logger logger;
    @Inject private AmberShop inst;
    @Inject private AmberConfManager cm;
    @Inject @ConfigDir(sharedRoot = false) private Path dir;

    @SneakyThrows
    public void init() {
        val path = dir.resolve("locale.conf");
        if (!Files.exists(path)) {
            val la = Sponge.getAssetManager().getAsset(inst, "locale/" + cm.get().language + ".conf")
                    .orElse(Sponge.getAssetManager().getAsset(inst, "locale/en_us.conf")
                            .orElseThrow(() -> new IOException("No default language present!")));
            la.copyToFile(path);
        }
        root = HoconConfigurationLoader.builder().setPath(path).build().load();
        info("language-using", cm.get().language);
    }

    @SneakyThrows
    public Text getText(String node, Object... params) {
        return Optional.ofNullable(root.getNode((Object[]) node.split("\\.")).getString())
                .map(it -> TextSerializers.FORMATTING_CODE.deserialize(replace(it, params)))
                .orElseGet(() -> Text.of(replace("Missing {0}", String.join(".", node))));
    }

    public String getString(String node, Object... params) {
        return replace(
                root.getNode((Object[]) node.split("\\."))
                        .getString(replace("Missing {0}", String.join(".", node))),
                params);
    }

    public void info(String node, Object... params) {
        logger.info(getString(node, params));
    }

    @SneakyThrows
    public List<Text> shopInfo(ShopRecord record) {
        val list = root.getNode("trade", "shop-info").getList(TypeToken.of(String.class));
        val owner = Sponge.getServiceManager().provideUnchecked(UserStorageService.class).get(record.owner).map(User::getName).orElse("");
        val type = getString("trade.type." + (record.price < 0 ? "sell" : "buy"));
        val stock = record.isUnlimited() ? getString("trade.type.unlimited") : String.valueOf(record.getStock());
        val price = record.price;
        return list.stream().map(it -> replace(it, owner, null, stock, price, type))
                .map(it -> {
                    val idx = it.indexOf("{1}");
                    if (idx != -1) {
                        val builder = Text.builder();
                        val a = it.substring(0, idx);
                        if (a.length() > 0) builder.append(TextSerializers.FORMATTING_CODE.deserialize(a));
                        builder.append(Text.builder().append(itemName(record.getItemType().createStack()))
                                .onHover(TextActions.showItem(record.getItemType())).build());
                        val b = it.substring(idx + 3);
                        if (b.length() > 0) builder.append(TextSerializers.FORMATTING_CODE.deserialize(b));
                        return builder.build();
                    } else return TextSerializers.FORMATTING_CODE.deserialize(it);
                }).collect(Collectors.toList());
    }

    public Text itemName(ItemStack itemStack) {
        return itemStack.get(Keys.DISPLAY_NAME).orElse(TranslatableText.of(itemStack.getTranslation()));
    }

    // this is a lot faster than the internal formatter
    private static String replace(String template, Object... args) {
        if (args.length == 0 || template.length() == 0) {
            return template;
        }
        val arr = template.toCharArray();
        val stringBuilder = new StringBuilder(template.length());
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] == '{' && Character.isDigit(arr[Math.min(i + 1, arr.length - 1)])
                    && arr[Math.min(i + 1, arr.length - 1)] - '0' < args.length
                    && arr[Math.min(i + 2, arr.length - 1)] == '}'
                    && args[arr[i + 1] - '0'] != null) {
                stringBuilder.append(args[arr[i + 1] - '0']);
                i += 2;
            } else {
                stringBuilder.append(arr[i]);
            }
        }
        return stringBuilder.toString();
    }

}
