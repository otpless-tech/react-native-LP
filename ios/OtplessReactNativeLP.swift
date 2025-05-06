import OtplessSwiftLP
import UIKit
import SafariServices


@objc(OtplessReactNativeLP)
class OtplessReactNativeLP: RCTEventEmitter, ConnectResponseDelegate {
  func onConnectResponse(_ response: OtplessResult) {
    var dict: [String: Any?] = [:]
    if response.status.lowercased() == "error" {
      dict["errorMesage"] = response.errorMessage
      dict["errorCode"] = response.errorCode
      dict["errorType"] = response.errorType
    } else {
      dict["token"] = response.token
    }
    dict["traceId"] = response.traceId
    dict["status"] = response.status.lowercased()
    sendEvent(withName: "OTPlessEventResult", body: dict)
  }
  
  private var currentTask: Task<Void, Never>?
  
  override func supportedEvents() -> [String]! {
    return ["OTPlessEventResult"]
  }
  
  @objc(initialize:callback:)
  func initialize(appId: String, callback: @escaping RCTResponseSenderBlock) {
    DispatchQueue.main.async {
      let rootViewController = UIApplication.shared.delegate?.window??.rootViewController
      if rootViewController != nil {
        OtplessSwiftLP.shared.initialize(appId: appId, onTraceIDReceived: { traceId in
          callback([traceId])
        })
        return
      }
      
      // Could not get an instance of RootViewController. Try to get RootViewController from `windowScene`.
      if #available(iOS 13.0, *) {
        let windowSceneVC = self.getRootViewControllerFromWindowScene()
        if windowSceneVC != nil {
          OtplessSwiftLP.shared.initialize(appId: appId, onTraceIDReceived: { traceId in
            callback([traceId])
          })
          return
        }
      }
    }
  }
  
  @objc(start:)
  func start(request: [String: Any]) {
    DispatchQueue.main.async {
      let windowSceneVC = self.getRootViewControllerFromWindowScene()
      if windowSceneVC == nil {
        return
      }
      let requestParams = self.parseRequest(request)
      let timeout = request["waitTime"] as? Int ?? 2000
      if let loadingUrl = request["loadingUrl"] as? String {
        OtplessSwiftLP.shared.start(baseUrl: loadingUrl, vc: windowSceneVC!, options: requestParams.options, extras: requestParams.extras, timeout: TimeInterval(timeout / 1000))
      } else {
        OtplessSwiftLP.shared.start(vc: windowSceneVC!, options: requestParams.options, extras: requestParams.extras, timeout: TimeInterval(timeout / 1000))
      }
      
    }
  }
  
  @objc(setLogging:)
  func setLogging(status: Bool) {
    print("enabling logging \(status)")
  }
  
  @objc(userAuthEvent:fallback:type:providerInfo:)
  func userAuthEvent(event: String, fallback: Bool, type: String, providerInfo: [String : Any]) {
    OtplessSwiftLP.shared.userAuthEvent(event: event, providerType: type, fallback: fallback, providerInfo: providerInfo.compactMapValues({ $0 as? String }))
  }
  
  
  @objc(setResponseCallback)
  func setResponseCallback() {
    OtplessSwiftLP.shared.setResponseDelegate(self)
  }
  
  @objc(stop)
  func stop() {
    OtplessSwiftLP.shared.cease()
  }
  
  @MainActor @available(iOS 13.0, *)
  private func getRootViewControllerFromWindowScene() -> UIViewController? {
    guard let windowScene = UIApplication.shared.connectedScenes
      .filter({ $0.activationState == .foregroundActive })
      .first as? UIWindowScene else {
      return nil
    }
    
    if #available(iOS 15.0, *) {
      let keyWindowVC = windowScene.windows.first?.windowScene?.keyWindow?.rootViewController
      if keyWindowVC != nil {
        return keyWindowVC
      }
    }
    
    return windowScene.windows.first?.rootViewController
  }
  
  @MainActor @available(iOS 13.0, *)
  private func getWindowScene() -> UIWindowScene? {
    guard let windowScene = UIApplication.shared.connectedScenes
      .filter({ $0.activationState == .foregroundActive })
      .first as? UIWindowScene else {
      return nil
    }
    return windowScene
  }
  
  func parseRequest(_ request: [String: Any]) -> (options: SafariCustomizationOptions, extras: [String: String]) {
    var preferredBarTintColor: UIColor? = nil
    var preferredControlTintColor: UIColor? = nil
    var dismissButtonStyle: SFSafariViewController.DismissButtonStyle = .close
    var modalPresentationStyle: UIModalPresentationStyle = .automatic
    
    if let safariVCParams = request["customTabParam"] as? [String: Any] {
      if let barColorHex = safariVCParams["preferredBarTintColor"] as? String {
        preferredBarTintColor = UIColor(hex: barColorHex)
      }
      if let controlColorHex = safariVCParams["preferredControlTintColor"] as? String {
        preferredControlTintColor = UIColor(hex: controlColorHex)
      }
      if let dismissStyleString = safariVCParams["dismissButtonStyle"] as? String {
        dismissButtonStyle = SFSafariViewController.DismissButtonStyle.from(string: dismissStyleString)
      }
      if let modalStyleString = safariVCParams["modalPresentationStyle"] as? String {
        modalPresentationStyle = UIModalPresentationStyle.from(string: modalStyleString)
      }
    }
    
    let options = SafariCustomizationOptions(
      preferredBarTintColor: preferredBarTintColor,
      preferredControlTintColor: preferredControlTintColor,
      dismissButtonStyle: dismissButtonStyle,
      modalPresentationStyle: modalPresentationStyle
    )
    
    let extras = (request["extraQueryParams"] as? [String: String])?.compactMapValues { "\($0)" } ?? [:]
    
    return (options, extras)
  }
  
}
