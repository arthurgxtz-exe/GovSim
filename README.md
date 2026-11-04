# GovSim — Simulador de Gestão Governamental em Turnos

**Instituição:** Instituto Federal do Norte de Minas Gerais (IFNMG) — Campus Januária  
**Curso:** Bacharelado em Sistemas de Informação  
**Disciplina:** Introdução à Economia — *Economics Games (2ª Entrega: MVP, Validação e Demonstração)*

---

## 1. Sobre o Projeto

O **GovSim** é um jogo educativo de simulação econômica em turnos — com mecânica e estética de cartas inspiradas no jogo ***Reigns*** — no qual o jogador assume o papel de **Presidente da República** durante um mandato de **10 turnos**. A cada turno, um personagem (Ministro da Economia, Presidente do Banco Central, Ministro da Saúde, Ministra da Educação, Representante dos Trabalhadores, Empresária, Governador, Líder da Oposição, Chanceler ou Representante da População) apresenta uma carta com um dilema econômico ou político, e o jogador deve deslizar a carta para a esquerda (**Decreto A**) ou para a direita (**Decreto B**) — ou utilizar os botões laterais e atalhos de teclado.

O simulador demonstra na prática:
- **Política Fiscal e Política Monetária** (gastos públicos, tributação, subsídios e taxa básica de juros Selic).
- **Os 4 Indicadores Principais da Proposta Original:**
  1. **Caixa Público** (`pts / R$ bi`)
  2. **Aprovação Popular** (`%`)
  3. **PIB (Crescimento)** (`% a.a.`)
  4. **Inflação (IPCA)** (`% a.a.`)
- **Trade-offs governamentais:** Nenhuma decisão é gratuita; estimular o PIB e a aprovação via gastos reduz o Caixa Público e pode pressionar a inflação, enquanto ajustes fiscais e altas de juros controlam a inflação e preservam o caixa ao custo da atividade econômica e da popularidade.
- **Efeitos Econômicos Atrasados (Defasagem Temporal):** Mudanças na taxa de juros, pacotes fiscais e investimentos em infraestrutura produzem efeitos em ondas nos turnos seguintes ($T+1$ e $T+2$).
- **IA Baseada em Regras:** Reações dinâmicas e explicadas da **Oposição Parlamentar** e do **Mercado Financeiro**.

---

## 2. Tecnologias Utilizadas

- **Java (JDK Puro):** Sem uso de frameworks externos (sem Spring Boot, sem bancos de dados externos).
- **Servidor HTTP Nativo:** `com.sun.net.httpserver.HttpServer`.
- **Interface Web (View):** HTML5, CSS3 e JavaScript puro (Vanilla JS + Canvas 2D nativo para o gráfico de evolução), funcionando **100% offline**.

---

## 3. Arquitetura do Projeto

```text
Economics Games/
├── executar.bat                      # Script de compilação e execução rápida no Windows
├── executar.sh                       # Script de compilação e execução rápida no Linux
├── README.md                         # Documentação geral e instruções de execução
├── REGRAS_ECONOMICAS.md              # Documentação detalhada das fórmulas e 14 cartas econômicas
├── RELATORIO_DE_TESTES.md            # Relatório de testes (automatizados, manuais, jogabilidade e 5 fichas de usuários)
├── AUDITORIA_FINAL_GOVSIM.md         # Relatório técnico completo da auditoria de software, economia e UX
├── bin/                              # Classes Java pré-compiladas (compatíveis com Java 8+)
└── src/
    └── govsim/
        ├── Main.java                 # Inicialização do servidor HTTP e suíte de verificação (--verificar)
        ├── model/
        │   ├── Estado.java           # Estado completo do mandato, indicadores e filas
        │   ├── Indicador.java        # Entidade dos 4 indicadores principais com limites e status
        │   ├── Evento.java           # Carta de personagem e dilema econômico/político do turno
        │   ├── Decreto.java          # Opção de decisão governamental (Decreto A ou B)
        │   ├── EfeitoAtrasado.java   # Consequência econômica programada para turnos futuros
        │   ├── ReacaoAgente.java     # Reação da IA baseada em regras (Oposição e Mercado)
        │   └── HistoricoTurno.java   # Registro detalhado e explicação didática de cada turno
        ├── engine/
        │   ├── MotorEconomico.java   # Regras econômicas determinísticas, interações e vitória/derrota
        │   ├── BancoEventos.java     # Baralho de 14 cartas de personagens/crises e seleção sem repetição
        │   └── AvaliadorIA.java      # Regras de reação da Oposição e do Mercado Financeiro
        ├── server/
        │   ├── ServidorHttp.java     # Servidor HTTP nativo e endpoints REST (/api/estado, /api/iniciar, /api/decidir)
        │   └── JsonUtil.java         # Serializador e leitor JSON em Java puro
        └── view/
            ├── index.html            # Mesa de cartas inspirada em Reigns com 4 pilares SVG no topo
            ├── style.css             # Estilização responsiva, animações 3D e modo escuro (100% offline)
            └── app.js                # Física de swipe, retratos SVG low-poly, Web Audio API e gráfico Canvas 2D
```

---

## 4. Como Executar

### No Windows
Basta dar um duplo clique em **`executar.bat`** ou executar no terminal (PowerShell / CMD):
```bat
.\executar.bat
```
O script compila automaticamente o código (caso o `javac` esteja presente) ou utiliza os binários pré-compilados em `bin/`, inicia o servidor local em **`http://localhost:8080`** e abre o navegador padrão.

### No Linux / macOS
No terminal, dê permissão de execução e rode **`executar.sh`**:
```bash
chmod +x executar.sh
./executar.sh
```
Em seguida, acesse **`http://localhost:8080`** no navegador.

### Verificação Automatizada via Linha de Comando
Para executar a bateria interna de testes do motor econômico no terminal:
```bash
java -cp bin govsim.Main --verificar
```

### Interface offline e sessões

Cada aba recebe um ID próprio em X-GovSim-Session. Clientes sem header recebem cookie govsim (HttpOnly, SameSite=Strict). Estado e motor são isolados por sessão; as decisões continuam sincronizadas pelo Estado. Recarregar a página abre uma nova partida nessa aba. Sessões expiram após 30 minutos sem acesso; o limite é 100, descartando a menos recentemente acessada quando necessário. A limpeza periódica roda a cada minuto.

A tela inicial apresenta três cartas de tutorial e seleção de dificuldade. A fila T+1/T+2 fica no cabeçalho, com detalhes expansíveis. Prévias de Médio/Difícil mostram direção e intensidade; relatórios preservam os valores realizados. Sparklines mostram trajetórias dos pilares, e o encerramento apresenta gráfico e lições extraídas das decisões.

Fontes WOFF2 (Geist Mono, licença SIL OFL em view/fonts/OFL.txt) e cenários SVG são locais; não há CDN. O jogo respeita prefers-reduced-motion e oferece setas, símbolos de alerta, foco visível e descrições textuais dos gráficos. Risco: caixa/aprovação abaixo de 20, PIB negativo ou inflação a partir de 20.

Teste HTTP: com o servidor na porta 18765, execute node tests/server-sessoes.cjs. Verifica isolamento, cookie, header, códigos 400/405/409 e fonte local. Esse script usa apenas módulos nativos do Node para testes; o jogo continua Java/JS puro.
