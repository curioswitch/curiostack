// BaseMap shader for use on ground/water regions.
// Diffuse and specular lighting, with shadow receiving (but not casing).
Shader "Google/Maps/Shaders/BaseMap Worldspace Textured Water" {
  Properties {
    _Color ("Color", Color) = (1,1,1,1)
    _MainTex ("Albedo (RGB)", 2D) = "white" {}
    _Glossiness ("Smoothness", Range(0,1)) = 0.5
    _Metallic ("Metallic", Range(0,1)) = 0.0
    _FrostingExponent("Frosting Exponent", Range(0, 10)) = 2.0
    _FrostingColor ("Frosting Color", Color) = (1, 1, 1, 1)

    // Speed of animated water movement in worldspace meters.
    _SpeedX("Base Speed (X)", Float) = 1.0
    _SpeedZ("Base Speed (Z)", Float) = 0.5
    _SpeedOverlay("Overlay Speed (Relative)", Float) = 1.5

    // Offset applied to worldspace texture coordinates. Only the first and
    // third (x and z) value of this Vector are used, providing a top-down
    // offset.
    _Offset("Worldspace Offset", Vector) = (0.0, 0.0, 0.0, 0.0)
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
    #pragma surface surf Standard fullforwardshadows alpha:blend

    // Use shader model 3.0 target, to get nicer looking lighting.
    #pragma target 3.0

    // Input parameters.
    half _Glossiness;
    half _Metallic;
    fixed4 _Color;
    sampler2D _MainTex;
    uniform fixed4 _Offset;
    uniform float4 _MainTex_ST;
    uniform float _ScaleOverlay;
    uniform float _SpeedX, _SpeedZ, _SpeedOverlay;
    uniform float _FrostingExponent;
    uniform float4 _FrostingColor;
    #define PI 3.14159265359

    // Vertex input.
    struct Input {
      float3 worldPos;
    };

    // Colorize a value
    float3 frost(float val, float3 color) {
      return lerp(color, _FrostingColor.rgb, pow(val, _FrostingExponent));
    }

    // Surface shader itself.
    void surf (Input input, inout SurfaceOutputStandard output) {
      // Convert worldspace position to uv coordinates. Translate x position to
      // u coordinates, and z positions to v coordinates.
      float2 worldspaceUv = float2(
          (input.worldPos.x + _Offset.x) / _MainTex_ST.x,
          (input.worldPos.z + _Offset.z) / _MainTex_ST.y);

      // Generate rotated texture coordinates moving at at different rate. These
      // rotated coordinates are used to overlay a copy of the map ontop itself,
      // with both versions moving against each other to simulate movement of
      // water.
      float sinHalfPi = sin(1.57079632679);
      float cosHalfPi = cos(1.57079632679);
      float2x2 rotationMatrix
          = float2x2(cosHalfPi, -sinHalfPi, sinHalfPi, cosHalfPi);
      float2 worldspaceUvOverlay = mul(worldspaceUv, rotationMatrix);

      // Distort textures to produce flowing effect
      worldspaceUv.x += sin(worldspaceUv.y*5*2*PI)*0.03;
      worldspaceUv.y += sin(worldspaceUv.x*1*2*PI)*0.07;
      worldspaceUvOverlay.x += sin(worldspaceUv.y*7*2*PI)*0.05;
      worldspaceUvOverlay.y += sin(worldspaceUv.x*3*2*PI)*0.05;

      // Offset texture coordinates over time to simulate moving water.
      worldspaceUv.x += _Time * _SpeedX;
      worldspaceUv.y += _Time * _SpeedZ;
      worldspaceUvOverlay.x += _Time * _SpeedX * _SpeedOverlay;
      worldspaceUvOverlay.y += _Time * _SpeedZ * _SpeedOverlay;

      // Albedo comes from worldspace texture and rotated worldspace overlay,
      // added together (then divided by 2 to bring back to a normal color
      // range), and finally tinted by given color.
      fixed4 color = (tex2D(_MainTex, worldspaceUv)
          + tex2D(_MainTex, worldspaceUvOverlay)) * 0.5;
      output.Albedo = frost(color.g, _Color);

      // Metallic and smoothness come from slider variables.
      output.Metallic = _Metallic;
      output.Smoothness = _Glossiness;
      output.Alpha = color.a;
    }
    ENDCG
  }
  FallBack "Diffuse"
}
