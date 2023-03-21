package com.qtgl.iga.utils.webSocket;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.JsonObject;
import com.qtgl.iga.utils.UrlUtil;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.WebSocket;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.framing.Framedata;
import org.java_websocket.handshake.ServerHandshake;

import javax.net.ssl.*;
import java.net.Socket;
import java.net.URI;
import java.nio.ByteBuffer;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;


@Slf4j
public class ReConnectWebSocketClient {
    /**
     * 字符串消息回调
     */
    private Consumer<String> msgStr;
    /**
     * 字节流消息回调
     */
    private Consumer<ByteBuffer> msgByte;
    /**
     * 异常回调
     */
    private Consumer<Exception> error;
    /**
     * 连接标识
     */
    private String key;
    /**
     * ws服务端连接
     */
    private URI serverUri;
    /**
     * 尝试重连标识
     */
    private AtomicBoolean tryReconnect;
    /**
     * 需要ping标识
     */
    private AtomicBoolean needPing;
    /**
     * websocket连接实体
     */
    private WebSocketClient webSocketClient;
    /**
     * 重连次数
     */
    private AtomicInteger reConnectTimes;
    /**
     * 连接结束标识
     */
    private AtomicBoolean end;
    /**
     * 连接后初始发送报文，这里也可以不需要，如果服务端主动断开连接，重连后可以继续推送报文的话。
     */
    private String initReConnectReq;
    /**
     * 结束回调
     */
    private Consumer<String> endConsumer;

    public ReConnectWebSocketClient(URI serverUri, String key, Consumer<String> msgStr, Consumer<ByteBuffer> msgByte, Consumer<Exception> error) {
        this.msgStr = msgStr;
        this.msgByte = msgByte;
        this.error = error;
        this.key = key;
        this.serverUri = serverUri;
        this.tryReconnect = new AtomicBoolean(false);
        this.needPing = new AtomicBoolean(true);
        this.reConnectTimes = new AtomicInteger(0);
        this.end = new AtomicBoolean(false);
        this.endConsumer = this::close;
        init();
    }

    /**
     * 初始化连接
     */
    public void init() {
        // 创建连接
        createWebSocketClient();
        // ping线程
        circlePing();
    }

    /**
     * todo : 断开后如何恢复(例如: bus 重启 后能恢复)
     *         sub since 支持,记录断开时间,指定since获取记录
     *         集群多节点,如何保证数据一致性
     */
    private void needReconnect() throws Exception {
        ThreadUtil.sleep(10, TimeUnit.SECONDS);
        int cul = reConnectTimes.incrementAndGet();
        if (cul > 3) {
            close("real stop");
            throw new Exception("服务端断连，3次重连均失败");
        }
        log.warn("[{}]第[{}]次断开重连", key, cul);
        if (tryReconnect.get()) {
            log.error("[{}]第[{}]次断开重连结果 -> 连接正在重连，本次重连请求放弃", key, cul);
            needReconnect();
            return;
        }
        try {
            tryReconnect.set(true);

            if (webSocketClient.isOpen()) {
                log.warn("[{}]第[{}]次断开重连，关闭旧连接", key, cul);
                webSocketClient.closeConnection(2, "reconnect stop");
            }
            webSocketClient = null;
            createWebSocketClient();
            connect();
            if (!StrUtil.hasBlank(initReConnectReq)) {
                send(initReConnectReq);
            }
        } catch (Exception exception) {
            log.error("[{}]第[{}]次断开重连结果 -> 连接正在重连，重连异常:[{}]", key, cul, exception.getMessage());
            needReconnect();
        } finally {
            tryReconnect.set(false);
        }
    }

    private void createWebSocketClient() {
        webSocketClient =
                new WebSocketClient(serverUri) {
                    @Override
                    public void onOpen(ServerHandshake serverHandshake) {
                        log.info("[{}]ReConnectWebSocketClient [onOpen]连接成功{}", key, getRemoteSocketAddress());
                        JSONObject jsonObject=new JSONObject();
                        jsonObject.put("query", "subscription {" +
                                "  sub(topics:\"usersource.cdc\"){" +
                                "    id clientid services type openid data subject " +
                                "  } }");
                        send(jsonObject.toString());
                        tryReconnect.set(false);
                    }

                    @Override
                    public void onMessage(String text) {
                        log.info("[{}]ReConnectWebSocketClient [onMessage]接收到服务端数据：text={}", key, text);
                        msgStr.accept(text);
                    }

                    @Override
                    public void onMessage(ByteBuffer bytes) {
                        log.info("[{}]ReConnectWebSocketClient [onMessage]接收到服务端数据：bytes={}", key, bytes);
                        msgByte.accept(bytes);
                    }

                    @Override
                    public void onWebsocketPong(WebSocket conn, Framedata f) {
                        log.debug(
                                "[{}]ReConnectWebSocketClient [onWebsocketPong]接收到服务端数据：opcode={}",
                                key,
                                f.getOpcode());
                    }

                    @Override
                    public void onClose(int i, String s, boolean b) {
                        log.info("[{}]ReConnectWebSocketClient [onClose]关闭，s={}，b={}", key, s, b);
                        if (StrUtil.hasBlank(s) || s.contains("https")) {
                            if (end.get()) {
                                return;
                            }
                            try {
                                needReconnect();
                            } catch (Exception exception) {
                                endConsumer.accept("reconnect error");
                                error.accept(exception);
                            }
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        log.info("[{}]ReConnectWebSocketClient [onError]异常，e={}", key, e);
                        endConsumer.accept("error close");
                        error.accept(e);
                    }
                };
        if (serverUri.toString().contains("wss://")) {
            trustAllHosts(webSocketClient);
        }
    }

    public void circlePing() {
        new Thread(
                () -> {
                    while (needPing.get()) {
                        if (webSocketClient.isOpen()) {
                            webSocketClient.sendPing();
                        }
                        ThreadUtil.sleep(5, TimeUnit.SECONDS);
                    }
                    log.warn("[{}]Ping循环关闭", key);
                })
                .start();
    }

    /**
     * 连接
     *
     * @throws Exception 异常
     */
    public void connect() throws Exception {
        webSocketClient.connectBlocking(10, TimeUnit.SECONDS);
    }

    /**
     * 发送
     *
     * @param msg 消息
     * @throws Exception 异常
     */
    public void send(String msg) throws Exception {
        this.initReConnectReq = msg;
        if (webSocketClient.isOpen()) {
            webSocketClient.send(msg);
        }
    }

    /**
     * 关闭
     *
     * @param msg 关闭消息
     */
    public void close(String msg) {
        needPing.set(false);
        end.set(true);
        if (webSocketClient != null) {
            webSocketClient.closeConnection(3, msg);
        }
    }

    /**
     * 忽略证书
     *
     * @param client
     */
    public void trustAllHosts(WebSocketClient client) {
        TrustManager[] trustAllCerts =
                new TrustManager[]{
                        new X509ExtendedTrustManager() {

                            @Override
                            public void checkClientTrusted(
                                    X509Certificate[] x509Certificates, String s, Socket socket)
                                    throws CertificateException {
                            }

                            @Override
                            public void checkServerTrusted(
                                    X509Certificate[] x509Certificates, String s, Socket socket)
                                    throws CertificateException {
                            }

                            @Override
                            public void checkClientTrusted(
                                    X509Certificate[] x509Certificates, String s, SSLEngine sslEngine)
                                    throws CertificateException {
                            }

                            @Override
                            public void checkServerTrusted(
                                    X509Certificate[] x509Certificates, String s, SSLEngine sslEngine)
                                    throws CertificateException {
                            }

                            @Override
                            public void checkClientTrusted(X509Certificate[] x509Certificates, String s)
                                    throws CertificateException {
                            }

                            @Override
                            public void checkServerTrusted(X509Certificate[] x509Certificates, String s)
                                    throws CertificateException {
                            }

                            @Override
                            public X509Certificate[] getAcceptedIssuers() {
                                return null;
                            }
                        }
                };

        try {
            SSLContext ssl = SSLContext.getInstance("SSL");
            ssl.init(null, trustAllCerts, new java.security.SecureRandom());
            SSLSocketFactory socketFactory = ssl.getSocketFactory();
            client.setSocketFactory(socketFactory);
        } catch (Exception e) {
            log.error("ReConnectWebSocketClient trustAllHosts 异常，e={0}", e);
        }
    }




}