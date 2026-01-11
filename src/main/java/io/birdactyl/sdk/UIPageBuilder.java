package io.birdactyl.sdk;

import io.birdactyl.sdk.proto.PluginUIPage;

public class UIPageBuilder {
    private final UIBuilder ui;
    private final PluginUIPage.Builder page;

    UIPageBuilder(UIBuilder ui, String path, String component) {
        this.ui = ui;
        this.page = PluginUIPage.newBuilder()
                .setPath(path)
                .setComponent(component);
    }

    public UIPageBuilder title(String title) {
        page.setTitle(title);
        return this;
    }

    public UIPageBuilder icon(String icon) {
        page.setIcon(icon);
        return this;
    }

    public UIPageBuilder adminOnly() {
        page.setGuard("admin");
        return this;
    }

    public UIPageBuilder guard(String guard) {
        page.setGuard(guard);
        return this;
    }

    public UIBuilder done() {
        ui.addPage(page.build());
        return ui;
    }
}
