using System.Collections.Generic;
using Google.Maps.Event;
using Google.Maps.Feature;
using Google.Maps.Unity.Intersections;
using UnityEngine;

namespace Google.Maps.Examples {
  /// <summary>
  /// Spawns <see cref="Vehicle"/> prefabs on a road lattice.
  /// </summary>
  [RequireComponent(typeof(TrafficSystem))]
  [RequireComponent(typeof(MapsService))]
  public class VehicleSpawner : MonoBehaviour {
    /// <summary>
    /// The possible vehicles to spawn.
    /// </summary>
    public Vehicle[] Vehicles;

    /// <summary>
    /// Whenever a segment is created, the number of vehicles to spawn at random locations is equal
    /// to the segment length divided by <see cref="SegmentLengthToVehicles"/>.
    /// </summary>
    public float SegmentLengthToVehicles;

    /// <summary>
    /// The traffic system.
    /// </summary>
    private TrafficSystem TrafficSystem;

    /// <summary>
    /// The number of vehicles to be spawned on the next road lattice event.
    /// </summary>
    private int VehiclesToSpawn;

    void Awake() {
      TrafficSystem = GetComponent<TrafficSystem>();

      // Subscribe to segment and road lattice events.
      MapsService mapsService = GetComponent<MapsService>();
      mapsService.Events.SegmentEvents.DidCreate.AddListener(OnSegmentCreated);
      mapsService.Events.RoadLatticeEvents.DidModify.AddListener(OnRoadLatticeModified);
    }

    /// <summary>
    /// Updates the number of vehicles to spawn when a traversable segment is created.
    /// </summary>
    private void OnSegmentCreated(DidCreateSegmentArgs args) {
      if (!Vehicle.IsTraversableRoad(args.MapFeature)) {
        // Don't spawn vehicles for non-traversable segments.
        return;
      }

      float segmentLength = 0;

      // Calculate the length of the segment.
      Vector2[] polyline = args.MapFeature.Shape.Lines[0].Vertices;
      for (int i = 1; i < polyline.Length; i++) {
        segmentLength += (polyline[i] - polyline[i - 1]).magnitude;
      }

      // The number of vehicles to spawn at random locations is equal to the segment length divided
      // by `SegmentLengthToVehicles`.
      VehiclesToSpawn += Mathf.RoundToInt(segmentLength / SegmentLengthToVehicles);
    }

    /// <summary>
    /// Spawns as many vehicles as specified by <see cref="VehiclesToSpawn"/>.
    /// </summary>
    private void OnRoadLatticeModified(DidModifyRoadLatticeArgs args) {
      // Get all traversable nodes in the road lattice.
      List<RoadLatticeNode> nodes = new List<RoadLatticeNode>();
      foreach (RoadLatticeNode node in args.RoadLattice.Nodes) {
        foreach (RoadLatticeEdge edge in node.Edges) {
          if (!Vehicle.IsTraversableRoad(edge.Segment)) {
            continue;
          }

          nodes.Add(node);
          break;
        }
      }

      // Don't spawn any vehicles if we have no traversable nodes to spawn them  on.
      if (nodes.Count == 0) {
        return;
      }

      for (; VehiclesToSpawn > 0; VehiclesToSpawn--) {
        // Get a random node to spawn a vehicle on.
        RoadLatticeNode spawnNode = nodes[Random.Range(0, nodes.Count)];

        // Instantiate a random vehicle.
        Vehicle vehiclePrefab = Vehicles[Random.Range(0, Vehicles.Length)];
        Vehicle vehicle = Instantiate(vehiclePrefab);

        // Initialize it at the spawn node.
        vehicle.Initialize(TrafficSystem, spawnNode);
      }
    }
  }
}
