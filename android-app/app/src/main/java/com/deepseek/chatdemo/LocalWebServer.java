package com.deepseek.chatdemo;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import fi.iki.elonen.NanoHTTPD;

import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 内嵌本地静态服务器：把打包进 assets/web 的前端产物以
 * http://127.0.0.1:<port> 提供给应用内 WebView 访问。
 */
public class LocalWebServer extends NanoHTTPD {

    private static final String TAG = "LocalWebServer";
    private static final String WEB_ROOT = "web";

    private static final Map<String, String> MIME = new HashMap<>();

    static {
        MIME.put("html", "text/html; charset=utf-8");
        MIME.put("js", "application/javascript; charset=utf-8");
        MIME.put("css", "text/css; charset=utf-8");
        MIME.put("json", "application/json; charset=utf-8");
        MIME.put("svg", "image/svg+xml");
        MIME.put("png", "image/png");
        MIME.put("jpg", "image/jpeg");
        MIME.put("jpeg", "image/jpeg");
        MIME.put("gif", "image/gif");
        MIME.put("ico", "image/x-icon");
        MIME.put("woff", "font/woff");
        MIME.put("woff2", "font/woff2");
        MIME.put("ttf", "font/ttf");
        MIME.put("wasm", "application/wasm");
        MIME.put("txt", "text/plain; charset=utf-8");
    }

    private final Context ctx;

    public LocalWebServer(Context ctx, int port) {
        super(port);
        this.ctx = ctx.getApplicationContext();
    }

    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();
        if (uri == null || uri.isEmpty()) {
            uri = "/";
        }
        try {
            if ("/api/exec".equals(normalize(uri))) {
                return handleExec(session);
            }
            String path = normalize(uri);
            byte[] body = readAsset(path);
            if (body == null) {
                // SPA 路由回退到首页
                body = readAsset("index.html");
                if (body == null) {
                    return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Not found");
                }
                return newFixedLengthResponse(Response.Status.OK, MIME.get("html"),
                        new ByteArrayInputStream(body), body.length);
            }
            return newFixedLengthResponse(Response.Status.OK, mimeFor(path),
                    new ByteArrayInputStream(body), body.length);
        } catch (Exception e) {
            Log.e(TAG, "serve error: " + uri, e);
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", e.getMessage());
        }
    }

    /** POST /api/exec：在沙箱目录执行一条命令，仅允许本机回环访问。 */
    private Response handleExec(IHTTPSession session) {
        if (!isLoopback(session)) {
            return newFixedLengthResponse(Response.Status.FORBIDDEN, "text/plain", "Forbidden");
        }
        if (!"POST".equalsIgnoreCase(session.getMethod().name())) {
            return newFixedLengthResponse(Response.Status.METHOD_NOT_ALLOWED, "text/plain", "Use POST");
        }
        try {
            Map<String, String> body = new HashMap<>();
            session.parseBody(body);
            String raw = body.get("postData");
            JSONObject json = raw != null ? new JSONObject(raw) : new JSONObject();
            String command = json.optString("command", "");
            ShellSession.Result result = ShellSession.exec(ctx, command);
            JSONObject out = new JSONObject();
            out.put("stdout", result.stdout);
            out.put("stderr", result.stderr);
            out.put("code", result.code);
            return newFixedLengthResponse(Response.Status.OK, "application/json", out.toString());
        } catch (Exception e) {
            Log.e(TAG, "exec error", e);
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "application/json",
                    "{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    private boolean isLoopback(IHTTPSession session) {
        String ip = session.getRemoteIpAddress();
        return "127.0.0.1".equals(ip) || "::1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip);
    }

    private String normalize(String uri) {
        int q = uri.indexOf('?');
        if (q >= 0) {
            uri = uri.substring(0, q);
        }
        if (uri.equals("/")) {
            return "index.html";
        }
        String p = uri.startsWith("/") ? uri.substring(1) : uri;
        if (p.contains("..")) {
            return "index.html";
        }
        if (p.endsWith("/")) {
            p = p + "index.html";
        }
        return p;
    }

    private byte[] readAsset(String rel) {
        AssetManager am = ctx.getAssets();
        try (InputStream in = am.open(WEB_ROOT + "/" + rel);
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) != -1) {
                bos.write(buf, 0, n);
            }
            return bos.toByteArray();
        } catch (Exception e) {
            return null;
        }
    }

    private String mimeFor(String path) {
        int dot = path.lastIndexOf('.');
        if (dot < 0) {
            return MIME.get("txt");
        }
        String ext = path.substring(dot + 1).toLowerCase();
        return MIME.getOrDefault(ext, "application/octet-stream");
    }

    /** 供界面展示的访问地址（本机回环） */
    public String baseUrl() {
        return "http://127.0.0.1:" + getListeningPort();
    }
}
