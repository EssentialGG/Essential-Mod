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
/*
 * Copyright 2013, Morten Nobel-Joergensen
 *
 * License: The BSD 3-Clause License
 * http://opensource.org/licenses/BSD-3-Clause
 */
package gg.essential.image.imagescaling;

final class Lanczos3Filter implements ResampleFilter {
    private final static float PI_FLOAT = (float) Math.PI;

	private float sincModified(float value)
	{
		return ((float)Math.sin(value)) / value;
	}

	public final float apply(float value)
	{
		if (value==0){
			return 1.0f;
		}
		if (value < 0.0f)
		{
			value = -value;
		}

		if (value < 3.0f)
		{
			value *= PI_FLOAT;
			return sincModified(value) * sincModified(value / 3.0f);
		}
		else
		{
			return 0.0f;
		}
	}

    public float getSamplingRadius() {
        return 3.0f;
    }

    public String getName()
	{
		return "Lanczos3";
	}
}
