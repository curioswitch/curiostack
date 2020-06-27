using Google.Maps.Examples.Shared;
using UnityEngine;

namespace Google.Maps.Examples {
  /// <summary>
  /// Example demonstrating how to add parapets to the top edge of buildings.
  /// </summary>
  /// <remarks>
  /// This example is almost identical to the Nine Slicing example. The only difference is the
  /// use of <see cref="Extruder"/> to add modelled parapets to buildings.
  /// <para>
  /// By default loads Melbourne, Australia. If a new latitude/longitude is set in Inspector
  /// (before pressing start), will load new location instead.
  /// </para>
  /// Uses <see cref="DynamicMapsService"/> component to allow navigation around the world, with the
  /// <see cref="MapsService"/> component keeping only the viewed part of the world loaded at all
  /// times.
  /// <para>
  /// Also uses <see cref="BuildingTexturer"/> component to apply Nine-Sliced <see
  /// cref="Material"/>s.
  /// </para>
  /// Also uses <see cref="ErrorHandling"/> component to display any errors encountered by the
  /// <see cref="MapsService"/> component when loading geometry.
  /// </remarks>
  [RequireComponent(typeof(DynamicMapsService), typeof(BuildingTexturer), typeof(ErrorHandling))]
  public sealed class Parapets : MonoBehaviour {
    /// <summary>
    /// Create a <see cref="MapsService"/> to load buildings, then add parapets to them.
    /// </summary>
    private void Start() {
      // Get the required Dynamic Maps Service on this GameObject.
      DynamicMapsService dynamicMapsService = GetComponent<DynamicMapsService>();

      // Get required BuildingTexturer component on this GameObject.
      BuildingTexturer buildingTexturer = GetComponent<BuildingTexturer>();

      // Sign up to event called after each new building is loaded, so can assign Materials to this
      // new building, and add an lofted parapet around the top of each building. Note that:
      // - DynamicMapsService.MapsService is auto-found on first access (so will not be null).
      // - This event must be set now during Awake, so that when Dynamic Maps Service starts loading
      //   the map during Start, this event will be triggered for all Extruded Structures.
      dynamicMapsService.MapsService.Events.ExtrudedStructureEvents.DidCreate.AddListener(args => {
        // Apply nine sliced wall and roof materials to this building.
        buildingTexturer.AssignNineSlicedMaterials(args.GameObject);

        // Add a parapet to this building, making sure it shares the building's roof Material. This
        // should have just been added as the building's second SharedMaterial.
        Material roofMaterial = args.GameObject.GetComponent<MeshRenderer>().sharedMaterials[1];
        Extruder.AddRandomBuildingParapet(args.GameObject, args.MapFeature.Shape, roofMaterial);
      });
    }
  }
}
