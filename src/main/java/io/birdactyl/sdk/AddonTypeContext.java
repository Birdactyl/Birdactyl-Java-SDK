package io.birdactyl.sdk;

import java.util.Map;

public class AddonTypeContext {
    private final String typeId;
    private final String serverId;
    private final String nodeId;
    private final String downloadUrl;
    private final String fileName;
    private final String installPath;
    private final Map<String, String> sourceInfo;
    private final Map<String, String> serverVariables;

    public AddonTypeContext(String typeId, String serverId, String nodeId, String downloadUrl,
                           String fileName, String installPath, Map<String, String> sourceInfo,
                           Map<String, String> serverVariables) {
        this.typeId = typeId;
        this.serverId = serverId;
        this.nodeId = nodeId;
        this.downloadUrl = downloadUrl;
        this.fileName = fileName;
        this.installPath = installPath;
        this.sourceInfo = sourceInfo;
        this.serverVariables = serverVariables;
    }

    public String getTypeId() { return typeId; }
    public String getServerId() { return serverId; }
    public String getNodeId() { return nodeId; }
    public String getDownloadUrl() { return downloadUrl; }
    public String getFileName() { return fileName; }
    public String getInstallPath() { return installPath; }
    public Map<String, String> getSourceInfo() { return sourceInfo; }
    public Map<String, String> getServerVariables() { return serverVariables; }

    public String getSourceInfo(String key) {
        return sourceInfo != null ? sourceInfo.get(key) : null;
    }

    public String getServerVariable(String key) {
        return serverVariables != null ? serverVariables.get(key) : null;
    }
}
