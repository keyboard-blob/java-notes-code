package socket.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

//网页版聊天室服务端：注册登录、在线列表、群聊私聊、表情、文件
public class WebChatServer {

    private static final int PORT = 8080;
    private static final Path PAGE = Path.of("web", "index.html");
    private static final Path EMOJI_DIR = Path.of("emoji");
    private static final Path UPLOAD_DIR = Path.of("web-uploads");

    //账号表：昵称 -> 密码
    private static final Map<String, String> ACCOUNTS = new ConcurrentHashMap<>();
    //登录凭证：token -> 昵称
    private static final Map<String, String> TOKENS = new ConcurrentHashMap<>();
    //在线表：昵称 -> 最后一次心跳时间
    private static final Map<String, Long> ONLINE = new ConcurrentHashMap<>();
    //已读位置：昵称 -> (会话名 -> 读到的消息编号)
    private static final Map<String, Map<String, Long>> READ = new ConcurrentHashMap<>();
    //文件表：id -> 信息
    private static final Map<Long, FileMeta> FILES = new ConcurrentHashMap<>();

    private static final List<Msg> MESSAGES = new ArrayList<>();
    private static long nextId = 1;
    private static long nextFileId = 1;

    private static final long ONLINE_TIMEOUT = 30_000;

    record Msg(long id, String from, String to, String kind, String text,
               long fileId, String fileName, long fileSize, long time) { }

    record FileMeta(long id, String name, Path path, String uploader) { }

    public static void main(String[] args) throws IOException {
        Files.createDirectories(UPLOAD_DIR);

        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/", WebChatServer::page);
        server.createContext("/register", WebChatServer::register);
        server.createContext("/login", WebChatServer::login);
        server.createContext("/logout", WebChatServer::logout);
        server.createContext("/read", WebChatServer::read);
        server.createContext("/state", WebChatServer::state);
        server.createContext("/poll", WebChatServer::poll);
        server.createContext("/send", WebChatServer::send);
        server.createContext("/emojis", WebChatServer::emojiList);
        server.createContext("/emojiimg", WebChatServer::emojiImage);
        server.createContext("/emoji", WebChatServer::emojiSend);
        server.createContext("/upload", WebChatServer::upload);
        server.createContext("/download", WebChatServer::download);

        server.setExecutor(null);
        server.start();
        System.out.println("网页聊天室已启动: http://localhost:" + PORT + "/");
    }

    // ---------- 页面 ----------

    private static void page(HttpExchange ex) throws IOException {
        if (!"/".equals(ex.getRequestURI().getPath())) {
            send(ex, 404, "text/plain", "not found");
            return;
        }
        if (Files.exists(PAGE)) {
            send(ex, 200, "text/html; charset=utf-8", Files.readAllBytes(PAGE));
        } else {
            send(ex, 200, "text/html; charset=utf-8", ("<h1>缺少 " + PAGE + "</h1>").getBytes(StandardCharsets.UTF_8));
        }
    }

    // ---------- 账号 ----------

    private static void register(HttpExchange ex) throws IOException {
        Map<String, String> f = form(ex);
        String name = f.getOrDefault("name", "").trim();
        String password = f.getOrDefault("password", "");

        if (name.isEmpty() || password.isEmpty()) {
            json(ex, "{\"ok\":false,\"msg\":\"昵称和密码不能为空\"}");
            return;
        }
        if (name.length() > 20) {
            json(ex, "{\"ok\":false,\"msg\":\"昵称太长\"}");
            return;
        }
        if (ACCOUNTS.containsKey(name)) {
            json(ex, "{\"ok\":false,\"msg\":\"昵称已存在\"}");
            return;
        }
        ACCOUNTS.put(name, password);
        json(ex, "{\"ok\":true}");
    }

    private static void login(HttpExchange ex) throws IOException {
        Map<String, String> f = form(ex);
        String name = f.getOrDefault("name", "").trim();
        String password = f.getOrDefault("password", "");

        String saved = ACCOUNTS.get(name);
        if (saved == null || !saved.equals(password)) {
            json(ex, "{\"ok\":false,\"msg\":\"昵称或密码错误\"}");
            return;
        }

        //同一账号不允许两处同时在线
        Long lastSeen = ONLINE.get(name);
        boolean online = lastSeen != null && System.currentTimeMillis() - lastSeen <= ONLINE_TIMEOUT;
        boolean hasToken = TOKENS.containsValue(name);
        if (online && hasToken) {
            json(ex, "{\"ok\":false,\"msg\":\"该账号已在别处登录\"}");
            return;
        }

        //上次是异常掉线（token 还在但超过超时），把旧 token 清掉
        TOKENS.values().removeIf(v -> v.equals(name));
        String token = UUID.randomUUID().toString();
        TOKENS.put(token, name);
        touch(name);

        json(ex, "{\"ok\":true,\"token\":\"" + token + "\",\"name\":\"" + escape(name) + "\"}");
    }

    private static void logout(HttpExchange ex) throws IOException {
        String token = query(ex).getOrDefault("token", "");
        String name = TOKENS.remove(token);
        if (name != null) ONLINE.remove(name);
        json(ex, "{\"ok\":true}");
    }

    //已读位置：GET 取回，POST 更新（chat=会话名，id=读到的消息编号）
    private static void read(HttpExchange ex) throws IOException {
        String me = user(ex);
        if (me == null) {
            json(ex, "{}");
            return;
        }

        Map<String, Long> marks = READ.computeIfAbsent(me, k -> new ConcurrentHashMap<>());

        if ("POST".equals(ex.getRequestMethod())) {
            Map<String, String> f = form(ex);
            String chat = f.getOrDefault("chat", "").trim();
            long id;
            try {
                id = Long.parseLong(f.getOrDefault("id", "0"));
            } catch (NumberFormatException e) {
                id = 0;
            }
            if (!chat.isEmpty()) marks.put(chat, id);
            json(ex, "{\"ok\":true}");
            return;
        }

        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Long> e : marks.entrySet()) {
            if (!first) sb.append(',');
            first = false;
            sb.append('"').append(escape(e.getKey())).append("\":").append(e.getValue());
        }
        sb.append('}');
        json(ex, sb.toString());
    }

    // ---------- 状态与消息 ----------

    private static void state(HttpExchange ex) throws IOException {
        String me = user(ex);
        if (me == null) {
            json(ex, "{\"ok\":false,\"msg\":\"未登录\"}");
            return;
        }
        touch(me);

        //返回所有注册过的账号，带上在线状态：离线的人也要留在列表里
        long now = System.currentTimeMillis();
        List<String> names = new ArrayList<>(ACCOUNTS.keySet());
        names.sort(Comparator.naturalOrder());

        StringBuilder sb = new StringBuilder("{\"ok\":true,\"me\":\"").append(escape(me)).append("\",\"users\":[");
        boolean first = true;
        for (String n : names) {
            Long last = ONLINE.get(n);
            boolean online = last != null && now - last <= ONLINE_TIMEOUT;
            if (!first) sb.append(',');
            first = false;
            sb.append("{\"name\":\"").append(escape(n)).append("\",\"online\":").append(online).append('}');
        }
        sb.append("]}");
        json(ex, sb.toString());
    }

    private static void poll(HttpExchange ex) throws IOException {
        String me = user(ex);
        if (me == null) {
            json(ex, "[]");
            return;
        }
        touch(me);

        long since = 0;
        try {
            since = Long.parseLong(query(ex).getOrDefault("since", "0"));
        } catch (NumberFormatException ignored) {
        }

        StringBuilder sb = new StringBuilder("[");
        synchronized (MESSAGES) {
            for (Msg m : MESSAGES) {
                if (m.id() <= since) continue;
                if (!visible(m, me)) continue;
                if (sb.length() > 1) sb.append(',');
                sb.append(msgJson(m));
            }
        }
        sb.append(']');
        json(ex, sb.toString());
    }

    private static void send(HttpExchange ex) throws IOException {
        String me = user(ex);
        if (me == null) {
            json(ex, "{\"ok\":false,\"msg\":\"未登录\"}");
            return;
        }
        Map<String, String> f = form(ex);
        String to = f.getOrDefault("to", "群聊").trim();
        String text = f.getOrDefault("text", "").trim();

        if (text.isEmpty()) {
            json(ex, "{\"ok\":false,\"msg\":\"内容为空\"}");
            return;
        }
        if (text.length() > 500) text = text.substring(0, 500);
        if (to.isEmpty()) to = "群聊";

        touch(me);
        add(new Msg(nextId++, me, to, "text", text, 0, "", 0, System.currentTimeMillis()));
        json(ex, "{\"ok\":true}");
    }

    private static void emojiList(HttpExchange ex) throws IOException {
        List<String> names = new ArrayList<>();
        if (Files.isDirectory(EMOJI_DIR)) {
            try (var stream = Files.list(EMOJI_DIR)) {
                stream.filter(Files::isRegularFile).forEach(p -> {
                    String n = p.getFileName().toString();
                    int dot = n.lastIndexOf('.');
                    if (dot > 0) names.add(n.substring(0, dot));
                });
            }
        }
        names.sort(Comparator.naturalOrder());
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < names.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append('"').append(escape(names.get(i))).append('"');
        }
        sb.append(']');
        json(ex, sb.toString());
    }

    private static void emojiImage(HttpExchange ex) throws IOException {
        String name = query(ex).getOrDefault("name", "");
        Path file = EMOJI_DIR.resolve(name + ".jpg");
        if (!Files.exists(file) || !file.normalize().startsWith(EMOJI_DIR.normalize())) {
            send(ex, 404, "text/plain", "not found");
            return;
        }
        send(ex, 200, "image/jpeg", Files.readAllBytes(file));
    }

    private static void emojiSend(HttpExchange ex) throws IOException {
        String me = user(ex);
        if (me == null) {
            json(ex, "{\"ok\":false,\"msg\":\"未登录\"}");
            return;
        }
        Map<String, String> f = form(ex);
        String to = f.getOrDefault("to", "群聊").trim();
        String id = f.getOrDefault("id", "").trim();
        if (id.isEmpty()) {
            json(ex, "{\"ok\":false}");
            return;
        }
        touch(me);
        add(new Msg(nextId++, me, to, "emoji", id, 0, "", 0, System.currentTimeMillis()));
        json(ex, "{\"ok\":true}");
    }

    // ---------- 文件 ----------

    private static void upload(HttpExchange ex) throws IOException {
        String me = user(ex);
        if (me == null) {
            json(ex, "{\"ok\":false,\"msg\":\"未登录\"}");
            return;
        }
        Map<String, String> q = query(ex);
        String to = q.getOrDefault("to", "群聊").trim();
        String name = q.getOrDefault("name", "file").trim();
        if (name.length() > 100) name = name.substring(0, 100);

        byte[] data = ex.getRequestBody().readAllBytes();
        if (data.length == 0 || data.length > 200 * 1024 * 1024) {
            json(ex, "{\"ok\":false,\"msg\":\"文件为空或太大\"}");
            return;
        }

        long id = nextFileId++;
        Path stored = UPLOAD_DIR.resolve(id + "_" + name.replaceAll("[\\\\/:*?\"<>|]", "_"));
        Files.write(stored, data);
        FILES.put(id, new FileMeta(id, name, stored, me));

        touch(me);
        add(new Msg(nextId++, me, to.isEmpty() ? "群聊" : to, "file", "", id, name, data.length, System.currentTimeMillis()));
        json(ex, "{\"ok\":true,\"id\":" + id + "}");
    }

    private static void download(HttpExchange ex) throws IOException {
        long id;
        try {
            id = Long.parseLong(query(ex).getOrDefault("id", "0"));
        } catch (NumberFormatException e) {
            send(ex, 400, "text/plain", "bad id");
            return;
        }
        FileMeta meta = FILES.get(id);
        if (meta == null || !Files.exists(meta.path())) {
            send(ex, 404, "text/plain", "not found");
            return;
        }
        ex.getResponseHeaders().set("Content-Type", "application/octet-stream");
        ex.getResponseHeaders().set("Content-Disposition",
                "attachment; filename*=UTF-8''" + java.net.URLEncoder.encode(meta.name(), StandardCharsets.UTF_8));
        byte[] bytes = Files.readAllBytes(meta.path());
        ex.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    // ---------- 工具 ----------

    private static void add(Msg m) {
        synchronized (MESSAGES) {
            MESSAGES.add(m);
            if (MESSAGES.size() > 2000) MESSAGES.subList(0, MESSAGES.size() - 2000).clear();
        }
    }

    //这条消息对某人是否可见：群聊、发给他的、他自己发的
    private static boolean visible(Msg m, String me) {
        return "群聊".equals(m.to()) || m.to().equals(me) || m.from().equals(me);
    }

    private static String msgJson(Msg m) {
        return "{\"id\":" + m.id()
                + ",\"from\":\"" + escape(m.from())
                + "\",\"to\":\"" + escape(m.to())
                + "\",\"kind\":\"" + escape(m.kind())
                + "\",\"text\":\"" + escape(m.text())
                + "\",\"fileId\":" + m.fileId()
                + ",\"fileName\":\"" + escape(m.fileName())
                + "\",\"fileSize\":" + m.fileSize()
                + ",\"time\":" + m.time() + "}";
    }

    private static void touch(String name) {
        ONLINE.put(name, System.currentTimeMillis());
    }

    private static String user(HttpExchange ex) {
        String token = query(ex).getOrDefault("token", "");
        String name = TOKENS.get(token);
        if (name != null) touch(name);
        return name;
    }

    private static Map<String, String> form(HttpExchange ex) throws IOException {
        String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        return parseQuery(body);
    }

    private static Map<String, String> query(HttpExchange ex) {
        return parseQuery(ex.getRequestURI().getRawQuery());
    }

    private static Map<String, String> parseQuery(String raw) {
        Map<String, String> map = new HashMap<>();
        if (raw == null || raw.isEmpty()) return map;
        for (String pair : raw.split("&")) {
            int eq = pair.indexOf('=');
            if (eq < 0) continue;
            String k = URLDecoder.decode(pair.substring(0, eq), StandardCharsets.UTF_8);
            String v = URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8);
            map.put(k, v);
        }
        return map;
    }

    private static String escape(String s) {
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
                }
            }
        }
        return sb.toString();
    }

    private static void json(HttpExchange ex, String body) throws IOException {
        send(ex, 200, "application/json; charset=utf-8", body.getBytes(StandardCharsets.UTF_8));
    }

    private static void send(HttpExchange ex, int code, String type, String text) throws IOException {
        send(ex, code, type, text.getBytes(StandardCharsets.UTF_8));
    }

    private static void send(HttpExchange ex, int code, String type, byte[] data) throws IOException {
        ex.getResponseHeaders().set("Content-Type", type);
        ex.sendResponseHeaders(code, data.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(data);
        }
    }
}
