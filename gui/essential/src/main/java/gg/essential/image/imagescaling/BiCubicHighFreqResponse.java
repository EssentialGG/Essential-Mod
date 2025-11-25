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
final class BiCubicHighFreqResponse extends gg.essential.image.imagescaling.BiCubicFilter {

	public BiCubicHighFreqResponse() {
		super(-1.f);
	}

	@Override
	public String getName() {
		return "BiCubicHighFreqResponse";
	}

}