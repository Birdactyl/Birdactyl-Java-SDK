package io.birdactyl.sdk;

import io.birdactyl.sdk.proto.PluginUISidebarChild;
import io.birdactyl.sdk.proto.PluginUISidebarItem;

public class UISidebarBuilder {
    private final UIBuilder ui;
    private final PluginUISidebarItem.Builder item;

    UISidebarBuilder(UIBuilder ui, String id, String label, String href, String section) {
        this.ui = ui;
        this.item = PluginUISidebarItem.newBuilder()
                .setId(id)
                .setLabel(label)
                .setHref(href)
                .setSection(section);
    }

    public UISidebarBuilder icon(String icon) {
        item.setIcon(icon);
        return this;
    }

    public UISidebarBuilder order(int order) {
        item.setOrder(order);
        return this;
    }

    public UISidebarBuilder adminOnly() {
        item.setGuard("admin");
        return this;
    }

    public UISidebarBuilder guard(String guard) {
        item.setGuard(guard);
        return this;
    }

    public UISidebarBuilder child(String label, String href) {
        item.addChildren(PluginUISidebarChild.newBuilder()
                .setLabel(label)
                .setHref(href)
                .build());
        return this;
    }

    public UIBuilder done() {
        ui.addSidebarItem(item.build());
        return ui;
    }
}
