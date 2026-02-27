package io.birdactyl.sdk;

import io.birdactyl.sdk.proto.PluginUISidebarChild;
import io.birdactyl.sdk.proto.PluginUISidebarItem;

/**
 * Builder for a navigation item displayed in the top bar.
 * Replaces the old UISidebarBuilder (which is now a deprecated alias for this
 * class).
 */
public class UINavItemBuilder {
    private final UIBuilder ui;
    private final PluginUISidebarItem.Builder item;

    UINavItemBuilder(UIBuilder ui, String id, String label, String href, String section) {
        this.ui = ui;
        this.item = PluginUISidebarItem.newBuilder()
                .setId(id)
                .setLabel(label)
                .setHref(href)
                .setSection(section);
    }

    public UINavItemBuilder icon(String icon) {
        item.setIcon(icon);
        return this;
    }

    public UINavItemBuilder order(int order) {
        item.setOrder(order);
        return this;
    }

    public UINavItemBuilder adminOnly() {
        item.setGuard("admin");
        return this;
    }

    public UINavItemBuilder guard(String guard) {
        item.setGuard(guard);
        return this;
    }

    public UINavItemBuilder child(String label, String href) {
        item.addChildren(PluginUISidebarChild.newBuilder()
                .setLabel(label)
                .setHref(href)
                .build());
        return this;
    }

    public UIBuilder done() {
        ui.addNavItem(item.build());
        return ui;
    }
}
