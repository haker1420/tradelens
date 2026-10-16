package com.forex.chart.analysis.ai.ui.screens.premium_analysis

import androidx.annotation.Keep
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName

@Keep
enum class TradeSignal {
    BUY, SELL, HOLD
}

@Keep
@IgnoreExtraProperties
data class ReportSection(
    @get:PropertyName("title") @set:PropertyName("title")
    var title: String = "",

    @get:PropertyName("description") @set:PropertyName("description")
    var description: String = "",

    @get:PropertyName("titleColor") @set:PropertyName("titleColor")
    var titleColor: Long = 0xFFFFFFFF
)

@Keep
@IgnoreExtraProperties
data class MetricRow(
    @get:PropertyName("metricName") @set:PropertyName("metricName")
    var metricName: String = "",

    @get:PropertyName("resultValue") @set:PropertyName("resultValue")
    var resultValue: String = "",

    @get:PropertyName("isHighlight") @set:PropertyName("isHighlight")
    var isHighlight: Boolean = false
)

@Keep
@IgnoreExtraProperties
data class AnalysisResultModel(
    @get:PropertyName("id") @set:PropertyName("id")
    var id: String = "",

    @get:PropertyName("status") @set:PropertyName("status")
    var status: String = "",

    @get:PropertyName("analyzedImageUrl") @set:PropertyName("analyzedImageUrl")
    var analyzedImageUrl: String = "",

    @get:PropertyName("tradeSignal") @set:PropertyName("tradeSignal")
    var tradeSignal: String = "HOLD",

    @get:PropertyName("confidenceScore") @set:PropertyName("confidenceScore")
    var confidenceScore: Int = 0,

    @get:PropertyName("riskRewardRatio") @set:PropertyName("riskRewardRatio")
    var riskRewardRatio: String = "",

    @get:PropertyName("timeframe") @set:PropertyName("timeframe")
    var timeframe: String = "",

    @get:PropertyName("riskProfile") @set:PropertyName("riskProfile")
    var riskProfile: String = "",

    @get:PropertyName("detailedMetricsTable") @set:PropertyName("detailedMetricsTable")
    var detailedMetricsTable: List<MetricRow> = emptyList(),

    @get:PropertyName("aiAnalysisReport") @set:PropertyName("aiAnalysisReport")
    var aiAnalysisReport: List<ReportSection> = emptyList()
) {
    // Safely parses numbers or strings from DB without crashing
    @get:PropertyName("entryPrice") @set:PropertyName("entryPrice")
    var _entryPrice: Any = ""

    @get:PropertyName("stopLoss") @set:PropertyName("stopLoss")
    var _stopLoss: Any = ""

    @get:PropertyName("takeProfit") @set:PropertyName("takeProfit")
    var _takeProfit: Any = ""

    // ✅ FIX: Added @get:Exclude so Firestore does not serialize this property
    @get:Exclude
    val entryPrice: String get() = _entryPrice.toString()

    @get:Exclude
    val stopLoss: String get() = _stopLoss.toString()

    @get:Exclude
    val takeProfit: String get() = _takeProfit.toString()

    companion object {
        fun getDummyData(): AnalysisResultModel {
            return AnalysisResultModel(
                id = "pro_scan_001",
                status = "completed",
                analyzedImageUrl = "https://img.freepik.com/free-vector/dynamic-candlestick-chart-vector-format_1308-180804.jpg?semt=ais_hybrid&w=740&q=80",
                tradeSignal = "BUY",
                confidenceScore = 92,
                riskRewardRatio = "1:3.5",
                timeframe = "4H (Swing Trade)",
                riskProfile = "Low Risk",
                detailedMetricsTable = listOf(
                    MetricRow("Trend Analysis", "Strong Bullish Uptrend", true),
                    MetricRow("Candlestick Pattern", "Bullish Engulfing (Confirmed)"),
                    MetricRow("Support Level", "$63,800 (Major Liquidity Pool)"),
                    MetricRow("Resistance Level", "$67,200 (Previous Swing High)"),
                    MetricRow("Volume Analysis", "High Buy Volume (Institutional Accumulation)"),
                    MetricRow("Momentum (RSI/MACD)", "RSI 64.5 (Room to grow), MACD Bull Cross"),
                    MetricRow("Institutional SMC", "Mitigated Fair Value Gap (FVG) at $64,000", true),
                    MetricRow("Multi-Timeframe", "1H: Bullish | 4H: Bullish | 1D: Neutral"),
                    MetricRow("Risk Management", "Max 2% Account Risk suggested", true)
                ),
                aiAnalysisReport = listOf(
                    ReportSection(
                        title = "Market Context:",
                        description = "A clear bullish structure has formed on the current chart, and the price has successfully broken out to the upside with strong momentum.",
                        titleColor = 0xFF00E676
                    ),
                    ReportSection(
                        title = "Smart Money Concept (SMC):",
                        description = "The recent pullback perfectly filled the Fair Value Gap (FVG). Institutional liquidity has been swept, creating a strong foundation.",
                        titleColor = 0xFF9C27B0
                    ),
                    ReportSection(
                        title = "Action Plan:",
                        description = "Consider opening a Long position from the current entry zone. The bounce from the 0.618 Fibonacci level provides a low-risk, high-reward trading opportunity.",
                        titleColor = 0xFFFF5252
                    )
                )
            ).apply {
                _entryPrice = "$64,250.0"
                _stopLoss = "$62,100.0"
                _takeProfit = "$71,500.0"
            }
        }
    }
}