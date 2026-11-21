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
 * Entidade da tabela importacao_item (fase 5).
 *
 * <p>Item de staging extraÃ­do por IA, ainda nÃ£o gravado em lancamento/conta_a_pagar. R3: empresa
 * via {@link Empresa}, nunca de body. Valores monetÃ¡rios em {@link BigDecimal} com NUMERIC(14,2),
 * scale=2, nenhum double/float.
 */
@Entity
@Table(name = "importacao_item")
public class ImportacaoItem {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "empresa_id", nullable = false)
  private Empresa empresa;

  /** Usuário dono do item de staging (isolamento financeiro por usuário). */
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "usuario_id", nullable = false)
  private Usuario usuario;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "documento_id", nullable = false)
  private ImportacaoDocumento documento;

  @Column(nullable = false)
  private LocalDate data;

  @Column(nullable = false, length = 200)
  private String descricao;

  @Column(nullable = false, precision = 14, scale = 2)
  private BigDecimal valor;

  @Column(nullable = false, length = 16)
  private String tipo; // RECEITA | DESPESA_PAGA | A_PAGAR

  @Column(length = 80)
  private String categoriaNome;

  @Column(name = "categoria_id")
  private Long categoriaId;

  @Column(nullable = false, length = 16)
  private String origem; // CARTAO | EXTRATO

  private Integer parcelaAtual;

  private Integer parcelaTotal;

  @Column(nullable = false, length = 16)
  private String status = "PENDENTE"; // PENDENTE | IGNORADO | IMPORTADO

  @Column(name = "lancamento_id")
  private Long lancamentoId;

  @Column(name = "conta_a_pagar_id")
  private Long contaAPagarId;

  @Column(length = 200)
  private String alerta;

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

  public LocalDate getData() {
    return data;
  }

  public void setData(LocalDate data) {
    this.data = data;
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

  public String getTipo() {
    return tipo;
  }

  public void setTipo(String tipo) {
    this.tipo = tipo;
  }

  public String getCategoriaNome() {
    return categoriaNome;
  }

  public void setCategoriaNome(String categoriaNome) {
    this.categoriaNome = categoriaNome;
  }

  public Long getCategoriaId() {
    return categoriaId;
  }

  public void setCategoriaId(Long categoriaId) {
    this.categoriaId = categoriaId;
  }

  public String getOrigem() {
    return origem;
  }

  public void setOrigem(String origem) {
    this.origem = origem;
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

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public Long getLancamentoId() {
    return lancamentoId;
  }

  public void setLancamentoId(Long lancamentoId) {
    this.lancamentoId = lancamentoId;
  }

  public Long getContaAPagarId() {
    return contaAPagarId;
  }

  public void setContaAPagarId(Long contaAPagarId) {
    this.contaAPagarId = contaAPagarId;
  }

  public String getAlerta() {
    return alerta;
  }

  public void setAlerta(String alerta) {
    this.alerta = alerta;
  }

  public OffsetDateTime getCriadoEm() {
    return criadoEm;
  }

  public void setCriadoEm(OffsetDateTime criadoEm) {
    this.criadoEm = criadoEm;
  }
}
