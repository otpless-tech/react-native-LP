import OtplessSwiftLP


@objc(OtplessReactNativeLP)
class OtplessReactNativeLP: RCTEventEmitter, ConnectResponseDelegate {
  func onConnectResponse(_ response: [String : Any]) {
    sendEvent(withName: "OTPlessEventResult", body: response)
  }
  
  private var currentTask: Task<Void, Never>?
  
  override func supportedEvents() -> [String]! {
    return ["OTPlessEventResult"]
  }
  
  @objc(initialize:)
  func initialize(appId: String) {
    DispatchQueue.main.async {
      let rootViewController = UIApplication.shared.delegate?.window??.rootViewController
      if rootViewController != nil {
        OtplessSwiftLP.shared.initialize(appId: appId)
        return
      }
      
      // Could not get an instance of RootViewController. Try to get RootViewController from `windowScene`.
      if #available(iOS 13.0, *) {
        let windowSceneVC = self.getRootViewControllerFromWindowScene()
        if windowSceneVC != nil {
          OtplessSwiftLP.shared.initialize(appId: appId)
          return
        }
      }
    }
  }
  
  @objc(start)
  func start() {
    DispatchQueue.main.async {
      let windowSceneVC = self.getRootViewControllerFromWindowScene()
      if windowSceneVC == nil {
        return
      }
      OtplessSwiftLP.shared.start(vc: windowSceneVC!)
    }
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
}

