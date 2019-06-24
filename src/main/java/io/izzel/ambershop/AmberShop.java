package io.izzel.ambershop;

import com.google.inject.Inject;
import com.google.inject.Injector;
import io.izzel.ambershop.cmd.AmberCommands;
import io.izzel.ambershop.conf.AmberConfManager;
import io.izzel.ambershop.conf.AmberLocale;
import io.izzel.ambershop.data.ShopDataSource;
import io.izzel.ambershop.listener.*;
import io.izzel.ambershop.util.AmberTasks;
import io.izzel.ambershop.util.Updater;
import lombok.val;
import org.bstats.sponge.Metrics;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.GameReloadEvent;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.game.state.GameStartingServerEvent;
import org.spongepowered.api.event.game.state.GameStoppingEvent;
import org.spongepowered.api.plugin.Dependency;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.service.economy.EconomyService;

@Plugin(
        id = "ambershop",
        name = "AmberShop",
        description = "A trading plugin.",
        authors = {"IzzelAliz"},
        url = "https://github.com/IzzelAliz/AmberShop",
        dependencies = {
                @Dependency(id = "spongeapi"),
                @Dependency(id = "economylite", optional = true)
        }
)
public class AmberShop {

    public static Injector INJECTOR;
    public static AmberShop SINGLETON;

    @Inject
    public AmberShop(Injector injector) {
        INJECTOR = injector;
        SINGLETON = this;
    }

    @Inject private AmberLocale locale;
    @Inject public PluginContainer container;
    @Inject private ShopDataSource dataSource;
    @Inject private AmberTasks tasks;
    @Inject private AmberConfManager config;
    @Inject private Metrics metrics;
    @Inject private Updater updater;

    @Listener
    public void onServerStart(GameStartingServerEvent event) {
        tasks.init();
        locale.init();
        dataSource.init();
        Sponge.getCommandManager().register(this, INJECTOR.getInstance(AmberCommands.class).root()
                , "ambershop", "ashop", "as");
        val eco = ensureEconomy();
        if (config.get().shopSettings.enable && eco) {
            Sponge.getEventManager().registerListeners(this, INJECTOR.getInstance(ChunkListener.class));
            Sponge.getEventManager().registerListeners(this, INJECTOR.getInstance(ShopTradeListener.class));
            Sponge.getEventManager().registerListeners(this, INJECTOR.getInstance(ShopRemoveListener.class));
            if (config.get().shopSettings.createByInteract) {
                Sponge.getEventManager().registerListeners(this, INJECTOR.getInstance(ShopCreateListener.class));
            }
            if (config.get().shopSettings.displaySign || config.get().shopSettings.displayItem) {
                Sponge.getEventManager().registerListeners(this, INJECTOR.getInstance(DisplayListener.class));
            }
        }
        if (config.get().updater) {
            updater.init();
        }
        locale.info("startup", container.getVersion().get());
    }

    @Listener
    public void onReload(GameReloadEvent event) {
        val source = event.getCause().first(CommandSource.class).orElse(Sponge.getServer().getConsole());
        Sponge.getCommandManager().process(source, "ambershop reload");
    }

    @Listener
    public void onServerStop(GameStoppingEvent event) {
        tasks.shutdown();
    }

    private boolean ensureEconomy() {
        val provide = Sponge.getServiceManager().provide(EconomyService.class);
        if (provide.isPresent()) return true;
        else {
            Sponge.getServer().getConsole().sendMessage(locale.getText("economy-error"));
            return false;
        }
    }

}
