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
    @Shadow private Text description;
    @Inject(method = "getDescription", at = @At("HEAD"), cancellable = true)
    private void getDescriptionAlternative(CallbackInfoReturnable<Text> cir) {
        if(!CarpetSettings.customMOTD.equals(cached_customMOTD)) {
            cached_customMOTD = CarpetSettings.customMOTD;
            cached_desc = Mod.toLegacyText(cached_customMOTD.equals("_") ? this.description.asString() : cached_customMOTD);
        }
        cir.setReturnValue(cached_desc);
        cir.cancel();
    }
}