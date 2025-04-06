package androidtown.org.moveon.marathon

data class DirectionResponse(
    val route: Result_trackoption
)
data class Result_trackoption(
    val traoptimal: List<Result_path>
)
data class Result_path(
    val summary: Result_distance,
    val path: List<List<Double>>
)
data class Result_distance(
    val distance: Int
)
