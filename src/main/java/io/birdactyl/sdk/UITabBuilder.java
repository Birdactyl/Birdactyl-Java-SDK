package io.birdactyl.sdk;

import io.birdactyl.sdk.proto.PluginUITab;

public class UITabBuilder {
    private final UIBuilder ui;
    private final PluginUITab.Builder tab;

    UITabBuilder(UIBuilder ui, String id, String component, String target, String label) {
        this.ui = ui;
        this.tab = PluginUITab.newBuilder()
                .setId(id)
                .setComponent(component)
                .setTarget(target)
                .setLabel(label);
    }

    public UITabBuilder icon(String icon) {
        tab.setIcon(icon);
        return this;
    }

    public UITabBuilder order(int order) {
        tab.setOrder(order);
        return this;
    }

    public UIBuilder done() {
        ui.addTab(tab.build());
        return ui;
    }
}
