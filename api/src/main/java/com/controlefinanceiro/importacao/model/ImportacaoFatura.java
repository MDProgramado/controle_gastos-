package com.controlefinanceiro.importacao.model;

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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Entidade da tabela importacao_fatura (fase 5).
 *
 * <p>CabeÃ§alho extraÃ­do da fatura de cartÃ£o pelo staging. R3: empresa via {@link Empresa}, nunca
 * de body.
 */
@Entity
@Table(name = "importacao_fatura")
public class ImportacaoFatura {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "empresa_id", nullable = false)
  private Empresa empresa;

  /** Usuário dono do cabeçalho de staging (isolamento financeiro por usuário). */
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "usuario_id", nullable = false)
  private Usuario usuario;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "documento_id", nullable = false)
  private ImportacaoDocumento documento;

  @Column(nullable = false, precision = 14, scale = 2)
  private BigDecimal total;

  @Column(nullable = false)
  private LocalDate dataVencimento;

  private LocalDate mesReferencia;

  private LocalDate dataFechamento;

  @Column(name = "criado_em", nullable = false, updatable = false)
  private OffsetDateTime criadoEm;

  /**
   * R3: timestamp preenchido pela aplicaÃ§Ã£o (o DEFAULT now() do banco nÃ£o Ã© visto pelo INSERT
   * do Hibernate).
   */
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

  public BigDecimal getTotal() {
    return total;
  }

  public void setTotal(BigDecimal total) {
    this.total = total;
  }

  public LocalDate getDataVencimento() {
    return dataVencimento;
  }

  public void setDataVencimento(LocalDate dataVencimento) {
    this.dataVencimento = dataVencimento;
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

  public OffsetDateTime getCriadoEm() {
    return criadoEm;
  }

  public void setCriadoEm(OffsetDateTime criadoEm) {
    this.criadoEm = criadoEm;
  }
}
