package com.cybergstudios.genetics

import java.lang.RuntimeException
import kotlin.math.abs
import kotlin.math.ln
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

/**
 * pc       is the number of parameters that the generated function will take.
 * maxDepth defines how tall the generated expression can be
 * fpr      probability of a function node
 * ppr      probability of a param node if not a function node
 */
fun randomExpr(pc: Array<String>, maxDepth: Int = 4, fpr: Double = 0.5, ppr: Double = 0.6): Expr {
    assert(fpr in 0.0..1.0) { "fpr should be a value between 0.0 and 1.0"}
    assert(ppr in 0.0..1.0) { "ppr should be a value between 0.0 and 1.0"}
    assert(maxDepth >= 0) { "maxDepth should be a positive number" }

    return if (maxDepth > 0.0 && random() < fpr) {
        when (Expr.nonTerminal.random()) {
            Add::class -> Add(
                randomExpr(pc, maxDepth - 1, fpr, ppr),
                randomExpr(pc, maxDepth - 1, fpr, ppr))
            Sub::class -> Sub(
                randomExpr(pc, maxDepth - 1, fpr, ppr),
                randomExpr(pc, maxDepth - 1, fpr, ppr))
            Mul::class -> Mul(
                randomExpr(pc, maxDepth - 1, fpr, ppr),
                randomExpr(pc, maxDepth - 1, fpr, ppr))
            //Div::class -> Div(
            //    randomExpr(pc, maxDepth - 1, fpr, ppr),
            //    randomExpr(pc, maxDepth - 1, fpr, ppr))
            If::class -> If(
                randomExpr(pc, maxDepth - 1, fpr, ppr),
                randomExpr(pc, maxDepth - 1, fpr, ppr),
                randomExpr(pc, maxDepth - 1, fpr, ppr))
            Gt::class -> Gt(
                randomExpr(pc, maxDepth - 1, fpr, ppr),
                randomExpr(pc, maxDepth - 1, fpr, ppr))
            else -> throw RuntimeException("Unexpected nonTerminal class")
        }
    } else if (random() < ppr) {
        Param(pc.random())
    } else {
        Const(Random.nextDouble())
    }
}

fun mutate(e: Expr, pc: Array<String>, probchange: Double = 0.1): Expr {
    assert(probchange in 0.0..1.0) { "probchange should be a value between 0.0 and 1.0" }
    return if (random() < probchange) {
        randomExpr(pc)
    } else {
        when (e) {
            is Const, is Param  -> e
            is Add -> Add(mutate(e.e1, pc), mutate(e.e2, pc))
            is Sub -> Sub(mutate(e.e1, pc), mutate(e.e2, pc))
            is Mul -> Mul(mutate(e.e1, pc), mutate(e.e2, pc))
            //is Div -> Div(mutate(e.e1, pc), mutate(e.e2, pc))
            is If -> If(mutate(e.cond, pc), mutate(e.exprTrue, pc), mutate(e.exprFalse, pc))
            is Gt -> Gt(mutate(e.e1, pc), mutate(e.e2, pc))
        }
    }
}

fun crossover(e1: Expr, e2: Expr, probswap: Double = 0.7, top: Boolean = true): Expr {
    return if (!top && random() < probswap) {
        e2
    } else if (e1.isTerminal() || e2.isTerminal()) {
        e1
    } else {
        when (e1) {
            is Add -> Add(crossover(e1.e1, e2.randomChild(), probswap, false), crossover(e1.e2, e2.randomChild(), probswap, false))
            is Sub -> Sub(crossover(e1.e1, e2.randomChild(), probswap, false), crossover(e1.e2, e2.randomChild(), probswap, false))
            is Mul -> Mul(crossover(e1.e1, e2.randomChild(), probswap, false), crossover(e1.e2, e2.randomChild(), probswap, false))
            //is Div -> Div(crossover(e1.e1, e2.randomChild(), probswap, false), crossover(e1.e2, e2.randomChild(), probswap, false))
            is If -> If(
                crossover(e1.cond, e2.randomChild(), probswap, false),
                crossover(e1.exprTrue, e2.randomChild(), probswap, false),
                crossover(e1.exprFalse, e2.randomChild(), probswap, false))
            is Gt -> Gt(crossover(e1.e1, e2.randomChild(), probswap, false), crossover(e1.e2, e2.randomChild(), probswap, false))
            else -> throw RuntimeException("Unexpected crossover with terminal expression")
        }
    }
}

typealias Population = Array<Expr>
typealias Score = Double
data class Probe(val ctx: Context, val expected: Score)
typealias Dataset = Array<Probe>
typealias ScoredPopulation = Array<Pair<Score, Expr>>

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

fun evolve(
    pc: Array<String>,
    popsize: Int,
    rank: (Population) -> ScoredPopulation,
    maxgen: Int = 500,
    mutationRate: Double = 0.1,
    breedingRate: Double = 0.4,
    pexp: Double = 0.7,
    pnew: Double = 0.05
): Expr {
    fun selectIndex(): Int = (ln(random()) / ln(pexp)).toInt()
    var population: Population = Array(popsize) { randomExpr(pc) }
    var winner: Expr? = null
    for (i in 0 .. maxgen) {
        val scored = rank(population)
        winner = scored[0].second
        val winnerScore = scored[0].first
        println(winnerScore)
        if (abs(winnerScore - 0.0) < 0.000001) break;

        // build the next generation
        val newpop: Population = Array(population.size) {
            // the two best always make it
            if (it == 0 || it == 1) population.get(it)
            else {
                if (random() > pnew) {
                    val expr = crossover(scored.get(selectIndex()).second, scored.get(selectIndex()).second, breedingRate)
                    mutate(expr, pc, mutationRate)
                } else {
                    // add a random node to mix thing sup
                    randomExpr(pc)
                }
            }
        }

        population = newpop
    }

    return winner ?: throw RuntimeException("Failed to find the best expression")
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
    val evolvedExpr = evolve(variables, 500, rf, 500, 0.2, 0.1, 0.7, 0.1)
    println("evolved expr:\n$evolvedExpr")

    //val evolvedExpr = evolve(variables, 5000, rf, 5000, 0.2, 0.1, 0.7, 0.2)
    //0.007749497176098075
    //evolved expr:
    //(((( Context[x]  + ( Context[x]  + (( Context[y]  +  Context[y] ) + ( Context[x]  + (if ( 0.3982698023252428  > 0.0) {  Context[x]  } else {  Context[y]  }))))) + ((if ( Context[x]  > ( 0.7533623462933614  * (( Context[y]  -  Context[x] ) -  Context[y] ))) 1.0 else 0.0) +  3.1314018632511154E-4 )) + (((( Context[y]  + ((( 0.9993627460496781  + (if ( 0.42073487141258725  > (if ((if ( 0.8270598172038693  >  0.849312995975535 ) 1.0 else 0.0) >  Context[x] ) 1.0 else 0.0)) 1.0 else 0.0)) +  0.8058432992914634 ) - ( Context[x]  +  Context[y] ))) +  0.16247309044011993 ) +  0.30125583749684914 ) +  0.7308423254198689 )) - ((( 0.0020075377330577293  *  0.0017882992836799616 ) -  Context[x] ) *  Context[x] ))
}