using Google.Maps.Examples.Shared;
using UnityEngine;

namespace Google.Maps.Examples {
  /// <summary>
  /// Example demonstrating the use of Nine-Sliced <see cref="Material"/>s for better building
  /// texturing.
  /// </summary>
  /// <remarks>
  /// Uses <see cref="DynamicMapsService"/> component to allow navigation around the world, with the
  /// <see cref="MapsService"/> component keeping only the viewed part of the world loaded at all
  /// times.
  /// <para>
  /// Also uses <see cref="BuildingTexturer"/> component to apply Nine-Sliced <see
  /// cref="Material"/>s.
  /// </para>
  /// Also uses <see cref="ErrorHandling"/> component to display an errors encountered by the
  /// <see cref="MapsService"/> component when loading geometry.
  /// </remarks>
  [RequireComponent(typeof(DynamicMapsService), typeof(BuildingTexturer), typeof(ErrorHandling))]
  public sealed class NineSlicing : MonoBehaviour {
    /// <summary>
    /// Use events to connect <see cref="DynamicMapsService"/> to <see cref="BuildingTexturer"/> so
    /// that all extruded buildings can receive a Nine-Sliced <see cref="Material"/>.
    /// </summary>
    private void Awake() {
      // Get required Building Texturer component on this GameObject.
      BuildingTexturer buildingTexturer = GetComponent<BuildingTexturer>();

      // Get the required Dynamic Maps Service on this GameObject.
      DynamicMapsService dynamicMapsService = GetComponent<DynamicMapsService>();

      // Sign up to event called after each new building is loaded, so can assign Materials to this
      // new building. Note that:
      // - DynamicMapsService.MapsService is auto-found on first access (so will not be null).
      // - This event must be set now during Awake, so that when Dynamic Maps Service starts loading
      //   the map during Start, this event will be triggered for all Extruded Structures.
      dynamicMapsService.MapsService.Events.ExtrudedStructureEvents.DidCreate.AddListener(
          args => buildingTexturer.AssignNineSlicedMaterials(args.GameObject));
    }
  }
}
