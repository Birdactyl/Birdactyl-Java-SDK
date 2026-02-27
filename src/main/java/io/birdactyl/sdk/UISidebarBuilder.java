package io.birdactyl.sdk;

/**
 * @deprecated Use {@link UINavItemBuilder} instead.
 *             This class is kept for backward compatibility only.
 *             Navigation items are now displayed in the top bar, not a sidebar.
 */
@Deprecated
public class UISidebarBuilder extends UINavItemBuilder {
    UISidebarBuilder(UIBuilder ui, String id, String label, String href, String section) {
        super(ui, id, label, href, section);
    }
}
