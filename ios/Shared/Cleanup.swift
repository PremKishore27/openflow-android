import Foundation

/// Rule-based cleanup. A port of Cleanup.kt (and the Mac app's cleanup.py), so every platform cleans text the same way.
enum Cleanup {
    private static let spokenPairs: [(String, String)] = [
        ("new paragraph", "\n\n"), ("new line", "\n"), ("newline", "\n"),
        ("question mark", "?"), ("exclamation mark", "!"), ("exclamation point", "!"),
        ("full stop", "."), ("period", "."), ("comma", ","), ("colon", ":"), ("semicolon", ";"),
    ]

    private static func rx(_ pattern: String, caseInsensitive: Bool = false) -> NSRegularExpression {
        // Patterns are fixed in this file, so a failure here is a programming error.
        try! NSRegularExpression(pattern: pattern, options: caseInsensitive ? [.caseInsensitive] : [])
    }

    /// Rebuilds `s` by calling `transform` for every match, which returns the replacement text.
    private static func mapMatches(_ s: String, _ re: NSRegularExpression,
                                   _ transform: (NSTextCheckingResult, NSString) -> String) -> String {
        let ns = s as NSString
        var out = ""
        var last = 0
        for m in re.matches(in: s, range: NSRange(location: 0, length: ns.length)) {
            out += ns.substring(with: NSRange(location: last, length: m.range.location - last))
            out += transform(m, ns)
            last = m.range.location + m.range.length
        }
        out += ns.substring(from: last)
        return out
    }

    static func spokenPunctuation(_ text: String) -> String {
        var t = text
        for (word, rep) in spokenPairs {
            let re = rx("([,.?!]?)\\s*\\b" + NSRegularExpression.escapedPattern(for: word) + "\\b[,.?!]?", caseInsensitive: true)
            t = mapMatches(t, re) { m, ns in
                if rep.hasPrefix("\n") {
                    let lead = m.range(at: 1).length > 0 ? ns.substring(with: m.range(at: 1)) : ""
                    let keep = (lead == "." || lead == "?" || lead == "!") ? lead : ""
                    return keep + rep
                }
                return rep
            }
        }
        return t
    }

    /// "Meet at 3. Scratch that. Meet at 4." -> "Meet at 4."
    static func scratchThat(_ text: String) -> String {
        let re = rx("[,.]?\\s*\\b(?:scratch that|delete that|forget that)\\b[,.]?\\s*", caseInsensitive: true)
        var t = text
        while let m = re.firstMatch(in: t, range: NSRange(location: 0, length: (t as NSString).length)) {
            let ns = t as NSString
            let before = ns.substring(to: m.range.location)
            let after = ns.substring(from: m.range.location + m.range.length)
            let kept: String
            if let i = before.lastIndex(where: { ".!?\n".contains($0) }) {
                kept = String(before[...i])
            } else {
                kept = ""
            }
            t = (kept + " " + after).trimmingCharacters(in: .whitespaces)
        }
        return t
    }

    /// "Send it to John, no wait, to Mike." -> "Send it to Mike."
    static func selfCorrect(_ text: String) -> String {
        let correct = rx("[,;]?\\s*\\b(?:no,? wait|i mean|actually no)\\b[,.]?\\s*", caseInsensitive: true)
        let firstBreak = rx("^[^,.!?;\\n]+")
        var t = text
        while let m = correct.firstMatch(in: t, range: NSRange(location: 0, length: (t as NSString).length)) {
            let ns = t as NSString
            let before = ns.substring(to: m.range.location).trimmingCharacters(in: .whitespaces)
            let after = ns.substring(from: m.range.location + m.range.length)
            guard let mm = firstBreak.firstMatch(in: after, range: NSRange(location: 0, length: (after as NSString).length)) else {
                t = before + " " + after
                continue
            }
            let breakText = (after as NSString).substring(with: mm.range)
            let n = breakText.split(whereSeparator: { $0 == " " || $0 == "\t" || $0 == "\n" }).count
            var words = before.components(separatedBy: " ")
            words[words.count - 1] = words[words.count - 1].trimmingCharacters(in: CharacterSet(charactersIn: ",;"))
            let kept = n < words.count ? Array(words[0..<(words.count - n)]) : []
            t = (kept.joined(separator: " ") + " " + after).trimmingCharacters(in: .whitespacesAndNewlines)
        }
        return t
    }

    static func removeFillers(_ text: String) -> String {
        let re = rx("\\b(?:um+|uh+|uhm+|erm+|er|ah+|hmm+|mmm+)\\b[,.]?\\s*", caseInsensitive: true)
        return re.stringByReplacingMatches(in: text, range: NSRange(location: 0, length: (text as NSString).length), withTemplate: "")
    }

    static func removeStutters(_ text: String) -> String {
        let re = rx("\\b(\\w+)(?:\\s+\\1\\b)+", caseInsensitive: true)
        return mapMatches(text, re) { m, ns in
            let word = ns.substring(with: m.range(at: 1))
            let exempt = ["had", "that"].contains(word.lowercased()) || word.allSatisfy { $0.isNumber }
            return exempt ? ns.substring(with: m.range) : word
        }
    }

    static func tidy(_ text: String, protectedWords: Set<String> = []) -> String {
        var t = text
        func sub(_ pattern: String, _ template: String) {
            t = rx(pattern).stringByReplacingMatches(in: t, range: NSRange(location: 0, length: (t as NSString).length), withTemplate: template)
        }
        sub("[ \\t]+", " ")
        sub("[ \\t]*\\n[ \\t]*", "\n")
        sub("[ \\t]+([,.!?;:])", "$1")
        sub("([,.!?;:])\\1+", "$1")
        sub("^[,.;:\\s]+", "")
        sub("\\bi\\b(?!\\.)", "I")
        let start = rx("(^|[.!?]\\s+|\\n)([a-z][\\w']*)")
        t = mapMatches(t, start) { m, ns in
            let lead = ns.substring(with: m.range(at: 1))
            let word = ns.substring(with: m.range(at: 2))
            if protectedWords.contains(word) { return ns.substring(with: m.range) }
            return lead + word.prefix(1).uppercased() + word.dropFirst()
        }
        return t.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    private static func replacePhrases(_ text: String, _ mapping: [String: String]) -> String {
        var t = text
        for src in mapping.keys.sorted(by: { $0.count > $1.count }) {
            let dst = NSRegularExpression.escapedTemplate(for: mapping[src] ?? "")
            let re = rx("\\b" + NSRegularExpression.escapedPattern(for: src) + "\\b", caseInsensitive: true)
            t = re.stringByReplacingMatches(in: t, range: NSRange(location: 0, length: (t as NSString).length), withTemplate: dst)
        }
        return t
    }

    static func applyStyle(_ text: String, style: String) -> String {
        let sentenceEnds = rx("[.!?]").numberOfMatches(in: text, range: NSRange(location: 0, length: (text as NSString).length))
        if style == "casual" && !text.contains("\n") && sentenceEnds <= 1 && text.hasSuffix(".") {
            return String(text.dropLast())
        }
        return text
    }

    static func clean(_ text: String, dictionary: [String: String] = [:], snippets: [String: String] = [:],
                      style: String = "default", spoken: Bool = true) -> String {
        var t = text
        if spoken { t = spokenPunctuation(t) }
        t = scratchThat(t)
        t = selfCorrect(t)
        t = removeFillers(t)
        t = removeStutters(t)
        let protected = Set((Array(dictionary.values) + Array(snippets.values))
            .filter { !$0.trimmingCharacters(in: .whitespaces).isEmpty }
            .compactMap { $0.split(separator: " ").first.map(String.init) })
        t = tidy(t, protectedWords: protected)
        t = replacePhrases(t, dictionary)
        t = replacePhrases(t, snippets)
        return applyStyle(t, style: style)
    }
}
