package com.cybergstudios.genetics

import java.lang.RuntimeException
import kotlin.math.abs
import kotlin.math.ln
import kotlin.random.Random

typealias Population = Array<Expr>
typealias Score = Double
data class Probe(val ctx: Context, val expected: Score)
typealias Dataset = Array<Probe>
typealias ScoredPopulation = Array<Pair<Score, Expr>>

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
        println("$winnerScore\t$i")
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
            is If -> If(
                    crossover(e1.cond, e2.randomChild(), probswap, false),
                    crossover(e1.exprTrue, e2.randomChild(), probswap, false),
                    crossover(e1.exprFalse, e2.randomChild(), probswap, false))
            is Gt -> Gt(crossover(e1.e1, e2.randomChild(), probswap, false), crossover(e1.e2, e2.randomChild(), probswap, false))
            else -> throw RuntimeException("Unexpected crossover with terminal expression")
        }
    }
}
