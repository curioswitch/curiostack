Shader "Custom/BaseMapNoiseSeamless" {
  Properties {
    _Color ("Color", Color) = (1,1,1,1)
    _MainTex ("Albedo (RGB)", 2D) = "white" {}
    _Normal ("Normal (RGB)", 2D) = "bump" {}
    _NoiseCache ("Extra Noise texture", 2D) = "white" {}

    _Glossiness ("Smoothness", Range(0,1)) = 0.5
    _Metallic ("Metallic", Range(0,1)) = 0.0

    _NormalStrength("Normal Strength", Range(0, 10)) = 1.0
  }
  SubShader {
    Tags {
      "RenderType" = "Opaque"
    }

    LOD 200

    // Basemap renders multiple coincident ground plane features so we have to
    // disable z testing (make it always succeed) to allow for overdraw.
    ZTest Always

    CGPROGRAM
    // Physically based Standard lighting model, and enable shadows on all
    // light types
    #pragma surface surf Standard fullforwardshadows

    // Use shader model 3.0 target, to get nicer looking lighting
    #pragma target 3.0

    sampler2D _MainTex;
    sampler2D _Normal;
    sampler2D _NoiseCache;

    float _NormalStrength;

    struct Input {
      float2 uv_MainTex;
      float2 uv_NoiseCache;
    };

    half _Glossiness;
    half _Metallic;
    fixed4 _Color;

    float sum(float3 v) {
      return v.x + v.y + v.z;
    }

    float3 textureNoTile(in float2 x, float v, sampler2D t, bool isNormal) {
      float k = tex2D(_NoiseCache, 0.05 * x).xyz / 3.0;

      float2 duvdx = ddx(x);
      float2 duvdy = ddx(x);

      float l = k * 80.0;
      float i = floor(l);
      float f = frac(l);

      // The following can be replaced with any other hash.
      float2 offa = sin(float2(3.0, 7.0) * (i + 0.0));
      float2 offb = sin(float2(3.0, 7.0) * (i + 1.0));

      float4 cola = tex2D(t, x + v * offa, duvdx, duvdy);
      float4 colb = tex2D(t, x + v * offb, duvdx, duvdy);

      if (isNormal) {
        cola.rgb = UnpackScaleNormal(cola, _NormalStrength);
        colb.rgb = UnpackScaleNormal(colb, _NormalStrength);
      }
      return lerp(cola.rgb, colb.rgb, smoothstep(0.2, 0.8, f - 0.1 * sum(cola.rgb - colb.rgb)));
    }

    void surf(Input IN, inout SurfaceOutputStandard o) {
      o.Albedo = textureNoTile(IN.uv_MainTex, 1.0, _MainTex, false);
      o.Normal = textureNoTile(IN.uv_MainTex, 1.0, _Normal, true);
      o.Metallic = _Metallic;
      o.Smoothness = _Glossiness;
      o.Alpha = 1.0;
    }
    ENDCG
  }
  FallBack "Diffuse"
}
