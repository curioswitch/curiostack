using System.Collections.Generic;
using Google.Maps;
using Google.Maps.Feature;
using Google.Maps.Feature.Shape;
using Google.Maps.Unity;
using UnityEngine;

/// <summary>
/// Example demonstrating how to automatically remove all buildings that are over roads, rails,
/// ferry lanes, etc.
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
public sealed class BuildingsOverRoads : MonoBehaviour {
  [Tooltip("Index of physics layer to put all segments into (including roads).")]
  public int SegmentPhysicsLayer = 8;

  [Tooltip("Remove buildings that are over roads? (This must be set before Awake is called.)")]
  public bool RemoveOverRoads = true;

  [Tooltip("Remove buildings that are over railways? (This must be set before Awake is called.)")]
  public bool RemoveOverRailways;

  [Tooltip("Remove buildings that are over ferry lanes? (This must be set before Awake is "
      + "called.)")]
  public bool RemoveOverFerryLanes;

  [Tooltip("Debug successful removal of buildings over roads?")]
  public bool DebugRemovals = true;

  /// <summary>
  /// A <see cref="LayerMask"/> for ray-casting into only the Segment Physics Layer.
  /// </summary>
  private LayerMask SegmentPhysicsLayerMask;

  /// <summary>
  /// All buildings loaded that have not yet been ray-cast to check if they are over a segment.
  /// </summary>
  /// <remarks>
  /// Building are stored in this way so that ray-casting can be performed when all loading is done.
  /// </remarks>
  private readonly LinkedList<Building> BuildingsToRaycast = new LinkedList<Building>();

  /// <summary>
  /// All the data needed to ray-cast the vertices of a building to see if any of them are over a
  /// segment.
  /// </summary>
  private struct Building {
    /// <summary><see cref="GameObject"/> of this loaded building.</summary>
    public GameObject GameObject;

    /// <summary>Points to use to test if this building is over a segment.</summary>
    public Vector3[] TestPoints;
  }

  /// <summary>
  /// Use events to connect <see cref="DynamicMapsService"/> to <see cref="BuildingTexturer"/> so
  /// that all extruded buildings can receive a Nine-Sliced <see cref="Material"/>.
  /// </summary>
  private void Awake() {
    // Verify that the given Segment Physics layer is valid (positive and within the range of all
    // available physics layers).
    if (SegmentPhysicsLayer < 0 || SegmentPhysicsLayer > 31) {
      Debug.LogError(ExampleErrors.OutsideRange(this, SegmentPhysicsLayer, "Segment Physics Layer",
          0, 31));
      return;
    }

    // Convert Segment Physics Layer index into a Layer Mask for ray-casting into this layer only
    // (i.e. for seeing if any segments are hit by a ray-cast).
    SegmentPhysicsLayerMask = 1 << SegmentPhysicsLayer;

    // Get required Building Texturer component on this GameObject.
    BuildingTexturer buildingTexturer = GetComponent<BuildingTexturer>();

    // Get the required Dynamic Maps Service on this GameObject.
    DynamicMapsService dynamicMapsService = GetComponent<DynamicMapsService>();

    // See if any segment types have been given as types to remove buildings over. We check this so
    // that, if no segments types have been given, unnecessary setup can be avoided.
    bool removeAny = RemoveOverRoads || RemoveOverRailways || RemoveOverFerryLanes;

    // If any segment types have been given as types to remove buildings over, then sign up to event
    // called after each new segment is loaded, so can assign a Collider to it and place it in the
    // Segment Physics Layer. Note that:
    // - DynamicMapsService.MapsService is auto-found on first access (so will not be null).
    // - This event must be set now during Awake, so that when Dynamic Maps Service starts loading
    //   the map during Start, this event will be triggered for all Extruded Structures.
    if (removeAny) {
      dynamicMapsService.MapsService.Events.SegmentEvents.DidCreate.AddListener(args => {
        args.GameObject.AddComponent<MeshCollider>();
        args.GameObject.layer = SegmentPhysicsLayer;
      });
    }

    // Sign up to event called after each new building is loaded, so can store it for checking
    // against segments and apply a Nine sliced texture.
    dynamicMapsService.MapsService.Events.ExtrudedStructureEvents.DidCreate.AddListener(args => {
      // Store building so it can be check to be over a segment (after all segments are loaded). We
      // skip this if no segment types have been given as types to remove buildings over.
      if (removeAny) {
        StoreBuilding(args.GameObject, args.MapFeature);
      }

      // Assign Nine sliced texture to this building.
      buildingTexturer.AssignNineSlicedMaterials(args.GameObject);
    });

    // When all geometry is loaded, check if any buildings are over any segments, and remove them if
    // so. We skip this if no segment types have been given as types to remove buildings over.
    if (removeAny) {
      dynamicMapsService.MapsService.Events.MapEvents.Loaded.AddListener(args =>
        TryRemoveBuildings());
    }
  }

  /// <summary>
  /// Store a created building so it can be checked to be over a segment (after all segments are
  /// loaded).
  /// </summary>
  /// <param name="buildingGameObject"><see cref="GameObject"/> of building.</param>
  /// <param name="buildingFeature"><see cref="MapFeature"/> defining building's shape.</param>
  private void StoreBuilding(GameObject buildingGameObject, ExtrudedStructure buildingFeature) {
    // Generate a set of points to test whether they are over a segment. We start with the center
    // point of the building.
    Vector3 buildingCenter = buildingGameObject.transform.position;
    List<Vector3> buildingVertices = new List<Vector3> { buildingCenter };

    // Get all the parts of this building, accessing the vertices making up the base of each part.
    // We use these vertices to generate more points to test whether the building is over a segment.
    // This is to avoid false negatives, where the building is over a segment, but its very center
    // is over a gap in the segment, resulting in the building not being considered to be over a
    // segment.
    foreach (ExtrudedArea.Extrusion extrusion in buildingFeature.Shape.Extrusions) {
      // Vertices in the building FootPrint are relative to FootPrint.Origin. We add this origin
      // to make these vertices in the local coordinates of this building's GameObject, then add
      // the buildings world space position to make these local coordinates into world space
      // coordinates, ultimately resulting in an array of world space points for all the vertices
      // defining the base of this building.
      Vector2 origin2D = extrusion.FootPrint.Origin;
      foreach (Vector2 vertex in extrusion.FootPrint.Vertices) {
        Vector2 localPosition = vertex + origin2D;

        // Convert from 2D x, z coordinate to 3D x, z coordinates, and add the building's position
        // to get the world space position of this vertex.
        Vector3 worldPosition = new Vector3(
          localPosition.x + buildingGameObject.transform.position.x,
          0f,
          localPosition.y + buildingGameObject.transform.position.z);

        // Store the average of the actual world space position of this vertex, and the center of
        // the building. This is to avoid declaring a building as over a segment if one of its
        // corners is slightly over a segment, instead requiring a point towards the center of the
        // building be over the segment for the building to be considered as over a segment, and
        // removed.
        buildingVertices.Add(new Vector3(
          (worldPosition.x + buildingGameObject.transform.position.x) / 2f,
          0f,
          (worldPosition.z + buildingGameObject.transform.position.z) / 2f));
      }
    }

    // Store the GameObject and vertices of this building, so that after all geometry is loaded
    // we can check if any of this GameObject's vertices are over a segment.
    BuildingsToRaycast.AddLast(new Building {
        GameObject = buildingGameObject, TestPoints = buildingVertices.ToArray()
    });
  }

  /// <summary>
  /// See if any of the buildings that have just been loaded are over a segment of the desired
  /// type/s, removing any that are.
  /// </summary>
  private void TryRemoveBuildings() {
    int buildingsRemoved = 0;
    foreach (Building building in BuildingsToRaycast) {
      // Ray-cast down from a point 1m above each of the building's test points (with a maximum
      // range of 10 meters, which should be more than enough to hit any ground-level segments),
      // seeing if there are any hits in the Segment Physics Layer (i.e. if a segment is below this
      // point of this building).
      bool removeBuilding = false;
      foreach (Vector3 vertex in building.TestPoints) {
        foreach (RaycastHit hit in Physics.RaycastAll(vertex + Vector3.up, Vector3.down, 10f,
            SegmentPhysicsLayerMask)) {
          // Get the type of this segment. This is to check if it is a road, or another kind of
          // segment like a railway or ferry lane.
          SegmentComponent segmentComponent
              = hit.collider.gameObject.GetComponent<SegmentComponent>();
          // If this segment does not have a Segment Component, print an error to this effect
          if (segmentComponent == null) {
            Debug.LogErrorFormat("Segment found without a {0}, specifically segment on "
                + "GameObject named {1}.",
                typeof(SegmentComponent), building.GameObject.name);
            continue;
          }

          // Check the type of this Segment against the types we want to remove buildings over.
          switch (segmentComponent.MapFeature.Metadata.Usage) {
            // Check for Railways.
            case SegmentMetadata.UsageType.Rail:
              removeBuilding = RemoveOverRailways;
              break;

            // Check for Ferry Lanes.
            case SegmentMetadata.UsageType.Ferry:
              removeBuilding = RemoveOverFerryLanes;
              break;

            // We consider anything that is not a Railway or a Ferry Lane to be a type of Road.
            default:
              removeBuilding = RemoveOverRoads;
              break;
          };

          // Remove building if appropriate.
          if (removeBuilding) {
            Destroy(building.GameObject);
            buildingsRemoved++;
            break;
          }
        }

        // Stop checking the vertices of this building if it has already been removed.
        if (removeBuilding) {
          break;
        }
      }
    }

    // Optionally debug how many buildings were removed (if there were any).
    if (buildingsRemoved > 0 && DebugRemovals) {
      Debug.LogFormat("Successfully removed {0} buildings that were over segments.",
          buildingsRemoved);
    }

    // Clear all buildings from list of buildings to ray-cast, as have now ray-cast them all and
    // removed all that were over segments of the desired type(s).
    BuildingsToRaycast.Clear();
  }
}
