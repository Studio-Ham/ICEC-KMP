// swift-tools-version: 5.9
import PackageDescription
let package = Package(
  name: "KotlinMultiplatformLinkedPackage",
  platforms: [
    .iOS("15.0")
  ],
  products: [
    .library(
      name: "KotlinMultiplatformLinkedPackage",
      type: .none,
      targets: ["KotlinMultiplatformLinkedPackage"]
    )
  ],
  dependencies: [
    .package(path: "subpackages/_composeApp"),
    .package(path: "subpackages/_feature_editor"),
    .package(path: "subpackages/_platform_face-detection"),
    .package(path: "subpackages/_platform_face_detection")
  ],
  targets: [
    .target(
      name: "KotlinMultiplatformLinkedPackage",
      dependencies: [
        .product(name: "_composeApp", package: "_composeApp"),
        .product(name: "_feature_editor", package: "_feature_editor"),
        .product(name: "_platform_face-detection", package: "_platform_face-detection"),
        .product(name: "_platform_face_detection", package: "_platform_face_detection")
      ]
    )
  ]
)
