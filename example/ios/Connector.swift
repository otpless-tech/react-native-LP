//
//  Connector.swift
//  OtplessReactNativeLPExample
//
//  Created by Digvijay Singh on 06/07/23.
//

import Foundation
import OtplessSwiftLP

class Connector: NSObject {
  @objc public static func isOtplessDeeplink(_ url: URL) -> Bool {
    return OtplessSwiftLP.shared.isOtplessDeeplink(url: url)
  }
  
  @objc public static func processDeepLink(_ url: URL) {
    OtplessSwiftLP.shared.processOtplessDeeplink(url: url)
  }
}
