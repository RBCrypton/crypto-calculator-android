package com.example.cryptocalculator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.DecimalFormat

// ==========================================
// 1. DATA MODELS & ENUMS
// ==========================================

enum class PositionType { LONG, SHORT }

data class CalculatorUiState(
    val positionType: PositionType = PositionType.LONG,
    val entryPrice: String = "65000",
    val margin: String = "1000",
    val leverage: Float = 10f,
    val maintenanceMarginRate: Float = 0.005f, // 0.5%
    val targetPrice: String = "70000",
    val stopLossPrice: String = "62000"
) {
    // Calculated Outputs
    val positionSize: Double
        get() = (margin.toDoubleOrNull() ?: 0.0) * leverage

    val liquidationPrice: Double?
        get() {
            val entry = entryPrice.toDoubleOrNull() ?: return null
            if (entry <= 0 || leverage <= 0) return null

            return if (positionType == PositionType.LONG) {
                val liq = entry * (1.0 - (1.0 / leverage) + maintenanceMarginRate)
                if (liq > 0) liq else 0.0
            } else {
                entry * (1.0 + (1.0 / leverage) - maintenanceMarginRate)
            }
        }

    val distanceToLiquidationPercent: Double?
        get() {
            val entry = entryPrice.toDoubleOrNull() ?: return null
            val liq = liquidationPrice ?: return null
            if (entry == 0.0) return null
            return ((liq - entry) / entry) * 100
        }

    val takeProfitPnl: Double?
        get() = calculatePnl(targetPrice.toDoubleOrNull())

    val stopLossPnl: Double?
        get() = calculatePnl(stopLossPrice.toDoubleOrNull())

    private fun calculatePnl(exitPrice: Double?): Double? {
        val entry = entryPrice.toDoubleOrNull() ?: return null
        val exit = exitPrice ?: return null
        if (entry <= 0) return null

        val priceDiff = if (positionType == PositionType.LONG) exit - entry else entry - exit
        return positionSize * (priceDiff / entry)
    }
}

// ==========================================
// 2. VIEWMODEL
// ==========================================

class CryptoCalculatorViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(CalculatorUiState())
    val uiState: StateFlow<CalculatorUiState> = _uiState.asStateFlow()

    fun updatePositionType(type: PositionType) {
        _uiState.update { it.copy(positionType = type) }
    }

    fun updateEntryPrice(value: String) {
        if (value.all { it.isDigit() || it == '.' }) {
            _uiState.update { it.copy(entryPrice = value) }
        }
    }

    fun updateMargin(value: String) {
        if (value.all { it.isDigit() || it == '.' }) {
            _uiState.update { it.copy(margin = value) }
        }
    }

    fun updateLeverage(value: Float) {
        _uiState.update { it.copy(leverage = value) }
    }

    fun updateTargetPrice(value: String) {
        if (value.all { it.isDigit() || it == '.' }) {
            _uiState.update { it.copy(targetPrice = value) }
        }
    }

    fun updateStopLossPrice(value: String) {
        if (value.all { it.isDigit() || it == '.' }) {
            _uiState.update { it.copy(stopLossPrice = value) }
        }
    }
}

// ==========================================
// 3. UI IMPLEMENTATION (JETPACK COMPOSE)
// ==========================================

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFF388E3C),
                    secondary = Color(0xFFD32F2F),
                    surface = Color(0xFF1E1E2E),
                    background = Color(0xFF12121A)
                )
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CalculatorScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(viewModel: CryptoCalculatorViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val moneyFormat = remember { DecimalFormat("#,##0.00") }
    val cryptoFormat = remember { DecimalFormat("#,##0.00") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Futures Liquidation & PnL", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Position Selector (Long / Short)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { viewModel.updatePositionType(PositionType.LONG) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (state.positionType == PositionType.LONG) Color(0xFF2E7D32) else Color(0xFF2A2A38)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("LONG / BUY", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { viewModel.updatePositionType(PositionType.SHORT) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (state.positionType == PositionType.SHORT) Color(0xFFC62828) else Color(0xFF2A2A38)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("SHORT / SELL", fontWeight = FontWeight.Bold)
                }
            }

            // Input Fields Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    OutlinedTextField(
                        value = state.entryPrice,
                        onValueChange = { viewModel.updateEntryPrice(it) },
                        label = { Text("Entry Price ($)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = state.margin,
                        onValueChange = { viewModel.updateMargin(it) },
                        label = { Text("Margin / Collateral ($)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Leverage Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Leverage", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "${state.leverage.toInt()}x",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (state.leverage > 25f) Color(0xFFFF9800) else Color.White
                            )
                        }
                        Slider(
                            value = state.leverage,
                            onValueChange = { viewModel.updateLeverage(it) },
                            valueRange = 1f..100f,
                            steps = 98
                        )
                    }
                }
            }

            // Liquidation & Position Metrics Output Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (state.positionType == PositionType.LONG) Color(0xFF1B382B) else Color(0xFF381B22)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "Risk Metrics",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.LightGray
                    )

                    MetricRow(
                        label = "Total Position Size:",
                        value = "$${moneyFormat.format(state.positionSize)}"
                    )

                    MetricRow(
                        label = "Liquidation Price:",
                        value = state.liquidationPrice?.let { "$${cryptoFormat.format(it)}" } ?: "--",
                        valueColor = Color(0xFFFF5252),
                        isBold = true
                    )

                    MetricRow(
                        label = "Distance to Liquidation:",
                        value = state.distanceToLiquidationPercent?.let { "${cryptoFormat.format(it)}%" } ?: "--",
                        valueColor = Color(0xFFFFAB40)
                    )
                }
            }

            // Target Price & Stop Loss Simulator Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "TP / SL Scenario Simulator",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    OutlinedTextField(
                        value = state.targetPrice,
                        onValueChange = { viewModel.updateTargetPrice(it) },
                        label = { Text("Take Profit Target ($)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    MetricRow(
                        label = "Expected TP Profit:",
                        value = state.takeProfitPnl?.let {
                            "${if (it >= 0) "+" else ""}$${moneyFormat.format(it)}"
                        } ?: "--",
                        valueColor = if ((state.takeProfitPnl ?: 0.0) >= 0) Color(0xFF69F0AE) else Color(0xFFFF5252)
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = Color(0xFF2A2A38))

                    OutlinedTextField(
                        value = state.stopLossPrice,
                        onValueChange = { viewModel.updateStopLossPrice(it) },
                        label = { Text("Stop Loss Exit ($)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    MetricRow(
                        label = "Expected SL Loss:",
                        value = state.stopLossPnl?.let {
                            "${if (it >= 0) "+" else ""}$${moneyFormat.format(it)}"
                        } ?: "--",
                        valueColor = if ((state.stopLossPnl ?: 0.0) >= 0) Color(0xFF69F0AE) else Color(0xFFFF5252)
                    )
                }
            }
        }
    }
}

@Composable
fun MetricRow(
    label: String,
    value: String,
    valueColor: Color = Color.White,
    isBold: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.Gray, fontSize = 14.sp)
        Text(
            value,
            color = valueColor,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium,
            fontSize = if (isBold) 16.sp else 14.sp
        )
    }
}
