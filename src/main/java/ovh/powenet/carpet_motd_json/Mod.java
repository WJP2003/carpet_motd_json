package ovh.powenet.carpet_motd_json;

import carpet.CarpetExtension;
import carpet.CarpetServer;
import com.google.gson.*;
import it.unimi.dsi.fastutil.objects.Object2CharOpenHashMap;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.api.ModInitializer;
import net.minecraft.text.*;
import net.minecraft.util.Identifier;

import java.util.stream.StreamSupport;

import static ovh.powenet.carpet_motd_json.Mod.LegacyTextFormatting.*;

public class Mod implements CarpetExtension, ModInitializer, DedicatedServerModInitializer {
	public void onInitialize() { }

	public void onInitializeServer() { }

	private static final Gson gson = new GsonBuilder().setLenient().create();

	public static LiteralText toLegacyText(final String raw) {
		try {
			return toLegacyText(resolveTextComponents(gson.fromJson(raw,JsonObject.class)));
		} catch(JsonSyntaxException jsx) {
			if(raw.startsWith("{")) // Checking if it was actually intended to be parsed, or just plaintext
				return new LiteralText(jsx.toString());
			else
				return new LiteralText(raw);
		}
	}

	private static LiteralText toLegacyText(final JsonObject a) { return new LiteralText(toLegacyTextParent(a)); }

	private static String toLegacyTextParent(final JsonObject a) {
		StringBuilder b = new StringBuilder();
		boolean[] p = {false,false,false,false,false};
		char r = 'r';

		if(a.has("color")) {
			r = Colors.getOrDefault(a.get("color").getAsString(),InvalidColorPlaceholder);
			b.append(SpecialCharacter).append(r);
		} {
			JsonElement q = a.get("obfuscated");
			if(q != null && q.getAsBoolean()) {
				b.append(SpecialCharacter).append(Styles.obfuscated);
				p[0] = true;
			}
			q = a.get("bold");
			if(q != null && q.getAsBoolean()) {
				b.append(SpecialCharacter).append(Styles.bold);
				p[1] = true;
			}
			q = a.get("strikethrough");
			if(q != null && q.getAsBoolean()) {
				b.append(SpecialCharacter).append(Styles.strikethrough);
				p[2] = true;
			}
			q = a.get("underlined");
			if(q != null && q.getAsBoolean()) {
				b.append(SpecialCharacter).append(Styles.underlined);
				p[3] = true;
			}
			q = a.get("italic");
			if(q != null && q.getAsBoolean()) {
				b.append(SpecialCharacter).append(Styles.italic);
				p[4] = true;
			}
		}
		b.append(a.get("text").getAsString());
		// This recursively handles extra:[{},{}] tags.
		// The e flag is there so it doesn't handle extra tags inside of other extra tags (intended behavior).
		{
			JsonElement q = a.remove("extra");
			if(q != null && q.isJsonArray()) {
				final char R = r; // Necessary for lambda. Idk.
				StreamSupport.stream(q.getAsJsonArray().spliterator(),false)
					.forEachOrdered(c -> toLegacyTextExtra(c.getAsJsonObject(),p,b,R));
			}
		}
		return b.toString();
	}

	private static void toLegacyTextExtra(final JsonObject a,boolean[] p,StringBuilder b,char c) {
		if(a.has("color")) {
			b.append(SpecialCharacter).append(Colors.getOrDefault(a.get("color").getAsString(),InvalidColorPlaceholder));
		} else if(c != 'r') {
			b.append(SpecialCharacter).append(c);
		} {
			JsonElement q = a.get("obfuscated");
			if(q == null && p[0] || q != null && q.getAsBoolean())
				b.append(SpecialCharacter).append(Styles.obfuscated);
			q = a.get("bold");
			if(q == null && p[1] || q != null && q.getAsBoolean())
				b.append(SpecialCharacter).append(Styles.bold);
			q = a.get("strikethrough");
			if(q == null && p[2] || q != null && q.getAsBoolean())
				b.append(SpecialCharacter).append(Styles.strikethrough);
			q = a.get("underlined");
			if(q == null && p[3] || q != null && q.getAsBoolean())
				b.append(SpecialCharacter).append(Styles.underlined);
			q = a.get("italic");
			if(q == null && p[4] || q != null && q.getAsBoolean())
				b.append(SpecialCharacter).append(Styles.italic);
		}
		b.append(a.get("text").getAsString());
	}

	private static JsonObject resolveTextComponents(JsonObject a) { return resolveTextComponents(a,true); }

	private static JsonObject resolveTextComponents(JsonObject a,final boolean e) {
		MutableText b;
		// This if-else chain tests for each type of dynamic text type
		// (except keybinds, since, well, the server doesn't have keybinds)
		// and creates a ParsableText object accordingly, which is parsed
		// with Texts.parse().
		if(a.has("text")) {
			b = new LiteralText(a.get("text").getAsString());
		} else if(a.has("translate")) {
			// This line is quite long because it needs to take into account the "with" json array that comes with TranslatableText.
			// Luckily this isn't that hard, since the JsonArray has an iterator that can
			b = new TranslatableText(a.remove("translate").getAsString(),a.has("with") && a.get("with").isJsonArray() ? StreamSupport.stream(a.remove("with").getAsJsonArray().spliterator(),false).map(c -> resolveTextComponents(c.getAsJsonObject())).toArray() : null);
		} else if(a.has("score")) {
			b = new ScoreText(a.get("score").getAsJsonObject().get("name").getAsString(),a.remove("score").getAsJsonObject().get("objective").getAsString());
		} else if(a.has("selector")) {
			b = new SelectorText(a.remove("selector").getAsString());
		} else if(a.has("nbt")) {
			// Block NBT doesn't work because of a weird line in the World.getBlockEntity(pos)
			// method that checks if the current thread is the server main thread (which in this
			// case it never is), and if it isn't, it just returns null. Tried to get around
			// this with a Mixin but it both got overcomplicated and just didn't work. Idk.
                        /*if(a.has("block"))
                                b = new NbtText.BlockNbtText(a.remove("nbt").toString(),a.has("interpret") && a.remove("interpret").getAsBoolean(),a.remove("block").getAsString());
                        else*/
			if(a.has("entity"))
				b = new NbtText.EntityNbtText(a.remove("nbt").toString(),a.has("interpret") && a.remove("interpret").getAsBoolean(),a.remove("entity").getAsString());
			else if(a.has("storage"))
				b = new NbtText.StorageNbtText(a.remove("nbt").toString(),a.has("interpret") && a.remove("interpret").getAsBoolean(),new Identifier(a.remove("storage").getAsString()));
			else
				throw new JsonSyntaxException("Invalid nbt tag (Note: block tag not supported)");
		} else {
			throw new JsonSyntaxException("Invalid text component (No text, translate, score, selector, or nbt tag found)");
		}

		// This recursively handles extra:[{},{}] tags.
		// The e flag is there so it doesn't handle extra tags inside of other extra tags (intended behavior).
		if(e) {
			JsonElement q = a.remove("extra");
			if(q != null && q.isJsonArray()) {
				a.add("extra",
					gson.toJsonTree(
						StreamSupport.stream(
							q.getAsJsonArray().spliterator(),false
						).map(c -> resolveTextComponents(c.getAsJsonObject(),false)
						).toArray()
					).getAsJsonArray()
				);
			}
		} {
			String r;
			try {
				r = Texts.parse(CarpetServer.minecraft_server.getCommandSource(),b,null,0).getString();
				a.addProperty("text",r);
			} catch(Exception x) {
				a.addProperty("text",x.getClass().getTypeName());
			}
		}
		return a;
	}

	@SuppressWarnings("unused")  // I _am_ using this class! Shut up IntelliJ!
	public static final class LegacyTextFormatting {
		public static final char SpecialCharacter = '§'; // U+00A7
		public static final char InvalidColorPlaceholder = 'r'; // reset
		public static final Object2CharOpenHashMap<String> Colors = new Object2CharOpenHashMap<>(
			new String[]{"black","dark_blue","dark_green","dark_aqua","dark_red","dark_purple","gold","gray","dark_gray","blue","green","aqua","red","light_purple","yellow","white","reset"},
			new char[]{'0','1','2','3','4','5','6','7','8','9','a','b','c','d','e','f','r'});
		public static final class Styles {
			public static final char obfuscated = 'k',bold = 'l',strikethrough = 'm',underlined = 'n',italic = 'o';
			private Styles() { } // can't be instantiated, intended
		}
		private LegacyTextFormatting() { }
	}
}
