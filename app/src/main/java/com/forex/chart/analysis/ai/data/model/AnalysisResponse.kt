package com.forex.chart.analysis.ai.data.model

import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

@IgnoreExtraProperties
data class AnalysisResponse(
    var id: String = "",
    var imageUrl: String = "",
    var status: String = "",
    var fileName: String = "",
    var storagePath: String = "",
    @ServerTimestamp var createdAt: Date? = null,
    @get:PropertyName("analysis_data") @set:PropertyName("analysis_data")
    var data: AnalysisData? = null
)

@IgnoreExtraProperties
data class AnalysisData(
    var prediction: Prediction? = null,
    @get:PropertyName("key_insights") @set:PropertyName("key_insights") var keyInsights: KeyInsights? = null,
    @get:PropertyName("game_plan") @set:PropertyName("game_plan") var gamePlan: String? = null,
    @get:PropertyName("multi_timeframe_summary") @set:PropertyName("multi_timeframe_summary") var multiTimeframe: MultiTimeframe? = null,
    @get:PropertyName("institutional_analysis") @set:PropertyName("institutional_analysis") var institutional: InstitutionalAnalysis? = null,
    @get:PropertyName("sentiment_metrics") @set:PropertyName("sentiment_metrics") var sentiment: SentimentMetrics? = null,
    @get:PropertyName("breakout_readings") @set:PropertyName("breakout_readings") var breakout: BreakoutReadings? = null,
    @get:PropertyName("risk_management") @set:PropertyName("risk_management") var riskManagement: RiskManagement? = null,
    @get:PropertyName("detailed_analysis") @set:PropertyName("detailed_analysis") var detailedAnalysis: List<DetailItem> = emptyList()
)

@IgnoreExtraProperties
data class Prediction(
    var score: Any? = 0,
    var signal: String = "",
    @get:PropertyName("color_code") @set:PropertyName("color_code") var colorCode: String = "",
    @get:PropertyName("confidence_level") @set:PropertyName("confidence_level") var confidenceLevel: String = "",
    @get:PropertyName("risk_level") @set:PropertyName("risk_level") var riskLevel: String = ""
) {
    // Score
    fun getScoreInt(): Int {
        return when (score) {
            is Int -> score as Int
            is Long -> (score as Long).toInt() // Firestore Long
            is Double -> (score as Double).toInt()
            is String -> {

                (score as String).filter { it.isDigit() }.toIntOrNull() ?: 0
            }
            else -> 0
        }
    }
}



@IgnoreExtraProperties
data class KeyInsights(
    var trend: String = "",
    var volatility: String = "",
    @get:PropertyName("support_resistance") @set:PropertyName("support_resistance") var supportResistance: String = "",
    @get:PropertyName("market_sentiment") @set:PropertyName("market_sentiment") var marketSentiment: String = "",
    @get:PropertyName("momentum_strength") @set:PropertyName("momentum_strength") var momentumStrength: String = "",
    @get:PropertyName("liquidity_bias") @set:PropertyName("liquidity_bias") var liquidityBias: String = ""
)

@IgnoreExtraProperties
data class MultiTimeframe(
    @get:PropertyName("weekly_trend") @set:PropertyName("weekly_trend") var weekly: String = "",
    @get:PropertyName("daily_trend") @set:PropertyName("daily_trend") var daily: String = "",
    @get:PropertyName("4h_structure") @set:PropertyName("4h_structure") var h4: String = "",
    @get:PropertyName("1h_structure") @set:PropertyName("1h_structure") var h1: String = ""
)

@IgnoreExtraProperties
data class InstitutionalAnalysis(
    @get:PropertyName("order_blocks") @set:PropertyName("order_blocks") var orderBlocks: OrderBlocks? = null,
    @get:PropertyName("fair_value_gap") @set:PropertyName("fair_value_gap") var fvg: String = "",
    @get:PropertyName("liquidity_levels") @set:PropertyName("liquidity_levels") var liquidity: LiquidityLevels? = null
)

@IgnoreExtraProperties
data class OrderBlocks(
    @get:PropertyName("bullish_ob") @set:PropertyName("bullish_ob") var bullish: String = "",
    @get:PropertyName("bearish_ob") @set:PropertyName("bearish_ob") var bearish: String = ""
)

@IgnoreExtraProperties
data class LiquidityLevels(
    @get:PropertyName("buy_side_liquidity") @set:PropertyName("buy_side_liquidity") var buySide: String = "",
    @get:PropertyName("sell_side_liquidity") @set:PropertyName("sell_side_liquidity") var sellSide: String = ""
)

@IgnoreExtraProperties
data class SentimentMetrics(
    @get:PropertyName("volume_sentiment") @set:PropertyName("volume_sentiment") var volume: String = "",
    @get:PropertyName("fear_greed_score") @set:PropertyName("fear_greed_score") var fearGreed: String = "",
    @get:PropertyName("market_phase") @set:PropertyName("market_phase") var phase: String = "",
    @get:PropertyName("smart_money_flow") @set:PropertyName("smart_money_flow") var smartMoney: String = ""
)

@IgnoreExtraProperties
data class BreakoutReadings(
    @get:PropertyName("breakout_strength") @set:PropertyName("breakout_strength") var strength: String = "",
    @get:PropertyName("volume_confirmation") @set:PropertyName("volume_confirmation") var volumeConf: String = "",
    @get:PropertyName("fakeout_risk") @set:PropertyName("fakeout_risk") var fakeoutRisk: String = "",
    @get:PropertyName("retest_quality") @set:PropertyName("retest_quality") var retest: String = ""
)

@IgnoreExtraProperties
data class RiskManagement(
    @get:PropertyName("recommended_rr_ratio") @set:PropertyName("recommended_rr_ratio") var rrRatio: String = "",
    @get:PropertyName("stop_loss_zone") @set:PropertyName("stop_loss_zone") var slZone: String = "",
    @get:PropertyName("position_sizing") @set:PropertyName("position_sizing") var sizing: String = ""
)

@IgnoreExtraProperties
data class DetailItem(
    var title: String = "",
    var description: String = ""
)