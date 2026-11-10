// SPDX-License-Identifier: AGPL-3.0-only
import Foundation

public struct Bounds: Codable, Sendable, Equatable {
    public let left: Int
    public let top: Int
    public let right: Int
    public let bottom: Int
    public let coordinateSpace: String
    public init(left: Int, top: Int, right: Int, bottom: Int, coordinateSpace: String = "window") {
        self.left = left
        self.top = top
        self.right = right
        self.bottom = bottom
        self.coordinateSpace = coordinateSpace
    }
}

public struct Report: Codable, Sendable, Equatable {
    public let title: String
    public let description: String
    public let expectedBehavior: String
    public let actualBehavior: String
    public let reproSteps: String?
    public let screenId: String?
    public let evidence: [EvidenceStatus]
    public init(
        title: String = "",
        description: String,
        expectedBehavior: String = "",
        actualBehavior: String = "",
        reproSteps: String? = nil,
        screenId: String? = nil,
        evidence: [EvidenceStatus] = []
    ) {
        self.title = title
        self.description = description
        self.expectedBehavior = expectedBehavior
        self.actualBehavior = actualBehavior
        self.reproSteps = reproSteps
        self.screenId = screenId
        self.evidence = evidence
    }
}

public struct Manifest: Codable, Sendable, Equatable {
    public let schemaVersion: Int
    public let reportId: String
    public let createdAt: String
    public let producer: Producer
    public let app: App
    public let device: Device
    public let privacy: Privacy
    public let capture: Capture
    public let entries: [Entry]
    public let limits: Limits

    public struct Producer: Codable, Sendable, Equatable {
        public let sdkVersion: String
        public let platform: String
        public let collectionConfigurationId: String
    }
    public struct App: Codable, Sendable, Equatable {
        public let packageName: String
        public let versionName: String
        public let versionCode: Int64
        public let variant: String
        public let gitCommit: String?
    }
    public struct Device: Codable, Sendable, Equatable {
        public let apiLevel: Int
        public let manufacturer: String
        public let model: String
        public let windowWidthPx: Int
        public let windowHeightPx: Int
        public let densityDpi: Int
        public let locale: String
        public let fontScale: Double
        public let orientation: String
    }
    public struct Privacy: Codable, Sendable, Equatable {
        public let policyVersion: String
        public let redactionMode: String
        public let reviewedCategories: [String]
        public let previewContentHash: String?
    }
    public struct Capture: Codable, Sendable, Equatable {
        public let capturedAt: String
        public let collectionModes: [String]
        public let triggerOffsetMs: Int64?
        public let snapshotCutoffMs: Int64?
        public let providers: [Provider]
    }
    public struct Provider: Codable, Sendable, Equatable {
        public let providerId: String
        public let status: EvidenceStatus
    }
    public struct Entry: Codable, Sendable, Equatable {
        public let path: String
        public let mediaType: String
        public let uncompressedSize: Int64
        public let sha256: String
    }
    public struct Limits: Codable, Sendable, Equatable {
        public let maxCompressedBytes: Int64
        public let maxUncompressedBytes: Int64
        public let maxEntries: Int
        public let maxCompressionRatio: Int
        public let maxImageLongEdgePx: Int
        public let maxDecodedImageBytes: Int64
        public let maxConcurrentDecodedImageBytes: Int64
        public let maxJsonBytes: Int64
        public let maxJsonDepth: Int
        public let maxJsonNodes: Int
    }
}

public enum SchemaVersion {
    public static let supported = 1
    public static func isSupported(_ value: Int) -> Bool { value == supported }
}

public enum ReproParcelJSON {
    public static func encode<T: Encodable>(_ value: T) throws -> Data {
        let encoder = JSONEncoder()
        encoder.outputFormatting = [.sortedKeys]
        return try encoder.encode(value)
    }
    public static func decode<T: Decodable>(_ type: T.Type, from data: Data) throws -> T {
        try JSONDecoder().decode(type, from: data)
    }
}
