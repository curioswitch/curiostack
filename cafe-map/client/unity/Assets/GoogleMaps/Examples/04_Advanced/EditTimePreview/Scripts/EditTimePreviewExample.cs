using System;
using Google.Maps.Event;
using Google.Maps.Feature.Style;
using UnityEngine;

namespace Google.Maps.Examples {
  /// <summary>
  /// Example of scene set up to make use of the edit-time preview function. To achieve a consistent
  /// look between edit and play mode, the script must load regions using the settings from
  /// <see cref="mapsService.GetAttachedGameObjectOptions"/>.
  ///
  /// This <see cref="MonoBehaviour"/> also contains a custom handler
  /// <see cref="OnWillCreateExtrudedStructure"/> which randomizes the heights of extruded
  /// structures. This handler is registered with <see cref="mapsService"/> in the inspector in the
  /// example scene. When the settings on this <see cref="MonoBehaviour"/> are changed in the
  /// in the inspector the preview should update automatically. This illustrates how the effect of
  /// user-registered handlers can be previewed at edit time. Warning: code that runs at edit time
  /// can modify your project (e.g. adding many spurious objects to your scene). For this reason it
  /// is important to be careful about the sort of code that you allow to run at edit time.
  /// </summary>
  [RequireComponent(typeof(MapsService)), ExecuteInEditMode]
  public sealed class EditTimePreviewExample : MonoBehaviour {
    /// <summary>
    /// Random seed for varying heights of extruded structures.
    /// </summary>
    public int RandomSeed = 123;

    /// <summary>
    /// Minimum extruded structure height.
    /// </summary>
    [Range(5, 200)]
    public int MinExtrudedStructureHeight = 10;

    /// <summary>
    /// Maximum extruded structure height.
    /// </summary>
    [Range(5, 200)]
    public int MaxExtrudedStructureHeight = 50;

    /// <summary>
    /// Random number generator. We use our own generator to ensure consistent results across
    /// platforms.
    /// </summary>
    private sealed class PRNG {
      /// <summary>
      /// Generator state.
      /// </summary>
      private uint State;

      /// <summary>
      /// Constructor.
      /// </summary>
      /// <param name="seed">The seed to use for generating random numbers.</param>
      public PRNG(int seed) {
        State = (uint)seed;
      }

      /// <summary>
      /// Mix a value into the generator state, producing a new state deterministically from the
      /// current state plus the specified value.
      /// </summary>
      public void Mix(int value) {
        NextState();
        State ^= ((uint)value);
      }

      /// <summary>
      /// Transition to a new state deterministically from the current state.
      /// </summary>
      private void NextState() {
        uint rmb = State & 1;
        uint tap1 = (State & 4) >> 2;
        uint tap2 = (State & 16) >> 4;
        uint tap3 = (State & 2) >> 1;
        rmb ^= tap1;
        rmb ^= tap2;
        rmb ^= tap3;
        State >>= 1;
        State |= rmb << 31;
      }

      /// <summary>
      /// Generate a random *int* between <see cref="min"/> and <see cref="max"/> (inclusive).
      /// </summary>
      /// <param name="min">The minimum value.</param>
      /// <param name="max">The maximum value.</param>
      /// <returns>A random *int* between <see cref="min"/> and <see cref="max"/>.</returns>
      public int NextRandomInt(int min, int max) {
        int result = min + (Math.Abs((int)State) % (max - min + 1));
        NextState();

        return result;
      }

      /// <summary>
      /// Generate a random number between *0* and *1* (inclusive).
      /// </summary>
      public float NextRandomFloat() {
        return NextRandomInt(0, 10000) / 10000.0f;
      }
    }

    /// <summary>
    /// Handle Unity *Start* event.
    /// </summary>
    private void Start() {
      if (!Application.isPlaying) {
        return;
      }

      // Get required Maps Service component on this GameObject.
      MapsService mapsService = GetComponent<MapsService>();

      // Set real-world location to load.
      mapsService.InitFloatingOrigin(mapsService.MapPreviewOptions.Location);

      // Load map with default options.
      mapsService.LoadMap(mapsService.GetPreviewBounds(), mapsService.MaybeGetGameObjectOptions());
    }

    /// <summary>
    /// Randomize building heights deterministically based on a random seed plus the structure's
    /// place id.
    /// </summary>
    public void OnWillCreateExtrudedStructure(WillCreateExtrudedStructureArgs args) {
      if (!enabled) {
        return;
      }

      string placeId = args.MapFeature.MapFeatureMetadata.PlaceId;
      PRNG prng = new PRNG(RandomSeed);

      for (int i = 0; i < placeId.Length; i++) {
        prng.Mix(placeId[i]);
      }

      ExtrudedStructureStyle.Builder builder = args.Style.AsBuilder();
      builder.ApplyFixedHeight = true;
      int minHeight = MinExtrudedStructureHeight;
      int maxHeight = MaxExtrudedStructureHeight;
      builder.FixedHeight = minHeight + prng.NextRandomFloat() * (maxHeight - minHeight);

      args.Style = builder.Build();
    }

    /// <summary>
    /// Handle component reset.
    /// </summary>
    private void Reset() {
      GetComponent<MapsService>().RefreshPreview();
    }

    /// <summary>
    /// Handle Unity *OnEnable* event.
    /// </summary>
    private void OnEnable() {
      GetComponent<MapsService>().RefreshPreview();
    }

    /// <summary>
    /// Handle Unity *OnDisable* event.
    /// </summary>
    private void OnDisable() {
      GetComponent<MapsService>().RefreshPreview();
    }

    /// <summary>
    /// Handle Unity *OnValidate* event.
    /// </summary>
    private void OnValidate() {
      GetComponent<MapsService>().RefreshPreview();
    }
  }
}
