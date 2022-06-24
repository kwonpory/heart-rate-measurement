package com.example.heartratesample

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.HrAccuracy
import androidx.health.services.client.data.PassiveMonitoringUpdate
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class PassiveDataReceiver : BroadcastReceiver() {
    @Inject
    lateinit var repository: PassiveDataRepository

    override fun onReceive(context: Context, intent: Intent) {
        val state = PassiveMonitoringUpdate.fromIntent(intent) ?: return
        // 가장 최근의 심박수 측정값을 get
        val latestDataPoint = state.dataPoints
            // dataPoints can have multiple types (e.g. if the app registered for multiple types).
            .filter { it.dataType == DataType.HEART_RATE_BPM }
            // 정확도 정보가 있는 경우 정확도가 중간이거나 높은 값만 표시
            // (정확도 정보를 사용할 수 없는 경우 양의 값이면 표시)
            .filter {
                it.accuracy == null ||
                        setOf(
                            HrAccuracy.SensorStatus.ACCURACY_MEDIUM,
                            HrAccuracy.SensorStatus.ACCURACY_HIGH
                        ).contains((it.accuracy as HrAccuracy).sensorStatus)
            }
            .filter {
                it.value.asDouble() > 0
            }
            // HEART_RATE_BPM 은 SAMPLE type 이므로 시작 시간과 종료 시간이 동일
            .maxByOrNull { it.endDurationFromBoot }
        // If there were no data points, the previous function returns null.
            ?: return

        val latestHeartRate = latestDataPoint.value.asDouble() // HEART_RATE_BPM is a Float type.
        Log.d(TAG, "Received latest heart rate in background: $latestHeartRate")

        runBlocking {
            repository.storeLatestHeartRate(latestHeartRate)
        }
    }
}