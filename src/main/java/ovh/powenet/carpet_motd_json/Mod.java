package ovh.powenet.carpet_motd_json;

import carpet.CarpetExtension;
import carpet.CarpetServer;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.api.ModInitializer;
import net.md_5.bungee.api.chat.*;
import net.md_5.bungee.chat.*;
import net.minecraft.text.*;
import net.minecraft.util.Identifier;

public class Mod implements CarpetExtension,ModInitializer,DedicatedServerModInitializer {
        public void onInitialize() { }
        public void onInitializeServer() { }
        private static final Gson gson = new GsonBuilder().setLenient().
                registerTypeAdapter(BaseComponent.class, new ComponentSerializer()).
                registerTypeAdapter(TextComponent.class, new TextComponentSerializer()).
                registerTypeAdapter(TranslatableComponent.class, new TranslatableComponentSerializer()).
                registerTypeAdapter(ScoreComponent.class, new ScoreComponentSerializer()).
                registerTypeAdapter(SelectorComponent.class, new SelectorComponentSerializer()).
                create();
        public static Text parse(String raw) { return new LiteralText(parseToComponent(parseJsonObject(gson.fromJson(raw,JsonObject.class))).toLegacyText()); }
        private static BaseComponent parseToComponent(JsonObject a) { return gson.fromJson(a,BaseComponent.class); }
        private static JsonObject parseJsonObject(JsonObject a) { return parseJsonObject(a,false); }
        private static JsonObject parseJsonObject(JsonObject a,boolean e) {
                MutableText b;
                if(a.has("text"))
                        b = new LiteralText(a.get("text").getAsString());
                else if(a.has("translate"))
                        b = new TranslatableText(a.remove("translate").getAsString(),a.has("with") && a.get("with").isJsonArray() ? (Lists.newArrayList(a.remove("with").getAsJsonArray().iterator()).stream()).map(c -> parseJsonObject(c.getAsJsonObject())).toArray() : null);
                else if(a.has("score"))
                        b = new ScoreText(a.get("score").getAsJsonObject().get("name").getAsString(),a.remove("score").getAsJsonObject().get("objective").getAsString());
                else if(a.has("selector"))
                        b = new SelectorText(a.remove("selector").getAsString());
                else if(a.has("nbt"))
                        /*if(a.has("block"))
                                b = new NbtText.BlockNbtText(a.remove("nbt").toString(),a.has("interpret") && a.remove("interpret").getAsBoolean(),a.remove("block").getAsString());
                        else*/ if(a.has("entity"))
                                b = new NbtText.EntityNbtText(a.remove("nbt").toString(),a.has("interpret") && a.remove("interpret").getAsBoolean(),a.remove("entity").getAsString());
                        else if(a.has("storage"))
                                b = new NbtText.StorageNbtText(a.remove("nbt").toString(),a.has("interpret") && a.remove("interpret").getAsBoolean(),new Identifier(a.remove("storage").getAsString()));
                        else
                                throw new JsonSyntaxException("Invalid nbt tag (Note: block tag not supported)");
                                // Block NBT doesn't work because of a weird line in the World.getBlockEntity(pos)
                                // method that checks if the current thread is the server main thread (which in this
                                // case it never is), and if it isn't, it just returns null. Tried to get around
                                // this with a Mixin but it both got overcomplicated and just didn't work. Idk.
                else
                        throw new JsonSyntaxException("Invalid text component (No text, translate, score, selector, or nbt tag found)");

                // This recursively handles extra:[{},{}] tags.
                // The e flag is there so it doesn't handle extra tags inside of other extra tags (intended behavior).
                if(!e && a.has("extra") && a.get("extra").isJsonArray()) {
                        a.add("extra",gson.toJsonTree(Lists.newArrayList(a.remove("extra").getAsJsonArray().iterator()).stream().map(c -> {
                                return parseJsonObject(c.getAsJsonObject(),true);
                        }).toArray()).getAsJsonArray());
                }

                try {
                        a.addProperty("text",Texts.parse(CarpetServer.minecraft_server.getCommandSource(),b,null,0).getString());
                } catch (CommandSyntaxException x) {
                        a.addProperty("text","CommandSyntaxException thrown: "+x);
                }
                return a;
        }
}
