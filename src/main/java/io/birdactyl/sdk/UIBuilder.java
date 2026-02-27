package io.birdactyl.sdk;

import io.birdactyl.sdk.proto.*;
import java.util.ArrayList;
import java.util.List;

public class UIBuilder {
    public static final String TAB_TARGET_SERVER = "server";
    public static final String TAB_TARGET_USER_SETTINGS = "user-settings";

    /**
     * Section constants for navItem(). Items in the "nav" section appear as main
     * navigation tabs.
     */
    public static final String NAV_SECTION_NAV = "nav";
    public static final String NAV_SECTION_PLATFORM = "platform";
    public static final String NAV_SECTION_ADMIN = "admin";

    /** @deprecated Use NAV_SECTION_NAV instead. */
    @Deprecated
    public static final String SIDEBAR_SECTION_NAV = NAV_SECTION_NAV;
    /** @deprecated Use NAV_SECTION_PLATFORM instead. */
    @Deprecated
    public static final String SIDEBAR_SECTION_PLATFORM = NAV_SECTION_PLATFORM;
    /** @deprecated Use NAV_SECTION_ADMIN instead. */
    @Deprecated
    public static final String SIDEBAR_SECTION_ADMIN = NAV_SECTION_ADMIN;

    private boolean hasBundle = false;
    private byte[] bundleData = null;
    private final List<PluginUIPage> pages = new ArrayList<>();
    private final List<PluginUITab> tabs = new ArrayList<>();
    private final List<PluginUISidebarItem> navItems = new ArrayList<>();

    public UIBuilder() {
    }

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

    /** Register a navigation item in the top bar. */
    public UINavItemBuilder navItem(String id, String label, String href, String section) {
        return new UINavItemBuilder(this, id, label, href, section);
    }

    /** @deprecated Use navItem() instead. */
    @Deprecated
    public UINavItemBuilder sidebarItem(String id, String label, String href, String section) {
        return navItem(id, label, href, section);
    }

    void addPage(PluginUIPage page) {
        pages.add(page);
    }

    void addTab(PluginUITab tab) {
        tabs.add(tab);
    }

    void addNavItem(PluginUISidebarItem item) {
        navItems.add(item);
    }

    /** @deprecated Use addNavItem() instead. */
    @Deprecated
    void addSidebarItem(PluginUISidebarItem item) {
        addNavItem(item);
    }

    public PluginUIInfo build() {
        PluginUIInfo.Builder builder = PluginUIInfo.newBuilder()
                .setHasBundle(hasBundle)
                .addAllPages(pages)
                .addAllTabs(tabs)
                .addAllSidebarItems(navItems);
        if (bundleData != null) {
            builder.setBundleData(com.google.protobuf.ByteString.copyFrom(bundleData));
        }
        return builder.build();
    }
}
