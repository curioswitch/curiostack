Shader "Custom/StandardCustomShader" {
  Properties {
      _Color ("Color", Color) = (1,1,1,1)
    _MainTex ("Albedo (RGB)", 2D) = "white" {}
    _BumpMap ("Bottom Wall Bumpmap", 2D) = "bump" {}
    _MetalMap ("Bottom Wall Metalmap", 2D) = "black" {}
    _DisplacementAmount("_Displacement Amount", float)  = 1.0
  }
  SubShader {
    Tags {
      "RenderType" = "Opaque"
    }
    LOD 200

    CGPROGRAM
    // Physically based Standard lighting model, and enable shadows on all light types
    #pragma surface surf Standard fullforwardshadows vertex:vert addshadow

    // Use shader model 3.0 target, to get nicer looking lighting
    #pragma target 3.0

    sampler2D _MainTex;
    sampler2D _BumpMap;
    sampler2D _MetalMap;

    float _DisplacementAmount;
    fixed4 _Color;

    struct Input {
      float2 uv_MainTex;
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
    }

    void surf(Input IN, inout SurfaceOutputStandard o) {
      float4 color = tex2D(_MainTex, IN.uv_MainTex) * _Color;

      o.Albedo = color.rgb;
      o.Normal = UnpackScaleNormal(tex2D(_BumpMap, IN.uv_MainTex), 1);
      o.Metallic = tex2D(_MetalMap, IN.uv_MainTex).r;
      o.Smoothness = 0.45;
      o.Alpha = color.a;
    }
    ENDCG
  }
  FallBack "Diffuse"
}
