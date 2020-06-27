using UnityEngine;
using Random = UnityEngine.Random;

namespace Google.Maps.Examples {
  /// <summary>
  /// Class for setting the emission value for a range of Materials.
  /// </summary>
  public sealed class EmissionController : MonoBehaviour {
    [Tooltip(
        "Start/current emission value, in the range from 0f to 1f, where 0f means no emission " +
        "and 1f means full emission.")]
    public float Value = 0.5f;

    [Tooltip(
        "Materials to control emission of. Skipped if no Materials given, and error printed if " +
        "any given materials are null.")]
    public Material[] Materials;

    [SerializeField, Tooltip("Name of emission color variable in given Material-Shaders.")]
    public string EmissionVariable = "_EmitColor";

    [Tooltip(
        "Minimum emission color to apply when emission is 100%. Emission colors are created " +
        "randomly for each Material within the range of this minimum and maximum emission value " +
        "to give more natural randomness to the world.")]
    public Color EmissionMin = new Color(1f, 0.8f, 0.016f, 1f);

    [Tooltip(
        "Maximum emission color to apply when emission is 100%. Emission colors are created " +
        "randomly for each Material within the range of this minimum and maximum emission value " +
        "to give more natural randomness to the world.")]
    public Color EmissionMax = new Color(1f, 0.9f, 0.4f, 1f);

    /// <summary>
    /// Have any <see cref="Material"/>s been set?
    /// </summary>
    private bool HasMaterials;

    /// <summary>
    /// Array of emission colors created for given <see cref="Material"/>s.
    /// </summary>
    private Color[] EmissionColors;

    /// <summary>
    /// Array of offsets to apply to emission values to change when they turn on/off by slight
    /// amounts.
    /// </summary>
    private float[] EmissionOffsets;

    /// <summary>
    /// Set colors for given <see cref="Material"/>s.
    /// </summary>
    private void Awake() {
      // Verify all required parameters are defined and correctly setup, skipping any further setup
      // if any parameter is missing or invalid.
      if (!VerifyParameters()) {
        return;
      }

      // Create emission colors for given Materials. Each emission color is randomly chosen between
      // given minimum and maximum emission colors.
      CreateEmissionColors();

      // Apply starting value.
      SetEmission(Value);
    }

    /// <summary>
    /// Set current emission value for all controlled <see cref="Material"/>s.
    /// </summary>
    /// <param name="emissionValue">
    /// Emission value between 0f to 1f, where 0f means no emission and 1f means full emission.
    /// </param>
    internal void SetEmission(float emissionValue) {
      // Store given value.
      Value = emissionValue;

      // Verify have any controlled Materials to set.
      if (!HasMaterials) {
        // Note: 'name' and 'GetType()' just give the name of the GameObject this script is on, and
        // the name of this script respectively.
        Debug.LogWarningFormat(
            "Value of {0} given as {1}.{2}.Set, but {1} is controlling no " +
                "Materials, as none were defined as {1}.{2}.Materials.",
            emissionValue,
            name,
            GetType());

        return;
      }

      // Verify starting value is valid, clamping if not.
      if (emissionValue < 0f || emissionValue > 1f) {
        float originalValue = emissionValue;
        emissionValue = Mathf.Clamp01(emissionValue);

        Debug.LogWarningFormat(
            "Invalid value of {0} given as {1}.{2}.Value, which expected a value " +
                "in the range 0 to 1 (representing 0% to 100% emission)\n  Clamping to {3}.",
            originalValue,
            name,
            GetType(),
            emissionValue);
      }

      // Apply chosen emission color to each Material, multiplied by given emission value.
      for (int i = 0; i < Materials.Length; i++) {
        Materials[i].SetColor(EmissionVariable, EmissionColors[i] * emissionValue);
      }
    }

    /// <summary>
    /// Give this class an array of <see cref="Material"/>s to control this emission values of.
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
      CreateEmissionColors();
    }

    /// <summary>
    /// Create emission colors for given <see cref="Material"/>s. Each emission color is randomly
    /// chosen between given minimum and maximum emission colors.
    /// </summary>
    private void CreateEmissionColors() {
      EmissionColors = new Color[Materials.Length];

      for (int i = 0; i < EmissionColors.Length; i++) {
        EmissionColors[i] = Color.Lerp(EmissionMin, EmissionMax, Random.value);
      }
    }

    /// <summary>
    /// Verify that all required parameters have been correctly defined, returning false if not.
    /// </summary>
    private bool VerifyParameters() {
      // TODO(b/149056787): Standardize parameter verification across scripts.
      // Verify emission variable name is not null. This name is needed to set emission colors on
      // given Materials.
      if (string.IsNullOrEmpty(EmissionVariable)) {
        Debug.LogErrorFormat(
            "No variable name given as {0}.{1}.Emission Variable, which is needed " +
                "in order to set emission color on controlled Materials.",
            name,
            GetType());

        return false;
      }

      // Verify starting value is valid, clamping if not.
      if (Value < 0f || Value > 1f) {
        float originalValue = Value;
        Value = Mathf.Clamp01(Value);

        Debug.LogWarningFormat(
            "Invalid starting value of {0} given as {1}.{2}.Value, which expected " +
                "a value in the range 0 to 1 (representing 0% to 100% emission).\nClamping to {3}.",
            originalValue,
            name,
            GetType(),
            Value);
      }

      // See if any Materials have been given.
      HasMaterials = Materials.Length != 0;

      if (!HasMaterials) {
        return false;
      }

      // Verify defined Materials array. We do this after have checked if no Materials have been
      // given, to allow for an empty Materials array to be defined as parameters and then set using
      // SetMaterials function later. The Materials array is ony checked here if an array of
      // starting Materials was defined as a parameter.
      if (!VerifyMaterials(Materials, "defined for")) {
        return false;
      }

      // If have reached this point then have verified that all required parts are present and
      // properly setup.
      return true;
    }

    /// <summary>
    /// Verify a given array of <see cref="Material"/> is valid (not null, nor empty, nor containing
    /// a null <see cref="Material"/>), returning false if not.
    /// </summary>
    /// <param name="materials"><see cref="Material"/> array to check.</param>
    /// <param name="receivedPhrase">
    /// If array is invalid, use this phrase in error messages to describe how this array was
    /// received (e.g. was it 'given to' (a function), 'defined as' (a parameter) etc).
    /// </param>
    private bool VerifyMaterials(Material[] materials, string receivedPhrase) {
      // Verify array is not null.
      if (materials == null) {
        Debug.LogErrorFormat(
            "Null array of Materials {0} {1}.{2}, which needs a non-null Materials " +
                "array to run!",
            receivedPhrase,
            name,
            GetType());

        return false;
      }

      // Verify that at least one Material has been given.
      if (materials.Length == 0) {
        Debug.LogErrorFormat(
            "Empty Materials array {0} {1}.{2}, which needs Materials a non-empty " +
                "Materials array to run!",
            receivedPhrase,
            name,
            GetType());

        return false;
      }

      // Verify that no null Materials have been given.
      for (int i = 0; i < materials.Length; i++) {
        if (materials[i] == null) {
          Debug.LogErrorFormat(
              "Null Material {0} {1}.{2}, specifically Material {3} of {4} in given " +
                  "Material array.",
              receivedPhrase,
              name,
              GetType(),
              i + 1,
              materials.Length);

          return false;
        }
      }

      // If have reached this point then have verified this is a non-null, non-empty array of
      // Materials whose values are all non-null.
      return true;
    }
  }
}
