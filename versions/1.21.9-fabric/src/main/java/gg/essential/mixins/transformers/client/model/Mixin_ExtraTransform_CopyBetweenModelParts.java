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
package gg.essential.mixins.transformers.client.model;

import gg.essential.mixins.DummyTarget;
import org.spongepowered.asm.mixin.Mixin;

// As of 1.21.9, all the auxiliary feature renderers setup entirely separate models (which extends from
// PlayerEntityModel) and separately re-setup all the transforms on those (which will naturally be affected by our
// existing mixins just like the main model) instead of explicitly copying the transforms from the pre-configured main
// model.
// They have to do it that way because the order in which the models are rendered is somewhat undefined now.
// Since no more copying is happening, this mixin is no longer needed.
@Mixin(DummyTarget.class)
public abstract class Mixin_ExtraTransform_CopyBetweenModelParts {
}
