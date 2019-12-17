using System.Collections.Generic;
using Google.Maps.Feature;
using UnityEngine;
using UnityEngine.UI;

/// <summary>
/// Component for adding <see cref="Label"/>s to show the names of all buildings created by the Maps
/// Unity SDK.
/// </summary>
/// <remarks>
/// The specific LabelPrefab used in this example contains a <see cref="Text"/> element with a
/// custom shader assigned. This shader makes sure this <see cref="Text"/> element is displayed on
/// top of all in-scene geometry (even if it is behind or inside said geometry). Examine the shader
/// on this prefab to find out how this is achieved.
/// </remarks>
public sealed class BuildingLabeller : MonoBehaviour {
  [Tooltip("Canvas on which to create name-labels.")]
  public Canvas Canvas;

  [Tooltip("Prefab to show a road's name. Must contain a UI.Text element as a child.")]
  public Label LabelPrefab;

  [Tooltip("Start all Labels faded out?")]
  public bool StartFaded;

  [Tooltip("Should the Label which is the most closely aligned to the Camera be the most visible? "
    + "This helps reduce visual clutter by allowing all Labels not been directly looked at to be "
    + "faded out.")]
  public bool FadeWithView;

  /// <summary>All created building <see cref="Label"/>s, stored by PlaceId.</summary>
  /// <remarks>
  /// We use this list to ensure that only one <see cref="Label"/> is shown per building, i.e. if
  /// a new part of a building is added, the building will end up with only one name
  /// <see cref="Label"/>, not 2.
  /// </remarks>
  private readonly Dictionary<string, Label> BuildingLabels = new Dictionary<string, Label>();

  /// <summary>Make sure all required parameters are given.</summary>
  private void Awake() {
    // Make sure a canvas has been specified.
    if (Canvas == null) {
      Debug.LogError(ExampleErrors.MissingParameter(this, Canvas, "Canvas",
          "to show building names"));
      return;
    }

    // Make sure a prefab has been specified.
    if (LabelPrefab == null) {
      Debug.LogError(ExampleErrors.MissingParameter(this, LabelPrefab, "Label",
          "to use to display names above buildings in scene"));
    }
  }

  /// <summary>Show a name for a newly created extruded building.</summary>
  /// <param name="buildingGameObject"><see cref="GameObject"/> containing created building.</param>
  /// <param name="buildingData">
  /// <see cref="ExtrudedStructure"/> containing building's data, passed as eventArgs.MapFeature in
  /// <see cref="Google.Maps.Event.ExtrudedStructureEvents.DidCreate"/> event.
  /// </param>
  internal void NameExtrudedBuilding(GameObject buildingGameObject,
      ExtrudedStructure buildingData) {
    NameBuilding(buildingGameObject, buildingData.Metadata.PlaceId, buildingData.Shape.BoundingBox,
        buildingData.Metadata.Name);
  }

  /// <summary>Show a name for a newly created modeled building.</summary>
  /// <param name="buildingGameObject"><see cref="GameObject"/> containing created building.</param>
  /// <param name="buildingData">
  /// <see cref="ModeledStructure"/> containing building's data, passed as eventArgs.MapFeature in
  /// <see cref="Google.Maps.Event.ModeledStructureEvents.DidCreate"/> event.
  /// </param>
  internal void NameModeledBuilding(GameObject buildingGameObject, ModeledStructure buildingData) {
    NameBuilding(buildingGameObject, buildingData.Metadata.PlaceId, buildingData.Shape.BoundingBox,
        buildingData.Metadata.Name);
  }

  /// <summary>Show a name for a newly created extruded building.</summary>
  /// <param name="buildingGameObject"><see cref="GameObject"/> containing created building.</param>
  /// <param name="buildingPlaceId">PlaceId used to uniquely identify this building.</param>
  /// <param name="buildingBounds">
  /// Bounds of this building, used to find building roof to place Label upon if ray-casting fails.
  /// </param>
  /// <param name="buildingName">Name to display on this building (skipped if null/empty).</param>
  private void NameBuilding(GameObject buildingGameObject, string buildingPlaceId,
      Bounds buildingBounds, string buildingName) {
    // Skip showing name if it is null.
    if (string.IsNullOrEmpty(buildingName)
        || buildingName.Equals("ExtrudedStructure")
        || buildingName.Equals("ModeledStructure")) {
      return;
    }

    // See if a Label has already been created for this building, re-using it if so. This is to
    // ensure that when new parts are added to a building, the building will still only have a
    // single name Label (rather than getting a new Label for each new part).
    Label buildingLabel;
    if (BuildingLabels.ContainsKey(buildingPlaceId)) {
      buildingLabel = BuildingLabels[buildingPlaceId];
    } else {
      // Create a Label to show this brand new building's name.
      buildingLabel = Instantiate(LabelPrefab, Canvas.transform);
      buildingLabel.StartFadedOut = StartFaded;
      buildingLabel.FadeWithView = FadeWithView;
      buildingLabel.SetText(buildingName);

      // Add this new label to the Dictionary of all stored building Labels.
      BuildingLabels.Add(buildingPlaceId, buildingLabel);
    }

    // Ray-cast down to the building's center to get the height of the building at it's center. This
    // is so we can place the Label on the roof of the building. Note that this is done to ensure
    // the Label is touching the roof at its center (instead of using bounds, where a building
    // arial in its corner would give an invalid building height).
    Vector3 labelOrigin;
    Vector3 buildingCenter = buildingGameObject.transform.position;
    Ray centerRay = new Ray(buildingCenter + Vector3.up * 1000f, Vector3.down);
    RaycastHit[] centerHits = Physics.RaycastAll(centerRay, 1001f);
    // Note that ray-casting is done from 1km above the building, with a max distance of 1.001km. We
    // do this to ensure the ray-cast starts above the building (in order to hit its roof).

    // If there are no hits, then default to the center of the building's upper bounds.
    if (centerHits.Length == 0) {
      labelOrigin = buildingCenter + Vector3.up * buildingBounds.size.y;
    } else {
      // Check hits to find the highest hit of this building.
      int? highestHit = null;
      float highestHitDistance = 0f;
      for(int i = 0; i < centerHits.Length; i++) {
        if (centerHits[i].collider.gameObject == buildingGameObject) {
          if (!highestHit.HasValue || centerHits[i].distance < highestHitDistance) {
            highestHit = i;
            highestHitDistance = centerHits[i].distance;
          }
        }
      }

      // If at least one hit was for this building, use this hit as the roof-center point where the
      // label is placed. If no hits were for this building, then default to the center of the
      // building's upper bounds.
      labelOrigin = highestHit.HasValue
          ? centerHits[highestHit.Value].point
          : buildingCenter + Vector3.up * buildingBounds.max.y;
    }

    // Move label to the building roof's center.
    buildingLabel.transform.position = labelOrigin;
  }

  /// <summary>Fade in all building <see cref="Label"/>s.</summary>
  internal void ShowBuildingNames() {
    Label.StartFadingAllIn();
  }

  /// <summary>Fade out all building <see cref="Label"/>s.</summary>
  internal void HideBuildingNames() {
    Label.StartFadingAllOut();
  }
}
