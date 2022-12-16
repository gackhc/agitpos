package ggack.agitpos.data.model

import ggack.agitpos.data.model.networkmodel.ItemModel
import java.time.LocalDate

data class StatisticsModel(var date : LocalDate, var deposit : Int, var payment : Int)
data class StatisticsMonthModel(var date : LocalDate, var deposit : Int, var payment : Int)
data class StatisticsItemModel(var date : LocalDate, var items : MutableList<ItemModel>, var payment : Int)