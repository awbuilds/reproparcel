// SPDX-License-Identifier: Apache-2.0
import XCTest
@testable import ReproParcelCore

final class CoreTests: XCTestCase {
    func testImageBudget() {
        XCTAssertEqual(ImageBudget.scaledToLongestEdge(width: 3200, height: 1600), ImageDimensions(width: 1600, height: 800))
        XCTAssertFalse(ImageBudget.isWithinDecodedBudget(width: 4000, height: 4000))
        XCTAssertTrue(ImageBudget.isWithinDecodedBudget(width: 1600, height: 1600))
    }

    func testRedactorDropsSecretsAndKeepsAllowlist() {
        XCTAssertEqual(Redactor.sanitize(.screenId, "checkout"), "checkout")
        XCTAssertEqual(Redactor.sanitize(.routeTemplate, "/orders/{id}"), "/orders/{id}")
        XCTAssertNil(Redactor.sanitize(.screenId, "user@example.com"))
        XCTAssertNil(Redactor.sanitize(.routeTemplate, "/orders?id=secret"))
        XCTAssertNil(Redactor.sanitize(.httpMethod, "TRACE"))
        let safe = Redactor.sanitizeAttributes(["screen_id": "home", "password": "hunter2"])
        XCTAssertEqual(safe, ["screen_id": "home"])
    }

    func testSessionStateMachine() {
        var machine = SessionStateMachine()
        XCTAssertTrue(machine.transition(.enable))
        XCTAssertTrue(machine.transition(.startCapture))
        XCTAssertTrue(machine.transition(.captureSucceeded))
        XCTAssertTrue(machine.transition(.review))
        XCTAssertTrue(machine.transition(.finalize))
        XCTAssertTrue(machine.transition(.finalizeSucceeded))
        XCTAssertEqual(machine.state, .exportable)
        XCTAssertTrue(machine.transition(.saveSucceeded))
        XCTAssertEqual(machine.state, .completed)
        XCTAssertFalse(machine.transition(.saveSucceeded))
    }

    func testManifestRoundTripAndUnknownOptionalFields() throws {
        let json = """
        {"schemaVersion":1,"reportId":"rpt_ios","createdAt":"2026-11-11T00:00:00Z",
         "producer":{"sdkVersion":"1","platform":"ios","collectionConfigurationId":"c"},
         "app":{"packageName":"com.example.synthetic","versionName":"1","versionCode":1,"variant":"debug","gitCommit":null},
         "device":{"apiLevel":1,"manufacturer":"Apple","model":"Synthetic","windowWidthPx":100,
                   "windowHeightPx":100,"densityDpi":1,"locale":"en-US","fontScale":1.0,"orientation":"portrait"},
         "privacy":{"policyVersion":"p","redactionMode":"opaque","reviewedCategories":[],"previewContentHash":null},
         "capture":{"capturedAt":"2026-11-11T00:00:01Z","collectionModes":[],"triggerOffsetMs":null,"snapshotCutoffMs":null,"providers":[]},
         "entries":[],"limits":{"maxCompressedBytes":1,"maxUncompressedBytes":1,"maxEntries":1,
         "maxCompressionRatio":1,"maxImageLongEdgePx":1,"maxDecodedImageBytes":1,
         "maxConcurrentDecodedImageBytes":1,"maxJsonBytes":1,"maxJsonDepth":1,"maxJsonNodes":1},
         "futureOptionalField":true}
        """.data(using: .utf8)!
        let manifest = try ReproParcelJSON.decode(Manifest.self, from: json)
        XCTAssertEqual(manifest.reportId, "rpt_ios")
        XCTAssertTrue(SchemaVersion.isSupported(manifest.schemaVersion))
        let encoded = try ReproParcelJSON.encode(manifest)
        let decoded = try ReproParcelJSON.decode(Manifest.self, from: encoded)
        XCTAssertEqual(decoded.reportId, "rpt_ios")
    }

    func testReportRoundTrip() throws {
        let report = Report(description: "Synthetic", evidence: [EvidenceStatus(state: "collected")])
        let data = try ReproParcelJSON.encode(report)
        let decoded = try ReproParcelJSON.decode(Report.self, from: data)
        XCTAssertEqual(decoded.description, "Synthetic")
        XCTAssertEqual(decoded.evidence.first?.state, "collected")
    }
}
