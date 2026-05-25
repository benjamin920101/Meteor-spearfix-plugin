package com.spearfix.addon;

import com.mojang.logging.LogUtils;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.slf4j.Logger;

import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;

/**
 * SpearFix Meteor Addon
 *
 * 以 Java Agent + ASM 方式修復 DecoderHandler.decode 崩潰問題，
 * 當解碼異常時跳過錯誤封包，防止客戶端斷開連線。
 *
 * 使用方法：
 *   在 JVM 參數加入 -javaagent:spearfix.jar
 *   （例如在 PrismLauncher → 編輯執行個體 → Java 參數）
 */
public class SpearFixAddon extends MeteorAddon {
    public static final String VERSION = "1.0.3";
    public static final Logger LOG = LogUtils.getLogger();
    public static final Category CATEGORY = new Category("SpearFix");

    @Override
    public void onInitialize() {
        LOG.info("Initializing SpearFix Addon v{}", VERSION);
        LOG.info("Make sure to add -javaagent:spearfix.jar to your JVM arguments!");

        // 檢查是否有 -javaagent 已載入
        if (!isAgentLoaded()) {
            LOG.warn("SpearFix agent NOT detected. Please add -javaagent:spearfix.jar");
        }
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

    /** 檢查 JVM 是否已載入 SpearFixAgent */
    private static boolean isAgentLoaded() {
        // 透過 ManagementFactory 看是否有 premain-class 參數
        for (String arg : ManagementFactory.getRuntimeMXBean().getInputArguments()) {
            if (arg.contains("SpearFixAgent") || arg.contains("spearfix")) {
                return true;
            }
        }
        return false;
    }
}
