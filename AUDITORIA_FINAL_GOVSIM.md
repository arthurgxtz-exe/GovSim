# AUDITORIA TÉCNICA TOTAL — GOVSIM
**Projeto Acadêmico:** GovSim — Simulador de Gestão Governamental em Turnos (*Reigns Edition*)  
**Instituição:** Instituto Federal do Norte de Minas Gerais (IFNMG) — Campus Januária  
**Curso:** Bacharelado em Sistemas de Informação  
**Disciplina:** Introdução à Economia — Segunda Entrega do Economics Games  
**Equipe Auditora Multidisciplinar:** Engenharia de Software Java, Desenvolvimento Frontend (HTML/CSS/JS), Game Design de Cartas, Economia Aplicada, QA/Testes Automatizados, UX/UI e Segurança de Software.

---

## 1. Resumo Executivo da Auditoria

Esta auditoria técnica inspecionou integralmente o projeto **GovSim**, abrangendo código-fonte Java (`src/govsim/`), servidor HTTP nativo (`com.sun.net.httpserver.HttpServer`), motor econômico determinístico, baralho de cartas, interface interativa inspirada em *Reigns* (`src/govsim/view/`), scripts de compilação/execução (`executar.bat` e `executar.sh`) e documentação acadêmica (`README.md`, `REGRAS_ECONOMICAS.md`, `RELATORIO_DE_TESTES.md`).

O trabalho foi conduzido em **dois ciclos iterativos de inspeção, correção, recompilação, testes automatizados e testes de integração HTTP**, antecedidos por uma cópia de segurança integral (*backup* de 41 arquivos). Durante a auditoria:
- Foram identificados e corrigidos **9 achados técnicos e documentais** (0 críticos abertos, 0 altos abertos, 0 médios abertos, 0 baixos abertos ao final do Ciclo 2).
- A suíte de verificação automatizada (`.\executar.bat --verificar`) foi expandida para **8 baterias de testes determinísticos**, incluindo auditoria das 14 cartas / 28 decretos, maturação em ondas ($T+1$ e $T+2$) de efeitos atrasados, simulação das 7 estratégias de jogabilidade e *benchmark* de estabilidade com **500 partidas completas (~4.500 turnos)** executadas em **~380–399 ms** sem vazamento de memória ou exceções.
- A segurança e robustez do servidor HTTP foram endurecidas com validação estrita de métodos (`HTTP 405`), limite de *payload* (`4096 bytes`), proteção contra *Path Traversal* via `getCanonicalPath()`, escape completo de caracteres de controle JSON e trava de sincronização de turno (`HTTP 409 Conflict`) contra duplos cliques/submissões concorrentes.

---

## 2. Estado Inicial Encontrado no Projeto

Antes das intervenções desta auditoria, foi realizado o levantamento completo do estado do repositório:
1. **Estrutura Arquitetural:** Projeto organizado em arquitetura MVC / Camadas (`govsim.model`, `govsim.engine`, `govsim.server`, `govsim.view`), sem dependências externas, utilizando exclusivamente JDK padrão e HTML5/CSS3/JS puro.
2. **Ambiente de Compilação/Execução:** O sistema operacional Windows possuía `java` (`JRE 1.8.0_501`) no `PATH` global e o compilador `javac` (`JDK 25.0.4.1`) instalado no diretório da extensão Java do VS Code. O script `executar.bat` já continha detecção automática do `javac` com flag `--release 8 -Xlint:-options`, gerando bytecode versão `52.0` compatível com qualquer JVM $\ge 8$.
3. **Preservação e Backup:** Antes de qualquer modificação, todos os **41 arquivos** existentes (`src/`, `bin/`, scripts e documentos) foram copiados integralmente para o diretório seguro de backup pré-auditoria.
4. **Pontos que exigiam aprimoramento identificados na inspeção inicial:**
   - Risco de condição de corrida (*race condition*) caso o jogador disparasse um duplo clique rápido ou arraste + tecla simultâneos no mesmo turno (ausência de validação do número do turno no `POST /api/decidir`).
   - Efeitos atrasados pendentes permaneciam na lista `efeitosPendentes` após o encerramento antecipado da partida (`encerrarPartida`), podendo causar confusão visual nos contadores de rodapé.
   - Ausência do bloco expansível de **Memória de Cálculo Detalhada** na Carta de Consequências e do quadro de **Termômetro Institucional (IA)** dentro do modal de Crônicas após a migração para o layout *Reigns*.
   - Necessidade de endurecimento do `ServidorHttp` (limite de tamanho de *payload* e checagem canônica de diretório) e do `JsonUtil` (escape Unicode para caracteres de controle `< 0x20`).
   - Documentação `REGRAS_ECONOMICAS.md`, `RELATORIO_DE_TESTES.md` e `README.md` ainda referenciava 12 eventos em vez das 14 cartas do baralho *Reigns* e precisava separar explicitamente testes automatizados, manuais, de jogabilidade e o espaço em branco para os 5 participantes externos.

---

## 3. Lista Completa de Arquivos Analisados

| # | Caminho do Arquivo | Camada / Responsabilidade | Status Pós-Auditoria |
| :-: | :--- | :--- | :---: |
| 1 | [`GovSim_Etapa1_Proposta.pdf`](file:///c:/Users/Arthur/Desktop/Economics%20Games/GovSim_Etapa1_Proposta.pdf) | Proposta acadêmica original da Etapa 1 | Preservado (Referência) |
| 2 | [`src/govsim/Main.java`](file:///c:/Users/Arthur/Desktop/Economics%20Games/src/govsim/Main.java) | Ponto de entrada, CLI e suíte de 8 testes automatizados (`--verificar`) | Auditado e Expandido |
| 3 | [`src/govsim/model/Estado.java`](file:///c:/Users/Arthur/Desktop/Economics%20Games/src/govsim/model/Estado.java) | Estado central do mandato, 4 indicadores, filas e histórico | Auditado e Corrigido |
| 4 | [`src/govsim/model/Indicador.java`](file:///c:/Users/Arthur/Desktop/Economics%20Games/src/govsim/model/Indicador.java) | Entidade matemática dos indicadores, limites (*clamping*) e status | Auditado e Aprovado |
| 5 | [`src/govsim/model/Evento.java`](file:///c:/Users/Arthur/Desktop/Economics%20Games/src/govsim/model/Evento.java) | Entidade da carta de personagem, diálogo, contexto e par de decretos | Auditado e Aprovado |
| 6 | [`src/govsim/model/Decreto.java`](file:///c:/Users/Arthur/Desktop/Economics%20Games/src/govsim/model/Decreto.java) | Entidade de decisão (`A` ou `B`), impactos imediatos e efeitos atrasados | Auditado e Aprovado |
| 7 | [`src/govsim/model/EfeitoAtrasado.java`](file:///c:/Users/Arthur/Desktop/Economics%20Games/src/govsim/model/EfeitoAtrasado.java) | Entidade de efeito econômico defasado ($T+1, T+2$) | Auditado e Aprovado |
| 8 | [`src/govsim/model/ReacaoAgente.java`](file:///c:/Users/Arthur/Desktop/Economics%20Games/src/govsim/model/ReacaoAgente.java) | Entidade de reação da IA baseada em regras (Oposição e Mercado) | Auditado e Aprovado |
| 9 | [`src/govsim/model/HistoricoTurno.java`](file:///c:/Users/Arthur/Desktop/Economics%20Games/src/govsim/model/HistoricoTurno.java) | Registro auditável de cada turno e memória de cálculo | Auditado e Aprovado |
| 10 | [`src/govsim/engine/MotorEconomico.java`](file:///c:/Users/Arthur/Desktop/Economics%20Games/src/govsim/engine/MotorEconomico.java) | Motor de cálculo determinístico, interações endógenas e vitória/derrota | Auditado e Aprovado |
| 11 | [`src/govsim/engine/BancoEventos.java`](file:///c:/Users/Arthur/Desktop/Economics%20Games/src/govsim/engine/BancoEventos.java) | Catálogo de 14 cartas de personagens, 28 decretos e sorteio sem repetição | Auditado e Aprovado |
| 12 | [`src/govsim/engine/AvaliadorIA.java`](file:///c:/Users/Arthur/Desktop/Economics%20Games/src/govsim/engine/AvaliadorIA.java) | Avaliador de reações políticas e financeiras baseado em regras | Auditado e Aprovado |
| 13 | [`src/govsim/server/ServidorHttp.java`](file:///c:/Users/Arthur/Desktop/Economics%20Games/src/govsim/server/ServidorHttp.java) | Servidor HTTP nativo, rotas REST e segurança de arquivos estáticos | Auditado e Endurecido |
| 14 | [`src/govsim/server/JsonUtil.java`](file:///c:/Users/Arthur/Desktop/Economics%20Games/src/govsim/server/JsonUtil.java) | Serializador e parser JSON em Java puro | Auditado e Endurecido |
| 15 | [`src/govsim/view/index.html`](file:///c:/Users/Arthur/Desktop/Economics%20Games/src/govsim/view/index.html) | Estrutura semântica da interface de cartas inspirada em *Reigns* | Auditado e Aprimorado |
| 16 | [`src/govsim/view/style.css`](file:///c:/Users/Arthur/Desktop/Economics%20Games/src/govsim/view/style.css) | Estilização escura, animações 3D e *media queries* para 5 resoluções | Auditado e Aprimorado |
| 17 | [`src/govsim/view/app.js`](file:///c:/Users/Arthur/Desktop/Economics%20Games/src/govsim/view/app.js) | Física de *swipe*, retratos SVG low-poly, som Web Audio API e Canvas | Auditado e Aprimorado |
| 18 | [`executar.bat`](file:///c:/Users/Arthur/Desktop/Economics%20Games/executar.bat) | Script de compilação e inicialização automática no Windows | Auditado e Aprovado |
| 19 | [`executar.sh`](file:///c:/Users/Arthur/Desktop/Economics%20Games/executar.sh) | Script de compilação e inicialização automática no Linux/macOS | Auditado e Aprovado |
| 20 | [`REGRAS_ECONOMICAS.md`](file:///c:/Users/Arthur/Desktop/Economics%20Games/REGRAS_ECONOMICAS.md) | Documentação matemática das fórmulas e tabela das 14 cartas | Atualizado |
| 21 | [`RELATORIO_DE_TESTES.md`](file:///c:/Users/Arthur/Desktop/Economics%20Games/RELATORIO_DE_TESTES.md) | Relatório separado em 4 categorias + 5 fichas em branco para usuários | Atualizado |
| 22 | [`README.md`](file:///c:/Users/Arthur/Desktop/Economics%20Games/README.md) | Guia geral de arquitetura e execução | Atualizado |

---

## 4. Lista Completa de Problemas Encontrados e Registro de Correções

| ID | Descrição do Problema | Localização (Arquivo / Classe / Método) | Gravidade | Impacto no Sistema | Correção Aplicada | Status |
| :-: | :--- | :--- | :---: | :--- | :--- | :---: |
| **AUD-01** | Ausência de verificação de sincronia de turno no endpoint `POST /api/decidir`. Um duplo clique ou requisição concorrente podia processar dois turnos seguidos involuntariamente. | [`ServidorHttp.java`](file:///c:/Users/Arthur/Desktop/Economics%20Games/src/govsim/server/ServidorHttp.java#L106-L143) (`HandlerDecidir.handle`) e [`app.js`](file:///c:/Users/Arthur/Desktop/Economics%20Games/src/govsim/view/app.js#L430-L445) (`executarDecisao`) | **ALTO** | Risco de avanço duplo de turno sob clique rápido ou latência. | O frontend agora envia `"turno": estadoAtual.turnoAtual` e o backend valida `turnoRequisitado != estado.getTurnoAtual()`, retornando `HTTP 409 Conflict` se houver dessincronia. | **CORRIGIDO E VALIDADO** |
| **AUD-02** | Ao encerrar uma partida por vitória ou derrota (`encerrarPartida`), efeitos atrasados agendados em turnos anteriores permaneciam na lista `efeitosPendentes`. | [`Estado.java`](file:///c:/Users/Arthur/Desktop/Economics%20Games/src/govsim/model/Estado.java#L92-L101) (`encerrarPartida`) | **MÉDIO** | Exibição residual de efeitos pendentes no rodapé após o fim da partida. | Adicionada limpeza explícita `this.efeitosPendentes.clear()` e `this.eventoAtual = null` dentro de `Estado.encerrarPartida()`. | **CORRIGIDO E VALIDADO** |
| **AUD-03** | A Carta de Consequências pós-turno não exibia a lista detalhada da memória de cálculo (`detalhesCalculo` gerada pelo backend), reduzindo a transparência didática das fórmulas. | [`index.html`](file:///c:/Users/Arthur/Desktop/Economics%20Games/src/govsim/view/index.html#L209-L212), [`style.css`](file:///c:/Users/Arthur/Desktop/Economics%20Games/src/govsim/view/style.css#L583-L618) e [`app.js`](file:///c:/Users/Arthur/Desktop/Economics%20Games/src/govsim/view/app.js#L617-L625) | **MÉDIO** | O jogador via os deltas finais, mas não conseguia inspecionar a decomposição (direto vs. endógeno vs. IA). | Adicionado componente expansível `<details class="conseq-memoria">` na Carta de Consequências populado com `u.detalhesCalculo`. | **CORRIGIDO E VALIDADO** |
| **AUD-04** | O modal de Crônicas do Mandato não listava as justificativas completas do Termômetro Institucional (`reacoesAtuais` da IA: Oposição e Mercado). | [`index.html`](file:///c:/Users/Arthur/Desktop/Economics%20Games/src/govsim/view/index.html#L276-L279) e [`app.js`](file:///c:/Users/Arthur/Desktop/Economics%20Games/src/govsim/view/app.js#L714-L736) | **BAIXO** | Menor visibilidade das regras acionadas pela IA durante a consulta ao modal de Crônicas. | Adicionada seção *"Termômetro Institucional (Oposição e Mercado)"* com postura, declaração, regra acionada e efeito prático no modal. | **CORRIGIDO E VALIDADO** |
| **AUD-05** | `ServidorHttp.lerCorpoRequisicao` não limitava o tamanho do corpo HTTP recebido, e `HandlerArquivosEstaticos` verificava apenas `caminho.contains("..")` sem validar `getCanonicalPath()`. | [`ServidorHttp.java`](file:///c:/Users/Arthur/Desktop/Economics%20Games/src/govsim/server/ServidorHttp.java#L164-L234) | **MÉDIO** | Vulnerabilidade teórica a requisições gigantes ou *path traversal* codificado. | Adicionado limite `LIMITE_CORPO_BYTES = 4096` e verificação `canonicalTarget.startsWith(canonicalBase)` antes de servir qualquer arquivo. | **CORRIGIDO E VALIDADO** |
| **AUD-06** | `JsonUtil.quote` escapava `\n`, `\r`, `\t`, `\"` e `\\`, mas não escapava outros caracteres de controle ASCII (`c < 0x20`). | [`JsonUtil.java`](file:///c:/Users/Arthur/Desktop/Economics%20Games/src/govsim/server/JsonUtil.java#L237-L268) (`quote`) | **BAIXO** | Possibilidade de JSON inválido caso algum texto contivesse caractere de controle. | Adicionado escape `String.format("\\u%04x", (int) c)` para qualquer caractere `c < 0x20`. | **CORRIGIDO E VALIDADO** |
| **AUD-07** | Timers visuais (`timerSwipeDecisao` e `timersFlash`) no frontend podiam continuar pendentes se o usuário clicasse em *"Reiniciar"* no meio de uma transição. | [`app.js`](file:///c:/Users/Arthur/Desktop/Economics%20Games/src/govsim/view/app.js#L350-L435) (`iniciarOuReiniciar`, `executarDecisao`) | **BAIXO** | Flash residual nos pilares após reinício rápido de partida. | Armazenamento e cancelamento explícito (`clearTimeout`) de todos os timers ao reiniciar partida e ao redimensionar a janela (`resize`). | **CORRIGIDO E VALIDADO** |
| **AUD-08** | No Ciclo 2, verificou-se que `HandlerIniciar` aceitava `"demo"` e `"demonstracao"`, mas não o sinônimo `"deterministico"` no campo `modo`, e uma linha de log no `Main.java` continha caractere acentuado (`pós-encerramento`) no console OEM do Windows. | [`ServidorHttp.java`](file:///c:/Users/Arthur/Desktop/Economics%20Games/src/govsim/server/ServidorHttp.java#L93-L95) e [`Main.java`](file:///c:/Users/Arthur/Desktop/Economics%20Games/src/govsim/Main.java#L242) | **BAIXO** | Chamadas de API com `"modo":"deterministico"` caíam no modo aleatório; console Windows exibia caractere truncado. | Adicionado suporte a `"deterministico".equalsIgnoreCase(modo)` em `HandlerIniciar` e padronizado log ASCII em `Main.java`. | **CORRIGIDO E VALIDADO** |
| **AUD-09** | `REGRAS_ECONOMICAS.md`, `RELATORIO_DE_TESTES.md` e `README.md` listavam 12 eventos em vez das 14 cartas do baralho atual e precisavam separar as 4 categorias de testes. | [`REGRAS_ECONOMICAS.md`](file:///c:/Users/Arthur/Desktop/Economics%20Games/REGRAS_ECONOMICAS.md), [`RELATORIO_DE_TESTES.md`](file:///c:/Users/Arthur/Desktop/Economics%20Games/RELATORIO_DE_TESTES.md), [`README.md`](file:///c:/Users/Arthur/Desktop/Economics%20Games/README.md) | **MÉDIO** | Divergência entre documentação escrita e as 14 cartas implementadas no código. | Documentos atualizados com as 14 cartas (`EV_EDUCACAO_CIENCIA` e `EV_CRISE_ESTADOS` incluídas) e separação estrita das 4 seções de teste. | **CORRIGIDO E VALIDADO** |

---

## 5. Classificação de Gravidade dos Problemas

- **CRÍTICO (`0` encontrados):** Nenhum erro impedia a compilação, inicialização ou conclusão de partidas.
- **ALTO (`1` encontrado -> `0` pendentes):** `AUD-01` (proteção contra submissão duplicada de turno via `HTTP 409`).
- **MÉDIO (`4` encontrados -> `0` pendentes):** `AUD-02` (limpeza de efeitos pendentes pós-encerramento), `AUD-03` (memória de cálculo expansível na carta de consequências), `AUD-05` (limite de payload e canonical path), `AUD-09` (atualização documental para 14 cartas).
- **BAIXO (`4` encontrados -> `0` pendentes):** `AUD-04` (termômetro da IA no modal de Crônicas), `AUD-06` (escape Unicode em `JsonUtil`), `AUD-07` (limpeza de timers de UI no reinício), `AUD-08` (sinônimo `"deterministico"` na API e log ASCII no console).

---

## 6. Correções Realizadas em Cada Ciclo

### Ciclo 1 — Inspeção Profunda, Endurecimento Backend/Frontend e Expansão da Suíte de Testes
1. **Inspeção e Backup:** Cópia integral dos 41 arquivos originais antes de qualquer edição.
2. **Correções no Backend (`Estado.java`, `JsonUtil.java`, `ServidorHttp.java`):**
   - Limpeza de `efeitosPendentes` e `eventoAtual` em `Estado.encerrarPartida()`.
   - Escape de caracteres de controle `< 0x20` em `JsonUtil.quote()`.
   - Limite de 4 KB por requisição, validação `getCanonicalPath()` e trava de sincronia de turno (`HTTP 409`) em `ServidorHttp.java`.
3. **Correções no Frontend (`index.html`, `style.css`, `app.js`):**
   - Inclusão do bloco expansível *"Ver Memória de Cálculo Detalhada"* na Carta de Consequências.
   - Inclusão da seção *"Termômetro Institucional (IA)"* no modal de Crônicas.
   - Envio do turno atual no payload de `/api/decidir` e cancelamento de timers em reinícios/redimensionamentos.
   - Refinamento das *media queries* para as 5 resoluções obrigatórias.
4. **Expansão de `Main.java` (`--verificar`):** Implementação das 8 baterias de testes automatizados cobrindo todas as cartas, limites matemáticos, efeitos atrasados, estratégias de jogo e benchmark de 500 partidas.

### Ciclo 2 — Testes de Integração HTTP, Ajustes Finos e Sincronização Documental
1. **Execução de `.\executar.bat --verificar` e Testes HTTP:** Identificado `AUD-08` (suporte ao valor `"deterministico"` no parâmetro `modo` do endpoint `/api/iniciar` e caractere acentuado no console Windows).
2. **Correção e Recompilação:** Ajustados `ServidorHttp.java` e `Main.java`, recompilado com `--release 8` (0 erros, 0 avisos) e reexecutada toda a bateria automatizada e de endpoints HTTP.
3. **Sincronização da Documentação:** Atualizados `REGRAS_ECONOMICAS.md` (14 cartas), `RELATORIO_DE_TESTES.md` (separação em 4 partes com as 5 fichas humanas em branco) e `README.md`.

---

## 7. Validação da Arquitetura Java

- **Conformidade com as Tecnologias Obrigatórias:** O projeto utiliza **exclusivamente Java JDK puro**, sem Spring Boot, sem Maven/Gradle obrigatórios para execução, sem bibliotecas externas e sem banco de dados externo.
- **Servidor HTTP Nativo:** Implementado em [`ServidorHttp.java`](file:///c:/Users/Arthur/Desktop/Economics%20Games/src/govsim/server/ServidorHttp.java) sobre `com.sun.net.httpserver.HttpServer`, com sincronização `synchronized (estado)` garantindo *thread-safety* em todas as operações de leitura e escrita do estado.
- **Separação de Responsabilidades (MVC / Camadas):**
  - `govsim.model`: Apenas estruturas de dados, invariantes de domínio e limites matemáticos. Nenhuma regra de interface ou código HTML/HTTP.
  - `govsim.engine`: Toda a inteligência econômica ([`MotorEconomico`](file:///c:/Users/Arthur/Desktop/Economics%20Games/src/govsim/engine/MotorEconomico.java)), baralho de cartas ([`BancoEventos`](file:///c:/Users/Arthur/Desktop/Economics%20Games/src/govsim/engine/BancoEventos.java)) e IA baseada em regras ([`AvaliadorIA`](file:///c:/Users/Arthur/Desktop/Economics%20Games/src/govsim/engine/AvaliadorIA.java)).
  - `govsim.server`: Roteamento HTTP e serialização JSON ([`JsonUtil`](file:///c:/Users/Arthur/Desktop/Economics%20Games/src/govsim/server/JsonUtil.java)).
  - `govsim.view`: Apresentação visual desacoplada consumindo a API REST via `fetch`.

---

## 8. Validação do Motor Econômico

Todas as relações causais exigidas na Seção 6 do plano de auditoria foram verificadas no código de [`MotorEconomico.java`](file:///c:/Users/Arthur/Desktop/Economics%20Games/src/govsim/engine/MotorEconomico.java) e comprovadas nos testes automatizados:
1. **Aumento de gastos públicos / subsídios:** Reduz o `Caixa Público` imediatamente, estimula o `PIB` e a `Aprovação Popular` no curto prazo, mas gera pressão inflacionária imediata ou defasada ($T+1$).
2. **Corte de gastos / austeridade fiscal:** Preserva ou recompõe o `Caixa Público` e reduz pressões inflacionárias, mas desacelera o `PIB` e reduz a `Aprovação Popular`.
3. **Elevação da Taxa de Juros (Selic):** Desacelera a `Inflação` com forte efeito defasado ($T+1$ e $T+2$), mas encarece o serviço da dívida pública ($\Delta \text{Caixa}_{\text{juros}} = -0,7 \times (\text{Juros} - 12,0)$ para $\text{Juros} > 12\%$) e retrai o `PIB`.
4. **Redução da Taxa de Juros (Selic):** Estimula o crédito e o `PIB` e alivia o serviço da dívida no `Caixa` ($\text{Juros} < 9\%$), mas aumenta a `Inflação` nos turnos seguintes.
5. **Interação Endógena $\text{PIB} \rightarrow \text{Caixa}$:** Crescimento acima de $3,0\%$ gera dividendo de arrecadação ($+0,8 \times (\text{PIB} - 3,0)$); recessão ($\text{PIB} < 0\%$) derruba a receita tributária ($-1,2 \times |\text{PIB}|$).
6. **Interação Endógena $\text{Inflação} \rightarrow \text{Aprovação}$:** Inflação acima de $6,0\%$ corrói salários reais e penaliza a aprovação em $-0,75 \times (\text{Inflação} - 6,0)$; inflação na meta ($2,5\%$ a $4,5\%$) concede bônus de estabilidade de $+1,0\%$.

---

## 9. Validação dos Efeitos Atrasados

O subsistema de efeitos atrasados ([`EfeitoAtrasado.java`](file:///c:/Users/Arthur/Desktop/Economics%20Games/src/govsim/model/EfeitoAtrasado.java) e [`MotorEconomico.java`](file:///c:/Users/Arthur/Desktop/Economics%20Games/src/govsim/engine/MotorEconomico.java#L75-L108)) foi submetido ao teste **TA-04**:
- **Agendamento:** Ao assinar um decreto com efeitos defasados (ex.: `EV_ALTA_DOLAR` — Decreto A, que agenda ondas em $T+1$ e $T+2$), novas instâncias são criadas com `turnoOrigem = T` e `turnosRestantes = atraso`.
- **Maturação Exata sem Duplicação:** No início de cada turno subsequente, `decrementarTurno()` reduz `turnosRestantes` em 1. Apenas os efeitos com `turnosRestantes <= 0` entram em vigor naquele turno e são imediatamente removidos da fila via `Iterator.remove()`, garantindo que **nenhum efeito seja aplicado duas vezes**.
- **Limpeza em Fim de Jogo e Reinício:** Quando a partida termina (`encerrarPartida`) ou é reiniciada (`resetarParaNovaPartida`), a fila `efeitosPendentes` é integralmente esvaziada.

---

## 10. Validação das Cartas e Eventos

A auditoria automatizada (**TA-02**) percorreu as **14 cartas** e os **28 decretos** de [`BancoEventos.java`](file:///c:/Users/Arthur/Desktop/Economics%20Games/src/govsim/engine/BancoEventos.java):
- **Diversidade de Personagens (10 arquétipos visuais SVG):** Ministro da Economia, Presidente do Banco Central, Ministro da Saúde, Ministra da Educação, Ministro de Minas e Energia, Chanceler, Líder da Oposição, Representante dos Empresários, Líder dos Trabalhadores, Governador Estadual e Representante da População.
- **Completude de Campos:** 100% das cartas possuem `id`, `nome`, `categoria`, `personagemNome`, `personagemCargo`, `personagemAvatar`, `dialogo` em primeira pessoa, `contextualizacao`, `decretoA` (esquerda) e `decretoB` (direita) com impactos e explicações teóricas preenchidos.
- **Sorteio sem Repetição (**TA-05**):** Em qualquer partida de 10 turnos, o método `BancoEventos.sortearProximoEvento` utiliza o conjunto `eventosSorteadosIds` para garantir que **10 cartas distintas** sejam sorteadas sem nenhuma repetição durante todo o mandato.

---

## 11. Validação das Mecânicas Estilo *Reigns*

A interface em [`index.html`](file:///c:/Users/Arthur/Desktop/Economics%20Games/src/govsim/view/index.html), [`style.css`](file:///c:/Users/Arthur/Desktop/Economics%20Games/src/govsim/view/style.css) e [`app.js`](file:///c:/Users/Arthur/Desktop/Economics%20Games/src/govsim/view/app.js) implementa fielmente a experiência inspirada em *Reigns* adaptada ao contexto acadêmico do GovSim:
1. **Quatro Pilares Superiores com Preenchimento Interno (`clip-path`):** Os ícones SVG de Caixa Público (Cofre), Aprovação Popular (Povo), PIB (Engrenagem/Gráfico) e Inflação (Chama Monetária) enchem e esvaziam verticalmente de acordo com `percentualBarra`.
2. **Pontos de Pré-Visualização de Impacto (*Preview Dots*):** Ao inclinar a carta ou passar o mouse sobre os botões laterais, círculos pequenos (impacto moderado) ou grandes (impacto forte $\ge 10$) acendem acima dos pilares que serão afetados pelo decreto, exatamente como em *Reigns*, revelando a magnitude sem entregar a resposta pronta antes da decisão.
3. **Cortina Superior Deslizante (`choice-curtain`):** Mostra o título e o tipo de política do decreto correspondente à direção da inclinação.
4. **Interação Tripla Acessível:** Suporte simultâneo a **arraste por mouse/toque** (*swipe* esquerda/direita com física elástica), **botões laterais dedicados** (*Assinar Decreto A / B*) e **atalhos de teclado** (`Seta Esquerda`/`A`, `Seta Direita`/`B`, `Espaço`/`Enter` para avançar).
5. **Carta de Consequências Didática:** Após cada decisão, exibe o carimbo do decreto assinado, os deltas coloridos nos 4 indicadores, a explicação causal (*Por que mudou?* e *Trade-off e Efeitos Futuros*), as reações da IA e a **Memória de Cálculo Detalhada** expansível.

---

## 12. Validação da Interface e Responsividade

O layout foi auditado nas 5 resoluções exigidas na Seção 10:
- **Desktop Full HD (`1920x1080`):** Carta centralizada (`410px` de largura), asas de decisão A/B posicionadas nas laterais da carta e pilares espaçados no topo sem qualquer barra de rolagem desnecessária.
- **Notebook (`1366x768` e `1280x720`):** A *media query* `@media (max-height: 800px)` reduz proporcionalmente a altura do retrato SVG (`155px`) e o *padding* interno da carta, mantendo o cabeçalho, a carta inteira e o rodapé visíveis na mesma tela.
- **Tablet (`768x1024`):** A *media query* `@media (max-width: 900px)` reposiciona os botões de Decreto A e Decreto B lado a lado abaixo da carta ativa, facilitando o toque com os polegares e mantendo o *swipe* ativo.
- **Smartphone (`390x844`):** A *media query* `@media (max-width: 480px)` ajusta a largura da carta para `calc(100vw - 24px)`, compacta os 4 pilares superiores (`vessel` de `36x36px`), empilha a grade de consequências em 2 colunas legíveis e preserva todos os textos sem corte.

---

## 13. Resultados dos Testes Automatizados

Saída literal da execução de `.\executar.bat --verificar`:

```text
======================================================================
  SUITE DE AUDITORIA TECNICA E TESTES AUTOMATIZADOS - GOVSIM
======================================================================
[APROVADO] Teste 1: Inicializacao do Estado e 4 Indicadores Principais.
[APROVADO] Teste 2: Auditoria completa das 14 cartas e 28 decretos (0 falhas).
[APROVADO] Teste 3: Consistencia matematica, limites (clamping) e arredondamento.
[APROVADO] Teste 4: Agendamento, maturacao em ondas e nao-duplicacao de efeitos atrasados.
[APROVADO] Teste 5: Selecao de cartas sem repeticao e limpeza pos-encerramento.
[APROVADO] Teste 6: Rejeicao de entradas invalidas e bloqueio pos-encerramento.
----------------------------------------------------------------------
  SIMULACAO DE ESTRATEGIAS DE JOGABILIDADE (SECAO 12):
  [OK] 7.1 Estrategia Expansionista Radical -> Derrota Fiscal no Turno 7 (Caixa=0.0)
  [OK] 7.2 Estrategia Austeridade Cega      -> Derrota Politica no Turno 6 (Aprovacao=0.0%)
  [OK] 7.3 Estrategia Equilibrada           -> VITORIA (10 turnos | Caixa=137.8 | Aprov=53.9% | PIB=2.7% | Inflacao=2.4%)
  [OK] 7.4 Estrategia Juros/Controle        -> VITORIA (10 turnos | Caixa=157.7 | Aprov=40.8% | PIB=0.8% | Inflacao=3.2%)
  [OK] 7.5 Estrategia Estimulo + Ajuste     -> VITORIA (10 turnos | Caixa=81.1 | Aprov=5.8% | PIB=5.1% | Inflacao=23.0%)
  [OK] 7.6 Estrategia Alternada (A/B)       -> Finalizada no Turno 8 (DERROTA_CAIXA)
  [OK] 7.7 Reinicio Limpo de Partida        -> Estado restaurado sem residuos.
[APROVADO] Teste 7: Todas as 7 estrategias e cenarios de jogabilidade verificados.
----------------------------------------------------------------------
[APROVADO] Teste 8: Benchmark de Estabilidade (500 partidas completas + serializacao JSON em 399 ms | Memoria JVM utilizada: 211 MB).
======================================================================
  RESULTADO GLOBAL: 100% DOS TESTES AUTOMATIZADOS APROVADOS!
======================================================================
```

---

## 14. Resultados das Partidas Simuladas

As 7 simulações completas exigidas na Seção 12 demonstram que o jogo **não é trivial nem impossível**, recompensando a leitura econômica do cenário:
1. **Partida 1 — Priorizando gastos públicos e aprovação popular (`7.1`):** Leva à **Derrota por Colapso Fiscal (`DERROTA_CAIXA`) no Turno 7** (`Caixa = 0.0`), mesmo com `Aprovação = 100%` e `PIB = 9.8%`, provando que o populismo fiscal sem responsabilidade orçamentária inviabiliza o Estado.
2. **Partida 2 — Priorizando austeridade fiscal e corte de gastos (`7.2`):** Leva à **Derrota por Queda de Governabilidade (`DERROTA_APROVACAO`) no Turno 6** (`Aprovação = 0.0%`, `PIB = -5.7%`), provando que ajustes recessivos contínuos sem rede de proteção social derrubam o governo.
3. **Partida 3 — Estratégia Equilibrada Pragmática (`7.3`):** Conquista **Vitória com Excelência no Turno 10** (`Caixa = 137.8`, `Aprovação = 53.9%`, `PIB = +2.7%`, `Inflação = 2.4%`), combinando anticiclicidade fiscal e controle monetário.
4. **Partida 4 — Priorizando Controle da Inflação via Juros e Prudência (`7.4`):** Conquista **Vitória Ortodoxa no Turno 10** (`Caixa = 157.7`, `Aprovação = 40.8%`, `PIB = +0.8%`, `Inflação = 3.2%`), demonstrando o *trade-off* da Curva de Phillips (inflação baixa ao custo de menor crescimento do PIB).
5. **Partida 5 — Priorizando Crescimento do PIB com Ajuste Parcial (`7.5`):** Conquista **Vitória sob Pressão Inflacionária no Turno 10** (`Caixa = 81.1`, `Aprovação = 5.8%`, `PIB = +5.1%`, `Inflação = 23.0%`), evidenciando como o superaquecimento econômico acelera a inflação e desgasta a aprovação.
6. **Partida 6 — Escolhas Alternadas Mecânicas `A/B/A/B` (`7.6`):** Termina em **Derrota Fiscal no Turno 8** (`DERROTA_CAIXA`), comprovando que decisões aleatórias/mecânicas sem leitura de contexto falham.
7. **Partida 7 — Reinício de Partida (`7.7`):** Verifica que após qualquer vitória ou derrota, acionar nova partida restaura `Turno = 1`, `Caixa = 100.0`, `Aprovação = 60.0%`, `PIB = 2.5%`, `Inflação = 4.5%` e `efeitosPendentes = []`.

---

## 15. Validação do Funcionamento 100% Offline

A varredura automatizada sobre todos os arquivos de `src/govsim/view/` (`index.html`, `style.css`, `app.js`) confirmou **0 referências a URLs externas** (`http://` ou `https://`, exceto o *namespace* padrão XML `http://www.w3.org/2000/svg`):
- **Fontes Tipográficas:** Pilha nativa do sistema (`Georgia`, `Segoe UI`, `system-ui`, `Consolas`).
- **Ilustrações e Ícones:** 100% gerados em SVG inline/procedural em [`app.js`](file:///c:/Users/Arthur/Desktop/Economics%20Games/src/govsim/view/app.js#L790-L920) e [`index.html`](file:///c:/Users/Arthur/Desktop/Economics%20Games/src/govsim/view/index.html).
- **Efeitos Sonoros:** Sintetizados em tempo real via `AudioContext` nativo do navegador (Web Audio API), sem arquivos `.mp3`/`.wav` externos.
- **Gráfico de Evolução:** Renderizado via HTML5 `<canvas>` 2D nativo.

---

## 16. Limitações Conhecidas

1. **Estado em Memória (Single-Player Local):** Conforme definido na arquitetura da proposta original (sem banco de dados externo), o estado da partida reside em memória no servidor local (`localhost:8080`), sendo ideal para uso individual ou demonstração em sala de aula.
2. **Testes com os 5 Participantes Externos:** As fichas individuais em [`RELATORIO_DE_TESTES.md`](file:///c:/Users/Arthur/Desktop/Economics%20Games/RELATORIO_DE_TESTES.md) encontram-se prontas e intencionalmente em branco para preenchimento durante a aplicação real com os 5 estudantes/convidados externos ao grupo.

---

## 17. Instruções Finais de Execução e Demonstração

### 17.1. Como Executar o Jogo
- **No Windows:** Execute `.\executar.bat` no terminal ou dê duplo clique sobre [`executar.bat`](file:///c:/Users/Arthur/Desktop/Economics%20Games/executar.bat). O servidor iniciará automaticamente em **`http://localhost:8080`**.
- **No Linux / macOS:** Execute `chmod +x executar.sh && ./executar.sh` e abra **`http://localhost:8080`**.
- **Para Executar a Suíte de Auditoria Automatizada:** Execute `.\executar.bat --verificar` (ou `java -cp bin govsim.Main --verificar`).

### 17.2. Roteiro Rápido para Demonstração em Sala (3 a 5 minutos)
1. Com o jogo aberto em **`http://localhost:8080`**, clique no botão **`Modo Demo`** na barra superior para iniciar a sequência determinística guiada.
2. Mostre os **4 pilares no topo** (`Caixa: 100,0`, `Aprovação: 60,0%`, `PIB: +2,5%`, `Inflação: 4,5%`) e incline levemente a carta do **Turno 1 (Ministro da Saúde — Pandemia)** para a esquerda e para a direita, destacando os **pontos luminosos (*preview dots*)** sobre os pilares e a cortina superior.
3. Deslize a carta para a esquerda (**Decreto A — Pagar Auxílio Emergencial**) e mostre a **Carta de Consequências**, expandindo a **Memória de Cálculo Detalhada** para evidenciar a queda de `-22,0` no Caixa, o ganho de aprovação e o efeito atrasado agendado para o Turno 2 (`+1,5%` na Inflação).
4. Avance para o **Turno 2 (Presidente do Banco Central — Disparada do Dólar)**, assine o **Decreto A (Subir Juros Selic +2,5 p.p.)** e abra o modal **`Crônicas`** no topo para mostrar o **Gráfico de Evolução**, o **Termômetro Institucional (IA)** e a fila de **Efeitos Econômicos Atrasados** em maturação.

---

## 18. Veredito Final da Auditoria

Com base na verificação integral do código-fonte, na aprovação de **100% dos testes automatizados e de segurança HTTP**, na validação matemática do motor econômico e na conformidade integral com a proposta acadêmica e com a experiência de jogo inspirada em *Reigns*, o projeto **GovSim — Simulador de Gestão Governamental em Turnos** recebe o veredito final:

> ### **VEREDITO FINAL: APROVADO**
