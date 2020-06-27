using Google.Maps.Examples.Shared;
using Google.Maps.Feature.Style;
using UnityEngine;

namespace Google.Maps.Examples {
  /// <summary>
  /// Example demonstrating how to add borders around the base of buildings.
  /// </summary>
  /// <remarks>
  /// This example is almost identical to the Nine Slicing example. The only difference is the use
  /// of the <see cref="Extruder"/> class to add modelled borders to the bases of buildings. <para>
  /// Uses <see cref="DynamicMapsService"/> component to allow navigation around the world, with the
  /// <see cref="MapsService"/> component keeping only the viewed part of the world loaded at all
  /// times.
  /// </para><para>
  /// Also uses <see cref="BuildingTexturer"/> component to apply Nine-Sliced <see
  /// cref="Material"/>s.
  /// </para>
  /// Uses <see cref="ErrorHandling"/> component to display any errors encountered by the
  /// <see cref="MapsService"/> component when loading geometry.
  /// </remarks>
  [RequireComponent(typeof(DynamicMapsService), typeof(BuildingTexturer), typeof(ErrorHandling))]
  public sealed class BuildingBorders : MonoBehaviour {
    [Tooltip("Material to apply around bases of buildings and around roads.")]
    public Material BuildingAndRoadBorder;

    [Tooltip("Material to use for roads.")]
    public Material Roads;

    /// <summary>
    /// Create a <see cref="MapsService"/> to load buildings, then add borders around their bases
    /// and around the edges of roads.
    /// </summary>
    private void Start() {
      // Verify a Building Base Material has been given.
      if (BuildingAndRoadBorder == null) {
        Debug.LogError(ExampleErrors.MissingParameter(
            this,
            BuildingAndRoadBorder,
            "Building And Road Border",
            "to apply around the bases of buildings"));

        return;
      }

      // Verify a Roads Material has been given.
      if (Roads == null) {
        Debug.LogError(ExampleErrors.MissingParameter(this, Roads, "Roads", "to apply to roads"));

        return;
      }

      // Get the required Dynamic Maps Service on this GameObject.
      DynamicMapsService dynamicMapsService = GetComponent<DynamicMapsService>();

      // Create a roads style that defines a material for roads and for borders of roads. The
      // specific border material used is chosen to look just a little darker than the material of
      // the ground plane (helping the roads to visually blend into the surrounding ground).
      SegmentStyle roadsStyle =
          new SegmentStyle
              .Builder {
                Material = Roads,
                BorderMaterial = BuildingAndRoadBorder,
                Width = 7.0f,
                BorderWidth = 1.0f
              }
              .Build();

      // Get default style options.
      GameObjectOptions renderingStyles = ExampleDefaults.DefaultGameObjectOptions;

      // Replace default roads style with new, just created roads style.
      renderingStyles.SegmentStyle = roadsStyle;

      // Get required BuildingTexturer component on this GameObject.
      BuildingTexturer buildingTexturer = GetComponent<BuildingTexturer>();

      // Sign up to event called after each new building is loaded, so can assign Materials to this
      // new building, and add an extruded base around the building to fake an Ambient Occlusion
      // contact shadow. Note that:
      // - DynamicMapsService.MapsService is auto-found on first access (so will not be null).
      // - This event must be set now during Awake, so that when Dynamic Maps Service starts loading
      //   the map during Start, this event will be triggered for all Extruded Structures.
      dynamicMapsService.MapsService.Events.ExtrudedStructureEvents.DidCreate.AddListener(args => {
        // Apply nine sliced wall and roof materials to this building.
        buildingTexturer.AssignNineSlicedMaterials(args.GameObject);

        // Add a border around base to building using Building Border Builder class, coloring it
        // using the given border Material.
        Extruder.AddBuildingBorder(args.GameObject, args.MapFeature.Shape, BuildingAndRoadBorder);
      });
    }
  }
}
