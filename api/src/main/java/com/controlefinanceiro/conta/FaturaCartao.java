package com.controlefinanceiro.conta;

import com.controlefinanceiro.empresa.Empresa;
import com.controlefinanceiro.importacao.model.ImportacaoDocumento;
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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Entidade da tabela fatura_cartao (DDL V3, fase 5): cabeçalho consolidado da fatura confirmada.
 *
 * <p>R3: empresa via {@link Empresa} (@ManyToOne), nunca de body. Valores em {@link BigDecimal} com
 * NUMERIC(14,2) — nenhum double/float. Criada uma vez por documento confirmado (idempotência).
 */
@Entity
@Table(name = "fatura_cartao")
public class FaturaCartao {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** R3: tenant sempre do token — nunca de body; NOT NULL no schema. */
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "empresa_id", nullable = false)
  private Empresa empresa;

  /** Usuário dono da fatura (isolamento financeiro por usuário). */
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "usuario_id", nullable = false)
  private Usuario usuario;

  /** Documento de importação que originou a fatura (NOT NULL no schema). */
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "documento_id", nullable = false)
  private ImportacaoDocumento documento;

  private LocalDate mesReferencia;

  private LocalDate dataFechamento;

  @Column(name = "data_vencimento", nullable = false)
  private LocalDate dataVencimento;

  /** LAN-05: NUMERIC(14,2), CHECK total >= 0. */
  @Column(nullable = false, precision = 14, scale = 2)
  private BigDecimal total;

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

  public ImportacaoDocumento getDocumento() {
    return documento;
  }

  public void setDocumento(ImportacaoDocumento documento) {
    this.documento = documento;
  }

  public LocalDate getMesReferencia() {
    return mesReferencia;
  }

  public void setMesReferencia(LocalDate mesReferencia) {
    this.mesReferencia = mesReferencia;
  }

  public LocalDate getDataFechamento() {
    return dataFechamento;
  }

  public void setDataFechamento(LocalDate dataFechamento) {
    this.dataFechamento = dataFechamento;
  }

  public LocalDate getDataVencimento() {
    return dataVencimento;
  }

  public void setDataVencimento(LocalDate dataVencimento) {
    this.dataVencimento = dataVencimento;
  }

  public BigDecimal getTotal() {
    return total;
  }

  public void setTotal(BigDecimal total) {
    this.total = total;
  }

  public OffsetDateTime getCriadoEm() {
    return criadoEm;
  }

  public void setCriadoEm(OffsetDateTime criadoEm) {
    this.criadoEm = criadoEm;
  }
}
