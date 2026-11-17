import SwiftUI

enum Theme {
    static let indigo = Color(red: 0.37, green: 0.36, blue: 0.90)
    static let blue = Color(red: 0.04, green: 0.52, blue: 1.0)
    static let gradient = LinearGradient(colors: [indigo, blue], startPoint: .topLeading, endPoint: .bottomTrailing)
}

/// The OpenFlow logo: five white voice bars on a flat near-black circle. Same shape as the app icon.
struct LogoMark: View {
    var size: CGFloat = 80
    private let heights: [CGFloat] = [0.175, 0.325, 0.5, 0.325, 0.175]

    var body: some View {
        ZStack {
            Circle().fill(Color(red: 0.043, green: 0.043, blue: 0.059))
            Circle().strokeBorder(Color.white.opacity(0.18), lineWidth: max(1, size / 48))
            HStack(spacing: size * 0.07) {
                ForEach(0..<5, id: \.self) { i in
                    Capsule().fill(.white).frame(width: size * 0.075, height: size * heights[i])
                }
            }
        }
        .frame(width: size, height: size)
        .accessibilityHidden(true)
    }
}

/// A soft rounded card used by all the infographics.
struct Card<Content: View>: View {
    @ViewBuilder var content: Content
    var body: some View {
        content
            .padding(18)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Color(.secondarySystemGroupedBackground), in: RoundedRectangle(cornerRadius: 24, style: .continuous))
            .shadow(color: .black.opacity(0.08), radius: 16, y: 6)
    }
}

struct PrimaryButtonStyle: ButtonStyle {
    var enabled = true
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.headline)
            .foregroundStyle(.white)
            .frame(maxWidth: .infinity)
            .frame(height: 56)
            .background(enabled ? AnyShapeStyle(Theme.gradient) : AnyShapeStyle(Color.gray.opacity(0.4)),
                        in: RoundedRectangle(cornerRadius: 18, style: .continuous))
            .scaleEffect(configuration.isPressed ? 0.98 : 1)
            .animation(.easeOut(duration: 0.12), value: configuration.isPressed)
    }
}
