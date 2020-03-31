# Bondrewd likes his emotes
Convert phrases into emotes as specified in the Made in Abyss resource pack. Bondrewd likes his emotes.

### Emote mapping
Emotes start at `\uEBAF` (Bondrewd And Friends) and increment for each additional emote.

### How to add custom emotes
Place a png with maximum size of 256x256 in the `/plugins/BondrewdLikesHisEmotes/assets/` folder and reload the plugin. Images will be converted into the emote `:<file-name-minus-extension>:`.

### How to use a pre-existing `default.json` file
Place the `default.json` file in the `/plugins/BondrewdLikesHisEmotes/assets/` folder and it will be included.

### How to add to existing resource pack
Place the generated `/plugins/BondrewdLikesHisEmotes/default.json` file in the `/assets/minecraft/font/` directory of the resource pack, and place all images in the `/assets/minecraft/textures/emotes/` folder.

### Permissions
- `bondrewdemotes.emote.<emote>` - Permission to use the specified emote. OP by default.
- `bondrewdemotes.all` - Permission to use all emotes. OP by default.
- `bondrewdemotes.reload` - Permission to use the reload command. OP by default.
- `bondrewdemotes.list` - Permission to use the list command. True by default.
- `bondrewdemotes.admin` - Permission to use all the emotes and commands. OP by default.

### Commands
- `/emote list` - List all the emotes that the player has permission for.
- `/emote reload` - Reload the list of valid emotes

### Additional info
Note that it *is* possible for players to copy and paste the corresponding unicode character directly into chat and bypass permissions.