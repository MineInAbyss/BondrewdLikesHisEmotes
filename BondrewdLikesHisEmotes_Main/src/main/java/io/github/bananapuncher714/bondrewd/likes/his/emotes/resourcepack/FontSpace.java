package io.github.bananapuncher714.bondrewd.likes.his.emotes.resourcepack;

import com.google.gson.JsonObject;

public class FontSpace extends FontProvider {

    protected JsonObject spaceAdvances;

    public FontSpace(JsonObject spaceAdvances) {
        super("space");
        this.spaceAdvances = spaceAdvances;
    }

    public JsonObject toJsonObject() {
        JsonObject object = super.toJsonObject();
        object.add("advances", spaceAdvances);
        return object;
    }
}
