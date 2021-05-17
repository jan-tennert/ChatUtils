package de.jan.chatutils;

import net.labymod.api.LabyModAddon;
import net.labymod.api.event.Subscribe;
import net.labymod.api.event.events.client.TickEvent;
import net.labymod.api.event.events.client.chat.MessageModifyEvent;
import net.labymod.main.LabyMod;
import net.labymod.settings.elements.*;
import net.labymod.settings.elements.ControlElement.IconData;
import net.labymod.utils.Keyboard;
import net.labymod.utils.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.play.NetworkPlayerInfo;
import net.minecraft.util.text.Color;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextComponent;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

public class ChatUtils extends LabyModAddon {

    private boolean enabled;
    private boolean extraButton;
    private boolean removePlayer;
    private int clearChatKey;
    private boolean displayTimestamp;
    private String timestampPattern;

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
        displayTimestamp = (getConfig().has("displayTimestamp")) ? getConfig().get("displayTimestamp").getAsBoolean() : false;
        timestampPattern = (getConfig().has("timestampPattern")) ? getConfig().get("timestampPattern").getAsString() : "HH:mm:ss";
    }

    @Override
    protected void fillSettings(List<SettingsElement> list) {
        list.add(new BooleanElement("Enabled", this, new IconData(Material.REDSTONE), "enabled", this.enabled));
        list.add(new HeaderElement("Copy Messages"));
        list.add(new BooleanElement("Copy Button", this, new IconData(Material.GREEN_DYE), "extraButton", this.extraButton));
        list.add(new BooleanElement("Remove Player from Clipboard", this, new IconData(Material.PLAYER_HEAD), "removePlayer", this.removePlayer));
        list.add(new HeaderElement("Timestamp"));
        list.add(new BooleanElement("Display Timestamp", this, new IconData(Material.CLOCK), "displayTimestamp", this.displayTimestamp));

        StringElement pattern = new StringElement("Timestamp Pattern", this, new IconData(Material.SIGN), "timestampPattern", this.timestampPattern);
        pattern.setDescriptionText("HH = hour\nmm = minute\nss = second\nSSS = fraction of second");
        list.add(pattern);

        list.add(new HeaderElement("Misc"));
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
                String time = null;
                if (displayTimestamp) {
                    time = addTimestamp(e);
                }
                String author = removeNames(e.getComponent());
                if (extraButton) {
                    e.getComponent().getSiblings().add(generateCopyButton(e.getComponent(), author, "[" + time + "]"));
                } else {
                    ITextComponent c = e.getComponent();
                    TextComponent t = (TextComponent) c;
                    String toCopy = t.getString();
                    if (author != null) toCopy = toCopy.replace(author, "");
                    if (time != null) toCopy = toCopy.replace("[" + time + "]", "");
                    t.setStyle(t.getStyle()
                            .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new StringTextComponent(getCopyText())))
                            .setClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, toCopy)));
                }
            }
        }
    }

    private String addTimestamp(MessageModifyEvent e) {
        LocalDateTime time = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(timestampPattern);
        try {
            String formattedDate = time.format(formatter);
            StringTextComponent text = new StringTextComponent(String.format("[%s] ", formattedDate));
            text.getSiblings().add(e.getComponent());
            text.getSiblings().addAll(e.getComponent().getSiblings());
            e.setComponent(text);
            return formattedDate;
        } catch (Exception ignored) {
            return null;
        }
    }

    private String removeNames(ITextComponent c) {
        String author = null;
        if (Minecraft.getInstance().isIntegratedServerRunning() && removePlayer) {
            for (String player : Minecraft.getInstance().getIntegratedServer().getOnlinePlayerNames()) {
                if (c.getString().contains(player)) {
                    author = player;
                    break;
                }
            }
        } else if (Minecraft.getInstance().getConnection() != null && removePlayer) {
            for (NetworkPlayerInfo player : Minecraft.getInstance().getConnection().getPlayerInfoMap()) {
                if (player.getDisplayName() != null && c.getString().contains(player.getDisplayName().getString())) {
                    author = player.getDisplayName().getString();
                    break;
                }
            }
        }
        return author;
    }

    private TextComponent generateCopyButton(ITextComponent msg, String... remove) {
        StringTextComponent init = new StringTextComponent(" ");
        StringTextComponent b1 = new StringTextComponent("[");
        b1.setStyle(b1.getStyle().setColor(Color.fromHex("#ffffff")));
        StringTextComponent copy = new StringTextComponent(getCopyName());
        HoverEvent event = new HoverEvent(HoverEvent.Action.SHOW_TEXT, new StringTextComponent(getCopyText()));
        String toCopy = msg.getString();
        for (String s : remove) {
            if(s != null) {
                toCopy = toCopy.replace(s, "");
            }
        }
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
