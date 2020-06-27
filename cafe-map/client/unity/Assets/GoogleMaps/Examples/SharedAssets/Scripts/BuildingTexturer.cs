using UnityEngine;
using Random = UnityEngine.Random;

namespace Google.Maps.Examples.Shared {
  /// <summary>
  /// Script that takes multiple building materials and randomly assigns them to any given building.
  /// </summary>
  public sealed class BuildingTexturer : MonoBehaviour {
    [Tooltip(
        "Materials to apply to buildings walls. The specific Materials chosen must use a " +
        "Shader that is set up to work with Nine Slicing. For more info on how this is achieved, " +
        "examine the Shader: " +
        "Assets/GoogleMaps/Examples/Materials/NineSlicing/BuildingWalls.shader.")]
    public Material[] WallMaterials;

    [Tooltip(
        "Materials to apply to roofs (must be the same number of Materials as Building Wall " +
        "Materials array, as we will try to match given Walls to given Roofs (e.g. if a building " +
        "is given Building Wall Material 2, then it will also be given Building Roof Material 2).")]
    public Material[] RoofMaterials;

    /// <summary>
    /// Verify given <see cref="Material"/> arrays are valid (not empty nor containing any null
    /// entries, and both arrays of the same length).
    /// </summary>
    private void Awake() {
      // Verify that at least one Wall Material and at least one Roof Material has been given.
      if (WallMaterials.Length == 0) {
        Debug.LogError(ExampleErrors.EmptyArray(this, WallMaterials, "Wall Materials"));

        return;
      }

      if (RoofMaterials.Length == 0) {
        Debug.LogError(ExampleErrors.EmptyArray(this, RoofMaterials, "Roof Materials"));

        return;
      }

      // Verify that the same number of Wall and Roof Materials have been given.
      if (WallMaterials.Length != RoofMaterials.Length) {
        Debug.LogErrorFormat(
            "Incorrect number of Building Roof Materials defined for {0}.{1}: {2} " +
                "Building Wall Materials were given, but {3} Building Roof Materials were given." +
                "\n{1} needs the same number of Building Roof Materials as Building Wall " +
                "Materials, i.e. {2} of each.",
            name,
            GetType(),
            WallMaterials.Length,
            RoofMaterials.Length);

        return;
      }

      // Verify that no null Materials have been given.
      for (int i = 0; i < WallMaterials.Length; i++) {
        if (WallMaterials[i] == null) {
          Debug.LogError(ExampleErrors.NullArrayElement(this, WallMaterials, "Wall Materials", i));

          return;
        }

        if (RoofMaterials[i] == null) {
          Debug.LogError(ExampleErrors.NullArrayElement(this, RoofMaterials, "Roof Materials", i));

          return;
        }
      }

      // If have reached this point then have verified that all required parts are present and
      // properly set up.
    }

    /// <summary>
    /// Assign a randomly chosen Wall and Roof <see cref="Material"/> to a given building.
    /// </summary>
    /// <param name="building">Building to assign <see cref="Material"/>s to.</param>
    /// <param name="index">
    /// Optional index of Wall and Roof <see cref="Material"/> pair to apply. If this value is not
    /// set a random <see cref="Material"/> pair will be used.
    /// </param>
    internal void AssignNineSlicedMaterials(GameObject building, int? index = null) {
      // If a specific Material index was given, verify it is a valid index for a Wall and Roof
      // Material pair.
      if (index.HasValue) {
        if (index.Value < 0 || index.Value >= WallMaterials.Length) {
          Debug.LogError(
              ExampleErrors.InvalidArrayIndex(this, WallMaterials, "Wall Materials", index.Value));

          return;
        }
      } else {
        // Pick a random Material index to use for both Wall and Roof Materials. Not that the same
        // index will work for both arrays of Materials, as we have already verified that the Wall
        // and Roof Material arrays are the same length.
        index = Random.Range(0, WallMaterials.Length);
      }

      // Replace building MeshRenderer's sharedMaterials array with chosen Materials. Note that this
      // must be done by creating a new array of Materials, rather than altering the entries of this
      // MeshRenderer's sharedMaterials array, as altering the existing array will not actually
      // change the MeshRenderer's Materials.
      MeshRenderer buildingMeshRenderer = building.GetComponent<MeshRenderer>();

      buildingMeshRenderer.sharedMaterials =
          new Material[] { WallMaterials[index.Value], RoofMaterials[index.Value] };
    }
  }
}
