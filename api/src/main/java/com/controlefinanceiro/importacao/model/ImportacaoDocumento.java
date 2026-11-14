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
import java.time.OffsetDateTime;

/**
 * Entidade da tabela importacao_documento (fase 5).
 *
 * <p>R3: empresa via {@link Empresa}, nunca de body. O texto bruto do PDF nunca Ã© persistido
 * (LGPD) â€” ficam apenas hash, metadados e o resultado estruturado.
 */
@Entity
@Table(name = "importacao_documento")
public class ImportacaoDocumento {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "empresa_id", nullable = false)
  private Empresa empresa;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "usuario_id", nullable = false)
  private Usuario usuario;

  @Column(nullable = false, length = 255)
  private String nomeArquivo;

  @Column(nullable = false, length = 16)
  private String tipoDocumento; // CARTAO | EXTRATO

  @Column(nullable = false, length = 64)
  private String hashSha256;

  @Column(nullable = false)
  private Integer tamanhoBytes;

  private Integer paginas;

  @Column(nullable = false, length = 16)
  private String status; // ANALISADO | CONFIRMADO | DESCARTADO | DUPLICADO | SEM_TEXTO

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

  public String getNomeArquivo() {
    return nomeArquivo;
  }

  public void setNomeArquivo(String nomeArquivo) {
    this.nomeArquivo = nomeArquivo;
  }

  public String getTipoDocumento() {
    return tipoDocumento;
  }

  public void setTipoDocumento(String tipoDocumento) {
    this.tipoDocumento = tipoDocumento;
  }

  public String getHashSha256() {
    return hashSha256;
  }

  public void setHashSha256(String hashSha256) {
    this.hashSha256 = hashSha256;
  }

  public Integer getTamanhoBytes() {
    return tamanhoBytes;
  }

  public void setTamanhoBytes(Integer tamanhoBytes) {
    this.tamanhoBytes = tamanhoBytes;
  }

  public Integer getPaginas() {
    return paginas;
  }

  public void setPaginas(Integer paginas) {
    this.paginas = paginas;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public OffsetDateTime getCriadoEm() {
    return criadoEm;
  }

  public void setCriadoEm(OffsetDateTime criadoEm) {
    this.criadoEm = criadoEm;
  }
}
