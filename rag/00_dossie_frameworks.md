---
id: "00-dossie-frameworks"
titulo: "Dossiê de Frameworks Financeiros para RAG — Comportamento, Estratégia e Regras"
tipo: "sintese_original"
idioma: "pt-BR"
licenca: "Conteúdo original, redigido para este corpus. Uso livre pelo proprietário da aplicação."
nivel: "iniciante, intermediario, avancado"
casos_de_uso: ["diagnostico_orcamentario","conselho_comportamental","educacao_investidor","explicacao_produtos","tratamento_dividas","guardrails_normativos"]
---

# Dossiê de Frameworks Financeiros para RAG

## 0. Para que serve este documento

Este é o documento de síntese do corpus. Ele não reproduz texto de obras comerciais:
ele consolida, em formato operacional, os **frameworks** que essas obras popularizaram,
para que um LLM consiga (a) diagnosticar a situação financeira de um usuário a partir de
lançamentos, (b) responder com o tom de voz correto e (c) citar regras verificáveis em vez
de improvisar. Sempre que um conceito vem de uma obra específica, o nome do autor é citado
no campo `origem_conceitual` — o que permite ao RAG atribuir a ideia sem reproduzir o livro.

Mapa de substituição (obra comercial → o que este dossiê entrega):

| Obra de referência | O que é aproveitado aqui |
|---|---|
| A Psicologia Financeira (Morgan Housel) | Seção 1 — princípios de comportamento, tom de voz empático |
| O Homem Mais Rico da Babilônia (George S. Clason, 1926) | Seção 3 — "pague-se primeiro", as Sete Curas, as Cinco Leis do Ouro (texto integral em domínio público no arquivo 10) |
| Como Organizar Sua Vida Financeira / A Riqueza da Vida Simples (Gustavo Cerbasi) | Seções 4 e 9 — orçamento doméstico, custos fixos, "ralos financeiros" |
| Me Poupe! (Nathalia Arcuri) | Seção 5 — trilha de saída de dívidas, linguagem acessível, metas curtas |
| All Your Worth / Método 50-30-20 (Elizabeth Warren e Amelia Warren Tyagi) | Seção 2 — framework matemático de diagnóstico |
| O Investidor de Bom Senso (John C. Bogle) | Seção 8 — custo, diversificação, passividade, longo prazo |
| Rápido e Devagar (Daniel Kahneman) / Previsivelmente Irracional (Dan Ariely) | Seção 7 — catálogo de vieses com sinais detectáveis em extratos |
| Mercado Financeiro: Produtos e Serviços (Eduardo Fortuna) e materiais CVM | Seções 10 e 11 — taxonomia de produtos, tributação, mecânica de juros |

---

## 1. Camada comportamental: finanças são comportamento

**Origem conceitual:** Morgan Housel (*A Psicologia Financeira*), Daniel Kahneman, Dan Ariely.

A tese central: resultados financeiros dependem menos de conhecimento técnico e mais de
**comportamento sustentável no tempo**. Planilhas perfeitas abandonadas em três semanas
perdem para um orçamento medíocre mantido por dez anos. Para o RAG, isso significa que a
resposta padrão a um usuário em dificuldade não é uma aula de matemática financeira — é
**redução de atrito e reforço de hábito**.

### 1.1 Princípios operacionalizáveis

Cada princípio abaixo tem um **gatilho de detecção** (o que olhar nos dados) e uma
**resposta recomendada** (o que a IA deve fazer).

1. **Sobrevivência antes de otimização.**
   - Gatilho: reserva de emergência < 1 mês de despesas essenciais.
   - Resposta: focar 100% em caixa e corte de despesa fixa; não falar de rentabilidade.

2. **Margem de segurança é o que separa plano de ruína.**
   - Gatilho: despesas fixas > 50% da renda líquida, ou nenhuma folga prevista para imprevisto.
   - Resposta: apontar a rigidez do orçamento e sugerir redução de comprometimento.

3. **Retorno composto precisa de tempo; tempo precisa de paciência.**
   - Gatilho: usuário resgatando investimento de longo prazo em < 12 meses.
   - Resposta: mostrar o custo de interromper a composição, não moralizar.

4. **Risco é o que sobra quando você acha que pensou em tudo.**
   - Gatilho: concentração > 40% do patrimônio em um único ativo/emissor, ou aposta em ativo único.
   - Resposta: explicar concentração x diversificação com números do próprio usuário.

5. **Suficiente é uma decisão, não um cálculo.**
   - Gatilho: aumento de padrão de vida proporcional (ou maior) ao aumento de renda, repetidamente.
   - Resposta: mostrar "quanto já é suficiente" no padrão atual e o efeito de travar o padrão de vida.

6. **Custo invisível é o inimigo silencioso.**
   - Gatilho: soma de tarifas, anuidade, juros de rotativo, taxas de fundo acima de 1% a.a.
   - Resposta: quantificar em reais/ano o que está sendo pago — sempre em valor absoluto.

7. **Dinheiro compra tempo, não coisas.**
   - Gatilho: gastos discricionários altos e tempo livre/estresse declarados como problema.
   - Resposta: reenquadrar metas em "meses de liberdade" em vez de "valor acumulado".

8. **Ninguém é racional com dinheiro sob pressão.**
   - Gatilho: mês com estresse financeiro (atrasos, uso de limite, empréstimo).
   - Resposta: tom empático primeiro, plano mínimo (1 ação) depois. Nunca duas ou três
     recomendações simultâneas em cenário de crise.

### 1.2 Regra de tom de voz (para o system prompt)

- Fale como um mentor direto e caloroso, não como um gerente de banco.
- Reconheça o fato antes de corrigir: "você gastou 38% acima da média em delivery — isso
  acontece em mês de mudança de rotina" em vez de "você está gastando demais".
- Toda resposta termina em **uma** ação concreta, com número e prazo.
- Nunca use culpa, vergonha ou comparação social como ferramenta de motivação.
- Em situação de superendividamento, priorize direitos e caminhos de negociação (Seção 13)
  antes de qualquer recomendação de investimento.

---

## 2. Método 50-30-20: o framework de diagnóstico orçamentário

**Origem conceitual:** Elizabeth Warren e Amelia Warren Tyagi (*All Your Worth*).

### 2.1 Definição canônica

Sobre a **renda líquida mensal** (após impostos e contribuições obrigatórias):

- **50% — Necessidades (Essenciais):** moradia (aluguel/financiamento, condomínio, IPTU),
  contas de consumo (água, luz, gás, internet básica), alimentação no mercado, transporte
  essencial, saúde (plano, remédios), educação obrigatória, seguros obrigatórios,
  parcelas mínimas de dívidas essenciais.
- **30% — Desejos (Estilo de vida):** delivery e restaurantes, streaming e assinaturas,
  lazer, viagens, roupas não essenciais, academia, presentes, transporte por app não essencial,
  upgrades.
- **20% — Futuro (Poupança e dívidas):** reserva de emergência, investimentos, aportes
  extras para quitar dívidas, previdência, metas de médio/longo prazo.

### 2.2 Regra de classificação para a IA (evitar ambiguidade)

Um mesmo lançamento pode ser essencial ou desejo. Regra de decisão:

1. Se o corte do item **ameaça moradia, saúde, trabalho ou alimentação básica** → Necessidade.
2. Se o item é a **versão premium** de algo necessário (plano de saúde com cobertura extra,
   academia por saúde) → divida: valor da alternativa básica em Necessidade, excedente em Desejo.
3. Se o item é **recorrente e cancelável sem perda estrutural** (streaming, assinatura) → Desejo.
4. Em dúvida, classifique como Desejo e sinalize `confianca_classificacao: media` no output —
   nunca silencie a incerteza.

### 2.3 Métricas de diagnóstico

Sendo `R` = renda líquida mensal, `N` = necessidades, `D` = desejos, `F` = futuro:

- `pct_necessidades = N / R`
- `pct_desejos = D / R`
- `pct_futuro = F / R`
- `taxa_poupanca = (aportes + reserva formada) / R`
- `comprometimento_renda_dividas = (parcelas de dívida) / R` (benchmark crítico: > 30% é zona de alerta)
- `indice_rigidez = despesas_fixas / R` (> 60% = orçamento sem folga)

Faixas de interpretação:

| `pct_necessidades` | Leitura |
|---|---|
| ≤ 50% | Saudável |
| 51–60% | Atenção: pouca folga para imprevisto |
| 61–70% | Risco: qualquer choque vira dívida |
| > 70% | Crítico: reestruturação obrigatória (moradia, transporte, contratos) |

| `pct_futuro` | Leitura |
|---|---|
| ≥ 20% | No caminho |
| 10–19% | Insuficiente para objetivos de longo prazo |
| 1–9% | Fase de estabilização: aceitável se dívida caro estiver sendo quitada |
| 0% | Prioridade absoluta: criar mecanismo automático de aporte |

### 2.4 Adaptação ao contexto brasileiro

O 50-30-20 pressupõe renda estável e custo de moradia controlado — premissas que não valem
para boa parte das famílias brasileiras com renda baixa e informal. Regras de adaptação:

- Renda < 2 salários mínimos e moradia > 40% da renda: aplicar **80-20** (80% essencial,
  20% futuro) e comunicar explicitamente que o 50-30-20 é meta de médio prazo, não de agora.
- Renda variável (autônomo, freelancer): usar **média dos últimos 6 meses** como `R` e
  calcular o orçamento sobre **80% dessa média** (colchão de variabilidade).
- Renda anual concentrada (13º, férias, bonificação): tratar como receita separada e
  destinar preferencialmente a reserva e dívida caro, com regra explícita definida antes.

---

## 3. "Pague-se primeiro": as Sete Curas e as Cinco Leis do Ouro

**Origem conceitual:** George S. Clason, *The Richest Man in Babylon* (1926, domínio público
nos EUA — texto integral em inglês no arquivo `10_clason_richest_man_babylon_1926_EN.txt`).

### 3.1 As Sete Curas para uma bolsa vazia (paráfrase operacional)

1. **Comece a engordar a bolsa:** separe ao menos **10% de tudo que ganha** antes de qualquer gasto.
2. **Controle os gastos:** o orçamento serve para identificar os "gastos que podem ser
   reduzidos" — não para proibir, mas para descobrir que os desejos crescem junto com a renda.
3. **Faça o ouro se multiplicar:** poupança parada perde para a inflação; o excedente precisa
   render, com risco compatível e orientação competente.
4. **Previna a perda:** antes de investir, verifique o principal (o risco de perder o capital
   é maior que o de ganhar pouco) e desconfie de retorno "garantido" acima do mercado.
5. **Faça da moradia um investimento lucrativo:** imóvel próprio dentro da capacidade de
   pagamento, não como aposta.
6. **Garanta renda futura:** proteja a capacidade de ganhar (qualificação, seguro, sucessão,
   aposentadoria).
7. **Aumente a capacidade de ganhar:** renda é o teto do patrimônio possível; elevar renda
   supera, no longo prazo, cortar café.

### 3.2 As Cinco Leis do Ouro (paráfrase)

1. O ouro vem, e em quantidade crescente, para quem guarda ao menos um décimo do que ganha.
2. O ouro trabalha de bom grado para o dono que lhe encontra uso lucrativo.
3. O ouro permanece com o dono cauteloso que o investe com o conselho de pessoas experientes.
4. O ouro escapa do dono que o aplica em negócios ou fins que não domina.
5. O ouro foge de quem o força a retornos impossíveis, segue promessas de ganho fácil ou
   confia em conselho de quem não tem experiência.

### 3.3 Aplicação no produto

- **Regra de aporte automático:** sugerir 10% como piso inicial, elevando para 20% conforme
  o orçamento permitir. Implementar como "valor mínimo de transferência no dia do recebimento",
  não como meta abstrata.
- **Sinal de alerta da Lei 4:** detectar lançamentos para ativos ilíquidos/opacos,
  "investimentos" com rendimento fixo prometido, cripto em corretora desconhecida.
- **Chunks curtos:** as parábolas de Clason funcionam melhor em fragmentos de 1 a 2 frases —
  ideais para dicas rápidas no chat.

---

## 4. Orçamento doméstico e estrutura de custos fixos

**Origem conceitual:** Gustavo Cerbasi (*Como Organizar Sua Vida Financeira*,
*A Riqueza da Vida Simples*) e Caderno de Educação Financeira do BCB (arquivo 01).

### 4.1 Sequência de implantação (4 semanas)

1. **Semana 1 — retrato:** levantar 3 meses de extratos e faturas; classificar por categoria.
2. **Semana 2 — verdade de caixa:** separar o que é fixo, variável e eventual; calcular o
   resultado médio mensal (não o melhor mês).
3. **Semana 3 — corte estrutural:** atacar contratos e recorrências (plano de celular,
   internet, seguros, assinaturas, tarifas bancárias). Ganho permanente x esforço único.
4. **Semana 4 — automação:** agendar transferências e pagamentos no dia do recebimento;
   orçamento que depende de disciplina diária falha.

### 4.2 Regras de bolso úteis (com faixa de referência)

| Item | Faixa de referência | Observação |
|---|---|---|
| Moradia (aluguel/financiamento + condomínio + contas) | até 30% da renda líquida | Acima de 35%: risco estrutural |
| Transporte | até 15% | Carro próprio costuma custar 2–3x o transporte público |
| Alimentação (mercado + fora) | 15–25% | Monitorar a divisão mercado x delivery |
| Saúde | até 10–15% | Não cortar; renegociar plano é melhor que cancelar |
| Educação | até 15% | Inclui cursos próprios |
| Lazer e desejos | 10–20% | Zerar lazer costuma destruir aderência ao plano |
| Reserva/investimento | ≥ 20% | Piso de 10% em fase de aperto |

### 4.3 Cartão de crédito: regras de uso consciente

- Cartão é **meio de pagamento**, não fonte de renda. Se a fatura não fecha integralmente no
  vencimento, o cartão virou crédito — e crédito rotativo é a linha mais caro do mercado.
- Limite de crédito não é renda disponível; usar > 30% do limite de forma recorrente
  compromete a análise de crédito futura e sinaliza dependência.
- Parcelamento "sem juros" reduz a liquidez futura: somar todas as parcelas em aberto e
  comparar com a renda mensal (comprometimento futuro).
- Assinaturas recorrentes no cartão são o principal "ralo" invisível: revisar a cada 90 dias.

### 4.4 "Ralos financeiros" (deteccão automática)

Sinais que a IA deve procurar nos extratos:

1. Assinaturas duplicadas do mesmo serviço (dois streamings iguais, dois seguros).
2. Cobranças recorrentes sem uso aparente (academia sem lançamento de frequência, clube).
3. Tarifas bancárias e de plataforma acumuladas acima de R$ 30/mês.
4. Juros e IOF em conta corrente/cheque especial — sinal de caixa descontrolado.
5. Compras fracionadas de baixo valor em alta frequência (i-food, conveniência, delivery):
   o "vazamento" costuma ser 8–15% da renda.
6. Reajustes anuais de contratos não contestados (plano de saúde, escola, seguro).
7. Financiamento de veículo com prazo > 48 meses e entrada pequena.

---

## 5. Trilha de saída de dívidas

**Origem conceitual:** Nathalia Arcuri (*Me Poupe!*), práticas de negociação e CDC (arquivo 03),
Lei 14.181/2021 (arquivo 04).

### 5.1 Diagnóstico antes do plano

Levantar, para cada dívida: credor, saldo, taxa mensal, parcela, atraso, garantia, e se há
cobrança judicial. Calcular:

- `custo_total_divida = soma dos pagamentos futuros` (não o saldo)
- `taxa_media_ponderada` das dívidas
- `meses_para_quitar` no ritmo atual

### 5.2 Ordem de ataque

1. **Dívidas com garantia real** (financiamento de imóvel/veículo): risco de perder o bem —
   prioridade máxima de manutenção do pagamento.
2. **Rotativo do cartão e cheque especial:** maior custo do sistema; substituir por linha
   mais barata é quase sempre a decisão de maior impacto.
3. **Crédito consignado e empréstimo pessoal:** taxas intermediárias.
4. **Dívidas sem juros / de baixo custo:** alongar é aceitável.

Duas estratégias válidas, e o critério de escolha deve ser declarado:

- **Avalanche (matemática):** atacar a maior taxa primeiro → menor custo total. Preferida
  quando o usuário tolera prazo longo sem desânimo.
- **Bola de neve (comportamental):** quitar as menores primeiro → vitórias rápidas, maior
  aderência. Preferida quando há histórico de abandono de planos.

### 5.3 Escada de substituição de dívida (do mais caro ao mais barato)

Rotativo do cartão → parcelamento de fatura → empréstimo pessoal → consignado → crédito com
garantia (imóvel/veículo) → uso de reserva. Cada degrau deve ser usado **apenas** para
quitar o anterior, nunca para ampliar consumo.

### 5.4 Negociação: sequência prática

1. Levantar o valor realmente disponível para pagamento à vista (poder de barganha).
2. Pedir proposta de quitação com desconto por escrito, com prazo de validade.
3. Nunca pagar a primeira parcela antes de ter o acordo documentado.
4. Priorizar acordos que **removem negativação** e dão quitação plena, não apenas "baixa".
5. Se houver múltiplas dívidas e incapacidade real de pagamento: avaliar repactuação em bloco
   (Seção 13.1, Lei 14.181/2021).

---

## 6. Juros: a matemática que decide o resultado

**Origem conceitual:** materiais oficiais (BCB, CVM) e formulação padrão de matemática financeira.

### 6.1 Fórmulas de referência

- Juros compostos: `VF = VP × (1 + i)^n`
- Taxa equivalente mensal ↔ anual: `i_anual = (1 + i_mensal)^12 − 1`
- Prestação (Tabela Price): `PMT = VP × i / (1 − (1 + i)^−n)`
- Valor presente de uma dívida: `VP = PMT × (1 − (1 + i)^−n) / i`
- Custo Efetivo Total (CET): taxa interna de retorno do fluxo de caixa **incluindo** tarifas,
  IOF, seguros e impostos. É o número que deve ser comparado — nunca a "taxa de juros" nominal.

### 6.2 Leitura correta das taxas

- 2% ao mês ≈ 26,8% ao ano. 10% ao mês ≈ 213,8% ao ano.
- Regra dos 72: `anos para dobrar ≈ 72 / taxa_anual(%)`. A 8% a.a., dobra em 9 anos;
  a 2% a.a., em 36 anos.
- Juros de rotativo de cartão no Brasil operam tipicamente na casa de **centenas de por cento
  ao ano** — motivo pelo qual a lei impôs teto de 100% sobre o valor original da dívida (Seção 13).

### 6.3 Regra de decisão para a IA

Compare sempre **custo do dinheiro** x **retorno esperado do dinheiro**:

- Se `taxa_da_dívida > retorno_líquido_esperado_do_investimento` → pagar dívida é o "melhor
  investimento" disponível, com retorno igual à taxa e risco praticamente nulo.
- Só faz sentido investir mantendo dívida caríssima quando existe reserva de emergência mínima
  já formada — e mesmo assim a dívida vem primeiro.

---

## 7. Vieses cognitivos: catálogo com sinais detectáveis

**Origem conceitual:** Daniel Kahneman (*Rápido e Devagar*), Dan Ariely (*Previsivelmente Irracional*).

Cada viés aqui vem com: definição curta, **sinal nos dados**, **resposta recomendada** e
**frase-âncora** para usar no chat.

1. **Custo irrecuperável (sunk cost)**
   - Sinal: manter assinatura/produto não usado porque "já paguei"; insistir em investimento ruim.
   - Resposta: reenquadrar para o futuro — o gasto passado não muda a decisão de hoje.
   - Frase: "O que já foi gasto não volta; o que importa é o que você paga a partir de agora."

2. **Aversão à perda**
   - Sinal: não vender ativo ruim; evitar renegociar por medo de "assumir prejuízo".
   - Resposta: mostrar o custo de manter (juros, taxa, oportunidade) em reais/mês.

3. **Viés do presente / desconto hiperbólico**
   - Sinal: parcelamento longo para consumo imediato; uso de crédito para antecipar prazer.
   - Resposta: mostrar o preço futuro do prazer presente (valor total x à vista).

4. **Efeito ancoragem**
   - Sinal: compras em promoção com "de/por"; orçamento ancorado no preço de tabela.
   - Resposta: comparar com o preço médio histórico do próprio usuário, não com a âncora do vendedor.

5. **Contabilidade mental**
   - Sinal: "dinheiro do 13º é para gastar", reserva intocável mas cartão estourado.
   - Resposta: consolidar visão de patrimônio líquido; mostrar que o dinheiro é fungível.

6. **Desconto de dupla entrada / falácia do "sem juros"**
   - Sinal: parcelamento "sem juros" em contexto onde há desconto à vista.
   - Resposta: calcular o custo implícito do "sem juros" comparando com o preço à vista.

7. **Excesso de confiança**
   - Sinal: concentração em ativo único, operações frequentes, day trade, alavancagem.
   - Resposta: mostrar dispersão de resultados e o efeito de errar uma única vez grande.

8. **Viés de manada**
   - Sinal: aporte após alta forte de mercado; compra de ativo "da moda".
   - Resposta: retomar plano e alocação-alvo; aporte por regra, não por notícia.

9. **Viés de status quo / inércia**
   - Sinal: taxa de fundo alta há anos, conta antiga sem revisão, assinatura esquecida.
   - Resposta: agendar revisão trimestral e transformar a revisão em tarefa com data.

10. **Efeito dotação**
    - Sinal: supervalorizar bem que já possui (imóvel, veículo) na avaliação patrimonial.
    - Resposta: usar valor de mercado realizável, não o valor afetivo.

Regra antiabuso: **nomear o viés com o usuário apenas quando isso ajudar**. Chamar um gasto de
"viés cognitivo" em momento de fragilidade produz vergonha e abandono do app.

---

## 8. Investimento de bom senso: custo, diversificação e tempo

**Origem conceitual:** John C. Bogle (*O Investidor de Bom Senso*).

### 8.1 Os quatro pilares

1. **Custo importa mais do que parece.** Uma taxa de administração de 2% a.a. sobre um retorno
   bruto de 8% a.a. consome 25% do retorno — e, composta por 30 anos, pode levar mais de um
   terço do patrimônio final. Toda resposta da IA sobre produto deve citar custo total
   (taxa de administração + performance + corretagem + impostos + spread).
2. **Diversificação é a única "refeição grátis".** Reduz risco sem reduzir retorno esperado
   proporcionalmente. No Brasil: diversificar por classe (renda fixa, variável, exterior),
   por emissor, por indexador e por prazo.
3. **Prazo longo é a principal vantagem do investidor individual.** Não há necessidade de
   bater o mercado; há necessidade de capturar o retorno do mercado.
4. **Simplicidade vence.** Carteira que o usuário entende e mantém > carteira ótima que ele
   abandona. Produtos simples (ETFs de índice, Tesouro Direto, fundos de baixo custo).

### 8.2 Alocação: como orientar sem dar recomendação individual

A IA **não** deve prescrever carteira. Deve:

- explicar o **trade-off** risco x retorno x liquidez x prazo;
- pedir (ou inferir) horizonte, tolerância a oscilação e necessidade de liquidez;
- mostrar intervalos de referência por perfil, sempre rotulados como educacionais;
- encaminhar para o teste de suitability da corretora (Resolução CVM aplicável).

Faixas educacionais de referência (ilustrativas, não recomendação):

| Perfil | Renda fixa pós | Renda fixa inflação | Renda variável BR | Exterior | Liquidez |
|---|---|---|---|---|---|
| Conservador | 80–100% | 0–20% | 0–10% | 0–10% | Alta |
| Moderado | 40–60% | 10–20% | 20–30% | 10–15% | Média |
| Arrojado | 20–40% | 10–20% | 30–45% | 15–25% | Baixa/média |

### 8.3 Reserva de emergência: dimensionamento

- `reserva_alvo = despesas_essenciais_mensais × fator`
- `fator` = 6 a 12 meses para CLT com renda estável; 12 a 18 meses para autônomo/PJ;
  3 a 6 meses para quem tem dupla renda e baixa rigidez orçamentária.
- Onde: liquidez diária, baixo risco de crédito, sem carência. Rentabilidade é critério
  secundário.
- Regra de uso: reserva cobre **parafuso**, não **estilo de vida**.

---

## 9. Eficiência de estilo de vida: reduzir custo fixo permanentemente

**Origem conceitual:** Gustavo Cerbasi (*A Riqueza da Vida Simples*).

A ideia operacional: **um real de custo fixo cortado vale mais que um real de gasto eventual
cortado**, porque se repete todo mês e libera capacidade de poupança estrutural.

Hierarquia de impacto:

1. **Moradia** (maior peso): renegociar aluguel, revisar financiamento, considerar mudança.
2. **Transporte** (segundo maior): carro próprio vs. app/transporte público — incluir
   depreciação, seguro, IPVA, manutenção.
3. **Contratos recorrentes**: telecom, energia (troca de plano), seguros, bancos.
4. **Assinaturas e clubes**: revisão trimestral com regra "cancelar antes de renovar".
5. **Alimentação fora**: maior ralo variável, menor impacto estrutural.

Métrica de acompanhamento: `custo_fixo_total / R` deve **cair** ao longo dos anos mesmo com
renda subindo — é a tradução numérica de "manter o padrão de vida enquanto a renda cresce".

---

## 10. Taxonomia de produtos de renda fixa no Brasil

**Origem conceitual:** materiais CVM (arquivos 05, 06, 07) — para detalhe normativo, consultar
o corpus; aqui vai a camada de leitura rápida.

| Produto | Emissor | Garantia | Risco de crédito | Liquidez típica | Tributação |
|---|---|---|---|---|---|
| Tesouro Selic | União | Soberana | Muito baixo | D+1 | IR regressivo |
| Tesouro Prefixado | União | Soberana | Muito baixo | D+1 (a mercado) | IR regressivo |
| Tesouro IPCA+ | União | Soberana | Muito baixo | D+1 (a mercado) | IR regressivo |
| CDB | Banco | FGC até o limite vigente | Baixo a médio | No vencimento ou diária | IR regressivo |
| LCI / LCA | Banco | FGC | Baixo a médio | Carência típica 90 dias | Isento de IR (PF) |
| CRI / CRA | Securitizadora | Sem FGC | Médio | Baixa | Isento de IR (PF) |
| Debênture | Empresa | Sem FGC | Médio a alto | Baixa | IR regressivo |
| Fundo DI | Gestora | Sem FGC | Baixo | D+0/D+1 | IR regressivo + come-cotas |
| Poupança | Banco | FGC | Baixo | Imediata | Isenta |

Regras de leitura que a IA deve aplicar sempre:

- **Rentabilidade líquida** é o único número comparável: bruta − IR − IOF − taxas.
- **IOF** incide sobre resgates em menos de 30 dias, com alíquota regressiva de 96% a 0%.
- **IR regressivo** em renda fixa: 22,5% (até 180 dias), 20% (181–360), 17,5% (361–720),
  15% (acima de 720 dias).
- **Isenção não é vantagem automática**: LCI de 90% do CDI pode render menos, líquida, que um
  CDB de 105% do CDI no mesmo prazo — calcular sempre.
- **FGC** tem limite por CPF e por instituição; acima do limite, o risco de crédito volta.
- **Marcação a mercado** explica por que um título "seguro" pode dar prejuízo se vendido antes
  do vencimento — reserva de emergência não deve ir para título longo.

---

## 11. Leitura de faturas e extratos: regras de extração

Diretrizes para o pipeline de ingestão de PDFs bancários:

1. **Normalizar antes de analisar:** descrição do lançamento em maiúsculas, remover ruído
   (`PIX ENVIADO`, `COMPRA CARTAO`, códigos), separar estabelecimento de identificadores.
2. **Deduplicar:** mesmo valor + mesma data + mesmo estabelecimento em dois arquivos
   (fatura e extrato) é o mesmo lançamento. Marcar `origem` e não somar duas vezes.
3. **Separar competência de caixa:** compra parcelada aparece na fatura, mas o compromisso
   futuro vive em `parcelas_em_aberto`. Nunca tratar parcela futura como despesa do mês.
4. **Estornos e pagamentos:** pagamento de fatura não é despesa (é transferência); estorno
   reduz a despesa original, não é receita.
5. **Transferências entre contas próprias:** identificar e excluir do cálculo de gasto.
6. **Saque e depósito em espécie:** tratar como não classificado; pedir confirmação ao usuário.
7. **Encargos:** juros, multa, IOF e tarifa têm categoria própria — nunca diluir em "outros".
8. **Classificação com confiança:** todo lançamento classificado deve carregar
   `confianca` (alta/média/baixa) e `categoria_sugerida`; lançamentos de baixa confiança
   entram em fila de revisão.

---

## 12. Regras de apresentação de números (anti-erro)

- Todo valor monetário em **R$ com duas casas** e separador brasileiro.
- Nunca apresentar rentabilidade sem prazo e sem base (bruta/líquida).
- Percentual de renda sempre acompanhado do valor absoluto ("32% = R$ 1.920").
- Comparações de período precisam do mesmo número de dias/meses.
- Arredondar para exibição, mas calcular com precisão total.
- Se algum dado estiver faltando, dizer o que falta — não estimar silenciosamente.

---

## 13. Camada normativa: o que a IA pode afirmar com segurança

Base: arquivos 02, 02b, 03, 04 do corpus. **Sempre citar o dispositivo.**

### 13.1 Superendividamento — Lei 14.181/2021 (altera o CDC)

- Reconhece o **superendividamento** como situação do consumidor pessoa natural, de boa-fé,
  impossibilitado de pagar todas as dívidas sem comprometer o **mínimo existencial**.
- Garante o direito à **repactuação e ao parcelamento** de dívidas, com preservação do mínimo
  existencial (a regulamentação do conceito é do Decreto nº 11.150/2022: preservação de renda
  para despesas essenciais).
- Institui a **conciliação** em bloco de dívidas e veda práticas de assédio de cobrança e
  incentivo ao crédito irresponsável.
- Consequência prática para o produto: em cenário crítico, orientar o usuário sobre
  repactuação e órgãos de defesa do consumidor (Procon, Defensoria) **antes** de sugerir
  empréstimo para pagar dívida.

### 13.2 Cartão de crédito — rotativo, parcelamento e teto de 100%

- **Resolução CMN nº 4.549/2017** (com alterações da Resolução CMN nº 5.112/2023):
  o saldo devedor da fatura só pode ser financiado no **rotativo até o vencimento da fatura
  seguinte**; depois disso, deve migrar para **parcelamento de fatura** em condições mais
  vantajosas que o rotativo.
- **Lei nº 14.690/2023, art. 28**: o total cobrado a título de **juros e encargos financeiros**
  não pode exceder o **valor original da dívida** (teto de 100%), somadas as renegociações.
- A fatura deve detalhar **valor original da dívida** e **total já cobrado** de juros e encargos.
- É assegurada **renegociação a qualquer momento** dentro do limite do teto.
- Portabilidade do saldo devedor entre instituições é permitida em condições mais vantajosas.
- Consequência para o produto: se a fatura do usuário mostra juros + encargos acima do valor
  original, sinalizar para revisão/contestação no banco e, se necessário, reclamação no BCB
  (Registrato) e Procon.

### 13.3 Pix — pontos com efeito prático

- O Regulamento do Pix (Resolução BCB nº 1/2020) define deveres dos participantes,
  prazos, regras de devolução e o **Mecanismo Especial de Devolução (MED)** para casos de
  fraude, com prazo de **até 80 dias** para devolução (com bloqueio cautelar em até 30 dias
  quando há fundos disponíveis).
- Contestação deve ser feita **na instituição do pagador**, que abre o MED; a devolução
  depende de saldo na conta do recebedor.
- Consequência para o produto: em caso de fraude, orientar ação imediata (contato com o banco,
  registro de boletim de ocorrência) — o fator tempo é decisivo.

### 13.4 CDC — dispositivos que sustentam o aconselhamento

- **Art. 6º, IV** — informação adequada e clara sobre produtos e serviços, com especificação de
  quantidade, características, composição, qualidade, tributos e **preço**.
- **Art. 42** — na cobrança de débitos, o consumidor inadimplente não será exposto a ridículo
  nem submetido a constrangimento; **repetição de indébito** dá direito a devolução em dobro
  com juros e correção (salvo engano justificável).
- **Art. 42-A** — vedação de publicidade de crédito que incentive o superendividamento e
  exigência de informação sobre taxa de juros, CET e riscos.
- **Art. 51, IV** — são nulas cláusulas abusivas que estabeleçam obrigações iníquas ou
  vantagem exagerada.
- Consequência para o produto: nunca orientar "deixe atrasar"; orientar negociação,
  documentação e canais formais.

### 13.5 Tributação de investimentos (referência rápida)

- Renda fixa PF: IR regressivo 22,5% → 15% conforme prazo (ver Seção 10);
  IOF regressivo nos primeiros 30 dias.
- Renda variável (ações, ETFs, FIIs): regras próprias de alíquota e apuração; FIIs têm
  isenção de IR sobre rendimentos para PF em condições legais, mas ganho de capital é tributado.
- Sempre indicar a data de vigência da regra e recomendar confirmação em fonte oficial.

---

## 14. Arquitetura de RAG recomendada para este corpus

### 14.1 Pipeline

1. **Ingestão** — PDFs oficiais e textos, com `pdftotext` ou parser estruturado; preservar
   cabeçalhos, títulos e numeração de artigos.
2. **Normalização** — reflow de parágrafos, remoção de rodapés repetidos, unificação de
   paginação; marcação de tabelas.
3. **Chunking híbrido** (ver 14.3).
4. **Enriquecimento** — metadados por chunk (schema em 14.2) e uma "frase de contexto" gerada
   no início do chunk (contextual retrieval), ex.: "Trecho da Resolução CMN 4.549 sobre
   teto de juros no rotativo do cartão".
5. **Indexação dupla** — vetorial (embeddings) + BM25/lexical. Norma jurídica precisa de busca
   literal por número de artigo; educação financeira precisa de semântica.
6. **Reranking** — modelo cross-encoder sobre top-k (20–50 → 5–8).
7. **Geração com citação obrigatória** — cada afirmação normativa referencia `source_id` e,
   quando houver, o dispositivo.
8. **Validação** — checagem de que toda citação existe no contexto recuperado; se não existir,
   bloquear a resposta e reformular.

### 14.2 Schema de metadados por chunk

```json
{
  "chunk_id": "bcb-res4549::art-2c",
  "source_id": "02-bcb-res4549",
  "titulo": "Resolução CMN 4.549/2017 — rotativo e parcelamento de fatura",
  "organizacao": "Banco Central do Brasil",
  "url": "https://normativos.bcb.gov.br/...",
  "licenca": "documento publico oficial",
  "tipo": "normativo",
  "nivel": "avancado",
  "tema": ["cartao_credito", "juros", "superendividamento"],
  "dispositivo": "art. 2-C",
  "vigencia": "atualizada em 2023-12-21",
  "aplicacao_produto": ["alerta_fatura", "explicacao_teto_juros"]
}
```

Campos obrigatórios: `chunk_id`, `source_id`, `tipo`, `nivel`, `tema`, `licenca`, `vigencia`.
`vigencia` é obrigatório para todo conteúdo normativo — regra desatualizada é o erro mais
grave possível neste domínio.

### 14.3 Estratégia de chunking por tipo de fonte

| Tipo | Tamanho-alvo | Overlap | Estratégia |
|---|---|---|---|
| Normativo (lei, resolução) | por artigo (300–900 chars) | 0 entre artigos | cortar em `Art.`, `§`, `Inciso`; preservar ementa no chunk |
| Educacional (cadernos, guias) | 800–1.200 chars | 120–150 | por seção e subtítulo, respeitando parágrafos |
| Pesquisa/relatório (ANBIMA, PEIC) | 800–1.200 chars | 100 | por seção; tabelas como chunk próprio em markdown |
| Parábola/dica curta (Clason) | 200–500 chars | 0 | por parágrafo; ideal para dica rápida |
| Papers técnicos | 1.000–1.500 chars | 150 | por seção, com título da seção no início |
| Dados tabulares de transação | 1 linha = 1 registro + agregações pré-computadas | n/a | nunca chunkar texto de tabela; usar função/consulta |

### 14.4 Dados tabulares (transações) — regra de ouro

Não jogue CSV de transações no índice vetorial e espere que o LLM some valores. O correto:

1. Agregar em **views materializadas** (por categoria/mês, por estabelecimento, por mês vs.
   média de 3 meses) e transformar cada agregação em um chunk de texto curto e factual.
2. Expor ferramentas determinísticas (SQL/funções) para cálculos exatos.
3. RAG recupera a **interpretação**; a ferramenta calcula o **número**.

Isso elimina a maior fonte de alucinação em apps de finanças pessoais.

### 14.5 Guardrails

- Se a pergunta exige número e não há ferramenta disponível → responder que não é possível
  calcular, e não estimar.
- Se a pergunta é normativa e o chunk recuperado tem `vigencia` anterior à última versão
  conhecida → avisar que pode haver atualização.
- Nunca prometer rentabilidade; nunca recomendar compra/venda de ativo específico; sempre
  apresentar risco e sugerir fonte/profissional certificado quando a decisão for relevante.
- Bloquear respostas sobre produtos específicos sem `disclaimer` de que não é recomendação.

### 14.6 Avaliação

Construir um *golden set* de 30–50 perguntas com resposta esperada e fonte:

- 10 de comportamento/orçamento (resposta livre, avaliada por rubrica)
- 10 de cálculo (resposta exata, avaliada por igualdade numérica)
- 10 normativas (resposta com dispositivo correto obrigatório)
- 10 adversariais (perguntas fora do escopo, dados faltantes, tentativa de obter recomendação
  individual)

Métricas: fidelidade (faithfulness), precisão do contexto recuperado, exatidão numérica,
taxa de citação correta e taxa de recusa apropriada.

---

## 15. Glossário operacional (pt-BR)

- **CET** — Custo Efetivo Total: taxa que inclui juros, tarifas, IOF e seguros.
- **CDI** — taxa de referência dos empréstimos entre bancos; base de rendimento de boa parte da renda fixa.
- **Selic** — taxa básica de juros da economia, definida pelo Copom.
- **IPCA** — índice oficial de inflação ao consumidor.
- **Rotativo** — crédito automático do cartão para o saldo não pago integralmente, de custo muito alto.
- **Parcelamento de fatura** — linha que substitui o rotativo após o vencimento seguinte, em condições melhores.
- **Mínimo existencial** — parcela da renda que deve ser preservada para despesas essenciais no superendividamento.
- **Suitability** — análise que verifica se o produto é adequado ao perfil do investidor.
- **Come-cotas** — antecipação semestral de IR em alguns fundos.
- **Marcação a mercado** — atualização diária do preço do título; explica ganho/perda antes do vencimento.
- **FGC** — fundo que garante depósitos até o limite legal em instituições participantes.
- **MED** — Mecanismo Especial de Devolução do Pix, usado em fraudes.
- **Comprometimento de renda** — percentual da renda mensal comprometido com parcelas de dívida.
- **Ralo financeiro** — despesa recorrente de baixo valor percebido e alto impacto acumulado.

---

## 16. Limites deste documento

1. Não é recomendação de investimento nem consultoria financeira personalizada.
2. Regras tributárias e normativas mudam: todo chunk normativo carrega `vigencia` e deve ser
   reindexado quando houver nova versão.
3. As faixas percentuais são referências educacionais brasileiras, não leis.
4. Obras comerciais citadas por título e autor **não** tiveram seu texto incluído neste corpus
   por razões de direito autoral; o conteúdo aqui é síntese original dos frameworks.
