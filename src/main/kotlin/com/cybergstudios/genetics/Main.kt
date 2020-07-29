package com.cybergstudios.genetics

import kotlin.math.abs
import kotlin.math.pow
import kotlin.random.Random

fun random(): Double = Random.nextDouble(0.0, 1.0)

/** Build a tree that expects two arguments named: x, and y. */
fun exampleExpr(): Expr =
    If(
        Gt(Param("x"), Const(3.0)),
        Add(Param("y"), Const(5.0)),
        Sub(Param("y"), Const(2.0))
    )

fun hiddenFunction(x: Double, y: Double): Double =
    x.pow(2) + 2 * y + 3 * x + 5

fun buildHiddenSet(): Dataset =
    Array(200) {
        val x = Random.nextDouble(0.0, 40.0)
        val y = Random.nextDouble(0.0, 40.0)
        Probe(mapOf("x" to x, "y" to y), hiddenFunction(x, y))
    }

fun scoreFunction(expr: Expr, dataset: Dataset): Score {
    var dif = 0.0
    dataset.forEach {
        val v = expr.evaluate(it.ctx)
        dif += abs(v - it.expected)
    }
    return dif
}

fun getRankFunction(dataset: Dataset): (Population) -> ScoredPopulation =
    { population ->
        population
            .map { Pair(scoreFunction(it, dataset), it) }
            .sortedBy { it.first }
            .toTypedArray()
    }

fun sampleResult(): Expr = Add(Add(Add(Mul(Param("x"), Param("x")), Add(Param("x"), Param("y"))), Add(Add(Param("x"), Add(Gt(Mul(Param("x"), Param("y")),If(Mul(If(Gt(Param("y"),Param("y")), Gt(Const(0.19461961582865006),Const(0.4055039316155936)), Param("y")), Sub(Gt(Const(0.875451443202543),Param("x")), Param("x"))), Gt(Mul(Const(0.12025225441289078), Const(0.11970683002894345)),Param("y")), If(Param("x"), Const(0.5347030795768023), Const(0.15515739996369393)))), Param("y"))), Param("x"))), Add(Add(Gt(Const(0.4858039982862735),Sub(Const(0.15187656831984542), Param("x"))), Gt(Const(0.8825479234040398),Const(0.7533698766486537))), Add(Gt(Add(Const(0.9526475953120076), Const(0.42252085669747863)),Const(0.8825479234040398)), Gt(Param("y"),Sub(Gt(Param("x"),Param("y")), Const(0.7914566521846355))))))

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println("Options: examples | learn | compare")
        return
    }

    val variables = arrayOf("x", "y")

    // Run a few examples
    if (args[0] == "examples") {
        val expr = exampleExpr()
        val ctx1: Context = mapOf("x" to 2.0, "y" to 3.0)
        val ctx2: Context = mapOf("x" to 5.0, "y" to 3.0)

        println("expr:\n$expr")
        println("--------------------------------------------------------------------------------")

        println(expr.evaluate(ctx1)) // 1.0
        println(expr.evaluate(ctx2)) // 8.0
        println("--------------------------------------------------------------------------------")

        val rndExpr = randomExpr(variables)
        println(rndExpr)
        println(rndExpr.evaluate(ctx1))
        println("--------------------------------------------------------------------------------")

        println("expr mutation:\n" + mutate(expr, variables))

        println("--------------------------------------------------------------------------------")
        val expr2 = exampleExpr()
        println("expr2:\n$expr2")
        val crossover = crossover(expr, expr2)
        println("crossover:\n$crossover")
    }

    // There goes some manual labor to move the expression from the bottom of the output
    // if you want to plot it
    if (args[0] == "learning") {
        val dataset = buildHiddenSet()
        val rf = getRankFunction(dataset)
        val evolvedExpr = evolve(variables, 1000, rf, 10000, 0.1, 0.4, 0.7, 0.05)
        println("evolved expr:\n$evolvedExpr")
        return
    }

    // Compare sample result against our hidden function
    if (args[0] == "compare") {
        val expr = sampleResult()
        for (x in -100 .. 100) {
            for (y in -100 .. 100) {
                val zHidden = hiddenFunction(x.toDouble(), y.toDouble())
                val zLearned = expr.evaluate(mapOf("x" to x.toDouble(), "y" to y.toDouble()))
                println("$x\t$y\t$zHidden\t$zLearned")
            }
        }
        return
    }

    println("I don't speak your language!")

}
