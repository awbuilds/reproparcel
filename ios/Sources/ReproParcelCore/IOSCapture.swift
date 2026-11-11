// SPDX-License-Identifier: Apache-2.0
import Foundation
#if canImport(UIKit)
import UIKit

/// Reference iOS capture boundary. iOS does not expose Android's PixelCopy or
/// FLAG_SECURE equivalents; this implementation is intentionally conservative
/// and must be validated on supported iOS releases before any release claim.
@available(iOS 15.0, *)
public final class IOSCaptureController {
    public enum Outcome {
        case captured(UIImage)
        case omitted(OmissionReason)
    }

    public init() {}

    @MainActor
    public func capture(window: UIWindow) -> Outcome {
        guard !window.isHidden else { return .omitted(.hostUnavailable) }
        let bounds = window.bounds
        guard bounds.width > 0, bounds.height > 0 else { return .omitted(.unsupportedSurface) }
        guard ImageBudget.isWithinDecodedBudget(
            width: Int(bounds.width * window.screen.scale),
            height: Int(bounds.height * window.screen.scale)
        ) else { return .omitted(.sizeLimit) }

        let renderer = UIGraphicsImageRenderer(bounds: bounds)
        let image = renderer.image { _ in
            window.drawHierarchy(in: bounds, afterScreenUpdates: true)
        }
        // UIKit cannot prove secure-text subtrees are absent from a hierarchy
        // snapshot. Hosts must exclude sensitive screens; omission is safer.
        return .captured(image)
    }
}
#endif
