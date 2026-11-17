// swift-tools-version: 5.9
import PackageDescription
let package = Package(
  name: "_platform_face-detection",
  platforms: [
    .iOS("15.0")
  ],
  products: [
    .library(
      name: "_platform_face-detection",
      type: .none,
      targets: ["_platform_face-detection"]
    )
  ],
  dependencies: [
    .package(
      url: "https://github.com/d-date/google-mlkit-swiftpm.git",
      exact: "9.0.3"
    )
  ],
  targets: [
    .target(
      name: "_platform_face-detection",
      dependencies: [
        .product(
          name: "MLKitFaceDetection",
          package: "google-mlkit-swiftpm"
        )
      ]
    )
  ]
)
