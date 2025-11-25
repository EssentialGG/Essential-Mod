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
 * @author Heinz Doerr
 */
class BiCubicFilter implements ResampleFilter {

	final protected float a;

	public BiCubicFilter() {
		a= -0.5f;
	}

	protected BiCubicFilter(float a) {
		this.a= a;
	}

	public final float apply(float value) {
		if (value == 0)
			return 1.0f;
		if (value < 0.0f)
			value = -value;
		float vv= value * value;
		if (value < 1.0f) {
			return (a + 2f) * vv * value - (a + 3f) * vv + 1f;
		}
		if (value < 2.0f) {
			return a * vv * value - 5 * a * vv + 8 * a * value - 4 * a;
		}
		return 0.0f;
	}

    public float getSamplingRadius() {
        return 2.0f;
    }

    public String getName()
	{
		return "BiCubic"; // also called cardinal cubic spline
	}
}
