import Foundation

/// Matches capitalisation and spacing to the text before the cursor. A port of ContextFit.kt.
enum ContextFit {
    private static let sentenceEnd = ".!?\n"
    private static let noSpaceBefore = ".,!?;:)]}\"'”’%"
    private static let noSpaceAfter = "([{“‘\"'/-@#\n\t "

    private static func lowerFirst(_ text: String, protected: Set<String>) -> String {
        let re = try! NSRegularExpression(pattern: "^[\\w'’]+")
        guard let m = re.firstMatch(in: text, range: NSRange(location: 0, length: (text as NSString).length)) else { return text }
        let word = (text as NSString).substring(with: m.range)
        let stem = word.split(separator: "'").first.map(String.init)?.split(separator: "’").first.map(String.init) ?? word
        if stem == "I" { return text }                                    // I, I'm, I'll
        if word.dropFirst().contains(where: { $0.isUppercase }) || protected.contains(word) { return text }  // NASA, iPhone
        return text.prefix(1).lowercased() + text.dropFirst()
    }

    /// `before == nil` means unknown (password field or no access): the text is returned unchanged.
    static func fit(_ text: String, before: String?, protected: Set<String> = []) -> String {
        guard !text.isEmpty, let before = before, !before.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { return text }
        var t = text
        let trimmed = before.trimmingCharacters(in: CharacterSet(charactersIn: " \t"))
        if let last = trimmed.last, sentenceEnd.contains(last) {
            // sentence start: keep the capital
        } else {
            t = lowerFirst(t, protected: protected)
        }
        if let lastChar = before.last, !noSpaceAfter.contains(lastChar), let first = t.first, !noSpaceBefore.contains(first) {
            t = " " + t
        }
        return t
    }
}
