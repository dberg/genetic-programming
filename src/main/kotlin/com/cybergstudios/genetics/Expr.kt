package com.cybergstudios.genetics

import kotlin.random.Random
import kotlin.reflect.KClass

typealias Context = Map<String, Double>

sealed class Expr {
    abstract fun evaluate(ctx: Context): Double
    abstract fun isTerminal(): Boolean
    abstract fun randomChild(): Expr
    override fun toString(): String = "Expr"

    companion object {
        val nonTerminal: Array<KClass<out Expr>> = arrayOf(
            Add::class,
            Sub::class,
            Mul::class,
            If::class,
            Gt::class
        )
    }
}

data class Const(val v: Double) : Expr() {
    override fun evaluate(ctx: Context): Double = v
    override fun isTerminal(): Boolean = true
    override fun randomChild(): Expr = this
    override fun toString(): String = " $v "
}

data class Param(val name: String) : Expr() {
    override fun evaluate(ctx: Context): Double =
        ctx.getOrElse(name, { throw RuntimeException("Mal-formed Context") })
    override fun isTerminal(): Boolean = true
    override fun randomChild(): Expr = this
    override fun toString(): String = " Context[$name] "
}

data class Add(val e1: Expr, val e2: Expr) : Expr() {
    override fun evaluate(ctx: Context): Double = e1.evaluate(ctx) + e2.evaluate(ctx)
    override fun isTerminal(): Boolean = false
    override fun randomChild(): Expr = if (Random.nextBoolean()) e1 else e2
    override fun toString(): String = "($e1 + $e2)"
}

data class Sub(val e1: Expr, val e2: Expr) : Expr() {
    override fun evaluate(ctx: Context): Double = e1.evaluate(ctx) - e2.evaluate(ctx)
    override fun isTerminal(): Boolean = false
    override fun randomChild(): Expr = if (Random.nextBoolean()) e1 else e2
    override fun toString(): String = "($e1 - $e2)"
}

data class Mul(val e1: Expr, val e2: Expr) : Expr() {
    override fun evaluate(ctx: Context): Double = e1.evaluate(ctx) * e2.evaluate(ctx)
    override fun isTerminal(): Boolean = false
    override fun randomChild(): Expr = if (Random.nextBoolean()) e1 else e2
    override fun toString(): String = "($e1 * $e2)"
}

data class If(val cond: Expr, val exprTrue: Expr, val exprFalse: Expr) : Expr() {
    override fun evaluate(ctx: Context): Double =
        if (cond.evaluate(ctx) > 0.0) exprTrue.evaluate(ctx) else exprFalse.evaluate(ctx)
    override fun isTerminal(): Boolean = false
    override fun randomChild(): Expr = when (Random.nextInt(0, 3))  {
        0 -> cond
        1 -> exprTrue
        else -> exprFalse
    }
    override fun toString(): String = "(if ($cond > 0.0) { $exprTrue } else { $exprFalse })"
}

data class Gt(val e1: Expr, val e2: Expr) : Expr() {
    override fun evaluate(ctx: Context): Double =
        if (e1.evaluate(ctx) > e2.evaluate(ctx)) 1.0 else 0.0
    override fun isTerminal(): Boolean = false
    override fun randomChild(): Expr = if (Random.nextBoolean()) e1 else e2
    override fun toString(): String = "(if ($e1 > $e2) 1.0 else 0.0)"
}
