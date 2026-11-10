// SPDX-License-Identifier: AGPL-3.0-only
import Foundation

public enum SafeAttribute: String, CaseIterable, Sendable {
    case screenId = "screen_id"
    case actionId = "action_id"
    case routeTemplate = "route_template"
    case httpMethod = "http_method"
    case statusCode = "status_code"
    case durationMs = "duration_ms"
    case result
    case category
    case component
    case errorCode = "error_code"
    case orientation
    case density
    case fontScale = "font_scale"
    case apiLevel = "api_level"
    case manufacturer
    case model
    case appVersion = "app_version"
    case variant
    case gitCommit = "git_commit"
}

public enum Redactor {
    private static let email = try! NSRegularExpression(pattern: "[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}")
    private static let bearer = try! NSRegularExpression(pattern: "(?i)\\b(bearer|basic)\\s+[A-Za-z0-9._~+/=-]+")
    private static let url = try! NSRegularExpression(pattern: "(?i)(https?|ftp)://|\\?|#")
    private static let longDigits = try! NSRegularExpression(pattern: "\\d{7,}")
    private static let route = try! NSRegularExpression(pattern: "^/[A-Za-z0-9_./{}:-]{0,120}$")

    public static func sanitize(_ attribute: SafeAttribute, _ value: String) -> String? {
        let trimmed = value.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty, trimmed.count <= 256 else { return nil }
        let range = NSRange(trimmed.startIndex..<trimmed.endIndex, in: trimmed)
        if email.firstMatch(in: trimmed, range: range) != nil { return nil }
        if bearer.firstMatch(in: trimmed, range: range) != nil { return nil }
        if longDigits.firstMatch(in: trimmed, range: range) != nil { return nil }
        if trimmed.contains("..") { return nil }

        switch attribute {
        case .routeTemplate:
            guard route.firstMatch(in: trimmed, range: range) != nil else { return nil }
            guard url.firstMatch(in: trimmed, range: range) == nil else { return nil }
            return trimmed
        case .httpMethod:
            let method = trimmed.uppercased()
            return ["GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS"].contains(method) ? method : nil
        case .statusCode, .durationMs, .density, .apiLevel:
            return Int64(trimmed) == nil ? nil : trimmed
        case .fontScale:
            guard let value = Double(trimmed), (0.5...4.0).contains(value) else { return nil }
            return trimmed
        case .orientation:
            return ["portrait", "landscape", "unknown"].contains(trimmed) ? trimmed : nil
        case .variant:
            return trimmed.range(of: "^[A-Za-z0-9_.-]{1,32}$", options: .regularExpression) != nil ? trimmed : nil
        case .gitCommit:
            return trimmed.range(of: "^[0-9a-f]{7,64}$", options: .regularExpression) != nil ? trimmed : nil
        default:
            guard url.firstMatch(in: trimmed, range: range) == nil else { return nil }
            return trimmed.count <= 120 ? trimmed : nil
        }
    }

    public static func sanitizeAttributes(_ raw: [String: String]) -> [String: String] {
        var result: [String: String] = [:]
        for (key, value) in raw {
            guard let attribute = SafeAttribute(rawValue: key) else { continue }
            if let safe = sanitize(attribute, value) { result[key] = safe }
        }
        return result
    }
}

public enum SessionState: String, Codable, Sendable {
    case disabled, ready, capturing, editing, reviewing, finalizing, exportable
    case completed, cancelled, exportFailed = "export_failed", shareLaunched = "share_launched"
}

public enum SessionEvent: Sendable {
    case enable, disable, startCapture, captureSucceeded, captureOmitted
    case review, finalize, finalizeSucceeded, finalizeFailed
    case saveSucceeded, saveCancelled, shareLaunched, exportFailed, hostDestroyed, reset
}

public struct SessionStateMachine: Sendable {
    public private(set) var state: SessionState
    public init(state: SessionState = .disabled) { self.state = state }

    public mutating func transition(_ event: SessionEvent) -> Bool {
        if event == .disable { state = .disabled; return true }
        if event == .hostDestroyed {
            state = [.capturing, .editing, .reviewing, .finalizing, .exportable].contains(state) ? .cancelled : .disabled
            return true
        }
        let next: SessionState?
        switch (state, event) {
        case (.disabled, .enable): next = .ready
        case (.ready, .startCapture): next = .capturing
        case (.capturing, .captureSucceeded), (.capturing, .captureOmitted): next = .editing
        case (.editing, .review): next = .reviewing
        case (.reviewing, .finalize): next = .finalizing
        case (.finalizing, .finalizeSucceeded): next = .exportable
        case (.finalizing, .finalizeFailed): next = .exportFailed
        case (.exportable, .saveSucceeded): next = .completed
        case (.exportable, .saveCancelled): next = .cancelled
        case (.exportable, .shareLaunched): next = .shareLaunched
        case (.exportable, .exportFailed): next = .exportFailed
        case (.completed, .reset), (.cancelled, .reset), (.exportFailed, .reset), (.shareLaunched, .reset): next = .ready
        default: next = nil
        }
        guard let next else { return false }
        state = next
        return true
    }
}
