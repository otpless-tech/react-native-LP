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
    return ["OTPlessEventResult", "OtplessEventObserver"]
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
      let extras = (request["extraQueryParams"] as? [String: String])?.compactMapValues { "\($0)" } ?? [:]
      if let loadingUrl = request["loadingUrl"] as? String {
        OtplessSwiftLP.shared.start(baseUrl: loadingUrl, vc: windowSceneVC!, extras: extras)
      } else {
        OtplessSwiftLP.shared.start(vc: windowSceneVC!, extras: extras)
      }
      
    }
  }
  
  @objc(setLogging:)
  func setLogging(status: Bool) {
    print("enabling logging \(status)")
    if status {
      OtplessSwiftLP.shared.enableSocketLogging()
    }
  }
  
  @objc(userAuthEvent:fallback:type:providerInfo:)
  func userAuthEvent(event: String, fallback: Bool, type: String, providerInfo: [String : Any]) {
    OtplessSwiftLP.shared.userAuthEvent(event: event, providerType: type, fallback: fallback, providerInfo: providerInfo.compactMapValues({ $0 as? String }))
  }
  
  
  @objc(setResponseCallback)
  func setResponseCallback() {
    OtplessSwiftLP.shared.setResponseDelegate(self)
  }
  
  @objc(addEventObserver)
  func addEventObserver() {
    OtplessSwiftLP.shared.setEventDelegate(self)
  }
  
  @objc(stop)
  func stop() {
    OtplessSwiftLP.shared.cease()
  }
  
  @objc(setWebViewInspectable)
  func setWebViewInspectable() {
    print("enabling webview inspectable property")
    OtplessSwiftLP.shared.webviewInspectable = true
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
  
  @objc(initializeSessionManager:)
  func initializeSessionManager(appId: String) {
    
  }
  
  @objc(getActiveSession:reject:)
  func getActiveSession(resolve: @escaping RCTPromiseResolveBlock, reject: RCTPromiseRejectBlock) {
    Task {
      let response = await OtplessSessionManager.shared.getActiveSession()
      var responseDictionary: [String: String] = [:]
      switch response {
      case .active(let sessionToken):
        responseDictionary["state"] =  "active"
        responseDictionary["sessionToken"] = sessionToken
      case .inactive:
        responseDictionary["state"] =  "inactive"
      }
      resolve(responseDictionary)
    }
  }
  
  @objc(logout)
  func logout() {
    Task {
      await OtplessSessionManager.shared.logout()
    }
  }
}


// implementation of external method for observing event
extension OtplessReactNativeLP: OnEventDelegate {
  
  func onEvent(_ event: OtplessEventData) {
    let map: [String: Any] = [
      "category": event.category.name,
      "eventType": event.eventType.name,
      "metaData": event.metaData
    ]
    sendEvent(withName: "OtplessEventObserver", body: map)
  }
}
