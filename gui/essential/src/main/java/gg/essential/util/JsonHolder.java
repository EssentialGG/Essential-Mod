/*
 * Copyright (c) 2024 ModCore Inc. All rights reserved.
 *
 * This code is part of ModCore Inc.'s Essential Mod repository and is protected
 * under copyright registration # TX0009138511. For the full license, see:
 * https://github.com/EssentialGG/Essential/blob/main/LICENSE
 *
 * You may not use, copy, reproduce, modify, sell, license, distribute,
 * commercialize, or otherwise exploit, or create derivative works based
 * upon, this file or any other in this repository, all of which is reserved by Essential.
 */
package gg.essential.util;

import gg.essential.lib.gson.JsonArray;
import gg.essential.lib.gson.JsonElement;
import gg.essential.lib.gson.JsonObject;
import gg.essential.lib.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;

/**
 * This is a copy of the class in gg.essential.api.util but using our bundled gson instead of the one provided by MC.
 */
public class JsonHolder {
    public static ThreadLocal<Boolean> printFormattingException = ThreadLocal.withInitial(() -> true);
    private JsonObject object;

    public JsonHolder(JsonObject object) {
        this.object = object;
    }

    @SuppressWarnings("deprecation")
    public JsonHolder(String raw) {
        if (raw == null)
            object = new JsonObject();
        else
            try {
                this.object = new JsonParser().parse(raw).getAsJsonObject();
            } catch (Exception e) {
                this.object = new JsonObject();
                if (printFormattingException.get())
                    e.printStackTrace();
            }
    }

    public JsonHolder() {
        this(new JsonObject());
    }

    public void ensureJsonHolder(String key) {
        if (!has(key))
            put(key, new JsonHolder());
    }

    public void ensureJsonArray(String key) {
        if (!has(key)) put(key, new JsonArray());
    }

    public JsonHolder optOrCreateJsonHolder(String key) {
        ensureJsonHolder(key);
        return optJSONObject(key);
    }

    public JsonArray optOrCreateJsonArray(String key) {
        ensureJsonArray(key);
        return optJSONArray(key);
    }

    @Override
    public String toString() {
        if (object != null)
            return object.toString();
        return "{}";
    }

    public JsonHolder put(String key, boolean value) {
        object.addProperty(key, value);
        return this;
    }

    public void mergeNotOverride(JsonHolder merge) {
        merge(merge, false);
    }

    public void mergeOverride(JsonHolder merge) {
        merge(merge, true);
    }

    public void merge(JsonHolder merge, boolean override) {
        JsonObject object = merge.getObject();
        merge.getKeys().stream().filter(s -> override || !this.has(s)).forEach(s -> put(s, object.get(s)));
    }

    private JsonHolder put(String s, JsonElement element) {
        this.object.add(s, element);
        return this;
    }

    public JsonHolder put(String key, String value) {
        object.addProperty(key, value);
        return this;
    }

    public JsonHolder put(String key, int value) {
        object.addProperty(key, value);
        return this;
    }

    public JsonHolder put(String key, double value) {
        object.addProperty(key, value);
        return this;
    }

    public JsonHolder put(String key, long value) {
        object.addProperty(key, value);
        return this;
    }

    private JsonHolder defaultOptJSONObject(String key, JsonObject fallBack) {
        try {
            return new JsonHolder(object.get(key).getAsJsonObject());
        } catch (Exception e) {
            return new JsonHolder(fallBack);
        }
    }

    public JsonArray defaultOptJSONArray(String key, JsonArray fallback) {
        try {
            return object.get(key).getAsJsonArray();
        } catch (Exception e) {
            return fallback;
        }
    }

    public JsonArray optJSONArray(String key) {
        return defaultOptJSONArray(key, new JsonArray());
    }


    public boolean has(String key) {
        return object.has(key);
    }

    public long optLong(String key, long fallback) {
        try {
            JsonElement jsonElement = object.get(key);
            if (jsonElement != null)
                return jsonElement.getAsLong();
        } catch (Exception ignored) {
        }
        return fallback;
    }

    public long optLong(String key) {
        return optLong(key, 0);
    }

    public boolean optBoolean(String key, boolean fallback) {
        try {
            JsonElement jsonElement = object.get(key);
            if (jsonElement != null)
                return jsonElement.getAsBoolean();
        } catch (Exception ignored) {
        }
        return fallback;
    }

    public boolean optBoolean(String key) {
        return optBoolean(key, false);
    }

    public JsonObject optActualJSONObject(String key) {
        try {
            return object.get(key).getAsJsonObject();
        } catch (Exception e) {
            return new JsonObject();
        }
    }

    public JsonHolder optJSONObject(String key) {
        return defaultOptJSONObject(key, new JsonObject());
    }


    public int optInt(String key, int fallBack) {
        try {
            return object.get(key).getAsInt();
        } catch (Exception e) {
            return fallBack;
        }
    }

    public int optInt(String key) {
        return optInt(key, 0);
    }


    public String defaultOptString(String key, String fallBack) {
        try {
            JsonElement jsonElement = object.get(key);
            if (jsonElement != null)
                return jsonElement.getAsString();
        } catch (Exception ignored) {
        }
        return fallBack;
    }

    public String optString(String key) {
        return defaultOptString(key, "");
    }


    public double optDouble(String key, double fallBack) {
        try {
            JsonElement jsonElement = object.get(key);
            if (jsonElement != null) {
                return jsonElement.getAsDouble();
            }
        } catch (Exception ignored) {
        }
        return fallBack;
    }

    public List<String> getKeys() {
        List<String> tmp = new ArrayList<>();
        object.entrySet().forEach(e -> tmp.add(e.getKey()));
        return tmp;
    }

    public double optDouble(String key) {
        return optDouble(key, 0.0);
    }

    public int getSize() {
        return object.entrySet().size();
    }

    public JsonObject getObject() {
        return object;
    }

    public boolean isNull(String key) {
        return object.has(key) && object.get(key).isJsonNull();
    }

    public JsonHolder put(String values, JsonHolder values1) {
        return put(values, values1.getObject());
    }

    public JsonHolder put(String values, JsonObject object) {
        this.object.add(values, object);
        return this;
    }

    public JsonHolder put(String key, JsonArray jsonElements) {
        this.object.add(key, jsonElements);
        return this;
    }

    public void remove(String header) {
        object.remove(header);
    }

    public JsonElement removeAndGet(String header) {
        return object.remove(header);
    }
}
