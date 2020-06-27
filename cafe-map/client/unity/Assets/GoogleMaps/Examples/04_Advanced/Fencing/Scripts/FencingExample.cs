using System;
using System.Collections.Generic;
using Google.Maps.Feature;
using UnityEngine;

namespace Google.Maps.Examples {
  /// <summary>
  /// Example script demonstrating how to implement client-side geo-fencing based upon building
  /// usage types.
  /// </summary>
  public class FencingExample : MonoBehaviour {
    /// <summary>
    /// Layer on which the player resides.
    /// </summary>
    private const int PLAYER_LAYER = 0;

    /// <summary>
    /// Layer on which the fencing objects are spawned.
    /// </summary>
    private const int FENCING_LAYER = 9;

    /// <summary>
    /// Cooldown time after firing.
    /// </summary>
    private const float COOLDOWN_TIME = 0.05f;

    /// <summary>
    /// The <see cref="MapsService"/> with which to register our event handlers.
    /// </summary>
    private MapsService MapsService;

    /// <summary>
    /// The object to use as the player's avatar.
    /// </summary>
    public GameObject Player;

    /// <summary>
    /// The object marking the location to spawn missiles from.
    /// </summary>
    public GameObject Muzzle;

    /// <summary>
    /// A prefab for the missiles shot by the player.
    /// </summary>
    public GameObject MissilePrefab;

    /// <summary>
    /// A material to use to mark out the fenced-off areas.
    /// </summary>
    public Material FencedZoneMaterial;

    /// <summary>
    /// Margin to add around fenced zone.
    /// </summary>
    public float FenceMargin = 20f;

    /// <summary>
    /// A material to apply to chunks of destroyed buildings.
    /// </summary>
    public Material BuildingChunkMaterial;

    /// <summary>
    /// Cooldown for missile firing. Fireable when less than or equal to zero.
    /// </summary>
    private float Cooldown;

    /// <summary>
    /// Set up Unity physics.
    /// </summary>
    private void PhysicsSetup() {
      Physics.gravity = new Vector3(0, -200, 0);
      Physics.autoSyncTransforms = true;

      // Allow player to enter fenced zones.
      Physics.IgnoreLayerCollision(FENCING_LAYER, PLAYER_LAYER);
    }

    /// <summary>
    /// Setup <see cref="MapsService"/>. Give buildings mesh colliders and make them explodeable.
    /// </summary>
    private void MapsServiceSetup() {
      MapsService = GetComponent<MapsService>();

      MapsService.Events.ExtrudedStructureEvents.DidCreate.AddListener(args => {
        // Assign every building a mesh collider, so player and bullets can collide with them.
        MeshFilter meshFilter = args.GameObject.GetComponent<MeshFilter>();
        MeshCollider collider = args.GameObject.AddComponent<MeshCollider>();

        collider.sharedMesh = meshFilter.sharedMesh;

        // Assign every building a building exploder.
        BuildingExploder exploder = args.GameObject.AddComponent<BuildingExploder>();
        exploder.ChunkMaterial = BuildingChunkMaterial;

        // Allow the fencing status of the building to be checked before damaging it.
        args.GameObject.AddComponent<FenceChecker>();
      });
    }

    /// <summary>
    /// Returns true if a building has a sensitive usage type (e.g. it is a school), false
    /// otherwise.
    /// </summary>
    private bool IsSensitiveBuilding(ExtrudedStructure feature) {
      if (feature.Metadata.Usage == StructureMetadata.UsageType.School) {
        return true;
      }
      if (feature.Metadata.Usage == StructureMetadata.UsageType.Shopping) {
        return true;
      }

      return false;
    }

    /// <summary>
    /// Create a fence covering the specified bounds.
    /// </summary>
    /// <param name="bounds">Bounds around which to create the fence.</param>
    private GameObject CreateFence(Bounds bounds) {
      GameObject fencedZone = GameObject.CreatePrimitive(PrimitiveType.Cube);
      fencedZone.transform.position = bounds.center;
      fencedZone.transform.localScale =
          new Vector3(bounds.size.x, bounds.size.y, bounds.size.z)
          + Vector3.one*FenceMargin*2f;
      fencedZone.GetComponent<Renderer>().sharedMaterial = FencedZoneMaterial;
      fencedZone.AddComponent<Fence>();
      fencedZone.layer = FENCING_LAYER;
      fencedZone.name = "Fence";

      return fencedZone;
    }

    /// <summary>
    /// Set up fence generation.
    /// </summary>
    private void FencingSetup() {
      MapsService.Events.ExtrudedStructureEvents.DidCreate.AddListener(args => {
        if (IsSensitiveBuilding(args.MapFeature)) {
          Bounds bounds = args.GameObject.GetComponent<Collider>().bounds;
          GameObject fencedZone = CreateFence(bounds);
          fencedZone.transform.SetParent(args.GameObject.transform, true);
        }
      });
    }

    /// <summary>
    /// Component awakening.
    /// </summary>
    private void Awake() {
      PhysicsSetup();
      MapsServiceSetup();
      FencingSetup();
    }

    /// <summary>
    /// Fire a missile from the player with the same orientation as the player.
    /// </summary>
    private void Fire(Vector3 targetPos) {
      GameObject missile = Instantiate(MissilePrefab);

      missile.transform.position = Muzzle.transform.position;
      missile.transform.LookAt(targetPos);
      ActionTimer despawnTimer = missile.AddComponent<ActionTimer>();
      despawnTimer.Action = delegate {
        Destroy(missile);
      };
      despawnTimer.Expiry = 32;

      // Put the missile on a special layer so it can be made to collide with the fencing zones.
      missile.layer = MissilePrefab.layer;

      Collider missileCollider = missile.GetComponent<Collider>();
      Collider playerCollider = Player.GetComponent<Collider>();
      Physics.IgnoreCollision(missileCollider, playerCollider);
    }

    /// <summary>
    /// Can the player fire?
    /// </summary>
    private bool CanFire() {
      return Cooldown <= 0f && (!Player.GetComponent<FenceChecker>().Fenced());
    }

    /// <summary>
    /// Start player moving toward the given position.
    /// </summary>
    /// <param name="position">Position to start player moving toward.</param>
    private void StartMovingToward(Vector3 position) {
      Player.GetComponent<TargetMovementController>().StartMovingToward(position);
    }

    /// <summary>
    /// Enumerates clicks/touches from different input sources.
    /// </summary>
    private IEnumerable<Vector2> GetScreenClicks() {
      if (Input.GetMouseButtonDown(0)) {
        yield return Input.mousePosition;
      }
    }

    /// <summary>
    /// Update logic.
    /// </summary>
    private void Update() {
      if (Cooldown > 0f) {
        Cooldown -= Time.deltaTime;
      }

      foreach (Vector2 screenPosition in GetScreenClicks()) {
        RaycastHit hit;
        Ray ray = Camera.main.ScreenPointToRay(screenPosition);
        if (Physics.Raycast(ray, out hit, Mathf.Infinity, ~(1 << FENCING_LAYER))) {
          if (hit.transform.gameObject.GetComponent<BuildingExploder>()) {
            if (CanFire()) {
              Fire(hit.point);
              Cooldown = COOLDOWN_TIME;
            }
          } else {
            StartMovingToward(hit.point);
          }
        }
      }
    }
  }
}
