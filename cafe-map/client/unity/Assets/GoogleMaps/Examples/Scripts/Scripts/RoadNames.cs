using Google.Maps;
using UnityEngine;
using UnityEngine.UI;

/// <summary>
/// Road names example, demonstrating how to get the name of a road created by the Maps Unity SDK.
/// </summary>
/// <remarks>
/// The specific LabelPrefab used in this example contains a <see cref="Text"/> element with a
/// custom shader assigned. This shader makes sure this <see cref="Text"/> element is displayed on
/// top of all in-scene geometry (even if it is behind or inside said geometry). Examine the shader
/// on this prefab to find out how this is achieved.
/// <para>
/// Uses <see cref="DynamicMapsService"/> component to allow navigation around the world, with the
/// <see cref="MapsService"/> component keeping only the viewed part of the world loaded at all
/// times.
/// </para>
/// Also uses <see cref="RoadLabeller"/> component to apply label names to roads.
/// <para>
/// Also uses <see cref="ErrorHandling"/> component to display any errors encountered by the
/// <see cref="MapsService"/> component when loading geometry.
/// </para></remarks>
[RequireComponent(typeof(DynamicMapsService), typeof(RoadLabeller), typeof(ErrorHandling))]
public sealed class RoadNames : MonoBehaviour {
  /// <summary>
  /// Use <see cref="MapsService"/> to load geometry, labelling all created roads with their names.
  /// </summary>
  private void Awake() {
    // Get the required Dynamic Maps Service on this GameObject.
    DynamicMapsService dynamicMapsService = GetComponent<DynamicMapsService>();

    // Get the required RoadLabeller component on this GameObject.
    RoadLabeller roadLabeller = GetComponent<RoadLabeller>();

    // Sign up to event called just after any new road (segment) is loaded, so can add to stored
    // roads and can create/re-center road's name Label using required RoadLabeller component.
    // Note that:
    // - DynamicMapsService.MapsService is auto-found on first access (so will not be null).
    // - This event must be set now during Awake, so that when Dynamic Maps Service starts loading
    //   the map during Start, this event will be triggered for all Extruded Structures.
    dynamicMapsService.MapsService.Events.SegmentEvents.DidCreate.AddListener(args
        => roadLabeller.NameRoad(args.GameObject, args.MapFeature));

    // Sign up to event called after all roads names are shown, so Labels showing names can all be
    // smoothly faded in.
    dynamicMapsService.MapsService.Events.MapEvents.Loaded.AddListener(args
        => roadLabeller.ShowRoadNames());
  }
}
