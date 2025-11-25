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


/**
 * A B-spline resample filter.
 */
final class BSplineFilter implements ResampleFilter
{
	public float getSamplingRadius() {
		return 2.0f;
	}

	public final float apply(float value)
	{
		if (value < 0.0f)
		{
			value = - value;
		}
		if (value < 1.0f)
		{
			float tt = value * value;
			return 0.5f * tt * value - tt + (2.0f / 3.0f);
		}
		else
		if (value < 2.0f)
		{
			value = 2.0f - value;
			return (1.0f / 6.0f) * value * value * value;
		}
		else
		{
			return 0.0f;
		}
	}

	public String getName() {
		return "BSpline";
	}
}
