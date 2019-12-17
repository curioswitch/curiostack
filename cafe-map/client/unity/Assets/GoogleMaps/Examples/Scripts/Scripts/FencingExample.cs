using Google.Maps;
using Google.Maps.Feature;
using Google.Maps.Fencing;
using UnityEngine;

namespace GoogleMaps.Examples.Scripts {
  /// <summary>
  /// Example script demonstrating how to use <see cref="FencingService"/> to forbid shooting
  /// within 20m of a building used for shopping. The player is allowed to walk inside the
  /// fencing zones, but not shoot while inside them. Projectiles are also not allowed to enter
  /// the fencing zones.
  ///
  /// The example also shows how you can hook the creation of fencing objects
  /// in order to add your own customizations. In this example, the fencing zones are marked in
  /// green.
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
    private const float COOLDOWN_TIME = 0.7f;

    /// <summary>
    /// The <see cref="MapsService"/> with which to register our event handlers.
    /// </summary>
    private MapsService MapsService;

    /// <summary>
    /// the <see cref="FencingService"/> to set up and register our event handlers with.
    /// </summary>
    private FencingService FencingService;

    /// <summary>
    /// The object to use as the player's avatar.
    /// </summary>
    public GameObject Player;

    /// <summary>
    /// The player's gun-barrel.
    /// </summary>
    public GameObject GunBarrel;

    /// <summary>
    /// A prefab for the missiles shot by the player.
    /// </summary>
    public GameObject MissilePrefab;

    /// <summary>
    /// A material to use to mark out the fenced-off areas.
    /// </summary>
    public Material FencedZoneMaterial;

    /// <summary>
    /// A material to apply to chunks of destroyed buildings.
    /// </summary>
    public Material BuildingChunkMaterial;

    /// <summary>
    /// Fencing rule specifying the fencing zones for this example.
    /// </summary>
    private readonly FencingRule FencingRule =
        FencingRule.NewRule()
            .StructureArea(StructureMetadata.UsageType.Shopping, 20f);


    /// <summary>
    /// Cooldown for missile firing. Fireable when less than or equal to zero.
    /// </summary>
    private float Cooldown = 0f;

    /// <summary>
    /// Set up Unity physics.
    /// </summary>
    private void PhysicsSetup() {
      Physics.gravity = new Vector3(0, -200, 0);

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

        collider.sharedMesh = meshFilter.sharedMesh  ;

        // Assign every building a building exploder.
        BuildingExploder exploder = args.GameObject.AddComponent<BuildingExploder>();
        exploder.ChunkMaterial = BuildingChunkMaterial;
      });
    }

    /// <summary>
    /// Component awakening.
    /// </summary>
    private void Awake() {
      PhysicsSetup();
      MapsServiceSetup();
    }

    /// <summary>
    /// Set up green markers for fencing zones.
    /// </summary>
    private void ZoneMarkerSetup() {
      FencingService.FencingServiceEvents.ObjectCreated.AddListener(args => {
        SphereCollider sphereCollider = args.GameObject.GetComponent<SphereCollider>();
        GameObject cylinder = GameObject.CreatePrimitive(PrimitiveType.Cylinder);
        float radius = sphereCollider.radius;
        cylinder.transform.localScale = new Vector3(radius*2, 0.1f, radius*2);
        cylinder.transform.position = sphereCollider.center;
        cylinder.transform.SetParent(args.GameObject.transform);

        Collider cylinderCollider = cylinder.GetComponent<Collider>();
        Destroy(cylinderCollider);

        Renderer renderer = cylinder.GetComponent<Renderer>();
        renderer.material = FencedZoneMaterial;
      });
    }

    /// <summary>
    /// Component startup.
    /// </summary>
    private void Start() {
      // Note: FencingService should be set up in Start, not Awake, as it is not guaranteed to
      // initialized until after its own Awake method is executed.
      FencingService = GetComponent<FencingServiceComponent>().FencingService;
      FencingService.RegisterRule(FencingRule, FENCING_LAYER);

      ZoneMarkerSetup();
    }

    /// <summary>
    /// Fire a missile from the player with the same orientation as the player.
    /// </summary>
    private void Fire() {
      GameObject missile = Instantiate(MissilePrefab);

      Collider bulletCollider = missile.GetComponent<Collider>();
      Collider playerCollider = Player.GetComponent<Collider>();

      Physics.IgnoreCollision(bulletCollider, playerCollider);

      missile.transform.position = Player.transform.position;
      missile.transform.rotation = Player.transform.rotation;

      // Put the missile on a special layer so it can be made to collide with the fencing zones.
      missile.layer = MissilePrefab.layer;
    }

    /// <summary>
    /// Is the player inside a fencing zone?
    /// </summary>
    /// <returns>True if the player is inside a fencing zone, false otherwise.</returns>
    private bool InsideFencedZone() {
      Collider[] colliders =
          Physics.OverlapSphere(Player.transform.position, 5, 1 << FENCING_LAYER);
      return colliders.Length > 0;
    }

    /// <summary>
    /// Can the player fire?
    /// </summary>
    /// <returns>True if the player can fire, false otherwise.</returns>
    private bool CanFire() {
      return (Cooldown <= 0f) && (!InsideFencedZone());
    }

    /// <summary>
    /// Show or hide the player's gun-barrel.
    /// </summary>
    /// <param name="show">
    /// Should be set to true to show the gun-barrel or false to hide it.
    /// </param>
    private void SetShowGunBarrel(bool show) {
      GunBarrel.SetActive(show);
    }

    /// <summary>
    /// Fire missile if the fire button is pressed and the cooldown is inactive.
    /// </summary>
    private void MaybeFire() {
      if (Cooldown > 0f) {
        Cooldown -= Time.deltaTime;
      }

      if ((Input.GetKey("space")) && (CanFire())) {
        Fire();

        Cooldown = COOLDOWN_TIME;
      }
    }

    /// <summary>
    /// Update logic.
    /// </summary>
    private void Update() {
      SetShowGunBarrel(!InsideFencedZone());
      MaybeFire();
    }
  }
}
