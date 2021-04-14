package ovh.powenet.carpet_motd_json;

import carpet.CarpetExtension;
import carpet.CarpetServer;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.api.ModInitializer;
import net.minecraft.text.*;
import net.minecraft.util.Identifier;
import java.util.stream.StreamSupport;
import static ovh.powenet.carpet_motd_json.Mod.LegacyTextFormatting.*;

public class Mod implements CarpetExtension,ModInitializer,DedicatedServerModInitializer {
        public void onInitialize() { }
        public void onInitializeServer() { }
        private static final Gson gson = new GsonBuilder().setLenient().create();
        public static LiteralText toLegacyText(String raw) {
                try {
                        return toLegacyText(resolveTextComponents(gson.fromJson(raw,JsonObject.class)));
                } catch(JsonSyntaxException jsx) {
                        if(raw.startsWith("{") || raw.startsWith("[")) // Checking if it was actually intended to be parsed, or just plaintext
                                return new LiteralText(jsx.toString());
                        else
                                return new LiteralText(raw);
                }
        }
        private static LiteralText toLegacyText(JsonObject a) { return new LiteralText(toLegacyTextParent(a)); }
        private static String toLegacyTextParent(JsonObject a) {
                StringBuilder b = new StringBuilder();
                boolean[] p = {false,false,false,false,false};
                char r = 'r';

                if(a.has("color")) {
                        r = Colors.getOrDefault(a.get("color").getAsString(),InvalidColorPlaceholder);
                        b.append(SpecialCharacter).append(r);
                }
                // Yes, this is code duplication. I just hate loops for stuff like this.
                // The lack of "else" is intentional; often text will be both bold and underlined something like that.
                if(a.has("obfuscated") && a.get("obfuscated").getAsBoolean()) {
                        b.append(SpecialCharacter).append(Styles.obfuscated);
                        p[0] = true;
                }
                if(a.has("bold") && a.get("bold").getAsBoolean()) {
                        b.append(SpecialCharacter).append(Styles.bold);
                        p[1] = true;
                }
                if(a.has("strikethrough") && a.get("strikethrough").getAsBoolean()) {
                        b.append(SpecialCharacter).append(Styles.strikethrough);
                        p[2] = true;
                }
                if(a.has("underlined") && a.get("underlined").getAsBoolean()) {
                        b.append(SpecialCharacter).append(Styles.underlined);
                        p[3] = true;
                }
                if(a.has("italic") && a.get("italic").getAsBoolean()) {
                        b.append(SpecialCharacter).append(Styles.italic);
                        p[4] = true;
                }

                b.append(a.get("text").getAsString());

                // This recursively handles extra:[{},{}] tags.
                // The e flag is there so it doesn't handle extra tags inside of other extra tags (intended behavior).
                if(a.has("extra") && a.get("extra").isJsonArray()) {
                        final char R = r; // Necessary for lambda. Idk.
                        StreamSupport.stream(a.remove("extra").getAsJsonArray().spliterator(),false).forEachOrdered(c -> toLegacyTextExtra(c.getAsJsonObject(),p,b,R));
                }

                return b.toString();
        }
        private static void toLegacyTextExtra(JsonObject a,boolean[] p,StringBuilder b,char c) {
                if(a.has("color")) {
                        b.append(SpecialCharacter).append(Colors.getOrDefault(a.get("color").getAsString(),InvalidColorPlaceholder));
                } else if(c != 'r') {
                        b.append(SpecialCharacter).append(c);
                }

                // Again, code duplication. Just trying to make the code a bit more efficient by unrolling a loop.
                if(!a.has("obfuscated") && p[0] || a.has("obfuscated") && a.get("obfuscated").getAsBoolean())
                        b.append(SpecialCharacter).append(Styles.obfuscated);
                if(!a.has("bold") && p[1] || a.has("bold") && a.get("bold").getAsBoolean())
                        b.append(SpecialCharacter).append(Styles.bold);
                if(!a.has("strikethrough") && p[2] || a.has("strikethrough") && a.get("strikethrough").getAsBoolean())
                        b.append(SpecialCharacter).append(Styles.strikethrough);
                if(!a.has("underlined") && p[3] || a.has("underlined") && a.get("underlined").getAsBoolean())
                        b.append(SpecialCharacter).append(Styles.underlined);
                if(!a.has("italic") && p[4] || a.has("italic") && a.get("italic").getAsBoolean())
                        b.append(SpecialCharacter).append(Styles.italic);
                
                b.append(a.get("text").getAsString());
        }
        private static JsonObject resolveTextComponents(JsonObject a) { return resolveTextComponents(a,true); }
        private static JsonObject resolveTextComponents(JsonObject a, boolean e) {
                MutableText b;
                // This if-else chain tests for each type of dynamic text type
                // (except keybinds, since, well, the server doesn't have keybinds)
                // and creates a ParsableText object accordingly, which is parsed
                // with Texts.parse().
                if(a.has("text"))
                        b = new LiteralText(a.get("text").getAsString());
                else if(a.has("translate"))
                        // This line is quite long because it needs to take into account the "with" json array that comes with TranslatableText.
                        // Luckily this isn't that hard, since the JsonArray has an iterator that can
                        b = new TranslatableText(a.remove("translate").getAsString(),a.has("with") && a.get("with").isJsonArray() ? StreamSupport.stream(a.remove("with").getAsJsonArray().spliterator(),false).map(c -> resolveTextComponents(c.getAsJsonObject())).toArray() : null);
                else if(a.has("score"))
                        b = new ScoreText(a.get("score").getAsJsonObject().get("name").getAsString(),a.remove("score").getAsJsonObject().get("objective").getAsString());
                else if(a.has("selector"))
                        b = new SelectorText(a.remove("selector").getAsString());
                else if(a.has("nbt"))
                        // Block NBT doesn't work because of a weird line in the World.getBlockEntity(pos)
                        // method that checks if the current thread is the server main thread (which in this
                        // case it never is), and if it isn't, it just returns null. Tried to get around
                        // this with a Mixin but it both got overcomplicated and just didn't work. Idk.
                        /*if(a.has("block"))
                                b = new NbtText.BlockNbtText(a.remove("nbt").toString(),a.has("interpret") && a.remove("interpret").getAsBoolean(),a.remove("block").getAsString());
                        else*/ if(a.has("entity"))
                                b = new NbtText.EntityNbtText(a.remove("nbt").toString(),a.has("interpret") && a.remove("interpret").getAsBoolean(),a.remove("entity").getAsString());
                        else if(a.has("storage"))
                                b = new NbtText.StorageNbtText(a.remove("nbt").toString(),a.has("interpret") && a.remove("interpret").getAsBoolean(),new Identifier(a.remove("storage").getAsString()));
                        else
                                throw new JsonSyntaxException("Invalid nbt tag (Note: block tag not supported)");
                else
                        throw new JsonSyntaxException("Invalid text component (No text, translate, score, selector, or nbt tag found)");

                // This recursively handles extra:[{},{}] tags.
                // The e flag is there so it doesn't handle extra tags inside of other extra tags (intended behavior).
                if(e && a.has("extra") && a.get("extra").isJsonArray()) {
                        a.add("extra",gson.toJsonTree(StreamSupport.stream(a.remove("extra").getAsJsonArray().spliterator(),false).map(c -> resolveTextComponents(c.getAsJsonObject(),false)).toArray()).getAsJsonArray());
                }

                try {
                        a.addProperty("text",Texts.parse(CarpetServer.minecraft_server.getCommandSource(),b,null,0).getString());
                } catch(Exception x) {
                        a.addProperty("text",x.getClass().getTypeName()+" thrown");
                }
                return a;
        }
        @SuppressWarnings("unused")  // I _am_ using this class! Shut up IntelliJ!
        public static final class LegacyTextFormatting {
                public static final char SpecialCharacter = '§'; // U+00A7
                public static final char InvalidColorPlaceholder = 'r'; // reset
                public static final ImmutableMap<String,Character> Colors = new ImmutableMap.Builder<String,Character>()
                        .put("black",'0')
                        .put("dark_blue",'1')
                        .put("dark_green",'2')
                        .put("dark_aqua",'3')
                        .put("dark_red",'4')
                        .put("dark_purple",'5')
                        .put("gold",'6')
                        .put("gray",'7')
                        .put("dark_gray",'8')
                        .put("blue",'9')
                        .put("green",'a')
                        .put("aqua",'b')
                        .put("red",'c')
                        .put("light_purple",'d')
                        .put("yellow",'e')
                        .put("white",'f')
                        .put("reset",'r').build();
                public static final class Styles {
                        public static final char
                                obfuscated = 'k',
                                bold = 'l',
                                strikethrough = 'm',
                                underlined = 'n',
                                italic = 'o';
                        private Styles() {} // can't be instantiated, intended
                }
                private LegacyTextFormatting() {}
        }
}
