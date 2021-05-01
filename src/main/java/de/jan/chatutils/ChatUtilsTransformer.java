package de.jan.chatutils;

import net.labymod.addon.AddonTransformer;
import net.labymod.api.TransformerType;

public class ChatUtilsTransformer extends AddonTransformer {

  @Override
  public void registerTransformers() {
    this.registerTransformer(TransformerType.VANILLA, "chatutils.mixin.json");
  }
}
