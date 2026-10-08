package fr.imero.mercuriale.client.mixin.aeternum;

import fr.imero.mercuriale.client.lang.LanguageOverride;
import net.minecraft.client.resources.language.ClientLanguage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientLanguage.class)
public abstract class ClientLanguageMixin {
	@Inject(method = "getOrDefault(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;",
		at = @At("HEAD"), cancellable = true)
	private void mercuriale$aeternumGet(String key, String fallback, CallbackInfoReturnable<String> cir) {
		String value = LanguageOverride.lookup(this, key);
		if (value != null) {
			cir.setReturnValue(value);
		}
	}

	@Inject(method = "has(Ljava/lang/String;)Z", at = @At("HEAD"), cancellable = true)
	private void mercuriale$aeternumHas(String key, CallbackInfoReturnable<Boolean> cir) {
		if (LanguageOverride.lookup(this, key) != null) {
			cir.setReturnValue(true);
		}
	}
}
