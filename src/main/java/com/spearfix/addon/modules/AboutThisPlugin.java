package com.spearfix.addon.modules;

import com.spearfix.addon.SpearFixAddon;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Text;
public class AboutThisPlugin extends Module {

    public AboutThisPlugin() {
        super(TranslateAddon.CATEGORY, "关于插件", "插件作者：benjamin920101");
    }

    @Override
    public void onActivate() {
        ChatUtils.sendMsg(Text.literal("插件作者：benjamin9020101"));
    }
}
