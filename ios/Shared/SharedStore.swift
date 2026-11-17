import Foundation

/// Data shared between the OpenFlow app and its keyboard through the App Group container.
enum SharedStore {
    static let suite = UserDefaults(suiteName: "group.com.openflow.ios") ?? .standard
    private static let resultKey = "lastResult"
    private static let resultAtKey = "lastResultAt"

    static var spoken: Bool {
        get { suite.object(forKey: "spoken") as? Bool ?? true }
        set { suite.set(newValue, forKey: "spoken") }
    }

    static var dictionaryText: String {
        get { suite.string(forKey: "dictionary") ?? "open flow = OpenFlow" }
        set { suite.set(newValue, forKey: "dictionary") }
    }

    /// One "heard = written" pair per line. Blank or malformed lines are skipped.
    static func parseMap(_ text: String) -> [String: String] {
        var out: [String: String] = [:]
        for line in text.components(separatedBy: .newlines) {
            guard let i = line.firstIndex(of: "=") else { continue }
            let k = line[..<i].trimmingCharacters(in: .whitespaces)
            let v = line[line.index(after: i)...].trimmingCharacters(in: .whitespaces)
            if !k.isEmpty { out[k] = v }
        }
        return out
    }

    static var dictionary: [String: String] { parseMap(dictionaryText) }

    /// The keyboard reads this once, shortly after you come back from OpenFlow.
    static func saveResult(_ text: String) {
        suite.set(text, forKey: resultKey)
        suite.set(Date().timeIntervalSince1970, forKey: resultAtKey)
    }

    static func takeResult(maxAge: TimeInterval = 120) -> String? {
        guard let text = suite.string(forKey: resultKey) else { return nil }
        let at = suite.double(forKey: resultAtKey)
        suite.removeObject(forKey: resultKey)
        suite.removeObject(forKey: resultAtKey)
        guard Date().timeIntervalSince1970 - at <= maxAge, !text.isEmpty else { return nil }
        return text
    }
}
