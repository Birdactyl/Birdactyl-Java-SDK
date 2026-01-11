package io.birdactyl.sdk;

import com.google.gson.Gson;
import com.google.protobuf.ByteString;
import io.birdactyl.sdk.proto.*;
import io.grpc.*;
import io.grpc.stub.StreamObserver;
import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Function;

public abstract class BirdactylPlugin {
    private static final Gson gson = new Gson();
    private final String id;
    private String name;
    private final String version;
    private final Map<String, Function<Event, EventResult>> eventHandlers = new ConcurrentHashMap<>();
    private final Map<String, Function<Request, Response>> routeHandlers = new ConcurrentHashMap<>();
    private final Map<String, Runnable> scheduleHandlers = new ConcurrentHashMap<>();
    private final Map<String, MixinRegistration> mixinHandlers = new ConcurrentHashMap<>();
    private final Map<String, AddonTypeRegistration> addonTypeHandlers = new ConcurrentHashMap<>();
    private final List<RouteInfo> routes = new ArrayList<>();
    private final List<ScheduleInfo> schedules = new ArrayList<>();
    private final List<MixinInfo> mixins = new ArrayList<>();
    private final List<AddonTypeInfo> addonTypes = new ArrayList<>();
    private final CountDownLatch shutdownLatch = new CountDownLatch(1);
    private PanelAPI api;
    private PanelAPIAsync asyncApi;
    private PanelServiceGrpc.PanelServiceStub asyncStub;
    private Executor asyncExecutor = ForkJoinPool.commonPool();
    private File dataDir;
    private boolean useDataDir = false;
    private Runnable onStartCallback;
    private StreamObserver<PluginMessage> outStream;
    private PluginUIInfo uiInfo;

    private static class MixinRegistration {
        final String target;
        final int priority;
        final MixinHandler handler;
        MixinRegistration(String target, int priority, MixinHandler handler) {
            this.target = target; this.priority = priority; this.handler = handler;
        }
    }

    private static class AddonTypeRegistration {
        final String typeId;
        final AddonTypeHandler handler;
        AddonTypeRegistration(String typeId, AddonTypeHandler handler) {
            this.typeId = typeId; this.handler = handler;
        }
    }

    @FunctionalInterface
    public interface AddonTypeHandler {
        AddonTypeResult handle(AddonTypeContext ctx);
    }

    public BirdactylPlugin(String id, String version) {
        this.id = id;
        this.name = id;
        this.version = version;
    }

    public BirdactylPlugin setName(String n) {
        this.name = n;
        return this;
    }

    public BirdactylPlugin useDataDir() {
        this.useDataDir = true;
        return this;
    }

    public BirdactylPlugin asyncExecutor(Executor executor) {
        this.asyncExecutor = executor;
        return this;
    }

    public BirdactylPlugin onStart(Runnable callback) {
        this.onStartCallback = callback;
        return this;
    }

    public BirdactylPlugin setUI(PluginUIInfo ui) {
        this.uiInfo = ui;
        return this;
    }

    public UIBuilder ui() {
        return new UIBuilder();
    }

    public void onEvent(String eventType, Function<Event, EventResult> handler) {
        eventHandlers.put(eventType, handler);
    }

    public void route(String method, String path, Function<Request, Response> handler) {
        routeHandlers.put(method + ":" + path, handler);
        routes.add(RouteInfo.newBuilder().setMethod(method).setPath(path).build());
    }

    public void schedule(String scheduleId, String cron, Runnable handler) {
        scheduleHandlers.put(scheduleId, handler);
        schedules.add(ScheduleInfo.newBuilder().setId(scheduleId).setCron(cron).build());
    }

    public void mixin(String target, MixinHandler handler) {
        mixin(target, 0, handler);
    }

    public void mixin(String target, int priority, MixinHandler handler) {
        mixinHandlers.put(target, new MixinRegistration(target, priority, handler));
        mixins.add(MixinInfo.newBuilder().setTarget(target).setPriority(priority).build());
    }

    public void addonType(String typeId, AddonTypeHandler handler) {
        addonTypeHandlers.put(typeId, new AddonTypeRegistration(typeId, handler));
        addonTypes.add(AddonTypeInfo.newBuilder().setTypeId(typeId).build());
    }

    public void registerMixin(Class<? extends MixinClass> clazz) {
        Mixin annotation = clazz.getAnnotation(Mixin.class);
        if (annotation == null) return;
        try {
            MixinClass instance = clazz.getDeclaredConstructor().newInstance();
            instance.init(this);
            mixin(annotation.value(), annotation.priority(), instance::handle);
        } catch (Exception e) {
            System.err.println("[mixin] Failed to register " + clazz.getName() + ": " + e.getMessage());
        }
    }

    public void registerMixins(Class<?>... classes) {
        for (Class<?> clazz : classes) {
            if (MixinClass.class.isAssignableFrom(clazz)) {
                registerMixin((Class<? extends MixinClass>) clazz);
            }
        }
    }

    public PanelAPI api() {
        return api;
    }

    public PanelAPIAsync async() {
        return asyncApi;
    }

    public ConsoleStream streamConsole(ConsoleStream.Builder builder) {
        ConsoleStream stream = builder.build();
        asyncStub.streamConsole(builder.buildRequest(), stream.createObserver());
        return stream;
    }

    public ConsoleStream.Builder console(String serverId) {
        return new ConsoleStream.Builder(serverId);
    }

    public File dataDir() {
        return dataDir;
    }

    public File dataPath(String filename) {
        return new File(dataDir, filename);
    }

    public <T> void saveConfig(T config) {
        saveConfig(config, "config.json");
    }

    public <T> void saveConfig(T config, String filename) {
        try {
            File file = dataPath(filename);
            file.getParentFile().mkdirs();
            try (java.io.FileWriter writer = new java.io.FileWriter(file)) {
                gson.toJson(config, writer);
            }
        } catch (Exception e) {
            System.err.println("[" + id + "] Failed to save config: " + e.getMessage());
        }
    }

    public <T> T loadConfig(Class<T> type) {
        return loadConfig(type, "config.json");
    }

    public <T> T loadConfig(Class<T> type, String filename) {
        try {
            File file = dataPath(filename);
            if (!file.exists()) {
                return null;
            }
            try (java.io.FileReader reader = new java.io.FileReader(file)) {
                return gson.fromJson(reader, type);
            }
        } catch (Exception e) {
            System.err.println("[" + id + "] Failed to load config: " + e.getMessage());
            return null;
        }
    }

    public <T> T loadConfigOrDefault(T defaultConfig, String filename) {
        try {
            File file = dataPath(filename);
            if (!file.exists()) {
                saveConfig(defaultConfig, filename);
                return defaultConfig;
            }
            try (java.io.FileReader reader = new java.io.FileReader(file)) {
                @SuppressWarnings("unchecked")
                T loaded = (T) gson.fromJson(reader, defaultConfig.getClass());
                return loaded != null ? loaded : defaultConfig;
            }
        } catch (Exception e) {
            System.err.println("[" + id + "] Failed to load config: " + e.getMessage());
            return defaultConfig;
        }
    }

    public void start(String panelAddress) throws Exception {
        String dataDirPath = id;

        String[] args = System.getProperty("sun.java.command", "").split(" ");
        if (args.length > 1) {
            dataDirPath = args[1] + "/" + id + "_data";
        } else {
            dataDirPath = id + "_data";
        }

        dataDir = new File(dataDirPath);
        if (useDataDir) {
            dataDir.mkdirs();
        }

        ManagedChannel channel = ManagedChannelBuilder.forTarget(panelAddress)
                .usePlaintext()
                .intercept(new ClientInterceptor() {
                    @Override
                    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method, CallOptions options, Channel next) {
                        return new ForwardingClientCall.SimpleForwardingClientCall<>(next.newCall(method, options)) {
                            @Override
                            public void start(Listener<RespT> listener, Metadata headers) {
                                headers.put(Metadata.Key.of("x-plugin-id", Metadata.ASCII_STRING_MARSHALLER), id);
                                super.start(listener, headers);
                            }
                        };
                    }
                })
                .build();

        api = new PanelAPI(PanelServiceGrpc.newBlockingStub(channel));
        asyncApi = new PanelAPIAsync(PanelServiceGrpc.newFutureStub(channel), asyncExecutor);
        asyncStub = PanelServiceGrpc.newStub(channel);

        CountDownLatch connectedLatch = new CountDownLatch(1);

        outStream = asyncStub.connect(new StreamObserver<>() {
            @Override
            public void onNext(PanelMessage msg) {
                if (msg.hasRegistered()) {
                    connectedLatch.countDown();
                    return;
                }
                handleMessage(msg);
            }

            @Override
            public void onError(Throwable t) {
                System.err.println("[" + id + "] stream error: " + t.getMessage());
                shutdownLatch.countDown();
            }

            @Override
            public void onCompleted() {
                System.out.println("[" + id + "] stream closed");
                shutdownLatch.countDown();
            }
        });

        outStream.onNext(PluginMessage.newBuilder().setRegister(buildInfo()).build());

        connectedLatch.await();
        System.out.println("[" + id + "] v" + version + " connected to panel");

        if (onStartCallback != null) {
            onStartCallback.run();
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> outStream.onCompleted()));
        shutdownLatch.await();
    }

    private PluginInfo buildInfo() {
        PluginInfo.Builder builder = PluginInfo.newBuilder()
                .setId(id)
                .setName(name)
                .setVersion(version)
                .addAllEvents(eventHandlers.keySet())
                .addAllRoutes(routes)
                .addAllSchedules(schedules)
                .addAllMixins(mixins)
                .addAllAddonTypes(addonTypes);
        if (uiInfo != null) {
            builder.setUi(uiInfo);
        }
        return builder.build();
    }

    private void handleMessage(PanelMessage msg) {
        PluginMessage.Builder resp = PluginMessage.newBuilder().setRequestId(msg.getRequestId());

        if (msg.hasEvent()) {
            resp.setEventResponse(handleEvent(msg.getEvent()));
        } else if (msg.hasHttp()) {
            resp.setHttpResponse(handleHTTP(msg.getHttp()));
        } else if (msg.hasSchedule()) {
            handleSchedule(msg.getSchedule());
            resp.setScheduleResponse(Empty.getDefaultInstance());
        } else if (msg.hasMixin()) {
            resp.setMixinResponse(handleMixin(msg.getMixin()));
        } else if (msg.hasAddonType()) {
            resp.setAddonTypeResponse(handleAddonType(msg.getAddonType()));
        } else if (msg.hasShutdown()) {
            System.out.println("[" + id + "] shutdown requested");
            System.exit(0);
        } else {
            return;
        }

        outStream.onNext(resp.build());
    }

    private EventResponse handleEvent(io.birdactyl.sdk.proto.Event ev) {
        Function<Event, EventResult> handler = eventHandlers.get(ev.getType());
        EventResult result = EventResult.allow();
        if (handler != null) {
            result = handler.apply(new Event(ev.getType(), ev.getDataMap(), ev.getSync()));
        }
        return EventResponse.newBuilder().setAllow(result.isAllowed()).setMessage(result.getMessage()).build();
    }

    private HTTPResponse handleHTTP(HTTPRequest request) {
        Function<Request, Response> handler = routeHandlers.get(request.getMethod() + ":" + request.getPath());
        if (handler == null) {
            for (Map.Entry<String, Function<Request, Response>> entry : routeHandlers.entrySet()) {
                String[] parts = entry.getKey().split(":", 2);
                if ((parts[0].equals("*") || parts[0].equals(request.getMethod())) && matchPath(parts[1], request.getPath())) {
                    handler = entry.getValue();
                    break;
                }
            }
        }

        Response resp;
        if (handler != null) {
            resp = handler.apply(new Request(request));
        } else {
            resp = Response.error(404, "not found");
        }

        return HTTPResponse.newBuilder()
                .setStatus(resp.getStatus())
                .putAllHeaders(resp.getHeaders())
                .setBody(ByteString.copyFrom(resp.getBody()))
                .build();
    }

    private void handleSchedule(ScheduleRequest request) {
        Runnable handler = scheduleHandlers.get(request.getScheduleId());
        if (handler != null) {
            handler.run();
        }
    }

    @SuppressWarnings("unchecked")
    private io.birdactyl.sdk.proto.MixinResponse handleMixin(io.birdactyl.sdk.proto.MixinRequest request) {
        MixinRegistration reg = mixinHandlers.get(request.getTarget());
        if (reg == null) {
            return io.birdactyl.sdk.proto.MixinResponse.newBuilder()
                    .setAction(io.birdactyl.sdk.proto.MixinResponse.Action.NEXT)
                    .build();
        }

        Map<String, Object> input = gson.fromJson(request.getInput().toStringUtf8(), Map.class);
        Map<String, Object> chainData = null;
        if (!request.getChainData().isEmpty()) {
            chainData = gson.fromJson(request.getChainData().toStringUtf8(), Map.class);
        }

        MixinContext ctx = new MixinContext(request.getTarget(), request.getRequestId(), input, chainData);
        MixinResult result = reg.handler.handle(ctx);

        io.birdactyl.sdk.proto.MixinResponse.Builder resp = io.birdactyl.sdk.proto.MixinResponse.newBuilder();

        switch (result.getAction()) {
            case NEXT:
                resp.setAction(io.birdactyl.sdk.proto.MixinResponse.Action.NEXT);
                if (result.getModifiedInput() != null) {
                    resp.setModifiedInput(ByteString.copyFromUtf8(gson.toJson(result.getModifiedInput())));
                }
                break;
            case RETURN:
                resp.setAction(io.birdactyl.sdk.proto.MixinResponse.Action.RETURN);
                if (result.getOutput() != null) {
                    resp.setOutput(ByteString.copyFromUtf8(gson.toJson(result.getOutput())));
                }
                break;
            case ERROR:
                resp.setAction(io.birdactyl.sdk.proto.MixinResponse.Action.ERROR);
                resp.setError(result.getError() != null ? result.getError() : "");
                break;
        }

        for (MixinResult.Notification n : result.getNotifications()) {
            resp.addNotifications(io.birdactyl.sdk.proto.Notification.newBuilder()
                    .setTitle(n.getTitle())
                    .setMessage(n.getMessage())
                    .setType(n.getType())
                    .build());
        }

        return resp.build();
    }

    private io.birdactyl.sdk.proto.AddonTypeResponse handleAddonType(io.birdactyl.sdk.proto.AddonTypeRequest request) {
        AddonTypeRegistration reg = addonTypeHandlers.get(request.getTypeId());
        if (reg == null) {
            return io.birdactyl.sdk.proto.AddonTypeResponse.newBuilder()
                    .setSuccess(false)
                    .setError("addon type handler not found")
                    .build();
        }

        AddonTypeContext ctx = new AddonTypeContext(
                request.getTypeId(),
                request.getServerId(),
                request.getNodeId(),
                request.getDownloadUrl(),
                request.getFileName(),
                request.getInstallPath(),
                request.getSourceInfoMap(),
                request.getServerVariablesMap()
        );

        AddonTypeResult result = reg.handler.handle(ctx);

        io.birdactyl.sdk.proto.AddonTypeResponse.Builder resp = io.birdactyl.sdk.proto.AddonTypeResponse.newBuilder()
                .setSuccess(result.isSuccess())
                .setError(result.getError() != null ? result.getError() : "")
                .setMessage(result.getMessage() != null ? result.getMessage() : "");

        for (AddonTypeResult.Action action : result.getActions()) {
            io.birdactyl.sdk.proto.AddonInstallAction.Builder actionBuilder = io.birdactyl.sdk.proto.AddonInstallAction.newBuilder()
                    .setType(io.birdactyl.sdk.proto.AddonInstallAction.ActionType.forNumber(action.getType()))
                    .setUrl(action.getUrl() != null ? action.getUrl() : "")
                    .setPath(action.getPath() != null ? action.getPath() : "")
                    .setCommand(action.getCommand() != null ? action.getCommand() : "")
                    .setNodeEndpoint(action.getNodeEndpoint() != null ? action.getNodeEndpoint() : "");
            if (action.getContent() != null) {
                actionBuilder.setContent(ByteString.copyFrom(action.getContent()));
            }
            if (action.getHeaders() != null) {
                actionBuilder.putAllHeaders(action.getHeaders());
            }
            if (action.getNodePayload() != null) {
                actionBuilder.setNodePayload(ByteString.copyFrom(action.getNodePayload()));
            }
            resp.addActions(actionBuilder.build());
        }

        return resp.build();
    }

    private boolean matchPath(String pattern, String path) {
        if (pattern.equals(path)) return true;
        if (pattern.endsWith("*")) {
            return path.startsWith(pattern.substring(0, pattern.length() - 1));
        }
        return false;
    }
}
