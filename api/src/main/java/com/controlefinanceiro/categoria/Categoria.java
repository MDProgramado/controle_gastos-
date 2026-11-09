package com.controlefinanceiro.categoria;

import com.controlefinanceiro.empresa.Empresa;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Entidade da tabela categoria (padrão P1; DDL do PLAN §3.4). R3: empresa via {@link Empresa}
 * (@ManyToOne), nunca de body. CAT-01: tipo ENTRADA | SAIDA. CAT-03: pessoal marca categorias [P].
 */
@Entity
@Table(name = "categoria")
public class Categoria {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** R3: tenant sempre do token — nunca de body; NOT NULL no schema. */
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "empresa_id", nullable = false)
  private Empresa empresa;

  @Column(nullable = false, length = 80)
  private String nome;

  @Column(nullable = false, length = 10)
  private String tipo;

  @Column(nullable = false)
  private boolean pessoal;

  @Column(nullable = false)
  private boolean ativo;

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

  public String getNome() {
    return nome;
  }

  public void setNome(String nome) {
    this.nome = nome;
  }

  public String getTipo() {
    return tipo;
  }

  public void setTipo(String tipo) {
    this.tipo = tipo;
  }

  public boolean isPessoal() {
    return pessoal;
  }

  public void setPessoal(boolean pessoal) {
    this.pessoal = pessoal;
  }

  public boolean isAtivo() {
    return ativo;
  }

  public void setAtivo(boolean ativo) {
    this.ativo = ativo;
  }
}
