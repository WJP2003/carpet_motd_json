package ovh.powenet.carpet_motd_json.mixins;

import carpet.CarpetSettings;
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
    @Shadow private Text description;
    @Inject(method = "getDescription", at = @At("HEAD"), cancellable = true)
    private void getDescriptionAlternative(CallbackInfoReturnable<Text> cir) {
        cir.setReturnValue(Mod.toLegacyText(CarpetSettings.customMOTD.equals("_") ? this.description.asString() : CarpetSettings.customMOTD));
        cir.cancel();
    }
}