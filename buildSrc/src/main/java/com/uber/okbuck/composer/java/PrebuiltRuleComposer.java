package com.uber.okbuck.composer.java;

import static com.uber.okbuck.core.dependency.BaseExternalDependency.AAR;
import static com.uber.okbuck.core.dependency.BaseExternalDependency.JAR;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.uber.okbuck.composer.jvm.JvmBuckRuleComposer;
import com.uber.okbuck.core.dependency.DependencyUtils;
import com.uber.okbuck.core.dependency.ExternalDependency;
import com.uber.okbuck.template.core.Rule;
import com.uber.okbuck.template.java.Prebuilt;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class PrebuiltRuleComposer extends JvmBuckRuleComposer {

  /**
   * @param dependencies External Dependencies whose rule needs to be created
   * @return List of rules
   */
  @SuppressWarnings("NullAway")
  public static List<Rule> compose(Collection<ExternalDependency> dependencies) {
    return dependencies
        .stream()
        .peek(
            dependency -> {
              if (!ImmutableSet.of(JAR, AAR).contains(dependency.getPackaging())) {
                throw new IllegalStateException("Dependency not a valid prebuilt: " + dependency);
              }
            })
        .sorted(
            (o1, o2) ->
                ComparisonChain.start()
                    .compare(o1.getPackaging(), o2.getPackaging())
                    .compare(o1.getTargetName(), o2.getTargetName())
                    .result())
        .map(
            dependency -> {
              Prebuilt rule =
                  new Prebuilt()
                      .mavenCoords(dependency.getMavenCoords())
                      .sha256(DependencyUtils.sha256(dependency.getRealDependencyFile()));
              if (dependency.hasSourceFile()) {
                rule.sourcesMavenCoords(dependency.getSourceMavenCoords())
                    .sourcesSha256(DependencyUtils.sha256(dependency.getRealSourceFile()));
              }
              rule.name(dependency.getTargetName());
              return ImmutableList.of(rule);
            })
        .flatMap(Collection::stream)
        .collect(Collectors.toList());
  }
}
