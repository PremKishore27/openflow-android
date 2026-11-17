import Foundation

/// Checks GitHub for a newer release. Sends nothing about you: one public, anonymous GET request.
/// Behaves like the Android app's update check, so both stay in sync.
enum Updates {
    static let repo = "PremKishore27/openflow-android"
    static let releasesPage = URL(string: "https://github.com/\(repo)/releases/latest")!

    struct Info { let tag: String; let notes: String; let pageURL: URL }

    enum Result {
        case newer(Info)
        case upToDate
        case failed(String)
    }

    /// "v0.10.0" -> [0, 10, 0]. Anything after the digits of a part (like "-beta") is ignored.
    static func parse(_ version: String) -> [Int] {
        var v = version.trimmingCharacters(in: .whitespaces)
        if v.hasPrefix("v") { v.removeFirst() }
        return v.split(separator: ".", omittingEmptySubsequences: false).map { Int($0.prefix(while: \.isNumber)) ?? 0 }
    }

    static func isNewer(_ latest: String, than current: String) -> Bool {
        let a = parse(latest), b = parse(current)
        for i in 0..<max(a.count, b.count) {
            let x = i < a.count ? a[i] : 0
            let y = i < b.count ? b[i] : 0
            if x != y { return x > y }
        }
        return false
    }

    /// "v0.10.0" -> "0.10.0" (only a leading "v" is removed).
    static func display(_ tag: String) -> String { tag.hasPrefix("v") ? String(tag.dropFirst()) : tag }

    static var installedVersion: String {
        Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "0"
    }

    static func check(current: String = installedVersion) async -> Result {
        guard let url = URL(string: "https://api.github.com/repos/\(repo)/releases/latest") else { return .failed("Bad address") }
        var request = URLRequest(url: url, timeoutInterval: 15)
        request.setValue("application/vnd.github+json", forHTTPHeaderField: "Accept")
        request.setValue("OpenFlow-update-check", forHTTPHeaderField: "User-Agent")
        do {
            let (data, response) = try await URLSession.shared.data(for: request)
            if let http = response as? HTTPURLResponse, http.statusCode != 200 {
                return .failed("GitHub answered \(http.statusCode)")
            }
            guard let json = try JSONSerialization.jsonObject(with: data) as? [String: Any],
                  let tag = json["tag_name"] as? String, !tag.isEmpty else { return .failed("No release found") }
            if !isNewer(tag, than: current) { return .upToDate }
            let page = (json["html_url"] as? String).flatMap(URL.init(string:)) ?? releasesPage
            return .newer(Info(tag: tag, notes: (json["body"] as? String) ?? "", pageURL: page))
        } catch {
            return .failed(error.localizedDescription)
        }
    }
}
