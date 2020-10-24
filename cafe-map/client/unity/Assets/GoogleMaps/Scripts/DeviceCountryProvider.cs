#if UNITY_IOS
using System.Runtime.InteropServices;
#endif
#if UNITY_ANDROID
using UnityEngine;
#endif

namespace Google.Maps.Scripts {
  /// <summary>
  /// Script to get the country portion of the device's locale using native code on Android and iOS.
  /// </summary>
  public class DeviceCountryProvider : CountryProvider {
    /// <summary>Device's country code, populated during <see cref="Awake"/>.</summary>
    private string CountryCode;

    /// <summary>
    /// When woken up by Unity, get the device's country code, if available. Do this once at the
    /// start, instead of in <see cref="GetCountry"/>, because native calls can be expensive on some
    /// platforms.
    /// </summary>
    public void Awake() {
#if UNITY_EDITOR

      // If running in the editor, don't try to run native code, even if the build target is one of
      // the mobile platforms below.
      CountryCode = null;
#elif UNITY_ANDROID
      try {
        using (var localeClass = new AndroidJavaClass("java.util.Locale")) {
          using (var defaultLocale = localeClass.CallStatic<AndroidJavaObject>("getDefault")) {
            CountryCode = defaultLocale.Call<string>("getCountry");
          }
        }
      } catch (System.Exception e) {
        Debug.LogWarningFormat("<color=red><b>[Maps SDK]</b></color> DeviceCountryError: " +
            "Couldn't get device country: {0}\nSee https://developers.google.com/maps/" +
            "documentation/gaming/support/error_codes#device-country-error for more information.",
            e);
      }
#elif UNITY_IOS
      CountryCode = MuskGetLocaleRegion();
#else
      CountryCode = null;
#endif
    }

    /// <inheritdoc/>
    public override string GetCountry() {
      return CountryCode;
    }

#if UNITY_IOS
    /// <summary>
    /// Native iOS function to get the device region.
    /// </summary>
    [DllImport("__Internal")]
    private static extern string MuskGetLocaleRegion();
#endif
  }
}
