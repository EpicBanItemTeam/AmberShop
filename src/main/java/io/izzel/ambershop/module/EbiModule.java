package io.izzel.ambershop.module;

import com.github.euonmyoji.epicbanitem.api.CheckRuleService;
import com.github.euonmyoji.epicbanitem.api.CheckRuleTrigger;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.izzel.ambershop.conf.AmberConfManager;
import lombok.SneakyThrows;
import lombok.val;
import org.slf4j.Logger;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.world.World;

@Singleton
public class EbiModule {

    private final EbiInterface impl;

    @Inject
    public EbiModule(AmberConfManager acm, Logger logger) {
        val ebi = acm.get().shopSettings.blacklistSettings;
        if (ebi.enable && checkEbiVersion(logger)) {
            impl = new EbiImpl(ebi.checkCreate, ebi.checkTrade);
            logger.info("Using EpicBanItem for item blacklist.");
        } else {
            impl = new AbstractImpl();
        }
    }

    private boolean checkEbiVersion(Logger logger) {
        try {
            Class.forName("com.github.euonmyoji.epicbanitem.api.CheckRuleService");
            return true;
        } catch (Exception e) {
            logger.error("Item blacklist module requires EpicBanItem 0.3.2+ .");
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
        public EbiImpl(boolean create, boolean trade) {
            this.create = create;
            this.trade = trade;
            service = CheckRuleService.instance();
            createTrigger = service.getTrigger("as_create", true).orElseThrow(Exception::new);
            tradeTrigger = service.getTrigger("as_trade", true).orElseThrow(Exception::new);
        }

        @Override
        public boolean checkCreate(ItemStack item, World world, Subject subject) {
            return create && !service.check(item, world, createTrigger, subject).isBanned();
        }

        @Override
        public boolean checkTrade(ItemStack item, World world, Subject subject) {
            return trade && !service.check(item, world, tradeTrigger, subject).isBanned();
        }

    }
}
