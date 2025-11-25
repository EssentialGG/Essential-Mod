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
package gg.essential.gui.screenshot.concurrent

import java.util.concurrent.Callable
import java.util.concurrent.Executor
import java.util.concurrent.FutureTask


/**
 * Runnable wrapper class that allows the thread pool to prioritize what it works on
 */
abstract class PrioritizedCallable<T>(
    private val windowIndex: Int,
    private val providerPriority: Int,
    private val indexInWindow: Int
) : Callable<T> {

    companion object {
        //Comparing based and Runnable and casting is required because otherwise the executors linked blocking queue
        //Receives an argument of incorrect type
        val comparator = compareBy<Runnable> { (it as PrioritizedTask<*>).prioritizedCallback.windowIndex }
            .thenByDescending { (it as PrioritizedTask<*>).prioritizedCallback.providerPriority }
            .thenBy { (it as PrioritizedTask<*>).prioritizedCallback.indexInWindow }

        const val FOCUS: Int = 10 // The focus image has the highest priority
        const val MIN_RES: Int = 1
        const val REGULAR: Int = 0
        const val CACHE_WRITE: Int = 0 // uses max window index to run after any REGULAR
        const val PRECOMPUTE: Int = -1 // lower than cache writes, so we only start new tasks when everything's done

        fun Executor.withPriority(windowIndex: Int, providerPriority: Int, indexInWindow: Int) = Executor { runnable ->
            if (runnable is PrioritizedTask<*>) {
                execute(runnable)
            } else {
                val callable = object : PrioritizedCallable<Unit>(windowIndex, providerPriority, indexInWindow) {
                    override fun call() {
                        runnable.run()
                    }
                }
                execute(PrioritizedTask(callable))
            }
        }
    }
}

class PrioritizedTask<D>(val prioritizedCallback: PrioritizedCallable<D>) : FutureTask<D>(prioritizedCallback)
