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
package gg.essential.model.molang

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonPrimitive
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.truncate
import kotlin.random.Random

@Serializable(MolangSerializer::class)
data class Molang(val expression: MolangExpression) {
    private val compiled: MolangEvalImpl =
        if (expression is StatementsExpr && expression.statements.last() is ReturnExpr) {
            // Complex molang expressions require a `return` statement to return a value other than 0.
            // In most cases that'll be the last expression, so we can easily optimize it to not have to emit a
            // [ReturnExpr.Return] exception in this case.
            StatementsWithResult(expression.statements.dropLast(1), expression.statements.last())
        } else if (expression is ReturnExpr) {
            expression.inner
        } else {
            expression
        }

    fun eval(context: MolangContext): Float {
        return try {
            compiled.eval(context)
        } catch (e: ReturnExpr.Return) {
            e.value
        }
    }

    private class StatementsWithResult(val statements: List<MolangExpression>, val result: MolangExpression) : MolangEvalImpl {
        override fun eval(context: MolangContext): Float {
            for (statement in statements) {
                statement.eval(context)
            }
            return result.eval(context)
        }
    }

    companion object {
        val ZERO = Molang(MolangExpression.ZERO)
        val ONE = Molang(MolangExpression.ONE)

        fun literal(value: Float): Molang = Molang(LiteralExpr(value))
    }
}

// Private interface which can't be actually private because `MolangExpression` inherits from it
interface MolangEvalImpl {
    fun eval(context: MolangContext): Float
}

sealed interface MolangExpression : MolangEvalImpl {
    companion object {
        val ZERO = LiteralExpr(0f)
        val ONE = LiteralExpr(1f)
    }
}

interface MolangVariable {
    val name: String
    fun assign(context: MolangContext, value: Float)
}

data class LiteralExpr(val value: Float) : MolangExpression {
    override fun eval(context: MolangContext): Float = value
}

data class NegExpr(val inner: MolangExpression) : MolangExpression {
    override fun eval(context: MolangContext): Float = -inner.eval(context)
}

data class AddExpr(val left: MolangExpression, val right: MolangExpression) : MolangExpression {
    override fun eval(context: MolangContext): Float = left.eval(context) + right.eval(context)
}

data class SubExpr(val left: MolangExpression, val right: MolangExpression) : MolangExpression {
    override fun eval(context: MolangContext): Float = left.eval(context) - right.eval(context)
}

data class MulExpr(val left: MolangExpression, val right: MolangExpression) : MolangExpression {
    override fun eval(context: MolangContext): Float = left.eval(context) * right.eval(context)
}

data class DivExpr(val left: MolangExpression, val right: MolangExpression) : MolangExpression {
    override fun eval(context: MolangContext): Float = left.eval(context) / right.eval(context)
}

data class SinExpr(val inner: MolangExpression) : MolangExpression {
    override fun eval(context: MolangContext): Float = sin(inner.eval(context).toRadians())
}

data class CosExpr(val inner: MolangExpression) : MolangExpression {
    override fun eval(context: MolangContext): Float = cos(inner.eval(context).toRadians())
}

data class FloorExpr(val inner: MolangExpression) : MolangExpression {
    override fun eval(context: MolangContext): Float = floor(inner.eval(context))
}

data class CeilExpr(val inner: MolangExpression) : MolangExpression {
    override fun eval(context: MolangContext): Float = ceil(inner.eval(context))
}

data class RoundExpr(val inner: MolangExpression) : MolangExpression {
    override fun eval(context: MolangContext): Float = round(inner.eval(context))
}

data class TruncExpr(val inner: MolangExpression) : MolangExpression {
    override fun eval(context: MolangContext): Float = truncate(inner.eval(context))
}

data class AbsExpr(val inner: MolangExpression) : MolangExpression {
    override fun eval(context: MolangContext): Float = abs(inner.eval(context))
}

data class ClampExpr(val value: MolangExpression, val min: MolangExpression, val max: MolangExpression) : MolangExpression {
    override fun eval(context: MolangContext): Float =
        value.eval(context).coerceIn(min.eval(context), max.eval(context))
}

data class RandomExpr(val low: MolangExpression, val high: MolangExpression) : MolangExpression {
    override fun eval(context: MolangContext): Float {
        val low = low.eval(context)
        val high = high.eval(context)
        val random = (context.query as? MolangQueryRandom)?.random ?: Random
        return random.nextFloat() * (high - low) + low
    }
}

abstract class QueryExpr(val name: String, val f: MolangQuery.() -> Float) : MolangExpression {
    override fun eval(context: MolangContext): Float = context.query.run(f)

    object AnimTime : QueryExpr("anim_time", { (this as? MolangQueryAnimation)?.animTime ?: 0f })
    object LifeTime : QueryExpr("life_time", { (this as? MolangQueryEntity)?.lifeTime ?: 0f })
    object ModifiedMoveSpeed : QueryExpr("modified_move_speed", { (this as? MolangQueryEntity)?.modifiedMoveSpeed ?: 0f })
    object ModifiedDistanceMoved : QueryExpr("modified_distance_moved", { (this as? MolangQueryEntity)?.modifiedDistanceMoved ?: 0f })
}

data class ComparisonExpr(val left: MolangExpression, val right: MolangExpression, val op: Op) : MolangExpression {
    override fun eval(context: MolangContext): Float =
        if (op.check(left.eval(context), right.eval(context))) 1f else 0f

    enum class Op(val check: (a: Float, b: Float) -> Boolean) {
        Equal({ a, b -> a == b }),
        NotEqual({ a, b -> a != b }),
        LessThan({ a, b -> a < b }),
        LessThanOrEqual({ a, b -> a <= b }),
        GreaterThan({ a, b -> a > b }),
        GreaterThanOrEqual({ a, b -> a >= b }),
    }
}

data class LogicalOrExpr(val left: MolangExpression, val right: MolangExpression) : MolangExpression {
    override fun eval(context: MolangContext): Float =
        if (left.eval(context) != 0f || right.eval(context) != 0f) 1f else 0f
}

data class LogicalAndExpr(val left: MolangExpression, val right: MolangExpression) : MolangExpression {
    override fun eval(context: MolangContext): Float =
        if (left.eval(context) != 0f && right.eval(context) != 0f) 1f else 0f
}

data class TernaryExpr(
    val condition: MolangExpression,
    val trueCase: MolangExpression,
    val falseCase: MolangExpression,
) : MolangExpression {
    override fun eval(context: MolangContext): Float {
        return (if (condition.eval(context) != 0f) trueCase else falseCase).eval(context)
    }
}

data class VariableExpr(override val name: String) : MolangExpression, MolangVariable {
    override fun eval(context: MolangContext): Float = context.variables[name]
    override fun assign(context: MolangContext, value: Float) {
        context.variables[name] = value
    }
}

data class AssignmentExpr(val variable: MolangVariable, val inner: MolangExpression) : MolangExpression {
    override fun eval(context: MolangContext): Float {
        variable.assign(context, inner.eval(context))
        return 0f
    }
}

data class StatementsExpr(val statements: List<MolangExpression>) : MolangExpression {
    override fun eval(context: MolangContext): Float {
        for (statement in statements) {
            statement.eval(context)
        }
        return 0f
    }
}

data class ReturnExpr(val inner: MolangExpression) : MolangExpression {
    override fun eval(context: MolangContext): Float {
        throw Return(inner.eval(context))
    }

    internal class Return(val value: Float) : Throwable()
}

fun MolangExpression.serializeToString(): String {
    fun StringBuilder.appendExpr(expr: MolangExpression, inStatements: Boolean = false): StringBuilder {
        when (expr) {
            is LiteralExpr -> when (expr.value) {
                PI.toFloat() -> append("math.pi")
                else -> append(expr.value)
            }
            is NegExpr -> append('-').appendExpr(expr.inner)
            is AddExpr -> append('(').appendExpr(expr.left).append('+').appendExpr(expr.right).append(')')
            is SubExpr -> append('(').appendExpr(expr.left).append('-').appendExpr(expr.right).append(')')
            is MulExpr -> append('(').appendExpr(expr.left).append('*').appendExpr(expr.right).append(')')
            is DivExpr -> append('(').appendExpr(expr.left).append('/').appendExpr(expr.right).append(')')
            is SinExpr -> append("math.sin(").appendExpr(expr.inner).append(')')
            is CosExpr -> append("math.cos(").appendExpr(expr.inner).append(')')
            is FloorExpr -> append("math.floor(").appendExpr(expr.inner).append(')')
            is CeilExpr -> append("math.ceil(").appendExpr(expr.inner).append(')')
            is RoundExpr -> append("math.round(").appendExpr(expr.inner).append(')')
            is TruncExpr -> append("math.trunc(").appendExpr(expr.inner).append(')')
            is AbsExpr -> append("math.abs(").appendExpr(expr.inner).append(')')
            is ClampExpr -> append("math.clamp(").appendExpr(expr.value).append(',').appendExpr(expr.min).append(',').appendExpr(expr.max).append(')')
            is RandomExpr -> append("math.random(").appendExpr(expr.low).append(',').appendExpr(expr.high).append(')')
            is QueryExpr -> append("q.").append(expr.name)
            is ComparisonExpr -> append('(').appendExpr(expr.left).append(when (expr.op) {
                ComparisonExpr.Op.Equal -> "=="
                ComparisonExpr.Op.NotEqual -> "!="
                ComparisonExpr.Op.LessThan -> "<"
                ComparisonExpr.Op.LessThanOrEqual -> "<="
                ComparisonExpr.Op.GreaterThan -> ">"
                ComparisonExpr.Op.GreaterThanOrEqual -> ">="
            }).appendExpr(expr.right).append(')')
            is LogicalOrExpr -> append('(').appendExpr(expr.left).append("||").appendExpr(expr.right).append(')')
            is LogicalAndExpr -> append('(').appendExpr(expr.left).append("&&").appendExpr(expr.right).append(')')
            is TernaryExpr -> append('(').appendExpr(expr.condition).append('?').appendExpr(expr.trueCase).append(':').appendExpr(expr.falseCase).append(')')
            is VariableExpr -> append("variable.").append(expr.name)
            is AssignmentExpr -> {
                if (!inStatements) append('{')
                append("v.").append(expr.variable.name).append('=').appendExpr(expr.inner)
                if (!inStatements) append('}')
            }
            is StatementsExpr -> {
                append("{")
                for (statement in expr.statements) {
                    appendExpr(statement, inStatements = true).append(';')
                }
                append("}")
            }
            is ReturnExpr -> {
                if (!inStatements) append('{')
                append("return ").appendExpr(expr.inner)
                if (!inStatements) append('}')
            }
        }
        return this
    }

    return StringBuilder().appendExpr(this).toString()
}

private fun Float.toRadians() = this / 180 * PI.toFloat()

private class Parser(str: String) {
    val str = str.lowercase()
    var i = 0

    val curr get() = str[i]

    fun reads(char: Char): Boolean = reads { it == char }
    fun reads(char: CharRange): Boolean = reads { it in char }
    inline fun reads(f: (char: Char) -> Boolean): Boolean = when {
        i >= str.length -> false
        f(curr) -> {
            i++
            skipWhitespace()
            true
        }
        else -> false
    }

    fun reads(s: String): Boolean = if (str.startsWith(s, i)) {
        i += s.length
        skipWhitespace()
        true
    } else {
        false
    }

    @Suppress("ControlFlowWithEmptyBody")
    fun skipWhitespace() {
        while (reads { it.isWhitespace() });
    }

    @Suppress("ControlFlowWithEmptyBody")
    fun parseLiteral(): LiteralExpr {
        val start = i
        while (reads('0'..'9'));
        if (reads('.')) {
            while (reads('0'..'9'));
        }
        reads('f')
        return LiteralExpr(str.slice(start until i).replace(" ", "").toFloat())
    }

    fun parseIdentifier(): String {
        val start = i
        @Suppress("ControlFlowWithEmptyBody")
        while (reads { it.isLetterOrDigit() || it == '_' });
        return str.slice(start until i).dropLastWhile { it.isWhitespace() }
    }

    fun parseSimpleExpression(): MolangExpression = when {
        reads('(') -> parseExpression().also { reads(')') }
        reads('{') -> parseStatements().also { reads('}') }
        curr in '0'..'9' -> parseLiteral()
        curr == '-' -> {
            reads('-')
            NegExpr(parseSimpleExpression())
        }
        else -> {
            val qualifier = parseIdentifier()
            if (qualifier.isEmpty()) throw IllegalArgumentException("Unexpected identifier at index $i")
            if (!reads(".")) throw IllegalArgumentException("Expected `.` at index $i")

            val qualified = parseIdentifier()
            if (qualified.isEmpty()) throw IllegalArgumentException("Unexpected identifier at index $i")

            val args = mutableListOf<MolangExpression>()
            if (reads('(')) {
                do {
                    args.add(parseExpression())
                } while (reads(','))
                reads(')')
            }
            fun checkArgs(count: Int) {
                if (args.size != count) {
                    throw IllegalArgumentException("`$qualifier.$qualified` takes $count arguments but ${args.size} were given")
                }
            }

            when (qualifier) {
                "math" -> when (qualified) {
                    "pi" -> { checkArgs(0); LiteralExpr(PI.toFloat()) }
                    "cos" -> { checkArgs(1); CosExpr(args[0]) }
                    "sin" -> { checkArgs(1); SinExpr(args[0]) }
                    "floor" -> { checkArgs(1); FloorExpr(args[0]) }
                    "ceil" -> { checkArgs(1); CeilExpr(args[0]) }
                    "round" -> { checkArgs(1); RoundExpr(args[0]) }
                    "trunc" -> { checkArgs(1); TruncExpr(args[0]) }
                    "abs" -> { checkArgs(1); AbsExpr(args[0]) }
                    "clamp" -> { checkArgs(3); ClampExpr(args[0], args[1], args[2]) }
                    "random" -> { checkArgs(2); RandomExpr(args[0], args[1]) }
                    else -> throw IllegalArgumentException("Unknown math function `$qualified`")
                }
                "query", "q" -> {
                    checkArgs(0)
                    when (qualified) {
                        "anim_time" -> QueryExpr.AnimTime
                        "life_time" -> QueryExpr.LifeTime
                        "modified_move_speed" -> QueryExpr.ModifiedMoveSpeed
                        "modified_distance_moved" -> QueryExpr.ModifiedDistanceMoved
                        else -> throw IllegalArgumentException("Unknown query `$qualified`")
                    }
                }
                "variable", "v" -> {
                    checkArgs(0)
                    VariableExpr(qualified)
                }
                else -> throw IllegalArgumentException("Unknown qualifier `$qualifier`")
            }
        }
    }

    fun parseProduct(): MolangExpression {
        var left = parseSimpleExpression()
        while (true) {
            left = when {
                reads('*') -> MulExpr(left, parseSimpleExpression())
                reads('/') -> DivExpr(left, parseSimpleExpression())
                else -> return left
            }
        }
    }

    fun parseSum(): MolangExpression {
        var left = parseProduct()
        while (true) {
            left = when {
                reads('+') -> AddExpr(left, parseProduct())
                reads('-') -> SubExpr(left, parseProduct())
                else -> return left
            }
        }
    }

    fun parseComparisons(): MolangExpression {
        val left = parseSum()
        return when {
            reads("<=") -> ComparisonExpr(left, parseSum(), ComparisonExpr.Op.LessThanOrEqual)
            reads(">=") -> ComparisonExpr(left, parseSum(), ComparisonExpr.Op.GreaterThanOrEqual)
            reads('<') -> ComparisonExpr(left, parseSum(), ComparisonExpr.Op.LessThan)
            reads('>') -> ComparisonExpr(left, parseSum(), ComparisonExpr.Op.GreaterThan)
            else -> left
        }
    }

    fun parseEqualityChecks(): MolangExpression {
        val left = parseComparisons()
        return when {
            reads("==") -> ComparisonExpr(left, parseComparisons(), ComparisonExpr.Op.Equal)
            reads("!=") -> ComparisonExpr(left, parseComparisons(), ComparisonExpr.Op.NotEqual)
            else -> left
        }
    }

    fun parseLogicalAnds(): MolangExpression {
        var left = parseEqualityChecks()
        while (true) {
            left = when {
                reads("&&") -> LogicalAndExpr(left, parseEqualityChecks())
                else -> return left
            }
        }
    }

    fun parseLogicalOrs(): MolangExpression {
        var left = parseLogicalAnds()
        while (true) {
            left = when {
                reads("||") -> LogicalOrExpr(left, parseLogicalAnds())
                else -> return left
            }
        }
    }

    fun parseTernary(): MolangExpression {
        val condition = parseLogicalOrs()
        return if (reads('?')) {
            val trueCase = parseTernary()
            reads(':')
            val falseCase = parseTernary()
            TernaryExpr(condition, trueCase, falseCase)
        } else {
            condition
        }
    }

    fun parseNullCoalescing(): MolangExpression {
        return parseTernary() // TODO implement
    }

    fun parseExpression(): MolangExpression {
        return parseNullCoalescing()
    }

    fun parseAssignment(): MolangExpression {
        val left = parseExpression()
        if (!reads('=')) {
            return left
        }
        if (left !is MolangVariable) {
            throw IllegalArgumentException("Cannot assign value to $left")
        }
        val right = parseExpression()
        return AssignmentExpr(left, right)
    }

    fun parseStatement(): MolangExpression {
        return when {
            reads("return") -> ReturnExpr(parseExpression())
            else -> parseAssignment()
        }
    }

    fun parseStatements(): MolangExpression {
        val first = parseStatement()
        if (!reads(';')) {
            return first
        }
        val statements = mutableListOf(first)
        while (i < str.length && curr != '}') {
            statements.add(parseStatement())
            reads(';')
        }
        return StatementsExpr(statements)
    }

    fun parseMolang(): MolangExpression {
        skipWhitespace()
        return parseStatements()
    }

    fun fullyParseMolang(): MolangExpression {
        return parseMolang().also {
            if (i < str.length) {
                throw IllegalArgumentException("Failed to fully parse input, remaining: ${str.substring(i)}")
            }
        }
    }

    fun tryFullyParseMolang(): MolangExpression = try {
        fullyParseMolang()
    } catch (e: Exception) {
        throw MolangParserException("Failed to parse `$str`:", e)
    }
}

class MolangParserException(message: String, cause: Throwable?) : Exception(message, cause)

fun String.parseMolangExpression(): MolangExpression = Parser(this).tryFullyParseMolang()

object MolangSerializer : KSerializer<Molang> {
    override val descriptor: SerialDescriptor = JsonElement.serializer().descriptor

    override fun deserialize(decoder: Decoder): Molang {
        override.get()?.let { return it.deserialize(decoder) }
        val json = (decoder as JsonDecoder).decodeJsonElement() as JsonPrimitive
        return when {
            json.isString -> Molang(json.content.parseMolangExpression())
            else -> Molang.literal(json.content.toFloat())
        }
    }

    override fun serialize(encoder: Encoder, value: Molang) {
        override.get()?.let { return it.serialize(encoder, value) }
        return (encoder as JsonEncoder).encodeJsonElement(when (val expression = value.expression) {
            is LiteralExpr -> JsonPrimitive(expression.value)
            else -> JsonPrimitive(expression.serializeToString())
        })
    }

    @PublishedApi
    internal val override = ThreadLocal<KSerializer<Molang>>()
}
