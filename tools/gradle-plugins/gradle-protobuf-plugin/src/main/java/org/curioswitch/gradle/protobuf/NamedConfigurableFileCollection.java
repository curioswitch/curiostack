/*
 * MIT License
 *
 * Copyright (c) 2018 Choko (choko@curioswitch.org)
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

package org.curioswitch.gradle.protobuf;

import groovy.lang.Closure;
import java.io.File;
import java.util.Iterator;
import java.util.Set;
import org.gradle.api.Named;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileTree;
import org.gradle.api.internal.file.FileCollectionInternal;
import org.gradle.api.internal.file.FileCollectionVisitor;
import org.gradle.api.internal.file.FileSystemSubset;
import org.gradle.api.reflect.HasPublicType;
import org.gradle.api.reflect.TypeOf;
import org.gradle.api.specs.Spec;
import org.gradle.api.tasks.TaskDependency;

/**
 * Wrapper around {@link ConfigurableFileCollection} to allow storing in a named container.
 * Hopefully Gradle will provide a better way of doing this eventually.
 */
public class NamedConfigurableFileCollection
    implements ConfigurableFileCollection, FileCollectionInternal, HasPublicType, Named {

  private final String name;
  private final ConfigurableFileCollection delegate;

  public NamedConfigurableFileCollection(String name, ConfigurableFileCollection delegate) {
    this.name = name;
    this.delegate = delegate;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public Set<Object> getFrom() {
    return delegate.getFrom();
  }

  @Override
  public void setFrom(Iterable<?> paths) {
    delegate.setFrom(paths);
  }

  @Override
  public void setFrom(Object... paths) {
    delegate.setFrom(paths);
  }

  @Override
  public ConfigurableFileCollection from(Object... paths) {
    return delegate.from(paths);
  }

  @Override
  public Set<Object> getBuiltBy() {
    return delegate.getBuiltBy();
  }

  @Override
  public ConfigurableFileCollection setBuiltBy(Iterable<?> tasks) {
    return delegate.setBuiltBy(tasks);
  }

  @Override
  public ConfigurableFileCollection builtBy(Object... tasks) {
    return delegate.builtBy(tasks);
  }

  @Override
  public File getSingleFile() {
    return delegate.getSingleFile();
  }

  @Override
  public Set<File> getFiles() {
    return delegate.getFiles();
  }

  @Override
  public boolean contains(File file) {
    return delegate.contains(file);
  }

  @Override
  public String getAsPath() {
    return delegate.getAsPath();
  }

  @Override
  public FileCollection plus(FileCollection collection) {
    return delegate.plus(collection);
  }

  @Override
  public FileCollection minus(FileCollection collection) {
    return delegate.minus(collection);
  }

  @Override
  public FileCollection filter(Closure filterClosure) {
    return delegate.filter(filterClosure);
  }

  @Override
  public FileCollection filter(Spec<? super File> filterSpec) {
    return delegate.filter(filterSpec);
  }

  @Override
  @Deprecated
  public Object asType(Class<?> type) {
    return delegate.asType(type);
  }

  @Override
  @Deprecated
  public FileCollection add(FileCollection collection) {
    return delegate.add(collection);
  }

  @Override
  public boolean isEmpty() {
    return delegate.isEmpty();
  }

  @Override
  @Deprecated
  public FileCollection stopExecutionIfEmpty() {
    return delegate.stopExecutionIfEmpty();
  }

  @Override
  public FileTree getAsFileTree() {
    return delegate.getAsFileTree();
  }

  @Override
  public void addToAntBuilder(Object builder, String nodeName, AntType type) {
    delegate.addToAntBuilder(builder, nodeName, type);
  }

  @Override
  public Object addToAntBuilder(Object builder, String nodeName) {
    return delegate.addToAntBuilder(builder, nodeName);
  }

  @Override
  public TaskDependency getBuildDependencies() {
    return delegate.getBuildDependencies();
  }

  @Override
  public Iterator<File> iterator() {
    return delegate.iterator();
  }

  @Override
  public TypeOf<?> getPublicType() {
    return TypeOf.typeOf(ConfigurableFileCollection.class);
  }

  @Override
  public void registerWatchPoints(FileSystemSubset.Builder builder) {
    ((FileCollectionInternal) delegate).registerWatchPoints(builder);
  }

  @Override
  public void visitRootElements(FileCollectionVisitor visitor) {
    ((FileCollectionInternal) delegate).visitRootElements(visitor);
  }
}
