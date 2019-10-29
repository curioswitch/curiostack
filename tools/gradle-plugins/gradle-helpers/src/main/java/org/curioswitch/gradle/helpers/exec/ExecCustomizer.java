/*
 * MIT License
 *
 * Copyright (c) 2019 Choko (choko@curioswitch.org)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.curioswitch.gradle.helpers.exec;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Streams;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.gradle.api.Project;
import org.gradle.process.BaseExecSpec;
import org.gradle.process.CommandLineArgumentProvider;
import org.gradle.process.ExecSpec;
import org.gradle.process.ProcessForkOptions;

/**
 * A {@link Serializable} {@link ExecSpec} for customizing execution of an external process from a
 * {@link org.gradle.workers.WorkerExecutor}.
 */
class ExecCustomizer implements ExecSpec, Serializable {

  // Only used to resolve files, not serialized.
  private final transient Project project;

  // Unlike the normal Exec, we force all values to String to make sure they're Serializable.
  // This works for most objects.
  @Nullable private String executable;
  @Nullable private List<String> arguments;
  @Nullable private Map<String, String> environment;
  @Nullable private Boolean ignoreExitValue;
  @Nullable private File workingDir;
  @Nullable private InputStream standardInput;
  @Nullable private OutputStream standardOutput;
  @Nullable private OutputStream standardError;

  ExecCustomizer(Project project) {
    this.project = checkNotNull(project, "project");
  }

  @Override
  public void setCommandLine(List<String> args) {
    commandLine(args);
  }

  @Override
  public void setCommandLine(Object... args) {
    commandLine(Arrays.asList(args));
  }

  @Override
  public void setCommandLine(Iterable<?> args) {
    commandLine(args);
  }

  @Override
  public ExecSpec commandLine(Object... args) {
    return commandLine(Arrays.asList(args));
  }

  @Override
  public ExecSpec commandLine(Iterable<?> args) {
    List<Object> argsList = ImmutableList.copyOf(args);
    executable(argsList.get(0));
    setArgs(argsList.subList(1, argsList.size()));
    return this;
  }

  @Override
  public ExecSpec args(Object... args) {
    return args(Arrays.asList(args));
  }

  @Override
  public ExecSpec args(Iterable<?> args) {
    Streams.stream(args).map(Object::toString).forEach(arguments()::add);
    return this;
  }

  @Override
  public ExecSpec setArgs(List<String> args) {
    return setArgs((Iterable<?>) args);
  }

  @Override
  public ExecSpec setArgs(Iterable<?> args) {
    arguments().clear();
    args(args);
    return this;
  }

  @Override
  public List<String> getArgs() {
    return arguments();
  }

  @Override
  public List<CommandLineArgumentProvider> getArgumentProviders() {
    // Can't customize an exec to have argument providers.
    return ImmutableList.of();
  }

  @Override
  public BaseExecSpec setIgnoreExitValue(boolean ignoreExitValue) {
    this.ignoreExitValue = ignoreExitValue;
    return this;
  }

  @Override
  public boolean isIgnoreExitValue() {
    return Boolean.TRUE.equals(ignoreExitValue);
  }

  @Override
  public BaseExecSpec setStandardInput(InputStream inputStream) {
    standardInput = inputStream;
    return this;
  }

  @Override
  @Nullable
  public InputStream getStandardInput() {
    return standardInput;
  }

  @Override
  public BaseExecSpec setStandardOutput(OutputStream outputStream) {
    standardOutput = outputStream;
    return this;
  }

  @Override
  @Nullable
  public OutputStream getStandardOutput() {
    return standardOutput;
  }

  @Override
  public BaseExecSpec setErrorOutput(OutputStream outputStream) {
    standardError = outputStream;
    return this;
  }

  @Override
  @Nullable
  public OutputStream getErrorOutput() {
    return standardError;
  }

  @Override
  public List<String> getCommandLine() {
    List<String> commandLine = new ArrayList<>();
    commandLine.add(getExecutable());
    commandLine.addAll(getArgs());
    return commandLine;
  }

  @Override
  @Nullable
  public String getExecutable() {
    return executable;
  }

  @Override
  public void setExecutable(String executable) {
    this.executable = executable;
  }

  @Override
  public void setExecutable(Object executable) {
    this.executable = executable == null ? null : executable.toString();
  }

  @Override
  public ProcessForkOptions executable(Object executable) {
    setExecutable(executable);
    return this;
  }

  @Override
  @Nullable
  public File getWorkingDir() {
    return workingDir;
  }

  @Override
  public void setWorkingDir(File dir) {
    workingDir = dir;
  }

  @Override
  public void setWorkingDir(Object dir) {
    setWorkingDir(project.file(dir));
  }

  @Override
  public ProcessForkOptions workingDir(Object dir) {
    setWorkingDir(dir);
    return this;
  }

  @Override
  public Map<String, Object> getEnvironment() {
    return ImmutableMap.copyOf(environment());
  }

  @Override
  public void setEnvironment(Map<String, ?> environmentVariables) {
    environment().clear();
    environmentVariables.forEach(
        (key, value) -> environment().put(key, value == null ? null : value.toString()));
  }

  @Override
  public ProcessForkOptions environment(Map<String, ?> environmentVariables) {
    setEnvironment(environmentVariables);
    return this;
  }

  @Override
  public ProcessForkOptions environment(String name, Object value) {
    environment().put(name, value == null ? null : value.toString());
    return this;
  }

  @Override
  public ProcessForkOptions copyTo(ProcessForkOptions options) {
    throw new UnsupportedOperationException("copyTo other options is not supported by this task.");
  }

  void copyTo(ExecSpec exec) {
    if (executable != null) {
      exec.executable(executable);
    }
    if (arguments != null) {
      exec.args(arguments);
    }
    if (environment != null) {
      exec.environment(environment);
    }
    if (ignoreExitValue != null) {
      exec.setIgnoreExitValue(ignoreExitValue);
    }
    if (workingDir != null) {
      exec.workingDir(workingDir);
    }
    if (standardInput != null) {
      exec.setStandardInput(standardInput);
    }
    if (standardOutput != null) {
      exec.setStandardOutput(standardOutput);
    }
    if (standardError != null) {
      exec.setErrorOutput(standardError);
    }
  }

  boolean isSerializable() {
    return standardInput == null && standardOutput == null && standardError == null;
  }

  @SuppressWarnings("UngroupedOverloads")
  private List<String> arguments() {
    if (arguments != null) {
      return arguments;
    }
    return arguments = new ArrayList<>();
  }

  @SuppressWarnings("UngroupedOverloads")
  private Map<String, String> environment() {
    if (environment != null) {
      return environment;
    }
    return environment = new HashMap<>();
  }
}
