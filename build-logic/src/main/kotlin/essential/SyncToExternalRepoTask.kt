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
package essential

import org.gradle.api.DefaultTask
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.*
import org.gradle.process.ExecOperations
import org.gradle.process.ExecSpec
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.Path
import javax.inject.Inject
import kotlin.io.path.pathString

private val sourceCommitRegex = Regex("Source-Commit: (\\w+)")

abstract class SyncToExternalRepoTask : DefaultTask() {

    @get:Input
    abstract val targetRepoPath: Property<Path>
    private val _targetRepoPath by targetRepoPath

    @get:Input
    abstract val targetDirectories: ListProperty<String>

    @get:Input
    abstract val sourceDirectories: ListProperty<String>

    @get:Input
    abstract val replacements: ListProperty<Pair<String, String>>

    private val srcRepoPath = project.rootDir.toPath()

    @get:Inject
    protected abstract val execOperations: ExecOperations

    @TaskAction
    fun sync() {
        //make sure the target directories exist in the target repository
        targetDirectories.get().forEach { directory ->
            val directoryPath = _targetRepoPath.resolve(directory)
            if (!Files.exists(directoryPath)) {
                Files.createDirectories(directoryPath)
            }
        }
        // get synced commit(s) in the target repo
        val targetFilters = targetDirectories.get().toTypedArray()
        val syncedCommitMessages = git(_targetRepoPath, "log", "--format=%b", "-E", "--grep=${sourceCommitRegex.pattern}", "--", *targetFilters)
        // parse the source commit hash(es)
        val syncedCommits = sourceCommitRegex.findAll(syncedCommitMessages).map { it.groupValues[1] }.toSet()
        val latestSyncedCommit = syncedCommits.firstOrNull()
        // create revision range of "from last synced to head"
        val revisionsSinceLastSyncedCommit = if (latestSyncedCommit == null) "HEAD" else "$latestSyncedCommit..HEAD"
        // get commits to sync
        val sourceFilters = sourceDirectories.get().toTypedArray()
        // Here we use a format string without speficying the leading `format:` which results in linebreaks following
        // each line. We then split by linebreak and drop the last one as the trailing linebreak results in a single
        // empty string
        val commitsToSync = git(srcRepoPath, "log", "--format=%H %s", "--reverse", "--topo-order", revisionsSinceLastSyncedCommit, "--", *sourceFilters)
            .split("\n")
            .dropLast(1)
        if (commitsToSync.isEmpty()) {
            println("No commits to sync")
            return
        }
        commitsToSync.forEach { commitAndSubject ->
            val (commit, subject) = commitAndSubject.split(" ", limit = 2)
            if (commit in syncedCommits) {
                return@forEach
            }
            println("Applying $commit $subject")
            // get diff of current commit
            val diff = git(srcRepoPath, "show",  commit, "--remerge-diff", "--", *sourceFilters)
            // process diff by applying replacements
            val processed = replacements.get().fold(diff) { processing, (replaced, replacement) ->
                processing.replace(replaced, replacement)
            }
            // apply commit diff using stdin
            try {
                git(_targetRepoPath, "apply", "--allow-empty") {
                    standardInput = processed.byteInputStream()
                }
            } catch (e: Exception) {
                git(_targetRepoPath, "apply", "--reject", "--allow-empty") {
                    standardInput = processed.byteInputStream()
                }
            }
            // ignore commit if diff was empty (e.g. trivial merge commit)
            if (git(_targetRepoPath, "status", "--porcelain").isBlank()) {
                return@forEach
            }
            // stage changes
            git(_targetRepoPath, "add", "--", *targetFilters)
            // get source commit message
            val srcCommitMsg = git(srcRepoPath, "show", "--format=%B", "--no-patch", commit)
            val srcCommitData = configureCommitData(commit)
            // commit changes
            git(_targetRepoPath, "commit", "-m", "$srcCommitMsg\n" +
                    "Source-Commit: $commit") {
                environment(srcCommitData)
            }
        }
    }

    private fun configureCommitData(hash: String): Map<String, String> =
        mapOf(
            "GIT_COMMITER_NAME" to git(srcRepoPath, "show", "--format=%cn", "--no-patch", hash),
            "GIT_COMMITER_EMAIL" to git(srcRepoPath, "show", "--format=%ce", "--no-patch", hash),
            "GIT_COMMITER_DATE" to git(srcRepoPath, "show", "--format=%ai", "--no-patch", hash),
            "GIT_AUTHOR_NAME" to git(srcRepoPath, "show", "--format=%an", "--no-patch", hash),
            "GIT_AUTHOR_EMAIL" to git(srcRepoPath, "show", "--format=%ae", "--no-patch", hash),
            "GIT_AUTHOR_DATE" to git(srcRepoPath, "show", "--format=%ai", "--no-patch", hash),
        )

    private fun git(workingDirectory: Path, command: String, vararg args: String, configure: ExecSpec.() -> Unit = {}): String {
        val stream = ByteArrayOutputStream()
        execOperations.exec {
            commandLine("git")
            args("-C", workingDirectory.pathString)
            args(command)
            args(*args)

            standardOutput = stream

            configure()

            // If we are on windows, we need to properly escape arguments passed
            if (System.getProperty("os.name").contains("windows", ignoreCase = true)) {
                this.args = this.args.map { arg ->
                    // https://learn.microsoft.com/en-us/cpp/cpp/main-function-command-line-args?view=msvc-170#parsing-c-command-line-arguments
                    arg.split('"').joinToString("\\\"", prefix = "\"", postfix = "\"") { part ->
                        part + part.takeLastWhile { it == '\\' }
                    }
                }
            }
        }
        return stream.toString()
    }
}
