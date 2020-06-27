using System.Collections.Generic;
using System.Linq;
using UnityEngine;
using Google.Maps.Event;
using Google.Maps.Unity.Intersections;

namespace Google.Maps.Examples {
  /// <summary>
  /// Note: Road Lattice support is a beta feature subject to performance considerations and future
  /// changes.
  /// This example of pathfinding with the Maps SDK illustrates usages of the road lattice and the
  /// built-in A* algorithm to move elements from A to B on a loaded map.
  /// It puts into action a character controlled by the user and also basic AI agents.
  /// The behavior of the opponents is to catch up with the character regardless of its position on
  /// the loaded graph of road segments.
  /// <see cref="RoadLatticeCharacterController"/> and <see cref="RoadLatticeAIController"/>
  /// </summary>
  [RequireComponent(typeof(BaseMapLoader))]
  public class PathFindingExample : MonoBehaviour {
    [Tooltip("The Base Map Loader handles the Maps Service initialization and basic event flow.")]
    public BaseMapLoader BaseMapLoader;

    /// <summary>
    /// Prefab used to instantiate our main character
    /// </summary>
    public GameObject AvatarPrefab;

    /// <summary>
    /// Prefab used to instantiate basic AI agents
    /// </summary>
    public GameObject NPCPrefab;

    /// <summary>
    /// Container to keep track of all characters
    /// </summary>
    public Transform CharactersContainer;

    /// <summary>
    /// Script used to follow the main character
    /// </summary>
    public SmoothFollowCamera SmoothFollowCamera;

    /// <summary>
    /// Indicates the number of AI agents used in the example
    /// </summary>
    public int NumberOfOpponents = 1;

    /// <summary>
    /// Indicates if AI agents are actively searching for the main character
    /// </summary>
    public bool IsAISearchActive = true;

    /// <summary>
    /// The maximum distance from the avatar that AI agents will start from.
    /// </summary>
    public float MaxAIStartDistanceFromAvatar = 100;

    /// <summary>
    /// Indicates if the path between the AI Agent and their target should be revealed
    /// </summary>
    public bool IsDebugPathOn = false; // buggy - needs work

    /// <summary>
    /// References to AI Agents instances
    /// </summary>
    private List<RoadLatticeAIController> NPCs = new List<RoadLatticeAIController>();

    /// <summary>
    /// Makes sure that the MapsService flag EnableExhaustiveIntersectionReconstruction is set.
    /// <see cref="MapsService"/> for more information about the role of this parameter.
    /// </summary>
    void Awake() {
      // NOTE: EnableExhaustiveIntersectionReconstruction enables a slow but more accurate method of
      // determining intersections in the road lattice by recalculating intersections that have been
      // optimized out of the served maps data.
      // Enable intersection reconstruction.
      if (BaseMapLoader != null && BaseMapLoader.MapsService != null)
        BaseMapLoader.MapsService.EnableExhaustiveIntersectionReconstruction = true;
    }

    /// <summary>
    /// Once the map is loaded, setup main character and AI agents.
    /// Start activating the search.
    /// </summary>
    private void OnMapLoaded(MapLoadedArgs args) {
      if (CharactersContainer != null && BaseMapLoader != null) {
        GameObject avatarGO = GetRandomCharacter(AvatarPrefab);
        RoadLatticeCharacterController cc = avatarGO.GetComponent<RoadLatticeCharacterController>();

        if (cc != null) {
          cc.BaseMapLoader = BaseMapLoader;
          cc.ShowPath = true;
        }

        // Start following our Avatar on the map
        if (SmoothFollowCamera != null) {
          SmoothFollowCamera.target = avatarGO.transform;
          SmoothFollowCamera.transform.LookAt(SmoothFollowCamera.target);
        }

        // Add random npc characters on map (red)
        for (int i = 0; i < NumberOfOpponents; i++) {
          GameObject npcGO = GetRandomCharacter(NPCPrefab);
          RoadLatticeAIController aic = npcGO.GetComponent<RoadLatticeAIController>();

          if (aic != null) {
            aic.BaseMapLoader = BaseMapLoader;
            aic.Target = avatarGO;
            aic.enabled = true;
            NPCs.Add(aic);

            if (cc != null) {
              // Move distant NPCs closer to the avatar so that they don't take too long to reach
              // their target.
              Vector3 diff = aic.transform.position - cc.transform.position;
              Vector3 desiredPosition = Vector3.MoveTowards(cc.transform.position,
                  aic.transform.position, Mathf.Min(diff.magnitude, MaxAIStartDistanceFromAvatar));

              RoadLatticeNode node =
                  BaseMapLoader.MapsService.RoadLattice.SnapToNode(desiredPosition);
              aic.transform.position = new Vector3(node.Location.x, 0, node.Location.y);
            }
          }
        }
      }

      ActiveAllNPCs(IsAISearchActive);
      ShowAIPaths(IsDebugPathOn);
    }

    /// <summary>
    /// Registers interest for the map loaded event.
    /// </summary>
    private void OnEnable() {
      // Listen to the map loaded event
      BaseMapLoader.MapsService.Events.MapEvents.Loaded.AddListener(OnMapLoaded);
    }

    /// <summary>
    /// UnRegisters interest for the map loaded event.
    /// </summary>
    private void OnDisable() {
      // Listen to the map loaded event
      BaseMapLoader.MapsService.Events.MapEvents.Loaded.RemoveListener(OnMapLoaded);
    }

    /// <summary>
    /// Notify all AI Agents that the search is on
    /// </summary>
    /// <param name="isOn"></param>
    public void ActiveAllNPCs(bool isOn) {
      IsAISearchActive = isOn;

      foreach (RoadLatticeAIController aic in NPCs)
        aic.enabled = IsAISearchActive;
    }

    /// <summary>
    /// Notify all AI agents that the active search path should be displayed.
    /// </summary>
    /// <param name="isOn"></param>
    public void ShowAIPaths(bool isOn) {
      IsDebugPathOn = isOn;

      foreach (RoadLatticeAIController aic in NPCs)
        aic.ShowPath = IsDebugPathOn;
    }

    /// <summary>
    /// Creates characters and places them randomly on the map.
    /// </summary>
    private GameObject GetRandomCharacter(GameObject prefab) {
      GameObject character = Instantiate(prefab, CharactersContainer, false);

      List<RoadLatticeNode> nodes = BaseMapLoader.MapsService.RoadLattice.GetNodes();

      if (nodes.Count == 0) {
        Debug.LogError("Could not find any nodes to spawn a character at.");
      } else {
        RoadLatticeNode firstRandomNode = nodes.ElementAt(Random.Range(0, nodes.Count));
        Debug.LogFormat("Character spawned at {0}", firstRandomNode.Location);

        Vector3 newPosition =
            new Vector3(firstRandomNode.Location.x, 0f, firstRandomNode.Location.y);
        character.transform.position = newPosition;
      }

      return character;
    }
  }
}
