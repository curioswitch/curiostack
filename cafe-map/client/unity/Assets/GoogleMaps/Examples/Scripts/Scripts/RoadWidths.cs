using System.Collections.Generic;
using Google.Maps;
using Google.Maps.Feature;
using Google.Maps.Feature.Style;
using UnityEngine;

/// <summary>
/// Road widths example, demonstrating how to set widths by the type of road for all roads created
/// by the Maps Unity SDK.
/// </summary>
/// <remarks>
/// Uses <see cref="DynamicMapsService"/> to allow navigation around the world, with the
/// <see cref="MapsService"/> keeping only the viewed part of the world loaded at all times.
/// <para>
/// Also uses <see cref="ErrorHandling"/> component to display any errors encountered by the
/// <see cref="MapsService"/> component when loading geometry.
/// </para></remarks>
[RequireComponent(typeof(DynamicMapsService), typeof(ErrorHandling))]
public sealed class RoadWidths : MonoBehaviour {
  [Tooltip("Width of an unspecified/default type of road in meters.")]
  public float DefaultWidth = 7f;

  [Tooltip("Width of a local (small) road in meters.")]
  public float LocalRoadWidth = 3.5f;

  [Tooltip("Width of an arterial (major) road in meters.")]
  public float MajorRoadWidth = 8.5f;

  [Tooltip("Width of a highway in meters.")]
  public float HighwayWidth = 10f;

  [Tooltip("Width of a controlled access highway in meters.")]
  public float ControlledAccessHighwayWidth = 10f;

  [Tooltip("Width of a footpath in meters.")]
  public float FootpathWidth = 1f;

  [Tooltip("Width of a railway in meters.")]
  public float RailWidth = 2.5f;

  [Tooltip("Width of a ferry lane in meters.")]
  public float FerryLaneWidth = 1f;

  /// <summary>
  /// Use <see cref="MapsService"/> to load geometry, setting the widths of all roads by their type.
  /// </summary>
  private void Awake() {
    // Get the required Dynamic Maps Service on this GameObject.
    DynamicMapsService dynamicMapsService = GetComponent<DynamicMapsService>();
    SegmentStyle defaultSegmentStyle = new SegmentStyle.Builder{Width = DefaultWidth}.Build();

    // Create a Dictionary of separate styles, one for each type of road. The only difference
    // between each of these styles is that each of them has a different width applied to it,
    // resulting in a different road width per style.
    var roadWidths = new Dictionary<SegmentMetadata.UsageType, SegmentStyle> {
      {SegmentMetadata.UsageType.Unspecified, defaultSegmentStyle},
      {SegmentMetadata.UsageType.Road, defaultSegmentStyle},
      {SegmentMetadata.UsageType.LocalRoad,
          new SegmentStyle.Builder {Width = LocalRoadWidth}.Build()},
      {SegmentMetadata.UsageType.ArterialRoad,
          new SegmentStyle.Builder {Width = MajorRoadWidth}.Build()},
      {SegmentMetadata.UsageType.Highway,
          new SegmentStyle.Builder {Width = HighwayWidth}.Build()},
      {SegmentMetadata.UsageType.ControlledAccessHighway,
          new SegmentStyle.Builder {Width = ControlledAccessHighwayWidth}.Build()},
      {SegmentMetadata.UsageType.Footpath,
          new SegmentStyle.Builder {Width = FootpathWidth}.Build()},
      {SegmentMetadata.UsageType.Rail,
          new SegmentStyle.Builder {Width = RailWidth}.Build()},
      {SegmentMetadata.UsageType.Ferry,
          new SegmentStyle.Builder {Width = FerryLaneWidth}.Build()},
    };

    // Sign up to event called just before any new road (segment) is loaded, so can assign it a
    // width based on its type. Note that:
    // - DynamicMapsService.MapsService is auto-found on first access (so will not be null).
    // - This event must be set now during Awake, so that when Dynamic Maps Service starts loading
    //   the map during Start, this event will be triggered for all Roads.
    dynamicMapsService.MapsService.Events.SegmentEvents.WillCreate.AddListener(args => {
      // Make sure we have defined a width for this specific type of road. This should be the case,
      // as the above Dictionary includes all currently available road types.
      SegmentMetadata.UsageType roadType = args.MapFeature.Metadata.Usage;
      if (roadWidths.ContainsKey(roadType)) {
        // Tell the SDK to build this specific road using the style containing the width for this
        // type of road.
        args.Style = roadWidths[roadType];
      } else {
        // If a style has not been defined for this specific type of road, warn the developer.
        Debug.LogWarningFormat(
            "No road width defined for road of type {0}, using the default road width.\n" +
            roadType);
        args.Style = defaultSegmentStyle;
      }
    });
  }
}

