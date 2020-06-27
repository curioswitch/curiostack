using System.Collections.Generic;
using UnityEngine;

namespace Google.Maps.Examples {
  /// <summary>
  /// Script to blow a building up into cubes.
  /// </summary>
  public class BuildingExploder : MonoBehaviour {
    /// <summary>
    /// Maximum chunks to spawn when a building explodes.
    /// </summary>
    private const int MAX_CHUNKS_PER_BUILDING = 200;

    /// <summary>
    /// Layer on which to spawn debris.
    /// </summary>
    private const int DEBRIS_LAYER = 10;

    /// <summary>
    /// Lifetime of each piece of debris.
    /// </summary>
    private const float DEBRIS_LIFETIME = 2;

    /// <summary>
    /// The material to apply to spawned chunks.
    /// </summary>
    public Material ChunkMaterial;

    /// <summary>
    /// Estimate the volume of the building.
    /// </summary>
    private float EstimateVolume() {
      Renderer buildingRenderer = gameObject.GetComponent<Renderer>();
      Bounds bounds = buildingRenderer.bounds;
      Vector3 size = bounds.size;

      return size.x * size.y * size.z;
    }

    /// <summary>
    /// Spawn and throw a chunk with the specified volume.
    /// </summary>
    /// <param name="explosionPosition">The position of the explosion force.</param>
    /// <param name="volume">The volume of the chunk.</param>
    private void ThrowChunk(Vector3 explosionPosition, float volume) {
      Physics.IgnoreLayerCollision(DEBRIS_LAYER, 0);

      float dims = Mathf.Pow(volume, 1.0f / 3.0f);

      GameObject chunk = GameObject.CreatePrimitive(PrimitiveType.Cube);
      float force = 5000f;
      float spacing = 5f;
      chunk.transform.localScale = Vector3.one * dims;
      chunk.transform.position = gameObject.transform.position
          + new Vector3(
              Random.Range(-spacing, spacing),
              Random.Range(0, spacing),
              Random.Range(-spacing, spacing));
      Rigidbody rigidbody = chunk.AddComponent<Rigidbody>();
      rigidbody.AddExplosionForce(force, explosionPosition, 50f, 100f);

      chunk.layer = DEBRIS_LAYER;

      ActionTimer despawnTimer = chunk.AddComponent<ActionTimer>();
      despawnTimer.Action = delegate {
        Destroy(chunk);
      };
      despawnTimer.Expiry = DEBRIS_LIFETIME;

      Renderer chunkRenderer = chunk.GetComponent<Renderer>();
      chunkRenderer.sharedMaterial = ChunkMaterial;
    }

    /// <summary>
    /// Throw out randomly sized chunks until we've ejected the estimated volume of the
    /// building.
    /// </summary>
    /// <param name="explosionPosition">The position of the explosion.</param>
    public void Explode(Vector3 explosionPosition) {
      float volumeRemaining = EstimateVolume()*0.5f;

      System.Random random = new System.Random();
      int chunks = 0;

      while ((volumeRemaining > 0) && (chunks < MAX_CHUNKS_PER_BUILDING)) {
        float chunkVolume = 0.5f + random.Next(0, 30);
        chunkVolume = Mathf.Min(chunkVolume, volumeRemaining);
        ThrowChunk(explosionPosition, chunkVolume);
        volumeRemaining -= chunkVolume;
        chunks++;
      }

      Destroy(gameObject);
    }
  }
}
