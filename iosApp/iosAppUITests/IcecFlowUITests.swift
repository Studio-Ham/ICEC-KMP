import XCTest

/// Full flow on a simulator: Home → Gallery → tool palette → Editor or basic edit → Result.
/// Needs a 3-face photo as the newest library item (`xcrun simctl addmedia`).
final class IcecFlowUITests: XCTestCase {
    private let app = XCUIApplication()
    private let springboard = XCUIApplication(bundleIdentifier: "com.apple.springboard")
    private static let preferred = ["Full", "All", "전체", "모든"]
    private static let forbidden = ["Don’t", "Don't", "허용 안 함", "Limit", "제한"]
    private static let fallback = ["Allow", "OK", "허용", "확인"]
    private static var savedCount = 0

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
        let home = element("Home")
        XCTAssertTrue(home.exists)
        home.tap()
        XCTAssertTrue(element("Load photo").waitForExistence(timeout: 5))
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

    /// Wiki 기본 편집: palette → basic edit → Result → 다시 편집하기 keeps the edit.
    func testBasicEditFlow() {
        let load = element("Load photo")
        XCTAssertTrue(load.waitForExistence(timeout: 20))
        load.tap()
        allowPhotos()
        let photos = app.descendants(matching: .any).matching(NSPredicate(format: "label BEGINSWITH 'Photo, '"))
        XCTAssertTrue(photos.firstMatch.waitForExistence(timeout: 20))
        photos.firstMatch.tap()
        let edit = element("Edit")
        XCTAssertTrue(edit.waitForExistence(timeout: 10))
        snap("06-palette")
        edit.tap()
        let done = element("Done")
        XCTAssertTrue(done.waitForExistence(timeout: 10))
        XCTAssertTrue(done.isEnabled) // D2: always enabled
        // `Done` waits for the photo (spec: input before it loads is ignored); the crop area appears with it.
        XCTAssertTrue(element("Crop area").waitForExistence(timeout: 30))
        snap("07-basic-edit")
        // D2: saving with no change still makes a new photo.
        done.tap()
        allowPhotos()
        XCTAssertTrue(element("Saved to your photo library").waitForExistence(timeout: 30))
        Self.savedCount += 1
        snap("07-basic-edit-unchanged-result")
        element("Edit again").tap()
        XCTAssertTrue(done.waitForExistence(timeout: 5))
        let crop = element("Crop area")
        crop.pinch(withScale: 2.5, velocity: 2) // two fingers zoom like the mosaic editor
        snap("08-basic-edit-zoomed")
        crop.swipeLeft() // one finger pans the zoomed photo, like the mosaic editor
        snap("09-basic-edit-panned")
        element("Rotate 90°").tap()
        element("Flip").tap()
        done.tap()
        allowPhotos()
        let editAgain = element("Edit again")
        XCTAssertTrue(editAgain.waitForExistence(timeout: 30))
        Self.savedCount += 1
        snap("10-basic-edit-result")
        editAgain.tap()
        XCTAssertTrue(element("Done").waitForExistence(timeout: 5))
    }

    func testRapidLeaveDoesNotCrash() {
        openEditor()
        XCTAssertTrue(element("0 of 3 faces selected").waitForExistence(timeout: 30))
        element("Back").tap()

        let leave = element("Leave")
        XCTAssertTrue(leave.waitForExistence(timeout: 5))
        let frame = leave.frame
        let leaveCenter = app.coordinate(withNormalizedOffset: .zero)
            .withOffset(CGVector(dx: frame.midX, dy: frame.midY))
        for _ in 0..<3 {
            leaveCenter.tap()
        }

        Thread.sleep(forTimeInterval: 1)
        XCTAssertEqual(app.state, .runningForeground)
        let photo = app.descendants(matching: .any)
            .matching(NSPredicate(format: "label BEGINSWITH 'Photo, '")).firstMatch
        // With a full gallery, the extra taps can land on a photo and open its tool palette.
        // Both mean the Editor closed without a crash (#43).
        XCTAssertTrue(photo.exists || element("Edit photo").exists)
        snap("gallery-after-rapid-leave")
    }

    func testResultBlocksSwipeBack() {
        openEditor()
        tapFace(1)
        save()

        app.coordinate(withNormalizedOffset: CGVector(dx: 0.01, dy: 0.5))
            .press(
                forDuration: 0.1,
                thenDragTo: app.coordinate(withNormalizedOffset: CGVector(dx: 0.9, dy: 0.5))
            )

        Thread.sleep(forTimeInterval: 1)
        XCTAssertTrue(element("Saved to your photo library").exists)
        let editAgain = element("Edit again")
        XCTAssertTrue(editAgain.exists)
        snap("result-after-swipe-back")
        editAgain.tap()
        XCTAssertTrue(element("1 of 3 faces selected").waitForExistence(timeout: 5))
    }

    private func openEditor() {
        let load = element("Load photo")
        XCTAssertTrue(load.waitForExistence(timeout: 20))
        snap("01-home")
        load.tap()
        allowPhotos()
        let photos = app.descendants(matching: .any)
            .matching(NSPredicate(format: "label BEGINSWITH 'Photo, '"))
        XCTAssertTrue(photos.firstMatch.waitForExistence(timeout: 20))
        let galleryPhotos = photos.allElementsBoundByIndex
        // The newest-first gallery lists saved results, then the fixture, then simulator samples.
        guard galleryPhotos.indices.contains(Self.savedCount) else {
            XCTFail("Expected a fixture photo in the gallery")
            return
        }
        snap("02-gallery")
        galleryPhotos[Self.savedCount].tap()
        let mosaic = element("Mosaic")
        XCTAssertTrue(mosaic.waitForExistence(timeout: 10))
        snap("03-palette")
        mosaic.tap()
        snap("03-editor")
    }

    private func save(name: String = "06-result") {
        element("Save").tap()
        allowPhotos()
        guard element("Saved to your photo library").waitForExistence(timeout: 30) else {
            XCTFail("Expected the photo to save successfully")
            return
        }
        Self.savedCount += 1
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
