package io.izzel.ambershop.module;

import com.google.common.reflect.TypeToken;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.izzel.amber.commons.i18n.AmberLocale;
import io.izzel.ambershop.AmberShop;
import io.izzel.ambershop.conf.AmberConfManager;
import lombok.SneakyThrows;
import lombok.val;
import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.event.game.GameRegistryEvent;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.annotation.NonnullByDefault;
import org.spongepowered.api.world.World;
import team.ebi.epicbanitem.api.CheckRuleService;
import team.ebi.epicbanitem.api.CheckRuleTrigger;

@Singleton
public class EbiModule {

    private final EbiInterface impl;

    @Inject
    public EbiModule(AmberConfManager acm, AmberLocale locale, Logger logger) {
        val ebi = acm.get().shopSettings.blacklistSettings;
        if (ebi.enable && checkEbiVersion(logger)) {
            impl = new EbiImpl(locale ,ebi.checkCreate, ebi.checkTrade);
            logger.info("Using EpicBanItem for item blacklist.");
        } else {
            impl = new AbstractImpl();
        }
    }

    private boolean checkEbiVersion(Logger logger) {
        try {
            Class.forName("team.ebi.epicbanitem.api.CheckRuleService");
            return true;
        } catch (Exception e) {
            logger.error("Item blacklist module requires EpicBanItem 0.4.0+ .");
            return false;
        }
    }

    public boolean checkCreate(ItemStack item, World world, Subject subject) {
        return impl.checkCreate(item, world, subject);
    }

    public boolean checkTrade(ItemStack item, World world, Subject subject) {
        return impl.checkTrade(item, world, subject);
    }

    private interface EbiInterface {
        boolean checkCreate(ItemStack item, World world, Subject subject);

        boolean checkTrade(ItemStack item, World world, Subject subject);
    }

    private static class AbstractImpl implements EbiInterface {

        @Override
        public boolean checkCreate(ItemStack item, World world, Subject subject) {
            return true;
        }

        @Override
        public boolean checkTrade(ItemStack item, World world, Subject subject) {
            return true;
        }
    }

    private static class EbiImpl implements EbiInterface {

        private final boolean create;
        private final boolean trade;
        private final CheckRuleService service;
        private final CheckRuleTrigger createTrigger;
        private final CheckRuleTrigger tradeTrigger;

        @SneakyThrows
        public EbiImpl(AmberLocale locale, boolean create, boolean trade) {
            this.create = create;
            this.trade = trade;
            service = Sponge.getServiceManager().provideUnchecked(CheckRuleService.class);
            Sponge.getEventManager().registerListener(AmberShop.SINGLETON, new TypeToken<GameRegistryEvent.Register<CheckRuleTrigger>>() {},  this::onRegistryTrigger);
            createTrigger = new TriggerImpl(locale, "create", "ambershop:create");
            tradeTrigger = new TriggerImpl(locale, "trade", "ambershop:trade");
        }

        public void onRegistryTrigger(GameRegistryEvent.Register<CheckRuleTrigger> event) {
            event.register(createTrigger);
            event.register(tradeTrigger);
        }

        @Override
        public boolean checkCreate(ItemStack item, World world, Subject subject) {
            return create && !service.check(item, world, createTrigger, subject).isBanned();
        }

        @Override
        public boolean checkTrade(ItemStack item, World world, Subject subject) {
            return trade && !service.check(item, world, tradeTrigger, subject).isBanned();
        }

        @NonnullByDefault
        private static class TriggerImpl implements CheckRuleTrigger {

            private final AmberLocale locale;
            private final String name;
            private final String id;

            private TriggerImpl(AmberLocale locale, String name, String id) {
                this.locale = locale;
                this.name = name;
                this.id = id;
            }

            @Override
            public String getId() {
                return id;
            }

            @Override
            public String getName() {
                return name;
            }

            @Override
            public Text toText() {
                return locale.get("module.ebi.trigger." + name).orElse(Text.of(name));
            }

            @Override
            public String toString() {
                return name;
            }
        }

    }
}
