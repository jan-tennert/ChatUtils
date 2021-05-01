package de.jan.chatutils;

import java.util.Arrays;
import java.util.List;

import net.labymod.api.LabyModAddon;
import net.labymod.api.event.Subscribe;
import net.labymod.api.event.events.client.TickEvent;
import net.labymod.api.event.events.client.chat.MessageModifyEvent;
import net.labymod.main.LabyMod;
import net.labymod.settings.elements.BooleanElement;
import net.labymod.settings.elements.ControlElement.IconData;
import net.labymod.settings.elements.KeyElement;
import net.labymod.settings.elements.SettingsElement;
import net.labymod.utils.Keyboard;
import net.labymod.utils.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.play.NetworkPlayerInfo;
import net.minecraft.util.text.*;
import net.minecraft.util.text.Color;
import net.minecraft.util.text.TextComponent;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;

public class ChatUtils extends LabyModAddon {

    private boolean enabled;
    private boolean extraButton;
    private boolean removePlayer;
    private int clearChatKey;

    @Override
    public void onEnable() {
        getApi().getEventService().registerListener(this);
    }

    @Override
    public void loadConfig() {
        enabled = (getConfig().has("enabled")) ? getConfig().get("enabled").getAsBoolean() : true;
        extraButton = (getConfig().has("extraButton")) ? getConfig().get("extraButton").getAsBoolean() : true;
        removePlayer = (getConfig().has("removePlayer")) ? getConfig().get("removePlayer").getAsBoolean() : true;
        clearChatKey = (getConfig().has("clearChatKey")) ? getConfig().get("clearChatKey").getAsInt() : 52;
    }

    @Override
    protected void fillSettings(List<SettingsElement> list) {
        list.add(new BooleanElement("Enabled", this, new IconData(Material.REDSTONE), "enabled", this.enabled));
        list.add(new BooleanElement("Copy Button", this, new IconData(Material.GREEN_DYE), "extraButton", this.extraButton));
        BooleanElement removePlayerC = new BooleanElement("Remove Player from Clipboard", this, new IconData(Material.PLAYER_HEAD), "removePlayer", this.removePlayer);
        list.add(removePlayerC);
        list.add(new KeyElement("Clear Chat Key", this, null, "clearChatKey", this.clearChatKey));
    }

    @Subscribe
    public void tick(TickEvent e) {
        if (LabyMod.getInstance().isInGame()) {
            if (Keyboard.isKeyDown(clearChatKey) && enabled) {
                for (int i = 0; i < 30; i++) {
                    LabyMod.getInstance().displayMessageInChat("\n");
                }
            }
        }
    }

    @Subscribe
    public void onMessage(MessageModifyEvent e) {
        if (e.getComponent().getString().equals("\n")) return;
        if (Minecraft.getInstance().player != null && LabyMod.getInstance().isInGame()) {
            if (enabled) {
                String author = null;
                if (Minecraft.getInstance().isIntegratedServerRunning() && removePlayer) {
                    for (String player : Minecraft.getInstance().getIntegratedServer().getOnlinePlayerNames()) {
                        if (e.getComponent().getString().contains(player)) {
                            author = player;
                        }
                    }
                } else if (Minecraft.getInstance().getConnection() != null && removePlayer) {
                    for (NetworkPlayerInfo player : Minecraft.getInstance().getConnection().getPlayerInfoMap()) {
                        if (player.getDisplayName() != null && e.getComponent().getString().contains(player.getDisplayName().getString())) {
                            author = player.getDisplayName().getString();
                        }
                    }
                }


                if (extraButton) {
                    e.getComponent().getSiblings().add(generateCopyButton(e.getComponent(), author));
                } else {
                    ITextComponent c = e.getComponent();
                    TextComponent t = (TextComponent) c;
                    String toCopy = t.getString();
                    if (author != null) toCopy = toCopy.replace(author, "");
                    t.setStyle(t.getStyle()
                            .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new StringTextComponent(getCopyText())))
                            .setClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, toCopy)));
                }
            }
        }
    }

    private TextComponent generateCopyButton(ITextComponent msg, String author) {
        StringTextComponent init = new StringTextComponent(" ");
        StringTextComponent b1 = new StringTextComponent("[");
        b1.setStyle(b1.getStyle().setColor(Color.fromHex("#ffffff")));
        StringTextComponent copy = new StringTextComponent(getCopyName());
        HoverEvent event = new HoverEvent(HoverEvent.Action.SHOW_TEXT, new StringTextComponent(getCopyText()));
        String toCopy = msg.getString();
        if (author != null) toCopy = toCopy.replace(author, "");
        copy.setStyle(copy.getStyle().setColor(Color.fromHex("#00f53d"))
                .setHoverEvent(event)
                .setClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, toCopy)));

        StringTextComponent b2 = new StringTextComponent("]");
        b2.setStyle(b2.getStyle().setColor(Color.fromHex("#ffffff")));
        init.getSiblings().addAll(list(b1, copy, b2));
        return init;
    }

    private String getCopyName() {
        String name = "Copy";
        switch (Minecraft.getInstance().getLanguageManager().getCurrentLanguage().getCode()) {
            case "de_de":
                return "Kopieren";
            case "fr_fr":
                return "Copier";
            default:
                return name;
        }
    }

    private String getCopyText() {
        String name = "Copy to Clipboard";
        switch (Minecraft.getInstance().getLanguageManager().getCurrentLanguage().getCode()) {
            case "de_de":
                return "In die Zwischenablage kopieren";
            case "fr_fr":
                return "Copier dans le presse-papiers";
            default:
                return name;
        }
    }

    @SafeVarargs
    private final <T> List<T> list(T... c) {
        return Arrays.asList(c);
    }
}
