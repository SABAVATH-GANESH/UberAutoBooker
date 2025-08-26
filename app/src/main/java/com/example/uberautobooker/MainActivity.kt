package com.example.uberautobooker

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import java.util.*
import android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private var pickupLatLng: LatLng? = null
    private var dropLatLng: LatLng? = null
    private var selectedTimeMillis: Long = 0L

    private lateinit var tvPicked: TextView
    private lateinit var btnPickDateTime: Button
    private lateinit var btnSchedule: Button
    private lateinit var btnEnableAccessibility: Button
    private lateinit var tvStatus: TextView

    private val PREFS = "uber_book_prefs"
    private val KEY_PREFIX = "booking_"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvPicked = findViewById(R.id.tvPicked)
        btnPickDateTime = findViewById(R.id.btnPickDateTime)
        btnSchedule = findViewById(R.id.btnSchedule)
        btnEnableAccessibility = findViewById(R.id.btnEnableAccessibility)
        tvStatus = findViewById(R.id.tvStatus)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        btnPickDateTime.setOnClickListener {
            val now = Calendar.getInstance()
            val dpd = android.app.DatePickerDialog(this, { _, y, m, d ->
                val tpd = android.app.TimePickerDialog(this, { _, hour, minute ->
                    val cal = Calendar.getInstance()
                    cal.set(y, m, d, hour, minute, 0)
                    selectedTimeMillis = cal.timeInMillis
                    tvPicked.text = "Scheduled: ${Date(selectedTimeMillis)}"
                }, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), true)
                tpd.show()
            }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH))
            dpd.datePicker.minDate = now.timeInMillis
            dpd.show()
        }

        btnEnableAccessibility.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            tvStatus.text = "Enable 'Uber Auto Booker' in Accessibility settings."
        }

        btnSchedule.setOnClickListener {
            val p = pickupLatLng
            val d = dropLatLng
            if (p == null || d == null) {
                tvStatus.text = "Select both pickup and drop on the map."
                return@setOnClickListener
            }
            if (selectedTimeMillis <= System.currentTimeMillis()) {
                tvStatus.text = "Pick a future time."
                return@setOnClickListener
            }

            // 1) persist booking so AlarmReceiver can read it
            val requestId = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
            val prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val json = "${p.latitude},${p.longitude},${d.latitude},${d.longitude},${selectedTimeMillis}"
            prefs.edit().putString("$KEY_PREFIX$requestId", json).apply()

            // 2) schedule with the requestId extra
            scheduleExactBooking(this, selectedTimeMillis, requestId)

            tvStatus.text = "Booking scheduled (id=$requestId) at ${Date(selectedTimeMillis)}"
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.moveCamera(
            CameraUpdateFactory.newLatLngZoom(LatLng(28.6139, 77.2090), 10f) // Delhi
        )

        mMap.setOnMapClickListener { latLng ->
            if (pickupLatLng == null) {
                pickupLatLng = latLng
                mMap.addMarker(MarkerOptions().position(latLng).title("Pickup"))
                tvStatus.text = "Pickup set. Now tap to set Drop."
            } else if (dropLatLng == null) {
                dropLatLng = latLng
                mMap.addMarker(MarkerOptions().position(latLng).title("Drop"))
                tvStatus.text = "Pickup & Drop selected."
            } else {
                mMap.clear()
                pickupLatLng = latLng
                dropLatLng = null
                mMap.addMarker(MarkerOptions().position(latLng).title("Pickup"))
                tvStatus.text = "Pickup reset. Now tap Drop."
            }
        }
    }

    private fun scheduleExactBooking(context: Context, triggerAtMillis: Long, requestId: Int) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                startActivity(Intent(ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                tvStatus.text = "Please allow exact alarms in settings."
                return
            }
        }

        val intent = Intent(this, AlarmReceiver::class.java).apply {
            putExtra("requestId", requestId) // <-- IMPORTANT
        }

        val pending = PendingIntent.getBroadcast(
            this,
            requestId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pending)
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pending)
        }
    }
}
