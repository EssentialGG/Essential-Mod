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
package gg.essential.mixins.impl.client.gui;

import com.google.common.collect.MapMaker;

import java.util.AbstractList;
import java.util.List;
import java.util.Map;

/**
 * With 1.21.9, the vanilla list widget includes an entry for the title.
 * Our dragging code has been built to make various assumptions about the indices in that list, which are broken
 * in various ways by this 1.21.9 change.
 * <br>
 * To work around this, this class wrap around the inner list and hides the first entry.
 * Since the dragging code also relies on comparing the lists via reference equality, a WeakMap-backed {@link #of(List)}
 * method is provided which returns the same wrapper instance for a given inner list on every call.
 */
public class ResourcePackListWrapper<E> extends AbstractList<E> {
    // Using guava's weak map instead of the JRE one because guava uses identity rather than hashCode/equals, so we can
    // independently store two list that happen to have identical content right now.
    private static final Map<List<?>, List<?>> INSTANCES = new MapMaker().weakKeys().makeMap();

    @SuppressWarnings("unchecked")
    public static <E> List<E> of(List<E> inner) {
        List<E> wrapper = (List<E>) INSTANCES.get(inner);
        if (wrapper == null) {
            wrapper = new ResourcePackListWrapper<>(inner);
            INSTANCES.put(inner, wrapper);
        }
        return wrapper;
    }

    private final List<E> inner;

    public ResourcePackListWrapper(List<E> inner) {
        this.inner = inner;
    }

    @Override
    public E get(int index) {
        return inner.get(index + 1);
    }

    @Override
    public int size() {
        return inner.size() - 1;
    }

    @Override
    public E set(int index, E element) {
        return inner.set(index + 1, element);
    }

    @Override
    public void add(int index, E element) {
        inner.add(index + 1, element);
    }

    @Override
    public E remove(int index) {
        return inner.remove(index + 1);
    }
}
