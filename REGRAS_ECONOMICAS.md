# REGRAS ECONÔMICAS DO GOVSIM
**Projeto Acadêmico:** GovSim — Simulador de Gestão Governamental em Turnos  
**Instituição:** Instituto Federal do Norte de Minas Gerais (IFNMG) — Campus Januária  
**Curso:** Bacharelado em Sistemas de Informação  
**Disciplina:** Introdução à Economia (Economics Games — 2ª Entrega)

> **Nota Metodológica Importante:**  
> O **GovSim** não é um modelo econométrico de previsão real. Trata-se de um **simulador educacional simplificado, com ruído no modo normal e determinismo no modo demonstração**, projetado para demonstrar de forma didática, transparente e reproduzível as relações entre **Política Fiscal**, **Política Monetária**, **Demanda Agregada**, **Inflação**, **Crescimento do PIB**, **Caixa Público** e **Aprovação Popular**, incluindo efeitos defasados no tempo e *trade-offs* governamentais.

---

## Balanceamento e dificuldades

Os valores abaixo são os parâmetros finais após calibração. Substituem os valores iniciais propostos; apenas o enum foi ajustado durante a calibração.

| Nível | Turnos | Caixa | Aprovação | Benefício | Custo | Ruído | IA | Prévia |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | --- |
| FACIL | 10 | 100 | 60 | 1,00 | 1,17 | 0 | 0,5 | números |
| MEDIO | 12 | 100 | 60 | 0,90 | 1,18 | 0,55 | 1,0 | setas |
| DIFICIL | 15 | 70 | 50 | 0,95 | 1,035 | 0,30 | 1,5 | setas |

Para cada efeito direto ou atrasado de carta: `efeito = base × multiplicador × (1 + U(-ruído, +ruído))`. Benefícios são aumentos de caixa/aprovação/PIB e reduções de inflação; custos são os sinais contrários. Juros são um instrumento: sua alteração direta não recebe classificação de benefício/custo. O canal defasado da Selic recebe o ajuste por indicador. O ruído usa Random, independente por componente, e vale zero em demonstração. A IA tem seus deltas multiplicados pelo fator IA, sem novo ruído. Relações endógenas não recebem esses multiplicadores.

Efeitos atrasados são ajustados e arredondados ao agendar, sem novo sorteio ou multiplicação na maturação. A prévia usa os valores base da carta (estimativas), números no Fácil e direção nos demais níveis. Indicadores realizados e explicações após a decisão continuam numéricos para análise didática.

POST /api/iniciar exige `{"modo":"normal","dificuldade":"DIFICIL","turnos":0}`. Turnos=0 usa o padrão do nível; valores explícitos devem estar entre 3 e 24. Campos ausentes, tipos errados, valores inválidos ou JSON malformado retornam HTTP 400 sem alterar a partida.

### Reversão, inércia e transmissão monetária

- PIB intermediário = PIB anterior + 0,30 × (2,5 − PIB anterior) + efeitos diretos e maturados.
- Inflação intermediária = max(0, 4 + 0,82 × (inflação anterior − 4) + efeitos diretos e maturados).
- As relações endógenas abaixo usam esses valores intermediários. A IA reage à projeção antes de seus próprios efeitos. A reversão é incluída uma única vez no delta final.
- A cada turno com Selic diferente de 10,5%, agendam-se duas ondas, T+1 e T+2. Cada onda tem ΔPIB = −0,08 × (Selic − 10,5) e Δinflação = −0,10 × (Selic − 10,5), ajustados pela dificuldade. Nível alto contrai crédito e demanda; nível baixo estimula ambos. Estas ondas representam o nível persistente dos juros, além dos efeitos específicos das cartas.
- Cada agendamento e maturação fica no histórico e no detalhesCalculo. Ondas pendentes desaparecem no encerramento.

### Derrotas e avaliação final

Depois dos efeitos e limites, a prioridade é: DERROTA_AMBOS, DERROTA_CAIXA, DERROTA_APROVACAO, DERROTA_HIPERINFLACAO (inflação >=25), DERROTA_DEPRESSAO (PIB <−3 por dois turnos consecutivos), vitória ao completar o mandato. As duas derrotas macroeconômicas só valem em MEDIO/DIFICIL. PIB >=−3 zera o contador; reiniciar também o zera.

Estadista exige vitória, caixa >=85, inflação <=5, PIB >=2 e aprovação >=75. A combinação mantém a distinção rara, mas alcançável; o simulador informa sua frequência entre todas as partidas.

### Simulação reproduzível

Execute `java -cp bin govsim.Main --simular [N]` (padrão 20000 por estratégia e nível: 360000 partidas). Semente inicial do motor: 20261103; escolhas aleatórias: 781. Cada estratégia reinicia as sementes e executa partidas normais; ruído e cartas compartilham a fonte Random do motor. Médias incluem vitórias e derrotas no momento do encerramento. Percentuais de derrota usam todas as N partidas como denominador; somados à vitória totalizam 100%, salvo arredondamento.

Estratégias: aleatória (50/50); sempre A; sempre B; maior aprovação direta; maior caixa direto; esperta. A esperta maximiza caixa quando caixa <50; caso contrário maximiza aprovação quando aprovação <40; fora disso maximiza 0,5×Δcaixa + Δaprovação − 2×Δinflação + ΔPIB. Em empate escolhe A. Usa apenas estado e números base da carta atual, sem antecipar sorteios. Se ambas as reservas estão baixas, prioriza solvência. As metas são aproximadas: 70/35/10% aleatória e 99/75/40% esperta.

## 1. Indicadores Principais e Valores Iniciais

A proposta original do GovSim estabelece **quatro indicadores principais** que resumem a situação fiscal, política, produtiva e monetária do país, acompanhados de uma variável instrumental complementar (**Taxa Básica de Juros — Selic**):

| Indicador | Código | Valor Inicial | Limite Mínimo | Limite Máximo | Unidade | Significado Econômico e Regra de Jogo |
| :--- | :---: | :---: | :---: | :---: | :---: | :--- |
| **1. Caixa Público** | `CAIXA` | **100,0** | `0,0` | `200,0` | `pts (R$ bi)` | Recursos financeiros disponíveis ao Tesouro Nacional para custear serviços públicos, investimentos, subsídios e o serviço da dívida. **Se chegar a `0,0`, ocorre Derrota imediata por insolvência fiscal.** |
| **2. Aprovação Popular** | `APROVACAO` | **60,0** | `0,0` | `100,0` | `%` | Percentual de apoio político e social da população ao governo. Reflete o bem-estar social, o nível de emprego e o poder de compra. **Se chegar a `0,0%`, ocorre Derrota imediata por perda de governabilidade.** |
| **3. PIB (Crescimento)** | `PIB` | **2,5** | `-10,0` | `+12,0` | `% a.a.` | Taxa anualizada de variação do Produto Interno Bruto (atividade econômica). Valores positivos indicam expansão produtiva e geração de empregos; valores negativos indicam recessão. |
| **4. Inflação (IPCA)** | `INFLACAO` | **4,5** | `0,0` | `35,0` | `% a.a.` | Variação geral do nível de preços ao consumidor. A faixa de equilíbrio (meta de inflação) situa-se entre **2,5% e 4,5% a.a.** Acima de **6,0% a.a.**, corrói o poder de compra das famílias. |
| **Instrumento: Taxa de Juros (Selic)** | `JUROS` | **10,5** | `2,0` | `25,0` | `% a.a.` | Instrumento de Política Monetária. Juros mais altos contêm a inflação com defasagem temporal, mas desaceleram o PIB e encarecem a rolagem da dívida pública no Caixa. |

### Coerência do Estado Inicial
No Turno 1, o país parte em **equilíbrio macroeconômico inicial**:
- **Caixa Público em `100,0 pts`**: orçamento equilibrado, sem estresse fiscal.
- **Aprovação Popular em `60,0%`**: maioria confortável de governabilidade no início do mandato.
- **PIB crescendo a `2,5% a.a.`**: crescimento moderado, próximo ao produto potencial (sem gerar pressão inflacionária de demanda nem desemprego recessivo).
- **Inflação em `4,5% a.a.`**: dentro do intervalo da meta (`2,5%` a `4,5%`), preservando o poder de compra.
- **Taxa de Juros em `10,5% a.a.`**: patamar neutro inicial.

---

## 2. Fórmulas de Atualização a Cada Turno

A cada turno $t$, quando o jogador escolhe um decreto $d \in \{A, B\}$, a variação total ($\Delta I_t$) de cada um dos quatro indicadores $I \in \{\text{Caixa}, \text{Aprovação}, \text{PIB}, \text{Inflação}\}$ é calculada pela soma de **quatro componentes**, com ajuste por dificuldade:

$$\Delta I_t = \Delta I_{\text{direto}}(d) + \Delta I_{\text{atrasado}}(t) + \Delta I_{\text{endógeno}}(t) + \Delta I_{\text{IA}}(t)$$

O novo valor do indicador ao final do turno é obtido aplicando os limites mínimo e máximo (*clamping*) e arredondamento para uma casa decimal:

$$I_t = \min\Big(I_{\max},\; \max\big(I_{\min},\; I_{t-1} + \Delta I_t\big)\Big)$$

### 2.1. Componente 1 — Impacto Direto Imediato ($\Delta I_{\text{direto}}$)
Corresponde ao efeito primário imediato do decreto assinado pelo jogador no turno atual (por exemplo, o desembolso imediato do Tesouro em um pacote de auxílio ou o corte imediato de despesas em um ajuste fiscal).

### 2.2. Componente 2 — Maturação de Efeitos Atrasados ($\Delta I_{\text{atrasado}}$)
Todo efeito atrasado agendado em um turno anterior $t_0$ com defasagem $k$ possui um contador `turnosRestantes`. No início do processamento de cada turno:
1. Decrementa-se `turnosRestantes` em $1$ unidade.
2. Todos os efeitos cujo `turnosRestantes == 0` entram em vigor naquele turno (`turnoAplicacao` $= t_0 + k$), somando seus impactos ($\Delta \text{Caixa}, \Delta \text{Aprovação}, \Delta \text{PIB}, \Delta \text{Inflação}$) ao cálculo do turno.

### 2.3. Componente 3 — Interações Macroeconômicas Endógenas ($\Delta I_{\text{endógeno}}$)
Para que os indicadores interajam entre si de maneira realista e didática, o motor calcula os valores intermediários:
- $\text{PIB}_{\text{inter}} = \text{PIB}_{t-1} + \Delta \text{PIB}_{\text{direto}} + \Delta \text{PIB}_{\text{atrasado}}$
- $\text{Inflação}_{\text{inter}} = \max(0,\; \text{Inflação}_{t-1} + \Delta \text{Inflação}_{\text{direto}} + \Delta \text{Inflação}_{\text{atrasado}})$
- $\text{Juros}_t = \text{clamp}(\text{Juros}_{t-1} + \Delta \text{Juros}_{\text{direto}},\; 2,0,\; 25,0)$

A partir deles, aplicam-se as cinco relações estruturais abaixo:

1. **Relação Atividade Econômica $\rightarrow$ Arrecadação Fiscal ($\text{PIB} \rightarrow \text{Caixa Público}$):**
   - **Expansão Forte ($\text{PIB}_{\text{inter}} > 3,0\%$):** O crescimento econômico amplia a base de arrecadação de impostos:
     $$\Delta \text{Caixa}_{\text{arrec}} = +0,8 \times (\text{PIB}_{\text{inter}} - 3,0)$$
   - **Recessão ($\text{PIB}_{\text{inter}} < 0,0\%$):** A retração da atividade derruba a arrecadação tributária:
     $$\Delta \text{Caixa}_{\text{arrec}} = -1,2 \times |\text{PIB}_{\text{inter}}|$$

2. **Relação Política Monetária $\rightarrow$ Custo da Dívida Pública ($\text{Juros} \rightarrow \text{Caixa Público}$):**
   - **Juros Elevados ($\text{Juros}_t > 12,0\%$):** Encarecem o serviço da dívida pública:
     $$\Delta \text{Caixa}_{\text{juros}} = -0,7 \times (\text{Juros}_t - 12,0)$$
   - **Juros Baixos ($\text{Juros}_t < 9,0\%$):** Reduzem a despesa financeira do Tesouro:
     $$\Delta \text{Caixa}_{\text{juros}} = +0,5 \times (9,0 - \text{Juros}_t)$$

3. **Relação Demanda Agregada $\rightarrow$ Nível de Preços ($\text{PIB} \rightarrow \text{Inflação}$ / Curva de Phillips):**
   - **Economia Superaquecida ($\text{PIB}_{\text{inter}} > 3,8\%$):** Hiato do produto positivo gera inflação de demanda:
     $$\Delta \text{Inflação}_{\text{demanda}} = +0,35 \times (\text{PIB}_{\text{inter}} - 3,8)$$
   - **Recessão Profunda ($\text{PIB}_{\text{inter}} < -1,0\%$):** A ociosidade produtiva reduz a pressão sobre preços:
     $$\Delta \text{Inflação}_{\text{demanda}} = -0,30 \times (|\text{PIB}_{\text{inter}}| - 1,0)$$

4. **Relação Poder de Compra $\rightarrow$ Apoio Político ($\text{Inflação} \rightarrow \text{Aprovação Popular}$):**
   Sendo $\text{Inflação}_{\text{pós}} = \text{Inflação}_{\text{inter}} + \Delta \text{Inflação}_{\text{demanda}}$:
   - **Inflação Alta ($\text{Inflação}_{\text{pós}} > 6,0\%$):** A carestia corrói os salários reais e penaliza a popularidade:
     $$\Delta \text{Aprovação}_{\text{inflação}} = -0,75 \times (\text{Inflação}_{\text{pós}} - 6,0)$$
   - **Inflação na Meta ($2,5\% \le \text{Inflação}_{\text{pós}} \le 4,5\%$):** Estabilidade do poder aquisitivo gera bônus de aprovação:
     $$\Delta \text{Aprovação}_{\text{inflação}} = +1,0$$

5. **Relação Emprego e Renda $\rightarrow$ Apoio Político ($\text{PIB} \rightarrow \text{Aprovação Popular}$):**
   - **Crescimento Gerador de Emprego ($\text{PIB}_{\text{inter}} \ge 2,5\%$):**
     $$\Delta \text{Aprovação}_{\text{emprego}} = +0,5 \times (\text{PIB}_{\text{inter}} - 2,0)$$
   - **Recessão e Desemprego ($\text{PIB}_{\text{inter}} < 0,0\%$):**
     $$\Delta \text{Aprovação}_{\text{emprego}} = -1,0 \times |\text{PIB}_{\text{inter}}|$$

### 2.4. Componente 4 — IA Baseada em Regras ($\Delta I_{\text{IA}}$)
O módulo `AvaliadorIA` analisa a decisão do jogador e o estado projetado dos indicadores sob a ótica de dois agentes:
- **Oposição Parlamentar e Sociedade:**
  - Se $\text{Aprovação}_{\text{proj}} \le 35,0\%$: mobilização nas ruas e obstrução parlamentar ($\Delta \text{Aprovação}_{\text{IA}} = -2,0\%$).
  - Se o decreto aplica corte social abrupto ($\Delta \text{Aprovação}_{\text{direto}} \le -8,0\%$): crítica parlamentar ($\Delta \text{Aprovação}_{\text{IA}} = -1,0\%$).
  - Se $\text{Inflação}_{\text{proj}} \ge 8,0\%$: denúncia contra carestia ($\Delta \text{Aprovação}_{\text{IA}} = -1,5\%$).
  - Se $\text{PIB}_{\text{proj}} < 0,0\%$: cobrança contra desemprego ($\Delta \text{Aprovação}_{\text{IA}} = -1,0\%$).
  - Se o decreto é altamente popular ($\ge +8,0\%$) com economia estável: trégua política ($\Delta \text{Aprovação}_{\text{IA}} = +0,5\%$).
- **Mercado Financeiro e Setor Produtivo:**
  - Se $\text{Caixa}_{\text{proj}} \le 40,0$: estresse fiscal e prêmio de risco ($\Delta \text{Caixa}_{\text{IA}} = -2,5$, $\Delta \text{PIB}_{\text{IA}} = -0,4\%$, $\Delta \text{Inflação}_{\text{IA}} = +0,4\%$).
  - Se $\text{Inflação}_{\text{proj}} \ge 8,0\%$ e $\Delta \text{Juros} \le 0$: desancoragem de expectativas ($\Delta \text{Inflação}_{\text{IA}} = +0,5\%$, $\Delta \text{PIB}_{\text{IA}} = -0,3\%$).
  - Se $\Delta \text{Juros} \ge +1,5$ com $\text{Inflação}_{\text{proj}} \ge 5,0\%$: ancoragem monetária ($\Delta \text{Inflação}_{\text{IA}} = -0,3\%$).
  - Se o decreto recompõe fortemente o Caixa ($\Delta \text{Caixa}_{\text{direto}} \ge +10,0$): queda do risco-país ($\Delta \text{Caixa}_{\text{IA}} = +1,5$, $\Delta \text{PIB}_{\text{IA}} = +0,3\%$).
  - Se o decreto expande gastos excessivamente ($\Delta \text{Caixa}_{\text{direto}} \le -18,0$): cautela fiscal ($\Delta \text{Inflação}_{\text{IA}} = +0,2\%$).
  - Em cenário estável ($\text{Caixa} > 40$ e $\text{Inflação} < 8\%$): previsibilidade institucional ($\Delta \text{PIB}_{\text{IA}} = +0,2\%$).

---

## 3. Catálogo de Eventos, Regras dos Decretos, Efeitos Imediatos e Atrasados

O jogo possui **14 cartas de eventos de crise econômica, social e política**, cada uma com **Decreto A (Esquerda)** e **Decreto B (Direita)** desenhados sem "escolha óbvia", exigindo análise de conjuntura e avaliação de efeitos defasados:

| # | Evento (Carta) | Personagem | Decreto | Impactos Imediatos Diretos (`Caixa` / `Aprov` / `PIB` / `Inflação` / `Juros`) | Efeitos Atrasados Programados |
| :-: | :--- | :--- | :--- | :--- | :--- |
| **1** | **Pandemia Sanitária** (`EV_PANDEMIA`) | Ministro da Saúde | **A — Auxílio Emergencial** | `-22,0` / `+12,0%` / `+1,8%` / `+0,8%` / `0,0` | **T+1:** `+0,4%` PIB, `+1,5%` Inflação (pressão de demanda defasada) |
| | | | **B — Austeridade Fiscal** | `-4,0` / `-14,0%` / `-2,2%` / `-0,7%` / `0,0` | **T+1:** `-4,0` Caixa, `-2,0%` Aprov, `-0,5%` PIB, `-0,6%` Inflação |
| **2** | **Alta do Dólar** (`EV_ALTA_DOLAR`) | Presidente do Banco Central | **A — Alta de Juros (+2,5 p.p.)** | `-6,0` / `-5,0%` / `-0,4%` / `-0,3%` / `+2,5%` | **T+1:** `-3,0` Caixa, `-2,0%` Aprov, `-1,1%` PIB, `-1,4%` Inflação<br>**T+2:** `+3,0%` Aprov, `-0,6%` PIB, `-1,0%` Inflação |
| | | | **B — Subsídio a Importados** | `-16,0` / `+6,0%` / `+0,8%` / `+1,2%` / `0,0` | **T+1:** `-3,0` Caixa, `-3,0%` Aprov, `+1,3%` Inflação (repasse cambial) |
| **3** | **Preço dos Alimentos** (`EV_PRECO_ALIMENTOS`) | Representante da População | **A — Subvenção à Safra** | `-17,0` / `+9,0%` / `+0,9%` / `-1,1%` / `0,0` | **T+1:** `+2,0%` Aprov, `+0,5%` PIB, `-0,8%` Inflação |
| | | | **B — Abertura + Juros (+1,5 p.p.)** | `+4,0` / `-11,0%` / `-0,9%` / `+0,9%` / `+1,5%` | **T+1:** `-2,0` Caixa, `-0,8%` PIB, `-1,4%` Inflação |
| **4** | **Crise Energética** (`EV_CRISE_ENERGETICA`) | Ministro de Minas e Energia | **A — Aporte Tesouro + Obras** | `-19,0` / `+7,0%` / `+1,1%` / `-0,4%` / `0,0` | **T+2:** `+4,0` Caixa, `+2,0%` Aprov, `+1,2%` PIB, `-0,6%` Inflação |
| | | | **B — Bandeira Vermelha** | `+5,0` / `-12,0%` / `-1,6%` / `+1,8%` / `0,0` | **T+1:** `+0,3%` PIB, `-0,7%` Inflação |
| **5** | **Desemprego Elevado** (`EV_DESEMPREGO_ELEVADO`) | Representante dos Trabalhadores | **A — Plano de Obras (PAC)** | `-20,0` / `+11,0%` / `+2,4%` / `+0,9%` / `0,0` | **T+1:** `+7,0` Caixa, `+3,0%` Aprov, `+1,0%` PIB, `+1,1%` Inflação |
| | | | **B — Corte de Juros (-2,0 p.p.)** | `+4,0` / `+2,0%` / `+0,5%` / `+0,4%` / `-2,0%` | **T+1:** `+4,0` Caixa, `+5,0%` Aprov, `+1,7%` PIB, `+1,4%` Inflação |
| **6** | **Crise Fiscal** (`EV_CRISE_FISCAL`) | Ministro da Economia | **A — Pacto de Ajuste Fiscal** | `+22,0` / `-10,0%` / `-1,2%` / `-0,9%` / `-1,0%` | **T+1:** `+8,0` Caixa, `+2,0%` Aprov, `+0,9%` PIB, `-0,6%` Inflação |
| | | | **B — Emissão de Dívida** | `-12,0` / `+7,0%` / `+0,7%` / `+1,4%` / `+1,5%` | **T+1:** `-10,0` Caixa, `-5,0%` Aprov, `-1,3%` PIB, `+1,2%` Inflação |
| **7** | **Guerra Internacional** (`EV_GUERRA_INTERNACIONAL`) | Chanceler / Relações Exteriores | **A — Fundo Combustíveis** | `-18,0` / `+8,0%` / `+0,8%` / `-0,6%` / `0,0` | **T+1:** `-4,0` Caixa, `+2,0%` Aprov, `+0,6%` PIB, `-0,4%` Inflação |
| | | | **B — Preços Internacionais** | `+7,0` / `-13,0%` / `-1,5%` / `+2,1%` / `+1,0%` | **T+1:** `+3,0` Caixa, `-0,4%` PIB, `-0,8%` Inflação |
| **8** | **Queda na Arrecadação** (`EV_QUEDA_ARRECADACAO`) | Líder da Oposição | **A — Recomposição Tributária** | `+16,0` / `-9,0%` / `-1,4%` / `-0,5%` / `0,0` | **T+1:** `+4,0` Caixa, `-2,0%` Aprov, `-0,6%` PIB, `-0,4%` Inflação |
| | | | **B — Alíquota Reduzida (Laffer)** | `-15,0` / `+8,0%` / `+1,5%` / `+0,6%` / `0,0` | **T+2:** `+12,0` Caixa, `+2,0%` Aprov, `+1,0%` PIB, `+0,3%` Inflação |
| **9** | **Crescimento Aquecido** (`EV_CRESCIMENTO_AQUECIDO`) | Presidente do Banco Central | **A — Poupança Anticíclica** | `+15,0` / `-4,0%` / `-0,6%` / `-0,8%` / `+1,5%` | **T+1:** `+5,0` Caixa, `+3,0%` Aprov, `-0,7%` PIB, `-1,3%` Inflação |
| | | | **B — Estímulo Pró-Cíclico** | `-11,0` / `+11,0%` / `+2,1%` / `+1,3%` / `-1,5%` | **T+1:** `-5,0` Caixa, `-8,0%` Aprov, `-0,9%` PIB, `+2,6%` Inflação |
| **10** | **Comércio Internacional** (`EV_COMERCIO_EXTERIOR`) | Representante dos Empresários | **A — Crédito Exportador** | `-14,0` / `+6,0%` / `+1,4%` / `-0,2%` / `0,0` | **T+1:** `+8,0` Caixa, `+2,0%` Aprov, `+0,8%` PIB, `-0,5%` Inflação |
| | | | **B — Protecionismo Tarifário** | `+12,0` / `-6,0%` / `-0,7%` / `+1,5%` / `0,0` | **T+1:** `-2,0` Caixa, `-2,0%` Aprov, `-0,6%` PIB, `+0,6%` Inflação |
| **11** | **Educação e Ciência** (`EV_EDUCACAO_CIENCIA`) | Ministra da Educação | **A — Investir em Ensino e Ciência** | `-15,0` / `+9,0%` / `+0,6%` / `0,0%` / `0,0` | **T+2:** `+5,0` Caixa, `+3,0%` Aprov, `+1,5%` PIB, `-0,5%` Inflação |
| | | | **B — Contingenciar Verba** | `+6,0` / `-10,0%` / `-0,4%` / `0,0%` / `0,0` | **T+2:** `-2,0%` Aprov, `-0,8%` PIB, `+0,3%` Inflação |
| **12** | **Crise nos Estados** (`EV_CRISE_ESTADOS`) | Governador Estadual | **A — Socorrer Cofres dos Estados** | `-17,0` / `+9,0%` / `+1,2%` / `+0,6%` / `0,0` | **T+1:** `+3,0` Caixa, `+2,0%` Aprov, `+0,4%` PIB, `+0,3%` Inflação |
| | | | **B — Negar Resgate e Exigir Ajuste** | `+7,0` / `-11,0%` / `-1,1%` / `-0,5%` / `0,0` | **T+1:** `+4,0` Caixa, `-0,3%` PIB, `-0,4%` Inflação |
| **13** | **Greve no Setor Público** (`EV_GREVE_GERAL`) | Líder dos Trabalhadores | **A — Reajuste Salarial** | `-16,0` / `+10,0%` / `+0,9%` / `+0,8%` / `0,0` | **T+1:** `-8,0` Caixa, `+2,0%` Aprov, `+0,3%` PIB, `+0,5%` Inflação |
| | | | **B — Congelamento Salarial** | `+6,0` / `-12,0%` / `-0,8%` / `-0,5%` / `0,0` | **T+1:** `+5,0` Caixa, `-2,0%` Aprov, `+0,4%` PIB, `-0,3%` Inflação |
| **14** | **Endividamento Familiar** (`EV_ENDIVIDAMENTO_FAMILIAS`) | Representante da População | **A — Renegociação + Corte Juros** | `-13,0` / `+10,0%` / `+1,6%` / `+0,7%` / `-1,5%` | **T+1:** `+3,0` Caixa, `+2,0%` Aprov, `+0,8%` PIB, `+1,0%` Inflação |
| | | | **B — Rigor Prudencial** | `+4,0` / `-9,0%` / `-1,1%` / `-1,0%` / `+1,0%` | **T+1:** `-0,4%` PIB, `-0,9%` Inflação |

---

## 4. Condições de Vitória e Derrota

Conforme estabelecido na Proposta da Etapa 1:

1. **Derrota por Insolvência Fiscal (`DERROTA_CAIXA`):**
   - Ocorre imediatamente ao final de qualquer turno em que `Caixa Público <= 0,0`.
   - Representa o esgotamento da capacidade financeira do Estado (moratória / paralisação da máquina pública).

2. **Derrota Política por Perda de Governabilidade (`DERROTA_APROVACAO`):**
   - Ocorre imediatamente ao final de qualquer turno em que `Aprovação Popular <= 0,0%`.
   - Representa a queda do governo por perda total de suporte social e parlamentar.

3. **Derrota Dupla (`DERROTA_AMBOS`):**
   - Ocorre caso ambos (`Caixa Público` e `Aprovação Popular`) cheguem a zero no mesmo turno.

4. **Vitória (`VITORIA`):**
   - Ocorre quando o jogador completa todos os turnos do mandato (**10 turnos** na configuração padrão) mantendo `Caixa Público > 0,0` e `Aprovação Popular > 0,0%`, sem acionar as derrotas macroeconômicas no Médio/Difícil.
   - Ao vencer, o sistema classifica qualitativamente a gestão (por exemplo: *Estadista / Excelência*, *Gestão Equilibrada*, *Mandato Concluído sob Pressão Inflacionária* ou *Mandato Ortodoxo de Baixo Crescimento*) de acordo com os valores finais de PIB, Inflação, Caixa e Aprovação.

## Catálogo externo, informação e memória da IA

O catálogo agora está em src/govsim/engine/eventos.json, com 24 cartas; as 14 primeiras preservam a ordem e conteúdo originais. JsonUtil lê JSON sem frameworks. O carregamento tenta primeiro o recurso do classpath e depois o arquivo da árvore src; falhas interrompem a inicialização. Para distribuir apenas bin, copiar eventos.json para bin/govsim/engine.

O sorteio usa peso base 1, acrescido de 2 para PRECO com inflação >6, FISCAL com caixa <50, DESEMPREGO com PIB <0 e SUPERAQUECIMENTO com PIB >4. Não repete IDs; mandatos acima do tamanho do catálogo são rejeitados pelo motor. A API aceita até 24 turnos. Demonstração mantém ordem original e posições originais. Fora dela, cada carta recebe uma posição A/B sorteada no início do mandato.

Em MEDIO/DIFICIL, eventoAtual usa uma whitelist: não inclui impactos numéricos, efeitos atrasados, consequências enumeradas nem explicação teórica da opção. A direção/intensidade está em direcoes (caixa, aprovacao, pib, inflacao, juros): ↑/↑↑/↓/↓↓ e → para neutro. Limiares de intensidade: 12/8/1,2/0,9/1,5. Quantidades no diálogo/contexto são ocultadas. O histórico após decisão preserva efeitos realizados e explicações exatas. FACIL mantém a serialização completa.

A IA aplica regras independentes: baixa aprovação, impopularidade, inflação e recessão podem somar pressão da Oposição. Paciência começa em 1, cai 0,15 por decreto impopular e recupera 0,10 nos demais (limites 0–1). A crítica a impacto de aprovação <=−8 custa 1+(1−paciência). Mercado combina risco fiscal, expectativas, aperto monetário, recomposição, gasto e previsibilidade. Confiança começa em 1, cai 0,10 com caixa projetado <=40 ou inflação >=8; sobe 0,05 nos demais, limitada a 0,4–1,6. Cada efeito de cada regra é classificado antes da soma: benefício (caixa/PIB positivos ou inflação negativa) recebe fator confiança; penalidade recebe fator 1/confiança. Assim, confiança baixa reduz benefícios e amplifica penalidades; confiança alta aumenta benefícios e amortece penalidades. Uma penalidade fiscal de −2,5 com confiança 0,4 passa a −6,25 (antes do arredondamento); com confiança 1,6 passa a −1,5625. Uma melhora fiscal de +1,5 passa a +0,6 e +2,4, respectivamente. Alta de inflação é penalidade e desinflação é benefício. Os efeitos já ajustados são somados e arredondados pelo ResultadoIA; depois o motor aplica o multiplicador IA da dificuldade. Não se aplica confiança ao saldo líquido agregado: benefícios e penalidades simultâneos mantêm seus próprios fatores. Ambos os estados reiniciam por partida.

POST /api/iniciar aceita seed numérica opcional, por exemplo {"modo":"normal","dificuldade":"MEDIO","turnos":0,"seed":1234}. A seed reinicia a fonte Random antes do embaralhamento, sorteio ponderado e ruído. Sem seed explícita, a fonte segue aleatória. A estratégia esperta de simulação continua usando números internos como benchmark privilegiado; jogadores de Médio/Difícil só veem sinais antes da decisão.

### Medição após expansão do catálogo (20000 por estratégia/nível)

Sem alterar os parâmetros de dificuldade: aleatória 74,69% / 41,52% / 10,53%; esperta 99,39% / 78,14% / 39,75%. A meta de aleatória no Médio (35%) ficou 6,52 pontos abaixo do observado; Fácil ficou 4,69 pontos acima de 70%. Sempre A: 73,83% / 42,38% / 11,03%; sempre B: 73,87% / 42,10% / 10,91%. A posição deixou de oferecer vantagem sistemática. Saída completa em simulacao-20000.txt.

### Sessões e recursos do servidor

Sessao reúne Estado, MotorEconomico e último acesso. O servidor mantém no máximo 100 sessões, com expiração após 30 minutos de inatividade. Uma consulta válida renova o último acesso. A limpeza ocorre antes de buscar/criar sessão e a cada minuto por uma tarefa daemon, encerrada ao parar o servidor. Ao atingir o limite, o registro descarta a sessão menos recentemente acessada (LRU). Acesso posterior a um ID expirado ou removido cria uma partida nova, sem recuperar estado nem motor anteriores. O synchronized por Estado permanece nos endpoints; a gestão do registro também é sincronizada.

### Recalibração após correção da confiança

Ajustes exclusivamente no enum: multiplicadorCusto do FACIL de 1,14 para 1,17; MEDIO de 1,14 para 1,18. Todos os demais parâmetros, inclusive DIFICIL, foram preservados. A correção da confiança foi aplicada antes de medir e calibrar; não houve mudança adicional na estrutura do motor para atingir as metas.

Mesmas sementes e 20000 partidas por estratégia/dificuldade (360000 no total):

| Nível | Aleatória antes | Aleatória depois | Meta | Esperta antes | Esperta depois |
| --- | ---: | ---: | ---: | ---: | ---: |
| FACIL | 74,69% | 70,23% | ~70% | 99,39% | 98,28% |
| MEDIO | 41,52% | 34,41% | ~35% | 78,14% | 63,64% |
| DIFICIL | 10,53% | 9,93% | ~10% | 39,75% | 33,63% |

As metas desta rodada são para a estratégia aleatória. A esperta perdeu eficiência com a confiança corrigida; esses resultados não representam manutenção das antigas metas dessa estratégia. Saídas completas em simulacao-antes.txt e simulacao-20000.txt.
