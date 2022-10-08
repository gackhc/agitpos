package ggack.agitpos.data.model

import java.time.LocalDate

data class StatisticsModel(var deposit : Int, var payment : Int)
data class StatisticsItemModel(var item : String, var payment : Int)