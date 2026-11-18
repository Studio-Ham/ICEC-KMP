import XCTest

/// Full flow on a simulator: Home → Gallery → Editor → Result.
/// Needs a 3-face photo as the newest library item (`xcrun simctl addmedia`).
final class IcecFlowUITests: XCTestCase {
    private let app = XCUIApplication()
    private let springboard = XCUIApplication(bundleIdentifier: "com.apple.springboard")
    private let allowLabels = [
        "Allow Full Access", "Allow Access to All Photos", "Allow Access to All",
        "Allow Full Library Access", "Allow", "OK",
    ]

    override func setUp() {
        continueAfterFailure = false
        app.launchArguments += ["-AppleLanguages", "(en)", "-AppleLocale", "en_US"]
        addUIInterruptionMonitor(withDescription: "Photos permission") { [allowLabels] alert in
            for label in allowLabels where alert.buttons[label].exists {
                alert.buttons[label].tap()
                return true
            }
            return false
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

    /// Taps the in-app "Allow access" button if shown, then the system alert.
    private func allowPhotos() {
        let inApp = element("Allow access")
        if inApp.waitForExistence(timeout: 3) { inApp.tap() }
        for label in allowLabels {
            let button = springboard.buttons[label]
            if button.waitForExistence(timeout: label == allowLabels[0] ? 3 : 0.5) {
                button.tap()
                break
            }
        }
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
