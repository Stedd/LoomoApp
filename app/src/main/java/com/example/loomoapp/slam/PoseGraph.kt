package com.example.loomoapp.slam

class PoseGraph {
}

data class Node(
    val x: Double,
    val y: Double,
    val theta: Double
) {
    val index: Int
    companion object {
        var instances = 0
            private set
    }
    init {
        index = instances
        instances++
    }
}

data class Edge(
    val connection: Pair<Node, Node>,
    val constraint: Constraint,
    val informationMatrix: InformationMatrix
)

data class Constraint(
    val relX: Double,
    val relY: Double,
    val relTheta: Double
)

data class InformationMatrix(
    val omega: Array<Double>
)