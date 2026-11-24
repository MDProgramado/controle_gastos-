package com.controlefinanceiro.categoria.grupo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoriaGrupo503020Request(@NotBlank @Size(max = 16) String grupo) {}
