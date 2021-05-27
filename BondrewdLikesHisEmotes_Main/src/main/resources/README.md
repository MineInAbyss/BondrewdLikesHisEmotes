# Bondrewd likes his emotes
Convert phrases into emotes as specified in the Made in Abyss resource pack. Bondrewd likes his emotes.

### Emote mapping
Emotes start at `\uEBAF` (Bondrewd And Friends) and increment for each additional emote.

### How to add custom emotes
Add an entry in the list of emotes in the config. They will be converted to `:<emote-name>:` format in-game.

### How to add to existing resource pack
Place the generated `/plugins/BondrewdLikesHisEmotes/fonts/*.json` files in the `/assets/minecraft/font/` directory of the resource pack, and place all images in the `/assets/minecraft/textures/emotes/` folder or wherever specified in the config.

### Permissions
- `bondrewdemotes.emote.<emote>` - Permission to use the specified emote. OP by default. Doesn't actually prevent the user from typing emotes, it just stops them from showing up in the list.
- `bondrewdemotes.all` - Permission to use all emotes. OP by default. Doesn't actually prevent the user from typing emotes, it just stops them from showing up in the list.
- `bondrewdemotes.reload` - Permission to use the reload command. OP by default.
- `bondrewdemotes.list` - Permission to use the list command. True by default.
- `bondrewdemotes.admin` - Permission to use all the emotes and commands. OP by default.

### Commands
- `/emote list` - List all the emotes that the player has permission for.
- `/emote reload` - Reload the list of valid emotes

### PlaceholderAPI
In the cases where providing `:<emote>:` doesn't work, or you'd like to use it in a plugin, then you can use the PlaceholderAPI expansion with the namespace `%emote_<emote>%`. If you need to use this option, then there is probably a better way to accomplish what you're trying to achieve.

### Additional info
Note that it *is* possible for players to copy and paste the corresponding unicode character directly into chat and bypass permissions. Using different font files can prevent this.