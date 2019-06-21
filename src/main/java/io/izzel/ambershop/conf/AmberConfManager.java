package io.izzel.ambershop.conf;

import com.google.common.reflect.TypeToken;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.SneakyThrows;
import lombok.val;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import org.slf4j.Logger;
import org.spongepowered.api.config.DefaultConfig;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

@Singleton
public class AmberConfManager {

    @Inject
    @DefaultConfig(sharedRoot = false)
    private Path cp;

    @Inject private Logger logger;

    private AmberConf conf;

    @SneakyThrows
    private void loadNewAmberConf() {
        if (!Files.exists(cp)) {
            conf = new AmberConf();
            save();
        } else {
            val load = HoconConfigurationLoader.builder().setPath(cp).build().load();
            conf = load.getValue(TypeToken.of(AmberConf.class));
        }
        Objects.requireNonNull(conf);
        logger.info("Configurations loaded ...");
    }

    public AmberConf get() {
        if (conf == null) loadNewAmberConf();
        return conf;
    }

    public AmberConf reload() {
        conf = null;
        return get();
    }

    @SneakyThrows
    public void save() {
        val loader = HoconConfigurationLoader.builder().setPath(cp).build();
        val emptyNode = loader.createEmptyNode();
        emptyNode.setValue(TypeToken.of(AmberConf.class), conf);
        loader.save(emptyNode);
    }

}
