package com.uber.okbuck.composer.base;

import com.uber.okbuck.OkBuckGradlePlugin;
import com.uber.okbuck.core.dependency.ExternalDependency;
import com.uber.okbuck.core.model.base.Target;
import com.uber.okbuck.core.model.jvm.JvmTarget;
import com.uber.okbuck.core.util.FileUtil;
import com.uber.okbuck.template.core.Rule;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.apache.commons.io.FilenameUtils;

public abstract class BuckRuleComposer {

  public BuckRuleComposer() {}

  public static Set<String> external(Set<ExternalDependency> deps) {
    return deps.stream().map(BuckRuleComposer::external).collect(Collectors.toSet());
  }

  public static String external(ExternalDependency dep) {
    return String.format("//%s:%s", dep.getTargetPath(), dep.getTargetName());
  }

  public static Set<String> externalApt(Set<ExternalDependency> deps) {
    return external(deps);
  }

  public static Set<String> targets(Set<Target> deps) {
    return deps.stream().map(BuckRuleComposer::targets).collect(Collectors.toSet());
  }

  private static String targets(Target dep) {
    return String.format("//%s:src_%s", dep.getPath(), dep.getName());
  }

  public static Set<String> targetsApt(Set<Target> deps) {
    return deps.stream()
        .filter(target -> target.getClass().equals(JvmTarget.class))
        .map(BuckRuleComposer::targets)
        .collect(Collectors.toSet());
  }

  public static String binTargets(Target dep) {
    return String.format("//%s:bin_%s", dep.getPath(), dep.getName());
  }

  @Nullable
  public static String fileRule(@Nullable String filePath) {
    if (filePath == null) {
      return null;
    }

    StringBuilder ext = new StringBuilder("//");
    ext.append(filePath);
    int ind = FilenameUtils.indexOfLastSeparator(filePath) + 2;
    if (ind >= 0) {
      return ext.replace(ind, ind + 1, ":").toString();
    }
    return ext.toString();
  }

  public static void composeBuckFile(Path path, List<Rule> rules) {
    File file = path.toFile();
    if (file.exists() || file.mkdirs()) {
      File buckFile = path.resolve(OkBuckGradlePlugin.BUCK).toFile();
      FileUtil.writeToBuckFile(rules, buckFile);
    } else {
      throw new RuntimeException(
          String.format("Failed to create directory at %s for composing buck file.", path));
    }
  }
}
