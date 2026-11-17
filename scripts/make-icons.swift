// Draws the OpenFlow icon art with CoreGraphics. Run on a Mac:
//   swift scripts/make-icons.swift <output folder> [ink|paper|lime]
// Writes: AppIcon.png (1024, for iOS), logo-512.png (README), social-preview.png (1280x640), options.png (colour preview).
import AppKit

struct Palette { let bg: NSColor; let bars: NSColor; let name: String }
func hex(_ v: UInt32) -> NSColor { NSColor(srgbRed: CGFloat((v >> 16) & 255) / 255, green: CGFloat((v >> 8) & 255) / 255, blue: CGFloat(v & 255) / 255, alpha: 1) }
let palettes: [String: Palette] = [
    "ink":   Palette(bg: hex(0x0B0B0F), bars: hex(0xFFFFFF), name: "Ink"),
    "paper": Palette(bg: hex(0xF5F5F7), bars: hex(0x0B0B0F), name: "Paper"),
    "lime":  Palette(bg: hex(0x0B0B0F), bars: hex(0xC6FF3D), name: "Ink + lime"),
]

let out = URL(fileURLWithPath: CommandLine.arguments.count > 1 ? CommandLine.arguments[1] : ".")
let choice = CommandLine.arguments.count > 2 ? CommandLine.arguments[2] : "ink"
let pal = palettes[choice] ?? palettes["ink"]!

func png(_ w: Int, _ h: Int, _ draw: (CGContext) -> Void) -> Data {
    let ctx = CGContext(data: nil, width: w, height: h, bitsPerComponent: 8, bytesPerRow: 0,
                        space: CGColorSpaceCreateDeviceRGB(), bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue)!
    draw(ctx)
    return NSBitmapImageRep(cgImage: ctx.makeImage()!).representation(using: .png, properties: [:])!
}

/// Five rounded voice bars, tallest in the middle, centred in a square of side `s` at origin `o`.
func bars(_ ctx: CGContext, origin o: CGPoint, side s: CGFloat, color: NSColor) {
    let k = s / 1024
    ctx.setFillColor(color.cgColor)
    for (x, h) in [(262, 280), (392, 480), (512, 640), (632, 480), (762, 280)] as [(CGFloat, CGFloat)] {
        let w = 76 * k
        let r = CGRect(x: o.x + (x - 38) * k, y: o.y + s / 2 - h * k / 2, width: w, height: h * k)
        ctx.addPath(CGPath(roundedRect: r, cornerWidth: w / 2, cornerHeight: w / 2, transform: nil)); ctx.fillPath()
    }
}

func icon(_ ctx: CGContext, side s: CGFloat, origin o: CGPoint = .zero, rounded: Bool, _ p: Palette) {
    if rounded { ctx.saveGState(); ctx.addPath(CGPath(roundedRect: CGRect(origin: o, size: CGSize(width: s, height: s)), cornerWidth: s * 0.225, cornerHeight: s * 0.225, transform: nil)); ctx.clip() }
    ctx.setFillColor(p.bg.cgColor); ctx.fill(CGRect(origin: o, size: CGSize(width: s, height: s)))
    bars(ctx, origin: o, side: s, color: p.bars)
    if rounded { ctx.restoreGState() }
}

func text(_ s: String, _ ctx: CGContext, at p: CGPoint, size: CGFloat, weight: NSFont.Weight, color: NSColor) {
    let a: [NSAttributedString.Key: Any] = [.font: NSFont.systemFont(ofSize: size, weight: weight), .foregroundColor: color]
    NSGraphicsContext.saveGraphicsState()
    NSGraphicsContext.current = NSGraphicsContext(cgContext: ctx, flipped: false)
    NSAttributedString(string: s, attributes: a).draw(at: p)
    NSGraphicsContext.restoreGraphicsState()
}

try! FileManager.default.createDirectory(at: out, withIntermediateDirectories: true)
try! png(1024, 1024) { icon($0, side: 1024, rounded: false, pal) }.write(to: out.appendingPathComponent("AppIcon.png"))
try! png(512, 512) { icon($0, side: 512, rounded: true, pal) }.write(to: out.appendingPathComponent("logo-512.png"))

try! png(1280, 640) { ctx in
    ctx.setFillColor(pal.bg.cgColor); ctx.fill(CGRect(x: 0, y: 0, width: 1280, height: 640))
    icon(ctx, side: 300, origin: CGPoint(x: 110, y: 170), rounded: true, Palette(bg: pal.bg, bars: pal.bars, name: ""))
    ctx.setStrokeColor(pal.bars.withAlphaComponent(0.25).cgColor); ctx.setLineWidth(3)
    ctx.addPath(CGPath(roundedRect: CGRect(x: 110, y: 170, width: 300, height: 300), cornerWidth: 67, cornerHeight: 67, transform: nil)); ctx.strokePath()
    text("OpenFlow", ctx, at: CGPoint(x: 470, y: 320), size: 124, weight: .bold, color: pal.bars)
    text("Talk. Tap ✓. It's typed.", ctx, at: CGPoint(x: 474, y: 250), size: 52, weight: .medium, color: pal.bars.withAlphaComponent(0.85))
    text("Free, open-source voice typing for Android and iPhone", ctx, at: CGPoint(x: 474, y: 190), size: 32, weight: .regular, color: pal.bars.withAlphaComponent(0.6))
}.write(to: out.appendingPathComponent("social-preview.png"))

try! png(1500, 560) { ctx in
    ctx.setFillColor(hex(0xE9E9EE).cgColor); ctx.fill(CGRect(x: 0, y: 0, width: 1500, height: 560))
    for (i, key) in ["ink", "paper", "lime"].enumerated() {
        let x = CGFloat(60 + i * 480)
        icon(ctx, side: 400, origin: CGPoint(x: x, y: 110), rounded: true, palettes[key]!)
        text(palettes[key]!.name, ctx, at: CGPoint(x: x + 140, y: 50), size: 36, weight: .semibold, color: hex(0x333340))
    }
}.write(to: out.appendingPathComponent("options.png"))
print("wrote icons for palette:", pal.name, "->", out.path)
