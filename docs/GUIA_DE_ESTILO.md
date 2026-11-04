# Guia de estilo do GovSim

Público: pessoa comum, com escolaridade de ensino médio. A mensagem deve permitir decidir sem conhecimento prévio de economia.

- Título: verbo e ação concreta, até 5 palavras. Nada de siglas ou nome de política econômica.
- Resumo emPoucasPalavras: uma frase de até 14 palavras explicando a ação prática, sem prometer resultado.
- Diálogo: problema concreto, voz do personagem, 1–2 frases, até 20 palavras. Não antecipe o efeito das opções.
- Tag: usar somente Gasto do governo, Juros, Imposto, Ajuda social, Comércio exterior, Obras, Serviços públicos, Emprego, Energia ou Prestação de contas.
- Conceito e lição: se usar um termo técnico, explicar na mesma frase. Prefira o padrão Termo: explicação.
- Resultado: dizer o que mudou para as pessoas e para o dinheiro do governo. No máximo um termo técnico por frase, explicado.
- Manchete: até 14 palavras e sem jargão. Não atribuir à última escolha efeitos que também vieram de decisões anteriores.
- Epílogo: começar pela ação mais marcante e citar títulos simples e turnos reais.
- Não mudar números, condições, escolhas internas ou a ordem dos sorteios para melhorar o texto.

## Termos proibidos em títulos, diálogos e tags

A lista central, lida pelo --verificar, está em src/govsim/view/glossario.json. Os itens com proibidoEmTituloDialogoTag=true e seus aliases são proibidos nesses três campos. Juros, imposto e empréstimo são permitidos por serem palavras comuns, com ajuda disponível na tela.

| Termo e variantes | Como explicar |
|---|---|
| PIB | É o valor do que o país produz. Aqui, o número mostra se a produção cresce ou diminui. |
| Inflação, inflações | É quando os preços sobem e o mesmo dinheiro compra menos. |
| Selic | É o nome dos juros definidos pelo Banco Central, que influenciam o custo dos empréstimos. |
| Tarifa, tarifas, tarifário, tarifária | É o preço cobrado por um serviço ou, em algumas situações, um imposto sobre uma compra de fora. |
| Superaquecimento | É quando as compras crescem mais rápido que a quantidade de produtos à venda. |
| Subsídio, subsídios, subsidiar | É quando o governo paga parte de uma conta para ajudar alguém. |
| Fiscal, fiscais | Nos textos econômicos, é o que trata do dinheiro recebido e gasto pelo governo. |
| Monetária, monetário | É uma palavra usada para decisões sobre juros e dinheiro. |
| Hiato do produto | É a diferença entre o que o país produz e o que poderia produzir. |
| Anticíclica, anticíclico | É uma ação que tenta ajudar quando as vendas e o emprego caem. |
| Crowding out | É quando o governo toma dinheiro emprestado e dificulta empréstimos para empresas. |
| Absorção fiscal | É uma forma técnica de dizer que o governo paga a conta. |
| Racionamento | É um limite para quanto cada pessoa ou empresa pode consumir. |
| Condicionalidades, contrapartidas | São compromissos exigidos para receber ajuda. |
| Estabilização | É a tentativa de evitar mudanças muito bruscas nos preços ou na produção. |
| Desoneração, desonerar | É reduzir ou retirar um imposto. |
| Contingenciamento, contingenciar | É adiar ou suspender gastos previstos pelo governo. |
| Alíquota, alíquotas | É a parte do preço ou da renda cobrada como imposto. |
| Superávit | É o dinheiro que sobra quando o governo recebe mais do que gasta. |
| Austeridade | É uma política de cortar gastos ou aumentar impostos para melhorar as contas. |
| Macroeconômico, macroeconômica | É o que trata da economia do país como um todo. |
| Endógeno, endógena, endógenos, endógenas | É um efeito causado por mudanças dentro do próprio jogo. |
| Demanda agregada | É o total de compras feitas no país. |
| Insumos, insumo | São materiais usados para produzir, como peças, combustível e sementes. |
| Liquidez | É ter dinheiro disponível para pagar contas sem demora. |
| Solvência | É a capacidade de pagar as contas e as dívidas. |
| Ortodoxo, ortodoxa | Nesse contexto, é uma gestão que aposta em cortar gastos e controlar preços. |
| Trade-off, trade-offs | É uma escolha em que ganhar algo exige abrir mão de outra coisa. |
| Arrecadação | É o dinheiro que o governo recebe, principalmente em impostos. |
| Tesouro | É uma forma de falar do dinheiro e das contas do governo. |
| Patronais | É o que se refere às empresas que contratam trabalhadores. |
| Recolocação | É a busca de outro emprego por quem perdeu o trabalho. |
| Requalificação, requalificar | É aprender uma profissão ou habilidade nova para trabalhar. |
| Repasse, repassar | É cobrar de outra pessoa um aumento de custo que chegou antes. |
| Governabilidade | É a capacidade de manter apoio suficiente para continuar governando. |
| Insolvência | É não conseguir pagar as contas e dívidas. |
| Expansionista | É uma medida que tenta aumentar as compras, a produção ou os gastos. |
| Contracionista | É uma medida que tenta diminuir o ritmo das compras e dos gastos. |

## Ajuda na tela

O glossário é servido localmente, sem internet. Cada entrada contém id, termo, aliases, definicao, comoMuda e a marca de proibição. Novas entradas são marcadas automaticamente nas telas. Botões e links existentes não recebem botões aninhados. A ajuda usa uma janela modal pequena com foco e fechamento por Esc, Enter/Espaço nos indicadores e toque/clique. Nomes de indicador permanecem para ensinar o vocabulário; a explicação fica à mão.
