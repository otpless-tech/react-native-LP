//
//  UIKitExtensions.swift
//  Pods
//
//  Created by Sparsh on 03/05/25.
//


import SafariServices

extension UIColor {
    convenience init?(hex: String) {
        var hexSanitized = hex.trimmingCharacters(in: .whitespacesAndNewlines).uppercased()
        if hexSanitized.hasPrefix("#") {
            hexSanitized.removeFirst()
        }

        var rgb: UInt64 = 0
        guard Scanner(string: hexSanitized).scanHexInt64(&rgb) else { return nil }

        switch hexSanitized.count {
        case 6:
            self.init(
                red: CGFloat((rgb & 0xFF0000) >> 16) / 255,
                green: CGFloat((rgb & 0x00FF00) >> 8) / 255,
                blue: CGFloat(rgb & 0x0000FF) / 255,
                alpha: 1.0
            )
        default:
            return nil
        }
    }
}

extension UIModalPresentationStyle {
    static func from(string: String) -> UIModalPresentationStyle {
        switch string.lowercased() {
        case "automatic": return .automatic
        case "fullscreen": return .fullScreen
        case "pagesheet": return .pageSheet
        case "formsheet": return .formSheet
        case "currentcontext": return .currentContext
        case "overfullscreen": return .overFullScreen
        case "overcurrentcontext": return .overCurrentContext
        case "popover": return .popover
        case "none": return .none
        default: return .automatic
        }
    }
}

extension SFSafariViewController.DismissButtonStyle {
    static func from(string: String) -> SFSafariViewController.DismissButtonStyle {
        switch string.lowercased() {
        case "done": return .done
        case "close": return .close
        case "cancel": return .cancel
        default: return .done
        }
    }
}
