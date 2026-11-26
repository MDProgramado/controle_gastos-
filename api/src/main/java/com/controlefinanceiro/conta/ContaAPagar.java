package com.controlefinanceiro.conta;

import com.controlefinanceiro.categoria.Categoria;
import com.controlefinanceiro.empresa.Empresa;
import com.controlefinanceiro.usuario.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Entidade da tabela conta_a_pagar (DDL V3, fase 5): compromisso futuro gerado por item A_PAGAR.
 *
 * <p>R3: empresa via {@link Empresa} (@ManyToOne), nunca de body. NÃO entra no saldo do dashboard
 * (D-11): o saldo soma apenas {@code lancamento.tipo = ENTRADA|SAIDA}. Categoria obrigatoriamente
 * do tipo SAIDA (integridade CAT-04). Valores em {@link BigDecimal} com NUMERIC(14,2).
 */
@Entity
@Table(name = "conta_a_pagar")
public class ContaAPagar {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** R3: tenant sempre do token — nunca de body; NOT NULL no schema. */
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "empresa_id", nullable = false)
  private Empresa empresa;

  /** Usuário dono do compromisso (isolamento financeiro por usuário). */
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "usuario_id", nullable = false)
  private Usuario usuario;

  /** Fatura de origem (quando o compromisso veio de fatura de cartão). */
  @Column(name = "fatura_id")
  private Long faturaId;

  /** Categoria do tipo SAIDA (NOT NULL + FK no schema). */
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "categoria_id", nullable = false)
  private Categoria categoria;

  @Column(nullable = false, length = 200)
  private String descricao;

  /** LAN-05: NUMERIC(14,2), CHECK valor > 0. */
  @Column(nullable = false, precision = 14, scale = 2)
  private BigDecimal valor;

  @Column(nullable = false)
  private LocalDate data;

  private Integer parcelaAtual;

  private Integer parcelaTotal;

  private LocalDate vencimento;

  @Column(nullable = false, length = 16)
  private String origem; // CARTAO | EXTRATO

  @Column(nullable = false)
  private boolean pago;

  private OffsetDateTime pagoEm;

  @Column(name = "lancamento_id")
  private Long lancamentoId;

  @Version
  @Column(nullable = false)
  private int versao;

  @Column(name = "criado_em", nullable = false, updatable = false)
  private OffsetDateTime criadoEm;

  @PrePersist
  void aoPersistir() {
    if (criadoEm == null) {
      criadoEm = OffsetDateTime.now();
    }
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Empresa getEmpresa() {
    return empresa;
  }

  public void setEmpresa(Empresa empresa) {
    this.empresa = empresa;
  }

  public Usuario getUsuario() {
    return usuario;
  }

  public void setUsuario(Usuario usuario) {
    this.usuario = usuario;
  }

  public Long getFaturaId() {
    return faturaId;
  }

  public void setFaturaId(Long faturaId) {
    this.faturaId = faturaId;
  }

  public Categoria getCategoria() {
    return categoria;
  }

  public void setCategoria(Categoria categoria) {
    this.categoria = categoria;
  }

  public String getDescricao() {
    return descricao;
  }

  public void setDescricao(String descricao) {
    this.descricao = descricao;
  }

  public BigDecimal getValor() {
    return valor;
  }

  public void setValor(BigDecimal valor) {
    this.valor = valor;
  }

  public LocalDate getData() {
    return data;
  }

  public void setData(LocalDate data) {
    this.data = data;
  }

  public Integer getParcelaAtual() {
    return parcelaAtual;
  }

  public void setParcelaAtual(Integer parcelaAtual) {
    this.parcelaAtual = parcelaAtual;
  }

  public Integer getParcelaTotal() {
    return parcelaTotal;
  }

  public void setParcelaTotal(Integer parcelaTotal) {
    this.parcelaTotal = parcelaTotal;
  }

  public LocalDate getVencimento() {
    return vencimento;
  }

  public void setVencimento(LocalDate vencimento) {
    this.vencimento = vencimento;
  }

  public String getOrigem() {
    return origem;
  }

  public void setOrigem(String origem) {
    this.origem = origem;
  }

  public boolean isPago() {
    return pago;
  }

  public void setPago(boolean pago) {
    this.pago = pago;
  }

  public OffsetDateTime getPagoEm() {
    return pagoEm;
  }

  public void setPagoEm(OffsetDateTime pagoEm) {
    this.pagoEm = pagoEm;
  }

  public Long getLancamentoId() {
    return lancamentoId;
  }

  public void setLancamentoId(Long lancamentoId) {
    this.lancamentoId = lancamentoId;
  }

  public int getVersao() {
    return versao;
  }

  public OffsetDateTime getCriadoEm() {
    return criadoEm;
  }

  public void setCriadoEm(OffsetDateTime criadoEm) {
    this.criadoEm = criadoEm;
  }
}
