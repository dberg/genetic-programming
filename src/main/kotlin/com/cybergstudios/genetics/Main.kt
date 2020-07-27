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

fun main(args: Array<String>) {
    val expr = exampleExpr()
    val ctx1: Context = mapOf("x" to 2.0, "y" to 3.0)
    val ctx2: Context = mapOf("x" to 5.0, "y" to 3.0)

    println("expr:\n$expr")
    println("--------------------------------------------------------------------------------")
    // (if ((if ( Context[x]  >  3.0 ) 1.0 else 0.0) > 0.0) { ( Context[y]  +  5.0 ) } else { ( Context[y]  -  2.0 ) })

    println(expr.evaluate(ctx1)) // 1.0
    println(expr.evaluate(ctx2)) // 8.0
    println("--------------------------------------------------------------------------------")

    val variables = arrayOf("x", "y")
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

    println("--------------------------------------------------------------------------------")
    val dataset = buildHiddenSet()
    val rf = getRankFunction(dataset)
    val evolvedExpr = evolve(variables, 1000, rf, 10000, 0.2, 0.1, 0.7, 0.1)
    println("evolved expr:\n$evolvedExpr")

    // sample run
    // score:
    // 9.155704410446219E-5
    // evolved expr:
    // (( 0.8304631131471493  +  0.8333408868711532 ) + ((((((( 0.5451961050671689  + (if ( Context[y]  >  Context[y] ) 1.0 else 0.0)) + (( Context[x]  * (if ( 0.9417800340873351  > 0.0) {  Context[x]  } else { (if ( Context[y]  > 0.0) { (if (( Context[x]  +  Context[x] ) > 0.0) {  0.7067047558997694  } else { (if ((if ((if ((if ( Context[y]  > 0.0) {  Context[y]  } else { (( Context[y]  -  0.8095091346235621 ) +  0.905213171309831 ) }) >  Context[y] ) 1.0 else 0.0) >  0.20350888649813792 ) 1.0 else 0.0) > 0.0) { (( Context[y]  *  0.7689390992611634 ) * (if ( 0.9256184582840955  > 0.0) { ((if ( Context[y]  > 0.0) {  0.24788624930942094  } else {  0.41487596587099984  }) * (if ( 0.6369077188452328  > 0.0) {  Context[x]  } else {  Context[x]  })) } else {  Context[y]  })) } else {  Context[x]  }) }) } else {  0.13579980426446758  }) })) + ((( Context[y]  +  Context[x] ) -  Context[y] ) + ( 0.2700341708631939  *  2.890804994429841E-4 )))) + ( Context[y]  + (if ( Context[y]  > 0.0) {  Context[y]  } else {  0.4181422619078603  }))) + ( 0.9575814042156403  +  Context[x] )) +  Context[x] ) + (if ( Context[x]  > (( 0.8928274715875624  -  0.8578502752830313 ) -  0.550369515794871 )) 1.0 else 0.0)) +  0.8333408868711532 ))
}
