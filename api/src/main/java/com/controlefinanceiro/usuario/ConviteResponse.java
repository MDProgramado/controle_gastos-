package com.controlefinanceiro.usuario;

import java.time.OffsetDateTime;

public record ConviteResponse(String token, OffsetDateTime expiraEm) {}
