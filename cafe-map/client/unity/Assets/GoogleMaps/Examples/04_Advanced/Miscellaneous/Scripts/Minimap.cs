using UnityEngine;
using UnityEngine.UI;

namespace Google.Maps.Examples {
  /// <summary>
  /// Simple script for automatically connecting <see cref="Camera.targetTexture"/> to a given
  /// <see cref="RawImage"/> component, allowing the view from the <see cref="Camera"/> to be
  /// displayed as an on screen UI element.
  ///
  /// When deployed on a mobile device, the minimap position is updated based on the camera rig,
  /// and not the camera itself. The practical reason is that the navigation controller used
  /// on mobile examples updates the camera rig.
  /// </summary>
  [RequireComponent(typeof(Camera))]
  public class Minimap : MonoBehaviour {
    [Tooltip("UI Image used to display Minimap.")]
    public RawImage Image;

    /// <summary>
    /// The <see cref="Transform"/> used by Minimap to offset its position and rotation from. In
    /// most cases, this will be the transform of the <see cref="Camera"/> viewing the world.
    ///
    /// When UpdateTransform is called, the position of Minimap is set to the position of Center
    /// offset by the given PositionOffset. The rotation of the Minimap is set to the rotation of
    /// Center around the y-axis offset by the given RotationOffset.
    /// </summary>
    [Tooltip("Transform used to center and rotate Minimap.")]
    public Transform Center;

    [Tooltip("Offset added to Center's position to give Minimap's center.")]
    public Vector3 PositionOffset;

    [Tooltip("Offset added to Center's rotation around the y-axis to give Minimap's rotation.")]
    public Vector3 RotationOffset;

    /// <summary>On start, make sure Minimap is properly setup.</summary>
    private void Awake() {

      #if (UNITY_IOS || UNITY_ANDROID) && !UNITY_EDITOR
        // When deployed on device, we update the minimap based on the transform of the camera rig
        // instead of the camera.
        Center = Center.parent;
      #endif

      // Make sure this Minimap Camera is not tagged as the scene's Main Camera. Calling the static
      // variable Camera.main actually just searches the current scene for all Camera's tagged
      // "MainCamera", returning the first result. Thus if we have multiple Camera's tagged as
      // "MainCamera", using Camera.main can give unexpected results. This is why we make sure this
      // secondary, Minimap Camera is not tagged as the scene's Main Camera.
      Camera minimapCamera = GetComponent<Camera>();
      minimapCamera.tag = "Untagged";

      // Make sure a Render Texture is connected to the Minimap Camera (as its "Target Texture"). We
      // use this Render Texture to save the Camera's rendered view each frame, storing it in a
      // Render Texture that can be displayed in a UI element on screen.
      if (minimapCamera.targetTexture == null) {
        Debug.LogErrorFormat(
            "No {0} defined for {1}.{2}.Target Texture. {1}.{3} needs a {0} in " +
                "order to be able to display its texture in a UI element.",
            typeof(RenderTexture),
            name,
            typeof(Camera),
            GetType());

        return;
      }

      // Make sure an Image component has been given to display the Minimap in the on-screen UI.
      if (Image == null) {
        Debug.LogErrorFormat(
            "No {0} defined for {1}.{2}.Image. {2} needs a {0} in order to " +
                "be able to display its texture in a UI element.",
            typeof(RawImage),
            name,
            GetType());

        return;
      }

      // Make sure our Image's texture is set to the Minimap's Render Texture, so that the Minimap
      // Camera's output will be displayed in this Image.
      if (Image.texture == null || Image.texture != minimapCamera.targetTexture) {
        Debug.LogWarningFormat(
            "{0}.{1}.Target Texture was not connected to {2}.{3}.Texture. {0}.{4} " +
                "needs these two to be connected to display the minimap in {2}'s {3}.\n" +
                "Making connection now, but to avoid this warning, drag the {5} set as {0}.{1}." +
                "Target Texture into {2}.{3}.Texture.",
            name,
            typeof(Camera),
            Image,
            Image.GetType(),
            GetType(),
            typeof(RenderTexture));
        Image.texture = minimapCamera.targetTexture;
      }

      // Set initial position and rotation
      UpdateTransform();
    }

    private void Update() {
      UpdateTransform();
    }

    /// <summary>
    /// Updates the position and rotation of Minimap.
    /// </summary>
    /// <remarks>
    /// In most cases, the desired behaviour would be to call this whenever Center moves.
    /// Performing this check in Update or LateUpdate may result in choppy movement if it executes
    /// before Center moves in the same frame, hence it is called elsewhere.
    ///
    /// For example, where Center is the Transform of a <see cref="CameraController"/>, the event
    /// <see cref="CameraController.OnTransform"/> can be used to call this method.
    /// </remarks>
    public void UpdateTransform() {
      if (Center != null) {
        transform.position = Center.position + PositionOffset;
        transform.rotation = Quaternion.Euler(Vector3.up * Center.eulerAngles.y + RotationOffset);
      }
    }
  }
}
