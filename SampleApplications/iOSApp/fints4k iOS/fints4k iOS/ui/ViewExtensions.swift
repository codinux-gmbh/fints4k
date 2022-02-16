import SwiftUI



extension Color {
    static let lightText = Color(UIColor.lightText)
    static let darkText = Color(UIColor.darkText)

    static let label = Color(UIColor.label)
    static let secondaryLabel = Color(UIColor.secondaryLabel)
    static let tertiaryLabel = Color(UIColor.tertiaryLabel)
    static let quaternaryLabel = Color(UIColor.quaternaryLabel)
    
    static let link = Color(UIColor.link)

    static let systemBackground = Color(UIColor.systemBackground)
    static let secondarySystemBackground = Color(UIColor.secondarySystemBackground)
    static let tertiarySystemBackground = Color(UIColor.tertiarySystemBackground)

    static let systemGroupedBackground = Color(UIColor.systemGroupedBackground)

    // There are more..

    static var destructive: Color {
        if UIColor.responds(to: Selector(("_systemDestructiveTintColor"))) {
            if let red = UIColor.perform(Selector(("_systemDestructiveTintColor")))?.takeUnretainedValue() as? UIColor {
                return Color(red)
            }
        }

        return Color.red
    }
}


extension View {
    
    func makeBackgroundTapable() -> some View {
        return self.background(Color.systemBackground)
    }


    func detailForegroundColor() -> some View {
        return self
            .foregroundColor(Color.secondary)
    }

    func detailFont() -> some View {
        return self
            .font(.callout)
    }

    func styleAsDetail() -> some View {
        return self
            .detailFont()
            .detailForegroundColor()
    }
    
}
