package com.controlefinanceiro.common;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaParameter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.Repository;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Barreiras estruturais do isolamento: nenhuma query financeira ou request de tenant/usuário. */
@AnalyzeClasses(
    packages = "com.controlefinanceiro",
    importOptions = ImportOption.DoNotIncludeTests.class)
class ArchUnitIsolamentoTest {

  @ArchTest
  static final ArchRule repositoriosFinanceirosExigemUsuarioId =
      classes()
          .that()
          .areAssignableTo(Repository.class)
          .and()
          .resideInAnyPackage(
              "com.controlefinanceiro.lancamento..",
              "com.controlefinanceiro.dashboard..",
              "com.controlefinanceiro.importacao..",
              "com.controlefinanceiro.conta..")
          .should(
              new ArchCondition<JavaClass>("expor metodo escalonado por usuarioId") {
                @Override
                public void check(JavaClass repositorio, ConditionEvents events) {
                  boolean escalado =
                      repositorio.getMethods().stream()
                          .anyMatch(
                              metodo ->
                                  metodo.getName().contains("UsuarioId")
                                      || metodo.getParameters().stream()
                                          .anyMatch(
                                              parametro ->
                                                  "usuarioId"
                                                      .equals(nomeParametro(metodo, parametro))));
                  if (!escalado) {
                    events.add(
                        SimpleConditionEvent.violated(
                            repositorio,
                            repositorio.getFullName() + " nao expoe metodo com usuarioId"));
                  }
                }
              });

  @ArchTest
  static final ArchRule requestsNaoAceitamTenantOuUsuario =
      classes()
          .that()
          .haveSimpleNameEndingWith("Request")
          .should(
              new ArchCondition<JavaClass>("nao declarar empresaId/usuarioId vindos do cliente") {
                @Override
                public void check(JavaClass request, ConditionEvents events) {
                  for (JavaField campo : request.getFields()) {
                    if (ehTenantOuUsuario(campo.getName())) {
                      events.add(
                          SimpleConditionEvent.violated(
                              request,
                              request.getFullName() + " declara campo " + campo.getName()));
                    }
                  }
                }
              });

  @ArchTest
  static final ArchRule controllersNaoRecebemTenantOuUsuarioDoCliente =
      classes()
          .that()
          .areAnnotatedWith(RestController.class)
          .should(
              new ArchCondition<JavaClass>(
                  "nao receber tenant/usuario em @RequestBody ou @RequestParam") {
                @Override
                public void check(JavaClass controller, ConditionEvents events) {
                  for (JavaMethod metodo : controller.getMethods()) {
                    for (JavaParameter parametro : metodo.getParameters()) {
                      boolean corpo = parametro.isAnnotatedWith(RequestBody.class);
                      boolean query = parametro.isAnnotatedWith(RequestParam.class);
                      if (!corpo && !query) {
                        continue;
                      }
                      String nome = nomeParametro(metodo, parametro);
                      if (ehTenantOuUsuario(nome)) {
                        events.add(
                            SimpleConditionEvent.violated(
                                controller,
                                controller.getFullName()
                                    + "."
                                    + metodo.getName()
                                    + " recebe "
                                    + nome));
                      }
                      if (corpo) {
                        for (JavaField campo : parametro.getRawType().getFields()) {
                          if (ehTenantOuUsuario(campo.getName())) {
                            events.add(
                                SimpleConditionEvent.violated(
                                    controller,
                                    parametro.getRawType().getFullName()
                                        + " declara campo "
                                        + campo.getName()));
                          }
                        }
                      }
                    }
                  }
                }
              });

  @ArchTest
  static final ArchRule memoriaDaIaNaoUsaMapaEmProducao =
      classes()
          .that()
          .haveSimpleName("AiMemoryStore")
          .should(
              new ArchCondition<JavaClass>("ter construtor @Autowired com repositorios JPA") {
                @Override
                public void check(JavaClass store, ConditionEvents events) {
                  boolean persistente =
                      store.getConstructors().stream()
                          .anyMatch(
                              construtor ->
                                  construtor.isAnnotatedWith(Autowired.class)
                                      && construtor.getParameters().stream()
                                              .filter(
                                                  parametro ->
                                                      parametro
                                                          .getRawType()
                                                          .isAssignableTo(Repository.class))
                                              .count()
                                          >= 2);
                  if (!persistente) {
                    events.add(
                        SimpleConditionEvent.violated(
                            store, "AiMemoryStore nao e injetado com repositorios JPA"));
                  }
                }
              });

  private static String nomeParametro(JavaMethod metodo, JavaParameter parametro) {
    return metodo.reflect().getParameters()[parametro.getIndex()].getName();
  }

  private static boolean ehTenantOuUsuario(String nome) {
    return "empresaId".equalsIgnoreCase(nome)
        || "usuarioId".equalsIgnoreCase(nome)
        || "empresa_id".equalsIgnoreCase(nome)
        || "usuario_id".equalsIgnoreCase(nome);
  }
}
