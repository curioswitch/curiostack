using Google.Maps.Examples.Shared;
using UnityEngine;

namespace Google.Maps.Examples {
  /// <summary>
  /// Example demonstrating how to use <see cref="Camera"/> render settings to render a minimap
  /// from the world's geometry (without reloading or duplicating this geometry).
  /// </summary>
  /// <remarks>
  /// Uses <see cref="DynamicMapsService"/> to allow navigation around the world, with the
  /// <see cref="MapsService"/> keeping only the viewed part of the world loaded at all times.
  /// <para>
  /// Also uses <see cref="BuildingTexturer"/> to apply Nine-Sliced <see cref="Material"/>s.
  /// </para>
  /// Also uses <see cref="ErrorHandling"/> component to display any errors encountered by the
  /// <see cref="MapsService"/> component when loading geometry.
  /// </remarks>
  [RequireComponent(typeof(DynamicMapsService), typeof(BuildingTexturer), typeof(ErrorHandling))]
  public sealed class MinimapExample : MonoBehaviour {
    [Tooltip("Index of physics layer to put all minimap showable elements into.")]
    public int MinimapLayer = 9;

    [Tooltip("The Minimap Camera used to render what the Minimap sees.")]
    public Camera MinimapCamera;

    [Tooltip("Optional ground quad to include in the Minimaps.")]
    public MeshRenderer Ground;

    [Tooltip(
        "Should ground quad be included in the Minimap? These values must be set before play " +
        "starts to have an effect.")]
    public bool ShowGround;

    [Tooltip(
        "Should regions be included in the Minimap? These values must be set before play starts " +
        "to have an effect.")]
    public bool ShowRegions = true;

    [Tooltip(
        "Should segments (roads) be included in the Minimap? These values must be set before " +
        "play starts to have an effect.")]
    public bool ShowSegments = true;

    [Tooltip(
        "Should water be included in the Minimap? These values must be set before play starts " +
        "to have an effect.")]
    public bool ShowWater = true;

    [Tooltip(
        "Should extruded structures be included in the Minimap? These values must be set before " +
        "play starts to have an effect.")]
    public bool ShowExtruded;

    [Tooltip(
        "Should modeled structures be included in the Minimap? These values must be set before " +
        "play starts to have an effect.")]
    public bool ShowModeled;

    /// <summary>
    /// Use events to make sure all desired geometry is in the correct layer to be seen by, and
    /// shown in, the on-screen Minimap.
    /// </summary>
    private void Awake() {
      // Verify that the given Minimap layer is valid (positive and within the range of all
      // available physics layers).
      if (MinimapLayer < 0 || MinimapLayer > 31) {
        // Note: 'name' and 'GetType()' just give the name of the GameObject this script is on, and
        // the name of this script respectively.
        Debug.LogErrorFormat(
            "Invalid Segment Physics Layer defined for {0}.{1}, which needs a valid " +
                "Physics Layer index (i.e. within the range 0 to 31).",
            name,
            GetType());

        return;
      }

      // Make sure the Minimap Camera is properly setup to render Minimap-layer geometry.
      if (MinimapCamera != null) {
        // Convert Minimap Layer index into a Layer Mask, and apply to the Minimap Camera.
        LayerMask minimapLayerMask = 1 << MinimapLayer;
        MinimapCamera.cullingMask = minimapLayerMask;
      }

      // If we've been given a Ground Quad to include in the Minimap, set it's physics layer to the
      // Minimap Layer now (so it can be seen by the Minimap Camera).
      if (Ground == null) {
        if (ShowGround) {
          Debug.LogErrorFormat(
              "{0}.{1} was unable to show ground in Minimap as no Ground " +
                  "MeshRenderer was defined.",
              name,
              GetType());
        }
      } else {
        // Set ground's layer to be either the Minimap layer (if it is to be shown in the Minimap),
        // or the default layer (if it is not to be shown in the Minimap).
        Ground.gameObject.layer = ShowGround ? MinimapLayer : 1 << 0;
      }

      // Get required Building Texturer component on this GameObject.
      BuildingTexturer buildingTexturer = GetComponent<BuildingTexturer>();

      // Get the required Dynamic Maps Service on this GameObject.
      DynamicMapsService dynamicMapsService = GetComponent<DynamicMapsService>();

      // Make sure any desired geometry is put into the Minimap Layer as it is created. Note that:
      // - DynamicMapsService.MapsService is auto-found on first access (so will not be null).
      // - This event must be set now during Awake, so that when Dynamic Maps Service starts loading
      //   the map during Start, these event will be triggered for all relevant geometry.
      if (ShowRegions) {
        dynamicMapsService.MapsService.Events.RegionEvents.DidCreate.AddListener(
            args => args.GameObject.layer = MinimapLayer);
      }

      if (ShowSegments) {
        dynamicMapsService.MapsService.Events.SegmentEvents.DidCreate.AddListener(
            args => args.GameObject.layer = MinimapLayer);
      }

      if (ShowWater) {
        dynamicMapsService.MapsService.Events.AreaWaterEvents.DidCreate.AddListener(
            args => args.GameObject.layer = MinimapLayer);

        dynamicMapsService.MapsService.Events.LineWaterEvents.DidCreate.AddListener(
            args => args.GameObject.layer = MinimapLayer);
      }

      if (ShowExtruded) {
        dynamicMapsService.MapsService.Events.ExtrudedStructureEvents.DidCreate.AddListener(
            args => args.GameObject.layer = MinimapLayer);
      }

      if (ShowModeled) {
        dynamicMapsService.MapsService.Events.ModeledStructureEvents.DidCreate.AddListener(
            args => args.GameObject.layer = MinimapLayer);
      }

      // Additionally, when extruded structures (buildings) are made, make sure they receive a
      // Nine-Sliced texture. We can connect to this event as many times as we want, with every
      // connected function/code block being called for each created extruded structure.
      dynamicMapsService.MapsService.Events.ExtrudedStructureEvents.DidCreate.AddListener(
          args => buildingTexturer.AssignNineSlicedMaterials(args.GameObject));
    }
  }
}
