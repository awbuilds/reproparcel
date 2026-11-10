// swift-tools-version: 6.0
import PackageDescription

let package = Package(
    name: "ReproParcel",
    platforms: [.iOS(.v15), .macOS(.v13)],
    products: [
        .library(name: "ReproParcelCore", targets: ["ReproParcelCore"]),
    ],
    targets: [
        .target(name: "ReproParcelCore"),
        .testTarget(name: "ReproParcelCoreTests", dependencies: ["ReproParcelCore"]),
    ]
)
