using UnityEngine;
using Google.Maps.Event;
using Google.Maps.Examples.Shared;

namespace Google.Maps.Examples {
  /// <summary>
  /// Building names example, demonstrating how to get the name of a building created by the Maps
  /// SDK.
  /// </summary>
  [RequireComponent(typeof(MapLabeller))]
  public class BuildingLabelsCreator : MonoBehaviour {
    /// <summary>
    /// The Labeller used to create building labels.
    /// </summary>
    private MapLabeller Labeller;

    void Awake() {
      Labeller = GetComponent<MapLabeller>();
    }

    /// <summary>
    /// Add listeners for new building creations.
    /// </summary>
    void OnEnable() {
      Labeller.BaseMapLoader.MapsService.Events.ExtrudedStructureEvents.DidCreate.AddListener(
          OnExtrudedStructureCreated);
      Labeller.BaseMapLoader.MapsService.Events.ModeledStructureEvents.DidCreate.AddListener(
          OnModeledStructureCreated);
    }

    /// <summary>
    /// Remove listeners for new building creations.
    /// </summary>
    void OnDisable() {
      Labeller.BaseMapLoader.MapsService.Events.ExtrudedStructureEvents.DidCreate.RemoveListener(
          OnExtrudedStructureCreated);
      Labeller.BaseMapLoader.MapsService.Events.ModeledStructureEvents.DidCreate.RemoveListener(
          OnModeledStructureCreated);
    }

    void OnExtrudedStructureCreated(DidCreateExtrudedStructureArgs args) {
      CreateLabel(args.GameObject, args.MapFeature.Metadata.PlaceId, args.MapFeature.Metadata.Name);
    }

    void OnModeledStructureCreated(DidCreateModeledStructureArgs args) {
      CreateLabel(args.GameObject, args.MapFeature.Metadata.PlaceId, args.MapFeature.Metadata.Name);
    }

    /// <summary>
    /// Creates a label for a building.
    /// </summary>
    /// <param name="buildingGameObject">The GameObject of the building.</param>
    /// <param name="placeId">The place ID of the building.</param>
    /// <param name="displayName">The name to display on the label for the building.</param>
    void CreateLabel(GameObject buildingGameObject, string placeId, string displayName) {
      if (!Labeller.enabled)
        return;

      // Ignore uninteresting names.
      if (displayName.Equals("ExtrudedStructure") || displayName.Equals("ModeledStructure")) {
        return;
      }

      Label label = Labeller.NameObject(buildingGameObject, placeId, displayName);
      if (label != null) {
        MapsGamingExamplesUtils.PlaceUIMarker(buildingGameObject, label.transform);
      }
    }
  }
}
