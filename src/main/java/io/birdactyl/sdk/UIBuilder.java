package io.birdactyl.sdk;

import io.birdactyl.sdk.proto.*;
import java.util.ArrayList;
import java.util.List;

public class UIBuilder {
    public static final String TAB_TARGET_SERVER = "server";
    public static final String TAB_TARGET_USER_SETTINGS = "user-settings";

    public static final String SIDEBAR_SECTION_NAV = "nav";
    public static final String SIDEBAR_SECTION_PLATFORM = "platform";
    public static final String SIDEBAR_SECTION_ADMIN = "admin";

    private boolean hasBundle = false;
    private byte[] bundleData = null;
    private final List<PluginUIPage> pages = new ArrayList<>();
    private final List<PluginUITab> tabs = new ArrayList<>();
    private final List<PluginUISidebarItem> sidebarItems = new ArrayList<>();

    public UIBuilder() {}

    public UIBuilder hasBundle() {
        this.hasBundle = true;
        return this;
    }

    public UIBuilder bundleBytes(byte[] data) {
        this.bundleData = data;
        this.hasBundle = true;
        return this;
    }

    public UIBuilder embedBundle(String resourcePath) {
        try {
            java.io.InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath);
            if (is != null) {
                this.bundleData = is.readAllBytes();
                this.hasBundle = true;
                is.close();
            }
        } catch (Exception e) {
            System.err.println("[ui] Failed to embed bundle: " + e.getMessage());
        }
        return this;
    }

    public byte[] getBundleData() {
        return bundleData;
    }

    public UIPageBuilder page(String path, String component) {
        return new UIPageBuilder(this, path, component);
    }

    public UITabBuilder tab(String id, String component, String target, String label) {
        return new UITabBuilder(this, id, component, target, label);
    }

    public UISidebarBuilder sidebarItem(String id, String label, String href, String section) {
        return new UISidebarBuilder(this, id, label, href, section);
    }

    void addPage(PluginUIPage page) {
        pages.add(page);
    }

    void addTab(PluginUITab tab) {
        tabs.add(tab);
    }

    void addSidebarItem(PluginUISidebarItem item) {
        sidebarItems.add(item);
    }

    public PluginUIInfo build() {
        PluginUIInfo.Builder builder = PluginUIInfo.newBuilder()
                .setHasBundle(hasBundle)
                .addAllPages(pages)
                .addAllTabs(tabs)
                .addAllSidebarItems(sidebarItems);
        if (bundleData != null) {
            builder.setBundleData(com.google.protobuf.ByteString.copyFrom(bundleData));
        }
        return builder.build();
    }
}
