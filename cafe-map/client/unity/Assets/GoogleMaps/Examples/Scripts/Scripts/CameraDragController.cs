using UnityEngine;
using UnityEngine.EventSystems;

/// <summary>
/// A mouse/touch based <see cref="Camera"/> controller, allowing dragging the world with the
/// mouse or by touching the screen.
/// </summary>
/// <remarks>
/// Intended to be attached to the <see cref="Camera"/> <see cref="GameObject"/> being controlled.
/// </remarks>
[RequireComponent(typeof(Camera))]
public class CameraDragController : MonoBehaviour {
  [Tooltip("Speed to drag in response to user input."), Range(0.1f, 2f)]
  public float DragSpeed = 0.5f;

  /// <summary>Screen point where the user started dragging from.</summary>
  /// <remarks>Null if the user is not currently dragging.</remarks>
  private Vector2? DragStart;

  /// <summary>Respond to user input to allow dragging the world around.</summary>
  private void Update() {
    // Skip dragging if the user is over a UI element.
    if (EventSystem.current.IsPointerOverGameObject()) {
      return;
    }

    // See if the user is clicking the mouse or pressing the screen this frame.
    bool isPressing = Input.GetMouseButton(0) || Input.GetMouseButton(1);

    // If the user has already started dragging, figure out how far they have dragged and use this
    // as the speed/direction of movement.
    if (DragStart.HasValue) {
      if (isPressing) {
        // See how far the user has dragged since they first pressed.
        Vector2 draggedPosition = Input.mousePosition;
        Vector2 dragDelta = draggedPosition - DragStart.Value;

        // Convert from x-y movement to x-z movement (parallel to the ground), and multiply by
        // dragging speed.
        Vector3 dragDeltaXz
            = new Vector3(dragDelta.x, 0f, dragDelta.y) * DragSpeed * Time.smoothDeltaTime;

        // Rotate by the Camera's current y-angle (so dragging up will move the Camera forwards),
        // and apply as movement.
        transform.position += Quaternion.Euler(0f, transform.eulerAngles.y, 0f) * dragDeltaXz;
      } else {
        // When the user stops clicking the mouse or pressing the screen, stop dragging the world
        // around.
        DragStart = null;
      }
    } else if (isPressing) {
      // Record the screen point where the user started dragging from.
      DragStart = Input.mousePosition;
    }
  }
}
