package org.curioswitch.gradle.plugins.gcloud;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.io.File;
import java.util.List;
import javax.annotation.CheckReturnValue;
import javax.annotation.Generated;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * Immutable implementation of {@link PlatformConfig}.
 * <p>
 * Use the builder to create immutable instances:
 * {@code ImmutablePlatformConfig.builder()}.
 */
@SuppressWarnings({"all"})
@ParametersAreNonnullByDefault
@Generated({"Immutables.generator", "PlatformConfig"})
@Immutable
@CheckReturnValue
final class ImmutablePlatformConfig implements PlatformConfig {
  private final String gcloudExec;
  private final File gcloudBinDir;
  private final File sdkDir;
  private final String dependency;

  private ImmutablePlatformConfig(String gcloudExec, File sdkDir, String dependency) {
    this.gcloudExec = gcloudExec;
    this.sdkDir = sdkDir;
    this.dependency = dependency;
    this.gcloudBinDir = Preconditions.checkNotNull(PlatformConfig.super.gcloudBinDir(), "gcloudBinDir");
  }

  /**
   * @return The value of the {@code gcloudExec} attribute
   */
  @Override
  public String gcloudExec() {
    return gcloudExec;
  }

  /**
   * @return The computed-at-construction value of the {@code gcloudBinDir} attribute
   */
  @Override
  public File gcloudBinDir() {
    return gcloudBinDir;
  }

  /**
   * @return The value of the {@code sdkDir} attribute
   */
  @Override
  public File sdkDir() {
    return sdkDir;
  }

  /**
   * @return The value of the {@code dependency} attribute
   */
  @Override
  public String dependency() {
    return dependency;
  }

  /**
   * Copy the current immutable object by setting a value for the {@link PlatformConfig#gcloudExec() gcloudExec} attribute.
   * An equals check used to prevent copying of the same value by returning {@code this}.
   * @param value A new value for gcloudExec
   * @return A modified copy of the {@code this} object
   */
  public final ImmutablePlatformConfig withGcloudExec(String value) {
    if (this.gcloudExec.equals(value)) return this;
    String newValue = Preconditions.checkNotNull(value, "gcloudExec");
    return new ImmutablePlatformConfig(newValue, this.sdkDir, this.dependency);
  }

  /**
   * Copy the current immutable object by setting a value for the {@link PlatformConfig#sdkDir() sdkDir} attribute.
   * A shallow reference equality check is used to prevent copying of the same value by returning {@code this}.
   * @param value A new value for sdkDir
   * @return A modified copy of the {@code this} object
   */
  public final ImmutablePlatformConfig withSdkDir(File value) {
    if (this.sdkDir == value) return this;
    File newValue = Preconditions.checkNotNull(value, "sdkDir");
    return new ImmutablePlatformConfig(this.gcloudExec, newValue, this.dependency);
  }

  /**
   * Copy the current immutable object by setting a value for the {@link PlatformConfig#dependency() dependency} attribute.
   * An equals check used to prevent copying of the same value by returning {@code this}.
   * @param value A new value for dependency
   * @return A modified copy of the {@code this} object
   */
  public final ImmutablePlatformConfig withDependency(String value) {
    if (this.dependency.equals(value)) return this;
    String newValue = Preconditions.checkNotNull(value, "dependency");
    return new ImmutablePlatformConfig(this.gcloudExec, this.sdkDir, newValue);
  }

  /**
   * This instance is equal to all instances of {@code ImmutablePlatformConfig} that have equal attribute values.
   * @return {@code true} if {@code this} is equal to {@code another} instance
   */
  @Override
  public boolean equals(@Nullable Object another) {
    if (this == another) return true;
    return another instanceof ImmutablePlatformConfig
        && equalTo((ImmutablePlatformConfig) another);
  }

  private boolean equalTo(ImmutablePlatformConfig another) {
    return gcloudExec.equals(another.gcloudExec)
        && gcloudBinDir.equals(another.gcloudBinDir)
        && sdkDir.equals(another.sdkDir)
        && dependency.equals(another.dependency);
  }

  /**
   * Computes a hash code from attributes: {@code gcloudExec}, {@code gcloudBinDir}, {@code sdkDir}, {@code dependency}.
   * @return hashCode value
   */
  @Override
  public int hashCode() {
    int h = 5381;
    h += (h << 5) + gcloudExec.hashCode();
    h += (h << 5) + gcloudBinDir.hashCode();
    h += (h << 5) + sdkDir.hashCode();
    h += (h << 5) + dependency.hashCode();
    return h;
  }

  /**
   * Prints the immutable value {@code PlatformConfig} with attribute values.
   * @return A string representation of the value
   */
  @Override
  public String toString() {
    return MoreObjects.toStringHelper("PlatformConfig")
        .omitNullValues()
        .add("gcloudExec", gcloudExec)
        .add("gcloudBinDir", gcloudBinDir)
        .add("sdkDir", sdkDir)
        .add("dependency", dependency)
        .toString();
  }

  /**
   * Creates an immutable copy of a {@link PlatformConfig} value.
   * Uses accessors to get values to initialize the new immutable instance.
   * If an instance is already immutable, it is returned as is.
   * @param instance The instance to copy
   * @return A copied immutable PlatformConfig instance
   */
  public static ImmutablePlatformConfig copyOf(PlatformConfig instance) {
    if (instance instanceof ImmutablePlatformConfig) {
      return (ImmutablePlatformConfig) instance;
    }
    return ImmutablePlatformConfig.builder()
        .from(instance)
        .build();
  }

  /**
   * Creates a builder for {@link ImmutablePlatformConfig ImmutablePlatformConfig}.
   * @return A new ImmutablePlatformConfig builder
   */
  public static ImmutablePlatformConfig.Builder builder() {
    return new ImmutablePlatformConfig.Builder();
  }

  /**
   * Builds instances of type {@link ImmutablePlatformConfig ImmutablePlatformConfig}.
   * Initialize attributes and then invoke the {@link #build()} method to create an
   * immutable instance.
   * <p><em>{@code Builder} is not thread-safe and generally should not be stored in a field or collection,
   * but instead used immediately to create instances.</em>
   */
  @NotThreadSafe
  static final class Builder {
    private static final long INIT_BIT_GCLOUD_EXEC = 0x1L;
    private static final long INIT_BIT_SDK_DIR = 0x2L;
    private static final long INIT_BIT_DEPENDENCY = 0x4L;
    private long initBits = 0x7L;

    private @Nullable String gcloudExec;
    private @Nullable File sdkDir;
    private @Nullable String dependency;

    private Builder() {
    }

    /**
     * Fill a builder with attribute values from the provided {@code PlatformConfig} instance.
     * Regular attribute values will be replaced with those from the given instance.
     * Absent optional values will not replace present values.
     * @param instance The instance from which to copy values
     * @return {@code this} builder for use in a chained invocation
     */
    @CanIgnoreReturnValue 
    public final Builder from(PlatformConfig instance) {
      Preconditions.checkNotNull(instance, "instance");
      gcloudExec(instance.gcloudExec());
      sdkDir(instance.sdkDir());
      dependency(instance.dependency());
      return this;
    }

    /**
     * Initializes the value for the {@link PlatformConfig#gcloudExec() gcloudExec} attribute.
     * @param gcloudExec The value for gcloudExec 
     * @return {@code this} builder for use in a chained invocation
     */
    @CanIgnoreReturnValue 
    public final Builder gcloudExec(String gcloudExec) {
      this.gcloudExec = Preconditions.checkNotNull(gcloudExec, "gcloudExec");
      initBits &= ~INIT_BIT_GCLOUD_EXEC;
      return this;
    }

    /**
     * Initializes the value for the {@link PlatformConfig#sdkDir() sdkDir} attribute.
     * @param sdkDir The value for sdkDir 
     * @return {@code this} builder for use in a chained invocation
     */
    @CanIgnoreReturnValue 
    public final Builder sdkDir(File sdkDir) {
      this.sdkDir = Preconditions.checkNotNull(sdkDir, "sdkDir");
      initBits &= ~INIT_BIT_SDK_DIR;
      return this;
    }

    /**
     * Initializes the value for the {@link PlatformConfig#dependency() dependency} attribute.
     * @param dependency The value for dependency 
     * @return {@code this} builder for use in a chained invocation
     */
    @CanIgnoreReturnValue 
    public final Builder dependency(String dependency) {
      this.dependency = Preconditions.checkNotNull(dependency, "dependency");
      initBits &= ~INIT_BIT_DEPENDENCY;
      return this;
    }

    /**
     * Builds a new {@link ImmutablePlatformConfig ImmutablePlatformConfig}.
     * @return An immutable instance of PlatformConfig
     * @throws java.lang.IllegalStateException if any required attributes are missing
     */
    public ImmutablePlatformConfig build() {
      if (initBits != 0) {
        throw new IllegalStateException(formatRequiredAttributesMessage());
      }
      return new ImmutablePlatformConfig(gcloudExec, sdkDir, dependency);
    }

    private String formatRequiredAttributesMessage() {
      List<String> attributes = Lists.newArrayList();
      if ((initBits & INIT_BIT_GCLOUD_EXEC) != 0) attributes.add("gcloudExec");
      if ((initBits & INIT_BIT_SDK_DIR) != 0) attributes.add("sdkDir");
      if ((initBits & INIT_BIT_DEPENDENCY) != 0) attributes.add("dependency");
      return "Cannot build PlatformConfig, some of required attributes are not set " + attributes;
    }
  }
}
