Shader "Custom/ModeledStructures" {
  Properties {
      _Color ("Color", Color) = (1,1,1,1)
    _MainTex ("Albedo (RGB)", 2D) = "white" {}
    _BumpMap ("Bottom Wall Bumpmap", 2D) = "bump" {}
    _MetalMap ("Bottom Wall Metalmap", 2D) = "black" {}
    _EmissiveMap ("Bottom Wall Emissivemap", 2D) = "black" {}
    [HDR] _EmissionColor ("Emission Color", Color) = (0,0,0)

    _SecondaryTex ("Secondary Main (RGB)", 2D) = "white" {}
    _SecondaryBumpMap ("Secondary Bumpmap", 2D) = "bump" {}
    _SecondaryMetalMap ("Secondary Metalmap", 2D) = "black" {}

    _Tile("_tile Wall and Roof", Vector)  = (1.0, 1.0, 1.0, 1.0)
    _FakeOcclusionColor ("Fake Occlusion Color", Color) = (1,1,1,1)

    [Toggle(NOISE_BLENDING)]
        _NoiseBlending ("Noise Blending", Float) = 0
        _BlendingTex ("Blending Main (RGB)", 2D) = "white" {}
    _BlendingBumpMap ("Blending Bumpmap", 2D) = "bump" {}
    _BlendingMetalMap ("Blending Metalmap", 2D) = "black" {}
    _NoiseZoom ("Noise Zoom", Float) = 0

  }
  SubShader {
    Tags {
      "RenderType" = "Opaque"
    }
    LOD 200

    CGPROGRAM
    // Physically based Standard lighting model, and enable shadows on all light types
    #pragma surface surf Standard fullforwardshadows vertex:vert addshadow

    #include "noise.cginc"

    // Use shader model 3.0 target, to get nicer looking lighting
    #pragma target 3.0

    #pragma shader_feature NOISE_BLENDING

    sampler2D _MainTex;
    sampler2D _BumpMap;
    sampler2D _MetalMap;
    sampler2D _EmissiveMap;
    float4 _EmissionColor;

    sampler2D _SecondaryTex;
    sampler2D _SecondaryBumpMap;
    sampler2D _SecondaryMetalMap;

    #ifdef NOISE_BLENDING
    sampler2D _BlendingTex;
    sampler2D _BlendingBumpMap;
    sampler2D _BlendingMetalMap;
    float _NoiseZoom;
    #endif

    float _GeometryHeight;
    float _DisplacementAmount;
    float4 _Tile;

    fixed4 _Color;
    fixed4 _FakeOcclusionColor;

    struct Input {
      float3 worldPos;
      float3 normal;
      float4 fakeOcclusion;
    };

    // Add instancing support for this shader.
    // 'Enable Instancing' must be enabled on materials that use this shader.
    // See https://docs.unity3d.com/Manual/GPUInstancing.html for more information about instancing.
    // #pragma instancing_options assumeuniformscaling
    UNITY_INSTANCING_BUFFER_START(Props)
    // TODO: Put more per-instance properties here.
    UNITY_INSTANCING_BUFFER_END(Props)

    struct appdata {
      float4 vertex : POSITION;
      float3 normal: NORMAL;
      float4 tangent: TANGENT;
      float4 texcoord : TEXCOORD0;
      float4 texcoord1 : TEXCOORD1;
      float4 texcoord2 : TEXCOORD2;
    };

    void vert(inout appdata v, out Input o) {
      UNITY_INITIALIZE_OUTPUT(Input,o);
      o.normal = v.normal;
    }

    void surf(Input IN, inout SurfaceOutputStandard o) {
      float normalizedHeight =
          clamp(1.0 - IN.worldPos.y / (_GeometryHeight * _DisplacementAmount), 0.3, 1.0);

      float3 downVector = float3(0.0, -1.0, 0.0);

      float tiledWallX = fmod((IN.worldPos.x) * _Tile.x, 1.0);
      float tiledWallY = fmod((IN.worldPos.y) * _Tile.y, 1.0);

      float tiledRoofX = fmod(IN.worldPos.x * _Tile.z, 1.0);
      float tiledRoofY = fmod(IN.worldPos.z * _Tile.w, 1.0);

      float distance = step( dot(IN.normal, downVector) * 0.5 + 0.5, 0.3);

      float2 wallUV = float2(tiledWallX, tiledWallY);
      float2 roofUV = float2(tiledRoofX, tiledRoofY);

      float4 c = tex2D (_MainTex, wallUV) * _Color;
      float3 n = UnpackScaleNormal ( tex2D (_BumpMap, wallUV), 1 );
      float4 m = tex2D (_MetalMap, wallUV);
      float4 e = tex2D (_EmissiveMap, wallUV) * _EmissionColor;

      float4 c2 = tex2D (_SecondaryTex,roofUV) * _Color;
      float3 n2 = UnpackScaleNormal ( tex2D (_SecondaryBumpMap, roofUV), 1 );
      float4 m2 = tex2D (_SecondaryMetalMap, roofUV);

      #ifdef NOISE_BLENDING
      float noise = calculate_noise(IN.worldPos * _NoiseZoom) * 0.5 + 0.5f;

      fixed4 blendAlbedo = tex2D (_BlendingTex, roofUV);
      fixed4 blendMetal = tex2D (_BlendingMetalMap, roofUV);
      fixed3 blendNormal = UnpackScaleNormal(tex2D (_BlendingBumpMap, roofUV), 3);

      c2 = lerp(c2, blendAlbedo, noise);
      n2 = lerp(n2, blendNormal, noise);
      m2 = lerp(m2, blendMetal, noise);
      #endif

      o.Albedo = lerp(c.rgb, c2.rgb, distance);
      o.Albedo.rgb += lerp(e.rgb, float3(0.0, 0.0, 0.0), distance);

      o.Normal = lerp(n, n2, distance);
      o.Metallic = lerp(m.r, m2.r, distance);
      o.Smoothness = lerp(m.r, m2.r, distance);
      o.Alpha = c.a;
    }
    ENDCG
  }
  FallBack "Diffuse"
}
