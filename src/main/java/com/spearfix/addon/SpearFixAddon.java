package com.spearfix.addon;

import com.mojang.logging.LogUtils;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.slf4j.Logger;

import java.lang.management.ManagementFactory;

/**
 * SpearFix Meteor Addon
 *
 * 以 Mixin 方式攔截 ClientConnection.exceptionCaught 中的 DecoderException，
 * 當解碼異常時跳過錯誤封包，防止客戶端斷開連線。
 * 無需額外 JVM 參數，安裝即用。
 */
public class SpearFixAddon extends MeteorAddon {
    public static final String VERSION = "1.1.0";
    public static final Logger LOG = LogUtils.getLogger();
    public static final Category CATEGORY = new Category("SpearFix");

    @Override
    public void onInitialize() {
        LOG.info("Initializing SpearFix Addon v{}", VERSION);
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(CATEGORY);
    }

    @Override
    public String getPackage() {
        return "com.spearfix.addon";
    }

    @Override
    public GithubRepo getRepo() {
        return new GithubRepo("benjamin920101", "Meteor-spearfix-plugin");
    }
}
