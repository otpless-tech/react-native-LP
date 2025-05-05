package com.otplessreactnativelp

import android.app.Activity
import android.content.Intent
import com.facebook.react.bridge.ActivityEventListener
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.ReadableType
import com.facebook.react.bridge.WritableMap
import com.facebook.react.bridge.WritableNativeMap
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.otpless.loginpage.main.OtplessController
import com.otpless.loginpage.model.CustomTabParam
import com.otpless.loginpage.model.ErrorType
import com.otpless.loginpage.model.LoginPageParams
import com.otpless.loginpage.model.OtplessResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONException

class OtplessReactNativeLPModule(private val reactContext: ReactApplicationContext) :
  ReactContextBaseJavaModule(reactContext), ActivityEventListener {

  private lateinit var otplessController: OtplessController


  init {
    OtplessReactNativeLPManager.registerOtplessModule(this)
    reactContext.addActivityEventListener(this)
  }

  override fun getName(): String {
    return NAME
  }

  private fun sendResultCallback(result: OtplessResult) {
    fun sendResultEvent(result: OtplessResult) {
      try {
        val map = result.toWritableMap()
        this.reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
          .emit("OTPlessEventResult", map)
      } catch (_: JSONException) {

      }
    }
    sendResultEvent(result)
  }

  @ReactMethod
  fun stop() {
    if (this::otplessController.isInitialized) {
      otplessController.closeOtpless()
    }
  }

  @ReactMethod
  fun initialize(appId: String) {
    otplessController = OtplessController.getInstance(currentActivity!!)
    otplessController.initializeOtpless(appId)
  }

  @ReactMethod
  fun start(loginRequest: ReadableMap?) {
    CoroutineScope(Dispatchers.IO).launch {
      otplessController.startOtplessWithLoginPage(
        convertToLoginPageParams(loginRequest)
      )
    }
  }

  @ReactMethod
  fun setResponseCallback() {
    otplessController.registerResultCallback(this::sendResultCallback)
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
    otplessController.onNewIntent(currentActivity!!, intent)
  }

}

fun OtplessResult.toWritableMap(): WritableMap {
  val map: WritableMap = WritableNativeMap()
  when (this) {
    is OtplessResult.Success -> {
      map.putString("token", this.token)
      map.putString("traceId", this.traceId)
    }

    is OtplessResult.Error -> {
      map.putString("traceId", this.traceId)
      map.putString("errorMessage", this.errorMessage)
      map.putInt("errorCode", this.errorCode)
      map.putString("errorType", this.errorType.toStr())
    }
  }
  return map
}

private fun ErrorType.toStr(): String {
  return when (this) {
    ErrorType.VERIFY -> "VERIFY"
    ErrorType.NETWORK -> "NETWORK"
    ErrorType.INITIATE -> "INITIATE"
  }
}

private fun convertToLoginPageParams(request: ReadableMap?): LoginPageParams {
  request ?: return LoginPageParams()
  var waitTime = 2_000
  if (request.hasKey("waitTime")) {
    waitTime = request.getInt("waitTime")
  }
  var extraParams: Map<String, String> = emptyMap()
  request.getMap("extraQueryParams")?.let {
    extraParams = it.toStringMap()
  }
  //region parsing all members of custom tab params if any
  val customTapMap = request.getMap("customTabParam")
  val toolbarColor = customTapMap?.getString("toolbarColor") ?: ""
  val secondaryToolbarColor = customTapMap?.getString("secondaryToolbarColor") ?: ""
  val navigationBarColor = customTapMap?.getString("navigationBarColor") ?: ""
  val navigationBarDividerColor = customTapMap?.getString("navigationBarDividerColor") ?: ""
  val backgroundColor: String? = customTapMap?.getString("backgroundColor")
  val customTabParam = CustomTabParam(
    toolbarColor = toolbarColor, secondaryToolbarColor = secondaryToolbarColor,
    navigationBarColor = navigationBarColor, navigationBarDividerColor = navigationBarDividerColor, backgroundColor = backgroundColor
  )
  //endregion
  return LoginPageParams(waitTime = waitTime.toLong(), extraQueryParams = extraParams, customTabParam = customTabParam)
}

fun ReadableMap.toStringMap(): Map<String, String> {
  val result = mutableMapOf<String, String>()
  val iter = keySetIterator()
  while (iter.hasNextKey()) {
    val key = iter.nextKey()
    val valueStr = when (getType(key)) {
      ReadableType.String -> getString(key).orEmpty()
      ReadableType.Number -> getDouble(key).toString()
      ReadableType.Boolean -> getBoolean(key).toString()
      ReadableType.Null -> ""
      ReadableType.Map -> getMap(key).toString()
      ReadableType.Array -> getArray(key).toString()
    }
    result[key] = valueStr
  }
  return result
}






