package nl.jpelgrom.tibpri.complications

import android.graphics.drawable.Icon
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.MonochromaticImage
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.RangedValueComplicationData
import androidx.wear.watchface.complications.data.TimeRange
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import nl.jpelgrom.tibpri.EnergyPriceInfoQuery
import nl.jpelgrom.tibpri.R
import nl.jpelgrom.tibpri.data.apolloClient
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class CurrentPriceComplicationDataService : SuspendingComplicationDataSourceService() {

    companion object {
        private const val TAG = "CurrentPriceComplication"
    }

    override fun getPreviewData(type: ComplicationType): ComplicationData =
        RangedValueComplicationData.Builder(
            value = 0.272f,
            min = 0.2047f,
            max = 0.2902f,
            contentDescription = PlainComplicationText.Builder(getString(R.string.current_energy_price))
                .build()
        )
            .setText(PlainComplicationText.Builder(String.format("%.1f", 0.272f * 100)).build())
            .setMonochromaticImage(
                MonochromaticImage.Builder(
                    Icon.createWithResource(
                        this,
                        R.drawable.ic_rounded_bolt
                    )
                )
                    .build()
            )
            .build()

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData {
        val data = apolloClient.query(EnergyPriceInfoQuery()).execute().data
        val priceInfo = data?.viewer?.homes?.get(0)?.currentSubscription?.priceInfo

        val minToday = priceInfo?.today?.filter { it?.total != null }?.minBy { it!!.total!! }?.total
        val maxToday = priceInfo?.today?.filter { it?.total != null }?.maxBy { it!!.total!! }?.total
        val currentPrice = priceInfo?.today?.get(LocalTime.now().hour)?.total

        val image =
            MonochromaticImage.Builder(Icon.createWithResource(this, R.drawable.ic_rounded_bolt))
                .build()

        return if (minToday != null && maxToday != null && currentPrice != null) {
            val nextHourStarts = LocalDateTime.now()
                .plusHours(1)
                .truncatedTo(ChronoUnit.HOURS)
                .atZone(ZoneId.systemDefault())
                .toInstant()

            RangedValueComplicationData.Builder(
                value = currentPrice.toFloat(),
                min = minToday.toFloat(),
                max = maxToday.toFloat(),
                contentDescription = PlainComplicationText.Builder(getString(R.string.current_energy_price))
                    .build()
            )
                // Assume EUR and show nice prices: 0.2543 should be shown as 25.4
                .setText(
                    PlainComplicationText.Builder(String.format("%.1f", currentPrice * 100)).build()
                )
                .setValidTimeRange(TimeRange.before(nextHourStarts))
                .setMonochromaticImage(image)
                .build()
        } else { // Issue getting price data
            RangedValueComplicationData.Builder(
                value = 0f,
                min = 0f,
                max = 1f,
                contentDescription = PlainComplicationText.Builder(getString(R.string.error_fetching_data))
                    .build()
            )
                .setText(PlainComplicationText.Builder("?").build())
                .setMonochromaticImage(image)
                .build()
        }
    }
}