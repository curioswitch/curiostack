// BaseMap shader for use on ground/water regions.
// Diffuse and specular lighting, with shadow receiving (but not casing).
// Version without texture, just an input color.
Shader "Google/Maps/Shaders/BaseMap Color" {
  Properties {
    _Color ("Color", Color) = (1,1,1,1)
    _Glossiness ("Smoothness", Range(0,1)) = 0.5
    _Metallic ("Metallic", Range(0,1)) = 0.0
  }
  SubShader {
    Tags { "RenderType"="Opaque" }

    LOD 200

    // Basemap renders multiple coincident ground plane features so we have to
    // disable z testing (make it always succeed) to allow for overdraw.
    ZTest Always

    CGPROGRAM
    // Physically based Standard lighting model, and enable shadows on all
    // light types.
    #pragma surface surf Standard fullforwardshadows

    // Use shader model 3.0 target, to get nicer looking lighting.
    #pragma target 3.0

    // Input parameters.
    half _Glossiness;
    half _Metallic;
    fixed4 _Color;

    // Vertex input.
    struct Input {
      float2 uv_MainTex;
    };

    // Surface shader itself.
    void surf (Input input, inout SurfaceOutputStandard output) {
      // Albedo comes from a texture tinted by color.
      fixed4 color = _Color;
      output.Albedo = color.rgb;

      // Metallic and smoothness come from slider variables.
      output.Metallic = _Metallic;
      output.Smoothness = _Glossiness;
      output.Alpha = color.a;
    }
    ENDCG
  }
  FallBack "Diffuse"
}
