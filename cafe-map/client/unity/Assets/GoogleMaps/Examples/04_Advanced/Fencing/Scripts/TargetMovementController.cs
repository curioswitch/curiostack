using System;
using System.Security;
using Google.Maps.Feature.Style;
using UnityEngine;

namespace Google.Maps.Examples {
  public class TargetMovementController : MonoBehaviour {
    /// <summary>
    /// Position to move toward.
    /// </summary>
    private Vector3 TargetPosition;

    /// <summary>
    /// Speed at which to move toward target position.
    /// </summary>
    private float Speed = 1.0f;

    /// <summary>
    /// Start object moving toward target position.
    /// </summary>
    /// <param name="position">Position to move object toward.</param>
    public void StartMovingToward(Vector3 position) {
      TargetPosition = position;
      TargetPosition.y = transform.position.y;
    }

    /// <summary>
    /// Handle Unity *FixedUpdate* event.
    /// </summary>
    public void FixedUpdate() {
      Vector3 distanceVector = TargetPosition - transform.position;
      float speed = Mathf.Min(distanceVector.magnitude, Speed);
      Vector3 dirVector = distanceVector.normalized;

      GetComponent<CharacterController>().Move(dirVector*speed);
    }
  }
}
