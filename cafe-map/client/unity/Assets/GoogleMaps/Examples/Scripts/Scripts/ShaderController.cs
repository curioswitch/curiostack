using UnityEngine;

/// <summary>
/// Script for switching the <see cref="Shader"/> currently applied to a given set of
/// <see cref="Material"/>s.
/// </summary>
public sealed class ShaderController : MonoBehaviour {
  [Tooltip("Materials to control shaders of.")]
  public Material[] Materials;

  [Tooltip("Shader all materials are assumed to start with, and shader to switch to when "
      + "SwitchTo(false) is called.")]
  public Shader First;

  [Tooltip("Shader to when SwitchTo(true) is called.")]
  public Shader Second;

  /// <summary>Flag indicating which shader is in use.</summary>
  private bool UsingSecond;

  /// <summary>Have any <see cref="Material"/>s been set?</summary>
  private bool HasMaterials;

  /// <summary>
  /// Verify all required parameters are defined and correctly setup, skipping any further setup if
  /// any parameter is missing or invalid.
  /// </summary>
  private void Awake () {
    // Verify that have been given needed shaders.
    if (First == null) {
      // Note: 'name' and 'GetType()' just give the name of the GameObject this script is on, and
      // the name of this script respectively.
      Debug.LogErrorFormat("No First Shader defined for {0}.{1}, which needs this Shader to run!",
        name, GetType());
      return;
    }
    if (Second == null) {
      Debug.LogErrorFormat("No Second Shader defined for {0}.{1}, which needs this Shader to run!",
        name, GetType());
      return;
    }

    // See if any Materials have been given.
    HasMaterials = Materials.Length != 0;
    if (!HasMaterials) {
      return;
    }

    // Verify defined Materials array. We do this after have checked if no Materials have been
    // given, to allow for an empty Materials array to be defined in parameters and then set using
    // SetMaterials function later. As such, the contents of the Materials array is only checked
    // here if an array of starting Materials was defined as a parameter.
    VerifyMaterials(Materials, "defined for");
  }

  /// <summary>
  /// Give this class an array of <see cref="Material"/>s to control the <see cref="Shader"/>s of.
  /// </summary>
  internal void SetMaterials(Material[] materials) {
    // Verify given Materials array.
    if (!VerifyMaterials(materials, "given to")) {
      return;
    }

    // If have reached this point then given Material array is valid, so set it as the Materials
    // array this class controls and flag that now have Materials to control.
    Materials = materials;
    HasMaterials = true;
  }

  /// <summary>Set a given '_Value' float on all controller <see cref="Material"/>s.</summary>
  internal void SetValue(float newValue) {
    // Verify have any controlled Materials to set.
    if (!HasMaterials) {
      Debug.LogWarningFormat("Attempt to call SetValue on {0}.{1} with a value of {2:N2}, but {1} "
          + "is controlling no Materials, as none were defined as {0}.{1}.Materials nor given "
          + "using {0}.{1}.SetMaterials.",
          name, GetType(), newValue);
      return;
    }

    // Set value on all materials
    foreach (Material material in Materials) {
      material.SetFloat("_Value", newValue);
    }
  }

  /// <summary>Switch which <see cref="Shader"/> all <see cref="Material"/> are using.</summary>
  internal void Switch() {
    // Verify have any controlled Materials to set.
    if (!HasMaterials) {
      Debug.LogWarningFormat("Attempt to call Switch on {0}.{1}, but {1} is controlling no "
          + "Materials, as none were defined as {0}.{1}.Materials nor given using "
          + "{0}.{1}.SetMaterials.",
          name, GetType());
      return;
    }

    // If using first shader, switch to second, and vice versa.
    SwitchShadersTo(!UsingSecond);
  }

  /// <summary>Switch which <see cref="Shader"/> all <see cref="Material"/> are using.</summary>
  /// <param name="second">True to switch to second <see cref="Shader"/>, false for first.</param>
  internal void SwitchTo(bool second) {
    // Verify have any controlled Materials to set.
    if (!HasMaterials) {
      Debug.LogWarningFormat("Attempt to call SwitchTo({2}) on {0}.{1}, but {1} is controlling no "
          + "Materials, as none were defined as {0}.{1}.Materials nor given using "
          + "{0}.{1}.SetMaterials.",
          name, GetType(), second);
      return;
    }

    // Switch to desired shader.
    SwitchShadersTo(second);
  }

  /// <summary>Switch which <see cref="Shader"/> all <see cref="Material"/> are using.</summary>
  /// <param name="second">True to switch to first <see cref="Shader"/>, false for second.</param>
  private void SwitchShadersTo(bool second) {
    UsingSecond = second;
    Shader newShader = UsingSecond ? Second : First;
    foreach (Material material in Materials) {
      material.shader = newShader;
    }
  }

  /// <summary>
  /// Verify a given array of <see cref="Material"/> is valid (not null, nor empty, nor containing a
  /// null <see cref="Material"/>), returning false if any problems are detected.
  /// </summary>
  /// <param name="materials">Array of <see cref="Material"/> to check.</param>
  /// <param name="receivedPhrase">
  /// If array is invalid, use this phrase in error messages to describe how this array was
  /// received (e.g. was it 'given to' (a function), 'defined as' (a parameter) etc).
  /// </param>
  private bool VerifyMaterials(Material[] materials, string receivedPhrase) {
    // Verify array is not null.
    if (materials == null) {
      Debug.LogErrorFormat("Null array of Materials {0} {1}.{2}, which needs a non-null Materials "
        + "array to run!",
        receivedPhrase, name, GetType());
      return false;
    }

    // Verify that at least one Material has been given.
    if (materials.Length == 0) {
      Debug.LogErrorFormat("Empty Materials array {0} {1}.{2}, which needs Materials a non-empty "
        + "Materials array to run!",
        receivedPhrase, name, GetType());
      return false;
    }

    // Verify that no null Materials have been given.
    for (int i = 0; i < materials.Length; i++) {
      if (materials[i] == null) {
        Debug.LogErrorFormat("Null Material {0} {1}.{2}, specifically Material {3} of {4} in given "
          + "Material array.",
          receivedPhrase, name, GetType(), i + 1, materials.Length);
        return false;
      }
    }

    // If have reached this point then have verified this is a non-null, non-empty array of
    // Materials whose values are all non-null.
    return true;
  }
}
