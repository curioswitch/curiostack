using Google.Maps.Examples.Shared;
using UnityEngine;
using UnityEngine.Rendering;

namespace Google.Maps.Examples {
  /// <summary>
  /// Class for setting the angle, light and color of the sun and moon, based on a 0f to 1f value
  /// spanning 24 hours of lighting.
  /// </summary>
  [RequireComponent(typeof(Light), typeof(SliderController))]
  public sealed class SunAndMoonController : MonoBehaviour {
    /// <summary>Exposure of <see cref="Skybox"/> <see cref="Material"/> at midday.</summary>
    private const float SkyExposure = 1.4f;

    /// <summary>
    /// Atmosphere thickness of <see cref="Skybox"/> <see cref="Material"/> at midday.
    /// </summary>
    private const float DayThickness = 0.9f;

    /// <summary>
    /// Atmosphere thickness of <see cref="Skybox"/> <see cref="Material"/> at sunrise and sunset.
    /// </summary>
    private const float HalfLightThickness = 2.7f;

    /// <summary>
    /// Atmosphere thickness of <see cref="Skybox"/> <see cref="Material"/> at night.
    /// </summary>
    private const float NightThickness = 0.3f;

    /// <summary>Color to tint <see cref="Skybox"/> <see cref="Material"/> at midday.</summary>
    private static Color SkyTint = new Color(0f, 0.3f, 0.5f);

    /// <summary>Color of fog in the middle of the day.</summary>
    private static Color FogColor = new Color(0.625f, 0.86f, 1f);

    /// <summary>
    /// Amount of ambient light to show during the day.
    /// </summary>
    private const float DayAmbientParent = 0.5f;

    /// <summary>
    /// Amount of ambient light to show at sunrise and sunset.
    /// </summary>
    private const float HalfLightAmbientParent = 0.3f;

    [Tooltip(
        "Current value of light, where 0f is midday, 0.5f is midnight, and 1f is midday again.")]
    public float Value = 0.5f;

    [Range(0f, 12f), Tooltip("Hour of the morning when the sun rises (e.g. 5.5 = 5:30 am).")]
    public float Sunrise = 6f;

    [Range(0f, 12f), Tooltip("Hour of the evening when the sun sets (e.g. 7.5 = 7:30 pm).")]
    public float Sunset = 7.5f;

    [Range(0f, 12f),
     Tooltip(
         "Total hours after sunrise and before sunset when light as at its most " +
         "vivid color (e.g. 1f means light will be fully colored as sunset 1 hour before sun " +
         "fully sets and night begins.")]
    public float VividToDark = 1f;

    [Tooltip(
        "Size of the sun in Skybox Material (default value for this Procedural Skybox Shader is " +
        "0.04f, while 0.1f seems to give a nice, exaggerated size of sun and beautiful sunsets).")]
    public float SunSize = 0.1f;

    [Tooltip("Start point of linear fog in meters.")]
    public float FogStart = 300f;

    [Tooltip("End point of linear fog in meters.")]
    public float FogEnd = 1000f;

    [Tooltip(
        "Render distance of Camera.main - extended so ground plane will merge seamlessly into" +
        "skybox.")]
    public float RenderDistance = 20000f;

    [Header("Colors")]
    [Tooltip("Color of light at midday.")]
    public Color DayColor = new Color(1f, 0.96f, 0.86f);

    [Tooltip("Color of light at sunrise and sunset.")]
    public Color HalfLightColor = new Color(1f, 0.52f, 0f);

    [Tooltip("Color of light at full moon.")]
    public Color MoonColor = new Color(0f, 0.49f, 0.62f);

    [Tooltip("Color of light just after sunrise or sunset.")]
    public Color DarkestColor = new Color(0f, 0f, 0.4f);

    [Tooltip(
        "Script for setting emission values on given Materials, used to simulate building " +
        "lights turning on/off during day/night.")]
    public EmissionController EmissionController;

    /// <summary>The various stages of the day/night cycle.</summary>
    public enum Stage {
      FullMoonAfterToDark,
      FullMoonToDark,
      DarkToSunrise,
      SunriseToMidday,
      MiddayToSunset,
      SunsetToDark,
      DarkToFullMoonAfter,
      DarkToFullMoon,
      DarkToElse
    }

    [Header("Read Only"), Tooltip("Read only: the current stage of the day/night.")]
    public Stage CurrentStage;

    [Tooltip("Read only: current value within stage (before smoothing is applied)."), Range(0f, 1f)]
    public float CurrentValueRaw;

    [Tooltip("Read only: current value within stage (after smoothing is applied)."), Range(0f, 1f)]
    public float CurrentValue;

    [Tooltip("Read only: Current percent-value of emissive lights."), Range(0f, 1f)]
    public float CurrentEmission;

    [Tooltip("Read only: current ambient light color.")]
    public Color CurrentAmbient;

    /// <summary>
    /// Required light on this gameObject, used as the light of the sun and/or moon.
    /// </summary>
    private Light Light;

    /// <summary>
    /// Required component for getting input from <see cref="UnityEngine.UI.Slider"/>, which is
    /// animated until player input is detected.
    /// </summary>
    private SliderController SliderController;

    /// <summary>
    /// <see cref="Camera.main"/>, stored during start for ease of access later.
    /// </summary>
    private Camera MainCamera;

    /// <summary>
    /// Skybox material applied to the <see cref="Camera.main"/>'s <see cref="Skybox"/>.
    /// </summary>
    private Material Sky;

    /// <summary>
    /// Azimuth of the sun/moon (what direction the sun/moon is parallel to the ground, as opposed
    /// to zenith which is how high the sun/moon is relative to the horizon).
    /// </summary>
    private float Azimuth;

    /// <summary>
    /// Calculated point between 0f and 1f when sun rises.
    /// </summary>
    private float SunriseDark;

    /// <summary>
    /// Calculated point between 0f and 1f when sunrise is at its most vivid.
    /// </summary>
    private float SunriseVivid;

    /// <summary>
    /// Calculated point between 0f and 1f when sun sets.
    /// </summary>
    private float SunsetDark;

    /// <summary>
    /// Calculated point between 0f and 1f when sunset is at its most vivid.
    /// </summary>
    private float SunsetVivid;

    /// <summary>
    /// Calculated midpoint between sunrise and sunset.
    /// </summary>
    private float FullSun;

    /// <summary>
    /// Calculated midpoint between sunset and sunrise.
    /// </summary>
    private float FullMoon;

    /// <summary>
    /// Whether calculated midpoint between sunset and sunrise is before 1f, or after 0f (after
    /// modulating to within 0f to 1f range).
    /// </summary>
    private bool FullMoonAfterMidnight;

    /// <summary>
    /// Calculated midpoint between sunset and sunrise that is always small (below 0f or near 0f),
    /// for use in calculating percents between full moon and start of sunrise.
    /// </summary>
    private float FullMoonLower;

    /// <summary>Calculated sum of time between sunset and sunrise.</summary>
    private float NightTotal;

    /// <summary>
    /// Color of ambient light at midday - taken from given Day Color multiplied by Ambient Day
    /// Percent.
    /// </summary>
    private Color DayAmbient;

    /// <summary>
    /// Color of ambient light at sunrise and sunset - taken from given Half Light Color multiplied
    /// by Ambient Day Percent.
    /// </summary>
    private Color HalfLightAmbient;

    /// <summary>Has this script been setup yet?</summary>
    /// <remarks>
    /// This flag is used to allow this script to be setup early if needed (e.g. if another class
    /// needs to interact with it before this script's Start function is called.
    /// </remarks>
    private bool IsSetup;

    /// <summary>
    /// Setup required <see cref="UnityEngine.Light"/> and all values needed to use it as a sun
    /// and/or moon.
    /// </summary>
    /// <remarks>
    /// This flag is used to allow this script to be setup early if needed (e.g. if another class
    /// needs to interact with it before this script's Start function is called.
    /// </remarks>
    private void Start() {
      TrySetup();
    }

    /// <summary>
    /// Setup required <see cref="UnityEngine.Light"/> and all values needed to use it as a sun
    /// and/or moon.
    /// </summary>
    /// <remarks>
    /// This flag is used to allow this script to be setup early if needed (e.g. if another class
    /// needs to interact with it before this script's Start function is called.
    /// </remarks>
    private void TrySetup() {
      // Skip if this script has already been setup.
      if (IsSetup) {
        return;
      }

      // Verify all required parameters are defined and correctly setup, skipping any further setup
      // if any parameter is missing or invalid.
      if (!VerifyParameters()) {
        return;
      }

      // Get required SliderController for getting input from UI Slider.
      SliderController = GetComponent<SliderController>();

      // Connect changes in Slider input to changing sun/moon.
      SliderController.OnChange += Set;

      // Record the y-angle of the light as the azimuth of the sun/moon, i.e. what direction it is
      // in parallel to the ground.
      Azimuth = Light.transform.eulerAngles.y;

      // Convert given time values of sunrise/sunset into points between 0f and 1f.
      SunriseDark = Sunrise / 24f;

      // Calculate the most vivid point of sunrise.
      float vividToDarkInterval = VividToDark / 24f;
      SunriseVivid = SunriseDark + vividToDarkInterval;

      // Calculate point when light is most vivid.
      SunsetDark = Sunset / 24f + 0.5f;
      SunsetVivid = SunsetDark - vividToDarkInterval;

      // Calculate midpoints between sunrise and sunset, when sun and moon are at their fullest.
      FullSun = (SunriseDark + SunsetDark) / 2f;
      FullMoon = FullSun + 0.5f;
      FullMoonAfterMidnight = FullMoon > 1f;

      if (FullMoonAfterMidnight) {
        FullMoon %= 1f;
        FullMoonLower = FullMoon;
      } else {
        FullMoonLower = FullMoon - 1f;
      }

      // Calculate total night time.
      NightTotal = SunriseDark + (1f - SunsetDark);

      // Setup the ambient light to use a color, specifically the Darkest Color. This Darkest Color
      // is not actually used to color the darkest points of the sun/moon light's cycle,
      // specifically the points when the sun and moon drop below the horizon. This is because, when
      // the sun/moon light drops below the horizon, if the light has any color at all it will
      // sharply cut off as it drops beneath the ground plane. As such, at these points the sun/moon
      // light fades to a black (no light) color, while the ambient light fades in with the Darkest
      // Color.
      RenderSettings.ambientMode = AmbientMode.Flat;

      // Make sure fog is enabled, as this will be colored based on the changing light colors.
      RenderSettings.fog = true;
      RenderSettings.fogMode = FogMode.Linear;
      RenderSettings.fogStartDistance = FogStart;
      RenderSettings.fogEndDistance = FogEnd;

      // Setup the main Camera to work with a procedural Skybox, so we can adjust the sky based on
      // the time of day/night. Also extend Camera's far clipping plane so that the ground can
      // seamlessly merge from Fog to Skybox.
      MainCamera = Camera.main;
      MainCamera.clearFlags = CameraClearFlags.Skybox;
      MainCamera.farClipPlane = RenderDistance;

      Skybox skybox = MainCamera.gameObject.GetComponent<Skybox>();

      if (skybox == null) {
        skybox = MainCamera.gameObject.AddComponent<Skybox>();
      }

      Sky = skybox.material;

      if (Sky == null) {
        Sky = new Material(Shader.Find("Skybox/Procedural"));
        skybox.material = Sky;
      }

      Sky.SetFloat("_SunSize", SunSize);
      Sky.SetFloat("_Exposure", SkyExposure);
      Sky.SetColor("_SkyTint", SkyTint);

      // Calculate the ambient light color/value during midday and half light. At night the Darkest
      // Color is used (the reason why is explained above).
      DayAmbient = DayColor * DayAmbientParent;
      HalfLightAmbient = HalfLightColor * HalfLightAmbientParent;

      // Flag that this script has now been setup and apply starting value.
      IsSetup = true;
      Set(Value);
    }

    /// <summary>Start animating day-night cycle.</summary>
    internal void StartAnimating() {
      // Make sure this script is setup, setting it up now if not.
      TrySetup();

      // Start animation of this Slider.
      SliderController.StartAnimating();
    }

    /// <summary>Start animating day-night cycle at a given point in the day.</summary>
    /// <param name="startValue">
    /// Time of day to start animation at, where 0f is midday, 0.5f is midnight, and 1f is midday
    /// again.
    /// </param>
    internal void StartAnimatingAt(float startValue) {
      // Make sure this script is setup, setting it up now if not.
      TrySetup();

      // Start animation of this Slider at given start value.
      SliderController.StartAnimatingAt(startValue);
    }

    /// <summary>Stop animating day-night cycle.</summary>
    internal void StopAnimating() {
      // Make sure this script is setup, setting it up now if not.
      TrySetup();

      // Skip if animation is not current in progress.
      if (SliderController.IsAnimating) {
        SliderController.StopAnimating();
      }
    }

    /// <summary>Stop animating day-night cycle at a given point in the day.</summary>
    /// <param name="stopValue">
    /// Time of day to start animation at, where 0f is midday, 0.5f is midnight, and 1f is midday
    /// again.
    /// </param>
    internal void StopAnimatingAt(float stopValue) {
      // Make sure this script is setup, setting it up now if not.
      TrySetup();

      // Setup end of animation if it is in progress.
      if (SliderController.IsAnimating) {
        SliderController.StopAnimatingAt(stopValue);
      } else {
        // If animation is not currently in progress, immediately apply new value instead.
        Set(stopValue);
      }
    }

    /// <summary>
    /// Set a new day-night value.
    /// </summary>
    /// <param name="newValue">
    /// Value to set, where 0f is midday, 0.5f is midnight, and 1f is midday again.
    /// </param>
    internal void Set(float newValue) {
      // Make sure this script is setup, setting it up now if not.
      TrySetup();

      // Verify given starting value is within the range 0f to 1f.
      if (newValue < 0f || newValue > 1f) {
        // Wrap value back to within range.
        float originalValue = newValue;

        if (newValue < 0f) {
          newValue = newValue * -1f % 1f * -1f;
        } else {
          newValue = newValue % 1f;
        }

        // Print a warning saying clamping was required.
        Debug.LogWarningFormat(
            "Invalid value of {0} given to {1}.{2}.Set, which expected a value " +
                "between 0f and 1f.\nAdjusting value from {0} to {3}.",
            originalValue,
            name,
            GetType(),
            newValue);
      }

      // Offset value so that 0f becomes exactly midday.
      newValue = (newValue + FullSun) % 1f;

      // Determine if, for this value, the sun or the moon should be used.
      Value = newValue;

      if (newValue > SunriseDark && newValue < SunsetDark) {
        // Set angle of sun using solarAzimuth and given value.
        float dayPercent = PercentOf(newValue, SunriseDark, SunsetDark);
        Light.transform.eulerAngles = new Vector3(dayPercent * 180f, Azimuth, 0f);
      } else {
        // Set angle of the moon using solarAzimuth and given value.
        float newValueAdjusted =
            newValue < SunriseDark ? newValue + (1f - SunsetDark) : newValue - SunsetDark;
        float nightPercent = newValueAdjusted / NightTotal;
        Light.transform.eulerAngles = new Vector3(nightPercent * 180f, Azimuth, 0f);
      }

      // Determine the exact time period of the day - midnight to sunrise dark, sunrise dark sunrise
      // vivid, sunrise vivid to midday, midday to sunset vivid, sunset vivid to sunset dark, sunset
      // dark to midnight. The specific interval is used to calculate color by lerping between given
      // colors for given times.
      float intervalStart, intervalEnd;
      bool smoothStart, smoothEnd;
      Color lightStart, lightEnd;
      Color? fogStart = null;
      Color? fogEnd = null;
      Color? fogStatic = null;
      Color? ambientStart = null;
      Color? ambientEnd = null;
      Color? ambientStatic = null;
      float? thicknessStart = null;
      float? thicknessEnd = null;
      float? thicknessStatic = null;
      float? emissionStart = null;
      float? emissionEnd = null;
      float? emissionStatic = null;

      if (FullMoonAfterMidnight && newValue < FullMoon) {
        // Set up values for - Sunset to Midnight.
        intervalStart = SunsetDark - 1f;
        intervalEnd = FullMoon;
        lightStart = Color.black;
        lightEnd = MoonColor;
        ambientStatic = fogStatic = DarkestColor;
        thicknessStatic = NightThickness;
        emissionStatic = 1f;
        smoothStart = false;
        smoothEnd = true;
        CurrentStage = Stage.FullMoonAfterToDark;
      } else if (newValue < SunriseDark) {
        // Set up values for - Midnight to Sunrise.
        intervalStart = FullMoonLower;
        intervalEnd = SunriseDark;
        lightStart = MoonColor;
        lightEnd = Color.black;
        ambientStatic = fogStatic = DarkestColor;
        thicknessStatic = NightThickness;
        emissionStatic = 1f;
        smoothStart = false;
        smoothEnd = true;
        CurrentStage = Stage.FullMoonToDark;
      } else if (newValue < SunriseVivid) {
        // Set up values for - Sunrise, Dark to Vivid.
        intervalStart = SunriseDark;
        intervalEnd = SunriseVivid;
        lightStart = Color.black;
        lightEnd = HalfLightColor;
        ambientStart = DarkestColor;
        ambientEnd = HalfLightAmbient;
        fogStart = DarkestColor;
        fogEnd = HalfLightColor;
        thicknessStart = NightThickness;
        thicknessEnd = HalfLightThickness;
        emissionStart = 1f;
        emissionEnd = 0f;
        smoothStart = true;
        smoothEnd = false;
        CurrentStage = Stage.DarkToSunrise;
      } else if (newValue < FullSun) {
        // Set up values for - Sunrise to Midday.
        intervalStart = SunriseVivid;
        intervalEnd = FullSun;
        lightStart = HalfLightColor;
        lightEnd = DayColor;
        ambientStart = HalfLightAmbient;
        ambientEnd = DayAmbient;
        fogStart = HalfLightColor;
        fogEnd = FogColor;
        thicknessStart = HalfLightThickness;
        thicknessEnd = DayThickness;
        emissionStatic = 0f;
        smoothStart = false;
        smoothEnd = true;
        CurrentStage = Stage.SunriseToMidday;
      } else if (newValue < SunsetVivid) {
        // Set up values for - Midday to Sunset.
        intervalStart = FullSun;
        intervalEnd = SunsetVivid;
        lightStart = DayColor;
        lightEnd = HalfLightColor;
        ambientStart = DayAmbient;
        ambientEnd = HalfLightAmbient;
        fogStart = FogColor;
        fogEnd = HalfLightColor;
        thicknessStart = DayThickness;
        thicknessEnd = HalfLightThickness;
        emissionStart = 0f;
        emissionEnd = 0.5f;
        smoothStart = true;
        smoothEnd = false;
        CurrentStage = Stage.MiddayToSunset;
      } else if (newValue < SunsetDark) {
        // Set up values for - Sunset, Vivid to Dark.
        intervalStart = SunsetVivid;
        intervalEnd = SunsetDark;
        lightStart = HalfLightColor;
        lightEnd = Color.black;
        ambientStart = HalfLightAmbient;
        ambientEnd = DarkestColor;
        fogStart = HalfLightColor;
        fogEnd = DarkestColor;
        thicknessStart = HalfLightThickness;
        thicknessEnd = NightThickness;
        emissionStart = 0.5f;
        emissionEnd = 1f;
        smoothStart = false;
        smoothEnd = true;
        CurrentStage = Stage.SunsetToDark;
      } else {
        // Set up values for - Sunset to Midnight.
        // Note that these values may be set up slightly differently depending on whether full moon
        // is nearer to 0f or 1f.
        if (FullMoonAfterMidnight) {
          intervalStart = SunsetDark;
          intervalEnd = FullMoon + 1f;
          CurrentStage = Stage.DarkToFullMoonAfter;
        } else if (newValue < FullMoon) {
          intervalStart = SunsetDark;
          intervalEnd = FullMoon;
          CurrentStage = Stage.DarkToFullMoon;
        } else {
          intervalStart = FullMoon;
          intervalEnd = SunriseDark + 1f;
          CurrentStage = Stage.DarkToElse;
        }

        lightStart = Color.black;
        lightEnd = MoonColor;
        ambientStatic = fogStatic = DarkestColor;
        thicknessStatic = NightThickness;
        emissionStatic = 1f;
        smoothStart = true;
        smoothEnd = false;
      }

      // Calculate exact color and apply.
      CurrentValueRaw = PercentOf(newValue, intervalStart, intervalEnd);
      CurrentValue = Smoothed(CurrentValueRaw, smoothStart, smoothEnd);
      Light.color = Color.Lerp(lightStart, lightEnd, CurrentValue);

      // Determine current emission value. This may be a static value (if in the middle of day or
      // night, where emission is 0% or 100% for a long period if time), or a transitioning value
      // (if transitioning between day and night, or vice versa).
      CurrentEmission =
          emissionStatic ?? Mathf.Lerp(emissionStart.Value, emissionEnd.Value, CurrentValue);
      EmissionController.SetEmission(CurrentEmission);

      // Set the current ambient light.
      CurrentAmbient =
          ambientStatic ?? Color.Lerp(ambientStart.Value, ambientEnd.Value, CurrentValue);
      RenderSettings.ambientLight = CurrentAmbient;

      // Color fog to match time of day. Also use this color for the ground color of the Skybox to
      // make sure fog blends into the start of the sky.
      Color fogColor = fogStatic ?? Color.Lerp(fogStart.Value, fogEnd.Value, CurrentValue);
      RenderSettings.fogColor = fogColor;
      Sky.SetColor("_GroundColor", fogColor);

      // Color sky with day/night transition.
      Sky.SetFloat(
          "_AtmosphereThickness",
          thicknessStatic ?? Mathf.Lerp(thicknessStart.Value, thicknessEnd.Value, CurrentValue));
    }

    /// <summary>
    /// Convert a given value into a 0f-1f percent between a given minimum and maximum.
    /// </summary>
    /// <param name="value">Value to convert.</param>
    /// <param name="minimum">Value representing 0f (or 0%).</param>
    /// <param name="maximum">Value representing 1f (or 100%).</param>
    private static float PercentOf(float value, float minimum, float maximum) {
      return (value - minimum) / (maximum - minimum);
    }

    /// <summary>
    /// Return a given value smoothed as is approaches 0f and/or 1f.
    /// </summary>
    /// <param name="value">Value to smooth.</param>
    /// <param name="smoothStart">Smooth towards 0f?</param>
    /// <param name="smoothEnd">Smooth towards 1f?</param>
    private static float Smoothed(float value, bool smoothStart, bool smoothEnd) {
      if (smoothStart) {
        return smoothEnd ? (Mathf.Sin((value - 0.5f) * Mathf.PI) + 1f) / 2f
                         : Mathf.Sin((value - 1f) * Mathf.PI / 2f) + 1f;
      }

      return smoothEnd ? Mathf.Sin(value * Mathf.PI / 2f) : value;
    }

    /// <summary>
    /// Verify that all required parameters have been correctly defined, returning false if not.
    /// </summary>
    private bool VerifyParameters() {
      // TODO(b/149056787): Standardize parameter verification across scripts.
      // Verify that an Emission Controller has been defined.
      if (EmissionController == null) {
        Debug.LogError(ExampleErrors.MissingParameter(
            this,
            EmissionController,
            "Emission Controller",
            "to control the emission values of in-scene lights/windows"));

        return false;
      }

      // Get required light on this gameObject and make sure it is a directional light.
      Light = GetComponent<Light>();

      if (Light.type != LightType.Directional) {
        Debug.LogWarningFormat(
            "{0}.{1} found a {2}-light attached to {0}, when a Directional Light " +
                "was expected.\n Changing type from {2} to Directional.",
            name,
            GetType(),
            Light.type);
        Light.type = LightType.Directional;
      }

      // Verify given Fog Distances.
      if (FogStart < 0f) {
        Debug.LogError(ExampleErrors.NotGreaterThanZero(
            this,
            FogStart,
            "Fog Start distance",
            ", as this represents the Linear Fog Start Distance for the scene"));

        return false;
      }

      if (FogEnd < FogStart) {
        Debug.LogError(ExampleErrors.NotGreaterThan(
            this, FogEnd, "Fog End distance", FogStart, "Fog Start distance"));

        return false;
      }

      // Verify given Render Distance is positive.
      if (RenderDistance < 0f) {
        Debug.LogError(ExampleErrors.NotGreaterThanZero(
            this,
            RenderDistance,
            "Render Distance",
            ", as this represents the Camera's far Clipping Plane"));

        return false;
      }

      // If have reached this point then have verified that all required parts are present and
      // properly setup.
      return true;
    }
  }
}
