# RELATÓRIO DE TESTES E VALIDAÇÃO — GOVSIM
**Projeto Acadêmico:** GovSim — Simulador de Gestão Governamental em Turnos  
**Instituição:** Instituto Federal do Norte de Minas Gerais (IFNMG) — Campus Januária  
**Curso:** Bacharelado em Sistemas de Informação  
**Disciplina:** Introdução à Economia — Segunda Entrega do Economics Games

> **Declaração de Rigor e Integridade Acadêmica:**  
> Este documento separa rigorosamente:
> 1. **Testes Automatizados** executados pela suíte de auditoria em Java (`.\executar.bat --verificar`);
> 2. **Testes Técnicos Manuais e de Segurança HTTP** realizados sobre o servidor e a interface;
> 3. **Testes de Jogabilidade e Estratégias Econômicas** validando diferentes perfis de decisão;
> 4. **Espaço Reservado para os Testes Reais com os Cinco Participantes Externos**, mantido integralmente em branco para preenchimento presencial/real com pessoas de fora do grupo, sem qualquer dado fictício.

## Confiança, recalibração e ciclo de vida das sessões

- Confiança do Mercado: testes em 0,4 / 1,0 / 1,6 confirmam benefícios multiplicados por confiança e penalidades pelo inverso, incluindo a classificação invertida da inflação. Uma regra fiscal benéfica e outra adversa foram disparadas juntas para confirmar o ajuste antes da soma, sem cancelamento prévio.
- Sessões: testes com relógio controlado verificam que não expiram antes de 30 minutos, que acesso renova o prazo, que expiram exatamente no limite e que estado/motor antigos são descartados. Limite de duas sessões no teste confirma descarte LRU e preservação da sessão acessada recentemente. A limpeza total e o encerramento também são cobertos. Os testes integram --verificar e não usam sleeps.
- Configuração de execução: prazo de 30 minutos, máximo de 100 sessões, limpeza periódica a cada minuto e limpeza adicional em cada busca.
- Calibração: custos do FACIL alterados de 1,14 para 1,17; MEDIO de 1,14 para 1,18. DIFICIL e demais parâmetros do enum permaneceram iguais. Não houve alteração na estrutura do motor para atingir as metas.
- O teste HTTP existente também passou com o novo registro: isolamento entre sessões, cookie, header e códigos 400/405/409.
- Saída anterior preservada em simulacao-antes.txt; a saída da nova rodada está em simulacao-20000.txt. Mesmas sementes e 20000 partidas por estratégia/dificuldade.

| Nível | Aleatória antes | Aleatória depois | Meta | Esperta antes | Esperta depois |
| --- | ---: | ---: | ---: | ---: | ---: |
| FACIL | 74,69% | 70,23% | ~70% | 99,39% | 98,28% |
| MEDIO | 41,52% | 34,41% | ~35% | 78,14% | 63,64% |
| DIFICIL | 10,53% | 9,93% | ~10% | 39,75% | 33,63% |

Resultado final: --verificar aprovado, incluindo confiança, 65 corpos HTTP inválidos e expiração/limite de sessões. Compilado com --release 8 e executado no Java 8 instalado. Testes de front e isolamento HTTP também aprovados. A calibração atingiu aproximadamente as metas de aleatória; a taxa da esperta diminuiu com a correção da confiança.

## Atualização corretiva — validação atual

Esta seção registra os testes da versão atual. As tabelas de estratégias e tempos nas seções antigas abaixo são registros históricos, anteriores às dificuldades e ao catálogo de 24 cartas; não representam as taxas atuais. Nenhum teste com participantes externos foi inventado ou preenchido.

As correções foram feitas na ordem solicitada, executando `java -cp bin govsim.Main --verificar` após cada item, com resultado aprovado nas quatro etapas.

| ID | Correção e teste | Evidência / resultado |
| --- | --- | --- |
| TC-01 | Base real dos indicadores em FACIL, MEDIO e DIFICIL | Caixa/aprovação atuais e iniciais iguais aos parâmetros do nível; variação inicial zero nos quatro pilares; caixa aumentado em 10 produz saldo +10; reinício restaura a base real. Integrado a VerificacaoEconomica. |
| TC-02 | Validação estruturada de /api/iniciar e /api/decidir | 65 corpos inválidos retornaram 400 com JSON de erro; comparação do JSON completo antes/depois confirmou estado preservado em cada caso. Integrado a VerificacaoApi e --verificar, com servidor em porta efêmera encerrado no finally. |
| TC-03 | Presença, tipos e valores dos campos da API | Testados campos ausentes, null, boolean, objeto, lista, string no lugar de inteiro, inteiro no lugar de texto, modo/dificuldade/decreto inválidos, turnos fora de limites, frações, overflow, JSON vazio/não objeto, lixo após JSON, documento concatenado, campos aninhados e duplicados. |
| TC-04 | Status HTTP existentes | Início/decisão válidos retornam 200; método inválido 405; repetição de turno válido 409. Validação ocorre antes de criar/obter sessão ou modificar a partida. |
| TC-05 | Recuperação do front | Teste do código real de comunicação de app.js via VM nativa do Node, com DOM/fetch simulados: erro de início preserva estado e renderização anterior; 400 não reenvia; 409 e falha de rede consultam /api/estado; falha da consulta preserva último estado conhecido; resposta JSON quebrada é ressincronizada; mensagem visível e liberação dos controles verificadas. |
| TC-06 | Build limpo Windows | bin apagado com caminho absoluto validado; executar.bat --verificar reconstruiu classes e recurso, com suíte aprovada. |
| TC-07 | Build limpo shell | bin novamente apagado; executar.sh --verificar executado no Git Bash, reconstruindo classes e recurso, com suíte aprovada. |
| TC-08 | Atualização do recurso a cada execução | Nos dois scripts, o JSON de bin foi substituído por um marcador desatualizado antes de executar. Ambos restauraram o recurso de src; hashes SHA-256 iguais ao final. |
| TC-09 | Compatibilidade Java 8 | Compilação com --release 8; versão de bytecode 52 conferida em Main e Requisicao; suíte também executada no runtime Java 1.8.0_501. |

Comandos de reprodução:

```text
executar.bat --verificar
bash executar.sh --verificar
java -cp bin govsim.Main --verificar
node --check src/govsim/view/app.js
node tests/front-comunicacao.cjs
java -cp bin govsim.Main --simular 20000
```

O teste de front usa apenas módulos nativos do Node e não adiciona dependência ao jogo. Ele verifica o fluxo de comunicação, sem substituir testes visuais completos de navegador. A saída da auditoria final está em verificacao-final.txt; a simulação completa está em simulacao-20000.txt.

Resultado da simulação anterior (correções de API/front/empacotamento): 20000 partidas por estratégia e dificuldade (360000 no total), com as mesmas sementes anteriores. As taxas se mantiveram:

| Estratégia | FACIL | MEDIO | DIFICIL |
| --- | ---: | ---: | ---: |
| Aleatória | 74,69% | 41,52% | 10,53% |
| Sempre A | 73,83% | 42,38% | 11,03% |
| Sempre B | 73,87% | 42,10% | 10,91% |
| Gananciosa por aprovação | 0,00% | 0,01% | 0,00% |
| Gananciosa por caixa | 0,00% | 0,00% | 0,00% |
| Esperta | 99,39% | 78,14% | 39,75% |

Esses valores reproduzem a rodada anterior: correções de base inicial, API, front e empacotamento não alteraram as regras de vitória. A meta de aleatória no Médio continua abaixo do observado (35% contra 41,52%); não houve recalibração nesta tarefa.

Contrato validado: /api/iniciar exige modo, dificuldade e turnos; turnos=0 seleciona o padrão do nível e 3–24 define mandato explícito. seed é opcional, mas, quando presente, deve ser inteiro no intervalo de long. /api/decidir exige decreto A/B e turno inteiro entre 1 e 24. Campos de texto aceitam maiúsculas/minúsculas e espaços externos; modo aceita normal, demo, demonstracao ou deterministico. Inteiros não aceitam strings nem notação fracionária/exponencial. Nenhuma regra econômica ou parâmetro de dificuldade foi recalibrado nesta correção.

---

## PARTE I — TESTES AUTOMATIZADOS (SUÍTE DE VERIFICAÇÃO EM JAVA)

Os testes automatizados são executados diretamente via linha de comando por meio de `.\executar.bat --verificar` (ou `java -cp bin govsim.Main --verificar`) e validam matematicamente o motor econômico, o baralho de cartas e a estabilidade da JVM.

| ID | Teste Automatizado | Escopo Verificado | Resultado Obtido | Status |
| :-: | :--- | :--- | :--- | :---: |
| **TA-01** | **Inicialização do Estado e 4 Indicadores Principais** | Verifica valores iniciais (`Caixa = 100.0`, `Aprovação = 60.0%`, `PIB = 2.5%`, `Inflação = 4.5%`, `Juros = 10.5%`), limites mínimo/máximo e presença da carta inicial no Turno 1. | Estado inicial consistente e dentro dos limites. | **APROVADO** |
| **TA-02** | **Auditoria Completa das 14 Cartas e 28 Decretos** | Inspeciona todas as 14 cartas de `BancoEventos` (`EV_PANDEMIA` a `EV_ENDIVIDAMENTO_FAMILIAS`), validando retrato SVG, personagem, diálogo, Decreto A, Decreto B, impactos numéricos finitos e textos didáticos. | 14 cartas e 28 decretos auditados com **0 falhas**. | **APROVADO** |
| **TA-03** | **Consistência Matemática, Clamping e Arredondamento** | Testa saturação nos limites (`Caixa` em `[0, 200]`, `Aprovação` em `[0, 100]`, `PIB` em `[-10, +12]`, `Inflação` em `[0, 35]`), rejeição de `NaN`/`Infinity` e arredondamento a 1 casa decimal. | Limites respeitados sem perda de precisão ou `NaN`. | **APROVADO** |
| **TA-04** | **Agendamento, Maturação em Ondas e Não-Duplicação de Efeitos Atrasados** | Verifica que o Decreto A de `EV_ALTA_DOLAR` agenda 2 ondas (`T+1` e `T+2`), aplica apenas a 1ª onda no Turno 2, aplica a 2ª onda no Turno 3 e esvazia a fila sem duplicar impactos. | Maturação exata nos turnos programados (`T+1` e `T+2`). | **APROVADO** |
| **TA-05** | **Seleção de Cartas sem Repetição e Limpeza Pós-Encerramento** | Garante que nos 10 turnos de uma partida são sorteadas 10 cartas distintas (0 repetições) e que `efeitosPendentes` é limpo ao encerrar a partida. | 10 cartas únicas sorteadas; fila limpa no encerramento. | **APROVADO** |
| **TA-06** | **Rejeição de Entradas Inválidas e Bloqueio Pós-Encerramento** | Verifica que códigos de decreto inválidos lançam `IllegalArgumentException` e tentativas de decidir após o fim da partida lançam `IllegalStateException`. | Exceções lançadas e estado preservado sem corrupção. | **APROVADO** |
| **TA-07** | **Simulação Automatizada de 7 Estratégias de Jogo** | Executa 7 partidas completas com diferentes perfis estratégicos (Expansionista, Austera, Equilibrada, Monetarista, Desenvolvimentista, Alternada e Reinício Limpo). | Todas as 7 estratégias produziram o desfecho econômico esperado. | **APROVADO** |
| **TA-08** | **Benchmark de Estabilidade e Memória (500 Partidas)** | Executa 500 partidas completas (~4.500 turnos processados) + serialização JSON completa de cada estado. | Concluído em **~380–399 ms** (~211 MB de RAM), 0 exceções. | **APROVADO** |

---

## PARTE II — TESTES TÉCNICOS MANUAIS E DE SEGURANÇA HTTP

Testes técnicos executados sobre o servidor HTTP nativo (`govsim.server.ServidorHttp`) na porta `8080` e sobre a interface web inspirada em *Reigns* (`src/govsim/view/`):

| ID | Caso de Teste Técnico Manual | Procedimento Executado | Resultado Observado | Status |
| :-: | :--- | :--- | :--- | :---: |
| **TM-01** | **Compilação Limpa Multi-Versão (`--release 8`)** | Execução de `.\executar.bat` em ambiente com `javac 25.0.4.1` e runtime `JRE 1.8.0_501`. | Compilação concluída com código de saída `0` e sem avisos. | **APROVADO** |
| **TM-02** | **Funcionamento 100% Offline** | Varredura de `index.html`, `style.css` e `app.js` por links externos (`http://` / `https://`) e carregamento sem internet. | **0 dependências externas**; ícones SVG, retratos low-poly e áudio Web Audio API gerados localmente. | **APROVADO** |
| **TM-03** | **Validação de Método HTTP (`HTTP 405`)** | Envio de requisição `GET` para `/api/decidir` e `POST` para `/api/estado`. | Servidor retornou `HTTP 405 Method Not Allowed` sem alterar o estado. | **APROVADO** |
| **TM-04** | **Validação de Decreto Inválido (`HTTP 400`)** | Envio de `POST /api/decidir` com payload `{"decreto":"INVALIDO","turno":1}`. | Servidor retornou `HTTP 400 Bad Request` com mensagem de erro tratada em JSON. | **APROVADO** |
| **TM-05** | **Proteção contra Duplo Clique / Dessincronização de Turno (`HTTP 409`)** | Envio de `POST /api/decidir` com `"turno": 99` divergente do turno ativo no servidor. | Servidor retornou `HTTP 409 Conflict`, impedindo avanço duplo de turno. | **APROVADO** |
| **TM-06** | **Proteção contra Payload Excessivo e Path Traversal** | Envio de corpo JSON > 4 KB e tentativa de acesso a caminhos com `..`. | Requisições bloqueadas com `HTTP 400` / `HTTP 403 Forbidden` (validação `getCanonicalPath`). | **APROVADO** |
| **TM-07** | **Interação Híbrida de Cartas (*Reigns* UX)** | Teste de arraste (mouse/touch), botões laterais (`A`/`B`), teclado (`←`/`→`, `A`/`B`, `Espaço`/`Enter`), *preview dots* nos 4 pilares e cortina superior de decisão. | Animação 3D, inclinação proporcional, pontos de impacto e carta de consequências com memória de cálculo funcionando sem travamentos. | **APROVADO** |
| **TM-08** | **Responsividade em 5 Resoluções** | Verificação de layout em `1920x1080`, `1366x768`, `1280x720`, `768x1024` (tablet) e `390x844` (mobile). | Cartas, pilares no topo e botões adaptam-se sem corte de conteúdo essencial. | **APROVADO** |

---

## PARTE III — TESTES DE JOGABILIDADE E ESTRATÉGIAS ECONÔMICAS

Simulações conduzidas no Modo Determinístico (10 turnos) para comprovar o balanceamento econômico, os *trade-offs* e a impossibilidade de vitória por escolhas extremas irresponsáveis:

| # | Perfil Estratégico Testado | Sequência de Decretos | Desfecho da Partida | Indicadores Finais | Análise Econômica e Pedagógica |
| :-: | :--- | :---: | :--- | :--- | :--- |
| **1** | **Expansionismo Fiscal Radical** (Priorizar gastos e subsídios sempre) | `A, B, A, A, A, B, A...` | **DERROTA FISCAL no Turno 7** (`DERROTA_CAIXA`) | `Caixa: 0,0` \| `Aprov: 100,0%` \| `PIB: 9,8%` \| `Inflação: 9,9%` | O excesso de gastos públicos e subsídios sem contrapartida de arrecadação esgota o Tesouro Nacional no Turno 7, apesar da alta aprovação e PIB aquecido. |
| **2** | **Austeridade Radical Cega** (Cortar gastos e subir juros sempre) | `B, A, B, B, B, A...` | **DERROTA POLÍTICA no Turno 6** (`DERROTA_APROVACAO`) | `Caixa: 84,8` \| `Aprov: 0,0%` \| `PIB: -5,7%` \| `Inflação: 0,0%` | O ajuste fiscal e monetário severo sem proteção social provoca recessão profunda (`PIB -5,7%`) e zera a governabilidade no Turno 6. |
| **3** | **Gestão Equilibrada Pragmática** (Alternando proteção social e responsabilidade fiscal) | `A, A, A, B, B, A, B, A, A, A` | **VITÓRIA no Turno 10** (*Estadista / Excelência*) | `Caixa: 137,8` \| `Aprov: 53,9%` \| `PIB: 2,7%` \| `Inflação: 2,4%` | O governo protege a população em choques agudos, mas realiza superávit fiscal e controle monetário nos momentos oportunos, encerrando com inflação na meta (`2,4%`) e PIB em expansão (`2,7%`). |
| **4** | **Estratégia Monetarista / Controle de Inflação** (Foco em juros e solvência) | `A, A, B, A, B, A, B, A, A, B` | **VITÓRIA no Turno 10** (*Mandato Ortodoxo*) | `Caixa: 157,7` \| `Aprov: 40,8%` \| `PIB: 0,8%` \| `Inflação: 3,2%` | Mantém contas públicas sólidas e inflação controlada (`3,2%`), pagando o *trade-off* de um crescimento econômico mais modesto (`PIB 0,8%`). |
| **5** | **Estratégia Desenvolvimentista com Ajuste Parcial** (Estímulo ao PIB + recomposição fiscal) | `A, B, A, B, A, A, B, A, B, A` | **VITÓRIA no Turno 10** (*Sob Pressão Inflacionária*) | `Caixa: 81,1` \| `Aprov: 5,8%` \| `PIB: 5,1%` \| `Inflação: 23,0%` | Alcança forte crescimento do PIB (`5,1%`) e evita a falência do Caixa (`81,1`), mas o excesso de estímulos gera inflação elevada (`23,0%`), corroendo a aprovação popular (`5,8%`). |
| **6** | **Escolhas Mecânicas Alternadas (`A/B/A/B...`)** | `A, B, A, B, A, B, A, B...` | **DERROTA FISCAL no Turno 8** (`DERROTA_CAIXA`) | `Caixa: 0,0` | Demonstra que clicar mecanicamente alternando esquerda/direita sem ler o contexto econômico de cada carta leva ao colapso fiscal. |
| **7** | **Reinício Completo de Partida Pós-Jogo** | Reinício após vitória/derrota | **Estado Restaurado no Turno 1** | `Caixa: 100,0` \| `Aprov: 60,0%` \| `PIB: 2,5%` \| `Inflação: 4,5%` | Confirma limpeza integral do histórico, das reações da IA e da fila de efeitos atrasados ao iniciar novo mandato. |

---

## PARTE IV — TESTES REAIS COM OS CINCO PARTICIPANTES EXTERNOS

> **Instrução Importante:**  
> As fichas abaixo são reservadas exclusivamente para o registro dos **cinco testes reais com participantes externos ao grupo**, conforme exigido na Segunda Entrega do Economics Games. Não preencha com dados fictícios.

### 4.1. Checklist de Funcionalidades Avaliadas na Sessão com Usuários
- [ ] **Visualização dos 4 Pilares no Topo:** Compreensão de Caixa Público (`100,0`), Aprovação Popular (`60,0%`), PIB (`2,5% a.a.`) e Inflação (`4,5% a.a.`).
- [ ] **Leitura das Cartas de Personagens:** Clareza do diálogo do personagem, contexto econômico e dilema proposto.
- [ ] **Mecânica de Decisão (Arrastar Carta ou Botões A/B):** Uso intuitivo do *swipe* (esquerda/direita) ou botões de decreto e percepção dos pontos de pré-visualização (*dots*) nos pilares.
- [ ] **Carta de Consequências e Memória de Cálculo:** Compreensão imediata dos deltas (`+`/`-`), da explicação didática e da memória de cálculo detalhada.
- [ ] **Fila de Efeitos Econômicos Atrasados e Crônicas:** Percepção dos efeitos agendados para turnos seguintes ($T+1, T+2$), reações da IA (Oposição e Mercado) e gráfico de evolução.

### 4.2. Roteiro Padronizado para Aplicação com Cada Participante (10 a 15 min)
1. **Contextualização Inicial (1 min):** Explique ao participante: *"Você é o Presidente da República em um mandato de 10 turnos. A cada turno, um personagem trará um dilema econômico. Arraste a carta para a esquerda (Decreto A) ou para a direita (Decreto B) — ou use os botões — sem deixar o Caixa Público nem a Aprovação Popular chegarem a zero."*
2. **Exploração Inicial sem Intervenção (2 min):** Observe se o participante compreende sozinho os 4 pilares superiores, os pontos de pré-visualização ao inclinar a carta e o diálogo do personagem.
3. **Execução dos Turnos 1 a 3 com Leitura Didática (4 min):** Peça para o participante ler a **Carta de Consequências** após cada decisão e abrir o modal **Crônicas** para visualizar o gráfico e a fila de efeitos atrasados.
4. **Conclusão da Partida até Vitória ou Derrota (4 min):** Deixe o participante concluir a partida até o Turno 10 ou até ocorrer uma derrota antecipada.
5. **Perguntas Pós-Teste (3 min):**
   - *Ficou claro por que os indicadores subiram ou caíram após suas escolhas?*
   - *Você percebeu que toda decisão envolvia um trade-off (benefício em uma área e custo em outra)?*
   - *Você notou os efeitos atrasados maturando nos turnos seguintes?*
   - *O que você achou da mecânica de cartas e o que sugere melhorar?*

---

### 4.3. Fichas Individuais dos Cinco Participantes Externos

#### Participante 1
- **Nome:** `[Preencher após o teste real]`
- **Perfil / Curso / Período:** `[Ex.: Estudante de Sistemas de Informação / Outro curso]`
- **Data do Teste:** `[DD/MM/2026]`
- **Modo Jogado:** `( ) Mandato Oficial (Aleatório)   ( ) Modo Demo (Guiado)`
- **Resultado da Partida:** `( ) Vitória no Turno 10   ( ) Derrota no Turno ___ (Motivo: Caixa / Aprovação)`
- **Indicadores Finais Alcançados:** `Caixa: ___ | Aprovação: ___% | PIB: ___% | Inflação: ___%`
- **Compreendeu os trade-offs e efeitos atrasados?** `( ) Sim, totalmente   ( ) Parcialmente   ( ) Teve dificuldade`
- **Dificuldades ou Problemas Observados:**
  - `[Registrar aqui o que o participante relatou ou onde teve dúvida]`
- **Sugestões do Participante:**
  - `[Registrar aqui as sugestões feitas pelo participante]`

---

#### Participante 2
- **Nome:** `[Preencher após o teste real]`
- **Perfil / Curso / Período:** `[Preencher]`
- **Data do Teste:** `[DD/MM/2026]`
- **Modo Jogado:** `( ) Mandato Oficial (Aleatório)   ( ) Modo Demo (Guiado)`
- **Resultado da Partida:** `( ) Vitória no Turno 10   ( ) Derrota no Turno ___ (Motivo: Caixa / Aprovação)`
- **Indicadores Finais Alcançados:** `Caixa: ___ | Aprovação: ___% | PIB: ___% | Inflação: ___%`
- **Compreendeu os trade-offs e efeitos atrasados?** `( ) Sim, totalmente   ( ) Parcialmente   ( ) Teve dificuldade`
- **Dificuldades ou Problemas Observados:**
  - `[Registrar aqui o que o participante relatou ou onde teve dúvida]`
- **Sugestões do Participante:**
  - `[Registrar aqui as sugestões feitas pelo participante]`

---

#### Participante 3
- **Nome:** `[Preencher após o teste real]`
- **Perfil / Curso / Período:** `[Preencher]`
- **Data do Teste:** `[DD/MM/2026]`
- **Modo Jogado:** `( ) Mandato Oficial (Aleatório)   ( ) Modo Demo (Guiado)`
- **Resultado da Partida:** `( ) Vitória no Turno 10   ( ) Derrota no Turno ___ (Motivo: Caixa / Aprovação)`
- **Indicadores Finais Alcançados:** `Caixa: ___ | Aprovação: ___% | PIB: ___% | Inflação: ___%`
- **Compreendeu os trade-offs e efeitos atrasados?** `( ) Sim, totalmente   ( ) Parcialmente   ( ) Teve dificuldade`
- **Dificuldades ou Problemas Observados:**
  - `[Registrar aqui o que o participante relatou ou onde teve dúvida]`
- **Sugestões do Participante:**
  - `[Registrar aqui as sugestões feitas pelo participante]`

---

#### Participante 4
- **Nome:** `[Preencher após o teste real]`
- **Perfil / Curso / Período:** `[Preencher]`
- **Data do Teste:** `[DD/MM/2026]`
- **Modo Jogado:** `( ) Mandato Oficial (Aleatório)   ( ) Modo Demo (Guiado)`
- **Resultado da Partida:** `( ) Vitória no Turno 10   ( ) Derrota no Turno ___ (Motivo: Caixa / Aprovação)`
- **Indicadores Finais Alcançados:** `Caixa: ___ | Aprovação: ___% | PIB: ___% | Inflação: ___%`
- **Compreendeu os trade-offs e efeitos atrasados?** `( ) Sim, totalmente   ( ) Parcialmente   ( ) Teve dificuldade`
- **Dificuldades ou Problemas Observados:**
  - `[Registrar aqui o que o participante relatou ou onde teve dúvida]`
- **Sugestões do Participante:**
  - `[Registrar aqui as sugestões feitas pelo participante]`

---

#### Participante 5
- **Nome:** `[Preencher após o teste real]`
- **Perfil / Curso / Período:** `[Preencher]`
- **Data do Teste:** `[DD/MM/2026]`
- **Modo Jogado:** `( ) Mandato Oficial (Aleatório)   ( ) Modo Demo (Guiado)`
- **Resultado da Partida:** `( ) Vitória no Turno 10   ( ) Derrota no Turno ___ (Motivo: Caixa / Aprovação)`
- **Indicadores Finais Alcançados:** `Caixa: ___ | Aprovação: ___% | PIB: ___% | Inflação: ___%`
- **Compreendeu os trade-offs e efeitos atrasados?** `( ) Sim, totalmente   ( ) Parcialmente   ( ) Teve dificuldade`
- **Dificuldades ou Problemas Observados:**
  - `[Registrar aqui o que o participante relatou ou onde teve dúvida]`
- **Sugestões do Participante:**
  - `[Registrar aqui as sugestões feitas pelo participante]`

---

### 4.4. Consolidação dos Problemas Identificados nos Testes com Usuários
*(Preencher após a aplicação com os 5 participantes externos)*

| ID | Problema / Dúvida Identificada | Frequência (Quantos participantes relataram) | Severidade (Baixa / Média / Alta) |
| :-: | :--- | :---: | :---: |
| **P1** | `[Aguardando execução dos testes com usuários]` | `_ / 5` | `[ ]` |
| **P2** | `[Aguardando execução dos testes com usuários]` | `_ / 5` | `[ ]` |
| **P3** | `[Aguardando execução dos testes com usuários]` | `_ / 5` | `[ ]` |

### 4.5. Ajustes Realizados após os Testes com Usuários
- `[Descrever aqui quais ajustes de interface, texto ou balanceamento foram aplicados após ouvir os 5 participantes externos]`


## Revisão em quatro fases — 04/11/2026

Fase 1: pointer events com captura, cancelamento e limiar de 35%; hover/foco restaurados. Causa: transform bloqueado no movimento reduzido e hover removido. Testes do front aprovados.

Fase 2: resultado compacto, explicação expansível, categorias acentuadas, turno de chegada, 24 diálogos completos. Escalas 0–200 / 0–100 / −10–12 / 0–35 confirmadas. Catálogo comparado campo a campo: apenas diálogo mudou. Fixture atualizada apenas no conteúdo do diálogo, preservando os tipos decimais da serialização Java. Compilação --release 8 aprovada; a restrição do ambiente exigiu executar o compilador fora do sandbox.

Fase 3: contexto de áudio único, resume seguro, ganho mestre e níveis .35/.60/.85; mute persistente, seis timbres e confirmação ao ativar. Causa: ganhos muito baixos, ausência de níveis e rejeição assíncrona não tratada. Testes automatizados e ciclo do botão no navegador aprovados, console sem erros.

Diálogos reescritos:
- Emergência Sanitária e Paralisação Econômica (EV_PANDEMIA)
- Fuga de Capitais e Disparada do Dólar (EV_ALTA_DOLAR)
- Disparo no Preço da Cesta Básica (EV_PRECO_ALIMENTOS)
- Reservatórios Secos e Risco de Apagão (EV_CRISE_ENERGETICA)
- Fechamento de Fábricas e Desemprego (EV_DESEMPREGO_ELEVADO)
- Alerta no Tesouro e Risco da Dívida (EV_CRISE_FISCAL)
- Guerra Internacional e Choque do Petróleo (EV_GUERRA_INTERNACIONAL)
- Tombo na Arrecadação e Disputa no Congresso (EV_QUEDA_ARRECADACAO)
- Euforia de Consumo e Fábricas no Limite (EV_CRESCIMENTO_AQUECIDO)
- Barreiras Externas e Queda nas Exportações (EV_COMERCIO_EXTERIOR)
- Crise nas Universidades e Escolas Técnicas (EV_EDUCACAO_CIENCIA)
- Estados à Beira da Falência Pedem Socorro (EV_CRISE_ESTADOS)
- Greve Geral do Funcionalismo Público (EV_GREVE_GERAL)
- Famílias Endividadas e Varejo Travado (EV_ENDIVIDAMENTO_FAMILIAS)
- Seca e preço dos alimentos (EV_SECA)
- Vencimento da dívida pública (EV_DIVIDA)
- Automação e desemprego (EV_AUTOMACAO)
- Expansão acelerada do crédito (EV_CREDITO)
- Escassez de energia (EV_ENERGIA)
- Crise financeira municipal (EV_MUNICIPIOS)
- Fechamento de fábricas (EV_INDUSTRIA)
- Boom imobiliário (EV_IMOVEIS)
- Congestionamento nos portos (EV_PORTOS)
- Queda da renda informal (EV_INFORMAIS)

Simulação antes e depois: saída idêntica, 20.000 partidas por estratégia/dificuldade.

```text
Dificuldade Estratégia Vitória% Caixa% Aprovação% Ambos% Hiper% Depressão% | Médias Caixa Aprovação PIB Inflação | Estadista%
FACIL aleatória 70.23  17.80  11.98   0.00   0.00   0.00 |   53.77  53.63   2.67   5.66 | 0.39
FACIL sempre A A    69.73  17.94  12.34   0.00   0.00   0.00 |   53.95  53.23   2.67   5.69 | 0.31
FACIL sempre B B    69.23  17.89  12.89   0.00   0.00   0.00 |   53.96  53.46   2.67   5.66 | 0.28
FACIL aprovação 0.00 100.00   0.00   0.00   0.00   0.00 |    0.00  99.32   5.79  10.69 | 0.00
FACIL caixa 0.00   0.00 100.00   0.00   0.00   0.00 |  143.05   0.00  -0.67   2.60 | 0.00
FACIL esperta 98.28   0.03   1.68   0.01   0.00   0.00 |   44.97  63.52   2.59   7.27 | 0.05
MEDIO aleatória 34.41  36.34  28.73   0.22   0.00   0.31 |   44.09  41.57   1.85   6.69 | 0.07
MEDIO sempre A A    34.17  36.02  29.22   0.23   0.00   0.37 |   44.48  41.15   1.85   6.69 | 0.07
MEDIO sempre B B    34.71  36.13  28.61   0.18   0.00   0.38 |   44.15  41.59   1.86   6.68 | 0.10
MEDIO aprovação 0.00 100.00   0.00   0.00   0.00   0.00 |    0.00  96.94   4.62  11.21 | 0.00
MEDIO caixa 0.00   0.00  99.84   0.00   0.00   0.16 |  135.96   0.01  -0.24   3.07 | 0.00
MEDIO esperta 63.64   1.48  29.63   0.61   0.11   4.55 |   35.96  30.65   0.14   8.33 | 0.01
DIFICIL aleatória 9.93  54.78  34.29   0.76   0.00   0.25 |   30.69  39.97   1.77   7.32 | 0.04
DIFICIL sempre A A     9.83  54.81  34.45   0.67   0.01   0.24 |   30.59  39.86   1.74   7.29 | 0.01
DIFICIL sempre B B    10.00  54.88  34.22   0.63   0.01   0.26 |   30.33  39.98   1.76   7.34 | 0.01
DIFICIL aprovação 0.00 100.00   0.00   0.00   0.00   0.00 |    0.00  90.90   3.88  10.23 | 0.00
DIFICIL caixa 0.00   0.00 100.00   0.00   0.00   0.00 |  107.38   0.00   0.92   2.97 | 0.00
DIFICIL esperta 33.63   1.49  59.98   1.50   0.09   3.32 |   36.25  16.36  -0.07   7.93 | 0.01
```

Resultado do turno conferido em desktop e 390×844: painel sem rolagem; explicação expandida com rolagem interna e botão de avanço visível. Recursos e sons offline, sem dependências externas.
