package com.otplessreactnativelp

import java.lang.ref.WeakReference

object OtplessReactNativeLPManager {

  private var wModule: WeakReference<OtplessReactNativeLPModule>? = null

  internal fun registerOtplessModule(otplessModule: OtplessReactNativeLPModule) {
    wModule = WeakReference(otplessModule)
  }
}
