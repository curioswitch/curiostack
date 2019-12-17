using UnityEngine;

namespace GoogleMaps.Examples.Scripts {
  /// <summary>
  /// Behaviour for a missile that can blow up buildings with a <see cref="BuildingExploder"/>
  /// attached. The prefab linked by <see cref="ExplosionPrefab"/> must have an attached
  /// <see cref="CharacterController"/>.
  /// </summary>
  public class MissileBehaviour : MonoBehaviour {
    /// <summary>
    /// The prefab to use for the explosion when the missile hits.
    /// </summary>
    public GameObject ExplosionPrefab;

    /// <summary>
    /// The speed at which the missile should fly.
    /// </summary>
    public float Speed = 1.5f;

    /// <summary>
    /// Move bullet forward.
    /// </summary>
    private void FixedUpdate() {
      CharacterController controller = GetComponent<CharacterController>();
      controller.Move(transform.forward * Speed);
    }

    /// <summary>
    /// Explode when the missile collides with something.
    /// </summary>
    /// <param name="hit">Information about the collision.</param>
    private void OnControllerColliderHit(ControllerColliderHit hit) {
      GameObject explosion = Instantiate(ExplosionPrefab);
      explosion.transform.position = gameObject.transform.position;

      BuildingExploder exploder = hit.gameObject.GetComponent<BuildingExploder>();
      if (exploder != null) {
        exploder.Explode(gameObject.transform.position);
      }

      Destroy(gameObject);
    }
  }
}
