// SPDX-License-Identifier: Apache-2.0
import Foundation

public enum ReproParcelLimits {
    public static let maxEvents = 50
    public static let maxLogs = 200
    public static let maxLogBytes = 256 * 1024
    public static let maxSemanticNodes = 500
    public static let maxSemanticDepth = 32
    public static let maxSemanticBytes = 512 * 1024
    public static let maxImageLongEdge = 1600
    public static let maxDecodedImageBytes = 12 * 1024 * 1024
    public static let maxConcurrentDecodedImageBytes = 24 * 1024 * 1024
    public static let maxParcelCompressedBytes = 5 * 1024 * 1024
    public static let maxParcelUncompressedBytes = 10 * 1024 * 1024
    public static let maxParcelEntries = 16
    public static let maxCompressionRatio = 100
    public static let maxJsonBytes = 2 * 1024 * 1024
    public static let maxJsonDepth = 32
    public static let maxJsonNodes = 10_000
    public static let maxDrafts = 10
    public static let maxDraftBytes = 25 * 1024 * 1024
    public static let draftTTL: TimeInterval = 72 * 60 * 60
}

public struct ImageDimensions: Equatable, Sendable {
    public let width: Int
    public let height: Int
    public init(width: Int, height: Int) { self.width = width; self.height = height }
    public var longestEdge: Int { max(width, height) }
    public var decodedBytes: Int64 { Int64(width) * Int64(height) * 4 }
}

public enum ImageBudget {
    public static func isWithinDecodedBudget(width: Int, height: Int) -> Bool {
        guard width > 0, height > 0 else { return false }
        let dims = ImageDimensions(width: width, height: height)
        return dims.longestEdge <= ReproParcelLimits.maxImageLongEdge * 4 &&
            dims.decodedBytes <= Int64(ReproParcelLimits.maxDecodedImageBytes)
    }

    public static func scaledToLongestEdge(width: Int, height: Int) -> ImageDimensions {
        let longest = max(width, height)
        guard longest > ReproParcelLimits.maxImageLongEdge else {
            return ImageDimensions(width: width, height: height)
        }
        let scale = Double(ReproParcelLimits.maxImageLongEdge) / Double(longest)
        return ImageDimensions(
            width: max(1, Int(Double(width) * scale)),
            height: max(1, Int(Double(height) * scale))
        )
    }
}

public enum OmissionReason: String, Codable, Sendable {
    case policyDisabled = "policy_disabled"
    case secureWindow = "secure_window"
    case unsupportedSurface = "unsupported_surface"
    case captureTimeout = "capture_timeout"
    case captureCancelled = "capture_cancelled"
    case policyChanged = "policy_changed"
    case staleFrame = "stale_frame"
    case providerFailed = "provider_failed"
    case encryptionUnavailable = "encryption_unavailable"
    case sizeLimit = "size_limit"
    case serializationFailed = "serialization_failed"
    case hostUnavailable = "host_unavailable"
    case capabilityUnsupported = "capability_unsupported"
    case invalidConfig = "invalid_config"
}

public struct EvidenceStatus: Codable, Sendable, Equatable {
    public let state: String
    public let reason: OmissionReason?
    public init(state: String, reason: OmissionReason? = nil) {
        self.state = state
        self.reason = reason
    }
}
