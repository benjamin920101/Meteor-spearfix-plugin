package com.spearfix.addon;

import com.mojang.logging.LogUtils;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;

public class TranslateAddon extends MeteorAddon {
    public static final String VERSION = "1.0.2";
    public static final Logger LOG = LogUtils.getLogger();
    public static final Category CATEGORY = new Category("SpearFix");

    public static final MinecraftClient MC = MeteorClient.mc;
    @Override
    public void onInitialize() {
        LOG.info("Initializing SpearFix Addon");
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
        return new GithubRepo("dingzhen-vape", "Meteor-spearfix-plugin");
    }
}
