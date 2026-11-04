package govsim.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import govsim.engine.MotorEconomico;
import govsim.model.Estado;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

/**
 * Servidor HTTP nativo do Java (com.sun.net.httpserver.HttpServer).
 * Responsável por servir a camada VIEW (HTML, CSS, JS) e expor a API REST JSON
 * para comunicação segura e sincronizada com o Motor Econômico.
 */
public class ServidorHttp {
    private static final int LIMITE_CORPO_BYTES = 4096;

    private final int porta;
    private static final long INATIVIDADE_MS = 30L * 60L * 1000L;
    private static final int LIMITE_SESSOES = 100;
    private final GerenciadorSessoes sessoes = new GerenciadorSessoes(INATIVIDADE_MS, LIMITE_SESSOES, System::currentTimeMillis);
    private java.util.concurrent.ScheduledExecutorService limpezaSessoes;

    private final File diretorioView;
    private HttpServer server;

    public ServidorHttp(int porta, File diretorioView) {
        this.porta = porta;
        this.diretorioView = diretorioView;

    }

    /** Header por aba; cookie como fallback para clientes HTTP convencionais. */
    private Sessao sessao(HttpExchange exchange) {
        String id=exchange.getRequestHeaders().getFirst("X-GovSim-Session");
        if(id==null){String cookie=exchange.getRequestHeaders().getFirst("Cookie");if(cookie!=null)for(String parte:cookie.split(";"))if(parte.trim().startsWith("govsim="))id=parte.trim().substring(7);}
        if(id==null || !id.matches("[a-fA-F0-9-]{36}"))id=java.util.UUID.randomUUID().toString();
        Sessao atual = sessoes.obter(id);
        exchange.getResponseHeaders().set("X-GovSim-Session",id);
        exchange.getResponseHeaders().add("Set-Cookie","govsim="+id+"; Path=/; HttpOnly; SameSite=Strict");
        return atual;
    }

    public void iniciar() throws IOException {
        this.server = HttpServer.create(new InetSocketAddress("127.0.0.1", this.porta), 0);

        // Endpoints da API do jogo
        this.server.createContext("/api/estado", new HandlerEstado());
        this.server.createContext("/api/iniciar", new HandlerIniciar());
        this.server.createContext("/api/decidir", new HandlerDecidir());

        // Arquivos estáticos da interface (VIEW)
        this.server.createContext("/", new HandlerArquivosEstaticos());

        this.server.setExecutor(null);
        this.server.start();
        limpezaSessoes = java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "govsim-limpeza-sessoes");
            thread.setDaemon(true);
            return thread;
        });
        limpezaSessoes.scheduleAtFixedRate(sessoes::expirarInativas, 1, 1, java.util.concurrent.TimeUnit.MINUTES);
    }

    int portaEfetiva() { return server.getAddress().getPort(); }

    public void parar() {
        if (limpezaSessoes != null) limpezaSessoes.shutdownNow();
        if (this.server != null) {
            this.server.stop(0);
        }
        sessoes.limpar();
    }

    private class HandlerEstado implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                    enviarJson(exchange, 405, "{\"erro\":\"Método não permitido. Use GET.\"}");
                    return;
                }
                Sessao atual = sessao(exchange);
                Estado estado = atual.getEstado();
                MotorEconomico motorEconomico = atual.getMotor();
                synchronized (estado) {
                    String json = JsonUtil.estadoParaJson(estado);
                    enviarJson(exchange, 200, json);
                }
            } catch (Exception e) {
                enviarJson(exchange, 500, "{\"erro\":" + JsonUtil.quote(e.getMessage()) + "}");
            }
        }
    }

    private class HandlerIniciar implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                    enviarJson(exchange, 405, "{\"erro\":\"Método não permitido. Use POST.\"}");
                    return;
                }
                Requisicao.Inicio requisicao = new Requisicao.Inicio(lerCorpoRequisicao(exchange));

                Sessao atual = sessao(exchange);
                Estado estado = atual.getEstado();
                MotorEconomico motorEconomico = atual.getMotor();
                synchronized (estado) {
                    motorEconomico.iniciarPartida(estado, requisicao.demo, requisicao.turnos, requisicao.dificuldade, requisicao.seed);
                    String json = JsonUtil.estadoParaJson(estado);
                    enviarJson(exchange, 200, json);
                }
            } catch (Exception e) {
                enviarJson(exchange, 400, "{\"erro\":" + JsonUtil.quote(e.getMessage()) + "}");
            }
        }
    }

    private class HandlerDecidir implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                    enviarJson(exchange, 405, "{\"erro\":\"Método não permitido. Use POST.\"}");
                    return;
                }
                Requisicao.Decisao requisicao = new Requisicao.Decisao(lerCorpoRequisicao(exchange));

                Sessao atual = sessao(exchange);
                Estado estado = atual.getEstado();
                MotorEconomico motorEconomico = atual.getMotor();
                synchronized (estado) {
                    if (estado.isPartidaEncerrada()) {
                        enviarJson(exchange, 409, "{\"erro\":\"A partida já foi encerrada. Inicie um novo mandato.\"}");
                        return;
                    }
                    if (requisicao.turno != estado.getTurnoAtual()) {
                        enviarJson(exchange, 409, "{\"erro\":\"Decisão duplicada ou fora de sincronia com o turno atual (" + estado.getTurnoAtual() + ").\"}");
                        return;
                    }
                    motorEconomico.processarDecisao(estado, requisicao.decreto);
                    String json = JsonUtil.estadoParaJson(estado);
                    enviarJson(exchange, 200, json);
                }
            } catch (IllegalArgumentException e) {
                enviarJson(exchange, 400, "{\"erro\":" + JsonUtil.quote(e.getMessage()) + "}");
            } catch (IllegalStateException e) {
                enviarJson(exchange, 409, "{\"erro\":" + JsonUtil.quote(e.getMessage()) + "}");
            } catch (Exception e) {
                enviarJson(exchange, 500, "{\"erro\":" + JsonUtil.quote(e.getMessage()) + "}");
            }
        }
    }

    private class HandlerArquivosEstaticos implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod()) && !"HEAD".equalsIgnoreCase(exchange.getRequestMethod())) {
                enviarTexto(exchange, 405, "Método não permitido.");
                return;
            }

            String caminho = exchange.getRequestURI().getPath();
            if (caminho == null || caminho.equals("/") || caminho.isEmpty()) {
                caminho = "/index.html";
            }

            if (caminho.contains("..")) {
                enviarTexto(exchange, 403, "Acesso negado.");
                return;
            }

            File arquivo = new File(diretorioView, caminho.substring(1));
            String canonicalBase = diretorioView.getCanonicalPath();
            String canonicalTarget = arquivo.getCanonicalPath();
            if (!canonicalTarget.startsWith(canonicalBase)) {
                enviarTexto(exchange, 403, "Acesso negado.");
                return;
            }

            if (!arquivo.exists() || !arquivo.isFile()) {
                enviarTexto(exchange, 404, "Arquivo não encontrado: " + caminho);
                return;
            }

            String contentType = determinarContentType(caminho);
            byte[] bytes = lerArquivoBytes(arquivo);

            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.getResponseHeaders().set("Cache-Control", "no-store, no-cache, must-revalidate");
            exchange.sendResponseHeaders(200, bytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
        }
    }

    private static String determinarContentType(String caminho) {
        String lower = caminho.toLowerCase();
        if (lower.endsWith(".html")) return "text/html; charset=utf-8";
        if (lower.endsWith(".css")) return "text/css; charset=utf-8";
        if (lower.endsWith(".js")) return "application/javascript; charset=utf-8";
        if (lower.endsWith(".woff2")) return "font/woff2";
        if (lower.endsWith(".json")) return "application/json; charset=utf-8";
        if (lower.endsWith(".svg")) return "image/svg+xml";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".ico")) return "image/x-icon";
        return "text/plain; charset=utf-8";
    }

    private static void enviarJson(HttpExchange exchange, int statusCode, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-store, no-cache, must-revalidate");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(bytes);
        os.close();
    }

    private static void enviarTexto(HttpExchange exchange, int statusCode, String texto) throws IOException {
        byte[] bytes = texto.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(bytes);
        os.close();
    }

    private static String lerCorpoRequisicao(HttpExchange exchange) throws IOException {
        InputStream is = exchange.getRequestBody();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] dados = new byte[1024];
        int totalLido = 0;
        int lidos;
        while ((lidos = is.read(dados, 0, dados.length)) != -1) {
            totalLido += lidos;
            if (totalLido > LIMITE_CORPO_BYTES) {
                throw new IllegalArgumentException("Corpo da requisição excede o limite permitido de " + LIMITE_CORPO_BYTES + " bytes.");
            }
            buffer.write(dados, 0, lidos);
        }
        return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
    }

    private static byte[] lerArquivoBytes(File arquivo) throws IOException {
        FileInputStream fis = new FileInputStream(arquivo);
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] dados = new byte[4096];
            int lidos;
            while ((lidos = fis.read(dados, 0, dados.length)) != -1) {
                buffer.write(dados, 0, lidos);
            }
            return buffer.toByteArray();
        } finally {
            fis.close();
        }
    }
}
