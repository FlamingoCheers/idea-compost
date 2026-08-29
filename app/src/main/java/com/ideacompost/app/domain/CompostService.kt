package com.ideacompost.app.domain

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 发酵前台服务（specs/41 + 工作计划 V2）：熄屏/离开页面后发酵继续。
 * 服务持有引擎任务；被停止（用户暂停）时任务随作用域取消，引擎把堆肥置为 suspended（断点保留）。
 */
@AndroidEntryPoint
class CompostService : Service() {

    @Inject lateinit var engine: CompostEngine
    @Inject lateinit var progressBus: CompostProgressBus

    private var scope: CoroutineScope? = null
    private var job: Job? = null
    private var compostId: String? = null
    private var notifManager: NotificationManager? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val id = intent?.getStringExtra(EXTRA_ID)
        if (id == null) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (job != null && compostId == id) return START_STICKY

        startInForeground(id)
        compostId = id
        val s = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        scope = s
        CompostServiceLock.active = id
        job = s.launch {
            try {
                engine.run(id)
            } catch (ce: kotlinx.coroutines.CancellationException) {
                android.util.Log.d("CompostService", "engine cancelled → suspended")
            } catch (t: Throwable) {
                android.util.Log.d("CompostService", "engine failed: ${t.javaClass.simpleName}")
            }
            CompostServiceLock.active = null
            stopSelf()
        }
        // 通知随逐菌进度更新
        s.launch {
            progressBus.state.collect { r ->
                if (r?.compostId == id && r.total > 0) {
                    notifManager?.notify(
                        NOTIF_ID,
                        buildNotification("正在发酵：第 ${r.round.removePrefix("r")} 轮 · ${r.done}/${r.total} 菌已归位", id)
                    )
                }
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        job?.cancel()
        scope?.cancel()
        scope = null
        job = null
        if (CompostServiceLock.active == compostId) CompostServiceLock.active = null
        super.onDestroy()
    }

    private fun startInForeground(compostId: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL, "思想发酵", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "堆肥进行时的前台通知"
                    setShowBadge(false)
                }
            )
        }
        startInForegroundCompat(buildNotification("正在为你的思想发酵……", compostId))
        notifManager = manager
    }

    private fun startInForegroundCompat(n: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIF_ID, n)
        }
    }

    private fun buildNotification(text: String, compostId: String): Notification =
        NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle("思想堆肥")
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(0, 0, true)
            .setContentIntent(
                android.app.PendingIntent.getActivity(
                    this@CompostService, 0,
                    Intent(this@CompostService, com.ideacompost.app.MainActivity::class.java)
                        .putExtra("open_compost_id", compostId),
                    android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
                )
            )
            .build()

    companion object {
        const val CHANNEL = "compost_ferment"
        const val NOTIF_ID = 47
        const val EXTRA_ID = "compost_id"

        fun start(context: Context, compostId: String) {
            val i = Intent(context, CompostService::class.java).putExtra(EXTRA_ID, compostId)
            androidx.core.content.ContextCompat.startForegroundService(context, i)
        }

        fun stop(context: Context, compostId: String) {
            if (CompostServiceLock.active == compostId) context.stopService(Intent(context, CompostService::class.java))
        }
    }
}

/** 进程内记录当前正在发酵的堆肥，供 UI 判断服务是否存活。 */
object CompostServiceLock {
    @Volatile var active: String? = null
}
