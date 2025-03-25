package nl.jpelgrom.tibpri

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.scrollAway
import androidx.wear.tooling.preview.devices.WearDevices
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import nl.jpelgrom.tibpri.data.PriceRepository
import nl.jpelgrom.tibpri.data.database.HourlyEnergyPrice
import nl.jpelgrom.tibpri.theme.TibpriTheme
import nl.jpelgrom.tibpri.type.PriceLevel
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var priceRepository: PriceRepository

    private var allPrices by mutableStateOf(emptyList<HourlyEnergyPrice>())
    private var minPrice by mutableStateOf<Double?>(null)
    private var maxPrice by mutableStateOf<Double?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)

        setTheme(android.R.style.Theme_DeviceDefault)

        setContent {
            WearApp(allPrices, minPrice, maxPrice)
        }
    }

    override fun onResume() {
        super.onResume()

        lifecycleScope.launch {
            val prices = priceRepository.getPrices().sortedBy { it.startsAt }
            val nowIndex = prices.indexOfLast {
                OffsetDateTime.parse(it.startsAt).isBefore(OffsetDateTime.now())
            }
            minPrice = prices.minByOrNull { it.total }?.total
            maxPrice = prices.maxByOrNull { it.total }?.total
            allPrices = prices.subList(nowIndex, prices.size)
        }
    }
}

@Composable
fun WearApp(prices: List<HourlyEnergyPrice>, minPrice: Double?, maxPrice: Double?) {
    TibpriTheme {
        val listState = rememberScalingLazyListState()
        val formatter = DateTimeFormatter.ofPattern("HH:mm")

        Scaffold(
            positionIndicator = { PositionIndicator(listState) },
            timeText = { TimeText(modifier = Modifier.scrollAway(listState)) }
        ) {
            ScalingLazyColumn(
                state = listState,
                contentPadding = PaddingValues(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(prices.size, key = { "${prices[it].startsAt}-${it == 0}" }) { index ->
                    val thisHour = prices[index]
                    val nextHour = prices.getOrNull(index + 1)

                    // Assume EUR and show nice prices: 0.2543 should be shown as 25.4
                    val thisHourString = OffsetDateTime.parse(thisHour.startsAt).format(formatter)
                    val nextHourString =
                        nextHour?.let { OffsetDateTime.parse(it.startsAt).format(formatter) }
                    val priceString =
                        String.format(Locale.getDefault(), "%.1f", thisHour.total * 100)

                    if (index == 0) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = priceString,
                                style = MaterialTheme.typography.title2
                            )
                            if (nextHourString == null) {
                                Text(
                                    text = thisHourString,
                                    fontWeight = FontWeight.Light
                                )
                            } else {
                                Text(
                                    text = "$thisHourString-$nextHourString",
                                    fontWeight = FontWeight.Light
                                )
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = thisHourString,
                                fontWeight = FontWeight.Light,
                                modifier = Modifier.padding(end = 16.dp)
                            )
                            HorizontalPriceBar(
                                price = thisHour,
                                minPrice = minPrice ?: 0.0,
                                maxPrice = maxPrice ?: 0.0
                            )
                            Text(
                                text = priceString,
                                modifier = Modifier.padding(start = 16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RowScope.HorizontalPriceBar(
    price: HourlyEnergyPrice,
    minPrice: Double,
    maxPrice: Double
) {
    val pricePercent = (price.total - minPrice) / (maxPrice - minPrice)

    val colorBack = MaterialTheme.colors.onBackground.copy(alpha = 0.3f)
    val colorFront = MaterialTheme.colors.onBackground

    val lineHeight: Dp = 8.dp

    Spacer(
        modifier = Modifier
            .height(lineHeight)
            .weight(1f)
            .drawBehind {
                val canvasWidth = size.width
                drawLine(
                    color = colorBack,
                    start = Offset(0f, lineHeight.toPx() / 2),
                    end = Offset(canvasWidth, lineHeight.toPx() / 2),
                    strokeWidth = lineHeight.toPx(),
                    cap = StrokeCap.Round
                )
                val priceWidthPx = (pricePercent * canvasWidth).toFloat()
                drawLine(
                    color = colorFront,
                    start = Offset(0f, lineHeight.toPx() / 2),
                    end = Offset(priceWidthPx, lineHeight.toPx() / 2),
                    strokeWidth = lineHeight.toPx(),
                    cap = StrokeCap.Round
                )
            }
    )
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun DefaultPreview() {
    WearApp(
        prices = listOf(
            HourlyEnergyPrice(startsAt = "2025-02-25T21:00:00Z", total = 0.251, energy = 0.201, tax = 0.05, level = PriceLevel.NORMAL, currency = "EUR"),
            HourlyEnergyPrice(startsAt = "2025-02-25T22:00:00Z", total = 0.241, energy = 0.191, tax = 0.05, level = PriceLevel.NORMAL, currency = "EUR"),
            HourlyEnergyPrice(startsAt = "2025-02-25T23:00:00Z", total = 0.231, energy = 0.181, tax = 0.05, level = PriceLevel.NORMAL, currency = "EUR"),
            HourlyEnergyPrice(startsAt = "2025-02-26T00:00:00Z", total = 0.221, energy = 0.171, tax = 0.05, level = PriceLevel.NORMAL, currency = "EUR"),
            HourlyEnergyPrice(startsAt = "2025-02-26T00:30:00Z", total = 0.216, energy = 0.165, tax = 0.05, level = PriceLevel.NORMAL, currency = "EUR"),
            HourlyEnergyPrice(startsAt = "2025-02-26T01:00:00Z", total = 0.211, energy = 0.161, tax = 0.05, level = PriceLevel.NORMAL, currency = "EUR")
        ),
        minPrice = 0.211,
        maxPrice = 0.251
    )
}