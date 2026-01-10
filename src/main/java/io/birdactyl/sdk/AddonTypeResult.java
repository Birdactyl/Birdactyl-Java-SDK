package io.birdactyl.sdk;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AddonTypeResult {
    private final boolean success;
    private final String error;
    private final String message;
    private final List<Action> actions;

    private AddonTypeResult(boolean success, String error, String message, List<Action> actions) {
        this.success = success;
        this.error = error;
        this.message = message;
        this.actions = actions;
    }

    public boolean isSuccess() { return success; }
    public String getError() { return error; }
    public String getMessage() { return message; }
    public List<Action> getActions() { return actions; }

    public static AddonTypeResult success(String message, Action... actions) {
        List<Action> actionList = new ArrayList<>();
        for (Action a : actions) actionList.add(a);
        return new AddonTypeResult(true, null, message, actionList);
    }

    public static AddonTypeResult error(String error) {
        return new AddonTypeResult(false, error, null, new ArrayList<>());
    }

    public static class Action {
        public static final int DOWNLOAD_FILE = 0;
        public static final int EXTRACT_ARCHIVE = 1;
        public static final int DELETE_FILE = 2;
        public static final int CREATE_FOLDER = 3;
        public static final int WRITE_FILE = 4;
        public static final int RUN_COMMAND = 5;
        public static final int PROXY_TO_NODE = 6;

        private final int type;
        private final String url;
        private final String path;
        private final byte[] content;
        private final String command;
        private final Map<String, String> headers;
        private final byte[] nodePayload;
        private final String nodeEndpoint;

        private Action(int type, String url, String path, byte[] content, String command,
                      Map<String, String> headers, byte[] nodePayload, String nodeEndpoint) {
            this.type = type;
            this.url = url;
            this.path = path;
            this.content = content;
            this.command = command;
            this.headers = headers;
            this.nodePayload = nodePayload;
            this.nodeEndpoint = nodeEndpoint;
        }

        public int getType() { return type; }
        public String getUrl() { return url; }
        public String getPath() { return path; }
        public byte[] getContent() { return content; }
        public String getCommand() { return command; }
        public Map<String, String> getHeaders() { return headers; }
        public byte[] getNodePayload() { return nodePayload; }
        public String getNodeEndpoint() { return nodeEndpoint; }

        public static Action downloadFile(String url, String path) {
            return downloadFile(url, path, null);
        }

        public static Action downloadFile(String url, String path, Map<String, String> headers) {
            return new Action(DOWNLOAD_FILE, url, path, null, null, headers, null, null);
        }

        public static Action extractArchive(String path) {
            return new Action(EXTRACT_ARCHIVE, null, path, null, null, null, null, null);
        }

        public static Action deleteFile(String path) {
            return new Action(DELETE_FILE, null, path, null, null, null, null, null);
        }

        public static Action createFolder(String path) {
            return new Action(CREATE_FOLDER, null, path, null, null, null, null, null);
        }

        public static Action writeFile(String path, byte[] content) {
            return new Action(WRITE_FILE, null, path, content, null, null, null, null);
        }

        public static Action proxyToNode(String endpoint, byte[] payload) {
            return new Action(PROXY_TO_NODE, null, null, null, null, null, payload, endpoint);
        }
    }
}
