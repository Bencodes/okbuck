package com.uber.okbuck.core.dependency;

import static com.uber.okbuck.core.dependency.BaseExternalDependency.AAR;
import static com.uber.okbuck.core.dependency.BaseExternalDependency.JAR;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.uber.okbuck.extension.ExternalDependenciesExtension;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;
import org.apache.commons.io.FilenameUtils;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;

/**
 * Represents a pre packaged dependency from an external source like gradle/maven cache or the
 * filesystem
 */
public final class ExternalDependency {
  private static final String SOURCE_FILE = "-sources.jar";
  private static final String LOCAL_DEP_VERSION = "1.0.0-LOCAL";
  private static final String LOCAL_GROUP = "local";

  private final BaseExternalDependency base;
  private final Path cachePath;

  @Nullable private Path realSourceFilePath;
  private boolean sourceFileInitialized;

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ExternalDependency that = (ExternalDependency) o;
    return Objects.equals(base, that.base);
  }

  @Override
  public int hashCode() {
    return this.base.hashCode();
  }

  @Override
  public String toString() {
    return this.base.toString();
  }

  /** Returns the group of the dependency. */
  public String getGroup() {
    return getVersionless().group();
  }

  /** Returns the name of the dependency. */
  public String getName() {
    return getVersionless().name();
  }

  /** Returns the VersionlessDependency of the dependency. */
  public VersionlessDependency getVersionless() {
    return this.base.versionless();
  }

  /** Returns the dependency as a Gradle Dependency. */
  public Dependency getAsGradleDependency() {
    return this.base.asGradleDependency();
  }

  /** Returns the version of the dependency. */
  public String getVersion() {
    return this.base.version();
  }

  /** Returns the real artifact file of the dependency. */
  public File getRealDependencyFile() {
    return this.base.realDependencyFile();
  }

  /** Returns the packaging of the the dependency: jar, aar, pex */
  public String getPackaging() {
    return this.base.packaging();
  }

  /** Returns the maven coordinates of the the dependency. */
  public String getMavenCoords() {
    return this.base.getMavenCoords();
  }

  /** Returns the maven coordinates of the the dependency source. */
  public String getSourceMavenCoords() {
    return this.base.getSourceMavenCoords();
  }

  /** Returns the target name of the dependency. */
  public String getTargetName() {
    return this.base.targetName() + "." + getPackaging();
  }

  /** Returns the target path of the dependency. */
  public String getTargetPath() {
    return cachePath.resolve(this.base.basePath()).toString();
  }

  /**
   * Gets the real sources jar path for a dependency if it exists.
   *
   * @param project The Project
   */
  void computeSourceFilePath(Project project) {
    if (!sourceFileInitialized) {
      realSourceFilePath = computeSourceFile(project);
      sourceFileInitialized = true;
    }
  }

  /** Gets the real sources jar file for the dependency if it exists. */
  @Nullable
  public File getRealSourceFile() {
    if (realSourceFilePath != null) {
      return realSourceFilePath.toFile();
    }
    return null;
  }

  /** Check whether the dependency has a sources jar file. */
  public boolean hasSourceFile() {
    return realSourceFilePath != null;
  }

  @Nullable
  private Path computeSourceFile(Project project) {
    if (!DependencyUtils.isWhiteListed(getRealDependencyFile())
        && ImmutableList.of(JAR, AAR).contains(getPackaging())) {

      String sourceFileName = getSourceFileNameFrom(getRealDependencyFile().getName());

      Path sourcesJar = getRealDependencyFile().getParentFile().toPath().resolve(sourceFileName);

      if (Files.exists(sourcesJar)) {
        return sourcesJar;
      } else if (!base.isLocal()) {
        // Most likely jar is in Gradle/Maven cache directory,
        // try to find sources jar in "jar/../..".
        return DependencyUtils.getSingleZipFilePath(
            project, getRealDependencyFile().getParentFile().getParentFile(), sourceFileName);
      }
    }
    return null;
  }

  private String getSourceFileNameFrom(String prebuiltName) {
    if (ImmutableList.of(JAR, AAR).contains(getPackaging())) {
      return prebuiltName.replaceFirst("\\.(jar|aar)$", SOURCE_FILE);
    }
    throw new RuntimeException("Couldn't get sources file name for " + prebuiltName);
  }

  private ExternalDependency(
      String group,
      String name,
      @Nullable String version,
      @Nullable String classifier,
      File depFile,
      boolean isLocal,
      ExternalDependenciesExtension extension) {
    VersionlessDependency versionlessDependency =
        VersionlessDependency.builder()
            .setGroup(group)
            .setName(name)
            .setClassifier(Optional.ofNullable(Strings.emptyToNull(classifier)))
            .build();

    this.base =
        BaseExternalDependency.builder()
            .setVersionless(versionlessDependency)
            .setVersion(Strings.isNullOrEmpty(version) ? LOCAL_DEP_VERSION : version)
            .setIsLocal(isLocal)
            .setIsVersioned(extension.isVersioned(versionlessDependency))
            .setRealDependencyFile(depFile)
            .build();

    this.cachePath = Paths.get(extension.getCache());
  }

  /**
   * Create an External Dependency
   *
   * @param group group of the dependency
   * @param name name of the dependency
   * @param version version of the dependency
   * @param dependencyFile file of the dependency
   * @param extension External Dependency Extension
   * @return External Dependency
   */
  public static ExternalDependency from(
      String group,
      String name,
      @Nullable String version,
      File dependencyFile,
      ExternalDependenciesExtension extension) {
    String classifier = DependencyUtils.getModuleClassifier(dependencyFile.getName(), version);
    return new ExternalDependency(
        group, name, version, classifier, dependencyFile, false, extension);
  }

  /**
   * Create an External Dependency from a local dependency
   *
   * @param localDep local dependency file
   * @param extension ExternalDependenciesExtension
   * @return External Dependency
   */
  public static ExternalDependency fromLocal(
      File localDep, ExternalDependenciesExtension extension) {
    return new ExternalDependency(
        LOCAL_GROUP,
        FilenameUtils.getBaseName(localDep.getName()),
        LOCAL_DEP_VERSION,
        null,
        localDep,
        true,
        extension);
  }
}
