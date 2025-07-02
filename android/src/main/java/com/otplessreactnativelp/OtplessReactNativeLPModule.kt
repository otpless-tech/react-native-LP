package com.otplessreactnativelp

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.facebook.react.bridge.ActivityEventListener
import com.facebook.react.bridge.Callback
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.ReadableType
import com.facebook.react.bridge.WritableMap
import com.facebook.react.bridge.WritableNativeMap
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.otpless.loginpage.main.OtplessController
import com.otpless.loginpage.model.AuthEvent
import com.otpless.loginpage.model.CustomTabParam
import com.otpless.loginpage.model.ErrorType
import com.otpless.loginpage.model.LoginPageParams
import com.otpless.loginpage.model.OtplessResult
import com.otpless.loginpage.model.ProviderType
import com.otpless.loginpage.util.Utility
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONException
import java.lang.ref.WeakReference

class OtplessReactNativeLPModule(private val reactContext: ReactApplicationContext) :
  ReactContextBaseJavaModule(reactContext), ActivityEventListener {

  init {
    OtplessReactNativeLPManager.registerOtplessModule(this)
    reactContext.addActivityEventListener(this)
  }

  private fun logd(message: String) {
    Log.d(Tag, message)
  }

  private fun logd(message: String, error: Throwable) {
    Log.d(Tag, message, error)
  }

  override fun getName(): String {
    return NAME
  }

  private fun sendResultCallback(result: OtplessResult) {
    fun sendResultEvent(result: OtplessResult) {
      logd("sending result: ${result}")
      try {
        val map = result.toWritableMap()
        this.reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
          .emit("OTPlessEventResult", map)
      } catch (thr: JSONException) {
        logd("exception in sendResultCallback", thr)
      }
    }
    sendResultEvent(result)
  }

  @ReactMethod
  fun stop() {
    logd("stop otpless called")
    currentActivity?.let { InstanceProvider.getInstance(it).closeOtpless() }
  }

  @ReactMethod
  fun initialize(appId: String, cctSupportConfig: ReadableMap, callback: Callback) {
    currentActivity?.let { activity ->
      val config = cctSupportConfigUtil(cctSupportConfig)

      InstanceProvider.getInstance(activity).initializeOtpless(appId, config) { traceId ->
        callback.invoke(traceId)
      }
    }
  }

  @ReactMethod
  fun start(loginRequest: ReadableMap?) {
    val request = convertToLoginPageParams(loginRequest)
    logd("start otpless called\nrequest: $request")
    currentActivity?.let {
      CoroutineScope(Dispatchers.IO).launch {
        InstanceProvider.getInstance(it).startOtplessWithLoginPage(request)
      }
    }

  }

  @ReactMethod
  fun setResponseCallback() {
    logd("set response callback called")
    currentActivity?.let { InstanceProvider.getInstance(it).registerResultCallback(this::sendResultCallback) }
  }

  companion object {
    const val NAME = "OtplessReactNativeLP"
    private const val Tag = "OTPLESS"
  }

  override fun onActivityResult(
    activity: Activity?,
    requestCode: Int,
    resultCode: Int,
    data: Intent?
  ) {
    logd("on activity result called req: $requestCode res: $resultCode")
  }

  override fun onNewIntent(intent: Intent?) {
    logd("on new intent called: ${intent?.data}")
    intent ?: return
    currentActivity?.let { InstanceProvider.getInstance(it).onNewIntent(it, intent) }
  }

  @ReactMethod
  fun setLogging(status: Boolean) {
    logd("setting logging status: $status")
    Utility.isLoggingEnabled = status
  }

  @ReactMethod
  fun userAuthEvent(event: String, fallback: Boolean, type: String, info: ReadableMap?) {
    currentActivity?.let {
      val controller = InstanceProvider.getInstance(it)
      val authEvent = when(event) {
        "AUTH_INITIATED" -> AuthEvent.AUTH_INITIATED
        "AUTH_SUCCESS" -> AuthEvent.AUTH_SUCCESS
        "AUTH_FAILED" -> AuthEvent.AUTH_FAILED
        else -> AuthEvent.AUTH_INITIATED
      }
      val providerType = when(type) {
        "OTPLESS" -> ProviderType.OTPLESS
        "CLIENT" -> ProviderType.CLIENT
        else -> ProviderType.OTPLESS
      }
      val providerInfo: Map<String, String> = info?.toMap() ?: emptyMap()
      logd("authEvent: $authEvent, fallback: $fallback, providerType: $providerType, providerInfo: ${providerInfo.size}")
      controller.userAuthEvent(authEvent, fallback, providerType, providerInfo)
    }
  }

}

fun OtplessResult.toWritableMap(): WritableMap {
  val map: WritableMap = WritableNativeMap()
  when (this) {
    is OtplessResult.Success -> {
      map.putString("status", "success")
      map.putString("token", this.token)
      map.putString("traceId", this.traceId)
    }

    is OtplessResult.Error -> {
      map.putString("status", "error")
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
  return LoginPageParams(waitTime = waitTime.toLong(), extraQueryParams = extraParams,
    customTabParam = customTabParam, loadingUrl = request.getString("loadingUrl"))
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



internal object InstanceProvider {
  private var otplessController: OtplessController? = null
  private var activity: WeakReference<Activity?> = WeakReference(null)

  fun getInstance(activity: Activity): OtplessController {
    if (activity != this.activity.get()) {
      otplessController?.closeOtpless()
      otplessController = OtplessController.getInstance(activity)
      this.activity = WeakReference(activity)
    }
    return otplessController!!
  }
}



