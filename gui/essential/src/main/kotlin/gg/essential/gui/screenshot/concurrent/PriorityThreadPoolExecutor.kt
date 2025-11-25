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

import java.util.*
import java.util.concurrent.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock


class PriorityThreadPoolExecutor(numThreads: Int) : ThreadPoolExecutor(
    numThreads, numThreads, 0L, TimeUnit.MILLISECONDS, MultiBlockingQueue(),
    ScreenshotWorkerThreadFactory
) {
    private val queue = getQueue() as MultiBlockingQueue

    override fun <T : Any?> newTaskFor(callable: Callable<T>): RunnableFuture<T> {
        return if (callable is PrioritizedCallable<T>) PrioritizedTask(callable) else FutureTask(callable)
    }

    fun stealBackgroundTask() = queue.pollBackgroundTask()

    /**
     * An unbounded blocking queue backed by two separate queues with an option to poll from the lower priority one.
     */
    private class MultiBlockingQueue : AbstractUnboundedBlockingQueue<Runnable>() {
        private val regularQueue = PriorityQueue(10, PrioritizedCallable.comparator)
        private val backgroundQueue = PriorityQueue(10, PrioritizedCallable.comparator)

        // Any task which is greater than or equal to this task goes into the background queue, others into the regular one
        private val backgroundTaskDiscriminator = PrioritizedTask(
            object : PrioritizedCallable<Unit>(Int.MAX_VALUE, Int.MAX_VALUE, Int.MIN_VALUE) {
                override fun call() {}
            }
        )

        override val size: Int
            get() = lock.withLock { regularQueue.size + backgroundQueue.size }

        override fun doOffer(e: Runnable): Boolean {
            val queue = if (PrioritizedCallable.comparator.compare(e, backgroundTaskDiscriminator) >= 0) {
                backgroundQueue
            } else {
                regularQueue
            }
            return queue.offer(e)
        }

        override fun poll(): Runnable? = lock.withLock {
            regularQueue.poll() ?: backgroundQueue.poll()
        }

        override fun peek(): Runnable? = lock.withLock {
            regularQueue.peek() ?: backgroundQueue.peek()
        }

        fun pollBackgroundTask(): Runnable? = lock.withLock {
            backgroundQueue.poll()
        }
    }

    /**
     * An unbounded blocking queue.
     */
    private abstract class AbstractUnboundedBlockingQueue<T>() : AbstractQueue<T>(), BlockingQueue<T> {
        protected val lock = ReentrantLock()
        private val notEmpty = lock.newCondition()

        override fun iterator(): MutableIterator<T> = throw UnsupportedOperationException()

        protected abstract fun doOffer(e: T): Boolean

        override fun offer(e: T): Boolean = lock.withLock {
            doOffer(e).also {
                notEmpty.signal()
            }
        }

        override fun offer(e: T, timeout: Long, unit: TimeUnit): Boolean = offer(e)

        override fun put(e: T) {
            offer(e)
        }

        override fun poll(timeout: Long, unit: TimeUnit): T? {
            var nanos = unit.toNanos(timeout)
            lock.withLock {
                while (nanos > 0) {
                    val result = poll()
                    if (result != null) {
                        return result
                    }
                    nanos = notEmpty.awaitNanos(nanos)
                }
                return null
            }
        }

        override fun take(): T {
            lock.withLock {
                while (true) {
                    val result = poll()
                    if (result != null) {
                        return result
                    }
                    notEmpty.await()
                }
            }
        }

        override fun remainingCapacity(): Int = Int.MAX_VALUE

        override fun drainTo(c: MutableCollection<in T>): Int = drainTo(c, Int.MAX_VALUE)

        override fun drainTo(c: MutableCollection<in T>, max: Int): Int {
            var drained = 0
            while (drained < max) {
                c.add(poll() ?: break)
                drained++
            }
            return drained
        }
    }
}
