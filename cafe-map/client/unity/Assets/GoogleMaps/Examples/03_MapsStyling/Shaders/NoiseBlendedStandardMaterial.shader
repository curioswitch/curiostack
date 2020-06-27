Shader "Custom/NoiseBlendedMaterial" {
  Properties {
    _Color ("Color", Color) = (1,1,1,1)
    _MainTex ("Albedo (RGB)", 2D) = "white" {}
    _Normal ("Normal (RGB)", 2D) = "bump" {}

    _BlendedNoiseTexture ("Extras Blended Albedo (RGB)", 2D) = "white" {}
    _BlendedNoiseNormal ("Extra Texture Normal (RGB)", 2D) = "bump" {}

    _Glossiness ("Smoothness", Range(0,1)) = 0.5
    _Metallic ("Metallic", Range(0,1)) = 0.0
    _DisplacementAmount("_Displacement Amount", float)  = 1.0
    _NoiseZoom("Noise Zoom", Range(0, 5)) = 1.0
  }
  SubShader {
    Tags {
      "RenderType" = "Opaque"
    }
    LOD 200

    CGPROGRAM
    // Physically based Standard lighting model, and enable shadows on all light types.
    #pragma surface surf Standard fullforwardshadows vertex:vert addshadow

    #include "noise.cginc"

    // Use shader model 3.0 target, to get nicer looking lighting
    #pragma target 3.0

    sampler2D _MainTex;
    sampler2D _Normal;

    sampler2D _BlendedNoiseTexture;
    sampler2D _BlendedNoiseNormal;

    float _NoiseZoom;
    float _DisplacementAmount;

    struct Input {
      float2 uv_MainTex;
      float2 uv_BlendedNoiseTexture;
    };

    half _Glossiness;
    half _Metallic;
    fixed4 _Color;

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
      UNITY_INITIALIZE_OUTPUT(Input, o);
    }

    void surf(Input IN, inout SurfaceOutputStandard o) {
      fixed4 primaryColor = tex2D (_MainTex, IN.uv_MainTex) * _Color;
      fixed3 primaryNormal = UnpackScaleNormal(tex2D(_Normal, IN.uv_MainTex), 3);
      fixed4 secondaryColor = tex2D (_BlendedNoiseTexture, IN.uv_BlendedNoiseTexture);
      fixed3 secondaryNormal =
          UnpackScaleNormal(tex2D (_BlendedNoiseNormal, IN.uv_BlendedNoiseTexture), 3);

      float noise = calculate_noise(IN.uv_MainTex * _NoiseZoom) / 2 + 0.5f;

      o.Albedo = lerp(primaryColor.rgb, secondaryColor.rgb, noise);
      o.Metallic = _Metallic;
      o.Smoothness = _Glossiness;
      o.Normal = lerp(primaryNormal, secondaryNormal, noise);
      o.Alpha = primaryColor.a;
    }
    ENDCG
  }
  FallBack "Diffuse"
}
