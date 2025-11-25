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

import java.awt.*;

/**
 * This class let you create dimension constrains based on a actual image.
 *
 * Class may be subclassed to create user defined behavior. To do this you need to overwrite
 * the method getDimension(Dimension).
 */
public class DimensionConstrain {
	protected DimensionConstrain ()
	{
	}

	/**
	 * Will always return a dimension with positive width and height;
	 * @param dimension of the unscaled image
	 * @return the dimension of the scaled image
	 */
	public Dimension getDimension(Dimension dimension){
		return dimension;
	}

	public DimensionConstrain coerceAtLeast(int min) {
		return new DimensionConstrain() {
			@Override
			public Dimension getDimension(Dimension dimension) {
				Dimension dim = DimensionConstrain.this.getDimension(dimension);
				return new Dimension(Math.max(dim.width, min) , Math.max(dim.height, min));
			}
		};
	}

	/**
	 * Used when the destination size is fixed. This may not keep the image aspect radio
	 * @param width destination dimension width
	 * @param height destination dimension height
	 * @return  destination dimension (width x height)
	 */
	public static DimensionConstrain createAbsolutionDimension(final int width, final int height){
		assert width>0 && height>0:"Dimension must be a positive integer";
		return new DimensionConstrain(){
			public Dimension getDimension(Dimension dimension) {
				return new Dimension(width, height);
			}
		};
	}

	/**
	 * Forces the image to keep radio and be keeped within the width and height
	 * @param width
	 * @param height
	 * @return
	 */
	public static DimensionConstrain createMaxDimension(int width, int height){
		return createMaxDimension(width, height,false);
	}

	/**
	 * Forces the image to keep radio and be keeped within the width and height.
	 * @param width
	 * @param height
	 * @param neverEnlargeImage if true only a downscale will occour
	 * @return
	 */
	public static DimensionConstrain createMaxDimension(final int width, final int height, final boolean neverEnlargeImage){
		assert width >0 && height > 0 : "Dimension must be larger that 0";
		final double scaleFactor = width/(double)height;
		return new DimensionConstrain(){
			public Dimension getDimension(Dimension dimension) {
				double srcScaleFactor = dimension.width/(double)dimension.height;
				double scale;
				if (srcScaleFactor>scaleFactor){
					scale = width/(double)dimension.width;
				}
				else{
					scale = height/(double)dimension.height;
				}
				if (neverEnlargeImage){
					scale = Math.min(scale,1);
				}
				int dstWidth = (int)Math.round (dimension.width*scale);
				int dstHeight = (int) Math.round(dimension.height*scale);
				return new Dimension(dstWidth, dstHeight);
			}
		};
	}


}
