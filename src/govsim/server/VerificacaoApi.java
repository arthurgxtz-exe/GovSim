package govsim.server;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Testes HTTP reais, em porta efêmera, executados por --verificar. */
public final class VerificacaoApi {
    private static void exigir(boolean ok, String mensagem) {
        if (!ok) throw new AssertionError(mensagem);
    }

    private static final class Resposta {
        int status;
        String corpo;
    }

    private static Resposta enviar(int porta, String endpoint, String metodo, String corpo, String sessao) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL("http://127.0.0.1:" + porta + endpoint).openConnection();
        try {
            c.setConnectTimeout(3000);
            c.setReadTimeout(3000);
            c.setRequestMethod(metodo);
            c.setRequestProperty("X-GovSim-Session", sessao);
            if (corpo != null) {
                c.setDoOutput(true);
                c.setRequestProperty("Content-Type", "application/json");
                try (OutputStream out = c.getOutputStream()) { out.write(corpo.getBytes(StandardCharsets.UTF_8)); }
            }
            Resposta r = new Resposta();
            r.status = c.getResponseCode();
            InputStream fluxo = r.status >= 400 ? c.getErrorStream() : c.getInputStream();
            try (InputStream in = fluxo; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                byte[] dados = new byte[1024]; int n;
                while ((n = in.read(dados)) != -1) out.write(dados, 0, n);
                r.corpo = new String(out.toByteArray(), StandardCharsets.UTF_8);
            }
            return r;
        } finally { c.disconnect(); }
    }

    public static void executar() {
        ServidorHttp servidor = new ServidorHttp(0, new File("src/govsim/view"));
        try {
            servidor.iniciar();
            int porta = servidor.portaEfetiva();
            String sessao = UUID.randomUUID().toString();
            String inicio = "{\"modo\":\"normal\",\"dificuldade\":\"DIFICIL\",\"turnos\":0,\"seed\":42}";
            exigir(enviar(porta,"/api/iniciar","POST",inicio,sessao).status == 200,"Início válido");
            String antes = enviar(porta,"/api/estado","GET",null,sessao).corpo;
            List<String> invalidosInicio = new ArrayList<String>(Arrays.asList(
                "", "null", "[]", "true", "{}", inicio + " lixo", inicio + "{}",
                inicio.replace("\"normal\"", "\"outro\""),
                inicio.replace("\"DIFICIL\"", "\"INVALIDA\""),
                inicio.replace("\"seed\":42", "\"seed\":null"),
                inicio.replace("\"seed\":42", "\"seed\":1.2"),
                inicio.replace("\"seed\":42", "\"seed\":9223372036854775808"),
                inicio.replace("\"modo\":\"normal\",", ""),
                inicio.replace("\"dificuldade\":\"DIFICIL\",", ""),
                inicio.replace("\"turnos\":0,", "")
            ));
            for (String campo : new String[]{"modo","dificuldade","turnos"}) {
                String original = campo.equals("modo") ? "\"normal\"" : campo.equals("dificuldade") ? "\"DIFICIL\"" : "0";
                for (String errado : new String[]{"null","true","[]","{}",campo.equals("turnos")?"\"10\"":"12"})
                    invalidosInicio.add(inicio.replace("\""+campo+"\":"+original,"\""+campo+"\":"+errado));
            }
            for (String numero : new String[]{"-1","1","2","25","2147483648","2.5","+3","03",".3","3.","1e999"})
                invalidosInicio.add(inicio.replace("\"turnos\":0","\"turnos\":"+numero));
            String decisao = "{\"decreto\":\"A\",\"turno\":1}";
            List<String> invalidosDecisao = new ArrayList<String>(Arrays.asList(
                "", "[]", "{}", decisao+" lixo", "{\"decreto\":\"A\"}", "{\"turno\":1}",
                "{\"decreto\":\"A\",\"decreto\":\"B\",\"turno\":1}",
                "{\"dados\":{\"decreto\":\"A\",\"turno\":1}}"
            ));
            for(String decreto : new String[]{"null","true","1","[]","{}","\"Z\"","\"\""})
                invalidosDecisao.add(decisao.replace("\"A\"",decreto));
            for(String turno : new String[]{"null","true","[]","{}","\"1\"","0","-1","25","1.5"})
                invalidosDecisao.add(decisao.replace(":1",":"+turno));
            int casos = 0;
            for(int tipo=0;tipo<2;tipo++) for(String corpo : tipo==0?invalidosInicio:invalidosDecisao) {
                Resposta erro = enviar(porta,tipo==0?"/api/iniciar":"/api/decidir","POST",corpo,sessao);
                exigir(erro.status==400,"Esperado 400 para: "+corpo+"; recebido "+erro.status);
                exigir(((Map<?,?>)JsonUtil.lerJson(erro.corpo)).get("erro") instanceof String,"Erro deve ser JSON");
                exigir(antes.equals(enviar(porta,"/api/estado","GET",null,sessao).corpo),"Requisição inválida alterou estado");
                casos++;
            }
            exigir(enviar(porta,"/api/iniciar","GET",null,sessao).status==405,"Método inválido: 405");
            exigir(enviar(porta,"/api/decidir","POST",decisao,sessao).status==200,"Decisão válida: 200");
            exigir(enviar(porta,"/api/decidir","POST",decisao,sessao).status==409,"Turno repetido: 409");
            System.out.println("[APROVADO] API: " + casos + " corpos inválidos retornam 400 sem alterar estado; 200/405/409 preservados.");
        } catch (IOException ex) { throw new AssertionError("Falha no teste HTTP", ex); }
        finally { servidor.parar(); }
    }
}
