package com.otplessreactnativelp

import android.app.Activity
import android.content.Intent
import com.facebook.react.bridge.ActivityEventListener
import com.facebook.react.bridge.Callback
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.otpless.loginpage.main.ConnectController
import com.otpless.loginpage.model.AuthResponse
import com.otpless.loginpage.util.Utility
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject

class OtplessReactNativeLPModule(private val reactContext: ReactApplicationContext) :
  ReactContextBaseJavaModule(reactContext), ActivityEventListener {

  private lateinit var connectController: ConnectController


  init {
    OtplessReactNativeLPManager.registerOtplessModule(this)
    reactContext.addActivityEventListener(this)
  }

  override fun getName(): String {
    return NAME
  }

  private fun sendResultCallback(result: AuthResponse) {
    fun sendResultEvent(result: JSONObject) {
      try {
        val map = convertJsonToMap(result)
        this.reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
          .emit("OTPlessEventResult", map)
      } catch (_: JSONException) {

      }
    }

    sendResultEvent(result.response)
  }

  @ReactMethod
  fun stop() {
    if (this::connectController.isInitialized) {
      connectController.closeOtpless()
    }
  }

  @ReactMethod
  fun initialize(appId: String) {
    connectController = ConnectController.getInstance(currentActivity!!, appId)
    connectController.initializeOtpless()
  }

  @ReactMethod
  fun start() {
    CoroutineScope(Dispatchers.IO).launch {
      connectController.startOtplessWithLoginPage()
    }
  }

  @ReactMethod
  fun setResponseCallback() {
    connectController.registerResponseCallback(this::sendResultCallback)
  }

  companion object {
    const val NAME = "OtplessReactNativeLP"
  }

  override fun onActivityResult(
    activity: Activity?,
    requestCode: Int,
    resultCode: Int,
    data: Intent?
  ) {
  }

  override fun onNewIntent(intent: Intent?) {
    intent ?: return
    connectController.onNewIntent(currentActivity!!, intent)
  }

}






