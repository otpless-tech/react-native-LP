package com.otplessreactnativelp

import android.app.Activity
import android.content.Intent
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.facebook.react.bridge.ActivityEventListener
import com.facebook.react.bridge.Callback
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.ReadableType
import com.facebook.react.bridge.WritableMap
import com.facebook.react.bridge.WritableNativeMap
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.otpless.loginpage.main.OtplessController
import com.otpless.loginpage.main.OtplessEventData
import com.otpless.loginpage.model.AuthEvent
import com.otpless.loginpage.model.CustomTabParam
import com.otpless.loginpage.model.LoginPageParams
import com.otpless.loginpage.model.OtplessResult
import com.otpless.loginpage.model.OtplessSessionState
import com.otpless.loginpage.model.ProviderType
import com.otpless.loginpage.session.OtplessSessionManager
import com.otpless.loginpage.util.Utility
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONException
import java.lang.ref.WeakReference

class OtplessReactNativeLPModule(private val reactContext: ReactApplicationContext) :
  ReactContextBaseJavaModule(reactContext), ActivityEventListener {

  init {

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
    logd("sending result: $result")
    try {
      val map = result.toWritableMap()
      this.reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
        .emit("OTPlessEventResult", map)
    } catch (thr: JSONException) {
      logd("exception in sendResultCallback", thr)
    }
  }

  @Suppress("unused")
  @ReactMethod
  fun stop() {
    logd("stop otpless called")
    currentActivity?.let { InstanceProvider.getInstance(it).closeOtpless() }
  }

  @Suppress("unused")
  @ReactMethod
  fun initialize(appId: String, callback: Callback) {
    currentActivity?.let {
      InstanceProvider.getInstance(it).initializeOtpless(appId) { traceId ->
        callback(traceId)
      }
    }
  }

  @Suppress("unused")
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

  @Suppress("unused")
  @ReactMethod
  fun setResponseCallback() {
    logd("set response callback called")
    currentActivity?.let { InstanceProvider.getInstance(it).registerResultCallback(this::sendResultCallback) }
  }

  @Suppress("unused")
  @ReactMethod
  fun addEventObserver() {
    logd("setting event observer")
    currentActivity?.let {
      InstanceProvider.getInstance(it).addEventObserver(this::onOtplessEventReceived)
    }
  }

  private fun onOtplessEventReceived(data: OtplessEventData) {
    logd("sending event: $data")
    try {
      val map = data.toWritableMap()
      this.reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
        .emit("OtplessEventObserver", map)
    } catch (thr: JSONException) {
      logd("exception in onOtplessEventReceived", thr)
    }
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

  @Suppress("unused")
  @ReactMethod
  fun setLogging(status: Boolean) {
    logd("setting logging status: $status")
    Utility.isLoggingEnabled = status
  }

  @Suppress("unused")
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

  @Suppress("unused")
  @ReactMethod
  fun initializeSessionManager(appId: String) {
    val componentActivity = currentActivity as? AppCompatActivity ?: return
    componentActivity.lifecycleScope.launch {
      logd("OtplessSessionManager Initialized")
      OtplessSessionManager.init(componentActivity.applicationContext, appId)
    }
  }

  @Suppress("unused")
  @ReactMethod
  fun logout() {
    val componentActivity = currentActivity as? AppCompatActivity ?: return
    componentActivity.lifecycleScope.launch {
      logd("OtplessSessionManager Logout")
      OtplessSessionManager.logout()
    }
  }

  @Suppress("unused")
  @ReactMethod
  fun getActiveSession(promise: Promise) {
    val componentActivity = currentActivity as? AppCompatActivity ?: return
    componentActivity.lifecycleScope.launch {
      logd("OtplessSessionManager active session requested")
      val session = OtplessSessionManager.getActiveSession()

      val convertedResponse = when(session) {
        is OtplessSessionState.Inactive -> {
          WritableNativeMap().also {
            it.putString("state", "inactive")
          }
        }
        is OtplessSessionState.Active -> {
          WritableNativeMap().also {
            it.putString("state", "active")
            it.putString("sessionToken", session.jwtToken)
          }
        }
      }
      // sending info of active session
      promise.resolve(convertedResponse)
    }
  }

  companion object {
    const val NAME = "OtplessReactNativeLP"
    private const val Tag = "OTPLESS"
  }
}

// region conversion helping method

/**
 * Converts an [OtplessResult] into a React Native [WritableMap] representation.
 *
 * The resulting map includes:
 * - For [OtplessResult.Success]:
 *   - `"status"` = `"success"`

 * - For [OtplessResult.Error]:
 *   - `"status"` = `"error"`
 *
 * Extra status field added before converting [OtplessResult.Success], [OtplessResult.Error] in
 * JSON, so that they can be parsed easily
 *
 * This map can be passed across the React Native bridge for consumption
 * on the JavaScript side.
 *
 * @receiver The [OtplessResult] instance to be converted.
 * @return A [WritableMap] containing the serialized result.
 */
internal fun OtplessResult.toWritableMap(): WritableMap {
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
      map.putString("errorType", this.errorType.name)
    }
  }
  return map
}

/**
 * Converts a [ReadableMap] request into a [LoginPageParams] object
 * that can be consumed by the Otpless SDK.
 *
 * If the map is `null`, default values are used:
 * - `waitTime` defaults to 2000 ms
 * - `extraQueryParams` defaults to an empty map
 * - `customTabParam` fields default to empty strings (or `null` where applicable)
 *
 * @param request The input request as a [ReadableMap] (may be `null`).
 * @return A [LoginPageParams] instance with values parsed from the request.
 */
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
  return LoginPageParams(
    waitTime = waitTime.toLong(), extraQueryParams = extraParams,
    customTabParam = customTabParam, loadingUrl = request.getString("loadingUrl")
  )
}

/**
 * Converts a React Native [ReadableMap] into a [Map] of `String` keys and values.
 *
 * All entries are coerced into string representations, preserving the original keys.
 *
 * @receiver The [ReadableMap] instance to convert.
 * @return A [Map] with all keys preserved and values as strings.
 */

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

//endregion


/**
 * Provides a singleton-like instance of [OtplessController] tied to the current [Activity].
 *
 * If a different [Activity] is passed than the one previously held,
 * the existing controller is closed and a new instance is created.
 *
 * The [Activity] reference is stored as a [WeakReference] to avoid memory leaks.
 *
 * Usage:
 * ```
 * val controller = InstanceProvider.getInstance(activity)
 * ```
 *
 * ### Note
 * Usually different activity is not passed, even in case of low memory, RN-Module is also
 * kill along with activity, but at the time of debugging, when refreshed through metro RN-Module is
 * not created and new activity is created, to avoid this, [InstanceProvider] is used. In **Release**
 * build as no metro bundler is used so always same activity instance will send
 */
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



