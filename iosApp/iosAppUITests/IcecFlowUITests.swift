import XCTest

/// Full flow on a simulator: Home → Gallery → Editor → Result.
/// Needs a 3-face photo as the newest library item (`xcrun simctl addmedia`).
final class IcecFlowUITests: XCTestCase {
    private let app = XCUIApplication()
    private let springboard = XCUIApplication(bundleIdentifier: "com.apple.springboard")
    private static let preferred = ["Full", "All", "전체", "모든"]
    private static let forbidden = ["Don’t", "Don't", "허용 안 함", "Limit", "제한"]
    private static let fallback = ["Allow", "OK", "허용", "확인"]

    /// Taps the full-access button of a permission alert. Never taps deny or limit.
    @discardableResult
    private static func allow(_ alert: XCUIElement) -> Bool {
        let buttons = alert.buttons.allElementsBoundByIndex.filter { button in
            !forbidden.contains { button.label.contains($0) }
        }
        let target = buttons.first { button in preferred.contains { button.label.contains($0) } }
            ?? buttons.first { fallback.contains($0.label) }
        target?.tap()
        return target != nil
    }

    override func setUp() {
        continueAfterFailure = false
        app.launchArguments += ["-AppleLanguages", "(en)", "-AppleLocale", "en_US"]
        addUIInterruptionMonitor(withDescription: "Photos permission") { alert in
            Self.allow(alert)
        }
        app.launch()
    }

    func testFullFlow() {
        openEditor()
        XCTAssertTrue(element("0 of 3 faces selected").waitForExistence(timeout: 30))
        snap("04-editor-analyzed")
        tapFace(1)
        XCTAssertTrue(element("1 of 3 faces selected").waitForExistence(timeout: 5))
        snap("05-editor-selected")
        save()
        XCTAssertTrue(element("Edit again").exists)
    }

    func testStoreScreenshots() {
        openEditor()
        XCTAssertTrue(element("of 3 faces selected", contains: true).waitForExistence(timeout: 30))
        tapFace(1)
        tapFace(2)
        XCTAssertTrue(element("2 of 3 faces selected").waitForExistence(timeout: 5))
        snap("store-03-editor")
        save(name: "store-04-result")
    }

    private func openEditor() {
        let load = element("Load photo")
        XCTAssertTrue(load.waitForExistence(timeout: 20))
        snap("01-home")
        load.tap()
        allowPhotos()
        let firstPhoto = app.descendants(matching: .any)
            .matching(NSPredicate(format: "label BEGINSWITH 'Photo, '")).firstMatch
        XCTAssertTrue(firstPhoto.waitForExistence(timeout: 20))
        snap("02-gallery")
        firstPhoto.tap()
        snap("03-editor")
    }

    private func save(name: String = "06-result") {
        element("Save").tap()
        allowPhotos()
        XCTAssertTrue(element("Saved!").waitForExistence(timeout: 30))
        snap(name)
    }

    private func tapFace(_ n: Int) {
        let face = element("Face \(n)")
        XCTAssertTrue(face.waitForExistence(timeout: 5))
        face.tap()
    }

    /// Taps the in-app "Allow access" button if shown, then the system alert directly.
    private func allowPhotos() {
        let inApp = element("Allow access")
        if inApp.waitForExistence(timeout: 3) { inApp.tap() }
        let alert = springboard.alerts.firstMatch
        if alert.waitForExistence(timeout: 5) { Self.allow(alert) }
    }

    private func element(_ label: String, contains: Bool = false) -> XCUIElement {
        let op = contains ? "CONTAINS" : "=="
        return app.descendants(matching: .any)
            .matching(NSPredicate(format: "label \(op) %@", label)).firstMatch
    }

    private func snap(_ name: String) {
        let shot = XCTAttachment(screenshot: XCUIScreen.main.screenshot())
        shot.name = name
        shot.lifetime = .keepAlways
        add(shot)
    }
}
