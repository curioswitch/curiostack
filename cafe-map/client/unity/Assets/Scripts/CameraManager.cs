using System;
using UnityEngine;
using UnityEngine.InputSystem;

[RequireComponent(typeof(Camera))]
public class CameraManager : MonoBehaviour
{
    [Tooltip("Movement speed when pressing movement keys (WASD for panning, QE for up/down).")]
    public float MovementSpeed = 20f;

    public InputAction translateAction;

    public InputAction zoomAction;
    
     /// <summary>
    /// The current desired rotation of the Camera around the Y-Axis. Applied in world space after
    /// Inclination is applied.
    /// </summary>
    private float Azimuth;

    /// <summary>
    /// The current desired rotation of the Camera around the X-Axis. Applied in world space before
    /// Azimuth is applied.
    /// </summary>
    private float Inclination;

    void Awake() {
        transform.Translate(0, 300, 0);
        transform.LookAt(Vector3.zero);
        InitializeAzimuthAndInclination();
    }

    private void Update()
    {
        Move();
    }

    private void Move()
    {
        var movement = translateAction.ReadValue<Vector2>();
        var zoom = zoomAction.ReadValue<float>();

        if (movement.Equals(Vector2.zero) && zoom == 0)
        {
            return;
        }

        // Move the camera at a speed that is linearly dependent on the height of the camera above
        // the ground plane to make camera manual camera movement practicable. The movement speed
        // is clamped between 1% and 100% of the configured MovementSpeed.
        float speed = Mathf.Clamp(transform.position.y, MovementSpeed * 0.01f, MovementSpeed);
        Vector3 up = Quaternion.Euler(0, Azimuth, 0) * Vector3.up;
        Vector3 right = Quaternion.Euler(0, Azimuth, 0) * Vector3.right;
        Vector3 forward = Quaternion.Euler(0, Azimuth, 0) * Vector3.forward;

        Vector3 motion = (right * movement.x + up * movement.y + forward * zoom) * speed;
        transform.Translate(motion);
    }
    
    private void InitializeAzimuthAndInclination() {
        // Initialize Azimuth and Inclination from the current rotation Euler angles. Reading Euler
        // angles is generally not a good idea but should be safe to do once at initialization if the
        // Camera starts in a non-extreme orientation.
        Azimuth = transform.eulerAngles.y;
        Inclination = transform.eulerAngles.x;
    }

    private void OnEnable()
    {
        translateAction.Enable();
        zoomAction.Enable();
    }

    private void OnDisable()
    {
        translateAction.Disable();
        zoomAction.Disable();
    }
}
