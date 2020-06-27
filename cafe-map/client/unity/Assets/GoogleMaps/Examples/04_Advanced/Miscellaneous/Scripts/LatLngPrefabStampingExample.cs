using System.Collections.Generic;
using Google.Maps.Coord;
using Google.Maps.Unity;
using UnityEngine;

namespace Google.Maps.Examples {
  /// <summary>
  /// Example script for stamping down prefabs at specified lat/long coordinates. Removes other
  /// structures within a specified radius of the stamps to avoid overlap. Note that if you move
  /// the floating origin then you will need to offset the stamp prefabs, as this will not be
  /// done automatically. Stamps are spawned within a circle around a reference game object (e.g.
  /// the camera).
  /// </summary>
  public class LatLngPrefabStampingExample : MonoBehaviour {
    /// <summary>
    /// The coordinates of each stamp.
    /// </summary>
    public List<LatLng> LatLngs;

    /// <summary>
    /// The prefabs to use for each stamp.
    /// </summary>
    public List<GameObject> Prefabs;

    /// <summary>
    /// The radius within which to suppress other structures around each stamp.
    /// </summary>
    public List<float> SuppressionRadiuses;

    /// <summary>
    /// Object around which to spawn stamps.
    /// </summary>
    public GameObject SpawnReference;

    /// <summary>
    /// The spawning radius, measured from the spawning reference. Stamps within this radius are
    /// spawned, stamps outside are despawned.
    /// </summary>
    public float SpawnRadius = 2000.0f;

    /// <summary>
    /// A mapping from stamp indices to prefab instances.
    /// </summary>
    private Dictionary<int, GameObject> InstanceMap = new Dictionary<int, GameObject>();

    /// <summary>
    /// Reference to <see cref="MapsService"/>.
    /// </summary>
    private MapsService MapsService;

    /// <summary>
    /// Structures that have yet to be checked against the stamps for the need for suppression.
    /// </summary>
    private HashSet<GameObject> UncheckedStructures = new HashSet<GameObject>();

    /// <summary>
    /// Gets a reference to the attached <see cref="MapsService"/>.
    /// </summary>
    private void Awake() {
      MapsService = GetComponent<MapsService>();
    }

    /// <summary>
    /// Handles the spawning of a structure. Hides objects and adds them to a set to later be
    /// checked for the need for suppression.
    /// </summary>
    /// <param name="gameObject">The game object representing the structure.</param>
    private void HandleStructureSpawn(GameObject gameObject) {
      gameObject.AddComponent<BoxCollider>();

      // Hide immediately, so that objects that will ultimately be suppressed don't flicker onto
      // the screen before being suppressed.
      SetGameObjectVisible(gameObject, false);

      UncheckedStructures.Add(gameObject);
    }

    /// <summary>
    /// Sets up event handlers to handle structure spawning.
    /// </summary>
    private void Start() {
      MapsService.Events.ExtrudedStructureEvents.DidCreate.AddListener(
          args => { HandleStructureSpawn(args.GameObject); });

      MapsService.Events.ModeledStructureEvents.DidCreate.AddListener(
          args => { HandleStructureSpawn(args.GameObject); });
    }

    /// <summary>
    /// Shows or hides a game object.
    /// </summary>
    /// <param name="gameObject">The game object to show or hide.</param>
    /// <param name="show">
    /// True if the object should be shown, false if it should be hidden.
    /// </param>
    private void SetGameObjectVisible(GameObject gameObject, bool show) {
      Renderer renderer = gameObject.GetComponent<Renderer>();
      renderer.enabled = show;
    }

    /// <summary>
    /// Determines whether or not a game object represents a structure.
    /// </summary>
    /// <param name="gameObject">The game object to test.</param>
    /// <returns>True if the game object is a structure, false otherwise.</returns>
    private bool IsStructure(GameObject gameObject) {
      ExtrudedStructureComponent extrudedComponent =
          gameObject.GetComponent<ExtrudedStructureComponent>();

      ModeledStructureComponent modeledComponent =
          gameObject.GetComponent<ModeledStructureComponent>();

      return ((extrudedComponent != null) || (modeledComponent != null));
    }

    /// <summary>
    /// Suppresses other structures within the specified radius of the given stamp. Structures will
    /// only be suppressed if they have an attached collider.
    /// </summary>
    /// <param name="index">The stamp around which to suppress other structures.</param>
    private void SuppressStructuresNearStamp(int index) {
      Vector3 center = GetStampPosition(index);
      float radius = SuppressionRadiuses[index];

      Collider[] otherColliders = Physics.OverlapSphere(center, radius);

      foreach (Collider otherCollider in otherColliders) {
        GameObject other = otherCollider.gameObject;

        if (IsStructure(other)) {
          SetGameObjectVisible(other, false);
        }
      }
    }

    /// <summary>
    /// Gets the position of a stamp in Unity World Space.
    /// </summary>
    /// <param name="index">The index of the stamp whose position should be returned.</param>
    /// <returns>The position of the specified stamp in Unity World Space.</returns>
    private Vector3 GetStampPosition(int index) {
      LatLng latLng = LatLngs[index];

      return MapsService.Coords.FromLatLngToVector3(latLng);
    }

    /// <summary>
    /// Spawns or despawns the stamp with the specified index depending on their distance to
    /// the spawn reference object.
    /// </summary>
    /// <param name="index">The index of the stamp to spawn or despawn.</param>
    private void SpawnOrDespawnStamp(int index) {
      GameObject prefab = Prefabs[index];
      Vector3 unityCoords = GetStampPosition(index);

      float distance = Vector3.Distance(SpawnReference.transform.position, unityCoords);
      bool shouldExist = distance < SpawnRadius;
      bool exists = InstanceMap.ContainsKey(index);

      if ((shouldExist) && (!exists)) {
        GameObject spawned = GameObject.Instantiate(prefab);
        spawned.AddComponent<BoxCollider>();
        spawned.transform.position = unityCoords;
        InstanceMap[index] = spawned;
      }

      if ((!shouldExist) && (exists)) {
        Destroy(InstanceMap[index]);
        InstanceMap.Remove(index);
      }
    }

    /// <summary>
    /// Suppresses any structure that is within the suppression radius of a stamp.
    /// </summary>
    private void SuppressUncheckedStructures() {
      if (UncheckedStructures.Count == 0) {
        return;
      }

      // Unsuppresses all structures to be checked. Those that need to be suppressed will be
      // resuppressed in the same frame.
      foreach (GameObject building in UncheckedStructures) {
        SetGameObjectVisible(building, true);
      }

      for (int i = 0; i < LatLngs.Count; i++) {
        SuppressStructuresNearStamp(i);
      }

      UncheckedStructures.Clear();
    }

    /// <summary>
    /// Performs per-frame update tasks.
    /// </summary>
    private void Update() {
      for (int i = 0; i < LatLngs.Count; i++) {
        SpawnOrDespawnStamp(i);
      }

      SuppressUncheckedStructures();
    }
  }
}
