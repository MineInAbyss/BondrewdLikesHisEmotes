package io.github.bananapuncher714.bondrewd.likes.his.emotes;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import de.themoep.minedown.MineDown;
import io.github.bananapuncher714.bondrewd.likes.his.emotes.api.ComponentTransformer;
import io.github.bananapuncher714.bondrewd.likes.his.emotes.api.PacketHandler;
import io.github.bananapuncher714.bondrewd.likes.his.emotes.resourcepack.FontBitmap;
import io.github.bananapuncher714.bondrewd.likes.his.emotes.resourcepack.FontIndex;
import io.github.bananapuncher714.bondrewd.likes.his.emotes.resourcepack.FontSpace;
import io.github.bananapuncher714.bondrewd.likes.his.emotes.resourcepack.NamespacedKey;
import io.github.bananapuncher714.bondrewd.likes.his.emotes.util.FileUtil;
import io.github.bananapuncher714.bondrewd.likes.his.emotes.util.PermissionBuilder;
import io.github.bananapuncher714.bondrewd.likes.his.emotes.util.ReflectionUtil;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.*;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.HoverEvent.Action;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.permissions.Permissible;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.StringUtil;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

public class BondrewdLikesHisEmotes extends JavaPlugin {
    // Could technically be 8, but it's small enough as it is so why not 11
    private static int EMOTE_HEIGHT = 11;
    private static int EMOTE_ASCENT = 9;
    private static String DEFAULT_NAMESPACE = "minecraft";
    private static String DEFAULT_FOLDER = "emotes";
    private static String DEFAULT_FONT = "emote";
    private static String DEFAULT_GIF_NAMESPACE = "minecraft";
    private static String DEFAULT_GIF_FOLDER = "gifs";
    private static String DEFAULT_GIF_FONT = "gif";

    private static NegativeSpaceType NEGATIVE_SPACE_TYPE = NegativeSpaceType.LEGACY;
    private static String NEGATIVE_SPACE_FONT = "space";
    private static String NEGATIVE_SPACE_TEXTURE = "space:font/space_split.png";

    private static final char STARTING_CHAR = '\uEBAF';
    private static final String EMOTE_FORMAT = ":%s:";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private PacketHandler handler;
    private LinkedList<Emote> emotes = new LinkedList<>();
    private LinkedList<Emote> spaces = new LinkedList<>();
    private LinkedList<Emote> emotes_and_spaces = new LinkedList<>();

    private BaseComponent[] noEmoteMessage;
    private BaseComponent[] noGifMessage;

    public enum NegativeSpaceType {
        LEGACY,
        MODERN,
        BOTH
    }

    @Override
    public void onEnable() {
        handler = ReflectionUtil.getNewPacketHandlerInstance();
        if (handler == null) {
            getLogger().severe(ReflectionUtil.VERSION + " is not currently supported! Disabling...");
            setEnabled(false);
            return;
        } else {
            getLogger().info("Detected version " + ReflectionUtil.VERSION);
        }

        handler.setTransformer(new ComponentTransformer() {
            @Override
            public BaseComponent transform(BaseComponent component) {
                return transformComponent(component, true);
            }

            @Override
            public String verifyFor(Player player, String string) {
                return verify(player, string);
            }

        });

        Bukkit.getPluginManager().registerEvents(new Listener() {
            @EventHandler
            private void onEvent(PlayerJoinEvent event) {
                handler.inject(event.getPlayer());
            }

            @EventHandler
            private void onEvent(PlayerEditBookEvent event) {
                if (event.isSigning()) {
                    BookMeta meta = event.getNewBookMeta();
                    List<BaseComponent[]> components = new ArrayList<BaseComponent[]>(meta.spigot().getPages());
                    for (BaseComponent[] componentArr : components) {
                        for (int i = 0; i < componentArr.length; i++) {
                            componentArr[i] = verify(event.getPlayer(), componentArr[i]);
                        }
                    }
                    meta.spigot().setPages(components);
                    event.setNewBookMeta(meta);
                }
            }
        }, this);

        for (Player player : Bukkit.getOnlinePlayers()) {
            handler.inject(player);
        }

        FileUtil.saveToFile(getResource("README.md"), new File(getDataFolder() + "/" + "README.md"), true);
        FileUtil.saveToFile(getResource("config.yml"), new File(getDataFolder() + "/" + "config.yml"), false);

        loadConfig();
        loadPermissions();

        if (emotes.isEmpty()) {
            getLogger().info("Didn't detect any emotes!");
        } else {
            getLogger().info("Loaded emotes: " + String.join(" ", emotes.stream().map(Emote::getId).collect(Collectors.toList())));
        }
    }

    @Override
    public void onDisable() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            handler.uninject(player);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        // Just a simple tab complete for now...
        List<String> completions = new ArrayList<String>();
        List<String> suggestions = new ArrayList<String>();
        if (args.length == 1) {
            if (sender.hasPermission("bondrewdemotes.reload")) {
                suggestions.add("reload");
            }
            if (sender.hasPermission("bondrewdemotes.list")) {
                suggestions.add("list");
            }
        }
        StringUtil.copyPartialMatches(args[args.length - 1], suggestions, completions);
        Collections.sort(completions);
        return completions;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            if (args[0].equalsIgnoreCase("reload")) {
                if (sender.hasPermission("bondrewdemotes.reload")) {
                    sender.sendMessage("Reloading the emote config...");
                    loadConfig();
                    loadPermissions();
                    sender.sendMessage(ChatColor.GREEN + "Done!");
                } else {
                    sender.sendMessage(ChatColor.RED + "You do not have permission to run this command!");
                }
            } else if (args[0].equalsIgnoreCase("list")) {
                if (sender.hasPermission("bondrewdemotes.list")) {
                    List<Emote> sortedEmotes = new ArrayList<Emote>(emotes);

                    Collections.sort(sortedEmotes, new Comparator<Emote>() {
                        @Override
                        public int compare(Emote e1, Emote e2) {
                            if (e1.isGif() ^ e2.isGif()) {
                                return e1.isGif() ? 1 : -1;
                            }
                            if (e1.getFormatting().toLowerCase().contains("k") ^ e2.getFormatting().toLowerCase().contains("k")) {
                                return e1.getFormatting().toLowerCase().contains("k") ? 1 : -1;
                            }
                            return e1.getId().toLowerCase().compareTo(e2.getId().toLowerCase());
                        }
                    });

                    StringBuilder builder = new StringBuilder(ChatColor.of(new Color(0x4abdff)) + "Available emotes:\n");
                    StringBuilder gifBuilder = new StringBuilder(ChatColor.of(new Color(0xf96854)) + "Available GIFs:\n");
                    boolean found = false;
                    boolean foundGif = false;
                    for (Emote emote : sortedEmotes) {
                        if (hasEmotePerms(sender, emote)) {
                            if (emote.isGif() || emote.getFormatting().toLowerCase().contains("k")) {
                                gifBuilder.append(":");
                                gifBuilder.append(emote.getId());
                                gifBuilder.append(": ");
                                foundGif = true;
                            } else {
                                builder.append(":");
                                builder.append(emote.getId());
                                builder.append(": ");
                                found = true;
                            }
                        }
                    }

                    if (found || foundGif) {
                        if (found) {
                            sender.sendMessage(builder.toString().trim());
                        }
                        if (found && foundGif) {
                            sender.sendMessage("");
                        }
                        if (foundGif) {
                            sender.sendMessage(gifBuilder.toString().trim());
                        }
                    } else {
                        sender.sendMessage(ChatColor.RED + "You cannot use any emotes!");
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + "You do not have permission to run this command!");
                }
            } else {
                sender.sendMessage(ChatColor.RED + "Invalid arguments!");
            }
        } else {
            sender.sendMessage(ChatColor.RED + "Invalid arguments!");
        }
        return false;
    }

    private void loadConfig() {
        FileConfiguration config = YamlConfiguration.loadConfiguration(new File(getDataFolder() + "/" + "config.yml"));

        EMOTE_HEIGHT = config.getInt("default-height");
        EMOTE_ASCENT = config.getInt("default-ascent");
        DEFAULT_NAMESPACE = config.getString("default-namespace", "minecraft");
        DEFAULT_FOLDER = config.getString("default-folder", "emotes");
        DEFAULT_FONT = config.getString("default-font", "default");
        DEFAULT_GIF_NAMESPACE = config.getString("default-gif-namespace", "minecraft");
        DEFAULT_GIF_FOLDER = config.getString("default-gif-folder", "gifs");
        DEFAULT_GIF_FONT = config.getString("default-gif-font", "default");

        NEGATIVE_SPACE_TYPE = NegativeSpaceType.valueOf(config.getString("negative-space-type", "LEGACY").toUpperCase());
        NEGATIVE_SPACE_FONT = config.getString("negative-space-font", "space");
        NEGATIVE_SPACE_TEXTURE = config.getString("negative-space-texture", "space:font/space.png");

        String noEmoteMessageString = config.getString("messages.no-emote");
        if (noEmoteMessageString != null && !noEmoteMessageString.isEmpty()) {
            noEmoteMessage = MineDown.parse(noEmoteMessageString);
        } else {
            noEmoteMessage = null;
        }
        String noGifMessageString = config.getString("messages.no-gif");
        if (noGifMessageString != null && !noGifMessageString.isEmpty()) {
            noGifMessage = MineDown.parse(noGifMessageString);
        } else {
            noGifMessage = null;
        }

        emotes.clear();
        spaces.clear();
        Map<String, FontIndex> fonts = new HashMap<String, FontIndex>();

        int c = STARTING_CHAR;
        Map<String, Integer> charHolder = new HashMap<String, Integer>();
        List<String> emoteList = config.getStringList("emotes");
        for (String emote : emoteList) {
            EmoteInfo emoteInfo = parseEmoteFrom(emote);
            Emote parsedEmote = emoteInfo.emote;

            String font = parsedEmote.getFont();
            FontIndex index = fonts.get(font);
            if (index == null) {
                index = new FontIndex();
                fonts.put(font, index);
            }

            int v = charHolder.getOrDefault(font, c);

            parsedEmote.setValue(String.valueOf((char) v));
            emotes.add(parsedEmote);

            FontBitmap provider = new FontBitmap(NamespacedKey.fromString(emoteInfo.namespace), new String[]{String.valueOf((char) v)});
            provider.setHeight(emoteInfo.height);
            provider.setAscent(emoteInfo.ascent);
            index.addProvider(provider);

            charHolder.put(font, v + 1);
        }

        // Load in all gifs
        ConfigurationSection gifList = config.getConfigurationSection("gifs");
        if (gifList != null) {
            for (String gif : gifList.getKeys(false)) {
                ConfigurationSection gifSec = gifList.getConfigurationSection(gif);

                int framecount = gifSec.getInt("framecount", 0);
                int width = gifSec.getInt("width", 0);

                if (framecount <= 0 || width <= 0) {
                    getLogger().warning(String.format("Unable to parse gif '%s', invalid framecount/width!", gif));
                }

                String namespace = gifSec.getString("namespace", DEFAULT_GIF_NAMESPACE);
                String folder = gifSec.getString("folder", DEFAULT_GIF_FOLDER);
                String font = gifSec.getString("font", DEFAULT_GIF_FONT);

                int height = gifSec.getInt("height", EMOTE_HEIGHT);
                int ascent = gifSec.getInt("ascent", EMOTE_ASCENT);

                Emote gifEmote = new Emote(gif, true);
                gifEmote.setFont(font);
                gifEmote.setFormatting("");

                int v = charHolder.getOrDefault(font, c);
                FontIndex index = fonts.get(font);
                if (index == null) {
                    index = new FontIndex();
                    fonts.put(font, index);
                }

                StringBuilder gifBuilder = new StringBuilder();
                char negativeSpace = (char) v++;
                FontBitmap provider = new FontBitmap(NamespacedKey.fromString(NEGATIVE_SPACE_TEXTURE), new String[]{String.valueOf(negativeSpace)});
                provider.setHeight(-(width + 3));
                provider.setAscent(-32768);
                index.addProvider(provider);

                for (int i = 0; i < framecount; i++) {
                    gifBuilder.append((char) v);
                    if (i < framecount - 1) {
                        gifBuilder.append(negativeSpace);
                    }
                    FontBitmap frameProvider = new FontBitmap(new NamespacedKey(namespace, String.format("%s/%s/%03d.png", folder, gif, i)), new String[]{String.valueOf((char) v++)});
                    frameProvider.setHeight(height);
                    frameProvider.setAscent(ascent);
                    index.addProvider(frameProvider);
                }
                gifEmote.setValue(gifBuilder.toString());

                emotes.add(gifEmote);
                charHolder.put(font, v);
            }
        }

        emotes_and_spaces.addAll(emotes);


        // Load in negative spaces
        generateSpaceEmoteInfo(fonts, charHolder);
        if (NEGATIVE_SPACE_TYPE == null || NEGATIVE_SPACE_TYPE == NegativeSpaceType.LEGACY) {
            addLegacySpaceEntries(fonts);
        } else if (NEGATIVE_SPACE_TYPE == NegativeSpaceType.MODERN) {
            addModernSpaceEntries(fonts);
        } else if (NEGATIVE_SPACE_TYPE == NegativeSpaceType.BOTH) {
            addLegacySpaceEntries(fonts);
            addModernSpaceEntries(fonts);
        }

        emotes_and_spaces.addAll(spaces);

        for (Entry<String, FontIndex> entry : fonts.entrySet()) {
            File fontFile = new File(getDataFolder() + "/fonts/" + entry.getKey() + ".json");
            FontIndex index = entry.getValue();

            if (fontFile.exists()) {
                fontFile.delete();
            }
            fontFile.getParentFile().mkdirs();

            try {
                OutputStreamWriter writer = new OutputStreamWriter(Files.newOutputStream(fontFile.toPath()), StandardCharsets.UTF_8);
                GSON.toJson(index.toJsonObject(), writer);
                writer.close();
            } catch (IOException e) {
                getLogger().severe(String.format("Unable to save font file '%s'", entry.getKey()));
                e.printStackTrace();
            }
        }
    }

    private void generateSpaceEmoteInfo(Map<String, FontIndex> fonts, Map<String, Integer> charHolder) {
        int v = charHolder.getOrDefault(NEGATIVE_SPACE_FONT, (int) STARTING_CHAR);
        for (int i = 1; i <= 1028; i++) {
            Emote space = generateSpaceData(fonts, charHolder, v, i);
            if (space == null) continue;
            spaces.add(space);
            v++;
        }
        for (int i = -1; i >= -1028; i--) {
            Emote space = generateSpaceData(fonts, charHolder, v, i);
            if (space == null) continue;
            spaces.add(space);
            v++;
        }
    }

    private Emote generateSpaceData(Map<String, FontIndex> fonts, Map<String, Integer> charHolder, int v, int i) {
        EmoteInfo spaceInfo = parseEmoteFrom(NEGATIVE_SPACE_FONT + "|space_" + i + " " + NEGATIVE_SPACE_TEXTURE);
        if (spaceInfo == null) return null;
        Emote space = spaceInfo.emote;
        if (space == null) return null;

        space.setValue(String.valueOf((char) v));

        FontIndex index = fonts.get(NEGATIVE_SPACE_FONT);
        if (index == null) {
            index = new FontIndex();
            fonts.put(NEGATIVE_SPACE_FONT, index);
        }

        charHolder.put(NEGATIVE_SPACE_FONT, v + 1);
        return space;
    }

    private void addModernSpaceEntries(Map<String, FontIndex> fonts) {
        JsonObject advances = new JsonObject();
        for (Emote space : spaces) {
            getLogger().info("Adding space " + space.id);
            getLogger().warning("value: " + space.value.charAt(0));
            int advance = Integer.parseInt(space.id.split("space_")[1]) + 3;
            advances.addProperty(space.value, advance);
        }
        fonts.get(NEGATIVE_SPACE_FONT).addProvider(new FontSpace(advances));

    }

    private void addLegacySpaceEntries(Map<String, FontIndex> fonts) {
        for (Emote space : spaces) {
            FontBitmap provider = new FontBitmap(NamespacedKey.fromString(NEGATIVE_SPACE_TEXTURE), new String[]{space.value});
            provider.setHeight(Integer.parseInt(space.id.split("space_")[1]) + 3);
            provider.setAscent(-32768);

            fonts.get(NEGATIVE_SPACE_FONT).addProvider(provider);
        }
    }

    private void loadPermissions() {
        PermissionBuilder admin = new PermissionBuilder("bondrewdemotes.admin").setDefault(PermissionDefault.OP);
        // Right now the emote permissions don't do anything apart from showing up in the list of emotes.
        PermissionBuilder all = new PermissionBuilder("bondrewdemotes.all").setDefault(PermissionDefault.OP);
        for (Emote emote : emotes) {
            all.addChild(new PermissionBuilder("bondrewdemotes.emote." + emote.getId()).setDefault(PermissionDefault.OP).register().build(), true);
        }

        admin.addChild(all.register().build(), true).register();
        admin.addChild(new PermissionBuilder("bondrewdemotes.reload").setDefault(PermissionDefault.OP).register().build(), true);
        admin.addChild(new PermissionBuilder("bondrewdemotes.list").setDefault(PermissionDefault.TRUE).register().build(), true);
    }

    public BaseComponent transformComponent(BaseComponent component) {
        return transformComponent(component, false);
    }

    private BaseComponent transformComponent(BaseComponent component, boolean full) {
        List<BaseComponent> subComponents = new LinkedList<BaseComponent>();

        HoverEvent hover = component.getHoverEvent();
        ClickEvent click = component.getClickEvent();
        if (hover != null) {
            BaseComponent[] hoverComps = hover.getValue();
            for (int i = 0; i < hoverComps.length; i++) {
                hoverComps[i] = transformComponent(hoverComps[i], full);
            }
            hover = new HoverEvent(hover.getAction(), hoverComps);
            component.setHoverEvent(hover);
        }

        if (component instanceof TextComponent) {
            TextComponent text = (TextComponent) component;

            List<TextComponent> components = new LinkedList<TextComponent>();
            components.add(text);

            for (Emote emote : emotes_and_spaces) {
                List<TextComponent> temp = new LinkedList<TextComponent>();
                String key = String.format(EMOTE_FORMAT, emote.getId());
                for (TextComponent comp : components) {
                    String val = comp.getText();
                    String[] split = val.split("(?<!\\\\)" + key, -1);
                    if (split.length > 1) {
                        for (int i = 0; i < split.length; i++) {
                            String sub = split[i].replace("\\" + key, key);
                            if (!sub.isEmpty()) {
                                TextComponent subText = new TextComponent(sub);
                                subText.copyFormatting(text);
                                temp.add(subText);
                            }

                            if (i < split.length - 1) {
                                TextComponent emoteComp = new TextComponent(emote.getValue());
                                if (emote.isGif()) {
                                    emoteComp.setColor(ChatColor.of(new Color(0xFEFEFE)));
                                } else {
                                    for (char c : emote.getFormatting().toCharArray()) {
                                        switch (c) {
                                            case 'k':
                                            case 'K':
                                                emoteComp.setObfuscated(true);
                                                break;
                                            case 'l':
                                            case 'L':
                                                emoteComp.setBold(true);
                                                break;
                                            case 'm':
                                            case 'M':
                                                emoteComp.setStrikethrough(true);
                                                break;
                                            case 'n':
                                            case 'N':
                                                emoteComp.setUnderlined(true);
                                                break;
                                            case 'o':
                                            case 'O':
                                                emoteComp.setItalic(true);
                                                break;
                                        }
                                    }

                                    emoteComp.setColor(ChatColor.WHITE);
                                }

                                // For renaming items
                                if (emoteComp.isItalicRaw() == null) {
                                    emoteComp.setItalic(false);
                                }

                                emoteComp.setFont(emote.getFont());

                                if (hover == null && full) {
                                    emoteComp.setHoverEvent(new HoverEvent(Action.SHOW_TEXT, new BaseComponent[]{new TextComponent(key)}));
                                }
                                // Remove hover and click from spaces
                                if (spaces.contains(emote)) {
                                    emoteComp.setClickEvent(null);
                                    emoteComp.setHoverEvent(null);
                                }
                                temp.add(emoteComp);
                            }
                        }
                    } else {
                        if (full) {
                            comp.setText(comp.getText().replace("\\" + key, key));
                        }
                        temp.add(comp);
                    }
                }

                components = temp;
            }

            subComponents.addAll(components);
        } else if (component instanceof TranslatableComponent) {
            TranslatableComponent translate = (TranslatableComponent) component;
            if (translate.getWith() != null) {
                List<BaseComponent> newWith = new ArrayList<BaseComponent>();
                for (BaseComponent with : translate.getWith()) {
                    newWith.add(transformComponent(with, full));
                }
                translate.setWith(newWith);
            }
            subComponents.add(component);
        } else {
            subComponents.add(component);
        }

        List<BaseComponent> extra = null;
        if (component.getExtra() != null) {
            extra = new LinkedList<BaseComponent>();
            for (BaseComponent comp : component.getExtra()) {
                extra.add(transformComponent(comp, full));
            }

            component.setExtra(new ArrayList<BaseComponent>());
        }

        if (subComponents.size() > 1) {
            if (extra != null && !extra.isEmpty()) {
                TextComponent extraComp = new TextComponent("");
                extraComp.setExtra(extra);
                subComponents.add(extraComp);
            }
            component = new TextComponent("");
            component.setHoverEvent(hover);
            component.setClickEvent(click);
            component.setExtra(subComponents);
        } else {
            component = subComponents.get(0);
            if (extra != null && !extra.isEmpty()) {
                component.setExtra(extra);
            }
        }

        return component;
    }

    public BaseComponent verify(Player player, BaseComponent component) {
        if (component instanceof TextComponent) {
            TextComponent text = (TextComponent) component;
            text.setText(verify(player, text.getText()));
        } else if (component instanceof TranslatableComponent) {
            TranslatableComponent translate = (TranslatableComponent) component;
            if (translate.getWith() != null) {
                translate.getWith().parallelStream().forEach(ex -> verify(player, ex));
            }
        }

        if (component.getExtra() != null) {
            component.getExtra().parallelStream().forEach(ex -> verify(player, ex));
        }

        return component;
    }

    private String verify(Player player, String string) {
        boolean noEmote = false;
        boolean noGif = false;
        for (Emote emote : emotes) {
            if (!hasEmotePerms(player, emote)) {
                String search = String.format(EMOTE_FORMAT, emote.getId());
                String replaced = string.replace(search, "\\" + search);
                if (!string.equals(replaced)) {
                    if (emote.isGif() || emote.getFormatting().toLowerCase().contains("k")) {
                        noGif = true;
                    } else {
                        noEmote = true;
                    }
                }
                string = replaced;
            }
        }

        if (noEmote && noEmoteMessage != null) {
            Bukkit.getScheduler().runTask(this, () -> player.spigot().sendMessage(noEmoteMessage));
        }

        if (noGif && noGifMessage != null) {
            Bukkit.getScheduler().runTask(this, () -> player.spigot().sendMessage(noGifMessage));
        }

        return string;
    }

    private boolean hasEmotePerms(Permissible player, Emote emote) {
        return player.hasPermission("bondrewdemotes.emote." + emote.getId()) ||
                player.hasPermission("bondrewdemotes.font." + emote.getFont().replace('.', '/'));
    }

    private EmoteInfo parseEmoteFrom(String string) {
        String[] split = string.split("\\s+");
        if (split.length == 0) {
            return null;
        }
        String id = split[0];
        String[] fontSplit = id.split("\\|.*?");
        if (fontSplit.length > 1) {
            id = fontSplit[1];
        }
        String[] formatSplit = id.split("&");
        if (formatSplit.length > 1) {
            id = formatSplit[0];
        }

        Emote emote = new Emote(id, false);
        if (fontSplit.length > 1) {
            emote.setFont(fontSplit[0]);
        } else {
            emote.setFont(DEFAULT_FONT);
        }

        if (formatSplit.length > 1) {
            emote.setFormatting(formatSplit[1]);
        } else {
            emote.setFormatting("");
        }

        String namespace = split.length > 1 ? split[1] : DEFAULT_NAMESPACE + ":" + DEFAULT_FOLDER + "/" + emote.id + ".png";
        int height = EMOTE_HEIGHT;
        int ascent = EMOTE_ASCENT;
        if (split.length > 3) {
            height = Integer.parseInt(split[2]);
            ascent = Integer.parseInt(split[3]);
        }
        EmoteInfo info = new EmoteInfo(emote, namespace, height, ascent);

        return info;
    }

    public List<Emote> getEmotes() {
        return Collections.unmodifiableList(emotes);
    }

    public static PacketHandler getHandler() {
        return JavaPlugin.getPlugin(BondrewdLikesHisEmotes.class).handler;
    }

    public static int getEmoteHeight() {
        return EMOTE_HEIGHT;
    }

    public static int getEmoteAscent() {
        return EMOTE_ASCENT;
    }

    private class EmoteInfo {
        Emote emote;
        String namespace;
        int height;
        int ascent;

        protected EmoteInfo(Emote emote, String namespace, int height, int ascent) {
            super();
            this.emote = emote;
            this.namespace = namespace;
            this.height = height;
            this.ascent = ascent;
        }
    }
}
