package io.izzel.ambershop.util;

import com.google.gson.JsonParser;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.izzel.amber.commons.i18n.AmberLocale;
import io.izzel.ambershop.AmberShop;
import lombok.val;
import org.spongepowered.api.Sponge;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.TimeUnit;

@Singleton
public class Updater implements Runnable {

    private static final String API_URL = "https://api.github.com/repos/IzzelAliz/AmberShop/releases";

    @Inject private AmberTasks tasks;
    @Inject private AmberLocale locale;

    public void init() {
        tasks.async().scheduleAtFixedRate(this, 0, 12, TimeUnit.HOURS);
    }

    @Override
    public void run() {
        try {
            val url = new URL(API_URL);
            val conn = ((HttpURLConnection) url.openConnection());
            val node = new JsonParser().parse(new InputStreamReader(conn.getInputStream())).getAsJsonArray().get(0).getAsJsonObject();
            val releaseUrl = node.get("html_url").getAsString();
            val newVer = node.get("tag_name").getAsString();
            val releaseDate = node.get("published_at").getAsString();
            val curVer = Sponge.getPluginManager().fromInstance(AmberShop.SINGLETON).orElseThrow(IllegalStateException::new)
                .getVersion().orElseThrow(IllegalStateException::new);
            if (!curVer.equals(newVer)) {
                locale.log("updater", newVer, releaseDate, releaseUrl);
            }
        } catch (Throwable ignored) {
        }
    }
}
