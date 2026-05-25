package com.spearfix.addon;

import com.mojang.logging.LogUtils;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.slf4j.Logger;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;

/**
 * SpearFix Meteor Addon
 *
 * 以 Java Agent + ASM 方式修復 DecoderHandler.decode 崩潰問題，
 * 當解碼異常時跳過錯誤封包，防止客戶端斷開連線。
 */
public class SpearFixAddon extends MeteorAddon {
    public static final String VERSION = "1.0.3";
    public static final Logger LOG = LogUtils.getLogger();
    public static final Category CATEGORY = new Category("SpearFix");

    @Override
    public void onInitialize() {
        LOG.info("Initializing SpearFix Addon v{}", VERSION);

        // 嘗試透過 Instrumentation 註冊 ASM Transformer
        // （若未以 -javaagent 啟動則無法取得 Instrumentation）
        tryRegisterTransformer();
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

    /**
     * 嘗試使用 Instrumentation API 註冊 ClassFileTransformer。
     * 若 Agent 已透過 premain 註冊則跳過。
     */
    private static void tryRegisterTransformer() {
        try {
            // 透過 reflection 取得 Instrumentation 實例
            // （Instrumentation 僅在 Java Agent 啟動時可用）
            Class<?> instClass = Class.forName("java.lang.instrument.Instrumentation");
            Method getInstrumentation = Class.forName("java.lang.instrument.Instruments")
                    .getMethod("getInstrumentation");
            Instrumentation inst = (Instrumentation) getInstrumentation.invoke(null);

            if (inst != null) {
                SpearFixAgent.register(inst);
                LOG.info("SpearFixAgent transformer registered successfully");
            }
        } catch (Exception e) {
            LOG.warn("Could not register ASM transformer via Instrumentation (not running as agent). " +
                     "Use -javaagent:spearfix.jar for early transformation, " +
                     "or ensure Mixin fallback is active.");
        }
    }
}
