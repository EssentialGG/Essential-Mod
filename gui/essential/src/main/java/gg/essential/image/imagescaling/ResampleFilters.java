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

public class ResampleFilters {
	private static BellFilter bellFilter = new BellFilter();
	private static gg.essential.image.imagescaling.BiCubicFilter biCubicFilter = new gg.essential.image.imagescaling.BiCubicFilter();
	private static BiCubicHighFreqResponse biCubicHighFreqResponse = new BiCubicHighFreqResponse();
	private static BoxFilter boxFilter = new BoxFilter();
	private static gg.essential.image.imagescaling.BSplineFilter bSplineFilter = new gg.essential.image.imagescaling.BSplineFilter();
	private static gg.essential.image.imagescaling.HermiteFilter hermiteFilter = new gg.essential.image.imagescaling.HermiteFilter();
	private static gg.essential.image.imagescaling.Lanczos3Filter lanczos3Filter = new gg.essential.image.imagescaling.Lanczos3Filter();
	private static gg.essential.image.imagescaling.MitchellFilter mitchellFilter = new gg.essential.image.imagescaling.MitchellFilter();
	private static gg.essential.image.imagescaling.TriangleFilter triangleFilter = new gg.essential.image.imagescaling.TriangleFilter();

	public static gg.essential.image.imagescaling.ResampleFilter getBiCubicFilter(){
		return biCubicFilter;
	}

	public static gg.essential.image.imagescaling.ResampleFilter getLanczos3Filter(){
		return lanczos3Filter;
	}

}
