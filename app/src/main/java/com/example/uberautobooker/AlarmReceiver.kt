package com.example.uberautobooker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra("requestId", -1)
        if (id == -1) return

        val prefs = context.getSharedPreferences("uber_book_prefs", Context.MODE_PRIVATE)
        val entry = prefs.getString("booking_$id", null) ?: return

        val parts = entry.split(",")
        if (parts.size < 5) return

        val pickupLat = parts[0].toDouble()
        val pickupLng = parts[1].toDouble()
        val dropLat = parts[2].toDouble()
        val dropLng = parts[3].toDouble()

        val uriString = "uber://?action=setPickup" +
                "&pickup[latitude]=$pickupLat" +
                "&pickup[longitude]=$pickupLng" +
                "&dropoff[latitude]=$dropLat" +
                "&dropoff[longitude]=$dropLng"

        val deeplink = Uri.parse(uriString)
        val appIntent = Intent(Intent.ACTION_VIEW, deeplink).apply {
            setPackage("com.ubercab")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(appIntent)
        } catch (e: Exception) {
            val webUri = Uri.parse(
                "https://m.uber.com/ul/?action=setPickup" +
                        "&pickup[latitude]=$pickupLat" +
                        "&pickup[longitude]=$pickupLng" +
                        "&dropoff[latitude]=$dropLat" +
                        "&dropoff[longitude]=$dropLng"
            )
            val webIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
        }
    }
}
