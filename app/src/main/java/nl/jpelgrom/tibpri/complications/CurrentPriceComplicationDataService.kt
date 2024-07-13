package nl.jpelgrom.tibpri.complications

import android.content.ComponentName
import android.content.Context
import android.graphics.drawable.Icon
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.MonochromaticImage
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.RangedValueComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.hilt.android.AndroidEntryPoint
import nl.jpelgrom.tibpri.R
import nl.jpelgrom.tibpri.data.PriceRepository
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class CurrentPriceComplicationDataService : SuspendingComplicationDataSourceService() {

    @Inject
    lateinit var priceRepository: PriceRepository

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
            .setText(PlainComplicationText.Builder(String.format(Locale.getDefault(), "%.1f", 0.272f * 100)).build())
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
        scheduleComplicationUpdate()

        val allPrices = priceRepository.getPrices()
        val minPrice = allPrices.minByOrNull { it.total }?.total
        val maxPrice = allPrices.maxByOrNull { it.total }?.total
        val dtNow = OffsetDateTime.now()
        val currentPrice = try {
            allPrices.sortedBy { it.startsAt }
                .lastOrNull { OffsetDateTime.parse(it.startsAt).isBefore(dtNow) }
                ?.total
        } catch (e: DateTimeParseException) {
            null
        }

        val image =
            MonochromaticImage.Builder(Icon.createWithResource(this, R.drawable.ic_rounded_bolt))
                .build()

        return if (minPrice != null && maxPrice != null && currentPrice != null) {
            RangedValueComplicationData.Builder(
                value = currentPrice.toFloat(),
                min = minPrice.toFloat(),
                max = maxPrice.toFloat(),
                contentDescription = PlainComplicationText.Builder(getString(R.string.current_energy_price))
                    .build()
            )
                // Assume EUR and show nice prices: 0.2543 should be shown as 25.4
                .setText(
                    PlainComplicationText.Builder(String.format(Locale.getDefault(), "%.1f", currentPrice * 100)).build()
                )
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

    override fun onComplicationDeactivated(complicationInstanceId: Int) {
        // Remove the scheduled worker, if set, to prevent unnecessary updates
        WorkManager.getInstance(this).cancelUniqueWork(CurrentPriceComplicationWorker.TAG)
    }

    private fun scheduleComplicationUpdate() {
        // Schedule a worker to push a complication update to ensure it shows the new price on time
        // Note: assumes price changes on every hour (:00) only
        val nextHourStarts = LocalDateTime.now().plusHours(1).truncatedTo(ChronoUnit.HOURS)
            .atZone(ZoneId.systemDefault()).toInstant()
        val workRequest = OneTimeWorkRequestBuilder<CurrentPriceComplicationWorker>()
            .setInitialDelay(
                nextHourStarts.toEpochMilli() - System.currentTimeMillis(),
                TimeUnit.MILLISECONDS
            )
            .build()
        WorkManager.getInstance(this)
            .enqueueUniqueWork(
                CurrentPriceComplicationWorker.TAG,
                ExistingWorkPolicy.KEEP,
                workRequest
            )
    }
}

class CurrentPriceComplicationWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    companion object {
        const val TAG = "CurrentPriceComplicationWorker"
    }

    override suspend fun doWork(): Result {
        applicationContext.let {
            ComplicationDataSourceUpdateRequester.create(
                it,
                ComponentName(it, CurrentPriceComplicationDataService::class.java)
            )
                .requestUpdateAll()
        }
        return Result.success()
    }

}