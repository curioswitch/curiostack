using UnityEngine;

namespace Google.Maps.Examples {
  /// <summary>
  /// Fades a light over time.
  /// </summary>
  public class LightFader : MonoBehaviour {
    /// <summary>
    /// Total life time of the light.
    /// </summary>
    public float TotalLifeTime;

    /// <summary>
    /// Remaining life time of the light.
    /// </summary>
    private float RemainingLifeTime;

    /// <summary>
    /// Light intensity at the beginning of the light's life time.
    /// </summary>
    public float MaxIntensity;

    /// <summary>
    /// Initialize remaining life time.
    /// </summary>
    public void Start() {
      RemainingLifeTime = TotalLifeTime;
    }

    /// <summary>
    /// Fade light by whatever time has passed since the last frame.
    /// </summary>
    public void Update() {
      RemainingLifeTime -= Time.deltaTime;
      float intensity = MaxIntensity*(RemainingLifeTime/TotalLifeTime);
      intensity = Mathf.Max(intensity, 0);
      GetComponent<Light>().intensity = intensity;
    }
  }
}
