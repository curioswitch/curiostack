using Google.Maps.Examples.Shared;
using UnityEngine;

namespace Google.Maps.Examples {
  /// <summary>
  /// Example demonstrating how to create a day-night cycle using a directional <see cref="Light"/>
  /// and emissive <see cref="Material"/>s.
  /// </summary>
  /// <remarks>
  /// This example is almost identical to the Nine Slicing example. The only difference is the
  /// addition of <see cref="SunAndMoonController"/> and <see cref="EmissionController"/> to control
  /// the lighting and emission-colors of <see cref="Material"/>s in order to simulate a day-night
  /// cycle. The underlying logic of applying <see cref="Material"/>s to buildings is the same as it
  /// is in <see cref="NineSlicing"/> example script.
  /// <para>
  /// Uses <see cref="DynamicMapsService"/> component to allow navigation around the world, with the
  /// <see cref="MapsService"/> component keeping only the viewed part of the world loaded at all
  /// times.
  /// </para>
  /// Also uses <see cref="BuildingTexturer"/> component to apply Nine-Sliced <see
  /// cref="Material"/>s. <para> Also uses <see cref="ErrorHandling"/> component to display any
  /// errors encountered by the <see cref="MapsService"/> component when loading geometry.
  /// </para></remarks>
  [RequireComponent(
      typeof(DynamicMapsService), typeof(BuildingTexturer), typeof(EmissionController))]
  [RequireComponent(typeof(ErrorHandling))]
  public sealed class DayAndNight : MonoBehaviour {
    [Tooltip(
        "Script for controlling day-night cycle. Will wait until all geometry is loaded " +
        "before starting animated day-night cycle, said animation stopping as soon as any input " +
        "is received from the user.")]
    public SunAndMoonController SunAndMoonController;

    /// <summary>
    /// Create a <see cref="MapsService"/> to load geometry.
    /// </summary>
    private void Start() {
      // Verify that a Sun and Moon Controller has been defined.
      if (SunAndMoonController == null) {
        ExampleErrors.MissingParameter(this, SunAndMoonController, "Sun and Moon Controller");

        return;
      }

      // Get required BuildingTexturer component on this GameObject.
      BuildingTexturer buildingTexturer = GetComponent<BuildingTexturer>();

      // Get required Emission Controller component, and give the Building Wall Materials to it so
      // that the building windows can be lit up at night time.
      GetComponent<EmissionController>().SetMaterials(buildingTexturer.WallMaterials);

      // Get required Dynamic Maps Service component on this GameObject.
      DynamicMapsService dynamicMapsService = GetComponent<DynamicMapsService>();

      // Sign up to event called after each new building is loaded, so can assign Materials to this
      // new building. Note that:
      // - DynamicMapsService.MapsService is auto-found on first access (so will not be null).
      // - This event must be set now during Awake, so that when Dynamic Maps Service starts loading
      //   the map during Start, this event will be triggered for all Extruded Structures.
      dynamicMapsService.MapsService.Events.ExtrudedStructureEvents.DidCreate.AddListener(
          args => buildingTexturer.AssignNineSlicedMaterials(args.GameObject));

      // Sign up to event called after all buildings have been loaded, so can start animating
      // day-night cycle.
      dynamicMapsService.MapsService.Events.MapEvents.Loaded.AddListener(
          args => SunAndMoonController.StartAnimating());
    }
  }
}
