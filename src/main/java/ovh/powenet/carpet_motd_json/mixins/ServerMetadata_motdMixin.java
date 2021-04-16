package ovh.powenet.carpet_motd_json.mixins;

import carpet.CarpetSettings;
import net.minecraft.text.LiteralText;
import ovh.powenet.carpet_motd_json.Mod;
import net.minecraft.server.ServerMetadata;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerMetadata.class)
public abstract class ServerMetadata_motdMixin {
	private String cached_customMOTD = null;
	private LiteralText cached_desc = null;
	private final boolean[] parse_not_cached = new boolean[]{true};  // array to emulate pass-by-reference
	@Shadow
	private Text description;
	@Inject(method = "getDescription", at = @At("HEAD"), cancellable = true)
	private void getDescriptionAlternative(CallbackInfoReturnable<Text> cir) {
		if(parse_not_cached[0]) {
			cir.setReturnValue(Mod.toLegacyText(CarpetSettings.customMOTD.equals("_") ? this.description.asString() : CarpetSettings.customMOTD,parse_not_cached));
		} else {
			if(!CarpetSettings.customMOTD.equals(cached_customMOTD)) {
				cached_customMOTD = CarpetSettings.customMOTD;
				cached_desc = Mod.toLegacyText(CarpetSettings.customMOTD.equals("_") ? this.description.asString() : CarpetSettings.customMOTD,parse_not_cached);
			}
			cir.setReturnValue(cached_desc);
		}
		System.out.println("MOTD is not cached:"+parse_not_cached[0]);
		cir.cancel();
	}
}