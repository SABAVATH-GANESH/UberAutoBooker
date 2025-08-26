package com.example.uberautobooker

import android.accessibilityservice.AccessibilityService
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.os.Handler
import android.os.Looper

class UberAccessibilityService : AccessibilityService() {

    private val TAG = "UberAccessibility"
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i(TAG, "Accessibility service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val root = rootInActiveWindow ?: return
        mainHandler.postDelayed({ tryClickConfirm(root) }, 500)
    }

    private fun tryClickConfirm(root: AccessibilityNodeInfo) {
        val possibleTexts = listOf("Request", "Request ride", "Confirm", "Book", "Request Uber")
        for (t in possibleTexts) {
            val nodes = root.findAccessibilityNodeInfosByText(t)
            if (!nodes.isNullOrEmpty()) {
                for (n in nodes) {
                    if (n.isClickable) {
                        n.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                        Log.i(TAG, "Clicked node with text: $t")
                        return
                    } else {
                        var parent = n.parent
                        while (parent != null) {
                            if (parent.isClickable) {
                                parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                                Log.i(TAG, "Clicked parent of node with text: $t")
                                return
                            }
                            parent = parent.parent
                        }
                    }
                }
            }
        }

        val altList = listOf("confirm", "book")
        // Note: findAccessibilityNodeInfosByViewId requires full view id; left as placeholder
        for (desc in altList) {
            // no-op for viewId fallback in this minimal project
        }

        mainHandler.postDelayed({ tryClickConfirm(root) }, 1200)
    }

    override fun onInterrupt() {
        Log.i(TAG, "Interrupted")
    }
}
